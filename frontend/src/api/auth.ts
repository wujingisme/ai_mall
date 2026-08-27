import { request } from '@/utils/request'
import type { CurrentUser, LoginRequest, RefreshTokenRequest, TokenResponse } from '@/types/auth'

const resource = '/api/v1/auth'

export const authApi = {
  // 使用用户名和密码登录；成功后返回用户信息及一组访问、刷新令牌。
  login: (data: LoginRequest) =>
    request<TokenResponse>({ url: `${resource}/login`, method: 'POST', data }),

  // 刷新成功后会同时轮换两个令牌，调用方必须完整替换本地旧令牌。
  refresh: (data: RefreshTokenRequest) =>
    request<TokenResponse>({ url: `${resource}/refresh`, method: 'POST', data }),

  // 退出接口是幂等的，即使会话已经失效，重复调用也视为成功。
  logout: (data: RefreshTokenRequest) =>
    request<void>({ url: `${resource}/logout`, method: 'POST', data }),

  // 使用访问令牌查询当前登录用户，用于恢复和校验登录状态。
  me: () =>
    request<CurrentUser>({ url: `${resource}/me`, method: 'GET' }),
}