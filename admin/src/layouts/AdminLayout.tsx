import { LogoutOutlined, ShoppingOutlined } from '@ant-design/icons';
import { Avatar, Button, Layout, Menu, Space, Typography, message } from 'antd';
import { Outlet, useNavigate } from 'react-router-dom';
import { logout } from '../api';
import { clearSession, getSession } from '../utils/auth';

export default function AdminLayout() {
  const navigate = useNavigate();
  const session = getSession();
  const signOut = async () => {
    try { if (session?.refreshToken) await logout(session.refreshToken); } catch { message.warning('服务端退出失败，本地登录状态已清除'); }
    clearSession(); navigate('/login', { replace: true });
  };
  return <Layout className="app-layout">
    <Layout.Sider breakpoint="lg" collapsedWidth="0"><div className="brand">AI Mall</div><Menu theme="dark" selectedKeys={['products']} items={[{ key: 'products', icon: <ShoppingOutlined />, label: '商品管理' }]} /></Layout.Sider>
    <Layout><Layout.Header className="app-header"><Typography.Text strong>商城管理后台</Typography.Text><Space><Avatar>{session?.user.displayName.slice(0, 1)}</Avatar><span>{session?.user.displayName}</span><Button type="text" icon={<LogoutOutlined />} onClick={signOut}>退出</Button></Space></Layout.Header><Layout.Content className="app-content"><Outlet /></Layout.Content></Layout>
  </Layout>;
}
