package com.aimall.auth.service;

import com.aimall.auth.entity.MallUser;
import com.aimall.auth.exception.CustomerManagementConflictException;
import com.aimall.auth.mapper.MallUserMapper;
import com.aimall.coupon.service.UserCouponService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/** 客户管理边界测试：双角色账号可展示，但不能被客户启停操作禁用。 */
class AdminCustomerServiceTest {
    private final MallUserMapper mapper = mock(MallUserMapper.class);
    private final UserCouponService userCouponService = mock(UserCouponService.class);
    private final AdminCustomerService service = new AdminCustomerService(mapper, userCouponService);

    @Test
    /** ADMIN,CUSTOMER 账号属于后台员工，客户管理入口必须拒绝启用/停用。 */
    void mixedStaffCustomerCannotBeEnabledOrDisabledFromCustomerPage() {
        MallUser user = user(7L, "ADMIN,CUSTOMER");
        when(mapper.selectById(7L)).thenReturn(user);

        assertThrows(CustomerManagementConflictException.class, () -> service.changeEnabled(7L, false));
        verify(mapper, never()).update(any(), any());
    }

    private MallUser user(Long id, String roles) {
        MallUser user = new MallUser();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername("jj");
        user.setDisplayName("管理员");
        user.setRoles(roles);
        user.setEnabled(true);
        return user;
    }
}
