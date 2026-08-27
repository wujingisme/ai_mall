import { request } from '@/utils/request'
import type { ProductDetail, ProductPage, ProductQuery, ProductWriteRequest } from '@/types/product'

const resource = '/api/v1/products'

// 把查询对象转换成 URL 参数，并过滤空值，避免生成多余的参数。
function queryString(query: ProductQuery) {
  const entries = Object.entries(query).filter(([, value]) => value !== undefined && value !== '')
  return entries.length ? `?${entries.map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`).join('&')}` : ''
}

export const productApi = {
  // 分页查询商品列表。
  list: (query: ProductQuery) => request<ProductPage>({ url: resource + queryString(query), method: 'GET' }),
  // 查询单个商品详情。
  get: (id: string) => request<ProductDetail>({ url: `${resource}/${id}`, method: 'GET' }),
  // 新增商品。
  create: (data: ProductWriteRequest) => request<ProductDetail>({ url: resource, method: 'POST', data }),
  // 修改商品。
  update: (id: string, data: ProductWriteRequest) =>
    request<ProductDetail>({ url: `${resource}/${id}`, method: 'PUT', data }),
  // 删除商品。
  remove: (id: string) => request<void>({ url: `${resource}/${id}`, method: 'DELETE' }),
}
