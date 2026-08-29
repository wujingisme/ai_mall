import { request } from '@/utils/request'
import type { Cart } from '@/types/cart'

export const cartApi = {
  get: () => request<Cart>({ url: '/api/v1/cart', method: 'GET' }),
  add: (data: { productId: string; quantity: number }) =>
    request<Cart>({ url: '/api/v1/cart/items', method: 'POST', data }),
  update: (productId: string, quantity: number) =>
    request<Cart>({ url: `/api/v1/cart/items/${productId}`, method: 'PUT', data: { quantity } }),
  remove: (productId: string) =>
    request<Cart>({ url: `/api/v1/cart/items/${productId}`, method: 'DELETE' }),
  clear: () => request<Cart>({ url: '/api/v1/cart/items', method: 'DELETE' }),
}
