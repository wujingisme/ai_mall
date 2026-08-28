<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import type { CurrentUser } from '@/types/auth'
import { authApi } from '@/api/auth'
import { clearAuthSession, getCurrentUser, getRefreshToken, updateCurrentUser } from '@/utils/auth-storage'

const user = ref<CurrentUser | null>(null)
const avatarText = computed(() => (user.value?.displayName || user.value?.username || 'A').slice(0, 1).toUpperCase())

// tabBar 页面会被缓存，因此每次进入“我的”页面都重新读取最新登录状态。
onShow(async () => {
  user.value = getCurrentUser()
  if (!user.value) return
  try {
    // 进入个人页时向后端校验登录状态，并以数据库中的最新用户资料覆盖本地缓存。
    user.value = await authApi.me()
    updateCurrentUser(user.value)
  } catch {
    // 401 的刷新及跳转由统一请求层处理。
  }
})

function goLogin() {
  if (!user.value) uni.navigateTo({ url: '/pages/auth/login' })
}
function comingSoon() { uni.showToast({ title: '功能即将上线', icon: 'none' }) }

function logout() {
  uni.showModal({
    title: '退出登录',
    content: '确定要退出当前账号吗？',
    success: async ({ confirm }) => {
      if (!confirm) return
      const refreshToken = getRefreshToken()
      try {
        if (refreshToken) await authApi.logout({ refreshToken })
      } finally {
        // 即使网络异常也清除本机会话；后端接口本身保持幂等。
        clearAuthSession()
        user.value = null
        uni.reLaunch({ url: '/pages/auth/login' })
      }
    },
  })
}
</script>

<template>
  <view class="page">
    <view class="profile" @click="goLogin"><view class="avatar">{{ avatarText }}</view><view><text class="name">{{ user?.displayName || user?.username || '登录 / 注册' }}</text><text class="hint">{{ user ? `账号：${user.username}` : '登录后享受完整购物体验' }}</text></view><text v-if="!user" class="arrow">›</text></view>
    <view class="orders"><view class="heading"><text>我的订单</text><text @click="comingSoon">全部订单 ›</text></view><view class="order-grid"><view @click="comingSoon"><text>◇</text><text>待付款</text></view><view @click="comingSoon"><text>▱</text><text>待发货</text></view><view @click="comingSoon"><text>▤</text><text>待收货</text></view><view @click="comingSoon"><text>☆</text><text>待评价</text></view></view></view>
    <view class="menu"><view @click="comingSoon"><text>收货地址</text><text>›</text></view><view @click="comingSoon"><text>优惠券</text><text>›</text></view><view @click="comingSoon"><text>收藏商品</text><text>›</text></view><view @click="comingSoon"><text>联系客服</text><text>›</text></view></view>
    <button v-if="user" class="logout-button" @click="logout">退出登录</button>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; padding: 38rpx 24rpx; background: #f6f7f4; }.profile { padding: 42rpx 30rpx; display: flex; align-items: center; border-radius: 26rpx; color: #fff; background: linear-gradient(145deg,#173f34,#2f6e5a); }.avatar { width: 112rpx; height: 112rpx; margin-right: 24rpx; display: flex; align-items: center; justify-content: center; border-radius: 50%; color: #245b4a; background: #e4c57d; font-size: 48rpx; font-weight: 800; }.name,.hint { display: block; }.name { font-size: 34rpx; font-weight: 650; }.hint { margin-top: 10rpx; color: rgba(255,255,255,.62); font-size: 23rpx; }.arrow { margin-left: auto; font-size: 48rpx; }
.orders,.menu { margin-top: 24rpx; border-radius: 22rpx; background: #fff; }.heading { padding: 28rpx; display: flex; justify-content: space-between; border-bottom: 1rpx solid #edf0ee; color: #243f36; font-size: 28rpx; font-weight: 600; }.heading text:last-child { color: #93a09c; font-size: 23rpx; font-weight: 400; }.order-grid { padding: 30rpx 12rpx; display: grid; grid-template-columns: repeat(4,1fr); }.order-grid view { display: flex; flex-direction: column; align-items: center; gap: 13rpx; color: #61756e; font-size: 23rpx; }.order-grid view text:first-child { color: #2d6654; font-size: 39rpx; }.menu { padding: 0 28rpx; }.menu view { padding: 29rpx 0; display: flex; justify-content: space-between; border-bottom: 1rpx solid #edf0ee; color: #344e45; font-size: 27rpx; }.menu view:last-child { border: 0; }.menu view text:last-child { color: #a4afab; }
.logout-button { margin-top: 30rpx; color: #b54235; border-radius: 18rpx; background: #fff; font-size: 28rpx; line-height: 88rpx; }
</style>
