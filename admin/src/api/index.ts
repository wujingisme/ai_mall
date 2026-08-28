import { request } from '../utils/request';
import type { Product, ProductPage, ProductPayload, TokenResponse } from '../types';

export const login = async (username: string, password: string) => (await request.post<TokenResponse>('/auth/login', { username, password })).data;
export const logout = async (refreshToken: string) => request.post('/auth/logout', { refreshToken });
export const listProducts = async (params: { page: number; pageSize: number; keyword?: string; status?: 0 | 1 }) => (await request.get<ProductPage>('/products', { params })).data;
export const getProduct = async (id: string) => (await request.get<Product>(`/products/${id}`)).data;
export const createProduct = async (payload: ProductPayload) => (await request.post<Product>('/products', payload)).data;
export const updateProduct = async (id: string, payload: ProductPayload) => (await request.put<Product>(`/products/${id}`, payload)).data;
export const deleteProduct = async (id: string) => request.delete(`/products/${id}`);
