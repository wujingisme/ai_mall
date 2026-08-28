import { Navigate, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute';
import AdminLayout from './layouts/AdminLayout';
import LoginPage from './pages/LoginPage';
import ProductsPage from './pages/ProductsPage';

export default function App() { return <Routes><Route path="/login" element={<LoginPage />} /><Route element={<ProtectedRoute />}><Route element={<AdminLayout />}><Route index element={<Navigate to="/products" replace />} /><Route path="/products" element={<ProductsPage />} /></Route></Route><Route path="*" element={<Navigate to="/" replace />} /></Routes>; }
