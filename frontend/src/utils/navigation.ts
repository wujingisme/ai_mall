export const ROUTES = {
  home: '/pages/product/list',
  login: '/pages/auth/login',
  register: '/pages/auth/register',
  productDetail: '/pages/product/detail',
  cart: '/pages/cart/index',
  profile: '/pages/profile/index',
  coupons: '/pages/coupon/list',
  couponDetail: '/pages/coupon/detail',
  couponClaim: '/pages/coupon/claim',
} as const

const tabRoutes = new Set<string>([ROUTES.home, ROUTES.cart, ROUTES.profile])

export function normalizePath(url: string): string {
  const path = url.split('?')[0]
  return path.startsWith('/') ? path : `/${path}`
}

export function safeInternalRoute(candidate?: string): string | null {
  if (!candidate || !candidate.startsWith('/') || candidate.startsWith('//') || candidate.includes('\\')) return null
  return candidate
}

export function navigateAfterLogin(redirect?: string) {
  const target = safeInternalRoute(redirect) || ROUTES.home
  const pathname = normalizePath(target)
  // 微信小程序的 switchTab 不支持查询参数；Tab 页面统一按页面路径切换。
  if (tabRoutes.has(pathname)) return uni.switchTab({ url: pathname })
  return uni.redirectTo({ url: target })
}

export function navigateToLogin(redirect?: string) {
  const target = safeInternalRoute(redirect)
  const query = target ? `?redirect=${encodeURIComponent(target)}` : ''
  return uni.navigateTo({ url: `${ROUTES.login}${query}` })
}
