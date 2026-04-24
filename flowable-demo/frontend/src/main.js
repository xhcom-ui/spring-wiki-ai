import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import { ElMessage } from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import { setAuthFailureHandler } from './api/http'
import 'element-plus/dist/index.css'
import './style.css'

let lastAuthNoticeAt = 0

function showAuthNotice(message, type = 'warning') {
  const now = Date.now()
  if (now - lastAuthNoticeAt < 1500) {
    return
  }
  lastAuthNoticeAt = now
  ElMessage({
    type,
    duration: 2200,
    grouping: true,
    message
  })
}

setAuthFailureHandler(({ type, message }) => {
  if (type === 'unauthorized') {
    showAuthNotice(message || '登录已失效，请重新登录。', 'warning')
    router.replace({ name: 'login', query: { reason: 'expired' } })
    return
  }
  if (type === 'forbidden') {
    showAuthNotice(message || '当前账号没有访问该页面的权限。', 'error')
    router.replace({ name: 'dashboard', query: { denied: '1' } })
  }
})

const app = createApp(App)

Object.entries(ElementPlusIconsVue).forEach(([name, component]) => {
  app.component(name, component)
})

app.use(router)
app.use(ElementPlus)
app.mount('#app')
