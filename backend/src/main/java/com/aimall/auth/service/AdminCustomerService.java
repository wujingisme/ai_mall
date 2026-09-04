package com.aimall.auth.service;

import com.aimall.auth.entity.MallUser;
import com.aimall.auth.exception.CustomerManagementConflictException;
import com.aimall.auth.exception.CustomerNotFoundException;
import com.aimall.auth.mapper.MallUserMapper;
import com.aimall.auth.vo.*;
import com.aimall.coupon.service.UserCouponService;
import com.aimall.coupon.vo.UserCouponPageResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.time.*;
import java.util.Arrays;
import java.util.Set;

@Service
/**
 * 后台客户查询服务。
 *
 * <p>“客户”在这里特指带有 CUSTOMER 角色的商城用户。后台账号也存在于同一张
 * {@code mall_user} 表中；支持双角色后，ADMIN,CUSTOMER 账号可以出现在列表中查看，
 * 但启停操作还会额外拒绝后台职责账号，防止误停用管理员。</p>
 */
public class AdminCustomerService {
    private final MallUserMapper mapper;
    private final UserCouponService userCouponService;
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    public AdminCustomerService(MallUserMapper mapper, UserCouponService userCouponService) { this.mapper = mapper; this.userCouponService = userCouponService; }

    /** 发券页面使用的轻量客户搜索，只返回非敏感摘要且只查询启用客户。 */
    public CustomerPageResponse list(int page, int pageSize, String keyword) {
        LambdaQueryWrapper<MallUser> query = new LambdaQueryWrapper<MallUser>()
                .eq(MallUser::getEnabled, true)
                .apply("FIND_IN_SET('CUSTOMER', roles) > 0");
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(group -> group.like(MallUser::getUsername, value).or().like(MallUser::getDisplayName, value));
        }
        query.orderByDesc(MallUser::getId);
        Page<MallUser> result = mapper.selectPage(Page.of(page, pageSize), query);
        return new CustomerPageResponse(result.getRecords().stream()
                .map(user -> new CustomerSummaryResponse(user.getId().toString(), user.getUsername(),
                        user.getDisplayName(), user.getAvatarUrl())).toList(),
                result.getCurrent(), result.getSize(), result.getTotal(), result.getPages());
    }

    /** 后台用户管理列表，可同时查看已停用客户和已开通消费者身份的后台账号。 */
    public AdminUserPageResponse listUsers(int page, int pageSize, String keyword, Boolean enabled) {
        LambdaQueryWrapper<MallUser> query = customerQuery(keyword).eq(enabled != null, MallUser::getEnabled, enabled)
                .orderByDesc(MallUser::getId);
        Page<MallUser> result = mapper.selectPage(Page.of(page, pageSize), query);
        return new AdminUserPageResponse(result.getRecords().stream().map(this::toAdminResponse).toList(),
                result.getCurrent(), result.getSize(), result.getTotal(), result.getPages());
    }

    /** 查询一个客户的公开管理信息，不返回密码、OpenID 等敏感身份字段。 */
    public AdminUserResponse getUser(Long id) { return toAdminResponse(requireCustomer(id)); }

    /**
     * 修改客户启用状态。
     *
     * <p>更新条件中再次限制 CUSTOMER，避免 ID 被替换或数据变化时误修改管理员。</p>
     */
    public AdminUserResponse changeEnabled(Long id, boolean enabled) {
        MallUser user = requireCustomer(id);
        if (hasStaffRole(user.getRoles())) {
            // 双角色账号会出现在客户列表，但停用它会同时阻断后台登录，所以必须拒绝该操作。
            throw new CustomerManagementConflictException();
        }
        if (!Boolean.valueOf(enabled).equals(user.getEnabled())) {
            mapper.update(null, com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaUpdate(MallUser.class)
                    .eq(MallUser::getId, id).apply("FIND_IN_SET('CUSTOMER', roles) > 0")
                    .set(MallUser::getEnabled, enabled));
        }
        return toAdminResponse(requireCustomer(id));
    }

    /** 先验证目标确实是客户，再复用“我的优惠券”服务按该用户查询。 */
    public UserCouponPageResponse listCoupons(Long userId, int page, int pageSize, String status) {
        requireCustomer(userId);
        return userCouponService.list(userId, page, pageSize, status);
    }

    private LambdaQueryWrapper<MallUser> customerQuery(String keyword) {
        // FIND_IN_SET 适配当前逗号分隔角色字段；后续角色表改造时应同步替换这里的查询。
        LambdaQueryWrapper<MallUser> query = new LambdaQueryWrapper<MallUser>().apply("FIND_IN_SET('CUSTOMER', roles) > 0");
        if (StringUtils.hasText(keyword)) { String value = keyword.trim(); query.and(group -> group.like(MallUser::getUsername, value).or().like(MallUser::getDisplayName, value)); }
        return query;
    }
    private MallUser requireCustomer(Long id) {
        // 查询不到或角色不是 CUSTOMER 时统一伪装成“客户不存在”，不泄露后台账号信息。
        MallUser user = mapper.selectById(id);
        if (user == null || !hasCustomerRole(user.getRoles())) throw new CustomerNotFoundException(id);
        return user;
    }
    private boolean hasCustomerRole(String roles) { return roles != null && Arrays.stream(roles.split(",")).map(String::trim).anyMatch("CUSTOMER"::equals); }
    /** 判断是否仍有后台职责；CUSTOMER 与后台角色并存时也不能走客户启停逻辑。 */
    private boolean hasStaffRole(String roles) {
        return roles != null && Arrays.stream(roles.split(",")).map(String::trim)
                .anyMatch(role -> Set.of("SUPER_ADMIN", "ADMIN", "OPERATOR").contains(role));
    }
    private AdminUserResponse toAdminResponse(MallUser user) {
        return new AdminUserResponse(user.getId().toString(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl(),
                Boolean.TRUE.equals(user.getEnabled()), user.getRoles(), StringUtils.hasText(user.getWechatOpenId()), toOffset(user.getCreatedAt()), toOffset(user.getUpdatedAt()));
    }
    private OffsetDateTime toOffset(LocalDateTime value) { return value == null ? null : value.atZone(ZONE).toOffsetDateTime(); }
}
