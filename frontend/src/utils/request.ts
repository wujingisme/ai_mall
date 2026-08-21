import type { ApiError } from '@/types/product'

// 正式环境留空时，H5 用浏览器当前域名（同源走相对路径，由部署服务器或后端 CORS 处理）
const baseUrl =
  import.meta.env.VITE_API_BASE_URL ||
  (typeof window !== 'undefined' && window.location ? window.location.origin : '')

export function request<T>(options: UniApp.RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    uni.request({
      ...options,
      url: `${baseUrl}${options.url}`,
      header: { 'Content-Type': 'application/json', ...options.header },
      success: (response) => {
        if (response.statusCode >= 200 && response.statusCode < 300) {
          resolve(response.data as T)
          return
        }
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
