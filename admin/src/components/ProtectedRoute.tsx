import { useEffect, useState } from 'react';
import { Spin } from 'antd';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { getCurrentUser } from '../api';
import { clearSession, getSession, updateSessionUser } from '../utils/auth';

export default function ProtectedRoute() {
  const location = useLocation();
  const session = getSession();
  const [checking, setChecking] = useState(Boolean(session?.accessToken));
  const [authorized, setAuthorized] = useState(false);

  useEffect(() => {
    if (!session?.accessToken) { setChecking(false); setAuthorized(false); return; }
    let active = true;
    // 渲染后台页面前使用 /auth/me 校验真实会话和最新角色，不能只信任 localStorage。
    getCurrentUser().then((user) => {
      const allowed = user.roles.some((role) => ['SUPER_ADMIN', 'ADMIN', 'OPERATOR'].includes(role));
      if (!allowed) clearSession(); else updateSessionUser(user);
      if (active) { setAuthorized(allowed); setChecking(false); }
    }).catch(() => {
      clearSession();
      if (active) { setAuthorized(false); setChecking(false); }
    });
    return () => { active = false; };
  }, [location.pathname]);

  if (checking) return <div className="auth-checking"><Spin tip="正在校验登录状态" /></div>;
  return authorized ? <Outlet /> : <Navigate to="/login" replace state={{ from: location }} />;
}
