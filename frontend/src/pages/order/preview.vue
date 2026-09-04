<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { cartApi } from '@/api/cart'
import { orderApi } from '@/api/order'
import type { OrderCreateResponse, OrderPreview } from '@/types/order'
import { getAccessToken } from '@/utils/auth-storage'
import { navigateToLogin, ROUTES } from '@/utils/navigation'

const preview = ref<OrderPreview | null>(null)
const createdOrder = ref<OrderCreateResponse | null>(null)
const loading = ref(false)
const submitting = ref(false)
const emptyCart = ref(false)
/**
 * 一次点击提交对应一个稳定幂等键。
 *
 * <p>如果请求已经到达后端但响应在网络中丢失，用户再次点击时必须复用此键，
 * 后端才能返回原订单而不是再次预留库存。重新加载购物车时才重置它。</p>
 */
const clientRequestId = ref('')

/**
 * 读取购物车后再请求订单预览。
 *
 * <p>页面不从 URL 接收金额或商品详情，避免用户修改查询参数伪造结算内容。
 * 当前购物车没有“勾选部分商品”的交互，因此先将全部可用商品提交给后端。</p>
 */
async function load() {
  if (!getAccessToken()) {
    navigateToLogin(ROUTES.orderPreview)
    return
  }
  try {
    loading.value = true
    createdOrder.value = null
    clientRequestId.value = ''
    emptyCart.value = false
    const cart = await cartApi.get()
    const availableItems = cart.items.filter((item) => item.available)
    if (!availableItems.length) {
      preview.value = null
      emptyCart.value = true
      return
    }
    // 金额、商品名称和库存都由后端以数据库最新值重新计算，前端只提交 ID 与数量。
    preview.value = await orderApi.preview({
      items: availableItems.map((item) => ({ productId: item.productId, quantity: item.quantity })),
    })
  } catch {
    // request() 已负责提示网络/业务错误；这里清空旧结果，避免展示过期金额。
    preview.value = null
  } finally {
    loading.value = false
  }
}

