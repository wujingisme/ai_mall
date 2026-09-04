<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { cartApi } from '@/api/cart'
import { orderApi } from '@/api/order'
import type { OrderPreview } from '@/types/order'
import { getAccessToken } from '@/utils/auth-storage'
import { navigateToLogin, ROUTES } from '@/utils/navigation'

const preview = ref<OrderPreview | null>(null)
const loading = ref(false)
const emptyCart = ref(false)

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

/** 第 2 阶段才会真正创建订单；现在先明确告诉用户按钮尚未开放。 */
function submitOrder() {
  uni.showToast({ title: '提交订单功能即将上线', icon: 'none' })
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
      <button class="submit" @click="submitOrder">提交订单（即将开放）</button>
    </template>
    <view v-else-if="emptyCart" class="state"><text class="state-icon">◇</text><text>没有可结算的商品</text><button class="back-button" @click="goCart">返回购物车</button></view>
    <view v-else class="state"><text class="state-icon">!</text><text>订单预览暂时无法加载</text><button class="back-button" @click="load">重新加载</button></view>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; padding: 24rpx 24rpx 48rpx; background: #f6f7f4; }.section { margin-bottom: 22rpx; padding: 28rpx; border-radius: 22rpx; background: #fff; }.section-title { margin-bottom: 22rpx; color: #203b32; font-size: 29rpx; font-weight: 650; }.item { padding: 21rpx 0; border-top: 1rpx solid #edf0ee; }.item-main { display: flex; align-items: center; justify-content: space-between; }.item-main + .item-main { margin-top: 12rpx; }.name { max-width: 70%; overflow: hidden; color: #314b42; font-size: 27rpx; text-overflow: ellipsis; white-space: nowrap; }.quantity,.unit-price { color: #8c9994; font-size: 23rpx; }.line-amount { color: #b94f3c; font-size: 28rpx; font-weight: 650; }.pickup-name,.pickup-address,.pickup-tip { display: block; }.pickup-name { color: #294d40; font-size: 28rpx; font-weight: 600; }.pickup-address { margin-top: 12rpx; color: #52675f; font-size: 25rpx; line-height: 1.5; }.pickup-tip { margin-top: 18rpx; color: #a17838; font-size: 22rpx; line-height: 1.5; }.summary { padding: 24rpx 8rpx; display: flex; align-items: center; justify-content: space-between; color: #687b73; font-size: 25rpx; }.total { color: #b94f3c; font-size: 38rpx; font-weight: 750; }.submit,.back-button { border-radius: 18rpx; color: #fff; background: #245b4a; font-size: 27rpx; }.submit { margin-top: 12rpx; }.state { padding-top: 220rpx; display: flex; flex-direction: column; align-items: center; color: #8c9b96; font-size: 26rpx; }.state-icon { margin-bottom: 24rpx; color: #aab8b2; font-size: 86rpx; }.back-button { margin-top: 30rpx; padding: 0 48rpx; }
</style>
