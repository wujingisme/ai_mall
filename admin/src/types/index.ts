export interface User { id: string; username: string; displayName: string; avatarUrl?: string; roles: Array<'ADMIN' | 'OPERATOR'> }
export interface TokenResponse { tokenType: 'Bearer'; accessToken: string; expiresIn: number; refreshToken: string; refreshExpiresIn: number; user: User }
export interface Product { id: string; sku: string; name: string; price: number; stock: number; status: 0 | 1; imageUrl?: string; description?: string; createdAt: string; updatedAt: string }
export interface ProductPage { items: Product[]; page: number; pageSize: number; total: number; totalPages: number }
export type ProductPayload = Pick<Product, 'sku' | 'name' | 'price' | 'stock' | 'status'> & Pick<Partial<Product>, 'imageUrl' | 'description'>;
