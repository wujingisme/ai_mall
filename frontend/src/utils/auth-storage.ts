import type { CurrentUser, TokenResponse } from '@/types/auth'

const AUTH_STORAGE_KEY = 'mall_auth_session'

export interface AuthSession extends TokenResponse {}

let runtimeSession: AuthSession | null = null
let runtimeRemember = false

// 登录状态同时保存在运行时内存中；勾选“记住登录状态”时再持久化到本地。
export function saveAuthSession(session: AuthSession, remember: boolean) {
  runtimeSession = session
  runtimeRemember = remember
  if (remember) {
    uni.setStorageSync(AUTH_STORAGE_KEY, session)
  } else {
    uni.removeStorageSync(AUTH_STORAGE_KEY)
  }
}

// 页面重新显示或应用重启时，统一从同一个入口恢复登录状态。
export function getAuthSession(): AuthSession | null {
  if (runtimeSession) return runtimeSession
  const stored = uni.getStorageSync(AUTH_STORAGE_KEY) as AuthSession | ''
  runtimeSession = stored || null
  runtimeRemember = Boolean(stored)
  return runtimeSession
}

export function getCurrentUser(): CurrentUser | null {
  return getAuthSession()?.user ?? null
}

export function getAccessToken(): string {
  return getAuthSession()?.accessToken ?? ''
}

export function getRefreshToken(): string {
  return getAuthSession()?.refreshToken ?? ''
}

export function replaceAuthSession(session: AuthSession) {
  saveAuthSession(session, runtimeRemember)
}

export function updateCurrentUser(user: CurrentUser) {
  const session = getAuthSession()
  if (session) saveAuthSession({ ...session, user }, runtimeRemember)
}

export function clearAuthSession() {
  runtimeSession = null
  runtimeRemember = false
  uni.removeStorageSync(AUTH_STORAGE_KEY)
}