/** 生成只在本地请求中使用的随机幂等键，不把用户信息拼进键中。 */
function createClientRequestId() {
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`
}

/**
 * 提交当前预览中的全部商品。
 *
 * <p>金额不从 preview 结果回传给后端，后端会在事务中再次读取商品并计算；
 * 前端只提交商品 ID、数量和幂等键。库存或购物车发生变化时，重新加载预览并生成新键。</p>
 */
async function submitOrder() {
  if (!preview.value || submitting.value || createdOrder.value) return
  if (preview.value.items.some((item) => !item.available)) {
    uni.showToast({ title: '商品库存已变化，请重新加载', icon: 'none' })
    await load()
    return
  }
  if (!clientRequestId.value) clientRequestId.value = createClientRequestId()
  try {
    submitting.value = true
    createdOrder.value = await orderApi.create({
      items: preview.value.items.map((item) => ({ productId: item.productId, quantity: item.quantity })),
      clientRequestId: clientRequestId.value,
    })
  } catch (error) {
    const code = (error as { code?: string } | null)?.code
    // 这两类错误说明本次预览已经过期，旧商品选择不能继续提交，刷新后生成新幂等键。
    if (code === 'ORDER_STOCK_INSUFFICIENT' || code === 'ORDER_RULE_INVALID') await load()
    // 极少数情况下本地键与已有请求冲突；清掉旧键并重新预览，避免按钮一直重复失败。
    if (code === 'ORDER_IDEMPOTENCY_CONFLICT') await load()
    // 网络错误不清空幂等键：用户再次点击时仍然能够安全复用同一次提交。
  } finally {
    submitting.value = false
  }
}

/** 创建成功后进入订单详情；取货码不会通过 URL 传递，避免出现在分享/历史记录中。 */
function openCreatedOrder() {
  if (!createdOrder.value) return
  uni.navigateTo({ url: `${ROUTES.orderDetail}?id=${encodeURIComponent(createdOrder.value.order.id)}` })
}

/** 返回我的订单列表，便于用户之后再次查看订单状态。 */
function openOrderList() {
  uni.redirectTo({ url: ROUTES.orderList })
}

function goCart() {
  uni.navigateBack({ fail: () => uni.switchTab({ url: ROUTES.cart }) })
}

function formatMoney(value: number) {
  return Number(value).toFixed(2)
}

onLoad(load)
</script>

<template>
  <view class="page">
    <view v-if="loading" class="state">正在计算订单...</view>
    <template v-else-if="createdOrder">
      <view class="success-card"><text class="success-title">订单提交成功</text><text class="success-order-no">订单号：{{ createdOrder.order.orderNo }}</text></view>
      <view class="pickup-code-card"><text class="pickup-code-label">到店取货码</text><text v-if="createdOrder.pickupCode" class="pickup-code">{{ createdOrder.pickupCode }}</text><text v-else class="pickup-code-missing">订单已存在，首次取货码未能恢复</text><text class="pickup-code-tip">取货码只展示这一次，请截图或记下后到取货点线下付款取货。</text></view>
      <view class="success-summary"><text>订单金额</text><text class="total">¥{{ formatMoney(createdOrder.order.totalAmount) }}</text></view>
      <button class="submit" @click="openCreatedOrder">查看订单详情</button><button class="secondary" @click="openOrderList">查看我的订单</button>
    </template>
    <template v-else-if="preview">
      <view class="section">
        <view class="section-title">商品清单</view>
        <view v-for="item in preview.items" :key="item.productId" class="item">
          <view class="item-main"><text class="name">{{ item.name }}</text><text class="quantity">× {{ item.quantity }}</text></view>
          <view class="item-main"><text class="unit-price">¥{{ formatMoney(item.unitPrice) }} / 件</text><text class="line-amount">¥{{ formatMoney(item.lineAmount) }}</text></view>
        </view>
      </view>
      <view class="section pickup">
        <view class="section-title">线下取货</view>
        <text class="pickup-name">{{ preview.pickupLocationName }}</text>
        <text class="pickup-address">{{ preview.pickupLocationAddress }}</text>
        <text class="pickup-tip">下单后请到此处取货，当前阶段暂不接入线上支付。</text>
      </view>
      <view class="summary"><text>共 {{ preview.totalQuantity }} 件</text><text class="total">¥{{ formatMoney(preview.totalAmount) }}</text></view>
      <button class="submit" :disabled="submitting" @click="submitOrder">{{ submitting ? '正在提交订单...' : '提交订单' }}</button>
    </template>
    <view v-else-if="emptyCart" class="state"><text class="state-icon">◇</text><text>没有可结算的商品</text><button class="back-button" @click="goCart">返回购物车</button></view>
    <view v-else class="state"><text class="state-icon">!</text><text>订单预览暂时无法加载</text><button class="back-button" @click="load">重新加载</button></view>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; padding: 24rpx 24rpx 48rpx; background: #f6f7f4; }.section { margin-bottom: 22rpx; padding: 28rpx; border-radius: 22rpx; background: #fff; }.section-title { margin-bottom: 22rpx; color: #203b32; font-size: 29rpx; font-weight: 650; }.item { padding: 21rpx 0; border-top: 1rpx solid #edf0ee; }.item-main { display: flex; align-items: center; justify-content: space-between; }.item-main + .item-main { margin-top: 12rpx; }.name { max-width: 70%; overflow: hidden; color: #314b42; font-size: 27rpx; text-overflow: ellipsis; white-space: nowrap; }.quantity,.unit-price { color: #8c9994; font-size: 23rpx; }.line-amount { color: #b94f3c; font-size: 28rpx; font-weight: 650; }.pickup-name,.pickup-address,.pickup-tip { display: block; }.pickup-name { color: #294d40; font-size: 28rpx; font-weight: 600; }.pickup-address { margin-top: 12rpx; color: #52675f; font-size: 25rpx; line-height: 1.5; }.pickup-tip { margin-top: 18rpx; color: #a17838; font-size: 22rpx; line-height: 1.5; }.summary,.success-summary { padding: 24rpx 8rpx; display: flex; align-items: center; justify-content: space-between; color: #687b73; font-size: 25rpx; }.total { color: #b94f3c; font-size: 38rpx; font-weight: 750; }.submit,.secondary,.back-button { border-radius: 18rpx; font-size: 27rpx; }.submit { margin-top: 12rpx; color: #fff; background: #245b4a; }.secondary { margin-top: 18rpx; color: #245b4a; background: #e8f0ec; }.success-card,.pickup-code-card { margin-bottom: 22rpx; padding: 34rpx 30rpx; border-radius: 22rpx; background: #fff; }.success-card { color: #fff; background: linear-gradient(145deg,#245b4a,#367b65); }.success-title,.success-order-no,.pickup-code-label,.pickup-code,.pickup-code-missing,.pickup-code-tip { display: block; }.success-title { font-size: 39rpx; font-weight: 700; }.success-order-no { margin-top: 13rpx; color: rgba(255,255,255,.76); font-size: 23rpx; }.pickup-code-card { text-align: center; }.pickup-code-label { color: #647970; font-size: 25rpx; }.pickup-code { margin-top: 16rpx; color: #245b4a; font-size: 62rpx; font-weight: 800; letter-spacing: 8rpx; }.pickup-code-missing { margin-top: 18rpx; color: #b94f3c; font-size: 29rpx; font-weight: 650; }.pickup-code-tip { margin-top: 18rpx; color: #a17838; font-size: 23rpx; line-height: 1.5; }.state { padding-top: 220rpx; display: flex; flex-direction: column; align-items: center; color: #8c9b96; font-size: 26rpx; }.state-icon { margin-bottom: 24rpx; color: #aab8b2; font-size: 86rpx; }.back-button { margin-top: 30rpx; padding: 0 48rpx; color: #fff; background: #245b4a; }
</style>
