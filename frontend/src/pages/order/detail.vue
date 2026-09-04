<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { orderApi } from '@/api/order'
import type { OrderDetail, OrderStatus } from '@/types/order'
import { getAccessToken } from '@/utils/auth-storage'
import { navigateToLogin, ROUTES } from '@/utils/navigation'

const order = ref<OrderDetail | null>(null)
const orderId = ref('')
const loading = ref(false)

/** 详情页只展示后端保存的订单/商品快照，不重新读取商品当前名称或价格。 */
function statusLabel(value: OrderStatus) {
  return value === 'PENDING_PICKUP' ? '待取货' : value === 'PICKED_UP' ? '已取货' : '已取消'
}

function formatDate(value: string | null) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function load() {
  if (!getAccessToken()) {
    navigateToLogin(`${ROUTES.orderDetail}?id=${encodeURIComponent(orderId.value)}`)
    return
  }
  if (!orderId.value) return
  try {
    loading.value = true
    order.value = await orderApi.getMine(orderId.value)
  } catch {
    order.value = null
  } finally {
    loading.value = false
  }
}

function goOrders() {
  uni.navigateBack({ fail: () => uni.redirectTo({ url: ROUTES.orderList }) })
}

onLoad((options) => {
  orderId.value = String(options?.id || '')
  void load()
})
</script>

<template>
  <view class="page">
    <view v-if="loading" class="state">正在加载订单...</view>
    <template v-else-if="order">
      <view class="status-card"><view><text class="status-title">{{ statusLabel(order.status) }}</text><text class="status-tip">线上下单，线下到店取货</text></view><text class="status-mark">{{ order.status === 'PICKED_UP' ? '✓' : order.status === 'CANCELLED' ? '×' : '○' }}</text></view>
      <view class="section"><view class="row"><text>订单号</text><text>{{ order.orderNo }}</text></view><view class="row"><text>下单时间</text><text>{{ formatDate(order.createdAt) }}</text></view><view v-if="order.cancelledAt" class="row"><text>取消时间</text><text>{{ formatDate(order.cancelledAt) }}</text></view><view v-if="order.pickedUpAt" class="row"><text>取货时间</text><text>{{ formatDate(order.pickedUpAt) }}</text></view></view>
      <view class="section"><view class="section-title">商品明细</view><view v-for="item in order.items" :key="`${item.sku}-${item.productName}`" class="item"><view class="item-main"><text class="name">{{ item.productName }}</text><text class="quantity">× {{ item.quantity }}</text></view><view class="item-main"><text class="sku">{{ item.sku }}</text><text class="line-amount">¥{{ Number(item.lineAmount).toFixed(2) }}</text></view></view><view class="total-row"><text>订单合计</text><text class="total">¥{{ Number(order.totalAmount).toFixed(2) }}</text></view></view>
      <view class="section pickup"><view class="section-title">取货信息</view><text class="pickup-name">{{ order.pickupLocationName }}</text><text class="pickup-address">{{ order.pickupLocationAddress }}</text><text class="pickup-tip">当前版本不展示线上支付和取货码；取货核销功能将在后续阶段开放。</text></view>
    </template>
    <view v-else class="state"><text class="state-icon">!</text><text>订单不存在或暂时无法加载</text><button class="back-button" @click="goOrders">返回订单列表</button></view>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; padding: 24rpx 24rpx 48rpx; background: #f6f7f4; }.status-card { margin-bottom: 22rpx; padding: 34rpx 30rpx; display: flex; align-items: center; justify-content: space-between; border-radius: 22rpx; color: #fff; background: linear-gradient(145deg,#245b4a,#367b65); }.status-title,.status-tip { display: block; }.status-title { font-size: 39rpx; font-weight: 700; }.status-tip { margin-top: 10rpx; color: rgba(255,255,255,.72); font-size: 23rpx; }.status-mark { color: #e4c57d; font-size: 82rpx; font-weight: 700; }.section { margin-bottom: 22rpx; padding: 28rpx; border-radius: 22rpx; background: #fff; }.row { padding: 14rpx 0; display: flex; justify-content: space-between; gap: 24rpx; color: #8a9791; font-size: 24rpx; }.row text:last-child { max-width: 70%; color: #3e584d; text-align: right; word-break: break-all; }.section-title { margin-bottom: 18rpx; color: #203b32; font-size: 29rpx; font-weight: 650; }.item { padding: 21rpx 0; border-top: 1rpx solid #edf0ee; }.item-main { display: flex; align-items: center; justify-content: space-between; }.item-main + .item-main { margin-top: 12rpx; }.name { max-width: 70%; overflow: hidden; color: #314b42; font-size: 27rpx; text-overflow: ellipsis; white-space: nowrap; }.quantity,.sku { color: #8c9994; font-size: 23rpx; }.line-amount { color: #b94f3c; font-size: 28rpx; font-weight: 650; }.total-row { margin-top: 15rpx; padding-top: 22rpx; display: flex; justify-content: space-between; border-top: 1rpx solid #edf0ee; color: #687b73; font-size: 25rpx; }.total { color: #b94f3c; font-size: 36rpx; font-weight: 750; }.pickup-name,.pickup-address,.pickup-tip { display: block; }.pickup-name { color: #294d40; font-size: 28rpx; font-weight: 600; }.pickup-address { margin-top: 12rpx; color: #52675f; font-size: 25rpx; line-height: 1.5; }.pickup-tip { margin-top: 18rpx; color: #a17838; font-size: 22rpx; line-height: 1.5; }.state { padding-top: 220rpx; display: flex; flex-direction: column; align-items: center; color: #8c9b96; font-size: 26rpx; }.state-icon { margin-bottom: 24rpx; color: #aab8b2; font-size: 86rpx; }.back-button { margin-top: 30rpx; padding: 0 48rpx; border-radius: 18rpx; color: #fff; background: #245b4a; font-size: 27rpx; }
</style>
