<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { productApi } from '@/api/product'
import type { ProductDetail } from '@/types/product'

const id = ref('')
const product = ref<ProductDetail>()

async function load() {
  if (id.value) product.value = await productApi.get(id.value)
}

function remove() {
  if (!product.value) return
  uni.showModal({
    title: '删除商品', content: `确定删除“${product.value.name}”吗？`, confirmColor: '#dc2626',
    success: async ({ confirm }) => {
      if (!confirm) return
      await productApi.remove(id.value)
      uni.showToast({ title: '删除成功' })
      setTimeout(() => uni.navigateBack(), 500)
    },
  })
}

onLoad((query) => { id.value = String(query?.id || '') })
onShow(load)
</script>

<template>
  <view v-if="product" class="page">
    <image v-if="product.imageUrl" class="hero" :src="product.imageUrl" mode="aspectFill" />
    <view v-else class="hero placeholder">暂无图片</view>
    <view class="panel">
      <view class="heading"><text class="name">{{ product.name }}</text><text :class="['status', product.status ? 'online' : 'offline']">{{ product.status ? '已上架' : '已下架' }}</text></view>
      <text class="price">¥{{ Number(product.price).toFixed(2) }}</text>
      <view class="row"><text>SKU</text><text>{{ product.sku }}</text></view>
      <view class="row"><text>库存</text><text>{{ product.stock }}</text></view>
      <view class="row"><text>创建时间</text><text>{{ product.createdAt.replace('T', ' ') }}</text></view>
      <view class="description"><text class="label">商品描述</text><text>{{ product.description || '暂无描述' }}</text></view>
    </view>
    <view class="footer">
      <button class="delete" @click="remove">删除</button>
      <button class="edit" @click="uni.navigateTo({ url: `/pages/product/form?id=${id}` })">编辑商品</button>
    </view>
  </view>
</template>

<style scoped>
.page { padding-bottom: 130rpx; }.hero { width: 100%; height: 600rpx; background: #e2e8f0; }.placeholder { display: flex; align-items: center; justify-content: center; color: #94a3b8; }
.panel { margin: 24rpx; padding: 30rpx; background: #fff; border-radius: 20rpx; }.heading, .row, .footer { display: flex; align-items: center; }.heading { justify-content: space-between; }.name { font-size: 38rpx; font-weight: 650; }.status { padding: 6rpx 16rpx; border-radius: 999rpx; }.online { color: #15803d; background: #dcfce7; }.offline { color: #64748b; background: #e2e8f0; }
.price { display: block; color: #dc2626; font-size: 44rpx; font-weight: 650; margin: 24rpx 0; }.row { justify-content: space-between; padding: 22rpx 0; border-top: 1rpx solid #e2e8f0; color: #475569; }.description { padding-top: 24rpx; border-top: 1rpx solid #e2e8f0; }.description text { display: block; line-height: 1.7; }.label { margin-bottom: 12rpx; font-weight: 600; }
.footer { position: fixed; left: 0; right: 0; bottom: 0; gap: 20rpx; padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom)); background: #fff; }.footer button { flex: 1; }.delete { color: #dc2626; background: #fee2e2; }.edit { color: #fff; background: #2563eb; }
</style>
