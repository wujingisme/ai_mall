import { authApi } from '@/api/auth'
import type { CurrentUser } from '@/types/auth'
import { clearAuthSession, getCurrentUser, updateCurrentUser } from '@/utils/auth-storage'

const DEFAULT_MAX_AGE_MS = 60_000
let lastValidatedAt = 0
let validationPromise: Promise<CurrentUser> | null = null

export function markSessionValidated() {
  lastValidatedAt = Date.now()
}

export function resetSessionValidation() {
  lastValidatedAt = 0
  validationPromise = null
}

export function validateCurrentUser(options: { force?: boolean; maxAgeMs?: number } = {}): Promise<CurrentUser> {
  const cachedUser = getCurrentUser()
  if (!cachedUser) return Promise.reject(new Error('当前没有登录会话'))

  const maxAgeMs = options.maxAgeMs ?? DEFAULT_MAX_AGE_MS
  if (!options.force && lastValidatedAt > 0 && Date.now() - lastValidatedAt < maxAgeMs) {
    return Promise.resolve(cachedUser)
  }
  if (validationPromise) return validationPromise

  // 多个页面同时恢复会话时共用同一请求；后台校验失败只清理会话，是否跳转由页面/路由守卫决定。
  validationPromise = authApi.me({ redirectOnUnauthorized: false })
    .then((user) => {
      updateCurrentUser(user)
      markSessionValidated()
      return user
    })
    .catch((error) => {
      clearAuthSession()
      lastValidatedAt = 0
      throw error
    })
    .finally(() => { validationPromise = null })

  return validationPromise
}
