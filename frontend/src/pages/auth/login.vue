<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { authApi } from '@/api/auth'
import { saveAuthSession } from '@/utils/auth-storage'

const account = ref('')
const password = ref('')
const remember = ref(true)
const showPassword = ref(false)
const submitting = ref(false)
const redirectPath = ref('')

onLoad((query) => {
  let candidate = ''
  try { candidate = typeof query?.redirect === 'string' ? decodeURIComponent(query.redirect) : '' } catch { candidate = '' }
  // 只接受应用内绝对路径，避免登录后被构造为外部开放重定向。
  redirectPath.value = candidate.startsWith('/') && !candidate.startsWith('//') ? candidate : ''
})

const canSubmit = computed(() => account.value.trim().length > 0 && password.value.length >= 8)

function toggleRemember() {
  remember.value = !remember.value
}

async function submit() {
  if (!account.value.trim()) {
    uni.showToast({ title: '请输入账号', icon: 'none' })
    return
  }
  if (password.value.length < 8) {
    uni.showToast({ title: '密码至少需要 8 位', icon: 'none' })
    return
  }

  try {
    submitting.value = true
    // 必须以接口返回为准保存会话，不能只跳转页面模拟登录成功。
    const session = await authApi.login({
      username: account.value.trim(),
      password: password.value,
    })
    saveAuthSession(session, remember.value)
    uni.showToast({ title: '登录成功', icon: 'success' })
    const target = redirectPath.value || '/pages/profile/index'
    const tabPages = ['/pages/product/list', '/pages/cart/index', '/pages/profile/index']
    if (tabPages.includes(target.split('?')[0])) uni.switchTab({ url: target })
    else uni.redirectTo({ url: target })
  } catch {
    // 请求工具已经展示后端错误信息，这里只负责恢复按钮状态。
  } finally {
    submitting.value = false
  }
}

function forgotPassword() {
  uni.showToast({ title: '请联系管理员重置密码', icon: 'none' })
}

function goToRegister() {
  uni.navigateTo({ url: '/pages/auth/register' })
}
</script>

<template>
  <view class="login-page">
    <view class="ambient ambient-one" />
    <view class="ambient ambient-two" />

    <view class="login-shell">
      <view class="brand-panel">
        <view class="brand-mark">
          <text class="brand-letter">A</text>
        </view>
        <text class="brand-name">AI MALL</text>
        <text class="brand-title">发现心仪好物，<br />享受品质生活。</text>
        <text class="brand-description">精选丰富商品与安心服务，让每一次购物都简单愉快。</text>

        <view class="feature-list">
          <view class="feature-item">
            <text class="feature-icon">✓</text>
            <text>严选品质好物</text>
          </view>
          <view class="feature-item">
            <text class="feature-icon">✓</text>
            <text>便捷购物与安心售后</text>
          </view>
        </view>
      </view>

      <view class="form-panel">
        <view class="mobile-brand">
          <view class="mobile-mark">A</view>
          <text>AI MALL</text>
        </view>

        <view class="form-heading">
          <text class="eyebrow">WELCOME BACK</text>
          <text class="form-title">欢迎回来</text>
          <text class="form-subtitle">登录 AI Mall，继续发现你的心仪好物。</text>
        </view>

        <view class="form-field">
          <text class="field-label">账号</text>
          <view class="input-wrap">
            <text class="input-icon">@</text>
            <input
              v-model="account"
              class="field-input"
              placeholder="请输入手机号或用户名"
              placeholder-class="placeholder"
              confirm-type="next" />
          </view>
        </view>

        <view class="form-field password-field">
          <text class="field-label">密码</text>
          <view class="input-wrap">
            <text class="input-icon lock-icon">●</text>
            <input
              v-model="password"
              class="field-input password-input"
              :password="!showPassword"
              placeholder="请输入登录密码"
              placeholder-class="placeholder"
              confirm-type="done"
              @confirm="submit" />
            <button class="visibility-button" @click="showPassword = !showPassword">
              {{ showPassword ? '隐藏' : '显示' }}
            </button>
          </view>
        </view>

        <view class="form-options">
          <view class="remember" @click="toggleRemember">
            <view :class="['checkbox', { checked: remember }]">{{ remember ? '✓' : '' }}</view>
            <text>记住登录状态</text>
          </view>
          <text class="forgot" @click="forgotPassword">忘记密码？</text>
        </view>

        <button
          :class="['login-button', { ready: canSubmit }]"
          :loading="submitting"
          :disabled="submitting"
          @click="submit">
          {{ submitting ? '正在登录' : '登录' }}
        </button>

        <view class="security-tip">
          <text class="shield">◆</text>
          <text>您的登录信息已加密保护</text>
        </view>

        <view class="register-entry">
          <text>还没有账号？</text>
          <text class="register-link" @click="goToRegister">立即注册</text>
        </view>
      </view>
    </view>

    <text class="copyright">© 2026 AI Mall · 品质生活商城</text>
  </view>
