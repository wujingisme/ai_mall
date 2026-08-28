import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { getSession } from '../utils/auth';

export default function ProtectedRoute() {
  const location = useLocation();
  return getSession()?.accessToken ? <Outlet /> : <Navigate to="/login" replace state={{ from: location }} />;
}
