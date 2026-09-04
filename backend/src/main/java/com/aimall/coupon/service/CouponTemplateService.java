package com.aimall.coupon.service;

import com.aimall.coupon.dto.CouponTemplateWriteRequest;
import com.aimall.coupon.entity.CouponTemplate;
import com.aimall.coupon.exception.*;
import com.aimall.coupon.mapper.CouponTemplateMapper;
import com.aimall.coupon.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.*;
import java.util.Set;

@Service
/**
 * 优惠券模板生命周期服务。
 *
 * <p>模板是“规则定义”，不是用户真正拥有的券。草稿可完整修改；ACTIVE 后核心规则锁定，
 * DISABLED 只阻止继续发放，不自动撤销已发出的用户券。</p>
 */
public class CouponTemplateService {
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISABLED = "DISABLED";
    public static final String TYPE_FIXED_AMOUNT = "FIXED_AMOUNT";
    public static final String VALIDITY_FIXED_RANGE = "FIXED_RANGE";
    public static final String VALIDITY_DAYS_AFTER_RECEIPT = "DAYS_AFTER_RECEIPT";
    private static final Set<String> STATUSES = Set.of(STATUS_DRAFT, STATUS_ACTIVE, STATUS_DISABLED);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final CouponTemplateMapper mapper;

    public CouponTemplateService(CouponTemplateMapper mapper) { this.mapper = mapper; }

    /** 分页查询模板，并把外部状态字符串规范化后再参与 SQL。 */
    public CouponTemplatePageResponse list(int page, int pageSize, String keyword, String status) {
        String normalizedStatus = normalizeOptionalStatus(status);
        LambdaQueryWrapper<CouponTemplate> query = new LambdaQueryWrapper<>();
        query.like(StringUtils.hasText(keyword), CouponTemplate::getName,
                        StringUtils.hasText(keyword) ? keyword.trim() : null)
                .eq(normalizedStatus != null, CouponTemplate::getStatus, normalizedStatus)
                .orderByDesc(CouponTemplate::getCreatedAt).orderByDesc(CouponTemplate::getId);
        Page<CouponTemplate> result = mapper.selectPage(Page.of(page, pageSize), query);
        return new CouponTemplatePageResponse(result.getRecords().stream().map(this::toResponse).toList(),
                result.getCurrent(), result.getSize(), result.getTotal(), result.getPages());
    }

    /** 查询模板详情，不存在时抛出统一 404 业务异常。 */
    public CouponTemplateResponse get(Long id) { return toResponse(requireTemplate(id)); }

    @Transactional
    /** 创建并校验模板；issuedQuantity 从 0 开始，status 固定为 DRAFT。 */
    public CouponTemplateResponse create(CouponTemplateWriteRequest request) {
        CouponTemplate template = new CouponTemplate();
        applyAndValidate(template, request);
        template.setIssuedQuantity(0);
        template.setStatus(STATUS_DRAFT);
        mapper.insert(template);
        return toResponse(requireTemplate(template.getId()));
    }

    @Transactional
    /** 完整替换草稿字段，并显式清理另一种有效期模式的 null 字段。 */
    public CouponTemplateResponse update(Long id, CouponTemplateWriteRequest request) {
        // 先读取旧模板，只有 DRAFT 可以修改；ACTIVE/DISABLED 的规则必须保持稳定。
        CouponTemplate existing = requireTemplate(id);
        if (!STATUS_DRAFT.equals(existing.getStatus())) {
            throw new CouponTemplateStateConflictException("只有草稿状态的优惠券模板可以修改");
        }
        // 在临时对象中完成全部校验，校验失败时不会碰数据库中的旧模板。
        CouponTemplate replacement = new CouponTemplate();
        applyAndValidate(replacement, request);
        if (replacement.getTotalQuantity() < existing.getIssuedQuantity()) {
            throw new CouponTemplateRuleException("发行总量不能小于已发行数量");
        }
        // PUT 是草稿可编辑字段的完整替换；显式写入 null，确保两种有效期模式切换时清除另一模式字段。
        // WHERE 同时带上 DRAFT 状态，避免管理员打开编辑页期间模板被别人启用后仍被覆盖。
        int updated = mapper.update(null, Wrappers.lambdaUpdate(CouponTemplate.class)
                .eq(CouponTemplate::getId, id)
                .eq(CouponTemplate::getStatus, STATUS_DRAFT)
                .set(CouponTemplate::getName, replacement.getName())
                .set(CouponTemplate::getCouponType, replacement.getCouponType())
                .set(CouponTemplate::getMinimumSpend, replacement.getMinimumSpend())
                .set(CouponTemplate::getDiscountAmount, replacement.getDiscountAmount())
                .set(CouponTemplate::getTotalQuantity, replacement.getTotalQuantity())
                .set(CouponTemplate::getPerUserLimit, replacement.getPerUserLimit())
                .set(CouponTemplate::getValidityType, replacement.getValidityType())
                .set(CouponTemplate::getValidFrom, replacement.getValidFrom())
                .set(CouponTemplate::getValidUntil, replacement.getValidUntil())
                .set(CouponTemplate::getValidDays, replacement.getValidDays())
                .set(CouponTemplate::getShareEnabled, replacement.getShareEnabled()));
        if (updated != 1) throw new CouponTemplateStateConflictException("优惠券模板状态已经变化，请刷新后重试");
        return toResponse(requireTemplate(id));
    }

