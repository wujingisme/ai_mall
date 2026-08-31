import { request } from '@/utils/request'
import type { CurrentUser, LoginRequest, RefreshTokenRequest, RegisterRequest, TokenResponse, WechatLoginRequest } from '@/types/auth'

const resource = '/api/v1/auth'

export const authApi = {
  // 基础注册暂不依赖短信验证码，只提交用户名、密码和用户昵称。
  register: (data: RegisterRequest) =>
    request<CurrentUser>({ url: `${resource}/register`, method: 'POST', data }),

  // 使用用户名和密码登录；成功后返回用户信息及一组访问、刷新令牌。
  login: (data: LoginRequest) =>
    request<TokenResponse>({ url: `${resource}/login`, method: 'POST', data }),

  wechatLogin: (data: WechatLoginRequest) =>
    request<TokenResponse>({ url: `${resource}/wechat/login`, method: 'POST', data }),

  // 刷新成功后会同时轮换两个令牌，调用方必须完整替换本地旧令牌。
  refresh: (data: RefreshTokenRequest) =>
    request<TokenResponse>({ url: `${resource}/refresh`, method: 'POST', data }),

  // 退出接口是幂等的，即使会话已经失效，重复调用也视为成功。
  logout: (data: RefreshTokenRequest) =>
    request<void>({ url: `${resource}/logout`, method: 'POST', data }),

  // 使用访问令牌查询当前登录用户，用于恢复和校验登录状态。
  me: (options: { redirectOnUnauthorized?: boolean } = {}) =>
    request<CurrentUser>({ url: `${resource}/me`, method: 'GET', ...options }),
}
