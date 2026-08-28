import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Card, Form, Input, Typography, message } from 'antd';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { login } from '../api';
import { getSession, saveSession } from '../utils/auth';

export default function LoginPage() {
  const navigate = useNavigate(); const location = useLocation();
  if (getSession()?.accessToken) return <Navigate to="/products" replace />;
  const submit = async (values: { username: string; password: string }) => {
    try { saveSession(await login(values.username, values.password)); message.success('登录成功'); navigate(location.state?.from?.pathname || '/products', { replace: true }); }
    catch { message.error('登录失败，请检查账号和密码'); }
  };
  return <div className="login-page"><Card className="login-card"><Typography.Title level={2}>AI Mall</Typography.Title><Typography.Paragraph type="secondary">登录商城管理后台</Typography.Paragraph><Form layout="vertical" onFinish={submit} size="large"><Form.Item name="username" label="账号" rules={[{ required: true, message: '请输入账号' }]}><Input prefix={<UserOutlined />} autoComplete="username" /></Form.Item><Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }, { min: 8, message: '密码至少 8 位' }]}><Input.Password prefix={<LockOutlined />} autoComplete="current-password" /></Form.Item><Button block type="primary" htmlType="submit">登录</Button></Form></Card></div>;
}
