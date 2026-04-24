import axios from 'axios'
import { ElMessage, ElLoading } from 'element-plus'
import router from '../router'
import { clearAuthSession, getFirstAccessibleHome } from './auth'

// 创建axios实例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  }
})

// 加载实例管理
const loadingMap = new Map()

// 请求拦截器
request.interceptors.request.use(
  config => {
    config.url = normalizeUrl(config.url)

    // 从localStorage获取token
    const token = localStorage.getItem('so-token')
    if (token) {
      // 在请求头中添加token
      config.headers['so-token'] = token
    }
    
    // 添加请求ID
    const requestId = generateRequestId()
    config.headers['X-Request-ID'] = requestId
    config.metadata = { requestId }
    
    // 显示加载状态
    if (config.loading !== false) {
      const loadingKey = requestId
      const loading = ElLoading.service({
        lock: true,
        text: '加载中...',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      loadingMap.set(loadingKey, loading)
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
    // 隐藏加载状态
    const loadingKey = response.config.metadata?.requestId
    if (loadingMap.has(loadingKey)) {
      loadingMap.get(loadingKey).close()
      loadingMap.delete(loadingKey)
    }


    const payload = response.data
    if (payload && typeof payload === 'object' && Object.prototype.hasOwnProperty.call(payload, 'code')) {
      if (payload.code === 200) {
        return toCompatiblePayload(payload.data ?? payload)
      } else {
        const message = payload.message || '请求失败'
        ElMessage.error(message)
        return Promise.reject(new Error(message))
      }
    }

    return toCompatiblePayload(payload)
  },
  error => {
    // 隐藏加载状态
    if (error.config) {
      const loadingKey = error.config.metadata?.requestId
      if (loadingMap.has(loadingKey)) {
        loadingMap.get(loadingKey).close()
        loadingMap.delete(loadingKey)
      }
    }
    
    console.error('响应错误:', error)
    
    // 处理网络错误
    if (!error.response) {
      ElMessage.error('网络连接失败，请检查网络设置')
      return Promise.reject(error)
    }
    
    // 处理401错误（未授权）
    if (error.response.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      clearAuthSession()
      router.push('/login')
    } else if (error.response.status === 403) {
      ElMessage.error('没有权限执行此操作')
      if (localStorage.getItem('so-token')) {
        router.push(getFirstAccessibleHome())
      }
    } else if (error.response.status === 404) {
      ElMessage.error('请求的资源不存在')
    } else if (error.response.status === 500) {
      ElMessage.error('服务器内部错误')
    } else {
      // 处理其他错误
      const message = error.response.data?.message || '请求失败'
      ElMessage.error(message)
    }
    
    return Promise.reject(error)
  }
)

// 生成请求ID
function generateRequestId() {
  return 'req_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}

function normalizeUrl(url = '') {
  if (!url) {
    return url
  }
  if (request.defaults.baseURL?.endsWith('/api') && url.startsWith('/api/')) {
    return url.slice(4)
  }
  return url
}

function toCompatiblePayload(payload) {
  if (payload !== null && typeof payload === 'object') {
    if (!Object.prototype.hasOwnProperty.call(payload, 'data')) {
      Object.defineProperty(payload, 'data', {
        value: payload,
        enumerable: false,
        configurable: true
      })
    }
    return payload
  }
  return {
    value: payload,
    data: payload
  }
}

export default request
