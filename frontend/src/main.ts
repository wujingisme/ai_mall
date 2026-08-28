import { createSSRApp } from 'vue'
import App from './App.vue'
import { installRouteGuard } from '@/utils/route-guard'

installRouteGuard()

export function createApp() {
  const app = createSSRApp(App)
  return { app }
}
