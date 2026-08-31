<script setup lang="ts">
import { onLaunch, onShow } from '@dcloudio/uni-app'
import { enforceCurrentRoute } from '@/utils/route-guard'
import { getAccessToken } from '@/utils/auth-storage'
import { validateCurrentUser } from '@/utils/session-validation'

onLaunch(() => console.info('AI Mall started'))
// 恢复前台时在后台合并校验会话；受保护深链再由路由守卫决定是否跳转登录。
onShow(() => setTimeout(() => {
  if (getAccessToken()) void validateCurrentUser().catch(() => undefined)
  void enforceCurrentRoute()
}, 0))
</script>

<style>
page {
  min-height: 100%;
  background: #f5f6f8;
  color: #1f2937;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}
button::after {
  border: none;
}
</style>
