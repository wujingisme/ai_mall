export type UserRole = 'CUSTOMER' | 'SUPER_ADMIN' | 'ADMIN' | 'OPERATOR';
export interface User { id: string; username: string; displayName: string; avatarUrl?: string; roles: UserRole[] }
export interface TokenResponse { tokenType: 'Bearer'; accessToken: string; expiresIn: number; refreshToken: string; refreshExpiresIn: number; user: User }
export interface AdminAccountPayload { username: string; password: string; displayName: string; role: 'ADMIN' | 'OPERATOR' }
export interface Product { id: string; sku: string; name: string; price: number; stock: number; status: 0 | 1; imageUrl?: string; description?: string; createdAt: string; updatedAt: string }
export interface ProductPage { items: Product[]; page: number; pageSize: number; total: number; totalPages: number }
export type ProductPayload = Pick<Product, 'sku' | 'name' | 'price' | 'stock' | 'status'> & Pick<Partial<Product>, 'imageUrl' | 'description'>;

export type CouponTemplateStatus = 'DRAFT' | 'ACTIVE' | 'DISABLED';
export type CouponValidityType = 'FIXED_RANGE' | 'DAYS_AFTER_RECEIPT';
export interface CouponTemplatePayload {
  name: string;
  couponType: 'FIXED_AMOUNT';
  minimumSpend: string;
  discountAmount: string;
  totalQuantity: number;
  perUserLimit: number;
  validityType: CouponValidityType;
  validFrom?: string | null;
  validUntil?: string | null;
  validDays?: number | null;
  shareEnabled: boolean;
}
export interface CouponTemplate extends CouponTemplatePayload {
  id: string;
  issuedQuantity: number;
  status: CouponTemplateStatus;
  createdAt: string;
  updatedAt: string;
}
export interface CouponTemplatePage {
  items: CouponTemplate[];
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
}
export interface CustomerSummary { id: string; username: string; displayName: string; avatarUrl?: string }
export interface CustomerPage { items: CustomerSummary[]; page: number; pageSize: number; total: number; totalPages: number }
export interface CouponGrantPayload { templateId: string; targetUserId: string; quantity: number; reason: string; idempotencyKey: string }
export interface CouponGrant extends CouponGrantPayload {
  id: string; operatorUserId: string; requestedQuantity: number; successQuantity: number; status: 'SUCCESS'; createdAt: string;
}
export interface AdminUser extends CustomerSummary { enabled: boolean; roles: string; wechatBound: boolean; createdAt?: string; updatedAt?: string }
export interface AdminUserPage { items: AdminUser[]; page: number; pageSize: number; total: number; totalPages: number }
export interface AdminUserCoupon { id: string; templateId: string; name: string; minimumSpend: string; discountAmount: string; validFrom: string; validUntil: string; status: 'AVAILABLE' | 'USED' | 'EXPIRED'; source: 'MANUAL'; createdAt: string }
export interface AdminUserCouponPage { items: AdminUserCoupon[]; page: number; pageSize: number; total: number; totalPages: number }
