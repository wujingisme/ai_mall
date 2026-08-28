import { getAccessToken } from '@/utils/auth-storage'

const protectedRoutes = new Set(['/pages/cart/index', '/pages/profile/index', '/pages/product/form'])

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

export function enforceCurrentRoute() {
  const pages = getCurrentPages()
  const route = pages[pages.length - 1]?.route
  if (route && protectedRoutes.has(normalizePath(route)) && !getAccessToken()) {
    uni.reLaunch({ url: `/pages/auth/login?redirect=${encodeURIComponent(`/${route}`)}` })
  }
}
