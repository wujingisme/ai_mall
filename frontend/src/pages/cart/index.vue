<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import type { ShopProductDetail } from '@/types/shop'

interface CartItem extends ShopProductDetail { quantity: number }
const items = ref<CartItem[]>([])
const total = computed(() => items.value.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0))

// 页面每次显示时恢复本地购物车，保证详情页加入商品后立即可见。
function load() { items.value = uni.getStorageSync('mall_cart') || [] }
function save() { uni.setStorageSync('mall_cart', items.value) }
function change(item: CartItem, step: number) {
  item.quantity += step
  if (item.quantity <= 0) items.value = items.value.filter((current) => current.id !== item.id)
  save()
}
function checkout() { uni.showToast({ title: '结算功能即将上线', icon: 'none' }) }
function goShopping() { uni.switchTab({ url: '/pages/product/list' }) }
onShow(load)
</script>

<template>
  <view class="page">
    <view v-if="items.length">
      <view v-for="item in items" :key="item.id" class="item">
        <image v-if="item.imageUrl" class="image" :src="item.imageUrl" mode="aspectFill" />
        <view v-else class="image placeholder">AI</view>
        <view class="content"><text class="name">{{ item.name }}</text><text class="price">¥{{ Number(item.price).toFixed(2) }}</text><view class="quantity"><button @click="change(item, -1)">−</button><text>{{ item.quantity }}</text><button @click="change(item, 1)">＋</button></view></view>
      </view>
    </view>
    <view v-else class="empty"><text class="bag">◇</text><text>购物车还是空的</text><button @click="goShopping">去逛逛</button></view>
    <view v-if="items.length" class="footer"><view><text>合计：</text><text class="total">¥{{ total.toFixed(2) }}</text></view><button @click="checkout">去结算</button></view>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; padding: 24rpx 24rpx 150rpx; background: #f6f7f4; }.item { margin-bottom: 20rpx; padding: 22rpx; display: flex; border-radius: 22rpx; background: #fff; }.image { width: 180rpx; height: 180rpx; flex: none; border-radius: 18rpx; background: #e8edea; }.placeholder { display: flex; align-items: center; justify-content: center; color: #98a7a2; }.content { flex: 1; min-width: 0; margin-left: 22rpx; }.name { display: block; overflow: hidden; color: #203b32; font-size: 29rpx; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }.price { display: block; margin-top: 16rpx; color: #b94f3c; font-size: 30rpx; font-weight: 700; }.quantity { margin-top: 20rpx; display: flex; justify-content: flex-end; align-items: center; }.quantity button { width: 52rpx; height: 48rpx; margin: 0 15rpx; padding: 0; color: #245b4a; background: #edf2f0; line-height: 48rpx; }
.empty { padding-top: 220rpx; display: flex; flex-direction: column; align-items: center; color: #8c9b96; }.bag { margin-bottom: 24rpx; color: #b5c0bc; font-size: 100rpx; }.empty button { margin-top: 34rpx; padding: 0 48rpx; color: #fff; background: #245b4a; font-size: 26rpx; }.footer { position: fixed; left: 0; right: 0; bottom: var(--window-bottom); padding: 18rpx 24rpx; display: flex; align-items: center; justify-content: space-between; background: #fff; box-shadow: 0 -8rpx 28rpx rgba(34,65,55,.08); }.total { color: #b94f3c; font-size: 34rpx; font-weight: 700; }.footer button { width: auto; margin: 0; padding: 0 50rpx; color: #fff; background: #245b4a; font-size: 27rpx; }
</style>
