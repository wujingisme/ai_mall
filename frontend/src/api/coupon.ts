import { request } from '@/utils/request'
import type { CouponShareCreated, CouponSharePreview, UserCoupon, UserCouponPage, UserCouponStatus } from '@/types/coupon'

export const couponApi = {
  listMine: (params: { page?: number; pageSize?: number; status?: UserCouponStatus }) =>
    request<UserCouponPage>({ url: '/api/v1/me/coupons', method: 'GET', data: params }),
  getMine: (id: string) => request<UserCoupon>({ url: `/api/v1/me/coupons/${id}`, method: 'GET' }),
  createShare: (userCouponId: string) => request<CouponShareCreated>({ url: '/api/v1/me/coupon-shares', method: 'POST', data: { userCouponId } }),
  resolveShare: (shareToken: string) => request<CouponSharePreview>({ url: '/api/v1/coupon-shares/resolve', method: 'POST', data: { shareToken }, redirectOnUnauthorized: false }),
  claimShare: (shareToken: string) => request<UserCoupon>({ url: '/api/v1/coupon-claims', method: 'POST', data: { shareToken } }),
}
