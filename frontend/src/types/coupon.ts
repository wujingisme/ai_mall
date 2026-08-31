export type UserCouponStatus = 'AVAILABLE' | 'USED' | 'EXPIRED'
export interface UserCoupon {
  id: string
  templateId: string
  name: string
  couponType: 'FIXED_AMOUNT'
  minimumSpend: string
  discountAmount: string
  validFrom: string
  validUntil: string
  status: UserCouponStatus
  source: 'MANUAL'
  usedAt?: string
  createdAt: string
}
export interface UserCouponPage { items: UserCoupon[]; page: number; pageSize: number; total: number; totalPages: number }
