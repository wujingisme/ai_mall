import { authApi } from '@/api/auth'
import { clearAuthSession, getAccessToken, updateCurrentUser } from '@/utils/auth-storage'

// 除登录和注册外，消费者端页面都要求有效登录状态。
const protectedRoutes = new Set(['/pages/product/list', '/pages/product/detail', '/pages/cart/index', '/pages/profile/index', '/pages/product/form'])

function normalizePath(url: string) {
  const path = url.split('?')[0]
  return path.startsWith('/') ? path : `/${path}`
}

function guard(args: { url?: string }) {
  if (!args.url || !protectedRoutes.has(normalizePath(args.url)) || getAccessToken()) return true
  const redirect = encodeURIComponent(args.url)
  // 阻止未登录导航，并记录目标地址供登录成功后恢复。
  uni.navigateTo({ url: `/pages/auth/login?redirect=${redirect}` })
  return false
}

export function installRouteGuard() {
  ;(['navigateTo', 'redirectTo', 'reLaunch', 'switchTab'] as const).forEach((method) => {
    uni.addInterceptor(method, { invoke: guard })
  })
}

export async function enforceCurrentRoute() {
  const pages = getCurrentPages()
  const route = pages[pages.length - 1]?.route
  if (!route || !protectedRoutes.has(normalizePath(route))) return
  if (!getAccessToken()) return void uni.reLaunch({ url: `/pages/auth/login?redirect=${encodeURIComponent(`/${route}`)}` })
  try {
    // 本地有 token 仍必须向后端确认，过期、伪造或已禁用账号都不能继续停留在受保护页。
    updateCurrentUser(await authApi.me())
  } catch {
    clearAuthSession()
    uni.reLaunch({ url: `/pages/auth/login?redirect=${encodeURIComponent(`/${route}`)}` })
  }
}
