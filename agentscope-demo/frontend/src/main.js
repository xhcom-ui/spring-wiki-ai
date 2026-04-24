import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import axios from 'axios'

// 创建Vue应用
const app = createApp(App)

// 创建axios实例
const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 从localStorage获取token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    return response
  },
  error => {
    console.error('响应错误:', error)
    return Promise.reject(error)
  }
)

// 全局注册axios
app.config.globalProperties.$axios = request

// 全局错误处理
app.config.errorHandler = (err, instance, info) => {
  console.error('全局错误:', err)
  console.error('错误信息:', info)
}

// 全局属性
app.config.globalProperties.$filters = {
  formatDate(date) {
    if (!date) return ''
    const d = new Date(date)
    return d.toLocaleString()
  }
}

// 挂载应用
app.mount('#app')

console.log('AgentScope Demo 应用初始化完成')
