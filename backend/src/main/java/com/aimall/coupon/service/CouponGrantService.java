package com.aimall.coupon.service;

import com.aimall.auth.entity.MallUser;
import com.aimall.auth.mapper.MallUserMapper;
import com.aimall.coupon.dto.CouponGrantRequest;
import com.aimall.coupon.entity.*;
import com.aimall.coupon.exception.*;
import com.aimall.coupon.mapper.*;
import com.aimall.coupon.vo.CouponGrantResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.Arrays;

@Service
/**
 * 人工发券服务。
 *
 * <p>一次发放同时产生审计记录和一张或多张用户优惠券。模板库存条件更新、幂等键、
 * 用户限领和事务共同保证“不会重复发、不会超发、失败会整体回滚”。</p>
 */
public class CouponGrantService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final CouponTemplateMapper templateMapper;
    private final CouponGrantMapper grantMapper;
    private final UserCouponMapper userCouponMapper;
    private final MallUserMapper userMapper;

    public CouponGrantService(CouponTemplateMapper templateMapper, CouponGrantMapper grantMapper,
            UserCouponMapper userCouponMapper, MallUserMapper userMapper) {
        this.templateMapper = templateMapper;
        this.grantMapper = grantMapper;
        this.userCouponMapper = userCouponMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    /**
     * 给一个 CUSTOMER 用户发放指定数量的优惠券。
     *
     * <p>相同幂等键的重复请求会返回第一次结果；如果请求参数不同则报冲突，
     * 防止调用方误把同一个键当成新的业务请求。</p>
     */
    public CouponGrantResponse grant(Long operatorUserId, CouponGrantRequest request) {
        // 阶段 1：先查幂等记录。网络超时后前端可能重试，这一步让重试读取第一次结果而不是再次发券。
        CouponGrant existing = grantMapper.selectOne(new LambdaQueryWrapper<CouponGrant>()
                .eq(CouponGrant::getIdempotencyKey, request.idempotencyKey()));
        if (existing != null) {
            ensureSameRequest(existing, operatorUserId, request);
            return toResponse(existing);
        }

        // 阶段 2：确认收券人是启用中的 CUSTOMER，防止给管理员或不存在用户发券。
        MallUser target = userMapper.selectById(request.targetUserId());
        if (target == null || !Boolean.TRUE.equals(target.getEnabled()) || !hasCustomerRole(target.getRoles())) {
            throw new CouponGrantRuleException("目标用户不存在、已停用或不是商城用户");
        }
        // 阶段 3：读取模板规则；真正的库存保留在下一条条件更新中完成。
        CouponTemplate template = templateMapper.selectById(request.templateId());
        if (template == null) throw new CouponTemplateNotFoundException(request.templateId());

        // 先取得模板行的条件更新锁；同一模板的并发发放随后串行检查用户限领数量。
        if (templateMapper.reserveIssueQuantity(template.getId(), request.quantity()) != 1) {
            throw new CouponGrantConflictException("模板未启用、已经过期或剩余发行量不足");
        }
        // 阶段 4：库存更新成功后再检查个人限领；异常会使事务回滚库存预留。
        long received = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getTemplateId, template.getId()).eq(UserCoupon::getUserId, target.getId()));
        if (received + request.quantity() > template.getPerUserLimit()) {
            throw new CouponGrantConflictException("这个用户已经达到该优惠券的领取上限，暂时不能继续发放");
        }

        // 阶段 5：写入不可变的审计记录，记录操作人和原始请求，方便追查后台操作。
        CouponGrant grant = new CouponGrant();
        grant.setTemplateId(template.getId()); grant.setTargetUserId(target.getId());
        grant.setOperatorUserId(operatorUserId); grant.setRequestedQuantity(request.quantity());
        grant.setSuccessQuantity(request.quantity()); grant.setReason(request.reason().trim());
        grant.setIdempotencyKey(request.idempotencyKey()); grant.setStatus("SUCCESS");
        try { grantMapper.insert(grant); }
        catch (DuplicateKeyException e) { throw new CouponGrantConflictException("相同幂等键的发放请求正在处理，请稍后查询结果"); }

        // 阶段 6：把模板规则复制到每张用户券，形成发放时快照。
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        LocalDateTime validFrom = "FIXED_RANGE".equals(template.getValidityType()) ? template.getValidFrom() : now;
        LocalDateTime validUntil = "FIXED_RANGE".equals(template.getValidityType())
                ? template.getValidUntil() : now.plusDays(template.getValidDays());
        for (int i = 0; i < request.quantity(); i++) {
            UserCoupon coupon = new UserCoupon();
            coupon.setUserId(target.getId()); coupon.setTemplateId(template.getId()); coupon.setGrantId(grant.getId());
            coupon.setSource("MANUAL"); coupon.setName(template.getName()); coupon.setCouponType(template.getCouponType());
            coupon.setMinimumSpend(template.getMinimumSpend()); coupon.setDiscountAmount(template.getDiscountAmount());
            coupon.setValidFrom(validFrom); coupon.setValidUntil(validUntil); coupon.setStatus("UNUSED");
            userCouponMapper.insert(coupon);
        }
        return toResponse(grantMapper.selectById(grant.getId()));
    }

    private boolean hasCustomerRole(String roles) {
        // 目标必须是 CUSTOMER；后台账号不能成为人工发券的消费者目标。
        return roles != null && Arrays.stream(roles.split(",")).map(String::trim).anyMatch("CUSTOMER"::equals);
    }

    private void ensureSameRequest(CouponGrant existing, Long operatorUserId, CouponGrantRequest request) {
        // 幂等键不只是“存在即返回”：还必须确认关键参数完全一致，避免键误用掩盖业务错误。
        if (!existing.getTemplateId().equals(request.templateId())
                || !existing.getTargetUserId().equals(request.targetUserId())
                || !existing.getOperatorUserId().equals(operatorUserId)
                || !existing.getRequestedQuantity().equals(request.quantity())) {
            throw new CouponGrantConflictException("幂等键已被另一项发放请求使用");
        }
    }

    private CouponGrantResponse toResponse(CouponGrant grant) {
        return new CouponGrantResponse(grant.getId().toString(), grant.getTemplateId().toString(),
                grant.getTargetUserId().toString(), grant.getOperatorUserId().toString(),
                grant.getRequestedQuantity(), grant.getSuccessQuantity(), grant.getReason(),
                grant.getIdempotencyKey(), grant.getStatus(), toOffset(grant.getCreatedAt()));
    }

    private OffsetDateTime toOffset(LocalDateTime value) {
        return value == null ? null : value.atZone(BUSINESS_ZONE).toOffsetDateTime();
    }
}
