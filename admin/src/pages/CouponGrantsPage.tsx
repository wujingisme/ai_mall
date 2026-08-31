import { GiftOutlined, SearchOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Form, Input, InputNumber, Select, Space, Typography, message } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { createCouponGrant, listCouponTemplates, listCustomers } from '../api';
import type { CouponTemplate, CustomerSummary } from '../types';

interface GrantForm { templateId: string; targetUserId: string; quantity: number; reason: string }
const newIdempotencyKey = () => `admin_${Date.now()}_${Math.random().toString(36).slice(2, 12)}`;

export default function CouponGrantsPage() {
  const [form] = Form.useForm<GrantForm>();
  const [templates, setTemplates] = useState<CouponTemplate[]>([]);
  const [customers, setCustomers] = useState<CustomerSummary[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const searchTimer = useRef<ReturnType<typeof setTimeout>>();

  const loadTemplates = async () => {
    try { setTemplates((await listCouponTemplates({ page: 1, pageSize: 100, status: 'ACTIVE' })).items); }
    catch { message.error('启用中的优惠券模板加载失败'); }
  };
  const searchCustomers = (keyword = '') => {
    clearTimeout(searchTimer.current);
    searchTimer.current = setTimeout(async () => {
      try { setCustomers((await listCustomers({ page: 1, pageSize: 20, keyword: keyword.trim() || undefined })).items); }
      catch { message.error('商城用户搜索失败'); }
    }, keyword ? 300 : 0);
  };
  useEffect(() => { void loadTemplates(); searchCustomers(); return () => clearTimeout(searchTimer.current); }, []);

  const submit = async () => {
    try {
      const values = await form.validateFields(); setSubmitting(true);
      const result = await createCouponGrant({ ...values, reason: values.reason.trim(), idempotencyKey: newIdempotencyKey() });
      message.success(`发放成功，共 ${result.successQuantity} 张`); form.resetFields(); await loadTemplates();
    } catch (error) { if (!(error as { errorFields?: unknown }).errorFields) message.error('发放失败，请检查库存和限领规则'); }
    finally { setSubmitting(false); }
  };

  return <Card title="人工发放优惠券">
    <Alert type="info" showIcon message="只可发放已启用模板" description="后端会再次校验模板库存、有效期和每人限领数量。重复点击由每次请求的幂等键保护。" />
    <Form form={form} layout="vertical" style={{ maxWidth: 680, marginTop: 24 }}>
      <Form.Item name="targetUserId" label="领取用户" rules={[{ required: true, message: '请选择用户' }]}>
        <Select showSearch filterOption={false} onSearch={searchCustomers} onFocus={() => searchCustomers()} placeholder="输入用户昵称或账号搜索" suffixIcon={<SearchOutlined />} options={customers.map((item) => ({ value: item.id, label: `${item.displayName}（${item.username}，ID ${item.id}）` }))} />
      </Form.Item>
      <Form.Item name="templateId" label="优惠券模板" rules={[{ required: true, message: '请选择模板' }]}>
        <Select placeholder="请选择已启用模板" options={templates.map((item) => ({ value: item.id, label: `${item.name}｜满 ¥${item.minimumSpend} 减 ¥${item.discountAmount}｜剩余 ${item.totalQuantity - item.issuedQuantity}` }))} />
      </Form.Item>
      <Space size="large" align="start"><Form.Item name="quantity" label="发放数量" initialValue={1} rules={[{ required: true }]}><InputNumber min={1} max={100} precision={0} /></Form.Item></Space>
      <Form.Item name="reason" label="发放原因" rules={[{ required: true, message: '请输入发放原因' }, { max: 200 }]}><Input.TextArea rows={4} placeholder="例如：售后补偿、活动奖励" showCount maxLength={200} /></Form.Item>
      <Button type="primary" icon={<GiftOutlined />} loading={submitting} onClick={submit}>确认发放</Button>
      <Typography.Text type="secondary" style={{ marginLeft: 16 }}>发放后可在用户的小程序“我的优惠券”中查看。</Typography.Text>
    </Form>
  </Card>;
}
