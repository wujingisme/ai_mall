import { EditOutlined, EyeOutlined, PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { Button, Card, DatePicker, Descriptions, Drawer, Form, Input, InputNumber, Popconfirm, Radio, Select, Space, Switch, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import { useEffect, useState } from 'react';
import { activateCouponTemplate, createCouponTemplate, deactivateCouponTemplate, getCouponTemplate, listCouponTemplates, updateCouponTemplate } from '../api';
import type { CouponTemplate, CouponTemplatePayload, CouponTemplateStatus, CouponValidityType } from '../types';

type DrawerMode = 'create' | 'edit' | 'detail';
interface CouponFormValues extends Omit<CouponTemplatePayload, 'validFrom' | 'validUntil'> { validRange?: [Dayjs, Dayjs] }

const statusMeta: Record<CouponTemplateStatus, { label: string; color?: string }> = {
  DRAFT: { label: '草稿', color: 'default' }, ACTIVE: { label: '已启用', color: 'success' }, DISABLED: { label: '已停用' },
};
const moneyPattern = /^\d+(\.\d{1,2})?$/;
const formatValidity = (item: CouponTemplate) => item.validityType === 'FIXED_RANGE'
  ? `${dayjs(item.validFrom).format('YYYY-MM-DD HH:mm')} 至 ${dayjs(item.validUntil).format('YYYY-MM-DD HH:mm')}`
  : `领取后 ${item.validDays} 天`;

export default function CouponTemplatesPage() {
  const [items, setItems] = useState<CouponTemplate[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState<CouponTemplateStatus>();
  const [drawerMode, setDrawerMode] = useState<DrawerMode>();
  const [current, setCurrent] = useState<CouponTemplate>();
  const [form] = Form.useForm<CouponFormValues>();
  const validityType = Form.useWatch('validityType', form);

  const load = async (overrides: Partial<{ page: number; pageSize: number; keyword: string; status: CouponTemplateStatus | undefined }> = {}) => {
    setLoading(true);
    try {
      const query = { page, pageSize, keyword, status, ...overrides };
      const data = await listCouponTemplates({ ...query, keyword: query.keyword.trim() || undefined });
      setItems(data.items); setTotal(data.total);
    } catch { message.error('优惠券模板列表加载失败'); }
    finally { setLoading(false); }
  };
  useEffect(() => { void load(); }, [page, pageSize, status]);

  const openCreate = () => {
    setCurrent(undefined); form.resetFields();
    form.setFieldsValue({ couponType: 'FIXED_AMOUNT', totalQuantity: 100, perUserLimit: 1, validityType: 'DAYS_AFTER_RECEIPT', validDays: 7, shareEnabled: false });
    setDrawerMode('create');
  };
  const openTemplate = async (record: CouponTemplate, mode: 'edit' | 'detail') => {
    try {
      const detail = await getCouponTemplate(record.id);
      setCurrent(detail);
      if (mode === 'edit') form.setFieldsValue({ ...detail, validRange: detail.validFrom && detail.validUntil ? [dayjs(detail.validFrom), dayjs(detail.validUntil)] : undefined });
      setDrawerMode(mode);
    } catch { message.error('优惠券模板详情加载失败'); }
  };
  const toPayload = (values: CouponFormValues): CouponTemplatePayload => ({
    name: values.name.trim(), couponType: 'FIXED_AMOUNT', minimumSpend: values.minimumSpend,
    discountAmount: values.discountAmount, totalQuantity: values.totalQuantity, perUserLimit: values.perUserLimit,
    validityType: values.validityType, validFrom: values.validityType === 'FIXED_RANGE' ? values.validRange?.[0].toISOString() : null,
    validUntil: values.validityType === 'FIXED_RANGE' ? values.validRange?.[1].toISOString() : null,
    validDays: values.validityType === 'DAYS_AFTER_RECEIPT' ? values.validDays : null, shareEnabled: values.shareEnabled,
  });
  const save = async () => {
    try {
      const values = await form.validateFields();
      if (Number(values.discountAmount) >= Number(values.minimumSpend)) { message.error('优惠金额必须小于使用门槛'); return; }
      if (values.perUserLimit > values.totalQuantity) { message.error('每人限领不能大于发行总量'); return; }
      setSaving(true);
      if (drawerMode === 'edit' && current) await updateCouponTemplate(current.id, toPayload(values));
      else await createCouponTemplate(toPayload(values));
      message.success(drawerMode === 'edit' ? '优惠券模板已更新' : '优惠券模板已创建');
      setDrawerMode(undefined); await load();
    } catch (error) { if (!(error as { errorFields?: unknown }).errorFields) message.error('保存失败，请检查模板规则'); }
    finally { setSaving(false); }
  };
  const changeStatus = async (record: CouponTemplate) => {
    try {
      if (record.status === 'DRAFT') await activateCouponTemplate(record.id); else await deactivateCouponTemplate(record.id);
      message.success(record.status === 'DRAFT' ? '优惠券模板已启用' : '优惠券模板已停用'); await load();
    } catch { message.error('状态修改失败，请刷新后重试'); }
  };

  const columns: ColumnsType<CouponTemplate> = [
    { title: '模板名称', dataIndex: 'name', render: (value) => <Typography.Text strong>{value}</Typography.Text> },
    { title: '优惠规则', render: (_, row) => `满 ¥${row.minimumSpend} 减 ¥${row.discountAmount}` },
    { title: '发行情况', render: (_, row) => `${row.issuedQuantity} / ${row.totalQuantity}` },
    { title: '每人限领', dataIndex: 'perUserLimit', render: (value) => `${value} 张` },
    { title: '有效期', render: (_, row) => formatValidity(row) },
    { title: '分享', dataIndex: 'shareEnabled', render: (value) => value ? <Tag color="blue">允许</Tag> : <Tag>关闭</Tag> },
    { title: '状态', dataIndex: 'status', render: (value: CouponTemplateStatus) => <Tag color={statusMeta[value].color}>{statusMeta[value].label}</Tag> },
    { title: '操作', key: 'actions', fixed: 'right', render: (_, row) => <Space>
      <Button type="link" icon={<EyeOutlined />} onClick={() => openTemplate(row, 'detail')}>查看</Button>
      {row.status === 'DRAFT' && <Button type="link" icon={<EditOutlined />} onClick={() => openTemplate(row, 'edit')}>编辑</Button>}
      {row.status !== 'DISABLED' && <Popconfirm title={row.status === 'DRAFT' ? '启用后将不能再编辑，确定启用？' : '确定停用该优惠券模板？'} onConfirm={() => changeStatus(row)}><Button type="link" danger={row.status === 'ACTIVE'}>{row.status === 'DRAFT' ? '启用' : '停用'}</Button></Popconfirm>}
    </Space> },
  ];

  return <><Card title="优惠券模板" extra={<Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增模板</Button>}>
    <Space wrap className="filters"><Input allowClear value={keyword} onChange={(e) => setKeyword(e.target.value)} onPressEnter={() => { setPage(1); void load({ page: 1 }); }} prefix={<SearchOutlined />} placeholder="模板名称" />
      <Select allowClear value={status} onChange={(value) => { setStatus(value); setPage(1); }} placeholder="全部状态" options={Object.entries(statusMeta).map(([value, meta]) => ({ value, label: meta.label }))} />
      <Button icon={<SearchOutlined />} onClick={() => { setPage(1); void load({ page: 1 }); }}>查询</Button>
      <Button icon={<ReloadOutlined />} onClick={() => { setKeyword(''); setStatus(undefined); setPage(1); void load({ page: 1, keyword: '', status: undefined }); }}>重置</Button>
    </Space>
    <Table rowKey="id" loading={loading} columns={columns} dataSource={items} scroll={{ x: 1150 }} pagination={{ current: page, pageSize, total, showSizeChanger: true, showTotal: (value) => `共 ${value} 条`, onChange: (next, size) => { setPage(next); setPageSize(size); } }} />
  </Card>
    <Drawer width={600} open={drawerMode !== undefined} title={drawerMode === 'create' ? '新增优惠券模板' : drawerMode === 'edit' ? '编辑优惠券模板' : '优惠券模板详情'} onClose={() => setDrawerMode(undefined)} extra={drawerMode !== 'detail' && <Button type="primary" loading={saving} onClick={save}>保存草稿</Button>}>
      {drawerMode === 'detail' && current ? <Descriptions bordered column={1}>
        <Descriptions.Item label="模板名称">{current.name}</Descriptions.Item><Descriptions.Item label="优惠类型">满减券</Descriptions.Item>
        <Descriptions.Item label="优惠规则">满 ¥{current.minimumSpend} 减 ¥{current.discountAmount}</Descriptions.Item><Descriptions.Item label="发行情况">已发行 {current.issuedQuantity} / 总量 {current.totalQuantity}</Descriptions.Item>
        <Descriptions.Item label="每人限领">{current.perUserLimit} 张</Descriptions.Item><Descriptions.Item label="有效期">{formatValidity(current)}</Descriptions.Item>
        <Descriptions.Item label="允许分享">{current.shareEnabled ? '是' : '否'}</Descriptions.Item><Descriptions.Item label="状态">{statusMeta[current.status].label}</Descriptions.Item>
      </Descriptions> : <Form form={form} layout="vertical">
        <Form.Item name="name" label="模板名称" rules={[{ required: true, message: '请输入模板名称' }, { max: 100, message: '最多 100 个字符' }]}><Input placeholder="例如：新用户满100减20" /></Form.Item>
        <Form.Item name="couponType" label="优惠类型"><Radio.Group disabled options={[{ label: '满减券', value: 'FIXED_AMOUNT' }]} /></Form.Item>
        <Space size="large" align="start"><Form.Item name="minimumSpend" label="使用门槛" rules={[{ required: true, message: '请输入使用门槛' }, { pattern: moneyPattern, message: '请输入最多两位小数的金额' }]}><Input prefix="¥" placeholder="100.00" /></Form.Item>
          <Form.Item name="discountAmount" label="优惠金额" rules={[{ required: true, message: '请输入优惠金额' }, { pattern: moneyPattern, message: '请输入最多两位小数的金额' }]}><Input prefix="¥" placeholder="20.00" /></Form.Item></Space>
        <Space size="large" align="start"><Form.Item name="totalQuantity" label="发行总量" rules={[{ required: true, message: '请输入发行总量' }]}><InputNumber min={1} precision={0} /></Form.Item>
          <Form.Item name="perUserLimit" label="每人限领" rules={[{ required: true, message: '请输入每人限领数量' }]}><InputNumber min={1} precision={0} /></Form.Item></Space>
        <Form.Item name="validityType" label="有效期方式" rules={[{ required: true }]}><Radio.Group options={[{ label: '固定时间范围', value: 'FIXED_RANGE' }, { label: '领取后若干天', value: 'DAYS_AFTER_RECEIPT' }]} /></Form.Item>
        {validityType === 'FIXED_RANGE' ? <Form.Item name="validRange" label="有效时间" rules={[{ required: true, message: '请选择有效时间' }]}><DatePicker.RangePicker showTime style={{ width: '100%' }} /></Form.Item>
          : <Form.Item name="validDays" label="领取后有效天数" rules={[{ required: true, message: '请输入有效天数' }]}><InputNumber min={1} max={3650} precision={0} addonAfter="天" /></Form.Item>}
        <Form.Item name="shareEnabled" label="允许分享领取" valuePropName="checked"><Switch checkedChildren="允许" unCheckedChildren="关闭" /></Form.Item>
      </Form>}
    </Drawer>
  </>;
}
