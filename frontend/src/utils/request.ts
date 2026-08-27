import type { ApiError } from '@/types/product'

// 统一接口前缀：开发环境通常为空，生产环境可配置线上服务器地址。
// 为空时，H5 会使用当前页面域名，并交给 Nginx 的 /api/ 代理转发。
const baseUrl =
  import.meta.env.VITE_API_BASE_URL || (typeof window !== 'undefined' && window.location ? window.location.origin : '')

export function request<T>(options: UniApp.RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    uni.request({
      ...options,
      // 在调用方传入的相对路径前拼接接口前缀，并统一设置 JSON 请求头。
      url: `${baseUrl}${options.url}`,
      header: { 'Content-Type': 'application/json', ...options.header },
      success: (response) => {
        // 2xx 表示请求成功，泛型 T 对应调用方期望的数据类型。
        if (response.statusCode >= 200 && response.statusCode < 300) {
          resolve(response.data as T)
          return
        }
        // 非 2xx 时按后端统一错误结构处理，并提示用户。
        const error = response.data as ApiError
        uni.showToast({ title: error?.message || '请求失败', icon: 'none' })
        reject(error)
      },
      fail: (error) => {
        // 网络不可达、超时等情况会进入 fail，而不是 success。
        uni.showToast({ title: '无法连接服务器', icon: 'none' })
        reject(error)
      },
    })
  })
}
