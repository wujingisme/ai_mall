/**
 * 订单状态只保留当前后端已经定义的三个值。
 *
 * <p>第一版线下取货：下单后等待取货，工作人员确认取货后完成；
 * 暂时没有线上支付状态，也没有物流状态。</p>
 */
export type OrderStatus = 'PENDING_PICKUP' | 'PICKED_UP' | 'CANCELLED'

/** 预览请求中的一行商品；金额、名称和库存由后端计算，不从前端提交。 */
export interface OrderPreviewItemRequest {
  productId: string
  quantity: number
}

/** 订单预览请求；items 至少包含一件当前用户购物车商品。 */
export interface OrderPreviewRequest {
  items: OrderPreviewItemRequest[]
}

/** 后端计算出的预览商品行。 */
export interface OrderPreviewItem {
  productId: string
  name: string
  unitPrice: number
  quantity: number
  lineAmount: number
  currentStock: number
  available: boolean
}

/** 订单预览结果；它只是确认页面数据，不代表订单已经创建。 */
export interface OrderPreview {
  items: OrderPreviewItem[]
  pickupLocationName: string
  pickupLocationAddress: string
  totalQuantity: number
  totalAmount: number
}

/** 我的订单列表中的摘要字段。 */
export interface OrderSummary {
  id: string
  orderNo: string
  status: OrderStatus
  pickupLocationName: string
  itemQuantity: number
  totalAmount: number
  createdAt: string
}

/** 历史订单中的商品快照；productId 在商品被删除后可能为空。 */
export interface OrderItem {
  productId: string | null
  sku: string
  productName: string
  unitPrice: number
  quantity: number
  lineAmount: number
}

/** 我的订单详情；后端不会重复返回明文取货码。 */
export interface OrderDetail extends OrderSummary {
  pickupLocationAddress: string
  items: OrderItem[]
  cancelledAt: string | null
  pickedUpAt: string | null
}

/** 后端统一分页结构，前端列表只消费 items 和分页信息。 */
export interface OrderPage {
  items: OrderSummary[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}

