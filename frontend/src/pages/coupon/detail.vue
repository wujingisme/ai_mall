<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onShareAppMessage } from '@dcloudio/uni-app'
import { couponApi } from '@/api/coupon'
import type { CouponShareCreated, UserCoupon } from '@/types/coupon'

const coupon = ref<UserCoupon>()
const share = ref<CouponShareCreated>()
const generating = ref(false)
function formatDate(value: string) { return new Date(value).toLocaleString('zh-CN', { hour12: false }) }
async function createShare() { if (!coupon.value || generating.value) return; try { generating.value = true; share.value = await couponApi.createShare(coupon.value.id); uni.showToast({ title: '分享已生成，请发送给好友', icon: 'none' }) } finally { generating.value = false } }
onLoad(async (query) => { if (query?.id) coupon.value = await couponApi.getMine(String(query.id)) })
onShareAppMessage(() => ({ title: coupon.value ? `送你一张${coupon.value.name}` : '送你一张优惠券', path: share.value?.sharePath || '/pages/product/list' }))
</script>

<template>
  <view v-if="coupon" class="page"><view class="card"><text class="name">{{ coupon.name }}</text><view class="amount"><text>¥</text>{{ Number(coupon.discountAmount).toFixed(2) }}</view><text class="rule">满 ¥{{ Number(coupon.minimumSpend).toFixed(2) }} 可使用</text><view class="line" /><view class="row"><text>状态</text><text>{{ coupon.status === 'AVAILABLE' ? '可使用' : coupon.status === 'USED' ? '已使用' : '已过期' }}</text></view><view class="row"><text>生效时间</text><text>{{ formatDate(coupon.validFrom) }}</text></view><view class="row"><text>失效时间</text><text>{{ formatDate(coupon.validUntil) }}</text></view><view class="row"><text>获得方式</text><text>{{ coupon.source === 'MANUAL' ? '活动发放' : '好友分享' }}</text></view></view><button v-if="coupon.status === 'AVAILABLE' && !share" class="share-button" :loading="generating" @click="createShare">生成好友分享</button><button v-if="share" class="share-button" open-type="share">发送给微信好友</button><view class="tip">每个分享链接限一位其他用户领取；能否分享及领取由后端最终校验。订单抵扣功能尚未开放。</view></view>
</template>

<style scoped>
.page{min-height:100vh;padding:32rpx 24rpx;background:#f6f7f4}.card{padding:42rpx 34rpx;border-radius:26rpx;background:#fff;box-shadow:0 10rpx 35rpx rgba(35,79,64,.07)}.name{display:block;text-align:center;color:#203b32;font-size:34rpx;font-weight:700}.amount{margin-top:28rpx;text-align:center;color:#b94f3c;font-size:76rpx;font-weight:760}.amount text{font-size:30rpx}.rule{display:block;text-align:center;color:#6f827b;font-size:25rpx}.line{margin:38rpx 0;border-top:1rpx dashed #d9e2de}.row{padding:14rpx 0;display:flex;justify-content:space-between;color:#82918c;font-size:24rpx}.row text:last-child{max-width:68%;text-align:right;color:#314c43}.share-button{margin-top:28rpx;color:#fff;border-radius:18rpx;background:#245b4a;font-size:28rpx}.tip{margin-top:20rpx;padding:24rpx;color:#7f8e89;font-size:23rpx;line-height:1.7}
</style>
