import { request } from '@/utils/request'
import type { ShopProductDetail, ShopProductPage, ShopProductQuery } from '@/types/shop'

const resource = '/api/v1/shop/products'

function queryString(query: ShopProductQuery) {
  const entries = Object.entries(query).filter(([, value]) => value !== undefined && value !== '')
  return entries.length ? `?${entries.map(([key, value]) => `${key}=${encodeURIComponent(String(value))}`).join('&')}` : ''
}

// 消费者页面统一从 shop 命名空间取数据，不复用后台商品管理接口。
export const shopApi = {
  list: (query: ShopProductQuery) => request<ShopProductPage>({ url: resource + queryString(query), method: 'GET' }),
  get: (id: string) => request<ShopProductDetail>({ url: `${resource}/${id}`, method: 'GET' }),
}
