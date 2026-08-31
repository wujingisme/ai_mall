<script setup lang="ts">
import { ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { shopApi } from '@/api/shop'
import type { ShopProductListItem } from '@/types/shop'

const products = ref<ShopProductListItem[]>([])
const keyword = ref('')
const page = ref(1)
const totalPages = ref(1)
const loading = ref(false)
const activeKeyword = ref('')
let lastLoadedAt = 0
const CACHE_MAX_AGE_MS = 30_000

// 消费者只能进入商品详情，不暴露新增、编辑和删除等后台能力。
function goToDetail(id: string) {
  uni.navigateTo({ url: `/pages/product/detail?id=${id}` })
}

// 消费端固定只查询已上架商品；下拉刷新替换数据，触底时追加下一页。
async function load(options: { reset?: boolean; force?: boolean } = {}) {
  const { reset = false, force = false } = options
  if (loading.value || (!reset && page.value > totalPages.value)) return
  if (reset && !force && lastLoadedAt > 0 && Date.now() - lastLoadedAt < CACHE_MAX_AGE_MS) return
  loading.value = true
  try {
    if (reset) page.value = 1
    const result = await shopApi.list({
      page: page.value,
      pageSize: 20,
      keyword: activeKeyword.value || undefined,
    })
    products.value = reset ? result.items : [...products.value, ...result.items]
    totalPages.value = result.totalPages
    page.value += 1
    lastLoadedAt = Date.now()
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

function search() {
  activeKeyword.value = keyword.value.trim()
  void load({ reset: true, force: true })
}

onShow(() => load({ reset: true }))
onPullDownRefresh(() => load({ reset: true, force: true }))
onReachBottom(() => load())
</script>

<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">AI MALL</text>
      <text class="headline">发现生活里的好物</text>
      <text class="subtitle">精选品质商品，让每次选择都更简单</text>
      <view class="search-wrap">
        <text class="search-icon">⌕</text>
        <input v-model="keyword" class="search" placeholder="搜索你喜欢的商品" confirm-type="search" @confirm="search" />
        <button class="search-button" @click="search">搜索</button>
      </view>
    </view>

    <view class="section-heading">
      <view><text class="section-title">今日精选</text><text class="section-note">为你挑选的热门好物</text></view>
      <text class="refresh" @click="load({ reset: true, force: true })">换一批</text>
    </view>

    <view v-if="products.length" class="grid">
      <view v-for="item in products" :key="item.id" class="card" @click="goToDetail(item.id)">
        <image v-if="item.imageUrl" class="image" :src="item.imageUrl" mode="aspectFill" lazy-load />
        <view v-else class="image placeholder">AI MALL</view>
        <text v-if="item.soldOut" class="sold-out">已售罄</text>
        <view class="card-content">
          <text class="title">{{ item.name }}</text>
          <text class="delivery">品质精选 · 安心送达</text>
          <view class="meta"><text class="price"><text class="currency">¥</text>{{ Number(item.price).toFixed(2) }}</text><view class="cart-button">＋</view></view>
        </view>
      </view>
    </view>
    <view v-else-if="!loading" class="empty">没有找到相关商品</view>
    <view v-if="loading" class="loading">正在加载好物...</view>
    <view v-else-if="products.length && page > totalPages" class="loading">已经到底啦</view>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; padding-bottom: 40rpx; background: #f6f7f4; }
.hero { padding: 72rpx 32rpx 38rpx; color: #fff; background: linear-gradient(145deg, #163f34, #2f6e5a); }
.eyebrow,.headline,.subtitle { display: block; }.eyebrow { color: #e6c77f; font-size: 22rpx; font-weight: 700; letter-spacing: 8rpx; }.headline { margin-top: 20rpx; font-size: 48rpx; font-weight: 700; }.subtitle { margin-top: 12rpx; color: rgba(255,255,255,.68); font-size: 26rpx; }
.search-wrap { height: 88rpx; margin-top: 36rpx; padding: 0 12rpx 0 24rpx; display: flex; align-items: center; border-radius: 22rpx; background: #fff; }.search-icon { color: #80948d; font-size: 38rpx; }.search { flex: 1; height: 88rpx; margin-left: 14rpx; color: #17362d; font-size: 28rpx; }.search-button { width: auto; margin: 0; padding: 0 24rpx; color: #fff; border-radius: 16rpx; background: #d1a957; font-size: 25rpx; line-height: 64rpx; }
.section-heading { padding: 34rpx 28rpx 22rpx; display: flex; justify-content: space-between; align-items: flex-end; }.section-title,.section-note { display: block; }.section-title { color: #17362d; font-size: 36rpx; font-weight: 700; }.section-note { margin-top: 7rpx; color: #8b9994; font-size: 23rpx; }.refresh { color: #2c6956; font-size: 25rpx; }
.grid { padding: 0 24rpx; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20rpx; }.card { position: relative; overflow: hidden; border-radius: 22rpx; background: #fff; box-shadow: 0 8rpx 24rpx rgba(31,65,54,.06); }.image { width: 100%; height: 330rpx; background: #e8edea; }.placeholder { display: flex; align-items: center; justify-content: center; color: #9aa9a3; font-size: 22rpx; letter-spacing: 4rpx; }.sold-out { position: absolute; top: 16rpx; right: 16rpx; padding: 7rpx 14rpx; border-radius: 999rpx; color: #fff; background: rgba(45,55,51,.75); font-size: 21rpx; }.card-content { padding: 20rpx; }.title { display: block; overflow: hidden; color: #203b32; font-size: 28rpx; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }.delivery { display: block; margin-top: 9rpx; color: #9aa6a2; font-size: 21rpx; }.meta { margin-top: 18rpx; display: flex; align-items: center; justify-content: space-between; }.price { color: #b94f3c; font-size: 32rpx; font-weight: 700; }.currency { margin-right: 3rpx; font-size: 22rpx; }.cart-button { width: 46rpx; height: 46rpx; display: flex; align-items: center; justify-content: center; border-radius: 50%; color: #fff; background: #245b4a; font-size: 31rpx; }
.empty,.loading { padding: 90rpx 30rpx; text-align: center; color: #96a49f; font-size: 25rpx; }
</style>
