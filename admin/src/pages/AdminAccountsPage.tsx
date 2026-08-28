import { Button, Card, Form, Input, Select, Typography, message } from 'antd';
import { Navigate } from 'react-router-dom';
import { createAdminAccount } from '../api';
import type { AdminAccountPayload } from '../types';
import { getSession } from '../utils/auth';

export default function AdminAccountsPage() {
  const [form] = Form.useForm<AdminAccountPayload>();
  const isSuperAdmin = getSession()?.user.roles.includes('SUPER_ADMIN');

  const submit = async (values: AdminAccountPayload) => {
    try {
      await createAdminAccount(values);
      message.success('后台账号创建成功');
      form.resetFields();
    } catch {
      // 接口层会保留后端 403、用户名重复等状态，此处提供统一的页面反馈。
      message.error('创建失败，请检查用户名是否重复或当前账号权限');
    }
  };

  // 直接输入地址时也阻止普通管理员进入；真正的安全边界仍是后端的 403 校验。
  if (!isSuperAdmin) return <Navigate to="/products" replace />;

  return <Card title="创建后台账号" className="account-card">
    <Typography.Paragraph type="secondary">仅超级管理员可以创建，普通管理员和消费者均无权调用此功能。</Typography.Paragraph>
    <Form form={form} layout="vertical" onFinish={submit} initialValues={{ role: 'OPERATOR' }}>
      <Form.Item name="username" label="用户名" rules={[
        { required: true, message: '请输入用户名' },
        { pattern: /^[A-Za-z0-9_]{3,64}$/, message: '请输入 3-64 位字母、数字或下划线' },
      ]}><Input autoComplete="off" /></Form.Item>
      <Form.Item name="displayName" label="显示名称" rules={[{ required: true, message: '请输入显示名称' }]}>
        <Input maxLength={100} />
      </Form.Item>
      <Form.Item name="password" label="初始密码" rules={[
        { required: true, message: '请输入初始密码' },
        { min: 6, max: 72, message: '密码长度为 6-72 位' },
      ]}><Input.Password autoComplete="new-password" /></Form.Item>
      <Form.Item name="role" label="账号角色" rules={[{ required: true }]}>
        <Select options={[
          { value: 'OPERATOR', label: '运营人员（商品管理）' },
          { value: 'ADMIN', label: '管理员' },
        ]} />
      </Form.Item>
      <Button type="primary" htmlType="submit">创建账号</Button>
    </Form>
  </Card>;
}
