import { EyeOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { Button, Card, Descriptions, Drawer, Input, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { getAdminOrder, listAdminOrders } from '../api';
import type { AdminOrderDetail, AdminOrderItem, AdminOrderSummary, OrderStatus } from '../types';

const STATUS_META: Record<OrderStatus, { label: string; color: string }> = {
  PENDING_PICKUP: { label: '待取货', color: 'processing' },
  PICKED_UP: { label: '已取货', color: 'success' },
  CANCELLED: { label: '已取消', color: 'default' },
};

/**
 * Admin 订单查询页面。
 *
 * <p>页面只负责筛选条件、分页状态和详情展示；订单归属、状态合法性和敏感字段
 * 是否返回由后端决定。当前阶段没有核销按钮，避免前端先出现尚未实现的库存变更操作。</p>
 */
export default function OrdersPage() {
  const [items, setItems] = useState<AdminOrderSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [status, setStatus] = useState<OrderStatus>();
  const [orderNo, setOrderNo] = useState('');
  const [userId, setUserId] = useState('');
  const [detail, setDetail] = useState<AdminOrderDetail>();
  const [drawerOpen, setDrawerOpen] = useState(false);

  /**
   * 加载订单列表。
   *
   * <p>支持 overrides 是为了处理“查询/重置后立刻请求”的 React 状态时序问题：
   * setState 是异步的，不能假设下一行代码已经读取到新筛选值。</p>
   */
  const load = async (overrides: Partial<{
    page: number;
    pageSize: number;
    status: OrderStatus | undefined;
    orderNo: string;
    userId: string;
  }> = {}) => {
    setLoading(true);
    try {
      const query = { page, pageSize, status, orderNo, userId, ...overrides };
      const data = await listAdminOrders({
        page: query.page,
        pageSize: query.pageSize,
        status: query.status,
        orderNo: query.orderNo.trim() || undefined,
        userId: query.userId.trim() || undefined,
      });
      setItems(data.items);
      setTotal(data.total);
    } catch {
      message.error('订单列表加载失败');
    } finally {
      setLoading(false);
    }
  };

  // 分页或状态下拉变化后自动加载；订单号和客户 ID 只有点击查询才提交。
  useEffect(() => { void load(); }, [page, pageSize, status]);

  /** 打开详情抽屉前重新读取服务端快照，避免使用列表行的简化字段冒充详情。 */
  const openDetail = async (record: AdminOrderSummary) => {
    try {
      const data = await getAdminOrder(record.id);
      setDetail(data);
      setDrawerOpen(true);
    } catch {
      message.error('订单详情加载失败');
    }
  };

  const formatTime = (value?: string | null) => value
    ? new Date(value).toLocaleString('zh-CN', { hour12: false })
    : '-';

  const statusTag = (value: OrderStatus) => {
    const meta = STATUS_META[value];
    return <Tag color={meta.color}>{meta.label}</Tag>;
  };

  const columns: ColumnsType<AdminOrderSummary> = [
    {
      title: '订单',
      render: (_, row) => <div>
        <Typography.Text strong>{row.orderNo}</Typography.Text><br />
        <Typography.Text type="secondary">客户：{row.displayName || row.username || '未知'}（ID：{row.userId || '-'}）</Typography.Text>
      </div>,
    },
    { title: '商品件数', dataIndex: 'itemQuantity' },
    { title: '金额', dataIndex: 'totalAmount', render: (value) => `¥${Number(value).toFixed(2)}` },
    { title: '状态', dataIndex: 'status', render: (value: OrderStatus) => statusTag(value) },
    { title: '取货点', dataIndex: 'pickupLocationName', render: (value) => value || '-' },
    { title: '创建时间', dataIndex: 'createdAt', render: (value) => formatTime(value) },
    { title: '操作', key: 'actions', fixed: 'right', render: (_, row) => <Button type="link" icon={<EyeOutlined />} onClick={() => void openDetail(row)}>查看详情</Button> },
  ];

  return <>
    <Card title="订单管理">
      <Space wrap className="filters">
        <Input
          allowClear
          value={orderNo}
          prefix={<SearchOutlined />}
          placeholder="订单号"
          onChange={(event) => setOrderNo(event.target.value)}
          onPressEnter={() => { setPage(1); void load({ page: 1 }); }}
        />
        <Input
          allowClear
          value={userId}
          placeholder="客户 ID"
          onChange={(event) => setUserId(event.target.value)}
          onPressEnter={() => { setPage(1); void load({ page: 1 }); }}
        />
        <Select<OrderStatus>
          allowClear
          value={status}
          placeholder="全部状态"
          onChange={(value) => { setStatus(value); setPage(1); }}
          options={Object.entries(STATUS_META).map(([value, meta]) => ({ value, label: meta.label }))}
        />
        <Button icon={<SearchOutlined />} onClick={() => { setPage(1); void load({ page: 1 }); }}>查询</Button>
        <Button icon={<ReloadOutlined />} onClick={() => {
          setOrderNo('');
          setUserId('');
          setStatus(undefined);
          setPage(1);
          void load({ page: 1, orderNo: '', userId: '', status: undefined });
        }}>重置</Button>
      </Space>
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={items}
        scroll={{ x: 1100 }}
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: true,
          showTotal: (value) => `共 ${value} 条`,
          onChange: (next, size) => { setPage(next); setPageSize(size); },
        }}
      />
    </Card>

    <Drawer
      width={680}
      title="订单详情"
      open={drawerOpen}
      onClose={() => setDrawerOpen(false)}
    >
      {detail && <>
        <Descriptions bordered column={1}>
          <Descriptions.Item label="订单号">{detail.orderNo}</Descriptions.Item>
          <Descriptions.Item label="客户">{detail.displayName || detail.username || '未知'}（ID：{detail.userId || '-'}）</Descriptions.Item>
          <Descriptions.Item label="状态">{statusTag(detail.status)}</Descriptions.Item>
          <Descriptions.Item label="取货点">{detail.pickupLocationName}</Descriptions.Item>
          <Descriptions.Item label="取货地址">{detail.pickupLocationAddress}</Descriptions.Item>
          <Descriptions.Item label="商品件数">{detail.itemQuantity}</Descriptions.Item>
          <Descriptions.Item label="订单金额">¥{Number(detail.totalAmount).toFixed(2)}</Descriptions.Item>
          <Descriptions.Item label="创建时间">{formatTime(detail.createdAt)}</Descriptions.Item>
          <Descriptions.Item label="取消时间">{formatTime(detail.cancelledAt)}</Descriptions.Item>
          <Descriptions.Item label="取货时间">{formatTime(detail.pickedUpAt)}</Descriptions.Item>
        </Descriptions>
        <Typography.Title level={5} style={{ marginTop: 24 }}>商品快照</Typography.Title>
        <Table<AdminOrderItem>
          size="small"
          rowKey={(item, index) => `${item.productId || item.sku}-${index}`}
          pagination={false}
          dataSource={detail.items}
          scroll={{ x: 560 }}
          columns={[
            { title: '商品', dataIndex: 'productName' },
            { title: 'SKU', dataIndex: 'sku' },
            { title: '单价', dataIndex: 'unitPrice', render: (value) => `¥${Number(value).toFixed(2)}` },
            { title: '数量', dataIndex: 'quantity' },
            { title: '小计', dataIndex: 'lineAmount', render: (value) => `¥${Number(value).toFixed(2)}` },
          ]}
        />
        <Typography.Paragraph type="secondary" style={{ marginTop: 16 }}>
          取货码不会在详情页重复展示；店家核销功能将在下一阶段接入。
        </Typography.Paragraph>
      </>}
    </Drawer>
  </>;
}
