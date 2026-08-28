import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { clearSession, getSession, saveSession } from './auth';
import type { TokenResponse } from '../types';

export const request = axios.create({ baseURL: '/api/v1', timeout: 15000 });
request.interceptors.request.use((config) => {
  const token = getSession()?.accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

let refreshing: Promise<string> | null = null;
request.interceptors.response.use(undefined, async (error: AxiosError) => {
  const original = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined;
  const session = getSession();
  if (error.response?.status !== 401 || !original || original._retried || !session?.refreshToken || original.url?.includes('/auth/refresh')) throw error;
  original._retried = true;
  refreshing ??= axios.post<TokenResponse>('/api/v1/auth/refresh', { refreshToken: session.refreshToken })
    .then(({ data }) => { saveSession(data); return data.accessToken; })
    .catch((refreshError) => { clearSession(); window.location.href = '/login'; throw refreshError; })
    .finally(() => { refreshing = null; });
  original.headers.Authorization = `Bearer ${await refreshing}`;
  return request(original);
});
