import { request } from '@/utils/request'
import type { OrderDetail, OrderPage, OrderPreview, OrderPreviewRequest, OrderStatus } from '@/types/order'

/**
 * 消费端订单 API 客户端。
 *
 * <p>这里仅描述 HTTP 地址和数据类型，不在前端计算金额、不拼接用户 ID。
 * request() 会统一附加 Bearer Token，并处理会话过期。</p>
 */
export const orderApi = {
  /** 请求后端重新计算购物车商品的最新价格、库存和取货点信息。 */
  preview: (data: OrderPreviewRequest) =>
    request<OrderPreview>({ url: '/api/v1/orders/preview', method: 'POST', data }),

  /** 分页查询当前登录用户的订单；用户归属由后端 JWT 决定。 */
  listMine: (params: { page?: number; pageSize?: number; status?: OrderStatus } = {}) =>
    request<OrderPage>({ url: '/api/v1/me/orders', method: 'GET', data: params }),

  /** 查询当前用户的订单详情；订单 ID 只作为资源定位，权限由后端再次校验。 */
  getMine: (id: string) =>
    request<OrderDetail>({ url: `/api/v1/me/orders/${id}`, method: 'GET' }),
}

