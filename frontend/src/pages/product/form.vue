<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { productApi } from '@/api/product'
import type { ProductStatus, ProductWriteRequest } from '@/types/product'

const id = ref('')
const saving = ref(false)
const isEdit = computed(() => Boolean(id.value))
const form = reactive({ sku: '', name: '', price: '', stock: '0', status: 1 as ProductStatus, imageUrl: '', description: '' })

async function load() {
  const data = await productApi.get(id.value)
  Object.assign(form, { ...data, price: String(data.price), stock: String(data.stock), imageUrl: data.imageUrl || '', description: data.description || '' })
}

function validate() {
  if (!form.sku.trim()) return '请输入 SKU'
  if (form.sku.trim().length > 64) return 'SKU 最多 64 个字符'
  if (!form.name.trim()) return '请输入商品名称'
  if (form.name.trim().length > 200) return '商品名称最多 200 个字符'
  if (!/^\d{1,8}(\.\d{1,2})?$/.test(form.price) || Number(form.price) < 0) return '请输入正确价格（最多两位小数）'
  if (!/^\d+$/.test(form.stock) || Number(form.stock) > 2147483647) return '请输入正确库存'
  if (form.imageUrl.length > 1000) return '图片地址最多 1000 个字符'
  return ''
}

async function submit() {
  const message = validate()
  if (message) return uni.showToast({ title: message, icon: 'none' })
  if (saving.value) return
  saving.value = true
  const payload: ProductWriteRequest = {
    sku: form.sku.trim(), name: form.name.trim(), price: Number(form.price), stock: Number(form.stock),
    status: form.status, imageUrl: form.imageUrl.trim() || undefined, description: form.description.trim() || undefined,
  }
  try {
    if (isEdit.value) await productApi.update(id.value, payload)
    else await productApi.create(payload)
    uni.showToast({ title: isEdit.value ? '保存成功' : '创建成功' })
    setTimeout(() => uni.navigateBack(), 500)
  } finally { saving.value = false }
}

function changeStatus(event: any) {
  form.status = Number(event.detail.value) as ProductStatus
}

onLoad((query) => {
  id.value = String(query?.id || '')
  uni.setNavigationBarTitle({ title: id.value ? '编辑商品' : '新增商品' })
  if (id.value) load()
})
</script>

<template>
  <view class="page">
    <view class="form">
      <view class="field"><text class="label">SKU <text class="required">*</text></text><input v-model="form.sku" maxlength="64" placeholder="例如 PHONE-001" /></view>
      <view class="field"><text class="label">商品名称 <text class="required">*</text></text><input v-model="form.name" maxlength="200" placeholder="请输入商品名称" /></view>
      <view class="field"><text class="label">价格 <text class="required">*</text></text><view class="input-prefix"><text>¥</text><input v-model="form.price" type="digit" placeholder="0.00" /></view></view>
      <view class="field"><text class="label">库存 <text class="required">*</text></text><input v-model="form.stock" type="number" placeholder="0" /></view>
      <view class="field"><text class="label">商品状态 <text class="required">*</text></text><radio-group class="radios" @change="changeStatus"><label><radio value="1" :checked="form.status === 1" color="#2563eb" />上架</label><label><radio value="0" :checked="form.status === 0" color="#2563eb" />下架</label></radio-group></view>
      <view class="field"><text class="label">主图 URL</text><input v-model="form.imageUrl" maxlength="1000" placeholder="https://example.com/image.jpg" /></view>
      <view class="field"><text class="label">商品描述</text><textarea v-model="form.description" auto-height maxlength="-1" placeholder="请输入商品描述" /></view>
    </view>
    <button class="submit" :loading="saving" :disabled="saving" @click="submit">{{ saving ? '保存中...' : isEdit ? '保存修改' : '创建商品' }}</button>
  </view>
</template>

<style scoped>
.page { padding: 24rpx; padding-bottom: 160rpx; }.form { overflow: hidden; padding: 0 28rpx; background: #fff; border-radius: 20rpx; box-shadow: 0 6rpx 24rpx rgba(15, 23, 42, .05); }.field { padding: 26rpx 0; border-bottom: 1rpx solid #e2e8f0; }.field:last-child { border-bottom: 0; }.label { display: block; margin-bottom: 16rpx; color: #334155; font-weight: 600; }.required { color: #dc2626; }.field input, .field textarea, .input-prefix { box-sizing: border-box; width: 100%; min-height: 76rpx; padding: 18rpx 20rpx; border-radius: 12rpx; background: #f8fafc; }.field textarea { min-height: 180rpx; }.input-prefix { display: flex; align-items: center; }.input-prefix input { flex: 1; min-height: auto; padding: 0 0 0 12rpx; }.radios { display: flex; gap: 60rpx; }.radios label { display: flex; align-items: center; gap: 8rpx; }.submit { position: fixed; left: 24rpx; right: 24rpx; bottom: calc(24rpx + env(safe-area-inset-bottom)); color: #fff; background: #2563eb; border-radius: 16rpx; }
</style>
