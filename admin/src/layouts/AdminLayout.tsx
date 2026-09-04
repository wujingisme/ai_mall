import { FileTextOutlined, GiftOutlined, LogoutOutlined, ShoppingOutlined, TeamOutlined, UserAddOutlined } from '@ant-design/icons';
import { Avatar, Button, Layout, Menu, Modal, Space, Typography, message } from 'antd';
import { useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { enableCurrentCustomerRole, logout } from '../api';
import { clearSession, getSession, updateSessionUser } from '../utils/auth';

export default function AdminLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  // 用状态保存会话，角色开通成功后立即刷新顶部按钮；真正的令牌仍由后端签发和校验。
  const [session, setSession] = useState(() => getSession());
  const isSuperAdmin = session?.user.roles.includes('SUPER_ADMIN');
  const signOut = async () => {
    try { if (session?.refreshToken) await logout(session.refreshToken); } catch { message.warning('服务端退出失败，本地登录状态已清除'); }
    clearSession(); navigate('/login', { replace: true });
  };
  /**
   * 当前管理员主动开通消费者身份。
   *
   * <p>这里只能操作当前 JWT 对应的账号，避免后台角色被用来修改其他用户。数据库角色
   * 更新成功后，前台使用同一账号重新登录即可拿到包含 CUSTOMER 的新 JWT。</p>
   */
  const enableCustomer = () => {
    Modal.confirm({
      title: '开通前台消费者身份',
      content: '当前后台账号将同时可以在前台购物和下单，已有后台权限不会改变。确定继续吗？',
      okText: '确认开通',
      cancelText: '取消',
      onOk: async () => {
        try {
          const user = await enableCurrentCustomerRole();
          updateSessionUser(user);
          setSession((current) => current ? { ...current, user } : current);
          message.success('消费者身份已开通，请在前台重新登录');
        } catch {
          message.error('消费者身份开通失败，请稍后重试');
        }
      },
    });
  };
  return <Layout className="app-layout">
    <Layout.Sider breakpoint="lg" collapsedWidth="0"><div className="brand">AI Mall</div><Menu theme="dark" selectedKeys={[location.pathname.startsWith('/accounts') ? 'accounts' : location.pathname.startsWith('/users') ? 'users' : location.pathname.startsWith('/orders') ? 'orders' : location.pathname.startsWith('/coupon-grants') ? 'coupon-grants' : location.pathname.startsWith('/coupon-templates') ? 'coupon-templates' : 'products']} onClick={({ key }) => navigate(`/${key}`)} items={[
      { key: 'products', icon: <ShoppingOutlined />, label: '商品管理' },
      { key: 'orders', icon: <FileTextOutlined />, label: '订单管理' },
      { key: 'coupon-templates', icon: <GiftOutlined />, label: '优惠券模板' },
      { key: 'coupon-grants', icon: <GiftOutlined />, label: '人工发券' },
      { key: 'users', icon: <TeamOutlined />, label: '用户管理' },
      // 普通管理员不展示入口；后端仍会独立执行 SUPER_ADMIN 权限校验。
      ...(isSuperAdmin ? [{ key: 'accounts', icon: <UserAddOutlined />, label: '后台账号' }] : []),
    ]} /></Layout.Sider>
    <Layout><Layout.Header className="app-header"><Typography.Text strong>商城管理后台</Typography.Text><Space>
      <Avatar>{session?.user.displayName.slice(0, 1)}</Avatar><span>{session?.user.displayName}</span>
      {/* 已经是双角色的账号不再显示入口，重复点击由后端同样保证幂等。 */}
      {session && !session.user.roles.includes('CUSTOMER') && <Button type="link" onClick={enableCustomer}>开通前台身份</Button>}
      <Button type="text" icon={<LogoutOutlined />} onClick={signOut}>退出</Button>
    </Space></Layout.Header><Layout.Content className="app-content"><Outlet /></Layout.Content></Layout>
  </Layout>;
}
