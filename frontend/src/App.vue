<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => console.info('AI Mall started'))
</script>

<style>
page {
  min-height: 100%;
  background: #f5f6f8;
  color: #1f2937;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}
button::after { border: none; }
</style>
