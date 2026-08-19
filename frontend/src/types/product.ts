export type ProductStatus = 0 | 1

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

export interface ProductDetail extends ProductListItem {
  description?: string
}

export interface ProductWriteRequest {
  sku: string
  name: string
  price: number
  stock: number
  status: ProductStatus
  imageUrl?: string
  description?: string
}

export interface ProductPage {
  items: ProductListItem[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}

export interface ProductQuery {
  page?: number
  pageSize?: number
  keyword?: string
  status?: ProductStatus
}

export interface ApiError {
  code: string
  message: string
  details?: Array<{ field: string; message: string }>
  timestamp: string
}
