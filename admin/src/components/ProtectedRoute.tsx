import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { getSession } from '../utils/auth';

export default function ProtectedRoute() {
  const location = useLocation();
  const session = getSession();
  const canUseAdmin = session?.user.roles.some((role) => ['SUPER_ADMIN', 'ADMIN', 'OPERATOR'].includes(role));
  return session?.accessToken && canUseAdmin ? <Outlet /> : <Navigate to="/login" replace state={{ from: location }} />;
}
