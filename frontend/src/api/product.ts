import { request } from '@/utils/request'
import type { ProductDetail, ProductPage, ProductQuery, ProductWriteRequest } from '@/types/product'

const resource = '/api/v1/products'

function queryString(query: ProductQuery) {
  const entries = Object.entries(query).filter(([, value]) => value !== undefined && value !== '')
  return entries.length ? `?${entries.map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`).join('&')}` : ''
}

export const productApi = {
  list: (query: ProductQuery) => request<ProductPage>({ url: resource + queryString(query), method: 'GET' }),
  get: (id: string) => request<ProductDetail>({ url: `${resource}/${id}`, method: 'GET' }),
  create: (data: ProductWriteRequest) => request<ProductDetail>({ url: resource, method: 'POST', data }),
  update: (id: string, data: ProductWriteRequest) =>
    request<ProductDetail>({ url: `${resource}/${id}`, method: 'PUT', data }),
  remove: (id: string) => request<void>({ url: `${resource}/${id}`, method: 'DELETE' }),
}
