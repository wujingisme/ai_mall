<script setup lang="ts">
import { ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { productApi } from '@/api/product'
import type { ProductListItem, ProductStatus } from '@/types/product'

const products = ref<ProductListItem[]>([])
const keyword = ref('')
const status = ref<'' | ProductStatus>('')
const page = ref(1)
const totalPages = ref(1)
const loading = ref(false)

function goToForm(id?: string | number) {
  uni.navigateTo({ url: id === undefined ? '/pages/product/form' : `/pages/product/form?id=${id}` })
}

function goToDetail(id: string | number) {
  uni.navigateTo({ url: `/pages/product/detail?id=${id}` })
}

async function load(reset = false) {
  if (loading.value) return
  if (!reset && page.value > totalPages.value) return
  loading.value = true
  try {
    if (reset) page.value = 1
    const result = await productApi.list({
      page: page.value,
      pageSize: 20,
      keyword: keyword.value.trim() || undefined,
      status: status.value === '' ? undefined : status.value,
    })
    products.value = reset ? result.items : [...products.value, ...result.items]
    totalPages.value = result.totalPages
    page.value += 1
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

function selectStatus(event: any) {
  const values: Array<'' | ProductStatus> = ['', 1, 0]
  status.value = values[Number(event.detail.value)]
  load(true)
}

function confirmDelete(item: ProductListItem) {
  uni.showModal({
    title: '删除商品',
    content: `确定删除“${item.name}”吗？`,
    confirmColor: '#dc2626',
    success: async ({ confirm }) => {
      if (!confirm) return
      await productApi.remove(item.id)
      uni.showToast({ title: '删除成功' })
      await load(true)
    },
  })
}

onShow(() => load(true))
onPullDownRefresh(() => load(true))
onReachBottom(() => load())
</script>

<template>
  <view class="page">
    <view class="toolbar">
      <view class="search-row">
        <input v-model="keyword" class="search" placeholder="搜索名称或 SKU" confirm-type="search" @confirm="load(true)" />
        <button class="search-button" size="mini" @click="load(true)">搜索</button>
      </view>
      <view class="filter-row">
        <picker :range="['全部状态', '已上架', '已下架']" @change="selectStatus">
          <view class="filter">{{ status === '' ? '全部状态' : status === 1 ? '已上架' : '已下架' }} ▾</view>
        </picker>
        <button class="add-button" size="mini" @click="goToForm()">新增商品</button>
      </view>
    </view>

    <view v-if="products.length" class="list">
      <view v-for="item in products" :key="item.id" class="card" @click="goToDetail(item.id)">
        <image v-if="item.imageUrl" class="image" :src="item.imageUrl" mode="aspectFill" />
        <view v-else class="image placeholder">暂无图片</view>
        <view class="content">
          <view class="title-row">
            <text class="title">{{ item.name }}</text>
            <text :class="['status', item.status === 1 ? 'online' : 'offline']">{{ item.status === 1 ? '上架' : '下架' }}</text>
          </view>
          <text class="sku">SKU：{{ item.sku }}</text>
          <view class="meta"><text class="price">¥{{ Number(item.price).toFixed(2) }}</text><text>库存 {{ item.stock }}</text></view>
          <view class="actions" @click.stop>
            <button size="mini" @click="goToForm(item.id)">编辑</button>
            <button class="delete" size="mini" @click="confirmDelete(item)">删除</button>
          </view>
        </view>
      </view>
    </view>
    <view v-else-if="!loading" class="empty">暂无商品，点击“新增商品”创建第一件商品</view>
    <view v-if="loading" class="loading">加载中...</view>
    <view v-else-if="products.length && page > totalPages" class="loading">没有更多了</view>
  </view>
</template>

<style scoped>
.page { padding: 24rpx; }
.toolbar, .card { background: #fff; border-radius: 20rpx; box-shadow: 0 6rpx 24rpx rgba(15, 23, 42, .06); }
.toolbar { padding: 24rpx; margin-bottom: 24rpx; }
.search-row, .filter-row, .title-row, .meta, .actions { display: flex; align-items: center; }
.search { flex: 1; height: 72rpx; padding: 0 22rpx; border-radius: 14rpx; background: #f1f5f9; }
.search-button, .add-button { margin: 0 0 0 16rpx; color: #fff; background: #2563eb; }
.filter-row { justify-content: space-between; margin-top: 20rpx; }
.filter { color: #475569; padding: 12rpx 0; }
.card { display: flex; padding: 20rpx; margin-bottom: 20rpx; }
.image { width: 180rpx; height: 180rpx; flex: none; border-radius: 16rpx; background: #e2e8f0; }
.placeholder { display: flex; align-items: center; justify-content: center; color: #94a3b8; font-size: 24rpx; }
.content { flex: 1; min-width: 0; margin-left: 22rpx; }
.title-row { justify-content: space-between; gap: 12rpx; }
.title { font-weight: 600; font-size: 32rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.status { flex: none; padding: 4rpx 12rpx; border-radius: 999rpx; font-size: 22rpx; }
.online { color: #15803d; background: #dcfce7; }.offline { color: #64748b; background: #e2e8f0; }
.sku { display: block; margin: 12rpx 0; color: #64748b; font-size: 24rpx; }
.meta { justify-content: space-between; color: #64748b; }.price { color: #dc2626; font-size: 32rpx; font-weight: 600; }
.actions { justify-content: flex-end; margin-top: 10rpx; }.actions button { margin: 0 0 0 12rpx; }.delete { color: #dc2626; }
.empty, .loading { padding: 100rpx 30rpx; text-align: center; color: #94a3b8; }
</style>
