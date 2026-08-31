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
  source: 'MANUAL' | 'SHARE'
  usedAt?: string
  createdAt: string
}
export interface UserCouponPage { items: UserCoupon[]; page: number; pageSize: number; total: number; totalPages: number }
export interface CouponShareCreated { shareToken: string; sharePath: string; expiresAt: string }
export interface CouponSharePreview { name: string; minimumSpend: string; discountAmount: string; expiresAt: string; claimable: boolean }
