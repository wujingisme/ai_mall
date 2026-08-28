import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Card, Form, Input, Typography, message } from 'antd';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { login } from '../api';
import { getSession, saveSession } from '../utils/auth';

export default function LoginPage() {
  const navigate = useNavigate(); const location = useLocation();
  if (getSession()?.accessToken) return <Navigate to="/products" replace />;
  const submit = async (values: { username: string; password: string }) => {
    try {
      const session = await login(values.username, values.password);
      // 管理后台拒绝 CUSTOMER 登录，不能只凭“拥有有效 token”判断后台权限。
      if (!session.user.roles.some((role) => ['SUPER_ADMIN', 'ADMIN', 'OPERATOR'].includes(role))) {
        message.error('该账号没有管理后台权限');
        return;
      }
      saveSession(session); message.success('登录成功'); navigate(location.state?.from?.pathname || '/products', { replace: true });
    }
    catch { message.error('登录失败，请检查账号和密码'); }
  };
  return <div className="login-page"><div className="ambient ambient-one" /><div className="ambient ambient-two" /><div className="login-shell"><section className="login-brand-panel"><div className="brand-mark">A</div><div className="brand-name">AI MALL</div><h1>管理精选好物，<br />服务品质生活。</h1><p>高效管理商品、库存与上下架状态，让每一件好物都被认真对待。</p><div className="brand-feature"><span>✓</span>统一的商品管理体验</div><div className="brand-feature"><span>✓</span>安全可靠的权限保护</div></section><Card className="login-card" bordered={false}><div className="mobile-brand"><span>A</span> AI MALL</div><div className="eyebrow">ADMIN CONSOLE</div><Typography.Title level={2}>欢迎回来</Typography.Title><Typography.Paragraph type="secondary">登录 AI Mall，开始管理商城。</Typography.Paragraph><Form layout="vertical" onFinish={submit} size="large"><Form.Item name="username" label="账号" rules={[{ required: true, message: '请输入账号' }]}><Input prefix={<UserOutlined />} placeholder="请输入管理员账号" autoComplete="username" /></Form.Item><Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }, { min: 6, message: '密码至少 6 位' }]}><Input.Password prefix={<LockOutlined />} placeholder="请输入登录密码" autoComplete="current-password" /></Form.Item><Button block type="primary" htmlType="submit">登录</Button></Form><div className="security-tip">◆ 您的登录信息已加密保护</div></Card></div><div className="copyright">© 2026 AI Mall · 品质生活商城</div></div>;
}
