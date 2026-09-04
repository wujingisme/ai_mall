import { clearAuthSession, getAccessToken } from '@/utils/auth-storage'
import { navigateToLogin, normalizePath, ROUTES } from '@/utils/navigation'
import { validateCurrentUser } from '@/utils/session-validation'

// 商品和个人中心允许游客浏览；只有读取用户私有数据的页面才在导航阶段要求登录。
const protectedRoutes = new Set<string>([ROUTES.cart, ROUTES.orderPreview, ROUTES.coupons, ROUTES.couponDetail])

function guard(args: { url?: string }) {
  if (!args.url || !protectedRoutes.has(normalizePath(args.url)) || getAccessToken()) return true
  navigateToLogin(args.url)
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
  if (!getAccessToken()) return void navigateToLogin(`/${route}`)
  try {
    await validateCurrentUser()
  } catch {
    clearAuthSession()
    navigateToLogin(`/${route}`)
  }
}
