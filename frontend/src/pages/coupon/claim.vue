<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { couponApi } from '@/api/coupon'
import type { CouponSharePreview } from '@/types/coupon'
import { getAccessToken } from '@/utils/auth-storage'
import { navigateToLogin, ROUTES } from '@/utils/navigation'

const token = ref(''); const preview = ref<CouponSharePreview>(); const loading = ref(true); const claiming = ref(false)
function formatDate(value: string) { return new Date(value).toLocaleString('zh-CN', { hour12: false }) }
async function claim() {
  if (!getAccessToken()) { navigateToLogin(`${ROUTES.couponClaim}?token=${encodeURIComponent(token.value)}`); return }
  try { claiming.value = true; const coupon = await couponApi.claimShare(token.value); uni.showModal({ title: '领取成功', content: `已获得“${coupon.name}”`, showCancel: false, success: () => uni.redirectTo({ url: `${ROUTES.couponDetail}?id=${coupon.id}` }) }) } finally { claiming.value = false }
}
onLoad(async (query) => { token.value = String(query?.token || ''); if (!token.value) { loading.value = false; return }; try { preview.value = await couponApi.resolveShare(token.value) } finally { loading.value = false } })
</script>
<template><view class="page"><view v-if="preview" class="share-card"><text class="gift">好友送你一张优惠券</text><text class="name">{{ preview.name }}</text><view class="amount"><text>¥</text>{{ Number(preview.discountAmount).toFixed(0) }}</view><text class="rule">满 {{ Number(preview.minimumSpend).toFixed(0) }} 可用</text><text class="expires">分享有效至 {{ formatDate(preview.expiresAt) }}</text><button :disabled="!preview.claimable" :loading="claiming" @click="claim">{{ preview.claimable ? '立即领取' : '分享已失效或已被领取' }}</button></view><view v-else-if="loading" class="state">正在打开好友分享...</view><view v-else class="state">分享不存在或已失效</view></view></template>
<style scoped>.page{min-height:100vh;padding:70rpx 36rpx;background:linear-gradient(160deg,#173f34,#2e6b58)}.share-card{padding:56rpx 34rpx;border-radius:34rpx;text-align:center;background:#fff}.share-card text{display:block}.gift{color:#96732f;font-size:25rpx}.name{margin-top:30rpx;color:#203b32;font-size:36rpx;font-weight:700}.amount{margin-top:22rpx;color:#b94f3c;font-size:92rpx;font-weight:780}.amount text{display:inline;font-size:32rpx}.rule{color:#60766e;font-size:26rpx}.expires{margin-top:38rpx;color:#94a19d;font-size:22rpx}.share-card button{margin-top:45rpx;color:#fff;border-radius:45rpx;background:#245b4a;font-size:29rpx}.share-card button[disabled]{background:#aab5b1}.state{padding-top:240rpx;text-align:center;color:#fff;font-size:28rpx}</style>
