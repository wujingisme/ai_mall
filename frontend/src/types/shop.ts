// 消费端商品模型不包含 SKU、精确库存和上下架状态等后台字段。
export interface ShopProductListItem {
  id: string
  name: string
  price: number
  imageUrl?: string | null
  soldOut: boolean
}

export interface ShopProductDetail extends ShopProductListItem {
  description?: string | null
}

export interface ShopProductPage {
  items: ShopProductListItem[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}

export interface ShopProductQuery {
  page?: number
  pageSize?: number
  keyword?: string
}