    @Transactional
    /** 条件更新 DRAFT -> ACTIVE；重复启用直接返回当前模板。 */
    public CouponTemplateResponse activate(Long id) {
        // 先读取是为了给重复启用和非法状态提供明确业务语义；真正切换使用条件更新。
        CouponTemplate template = requireTemplate(id);
        if (STATUS_ACTIVE.equals(template.getStatus())) return toResponse(template);
        if (!STATUS_DRAFT.equals(template.getStatus())) {
            throw new CouponTemplateStateConflictException("只有草稿状态的优惠券模板可以启用");
        }
        validateActivation(template);
        int updated = mapper.update(null, Wrappers.lambdaUpdate(CouponTemplate.class)
                .eq(CouponTemplate::getId, id).eq(CouponTemplate::getStatus, STATUS_DRAFT)
                .set(CouponTemplate::getStatus, STATUS_ACTIVE));
        if (updated != 1) throw new CouponTemplateStateConflictException("优惠券模板状态已经变化，请刷新后重试");
        return toResponse(requireTemplate(id));
    }

    @Transactional
    /** 条件更新 ACTIVE -> DISABLED；重复停用直接返回当前模板。 */
    public CouponTemplateResponse deactivate(Long id) {
        // 停用不删除模板，也不修改已发出的用户券，只阻止后续发放/分享领取。
        CouponTemplate template = requireTemplate(id);
        if (STATUS_DISABLED.equals(template.getStatus())) return toResponse(template);
        if (!STATUS_ACTIVE.equals(template.getStatus())) {
            throw new CouponTemplateStateConflictException("只有启用状态的优惠券模板可以停用");
        }
        int updated = mapper.update(null, Wrappers.lambdaUpdate(CouponTemplate.class)
                .eq(CouponTemplate::getId, id).eq(CouponTemplate::getStatus, STATUS_ACTIVE)
                .set(CouponTemplate::getStatus, STATUS_DISABLED));
        if (updated != 1) throw new CouponTemplateStateConflictException("优惠券模板状态已经变化，请刷新后重试");
        return toResponse(requireTemplate(id));
    }

