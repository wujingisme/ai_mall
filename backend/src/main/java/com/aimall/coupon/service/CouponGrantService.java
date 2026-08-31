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
    public CouponGrantResponse grant(Long operatorUserId, CouponGrantRequest request) {
        CouponGrant existing = grantMapper.selectOne(new LambdaQueryWrapper<CouponGrant>()
                .eq(CouponGrant::getIdempotencyKey, request.idempotencyKey()));
        if (existing != null) {
            ensureSameRequest(existing, operatorUserId, request);
            return toResponse(existing);
        }

        MallUser target = userMapper.selectById(request.targetUserId());
        if (target == null || !Boolean.TRUE.equals(target.getEnabled()) || !hasCustomerRole(target.getRoles())) {
            throw new CouponGrantRuleException("目标用户不存在、已停用或不是商城用户");
        }
        CouponTemplate template = templateMapper.selectById(request.templateId());
        if (template == null) throw new CouponTemplateNotFoundException(request.templateId());

        // 先取得模板行的条件更新锁；同一模板的并发发放随后串行检查用户限领数量。
        if (templateMapper.reserveIssueQuantity(template.getId(), request.quantity()) != 1) {
            throw new CouponGrantConflictException("模板未启用、已经过期或剩余发行量不足");
        }
        long received = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getTemplateId, template.getId()).eq(UserCoupon::getUserId, target.getId()));
        if (received + request.quantity() > template.getPerUserLimit()) {
            throw new CouponGrantConflictException("发放后将超过该用户的每人限领数量");
        }

        CouponGrant grant = new CouponGrant();
        grant.setTemplateId(template.getId()); grant.setTargetUserId(target.getId());
        grant.setOperatorUserId(operatorUserId); grant.setRequestedQuantity(request.quantity());
        grant.setSuccessQuantity(request.quantity()); grant.setReason(request.reason().trim());
        grant.setIdempotencyKey(request.idempotencyKey()); grant.setStatus("SUCCESS");
        try { grantMapper.insert(grant); }
        catch (DuplicateKeyException e) { throw new CouponGrantConflictException("相同幂等键的发放请求正在处理，请稍后查询结果"); }

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
        return roles != null && Arrays.stream(roles.split(",")).map(String::trim).anyMatch("CUSTOMER"::equals);
    }

    private void ensureSameRequest(CouponGrant existing, Long operatorUserId, CouponGrantRequest request) {
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
