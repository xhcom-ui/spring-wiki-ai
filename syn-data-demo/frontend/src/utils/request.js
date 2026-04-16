import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 创建axios实例
const request = axios.create({
  baseURL: '/',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 从localStorage获取token
    const token = localStorage.getItem('so-token')
    if (token) {
      // 在请求头中添加token
      config.headers['so-token'] = token
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
    
    // 处理401错误（未授权）
    if (error.response && error.response.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      // 清除localStorage中的token和用户信息
      localStorage.removeItem('so-token')
      localStorage.removeItem('user')
      // 重定向到登录页
      router.push('/login')
    } else {
      // 处理其他错误
      const message = error.response?.data?.message || '请求失败'
      ElMessage.error(message)
    }
    
    return Promise.reject(error)
  }
)

export default request