</template>

<style scoped>
.login-page {
  position: relative;
  box-sizing: border-box;
  min-height: 100vh;
  padding: 48rpx 32rpx 96rpx;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f6f4;
  color: #17362d;
}

.ambient {
  position: absolute;
  border-radius: 999rpx;
  filter: blur(4rpx);
  opacity: 0.72;
}

.ambient-one {
  width: 520rpx;
  height: 520rpx;
  top: -240rpx;
  right: -170rpx;
  background: rgba(196, 224, 198, 0.65);
}

.ambient-two {
  width: 440rpx;
  height: 440rpx;
  bottom: -220rpx;
  left: -160rpx;
  background: rgba(226, 199, 145, 0.28);
}

.login-shell {
  position: relative;
  z-index: 1;
  display: flex;
  width: min(1080px, 94vw);
  min-height: 660px;
  overflow: hidden;
  border: 1px solid rgba(30, 75, 61, 0.1);
  border-radius: 34px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 32px 80px rgba(28, 59, 49, 0.14);
}

.brand-panel,
.form-panel {
  box-sizing: border-box;
  width: 50%;
}

.brand-panel {
  position: relative;
  padding: 64px 58px;
  overflow: hidden;
  color: #f8fbf8;
  background: linear-gradient(145deg, #183f34 0%, #245d4d 62%, #326f5c 100%);
}

.brand-panel::after {
  content: '';
  position: absolute;
  width: 360px;
  height: 360px;
  right: -170px;
  bottom: -150px;
  border: 70px solid rgba(255, 255, 255, 0.06);
  border-radius: 50%;
}

.brand-mark {
  width: 54px;
  height: 54px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: #e4bd6d;
  box-shadow: 0 12px 28px rgba(8, 31, 24, 0.24);
}

.brand-letter {
  color: #173f34;
  font-size: 29px;
  font-weight: 800;
}

.brand-name {
  display: block;
  margin-top: 18px;
  color: #e9cf99;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 5px;
}

.brand-title {
  display: block;
  margin-top: 84px;
  font-size: 42px;
  font-weight: 700;
  line-height: 1.34;
  letter-spacing: 1px;
}

.brand-description {
  display: block;
  max-width: 360px;
  margin-top: 24px;
  color: rgba(244, 251, 247, 0.68);
  font-size: 16px;
  line-height: 1.8;
}

.feature-list {
  margin-top: 70px;
}

.feature-item {
  display: flex;
  align-items: center;
  margin-top: 16px;
  color: rgba(247, 251, 249, 0.82);
  font-size: 14px;
}

.feature-icon {
  width: 22px;
  height: 22px;
  margin-right: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #173f34;
  background: #e4bd6d;
  font-size: 12px;
  font-weight: 800;
}

.form-panel {
  padding: 76px 72px 56px;
}

.mobile-brand {
  display: none;
}

.eyebrow,
.form-title,
.form-subtitle,
.field-label {
  display: block;
}

.eyebrow {
  color: #b5893f;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 3px;
}

.form-title {
  margin-top: 15px;
  color: #17362d;
  font-size: 36px;
  font-weight: 700;
}

.form-subtitle {
  margin-top: 13px;
  color: #7b8e87;
  font-size: 15px;
}

.form-field {
  margin-top: 42px;
}

.password-field {
  margin-top: 24px;
}

.field-label {
  margin-bottom: 10px;
  color: #28463c;
  font-size: 14px;
  font-weight: 600;
}

.input-wrap {
  height: 56px;
  padding: 0 17px;
  display: flex;
  align-items: center;
  border: 1px solid #dce6e1;
  border-radius: 14px;
  background: #f9fbfa;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input-wrap:focus-within {
  border-color: #3d7865;
  box-shadow: 0 0 0 4px rgba(61, 120, 101, 0.1);
}

.input-icon {
  width: 24px;
  color: #779188;
  font-size: 17px;
  font-weight: 600;
}

.lock-icon {
  font-size: 10px;
}

.field-input {
  flex: 1;
  height: 56px;
  color: #17362d;
  font-size: 15px;
}

.placeholder {
  color: #a9b7b2;
}

.visibility-button {
  width: auto;
  margin: 0;
  padding: 4px 0 4px 12px;
  color: #688078;
  background: transparent;
  font-size: 13px;
  line-height: 1.4;
}

.form-options {
  margin-top: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}

.remember {
  display: flex;
  align-items: center;
  color: #64776f;
  cursor: pointer;
}

.checkbox {
  box-sizing: border-box;
  width: 19px;
  height: 19px;
  margin-right: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #bbc9c3;
  border-radius: 5px;
  color: #fff;
  font-size: 12px;
}

.checkbox.checked {
  border-color: #28604f;
  background: #28604f;
}

.forgot {
  color: #326b59;
  font-weight: 600;
  cursor: pointer;
}

.login-button {
  height: 56px;
  margin-top: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  color: #fff;
  background: #245848;
  box-shadow: 0 12px 24px rgba(36, 88, 72, 0.2);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
}

.login-button.ready {
  background: linear-gradient(135deg, #2b6955, #1d4b3d);
}

.security-tip {
  margin-top: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #98a7a1;
  font-size: 12px;
}

.register-entry {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #82928c;
  font-size: 13px;
}

.register-link {
  margin-left: 6px;
  color: #326b59;
  font-weight: 700;
  cursor: pointer;
}

.shield {
  margin-right: 7px;
  color: #b7c3be;
  font-size: 9px;
}

.copyright {
  position: absolute;
  z-index: 1;
  bottom: 24px;
  color: #82928c;
  font-size: 12px;
}

@media (max-width: 760px) {
  .login-page {
    min-height: 100vh;
    padding: 24px 20px 64px;
    align-items: center;
  }

  .login-shell {
    width: 100%;
    min-height: auto;
    border-radius: 26px;
  }

  .brand-panel {
    display: none;
  }

  .form-panel {
    width: 100%;
    padding: 36px 26px 34px;
  }

  .mobile-brand {
    display: flex;
    align-items: center;
    color: #1d4b3d;
    font-size: 14px;
    font-weight: 800;
    letter-spacing: 3px;
  }

  .mobile-mark {
    width: 38px;
    height: 38px;
    margin-right: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 11px;
    color: #1d4b3d;
    background: #e4bd6d;
    font-size: 20px;
    letter-spacing: 0;
  }

  .form-heading {
    margin-top: 38px;
  }

  .form-title {
    font-size: 31px;
  }

  .form-subtitle {
    line-height: 1.65;
  }

  .form-field {
    margin-top: 34px;
  }

  .password-field {
    margin-top: 22px;
  }

  .form-options {
    font-size: 12px;
  }

  .copyright {
    bottom: 20px;
  }
}
</style>
