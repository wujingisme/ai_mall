package com.aimall.coupon.service;

import com.aimall.coupon.dto.CouponShareCreateRequest;
import com.aimall.coupon.dto.CouponTokenRequest;
import com.aimall.coupon.entity.CouponClaim;
import com.aimall.coupon.entity.CouponShare;
import com.aimall.coupon.entity.CouponTemplate;
import com.aimall.coupon.entity.UserCoupon;
import com.aimall.coupon.exception.CouponShareException;
import com.aimall.coupon.mapper.CouponClaimMapper;
import com.aimall.coupon.mapper.CouponShareMapper;
import com.aimall.coupon.mapper.CouponTemplateMapper;
import com.aimall.coupon.mapper.UserCouponMapper;
import com.aimall.coupon.vo.CouponShareCreateResponse;
import com.aimall.coupon.vo.CouponShareResolveResponse;
import com.aimall.coupon.vo.UserCouponResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.*;
import java.util.Base64;

@Service
public class CouponShareService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private final SecureRandom random = new SecureRandom();
    private final CouponShareMapper shareMapper;
    private final CouponClaimMapper claimMapper;
    private final UserCouponMapper couponMapper;
    private final CouponTemplateMapper templateMapper;
    private final UserCouponService couponService;

    public CouponShareService(CouponShareMapper shareMapper, CouponClaimMapper claimMapper,
            UserCouponMapper couponMapper, CouponTemplateMapper templateMapper, UserCouponService couponService) {
        this.shareMapper = shareMapper; this.claimMapper = claimMapper; this.couponMapper = couponMapper;
        this.templateMapper = templateMapper; this.couponService = couponService;
    }

    @Transactional
    public CouponShareCreateResponse create(Long userId, CouponShareCreateRequest request) {
        LocalDateTime now = LocalDateTime.now(ZONE);
        UserCoupon owned = couponMapper.selectOne(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getId, request.userCouponId()).eq(UserCoupon::getUserId, userId));
        if (owned == null || !"UNUSED".equals(owned.getStatus()) || !owned.getValidUntil().isAfter(now))
            throw new CouponShareException("只能分享自己当前可用的优惠券", false);
        CouponTemplate template = templateMapper.selectById(owned.getTemplateId());
        if (template == null || !Boolean.TRUE.equals(template.getShareEnabled()) || !"ACTIVE".equals(template.getStatus()))
            throw new CouponShareException("该优惠券当前不允许分享领取", false);
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        CouponShare share = new CouponShare();
        share.setTemplateId(template.getId()); share.setCreatorUserId(userId); share.setCreatorUserCouponId(owned.getId());
        share.setTokenHash(hash(token)); share.setMaxClaims(1); share.setClaimedCount(0); share.setStatus("ACTIVE");
        share.setExpiresAt(owned.getValidUntil().isBefore(now.plusDays(7)) ? owned.getValidUntil() : now.plusDays(7));
        shareMapper.insert(share);
        return new CouponShareCreateResponse(token, "/pages/coupon/claim?token=" + token, toOffset(share.getExpiresAt()));
    }

    public CouponShareResolveResponse resolve(CouponTokenRequest request) {
        CouponShare share = find(request.shareToken());
        CouponTemplate template = templateMapper.selectById(share.getTemplateId());
        if (template == null) throw new CouponShareException("分享不存在或已失效", true);
        LocalDateTime now = LocalDateTime.now(ZONE);
        boolean claimable = "ACTIVE".equals(share.getStatus()) && share.getExpiresAt().isAfter(now)
                && share.getClaimedCount() < share.getMaxClaims() && "ACTIVE".equals(template.getStatus());
        return new CouponShareResolveResponse(template.getName(), template.getMinimumSpend().toPlainString(),
                template.getDiscountAmount().toPlainString(), toOffset(share.getExpiresAt()), claimable);
    }

    @Transactional
    public UserCouponResponse claim(Long userId, CouponTokenRequest request) {
        CouponShare share = find(request.shareToken());
        if (share.getCreatorUserId().equals(userId)) throw new CouponShareException("不能领取自己分享的优惠券", false);
        templateMapper.selectByIdForUpdate(share.getTemplateId());
        CouponClaim existing = claimMapper.selectOne(new LambdaQueryWrapper<CouponClaim>()
                .eq(CouponClaim::getShareId, share.getId()).eq(CouponClaim::getClaimantUserId, userId));
        if (existing != null) return couponService.response(couponMapper.selectById(existing.getUserCouponId()));
        CouponTemplate template = templateMapper.selectById(share.getTemplateId());
        if (template == null || !Boolean.TRUE.equals(template.getShareEnabled()) || !"ACTIVE".equals(template.getStatus()))
            throw new CouponShareException("该优惠券当前不允许分享领取", false);
        if (templateMapper.reserveIssueQuantity(template.getId(), 1) != 1)
            throw new CouponShareException("优惠券已过期、停用或已经领完", false);
        long received = couponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getTemplateId, template.getId()).eq(UserCoupon::getUserId, userId));
        if (received + 1 > template.getPerUserLimit()) throw new CouponShareException("你已达到该优惠券的领取上限", false);
        if (shareMapper.consumeClaim(share.getId()) != 1) throw new CouponShareException("分享已失效或已被领取", false);
        LocalDateTime now = LocalDateTime.now(ZONE);
        UserCoupon coupon = new UserCoupon(); coupon.setUserId(userId); coupon.setTemplateId(template.getId());
        coupon.setShareId(share.getId()); coupon.setSource("SHARE"); coupon.setName(template.getName());
        coupon.setCouponType(template.getCouponType()); coupon.setMinimumSpend(template.getMinimumSpend()); coupon.setDiscountAmount(template.getDiscountAmount());
        coupon.setValidFrom("FIXED_RANGE".equals(template.getValidityType()) ? template.getValidFrom() : now);
        coupon.setValidUntil("FIXED_RANGE".equals(template.getValidityType()) ? template.getValidUntil() : now.plusDays(template.getValidDays()));
        coupon.setStatus("UNUSED"); couponMapper.insert(coupon);
        CouponClaim claim = new CouponClaim(); claim.setShareId(share.getId()); claim.setClaimantUserId(userId); claim.setUserCouponId(coupon.getId()); claimMapper.insert(claim);
        return couponService.response(coupon);
    }

    private CouponShare find(String token) {
        CouponShare share = shareMapper.selectOne(new LambdaQueryWrapper<CouponShare>().eq(CouponShare::getTokenHash, hash(token)));
        if (share == null) throw new CouponShareException("分享不存在或已失效", true); return share;
    }
    private String hash(String value) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 不可用", e); }
    }
    private OffsetDateTime toOffset(LocalDateTime value) { return value.atZone(ZONE).toOffsetDateTime(); }
}
