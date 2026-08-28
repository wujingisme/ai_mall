<script setup lang="ts">
import { computed, ref } from 'vue'
import { authApi } from '@/api/auth'
import { saveAuthSession } from '@/utils/auth-storage'

const username = ref('')
const displayName = ref('')
const password = ref('')
const confirmPassword = ref('')
const agreed = ref(false)
const showPassword = ref(false)
const submitting = ref(false)

const canSubmit = computed(
  () =>
    /^[A-Za-z0-9_]{3,64}$/.test(username.value) &&
    displayName.value.trim().length > 0 &&
    password.value.length >= 8 &&
    password.value === confirmPassword.value &&
    agreed.value,
)

async function submit() {
  if (!/^[A-Za-z0-9_]{3,64}$/.test(username.value)) {
    uni.showToast({ title: '用户名需为 3-64 位字母、数字或下划线', icon: 'none' })
    return
  }
  if (!displayName.value.trim()) {
    uni.showToast({ title: '请输入昵称', icon: 'none' })
    return
  }
  if (password.value.length < 8) {
    uni.showToast({ title: '密码至少需要 8 位', icon: 'none' })
    return
  }
  if (password.value !== confirmPassword.value) {
    uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
    return
  }
  if (!agreed.value) {
    uni.showToast({ title: '请阅读并同意服务协议', icon: 'none' })
    return
  }

  try {
    submitting.value = true
    const registerData = {
      username: username.value.trim(),
      password: password.value,
      displayName: displayName.value.trim(),
    }
    // 后端注册接口只创建账号；注册成功后再登录，以获得完整令牌并实现“注册并登录”。
    await authApi.register(registerData)
    const session = await authApi.login({ username: registerData.username, password: registerData.password })
    saveAuthSession(session, true)
    submitting.value = false
    uni.showToast({ title: '注册成功', icon: 'success' })
    uni.switchTab({ url: '/pages/profile/index' })
  } catch {
    // 请求工具已经展示用户名重复、参数错误或网络异常等后端提示。
  } finally {
    submitting.value = false
  }
}

function goBack() {
  uni.navigateBack({ fail: () => uni.reLaunch({ url: '/pages/auth/login' }) })
}

function showAgreement(name: string) {
  uni.showToast({ title: `${name}暂未配置`, icon: 'none' })
}
</script>

<template>
  <view class="register-page">
    <view class="ambient ambient-one" />
    <view class="ambient ambient-two" />

    <view class="register-shell">
      <view class="intro-panel">
        <view class="brand-row">
          <view class="brand-mark">A</view>
          <text class="brand-name">AI MALL</text>
        </view>
        <view class="intro-content">
          <text class="intro-kicker">START YOUR JOURNEY</text>
          <text class="intro-title">创建账号，<br />开启品质生活。</text>
          <text class="intro-copy">收藏心仪好物、管理订单，让每一次购物都更简单。</text>
        </view>
        <view class="steps">
          <view class="step active"><text class="step-number">1</text><text>创建账号</text></view>
          <view class="step-line" />
          <view class="step"><text class="step-number">2</text><text>挑选好物</text></view>
          <view class="step-line" />
          <view class="step"><text class="step-number">3</text><text>安心购物</text></view>
        </view>
      </view>

      <view class="form-panel">
        <button class="back-button" @click="goBack">‹ <text>返回登录</text></button>
        <view class="form-heading">
          <text class="form-title">注册新账号</text>
          <text class="form-subtitle">只需一分钟，即可开始使用 AI Mall</text>
        </view>

        <view class="field">
          <text class="label">用户名</text>
          <view class="input-wrap">
            <input v-model="username" class="input" maxlength="64" placeholder="3-64 位字母、数字或下划线" placeholder-class="placeholder" />
          </view>
        </view>

        <view class="field compact-field">
          <text class="label">昵称</text>
          <view class="input-wrap">
            <input v-model="displayName" class="input" maxlength="100" placeholder="请输入商城昵称" placeholder-class="placeholder" />
          </view>
        </view>

        <view class="field compact-field">
          <text class="label">设置密码</text>
          <view class="input-wrap">
            <input v-model="password" class="input" :password="!showPassword" maxlength="72" placeholder="至少 6 位，不限制字符类型" placeholder-class="placeholder" />
            <button class="text-button" @click="showPassword = !showPassword">{{ showPassword ? '隐藏' : '显示' }}</button>
          </view>
        </view>

        <view class="field compact-field">
          <text class="label">确认密码</text>
          <view class="input-wrap">
            <input v-model="confirmPassword" class="input" :password="!showPassword" placeholder="请再次输入密码" placeholder-class="placeholder" confirm-type="done" @confirm="submit" />
          </view>
        </view>

        <view class="agreement" @click="agreed = !agreed">
          <view :class="['checkbox', { checked: agreed }]">{{ agreed ? '✓' : '' }}</view>
          <text>我已阅读并同意</text>
          <text class="agreement-link" @click.stop="showAgreement('用户协议')">《用户协议》</text>
          <text>和</text>
          <text class="agreement-link" @click.stop="showAgreement('隐私政策')">《隐私政策》</text>
        </view>

        <button :class="['register-button', { ready: canSubmit }]" :loading="submitting" :disabled="submitting" @click="submit">
          {{ submitting ? '正在创建账号' : '注册并登录' }}
        </button>

        <view class="login-entry">
          <text>已有账号？</text>
          <text class="login-link" @click="goBack">直接登录</text>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.register-page {
  position: relative;
  box-sizing: border-box;
  min-height: 100vh;
  padding: 42rpx 30rpx;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f6f4;
  color: #17362d;
}

