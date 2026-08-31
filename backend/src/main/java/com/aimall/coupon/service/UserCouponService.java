package com.aimall.coupon.service;

import com.aimall.coupon.entity.UserCoupon;
import com.aimall.coupon.exception.CouponGrantRuleException;
import com.aimall.coupon.exception.UserCouponNotFoundException;
import com.aimall.coupon.mapper.UserCouponMapper;
import com.aimall.coupon.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.*;
import java.util.Set;

@Service
public class UserCouponService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Set<String> FILTER_STATUSES = Set.of("AVAILABLE", "USED", "EXPIRED");
    private final UserCouponMapper mapper;

    public UserCouponService(UserCouponMapper mapper) { this.mapper = mapper; }

    public UserCouponPageResponse list(Long userId, int page, int pageSize, String status) {
        String normalized = normalizeStatus(status);
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        LambdaQueryWrapper<UserCoupon> query = new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getUserId, userId);
        if ("AVAILABLE".equals(normalized)) query.eq(UserCoupon::getStatus, "UNUSED").gt(UserCoupon::getValidUntil, now);
        if ("EXPIRED".equals(normalized)) query.eq(UserCoupon::getStatus, "UNUSED").le(UserCoupon::getValidUntil, now);
        if ("USED".equals(normalized)) query.eq(UserCoupon::getStatus, "USED");
        query.orderByDesc(UserCoupon::getCreatedAt).orderByDesc(UserCoupon::getId);
        Page<UserCoupon> result = mapper.selectPage(Page.of(page, pageSize), query);
        return new UserCouponPageResponse(result.getRecords().stream().map(item -> toResponse(item, now)).toList(),
                result.getCurrent(), result.getSize(), result.getTotal(), result.getPages());
    }

    public UserCouponResponse get(Long userId, Long id) {
        UserCoupon coupon = mapper.selectOne(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getId, id).eq(UserCoupon::getUserId, userId));
        if (coupon == null) throw new UserCouponNotFoundException(id);
        return toResponse(coupon, LocalDateTime.now(BUSINESS_ZONE));
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) return null;
        String normalized = status.trim().toUpperCase();
        if (!FILTER_STATUSES.contains(normalized)) throw new CouponGrantRuleException("优惠券状态筛选不合法");
        return normalized;
    }

    private UserCouponResponse toResponse(UserCoupon coupon, LocalDateTime now) {
        String displayStatus = "UNUSED".equals(coupon.getStatus()) && !coupon.getValidUntil().isAfter(now)
                ? "EXPIRED" : "UNUSED".equals(coupon.getStatus()) ? "AVAILABLE" : coupon.getStatus();
        return new UserCouponResponse(coupon.getId().toString(), coupon.getTemplateId().toString(), coupon.getName(),
                coupon.getCouponType(), coupon.getMinimumSpend().toPlainString(), coupon.getDiscountAmount().toPlainString(),
                toOffset(coupon.getValidFrom()), toOffset(coupon.getValidUntil()), displayStatus,
                coupon.getSource(), toOffset(coupon.getUsedAt()), toOffset(coupon.getCreatedAt()));
    }

    private OffsetDateTime toOffset(LocalDateTime value) {
        return value == null ? null : value.atZone(BUSINESS_ZONE).toOffsetDateTime();
    }
}
