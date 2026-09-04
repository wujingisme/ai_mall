<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { orderApi } from '@/api/order'
import type { OrderStatus, OrderSummary } from '@/types/order'
import { getAccessToken } from '@/utils/auth-storage'
import { navigateToLogin, ROUTES } from '@/utils/navigation'

const tabs: { label: string; value: OrderStatus | '' }[] = [
  { label: '全部', value: '' },
  { label: '待取货', value: 'PENDING_PICKUP' },
  { label: '已取货', value: 'PICKED_UP' },
  { label: '已取消', value: 'CANCELLED' },
]
const status = ref<OrderStatus | ''>('')
const items = ref<OrderSummary[]>([])
const loading = ref(false)

/** 将后端状态转换为用户能理解的中文，不让页面到处散落字符串判断。 */
function statusLabel(value: OrderStatus) {
  return tabs.find((tab) => tab.value === value)?.label || value
}

function formatDate(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/**
 * 查询当前筛选条件下的订单。
 *
 * <p>列表页面不接受用户 ID，orderApi 也不拼接用户 ID；后端从 Bearer Token
 * 确定数据归属。当前后端只读接口一次最多取 100 条，后续再补加载更多。</p>
 */
async function load() {
  if (!getAccessToken()) {
    navigateToLogin(ROUTES.orderList)
    return
  }
  try {
    loading.value = true
    items.value = (await orderApi.listMine({
      page: 1,
      pageSize: 100,
      status: status.value || undefined,
    })).items
  } catch {
    // request() 已统一提示错误；清空旧列表，避免网络失败时继续展示过期订单。
    items.value = []
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

function switchStatus(value: OrderStatus | '') {
  if (status.value === value) return
  status.value = value
  void load()
}

function openDetail(id: string) {
  uni.navigateTo({ url: `${ROUTES.orderDetail}?id=${encodeURIComponent(id)}` })
}

onLoad((options) => {
  const requestedStatus = options?.status as OrderStatus | undefined
  if (requestedStatus && tabs.some((tab) => tab.value === requestedStatus)) status.value = requestedStatus
  void load()
})
onPullDownRefresh(load)
</script>

<template>
  <view class="page">
    <view class="tabs"><view v-for="tab in tabs" :key="tab.value" :class="['tab', { active: status === tab.value }]" @click="switchStatus(tab.value)">{{ tab.label }}</view></view>
    <view v-if="items.length" class="list">
      <view v-for="item in items" :key="item.id" class="order" @click="openDetail(item.id)">
        <view class="order-header"><text class="order-no">订单号 {{ item.orderNo }}</text><text :class="['status', item.status.toLowerCase()]">{{ statusLabel(item.status) }}</text></view>
        <view class="order-middle"><text class="pickup">{{ item.pickupLocationName }}</text><text class="date">{{ formatDate(item.createdAt) }}</text></view>
        <view class="order-footer"><text>{{ item.itemQuantity }} 件商品</text><text class="amount">¥{{ Number(item.totalAmount).toFixed(2) }}</text><text class="arrow">›</text></view>
      </view>
    </view>
    <view v-else-if="!loading" class="empty"><text class="icon">◇</text><text>暂无{{ status ? statusLabel(status) : '' }}订单</text></view>
    <view v-if="loading" class="loading">正在加载...</view>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; padding: 24rpx; background: #f6f7f4; }.tabs { display: flex; padding: 8rpx; border-radius: 18rpx; background: #fff; }.tab { flex: 1; padding: 19rpx 0; text-align: center; color: #758780; font-size: 24rpx; }.tab.active { border-radius: 14rpx; color: #fff; background: #245b4a; font-weight: 600; }.list { margin-top: 22rpx; }.order { margin-bottom: 20rpx; padding: 26rpx; border-radius: 22rpx; background: #fff; box-shadow: 0 8rpx 28rpx rgba(35,79,64,.06); }.order-header,.order-middle,.order-footer { display: flex; align-items: center; justify-content: space-between; }.order-no { overflow: hidden; max-width: 70%; color: #6d7d77; font-size: 22rpx; text-overflow: ellipsis; white-space: nowrap; }.status { padding: 6rpx 13rpx; border-radius: 8rpx; color: #a17838; background: #f7efd9; font-size: 21rpx; }.status.picked_up { color: #55756a; background: #e9f1ed; }.status.cancelled { color: #9a7771; background: #f5eae8; }.order-middle { margin-top: 24rpx; }.pickup { color: #294d40; font-size: 28rpx; font-weight: 600; }.date { color: #99a49f; font-size: 21rpx; }.order-footer { margin-top: 22rpx; padding-top: 20rpx; border-top: 1rpx solid #edf0ee; color: #8b9792; font-size: 23rpx; }.amount { margin-left: auto; color: #b94f3c; font-size: 30rpx; font-weight: 700; }.arrow { margin-left: 18rpx; color: #a4afab; font-size: 38rpx; }.empty,.loading { padding-top: 220rpx; display: flex; flex-direction: column; align-items: center; color: #8c9b96; font-size: 26rpx; }.icon { margin-bottom: 24rpx; color: #b5c0bc; font-size: 90rpx; }
</style>
