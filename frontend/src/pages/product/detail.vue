<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { shopApi } from '@/api/shop'
import { cartApi } from '@/api/cart'
import type { ShopProductDetail } from '@/types/shop'
import { getAccessToken } from '@/utils/auth-storage'

const id = ref('')
const product = ref<ShopProductDetail>()

async function load() {
  if (id.value) product.value = await shopApi.get(id.value)
}

async function addToCart(): Promise<boolean> {
  if (!product.value) return false
  if (product.value.soldOut) {
    uni.showToast({ title: '商品已售罄', icon: 'none' })
    return false
  }
  if (!getAccessToken()) {
    // 游客可以浏览商品，但购物车属于用户数据；登录后返回当前商品详情页。
    const current = `/pages/product/detail?id=${id.value}`
    uni.navigateTo({ url: `/pages/auth/login?redirect=${encodeURIComponent(current)}` })
    return false
  }
  try {
    await cartApi.add({ productId: product.value.id, quantity: 1 })
    uni.showToast({ title: '已加入购物车', icon: 'success' })
    return true
  } catch {
    return false
  }
}

async function buyNow() {
  if (product.value?.soldOut) {
    uni.showToast({ title: '商品已售罄', icon: 'none' })
    return
  }
  if (await addToCart()) uni.switchTab({ url: '/pages/cart/index' })
}

function goHome() {
  uni.switchTab({ url: '/pages/product/list' })
}

onLoad((query) => {
  id.value = String(query?.id || '')
  load()
})
</script>

<template>
  <view v-if="product" class="page">
    <image v-if="product.imageUrl" class="hero" :src="product.imageUrl" mode="aspectFill" />
    <view v-else class="hero placeholder">AI MALL</view>
    <view class="panel">
      <text class="name">{{ product.name }}</text>
      <text class="price"><text>¥</text>{{ Number(product.price).toFixed(2) }}</text>
      <view class="benefits"><text>正品保障</text><text>安心配送</text><text>售后无忧</text></view>
      <view class="description"><text class="label">商品详情</text><text class="copy">{{ product.description || '这件好物暂时没有更多介绍。' }}</text></view>
    </view>
    <view class="footer">
      <view class="home" @click="goHome"><text>⌂</text><text>首页</text></view>
      <button class="cart" :disabled="product.soldOut" @click="addToCart">加入购物车</button>
      <button class="buy" :disabled="product.soldOut" @click="buyNow">{{ product.soldOut ? '已售罄' : '立即购买' }}</button>
    </view>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; padding-bottom: 150rpx; background: #f6f7f4; }.hero { width: 100%; height: 720rpx; background: #e7ece9; }.placeholder { display: flex; align-items: center; justify-content: center; color: #95a49f; letter-spacing: 8rpx; }
.panel { position: relative; margin: -22rpx 20rpx 24rpx; padding: 34rpx 30rpx; border-radius: 28rpx; background: #fff; }.name { display: block; color: #203b32; font-size: 38rpx; font-weight: 650; line-height: 1.5; }.price { display: block; margin-top: 18rpx; color: #b94f3c; font-size: 46rpx; font-weight: 700; }.price text { margin-right: 5rpx; font-size: 26rpx; }
.benefits { margin-top: 28rpx; padding: 22rpx 0; display: flex; justify-content: space-around; border-top: 1rpx solid #edf0ee; border-bottom: 1rpx solid #edf0ee; color: #537168; font-size: 23rpx; }.benefits text::before { content: '✓'; margin-right: 7rpx; color: #c09a4d; }.description { margin-top: 30rpx; }.label,.copy { display: block; }.label { color: #203b32; font-size: 30rpx; font-weight: 650; }.copy { margin-top: 18rpx; color: #71827c; font-size: 27rpx; line-height: 1.8; }
.footer { position: fixed; z-index: 5; left: 0; right: 0; bottom: 0; padding: 16rpx 22rpx calc(16rpx + env(safe-area-inset-bottom)); display: flex; align-items: center; gap: 14rpx; background: #fff; box-shadow: 0 -8rpx 28rpx rgba(34,65,55,.08); }.footer button { margin: 0; border-radius: 18rpx; font-size: 27rpx; }.home { width: 80rpx; display: flex; flex-direction: column; align-items: center; color: #657871; font-size: 21rpx; }.home text:first-child { font-size: 34rpx; }.cart { flex: 1; color: #245b4a; background: #e8f0ed; }.buy { flex: 1; color: #fff; background: #245b4a; }
</style>