    private void applyAndValidate(CouponTemplate template, CouponTemplateWriteRequest request) {
        // 这里集中处理跨字段规则，DTO 注解只能校验单个字段的格式，无法表达金额大小关系。
        String couponType = request.couponType().trim().toUpperCase();
        if (!TYPE_FIXED_AMOUNT.equals(couponType)) {
            throw new CouponTemplateRuleException("首版只支持 FIXED_AMOUNT 满减券");
        }
        BigDecimal minimumSpend = parseAmount(request.minimumSpend(), "使用门槛");
        BigDecimal discountAmount = parseAmount(request.discountAmount(), "优惠金额");
        if (minimumSpend.signum() <= 0) throw new CouponTemplateRuleException("使用门槛必须大于 0");
        if (discountAmount.signum() <= 0) throw new CouponTemplateRuleException("优惠金额必须大于 0");
        if (discountAmount.compareTo(minimumSpend) >= 0) {
            throw new CouponTemplateRuleException("优惠金额必须小于使用门槛");
        }
        if (request.perUserLimit() > request.totalQuantity()) {
            throw new CouponTemplateRuleException("每人限领数量不能大于发行总量");
        }

        String validityType = request.validityType().trim().toUpperCase();
        LocalDateTime validFrom = null;
        LocalDateTime validUntil = null;
        Integer validDays = null;
        if (VALIDITY_FIXED_RANGE.equals(validityType)) {
            // 两种有效期字段互斥：固定范围只能有起止时间，不能同时有 validDays。
            if (request.validFrom() == null || request.validUntil() == null || request.validDays() != null) {
                throw new CouponTemplateRuleException("固定有效期必须提供开始和结束时间，且不能提供有效天数");
            }
            validFrom = toBusinessTime(request.validFrom());
            validUntil = toBusinessTime(request.validUntil());
            if (!validUntil.isAfter(validFrom)) throw new CouponTemplateRuleException("结束时间必须晚于开始时间");
        } else if (VALIDITY_DAYS_AFTER_RECEIPT.equals(validityType)) {
            // 领取后有效模式的起止时间由领取时刻计算，因此写入模板时必须为空。
            if (request.validDays() == null || request.validFrom() != null || request.validUntil() != null) {
                throw new CouponTemplateRuleException("领取后有效模式必须提供有效天数，且不能提供固定开始和结束时间");
            }
            validDays = request.validDays();
        } else {
            throw new CouponTemplateRuleException("有效期类型只能为 FIXED_RANGE 或 DAYS_AFTER_RECEIPT");
        }

        template.setName(request.name().trim());
        template.setCouponType(couponType);
        template.setMinimumSpend(minimumSpend);
        template.setDiscountAmount(discountAmount);
        template.setTotalQuantity(request.totalQuantity());
        template.setPerUserLimit(request.perUserLimit());
        template.setValidityType(validityType);
        template.setValidFrom(validFrom);
        template.setValidUntil(validUntil);
        template.setValidDays(validDays);
        template.setShareEnabled(request.shareEnabled());
    }

    private void validateActivation(CouponTemplate template) {
        // 启用是权益规则冻结点；过期模板或没有库存的模板不应进入 ACTIVE。
        if (VALIDITY_FIXED_RANGE.equals(template.getValidityType())
                && !template.getValidUntil().isAfter(LocalDateTime.now(BUSINESS_ZONE))) {
            throw new CouponTemplateRuleException("固定有效期优惠券的结束时间必须晚于当前时间");
        }
        if (template.getTotalQuantity() <= template.getIssuedQuantity()) {
            throw new CouponTemplateRuleException("优惠券没有可发行数量，不能启用");
        }
    }

    private BigDecimal parseAmount(String value, String fieldName) {
        // 金额使用 BigDecimal 而不是 double，避免二进制浮点误差影响满减判断。
        try {
            return new BigDecimal(value).setScale(2);
        } catch (ArithmeticException | NumberFormatException e) {
            throw new CouponTemplateRuleException(fieldName + "格式不正确");
        }
    }

    private String normalizeOptionalStatus(String status) {
        if (!StringUtils.hasText(status)) return null;
        String value = status.trim().toUpperCase();
        if (!STATUSES.contains(value)) throw new CouponTemplateRuleException("模板状态不合法");
        return value;
    }

    private CouponTemplate requireTemplate(Long id) {
        // 所有公开操作共用此存在性检查，保持 404 语义一致。
        CouponTemplate template = mapper.selectById(id);
        if (template == null) throw new CouponTemplateNotFoundException(id);
        return template;
    }

    private LocalDateTime toBusinessTime(OffsetDateTime value) {
        return value.atZoneSameInstant(BUSINESS_ZONE).toLocalDateTime();
    }

    private OffsetDateTime toOffsetTime(LocalDateTime value) {
        return value == null ? null : value.atZone(BUSINESS_ZONE).toOffsetDateTime();
    }

    private CouponTemplateResponse toResponse(CouponTemplate template) {
        return new CouponTemplateResponse(template.getId().toString(), template.getName(), template.getCouponType(),
                template.getMinimumSpend().toPlainString(), template.getDiscountAmount().toPlainString(),
                template.getTotalQuantity(), template.getIssuedQuantity(), template.getPerUserLimit(),
                template.getValidityType(), toOffsetTime(template.getValidFrom()), toOffsetTime(template.getValidUntil()),
                template.getValidDays(), template.getShareEnabled(), template.getStatus(),
                toOffsetTime(template.getCreatedAt()), toOffsetTime(template.getUpdatedAt()));
    }
}
