package com.aimall.auth.service;

import com.aimall.auth.entity.AuthSession;
import com.aimall.auth.entity.MallUser;
import com.aimall.auth.exception.AccountDisabledException;
import com.aimall.auth.mapper.AuthSessionMapper;
import com.aimall.auth.mapper.MallUserMapper;
import com.aimall.auth.vo.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 微信登录 Service 单元测试：隔离微信客户端和 Mapper，验证用户复用与账号状态。 */
class AuthServiceWechatTest {
    private final MallUserMapper userMapper = mock(MallUserMapper.class);
    private final AuthSessionMapper sessionMapper = mock(AuthSessionMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final WechatMiniProgramClient wechatClient = mock(WechatMiniProgramClient.class);
    private AuthService service;

    @BeforeEach
    /** 每个用例重置 Mock，避免前一个登录场景的调用记录污染后一个场景。 */
    void setUp() {
        reset(userMapper, sessionMapper, passwordEncoder, jwtService, wechatClient);
        service = new AuthService(userMapper, sessionMapper, passwordEncoder, jwtService, wechatClient,
                900, 604800, 5, 900);
        when(jwtService.createAccessToken(any())).thenReturn("jwt");
    }

    @Test
    /** 首次 OpenID 登录创建 CUSTOMER 用户并建立会话。 */
    void firstWechatLoginCreatesCustomerAndSession() {
        when(wechatClient.exchangeCode("new-code"))
                .thenReturn(new WechatMiniProgramClient.WechatIdentity("open-id", "union-id"));
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode(any())).thenReturn("encoded-random-password");
        doAnswer(invocation -> {
            MallUser user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 42L);
            return 1;
        }).when(userMapper).insert(any(MallUser.class));

        TokenResponse response = service.loginWithWechat("new-code");

        ArgumentCaptor<MallUser> userCaptor = ArgumentCaptor.forClass(MallUser.class);
        verify(userMapper).insert(userCaptor.capture());
        MallUser created = userCaptor.getValue();
        assertEquals("wx_open-id", created.getUsername());
        assertEquals("open-id", created.getWechatOpenId());
        assertEquals("union-id", created.getWechatUnionId());
        assertEquals("CUSTOMER", created.getRoles());
        assertTrue(created.getEnabled());
        assertEquals("42", response.user().id());
        assertEquals("jwt", response.accessToken());
        verify(sessionMapper).insert(any(AuthSession.class));
    }

    @Test
    /** 已存在的 OpenID 复用原用户，不重复插入用户记录。 */
    void returningWechatUserIsReusedWithoutCreatingAnotherUser() {
        MallUser existing = wechatUser(7L, true);
        when(wechatClient.exchangeCode("returning-code"))
                .thenReturn(new WechatMiniProgramClient.WechatIdentity("open-id", null));
        when(userMapper.selectOne(any())).thenReturn(existing);

        TokenResponse response = service.loginWithWechat("returning-code");

        assertEquals("7", response.user().id());
        verify(userMapper, never()).insert(any(MallUser.class));
        verify(sessionMapper).insert(any(AuthSession.class));
    }

    @Test
    /** 已停用微信用户不能创建新的登录会话。 */
    void disabledWechatUserCannotCreateSession() {
        when(wechatClient.exchangeCode("disabled-code"))
                .thenReturn(new WechatMiniProgramClient.WechatIdentity("open-id", null));
        when(userMapper.selectOne(any())).thenReturn(wechatUser(8L, false));

        assertThrows(AccountDisabledException.class, () -> service.loginWithWechat("disabled-code"));
        verifyNoInteractions(sessionMapper);
    }

    private MallUser wechatUser(long id, boolean enabled) {
        // 构造与数据库中微信用户相同的最小实体，避免测试依赖真实 MySQL。
        MallUser user = new MallUser();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername("wx_open-id");
        user.setDisplayName("微信用户");
        user.setWechatOpenId("open-id");
        user.setRoles("CUSTOMER");
        user.setEnabled(enabled);
        user.setFailedLoginAttempts(0);
        return user;
    }
}
