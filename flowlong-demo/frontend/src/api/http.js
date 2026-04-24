import axios from 'axios'

const TOKEN_KEY = 'satoken'
const USER_KEY = 'flowlong_user'
let authFailureHandler = null

export const http = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    Accept: 'application/json'
  }
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.satoken = token
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = extractErrorMessage(error)
    error.normalizedMessage = message
    if (shouldHandleUnauthorized(error)) {
      clearToken()
      clearCurrentUser()
      authFailureHandler?.({
        type: 'unauthorized',
        message
      })
    } else if (shouldHandleForbidden(error)) {
      authFailureHandler?.({
        type: 'forbidden',
        message
      })
    }
    return Promise.reject(error)
  }
)

function extractErrorMessage(error) {
  const data = error?.response?.data
  if (typeof data === 'string' && data.trim()) {
    return data.trim()
  }
  return data?.error || data?.message || data?.msg || error.message || '请求失败'
}

function shouldHandleUnauthorized(error) {
  const status = error.response?.status
  const message = String(extractErrorMessage(error) || '')
  const normalizedMessage = message.toLowerCase().replace(/\s+/g, '')
  if (status === 401) {
    return true
  }
  return [
    '未登录',
    '请先登录',
    'token无效',
    'token 已过期',
    'token已过期',
    'token无效或已过期',
    'token无效，请重新登录',
    'token无效或已被踢下线',
    '未能读取到有效 token',
    '未能读取有效 token',
    '会话已过期',
    'login',
    'not login'
  ].some((keyword) => normalizedMessage.includes(keyword.toLowerCase().replace(/\s+/g, '')))
}

function shouldHandleForbidden(error) {
  const status = error.response?.status
  const message = String(extractErrorMessage(error) || '')
  if (status === 403) {
    return true
  }
  return message.includes('无权限访问')
}

export function saveToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  }
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function saveCurrentUser(user) {
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  }
}

export function getCurrentUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw)
  } catch (error) {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

export function clearCurrentUser() {
  localStorage.removeItem(USER_KEY)
}

function hasAuthSnapshot(user) {
  return Array.isArray(user?.permissions) && Array.isArray(user?.menuPaths)
}

export function hasPermission(permission) {
  if (!permission) {
    return true
  }
  const user = getCurrentUser()
  if (!user) {
    return false
  }
  if (user.isAdmin) {
    return true
  }
  if (!hasAuthSnapshot(user)) {
    return false
  }
  return Array.isArray(user.permissions) && user.permissions.includes(permission)
}

export function canAccessPath(path, permission) {
  const user = getCurrentUser()
  if (!user) {
    return false
  }
  if (user.isAdmin) {
    return true
  }
  if (!hasAuthSnapshot(user)) {
    return false
  }
  const menuPaths = Array.isArray(user.menuPaths) ? user.menuPaths : []
  return hasPermission(permission) || menuPaths.includes(path)
}

export function setAuthFailureHandler(handler) {
  authFailureHandler = typeof handler === 'function' ? handler : null
}
