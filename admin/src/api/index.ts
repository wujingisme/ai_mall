import { request } from '../utils/request';
import type { AdminAccountPayload, CouponTemplate, CouponTemplatePage, CouponTemplatePayload, CouponTemplateStatus, Product, ProductPage, ProductPayload, TokenResponse, User } from '../types';

export const login = async (username: string, password: string) => (await request.post<TokenResponse>('/auth/login', { username, password })).data;
export const logout = async (refreshToken: string) => request.post('/auth/logout', { refreshToken });
export const getCurrentUser = async () => (await request.get<User>('/auth/me')).data;
// 后端会再次校验 SUPER_ADMIN 权限，前端隐藏菜单不能替代服务端鉴权。
export const createAdminAccount = async (payload: AdminAccountPayload) => (await request.post<User>('/admin/accounts', payload)).data;
export const listProducts = async (params: { page: number; pageSize: number; keyword?: string; status?: 0 | 1 }) => (await request.get<ProductPage>('/products', { params })).data;
export const getProduct = async (id: string) => (await request.get<Product>(`/products/${id}`)).data;
export const createProduct = async (payload: ProductPayload) => (await request.post<Product>('/products', payload)).data;
export const updateProduct = async (id: string, payload: ProductPayload) => (await request.put<Product>(`/products/${id}`, payload)).data;
export const deleteProduct = async (id: string) => request.delete(`/products/${id}`);
export const listCouponTemplates = async (params: { page: number; pageSize: number; keyword?: string; status?: CouponTemplateStatus }) => (await request.get<CouponTemplatePage>('/admin/coupon-templates', { params })).data;
export const getCouponTemplate = async (id: string) => (await request.get<CouponTemplate>(`/admin/coupon-templates/${id}`)).data;
export const createCouponTemplate = async (payload: CouponTemplatePayload) => (await request.post<CouponTemplate>('/admin/coupon-templates', payload)).data;
export const updateCouponTemplate = async (id: string, payload: CouponTemplatePayload) => (await request.put<CouponTemplate>(`/admin/coupon-templates/${id}`, payload)).data;
export const activateCouponTemplate = async (id: string) => (await request.post<CouponTemplate>(`/admin/coupon-templates/${id}/activation`)).data;
export const deactivateCouponTemplate = async (id: string) => (await request.post<CouponTemplate>(`/admin/coupon-templates/${id}/deactivation`)).data;
