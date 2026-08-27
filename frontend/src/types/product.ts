// 商品状态：0 表示下架，1 表示上架。
export type ProductStatus = 0 | 1

// 列表页展示的商品字段。
export interface ProductListItem {
  id: string
  sku: string
  name: string
  price: number
  stock: number
  status: ProductStatus
  imageUrl?: string
  createdAt: string
  updatedAt: string
}

// 详情页在列表字段基础上增加商品描述。
export interface ProductDetail extends ProductListItem {
  description?: string
}

// 新增和编辑接口提交的数据结构。
export interface ProductWriteRequest {
  sku: string
  name: string
  price: number
  stock: number
  status: ProductStatus
  imageUrl?: string
  description?: string
}

// 分页接口返回结构。
export interface ProductPage {
  items: ProductListItem[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}

// 商品列表查询条件，问号表示该条件可以不传。
export interface ProductQuery {
  page?: number
  pageSize?: number
  keyword?: string
  status?: ProductStatus
}

// 后端统一错误响应结构。
export interface ApiError {
  code: string
  message: string
  details?: Array<{ field: string; message: string }>
  timestamp: string
}
