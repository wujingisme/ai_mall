export interface CartItem {
  productId: string
  name: string
  price: number
  imageUrl?: string
  quantity: number
  stock: number
  available: boolean
}

export interface Cart {
  items: CartItem[]
  totalQuantity: number
  totalAmount: number
}
