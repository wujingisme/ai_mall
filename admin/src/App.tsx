import { Navigate, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute';
import AdminLayout from './layouts/AdminLayout';
import LoginPage from './pages/LoginPage';
import ProductsPage from './pages/ProductsPage';
import AdminAccountsPage from './pages/AdminAccountsPage';
import CouponTemplatesPage from './pages/CouponTemplatesPage';
import CouponGrantsPage from './pages/CouponGrantsPage';
import AdminUsersPage from './pages/AdminUsersPage';

export default function App() { return <Routes><Route path="/login" element={<LoginPage />} /><Route element={<ProtectedRoute />}><Route element={<AdminLayout />}><Route index element={<Navigate to="/products" replace />} /><Route path="/products" element={<ProductsPage />} /><Route path="/coupon-templates" element={<CouponTemplatesPage />} /><Route path="/coupon-grants" element={<CouponGrantsPage />} /><Route path="/users" element={<AdminUsersPage />} /><Route path="/accounts" element={<AdminAccountsPage />} /></Route></Route><Route path="*" element={<Navigate to="/" replace />} /></Routes>; }
