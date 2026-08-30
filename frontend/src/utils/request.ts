import type { ApiError } from '@/types/product'
import type { TokenResponse } from '@/types/auth'
import { clearAuthSession, getAccessToken, getRefreshToken, replaceAuthSession } from '@/utils/auth-storage'

const baseUrl =
  import.meta.env.VITE_API_BASE_URL || (typeof window !== 'undefined' && window.location ? window.location.origin : '')

let refreshPromise: Promise<TokenResponse> | null = null

function refreshSession(): Promise<TokenResponse> {
  if (refreshPromise) return refreshPromise
  const refreshToken = getRefreshToken()
  if (!refreshToken) return Promise.reject(new Error('没有可用的刷新令牌'))

  refreshPromise = new Promise((resolve, reject) => {
    uni.request({
      url: `${baseUrl}/api/v1/auth/refresh`,
      method: 'POST',
      header: { 'Content-Type': 'application/json' },
      data: { refreshToken },
      success: (response) => {
        if (response.statusCode >= 200 && response.statusCode < 300) {
          const session = response.data as TokenResponse
          replaceAuthSession(session)
          resolve(session)
        } else reject(response.data)
      },
      fail: reject,
      complete: () => { refreshPromise = null },
    })
  })
  return refreshPromise
}

function redirectToLogin() {
  clearAuthSession()
  const pages = getCurrentPages()
  if (pages[pages.length - 1]?.route !== 'pages/auth/login') uni.reLaunch({ url: '/pages/auth/login' })
}

export function request<T>(options: UniApp.RequestOptions): Promise<T> {
  return executeRequest<T>(options, false)
}

function executeRequest<T>(options: UniApp.RequestOptions, retried: boolean): Promise<T> {
  return new Promise((resolve, reject) => {
    const accessToken = getAccessToken()
    uni.request({
      ...options,
      url: `${baseUrl}${options.url}`,
      header: {
        'Content-Type': 'application/json',
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
        ...options.header,
      },
      success: (response) => {
        if (response.statusCode >= 200 && response.statusCode < 300) {
          resolve(response.data as T)
          return
        }

        const publicAuthPaths = ['/api/v1/auth/login', '/api/v1/auth/wechat/login', '/api/v1/auth/register', '/api/v1/auth/refresh', '/api/v1/auth/logout']
        const isPublicAuth = publicAuthPaths.some((path) => String(options.url).includes(path))
        // 并发 401 共用一个刷新 Promise，每个原请求最多重放一次，避免刷新风暴和死循环。
        if (response.statusCode === 401 && !retried && !isPublicAuth && getRefreshToken()) {
          refreshSession()
            .then(() => executeRequest<T>(options, true).then(resolve, reject))
            .catch((error) => { redirectToLogin(); reject(error) })
          return
        }

        // 没有刷新令牌或刷新后的请求仍返回 401 时，立即清理伪登录状态并返回登录页。
        if (response.statusCode === 401 && !isPublicAuth) redirectToLogin()

        const error = response.data as ApiError
        uni.showToast({ title: error?.message || '请求失败', icon: 'none' })
        reject(error)
      },
      fail: (error) => {
        uni.showToast({ title: '无法连接服务器', icon: 'none' })
        reject(error)
      },
    })
  })
}
