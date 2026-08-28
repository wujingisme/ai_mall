<script setup lang="ts">
import { onLaunch, onShow } from '@dcloudio/uni-app'
import { enforceCurrentRoute } from '@/utils/route-guard'

onLaunch(() => console.info('AI Mall started'))
// 导航拦截器负责应用内跳转；onShow 再覆盖扫码、刷新或深链接直接进入受保护页的场景。
onShow(() => setTimeout(enforceCurrentRoute, 0))
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
