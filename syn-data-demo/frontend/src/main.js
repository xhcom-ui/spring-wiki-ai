import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from './utils/request'

// 创建Vue应用
const app = createApp(App)

// 全局注册axios
app.config.globalProperties.$axios = axios

// 全局错误处理
app.config.errorHandler = (err, instance, info) => {
  console.error('全局错误:', err)
  console.error('错误信息:', info)
  // 可以在这里添加错误上报等逻辑
}

// 全局属性
app.config.globalProperties.$filters = {
  formatDate(date) {
    if (!date) return ''
    const d = new Date(date)
    return d.toLocaleString()
  },
  formatStatus(status) {
    const statusMap = {
      0: '禁用',
      1: '启用',
      'success': '成功',
      'failed': '失败'
    }
    return statusMap[status] || status
  }
}

// 注册路由
app.use(router)

// 挂载应用
app.mount('#app')

console.log('应用初始化完成')
