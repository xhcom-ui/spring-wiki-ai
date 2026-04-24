const ADMIN_ROLE = 'admin'

export function getStoredUser() {
  const raw = localStorage.getItem('user')
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw)
  } catch (error) {
    console.warn('解析用户信息失败', error)
    return null
  }
}

export function getUserRole(user = getStoredUser()) {
  return user?.role || ''
}

export function hasRole(role, user = getStoredUser()) {
  return getUserRole(user) === role
}

export function isAdmin(user = getStoredUser()) {
  return hasRole(ADMIN_ROLE, user)
}

export function clearAuthSession() {
  localStorage.removeItem('so-token')
  localStorage.removeItem('user')
}

export function getFirstAccessibleHome(user = getStoredUser()) {
  return isAdmin(user) ? '/home/datasource' : '/home/dashboard'
}

export function canAccessRoles(roles, user = getStoredUser()) {
  if (!roles || !roles.length) {
    return true
  }
  const role = getUserRole(user)
  return Boolean(role) && roles.includes(role)
}
