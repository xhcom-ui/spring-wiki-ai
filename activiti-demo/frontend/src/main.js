import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import { setAuthFailureHandler } from './api/http'
import 'element-plus/dist/index.css'
import './style.css'

setAuthFailureHandler(async ({ type }) => {
  const current = router.currentRoute.value
  if (type === 'unauthorized') {
    if (current.name !== 'login') {
      await router.replace({ name: 'login', query: { reason: 'expired' } })
    }
    return
  }
  if (type === 'forbidden' && current.name !== 'dashboard') {
    await router.replace({ name: 'dashboard', query: { denied: '1' } })
  }
})

const app = createApp(App)

function toKebabCase(value) {
  return String(value || '')
    .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
    .replace(/_/g, '-')
    .toLowerCase()
}

Object.entries(ElementPlusIconsVue).forEach(([key, component]) => {
  app.component(key, component)
  app.component(`el-icon-${toKebabCase(key)}`, component)
})

app.use(ElementPlus)
app.use(router)
app.mount('#app')
