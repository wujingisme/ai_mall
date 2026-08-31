import { request } from '@/utils/request'
import type { UserCoupon, UserCouponPage, UserCouponStatus } from '@/types/coupon'

export const couponApi = {
  listMine: (params: { page?: number; pageSize?: number; status?: UserCouponStatus }) =>
    request<UserCouponPage>({ url: '/api/v1/me/coupons', method: 'GET', data: params }),
  getMine: (id: string) => request<UserCoupon>({ url: `/api/v1/me/coupons/${id}`, method: 'GET' }),
}
