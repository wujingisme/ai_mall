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

/** 订单状态与后端 OrderStatus 枚举保持一致，页面不能自行发明状态值。 */
export type OrderStatus = 'PENDING_PICKUP' | 'PICKED_UP' | 'CANCELLED';

/** Admin 订单列表一行数据；客户字段可能为空，兼容历史脏数据或已删除用户。 */
export interface AdminOrderSummary {
  id: string;
  orderNo: string;
  userId: string | null;
  username: string | null;
  displayName: string | null;
  status: OrderStatus;
  pickupLocationName: string;
  itemQuantity: number;
  totalAmount: number;
  createdAt: string;
}

export interface AdminOrderPage {
  items: AdminOrderSummary[];
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
}

/** 订单商品快照；详情接口返回的是下单时的名称/价格，不是当前商品表数据。 */
export interface AdminOrderItem {
  productId: string | null;
  sku: string;
  productName: string;
  unitPrice: number;
  quantity: number;
  lineAmount: number;
}

/** Admin 订单详情；取货码只在后续核销接口提交，不会出现在这里。 */
export interface AdminOrderDetail extends AdminOrderSummary {
  pickupLocationAddress: string;
  items: AdminOrderItem[];
  cancelledAt: string | null;
  pickedUpAt: string | null;
}
