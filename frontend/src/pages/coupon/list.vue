<script setup lang="ts">
import { ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { couponApi } from '@/api/coupon'
import type { UserCoupon, UserCouponStatus } from '@/types/coupon'
import { getAccessToken } from '@/utils/auth-storage'
import { navigateToLogin, ROUTES } from '@/utils/navigation'

const tabs: { label: string; value: UserCouponStatus }[] = [
  { label: '可使用', value: 'AVAILABLE' }, { label: '已使用', value: 'USED' }, { label: '已过期', value: 'EXPIRED' },
]
const status = ref<UserCouponStatus>('AVAILABLE')
const items = ref<UserCoupon[]>([])
const loading = ref(false)

function formatDate(value: string) {
  const date = new Date(value)
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
async function load() {
  if (!getAccessToken()) { navigateToLogin(ROUTES.coupons); return }
  try { loading.value = true; items.value = (await couponApi.listMine({ page: 1, pageSize: 100, status: status.value })).items }
  catch { items.value = [] }
  finally { loading.value = false; uni.stopPullDownRefresh() }
}
function switchStatus(value: UserCouponStatus) { if (status.value !== value) { status.value = value; void load() } }
function openDetail(id: string) { uni.navigateTo({ url: `${ROUTES.couponDetail}?id=${id}` }) }
onShow(load)
onPullDownRefresh(load)
</script>

<template>
  <view class="page">
    <view class="tabs"><view v-for="tab in tabs" :key="tab.value" :class="['tab', { active: status === tab.value }]" @click="switchStatus(tab.value)">{{ tab.label }}</view></view>
    <view v-if="items.length" class="list">
      <view v-for="item in items" :key="item.id" :class="['coupon', { muted: item.status !== 'AVAILABLE' }]" @click="openDetail(item.id)">
        <view class="amount"><text class="symbol">¥</text><text class="number">{{ Number(item.discountAmount).toFixed(0) }}</text><text class="threshold">满 {{ Number(item.minimumSpend).toFixed(0) }} 可用</text></view>
        <view class="info"><text class="name">{{ item.name }}</text><text class="source">{{ item.source === 'MANUAL' ? '活动发放' : '好友分享' }}</text><text class="validity">有效期至 {{ formatDate(item.validUntil) }}</text></view>
        <text class="arrow">›</text>
      </view>
    </view>
    <view v-else-if="!loading" class="empty"><text class="icon">◇</text><text>暂无{{ tabs.find(tab => tab.value === status)?.label }}优惠券</text></view>
    <view v-if="loading" class="loading">正在加载...</view>
  </view>
</template>

<style scoped>
.page{min-height:100vh;padding:24rpx;background:#f6f7f4}.tabs{display:flex;padding:8rpx;border-radius:18rpx;background:#fff}.tab{flex:1;padding:20rpx 0;text-align:center;color:#758780;font-size:27rpx}.tab.active{border-radius:14rpx;color:#fff;background:#245b4a;font-weight:600}.list{margin-top:22rpx}.coupon{min-height:190rpx;margin-bottom:20rpx;display:flex;align-items:center;border-radius:22rpx;overflow:hidden;background:#fff;box-shadow:0 8rpx 28rpx rgba(35,79,64,.06)}.amount{width:220rpx;align-self:stretch;display:flex;flex-wrap:wrap;align-content:center;justify-content:center;color:#fff;background:linear-gradient(145deg,#245b4a,#367b65)}.symbol{margin-top:16rpx;font-size:28rpx}.number{font-size:70rpx;font-weight:750}.threshold{width:100%;text-align:center;font-size:22rpx;opacity:.8}.info{flex:1;min-width:0;padding:28rpx}.info text{display:block}.name{overflow:hidden;color:#213c33;font-size:29rpx;font-weight:650;text-overflow:ellipsis;white-space:nowrap}.source{width:max-content;margin-top:14rpx;padding:5rpx 12rpx;border-radius:8rpx;color:#8a6a2d;background:#f7efd9;font-size:20rpx}.validity{margin-top:15rpx;color:#899792;font-size:21rpx}.arrow{padding-right:24rpx;color:#a4afab;font-size:44rpx}.muted{filter:grayscale(.8);opacity:.65}.empty,.loading{padding-top:220rpx;display:flex;flex-direction:column;align-items:center;color:#8c9b96;font-size:26rpx}.icon{margin-bottom:25rpx;font-size:90rpx}
</style>
