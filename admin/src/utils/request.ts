import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { clearSession, getSession, saveSession } from './auth';
import type { TokenResponse } from '../types';

const apiOrigin = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? '';
const apiBaseUrl = `${apiOrigin}/api/v1`;

export const request = axios.create({ baseURL: apiBaseUrl, timeout: 15000 });
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
  refreshing ??= axios.post<TokenResponse>(`${apiBaseUrl}/auth/refresh`, { refreshToken: session.refreshToken })
    .then(({ data }) => { saveSession(data); return data.accessToken; })
    .catch((refreshError) => { clearSession(); window.location.href = '/login'; throw refreshError; })
    .finally(() => { refreshing = null; });
  original.headers.Authorization = `Bearer ${await refreshing}`;
  return request(original);
});
