package com.aimall.auth.service;

import com.aimall.auth.entity.MallUser;
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

@Service
public class AdminCustomerService {
    private final MallUserMapper mapper;
    private final UserCouponService userCouponService;
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    public AdminCustomerService(MallUserMapper mapper, UserCouponService userCouponService) { this.mapper = mapper; this.userCouponService = userCouponService; }

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

    public AdminUserPageResponse listUsers(int page, int pageSize, String keyword, Boolean enabled) {
        LambdaQueryWrapper<MallUser> query = customerQuery(keyword).eq(enabled != null, MallUser::getEnabled, enabled)
                .orderByDesc(MallUser::getId);
        Page<MallUser> result = mapper.selectPage(Page.of(page, pageSize), query);
        return new AdminUserPageResponse(result.getRecords().stream().map(this::toAdminResponse).toList(),
                result.getCurrent(), result.getSize(), result.getTotal(), result.getPages());
    }

    public AdminUserResponse getUser(Long id) { return toAdminResponse(requireCustomer(id)); }

    public AdminUserResponse changeEnabled(Long id, boolean enabled) {
        MallUser user = requireCustomer(id);
        if (!Boolean.valueOf(enabled).equals(user.getEnabled())) {
            mapper.update(null, com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaUpdate(MallUser.class)
                    .eq(MallUser::getId, id).apply("FIND_IN_SET('CUSTOMER', roles) > 0")
                    .set(MallUser::getEnabled, enabled));
        }
        return toAdminResponse(requireCustomer(id));
    }

    public UserCouponPageResponse listCoupons(Long userId, int page, int pageSize, String status) {
        requireCustomer(userId);
        return userCouponService.list(userId, page, pageSize, status);
    }

    private LambdaQueryWrapper<MallUser> customerQuery(String keyword) {
        LambdaQueryWrapper<MallUser> query = new LambdaQueryWrapper<MallUser>().apply("FIND_IN_SET('CUSTOMER', roles) > 0");
        if (StringUtils.hasText(keyword)) { String value = keyword.trim(); query.and(group -> group.like(MallUser::getUsername, value).or().like(MallUser::getDisplayName, value)); }
        return query;
    }
    private MallUser requireCustomer(Long id) {
        MallUser user = mapper.selectById(id);
        if (user == null || !hasCustomerRole(user.getRoles())) throw new CustomerNotFoundException(id);
        return user;
    }
    private boolean hasCustomerRole(String roles) { return roles != null && Arrays.stream(roles.split(",")).map(String::trim).anyMatch("CUSTOMER"::equals); }
    private AdminUserResponse toAdminResponse(MallUser user) {
        return new AdminUserResponse(user.getId().toString(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl(),
                Boolean.TRUE.equals(user.getEnabled()), user.getRoles(), StringUtils.hasText(user.getWechatOpenId()), toOffset(user.getCreatedAt()), toOffset(user.getUpdatedAt()));
    }
    private OffsetDateTime toOffset(LocalDateTime value) { return value == null ? null : value.atZone(ZONE).toOffsetDateTime(); }
}
