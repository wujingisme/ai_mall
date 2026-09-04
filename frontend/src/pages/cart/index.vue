<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { cartApi } from '@/api/cart'
import type { Cart, CartItem } from '@/types/cart'
import { ROUTES } from '@/utils/navigation'

const items = ref<CartItem[]>([])
const total = ref(0)
const loading = ref(false)

function useCart(cart: Cart) {
  items.value = cart.items
  total.value = Number(cart.totalAmount)
}

async function load() {
  try {
    loading.value = true
    useCart(await cartApi.get())
  } catch {
    // 统一请求层负责提示和登录失效跳转。
  } finally {
    loading.value = false
  }
}

async function change(item: CartItem, step: number) {
  if (loading.value) return
  try {
    loading.value = true
    const quantity = item.quantity + step
    useCart(quantity <= 0 ? await cartApi.remove(item.productId) : await cartApi.update(item.productId, quantity))
  } catch {
    await load()
  } finally {
    loading.value = false
  }
}
function checkout() {
  if (items.value.some((item) => !item.available)) {
    uni.showToast({ title: '请先移除失效或库存不足的商品', icon: 'none' })
    return
  }
  // 当前购物车没有勾选控件，第一版先把全部可用商品交给预览页；后续可再扩展为选择部分商品。
  uni.navigateTo({ url: ROUTES.orderPreview })
}
function goShopping() { uni.switchTab({ url: '/pages/product/list' }) }
onShow(load)
</script>

<template>
  <view class="page">
    <view v-if="items.length">
      <view v-for="item in items" :key="item.productId" :class="['item', { unavailable: !item.available }]">
        <image v-if="item.imageUrl" class="image" :src="item.imageUrl" mode="aspectFill" />
        <view v-else class="image placeholder">AI</view>
        <view class="content"><text class="name">{{ item.name }}</text><text class="price">¥{{ Number(item.price).toFixed(2) }}</text><text v-if="!item.available" class="invalid">商品失效或库存不足</text><view class="quantity"><button :disabled="loading" @click="change(item, -1)">−</button><text>{{ item.quantity }}</text><button :disabled="loading || item.quantity >= item.stock" @click="change(item, 1)">＋</button></view></view>
      </view>
    </view>
    <view v-else class="empty"><text class="bag">◇</text><text>购物车还是空的</text><button @click="goShopping">去逛逛</button></view>
    <view v-if="items.length" class="footer"><view><text>合计：</text><text class="total">¥{{ total.toFixed(2) }}</text></view><button @click="checkout">去结算</button></view>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; padding: 24rpx 24rpx 150rpx; background: #f6f7f4; }.item { margin-bottom: 20rpx; padding: 22rpx; display: flex; border-radius: 22rpx; background: #fff; }.image { width: 180rpx; height: 180rpx; flex: none; border-radius: 18rpx; background: #e8edea; }.placeholder { display: flex; align-items: center; justify-content: center; color: #98a7a2; }.content { flex: 1; min-width: 0; margin-left: 22rpx; }.name { display: block; overflow: hidden; color: #203b32; font-size: 29rpx; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }.price { display: block; margin-top: 16rpx; color: #b94f3c; font-size: 30rpx; font-weight: 700; }.quantity { margin-top: 20rpx; display: flex; justify-content: flex-end; align-items: center; }.quantity button { width: 52rpx; height: 48rpx; margin: 0 15rpx; padding: 0; color: #245b4a; background: #edf2f0; line-height: 48rpx; }
.unavailable { opacity: .68; }.invalid { display: block; margin-top: 8rpx; color: #b94f3c; font-size: 22rpx; }
.empty { padding-top: 220rpx; display: flex; flex-direction: column; align-items: center; color: #8c9b96; }.bag { margin-bottom: 24rpx; color: #b5c0bc; font-size: 100rpx; }.empty button { margin-top: 34rpx; padding: 0 48rpx; color: #fff; background: #245b4a; font-size: 26rpx; }.footer { position: fixed; left: 0; right: 0; bottom: var(--window-bottom); padding: 18rpx 24rpx; display: flex; align-items: center; justify-content: space-between; background: #fff; box-shadow: 0 -8rpx 28rpx rgba(34,65,55,.08); }.total { color: #b94f3c; font-size: 34rpx; font-weight: 700; }.footer button { width: auto; margin: 0; padding: 0 50rpx; color: #fff; background: #245b4a; font-size: 27rpx; }
</style>
