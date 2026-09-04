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
/**
 * 优惠券分享和领取服务。
 *
 * <p>分享链接只携带随机 token，数据库只保存 token 的 SHA-256 摘要；公开解析只能展示预览，
 * 真正领取必须登录并经过本人保护、模板状态、库存、有效期、限领和唯一领取记录检查。</p>
 */
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
    /**
     * 为当前用户的一张可用优惠券创建分享凭证。
     * 分享有效期取“原券有效期”和“当前时间+7 天”中较早者，避免分享链接比原券更长寿。
     */
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
        // 解析是公开预览，不会修改库存或领取次数；claimable 只代表当前状态看起来可领取。
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
    /**
     * 登录用户领取分享券。
     *
     * <p>先锁模板行，再检查用户限领和模板库存，最后条件递增分享领取次数并写入用户券与领取审计。
     * 事务失败时这些写入一起回滚。</p>
     */
    public UserCouponResponse claim(Long userId, CouponTokenRequest request) {
        // 1. token 找分享；分享人本人不能领取自己的链接。
        CouponShare share = find(request.shareToken());
        if (share.getCreatorUserId().equals(userId)) throw new CouponShareException("不能领取自己分享的优惠券", false);
        // 2. 锁模板行，让同一模板的库存/限领判断在并发领取中按顺序进行。
        templateMapper.selectByIdForUpdate(share.getTemplateId());
        // 3. 先查领取审计，实现同一用户重复请求的幂等返回。
        CouponClaim existing = claimMapper.selectOne(new LambdaQueryWrapper<CouponClaim>()
                .eq(CouponClaim::getShareId, share.getId()).eq(CouponClaim::getClaimantUserId, userId));
        if (existing != null) return couponService.response(couponMapper.selectById(existing.getUserCouponId()));
        // 4. 重新读取模板，确认分享创建后模板仍允许分享且处于 ACTIVE。
        CouponTemplate template = templateMapper.selectById(share.getTemplateId());
        if (template == null || !Boolean.TRUE.equals(template.getShareEnabled()) || !"ACTIVE".equals(template.getStatus()))
            throw new CouponShareException("该优惠券当前不允许分享领取", false);
        // 5. 原子预占一张模板库存；SQL 返回 0 就代表过期、停用或已领完。
        if (templateMapper.reserveIssueQuantity(template.getId(), 1) != 1)
            throw new CouponShareException("优惠券已过期、停用或已经领完", false);
        // 6. 检查领取人对该模板的总拥有量，避免通过多个分享绕过 perUserLimit。
        long received = couponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getTemplateId, template.getId()).eq(UserCoupon::getUserId, userId));
        if (received + 1 > template.getPerUserLimit()) throw new CouponShareException("你已达到该优惠券的领取上限", false);
        // 7. 原子消耗分享名额；这是“同一分享只能被首个请求领取”的最后闸门。
        if (shareMapper.consumeClaim(share.getId()) != 1) throw new CouponShareException("分享已失效或已被领取", false);
        LocalDateTime now = LocalDateTime.now(ZONE);
        // 8. 写入用户券快照和领取审计；任一步失败都会回滚前面的库存变化。
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
        // token 本身不落库；即使数据库被读取，也只能得到不可直接使用的摘要。
        CouponShare share = shareMapper.selectOne(new LambdaQueryWrapper<CouponShare>().eq(CouponShare::getTokenHash, hash(token)));
        if (share == null) throw new CouponShareException("分享不存在或已失效", true); return share;
    }
    private String hash(String value) {
        // SHA-256 只用于查找和去重，不承担登录令牌那样的可逆职责。
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 不可用", e); }
    }
    private OffsetDateTime toOffset(LocalDateTime value) { return value.atZone(ZONE).toOffsetDateTime(); }
}
