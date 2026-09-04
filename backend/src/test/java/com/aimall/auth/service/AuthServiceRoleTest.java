package com.aimall.auth.service;

import com.aimall.auth.entity.AuthSession;
import com.aimall.auth.entity.MallUser;
import com.aimall.auth.mapper.AuthSessionMapper;
import com.aimall.auth.mapper.MallUserMapper;
import com.aimall.auth.vo.CurrentUserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/** 后台账号开通消费者身份的 Service 单元测试，验证角色追加、幂等和失败回滚边界。 */
class AuthServiceRoleTest {
    private final MallUserMapper userMapper = mock(MallUserMapper.class);
    private final AuthSessionMapper sessionMapper = mock(AuthSessionMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final WechatMiniProgramClient wechatClient = mock(WechatMiniProgramClient.class);
    private AuthService service;

    @BeforeEach
    /** 角色测试不需要真实密码、JWT 或微信客户端，只保留 AuthService 的依赖形状。 */
    void setUp() {
        reset(userMapper, sessionMapper, passwordEncoder, jwtService, wechatClient);
        service = new AuthService(userMapper, sessionMapper, passwordEncoder, jwtService, wechatClient,
                900, 604800, 5, 900);
    }

    @Test
    /** ADMIN 账号首次开通后应保留原后台角色并追加 CUSTOMER。 */
    void enableCustomerRoleAppendsWithoutRemovingAdminRole() {
        MallUser admin = user(7L, "ADMIN");
        when(userMapper.selectForUpdateById(7L)).thenReturn(admin);
        when(userMapper.updateRoles(7L, "ADMIN,CUSTOMER")).thenReturn(1);

        CurrentUserResponse response = service.enableCustomerRole(7L);

        assertEquals("7", response.id());
        assertEquals(java.util.List.of("ADMIN", "CUSTOMER"), response.roles());
        verify(userMapper).updateRoles(7L, "ADMIN,CUSTOMER");
    }

    @Test
    /** 重复点击已经双角色的账号不应重复写入 CUSTOMER。 */
    void enableCustomerRoleIsIdempotent() {
        MallUser user = user(7L, "ADMIN,CUSTOMER");
        when(userMapper.selectForUpdateById(7L)).thenReturn(user);

        CurrentUserResponse response = service.enableCustomerRole(7L);

        assertEquals(java.util.List.of("ADMIN", "CUSTOMER"), response.roles());
        verify(userMapper, never()).updateRoles(anyLong(), anyString());
    }

    @Test
    /** 数据库更新失败时不能返回成功，事务边界应让调用方感知异常。 */
    void enableCustomerRoleFailsWhenDatabaseUpdateDoesNotAffectOneRow() {
        MallUser admin = user(7L, "ADMIN");
        when(userMapper.selectForUpdateById(7L)).thenReturn(admin);
        when(userMapper.updateRoles(7L, "ADMIN,CUSTOMER")).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> service.enableCustomerRole(7L));
        assertEquals("ADMIN", admin.getRoles());
    }

    private MallUser user(Long id, String roles) {
        MallUser user = new MallUser();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername("admin_" + id);
        user.setDisplayName("后台账号");
        user.setRoles(roles);
        user.setEnabled(true);
        return user;
    }
}