.ambient { position: absolute; border-radius: 999rpx; }
.ambient-one { width: 560rpx; height: 560rpx; top: -280rpx; left: -180rpx; background: rgba(190, 220, 197, 0.56); }
.ambient-two { width: 480rpx; height: 480rpx; right: -180rpx; bottom: -250rpx; background: rgba(228, 189, 109, 0.2); }

.register-shell {
  position: relative;
  z-index: 1;
  width: min(1080px, 94vw);
  min-height: 710px;
  display: flex;
  overflow: hidden;
  border: 1px solid rgba(30, 75, 61, 0.1);
  border-radius: 34px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 32px 80px rgba(28, 59, 49, 0.14);
}

.intro-panel, .form-panel { box-sizing: border-box; width: 50%; }

.intro-panel {
  padding: 58px 54px;
  display: flex;
  flex-direction: column;
  color: #f7fbf8;
  background: linear-gradient(150deg, #173e33, #28624f 70%, #397461);
}

.brand-row { display: flex; align-items: center; }
.brand-mark { width: 48px; height: 48px; display: flex; align-items: center; justify-content: center; border-radius: 14px; color: #173e33; background: #e4bd6d; font-size: 25px; font-weight: 800; }
.brand-name { margin-left: 14px; color: #ead19d; font-size: 14px; font-weight: 700; letter-spacing: 4px; }
.intro-content { margin-top: 88px; }
.intro-kicker { display: block; color: #e0bd78; font-size: 11px; font-weight: 700; letter-spacing: 3px; }
.intro-title { display: block; margin-top: 20px; font-size: 40px; font-weight: 700; line-height: 1.35; }
.intro-copy { display: block; max-width: 350px; margin-top: 22px; color: rgba(246, 251, 248, 0.67); font-size: 15px; line-height: 1.8; }

.steps { margin-top: auto; display: flex; align-items: center; }
.step { display: flex; flex-direction: column; align-items: center; gap: 8px; color: rgba(255, 255, 255, 0.45); font-size: 11px; white-space: nowrap; }
.step.active { color: #f2d79f; }
.step-number { width: 27px; height: 27px; display: flex; align-items: center; justify-content: center; border: 1px solid rgba(255, 255, 255, 0.28); border-radius: 50%; }
.step.active .step-number { border-color: #e4bd6d; color: #173e33; background: #e4bd6d; font-weight: 800; }
.step-line { flex: 1; height: 1px; margin: 0 10px 23px; background: rgba(255, 255, 255, 0.2); }

.form-panel { padding: 38px 70px 44px; }
.back-button, .text-button, .code-button { width: auto; margin: 0; padding: 0; background: transparent; line-height: 1.4; }
.back-button { display: flex; align-items: center; color: #6f827a; font-size: 22px; }
.back-button text { margin-left: 6px; font-size: 13px; }
.form-heading { margin-top: 25px; }
.form-title, .form-subtitle, .label { display: block; }
.form-title { font-size: 32px; font-weight: 700; }
.form-subtitle { margin-top: 10px; color: #81928c; font-size: 14px; }
.field { margin-top: 28px; }
.compact-field { margin-top: 18px; }
.label { margin-bottom: 8px; color: #29483e; font-size: 13px; font-weight: 600; }
.input-wrap { height: 52px; padding: 0 16px; display: flex; align-items: center; border: 1px solid #dce6e1; border-radius: 13px; background: #f9fbfa; transition: border-color 0.2s, box-shadow 0.2s; }
.input-wrap:focus-within { border-color: #3d7865; box-shadow: 0 0 0 4px rgba(61, 120, 101, 0.1); }
.prefix { color: #315b4d; font-size: 14px; font-weight: 600; }
.divider { width: 1px; height: 18px; margin: 0 13px; background: #d7e1dd; }
.input { flex: 1; height: 52px; color: #17362d; font-size: 14px; }
.placeholder { color: #a9b7b2; }
.text-button, .code-button { padding-left: 12px; color: #326b59; font-size: 12px; font-weight: 600; }
.code-button { padding-left: 16px; border-left: 1px solid #dce6e1; border-radius: 0; }
.agreement { margin-top: 19px; display: flex; align-items: center; flex-wrap: wrap; color: #7c8d87; font-size: 11px; line-height: 1.8; cursor: pointer; }
.checkbox { box-sizing: border-box; width: 18px; height: 18px; margin-right: 8px; display: flex; align-items: center; justify-content: center; border: 1px solid #b9c8c2; border-radius: 5px; color: #fff; font-size: 11px; }
.checkbox.checked { border-color: #28604f; background: #28604f; }
.agreement-link { color: #326b59; font-weight: 600; }
.register-button { height: 54px; margin-top: 24px; display: flex; align-items: center; justify-content: center; border-radius: 14px; color: #fff; background: #527a6d; box-shadow: 0 11px 24px rgba(36, 88, 72, 0.18); font-size: 15px; font-weight: 600; letter-spacing: 2px; }
.register-button.ready { background: linear-gradient(135deg, #2d6b57, #1c4a3c); }
.login-entry { margin-top: 18px; display: flex; justify-content: center; color: #82928c; font-size: 12px; }
.login-link { margin-left: 6px; color: #326b59; font-weight: 700; cursor: pointer; }

@media (max-width: 760px) {
  .register-page { padding: 20px 18px; align-items: flex-start; }
  .register-shell { width: 100%; min-height: auto; margin: 10px 0; border-radius: 26px; }
  .intro-panel { display: none; }
  .form-panel { width: 100%; padding: 28px 24px 32px; }
  .form-heading { margin-top: 22px; }
  .form-title { font-size: 28px; }
  .field { margin-top: 25px; }
  .compact-field { margin-top: 17px; }
}
</style>
