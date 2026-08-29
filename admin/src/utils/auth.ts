import type { TokenResponse, User } from '../types';

const SESSION_KEY = 'ai-mall-admin-session';

export const getSession = (): TokenResponse | null => {
  try { return JSON.parse(localStorage.getItem(SESSION_KEY) || 'null'); } catch { return null; }
};
export const saveSession = (session: TokenResponse) => localStorage.setItem(SESSION_KEY, JSON.stringify(session));
// 后端校验成功后更新用户角色，令牌字段保持不变。
export const updateSessionUser = (user: User) => {
  const session = getSession();
  if (session) saveSession({ ...session, user });
};
export const clearSession = () => localStorage.removeItem(SESSION_KEY);
export const getCurrentUser = (): User | null => getSession()?.user ?? null;
