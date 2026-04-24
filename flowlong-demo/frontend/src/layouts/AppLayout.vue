<template>
  <div class="app-shell">
    <div v-if="sidebarOpen" class="sidebar-backdrop" @click="sidebarOpen = false"></div>

    <header class="shell-topbar">
      <div class="shell-topbar-left">
        <button type="button" class="menu-toggle shell-toggle" @click="sidebarOpen = !sidebarOpen">
          <span></span>
          <span></span>
          <span></span>
        </button>
        <strong class="shell-brand">Openflow</strong>
        <button type="button" class="shell-gear" aria-label="settings">
          <span></span>
        </button>
      </div>

      <div class="shell-topbar-right">
        <div class="shell-user-avatar">{{ userInitial }}</div>
        <div class="shell-user-meta">
          <strong>{{ currentUser?.nickname || currentUser?.username || '未登录用户' }}</strong>
          <small>{{ currentUser?.email || 'workflow operator' }}</small>
        </div>
        <button type="button" class="ghost-btn shell-logout-btn" @click="logout">退出</button>
      </div>
    </header>

    <aside :class="['sidebar-panel', { 'sidebar-open': sidebarOpen }]">
      <div class="sidebar-brand-block">
        <div class="sidebar-brand-mark">F</div>
        <div class="sidebar-brand-copy">
          <strong>FlowLong</strong>
          <small>Workflow Console</small>
        </div>
      </div>

      <nav class="sidebar-nav">
        <section v-for="group in menuGroups" :key="group.key" class="sidebar-group">
          <button
            type="button"
            :class="['sidebar-group-trigger', { 'sidebar-group-trigger-active': activeGroupKey === group.key }]"
            @click="toggleGroup(group.key)"
          >
            <span class="sidebar-group-main">
              <span :class="['sidebar-group-icon', `sidebar-group-icon-${group.icon}`]"></span>
              <span class="sidebar-group-title">{{ group.title }}</span>
            </span>
            <span :class="['sidebar-group-chevron', { 'sidebar-group-chevron-open': isGroupExpanded(group.key) }]"></span>
          </button>

          <div v-show="isGroupExpanded(group.key)" class="sidebar-group-body">
            <RouterLink
              v-for="menu in group.items"
              :key="menu.path"
              :to="menu.path"
              :class="[
                'sidebar-menu-item',
                {
                  'sidebar-menu-item-active': isMenuActive(menu.path),
                  'sidebar-menu-item-disabled': !menu.accessible
                }
              ]"
              @click="handleMenuClick($event, menu)"
            >
              <span class="sidebar-menu-marker"></span>
              <span class="sidebar-menu-text">{{ menu.name }}</span>
            </RouterLink>
          </div>
        </section>
      </nav>
    </aside>

    <main class="content-shell">
      <section class="route-bar">
        <div class="route-breadcrumb">
          <strong>{{ currentTitle }}</strong>
          <span>/</span>
          <span>{{ pageSummary }}</span>
        </div>
        <span class="route-time">{{ currentDateLabel }}</span>
      </section>
      <section class="page-shell">
        <RouterView />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { clearCurrentUser, clearToken, getCurrentUser, getToken, http, saveCurrentUser } from '../api/http'

const route = useRoute()
const router = useRouter()
const currentUser = ref(getCurrentUser())
const menus = ref([])
const sidebarOpen = ref(false)
const expandedGroups = ref([])

const currentTitle = computed(() => route.meta?.title || '工作台')
const currentDateLabel = computed(() =>
  new Date().toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
)
const userInitial = computed(() => {
  const source = currentUser.value?.nickname || currentUser.value?.username || 'U'
  return source.slice(0, 1).toUpperCase()
})
const menuPathSet = computed(() => new Set((menus.value || []).map((item) => item.path)))
const permissionSet = computed(() => new Set(currentUser.value?.permissions || []))

const menuCatalog = [
  {
    key: 'home',
    title: '首页',
    icon: 'home',
    items: [
      { name: '工作台首页', path: '/dashboard', permission: 'dashboard:view' }
    ]
  },
  {
    key: 'process-center',
    title: '流程中心',
    icon: 'process',
    items: [
      { name: '流程分类', path: '/designer', permission: 'process:design' },
      { name: '流程设计', path: '/designer/studio', permission: 'process:design' },
      { name: '发起流程', path: '/leave', permission: 'leave:submit' },
      { name: '待办审批', path: '/tasks', permission: 'task:approve' },
      { name: '流程监控', path: '/monitoring', permission: 'monitoring:view' },
      { name: '版本对比', path: '/versions', permission: 'process:compare' }
    ]
  },
  {
    key: 'org-center',
    title: '组织中心',
    icon: 'org',
    items: [
      { name: '用户管理', path: '/users', permission: 'user:manage' },
      { name: '角色管理', path: '/roles', permission: 'role:manage' },
      { name: '部门管理', path: '/departments', permission: 'department:manage' },
      { name: '岗位管理', path: '/posts', permission: 'post:manage' }
    ]
  },
  {
    key: 'settings',
    title: '系统设置',
    icon: 'settings',
    items: [
      { name: '菜单权限', path: '/menu-permissions', permission: 'menu:manage' },
      { name: '表单目录', path: '/form-catalogs', permission: 'form:manage' }
    ]
  }
]

const menuGroups = computed(() =>
  menuCatalog.map((group) => ({
    ...group,
    items: group.items.map((item) => ({
      ...item,
      accessible: canAccessMenu(item.path, item.permission)
    }))
  }))
)

const flatMenuItems = computed(() =>
  menuGroups.value.flatMap((group) => group.items.map((item) => ({ ...item, section: group.title })))
)

const activeGroupKey = computed(() =>
  menuGroups.value.find((group) => group.items.some((item) => isMenuActive(item.path)))?.key || menuGroups.value[0]?.key || ''
)

const pageSummary = computed(() => {
  const activeMenu = flatMenuItems.value.find((menu) => isMenuActive(menu.path))
  if (!activeMenu) {
    return '统一流程工作台'
  }
  return `${activeMenu.section} / ${activeMenu.name}`
})

function canAccessMenu(path, permission) {
  if (currentUser.value?.isAdmin) {
    return true
  }
  return menuPathSet.value.has(path) || permissionSet.value.has(permission)
}

function isMenuActive(path) {
  return route.path === path
}

function isGroupExpanded(key) {
  return expandedGroups.value.includes(key)
}

function toggleGroup(key) {
  if (isGroupExpanded(key)) {
    expandedGroups.value = expandedGroups.value.filter((item) => item !== key)
    return
  }
  expandedGroups.value = [...expandedGroups.value, key]
}

function syncExpandedGroups() {
  const visibleKeys = menuGroups.value.map((group) => group.key)
  const next = expandedGroups.value.filter((key) => visibleKeys.includes(key))
  if (activeGroupKey.value && !next.includes(activeGroupKey.value)) {
    next.push(activeGroupKey.value)
  }
  if (!next.length && visibleKeys.length) {
    expandedGroups.value = visibleKeys
    return
  }
  expandedGroups.value = next
}

function handleMenuClick(event, menu) {
  if (!menu?.accessible) {
    event.preventDefault()
    return
  }
  sidebarOpen.value = false
}

async function fetchCurrentUser() {
  const { data } = await http.get('/auth/current')
  currentUser.value = data
  saveCurrentUser(data)
}

async function fetchMenus() {
  const { data } = await http.get('/menu/all')
  menus.value = Array.isArray(data) ? data : []
}

async function logout() {
  try {
    await http.post('/auth/logout')
  } catch (error) {
    // ignore remote logout failure
  }
  clearToken()
  clearCurrentUser()
  router.replace('/login')
}

watch(
  () => route.fullPath,
  () => {
    sidebarOpen.value = false
    syncExpandedGroups()
  }
)

watch(menuGroups, () => {
  syncExpandedGroups()
})

onMounted(async () => {
  try {
    await Promise.all([fetchCurrentUser(), fetchMenus()])
    syncExpandedGroups()
  } catch (error) {
    const message = String(error?.normalizedMessage || error?.message || '')
    const authFailed = !getToken() || ['未登录', 'token', '会话已过期', 'login'].some((keyword) =>
      message.toLowerCase().includes(keyword.toLowerCase())
    )
    if (authFailed) {
      clearToken()
      clearCurrentUser()
      router.replace({ path: '/login', query: { reason: 'expired' } })
      return
    }
    currentUser.value = getCurrentUser()
    menus.value = []
    syncExpandedGroups()
  }
})
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: #e7ebf2;
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  grid-template-rows: 46px minmax(0, 1fr);
  grid-template-areas:
    "topbar topbar"
    "sidebar content";
}

.shell-topbar {
  grid-area: topbar;
  height: 46px;
  background: #fff;
  border-bottom: 1px solid #d6dde7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px 0 18px;
  position: sticky;
  top: 0;
  z-index: 30;
}

.shell-topbar-left,
.shell-topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.menu-toggle {
  width: 26px;
  height: 26px;
  border: 1px solid #d7deea;
  border-radius: 4px;
  background: #fff;
  padding: 0;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 3px;
}

.menu-toggle span {
  width: 13px;
  height: 1.5px;
  background: #617183;
}

.shell-brand {
  font-size: 13px;
  color: #2f3946;
  font-weight: 600;
}

.shell-gear {
  width: 20px;
  height: 20px;
  border: 0;
  background: transparent;
  padding: 0;
  position: relative;
}

.shell-gear span,
.shell-gear span::before,
.shell-gear span::after {
  content: '';
  position: absolute;
  inset: 0;
  margin: auto;
  width: 14px;
  height: 14px;
  border: 1.5px solid #7a8696;
  border-radius: 50%;
}

.shell-gear span::before {
  width: 2px;
  height: 18px;
  border: 0;
  background: #7a8696;
}

.shell-gear span::after {
  width: 18px;
  height: 2px;
  border: 0;
  background: #7a8696;
}

.shell-user-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffd7b0, #7da6ff);
  color: #24364f;
  display: inline-grid;
  place-items: center;
  font-size: 11px;
  font-weight: 700;
}

.shell-user-meta {
  display: grid;
  gap: 2px;
}

.shell-user-meta strong {
  font-size: 12px;
  color: #344153;
  font-weight: 500;
  line-height: 1.1;
}

.shell-user-meta small {
  font-size: 10px;
  color: #98a3b3;
  line-height: 1.1;
}

.shell-logout-btn {
  min-height: 28px;
  padding: 0 10px;
  border-radius: 4px;
  line-height: 26px;
}

.sidebar-backdrop {
  position: fixed;
  inset: 46px 0 0 0;
  background: rgba(26, 38, 54, 0.28);
  z-index: 35;
}

.sidebar-panel {
  grid-area: sidebar;
  background: #fff;
  border-right: 1px solid #d6dde7;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.sidebar-brand-block {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px 12px;
  border-bottom: 1px solid #edf1f6;
}

.sidebar-brand-mark {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: linear-gradient(180deg, #5c95f6, #2f74ee);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 13px;
  font-weight: 700;
}

.sidebar-brand-copy {
  display: grid;
  gap: 2px;
}

.sidebar-brand-copy strong {
  font-size: 13px;
  color: #2f3946;
}

.sidebar-brand-copy small {
  font-size: 11px;
  color: #93a0b2;
}

.sidebar-nav {
  padding: 10px 8px 18px;
  display: grid;
  gap: 8px;
}

.sidebar-group {
  display: grid;
  gap: 4px;
}

.sidebar-group-trigger {
  width: 100%;
  min-height: 38px;
  border: 0;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 10px;
  color: #465364;
  border-radius: 6px;
  transition: background-color 0.16s ease, color 0.16s ease;
}

.sidebar-group-trigger:hover,
.sidebar-group-trigger-active {
  background: #eef4fb;
  color: #1f6fe5;
}

.sidebar-group-main {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sidebar-group-title {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.sidebar-group-icon {
  width: 16px;
  height: 16px;
  position: relative;
  flex: 0 0 auto;
}

.sidebar-group-icon::before,
.sidebar-group-icon::after {
  content: '';
  position: absolute;
  box-sizing: border-box;
}

.sidebar-group-icon-home::before {
  left: 2px;
  right: 2px;
  bottom: 2px;
  height: 8px;
  border: 1.5px solid currentColor;
  border-top: 0;
  border-radius: 0 0 2px 2px;
}

.sidebar-group-icon-home::after {
  left: 3px;
  top: 2px;
  width: 10px;
  height: 10px;
  border-left: 1.5px solid currentColor;
  border-top: 1.5px solid currentColor;
  transform: rotate(45deg);
}

.sidebar-group-icon-process::before {
  left: 1px;
  top: 6px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 5px -4px 0 currentColor, 10px 0 0 currentColor;
}

.sidebar-group-icon-process::after {
  left: 3px;
  top: 5px;
  width: 9px;
  height: 2px;
  background: currentColor;
  box-shadow: 2px -4px 0 0 currentColor;
  transform: rotate(18deg);
}

.sidebar-group-icon-approval::before {
  left: 2px;
  top: 2px;
  width: 12px;
  height: 12px;
  border: 1.5px solid currentColor;
  border-radius: 3px;
}

.sidebar-group-icon-approval::after {
  left: 5px;
  top: 5px;
  width: 5px;
  height: 3px;
  border-left: 1.5px solid currentColor;
  border-bottom: 1.5px solid currentColor;
  transform: rotate(-45deg);
}

.sidebar-group-icon-org::before {
  left: 1px;
  top: 2px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  border: 1.5px solid currentColor;
  box-shadow: 8px 0 0 -1px currentColor, 4px 7px 0 -1px currentColor;
}

.sidebar-group-icon-org::after {
  left: 5px;
  top: 6px;
  width: 6px;
  height: 5px;
  border-left: 1.5px solid currentColor;
  border-bottom: 1.5px solid currentColor;
  transform: rotate(-45deg);
}

.sidebar-group-icon-monitor::before {
  left: 2px;
  top: 2px;
  width: 12px;
  height: 9px;
  border: 1.5px solid currentColor;
  border-radius: 2px;
}

.sidebar-group-icon-monitor::after {
  left: 6px;
  bottom: 1px;
  width: 4px;
  height: 2px;
  background: currentColor;
  box-shadow: 0 -4px 0 currentColor;
}

.sidebar-group-icon-settings::before {
  inset: 2px;
  border-radius: 50%;
  border: 1.5px solid currentColor;
  box-shadow:
    0 -6px 0 -4px currentColor,
    0 6px 0 -4px currentColor,
    6px 0 0 -4px currentColor,
    -6px 0 0 -4px currentColor;
}

.sidebar-group-icon-settings::after {
  left: 6px;
  top: 6px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: currentColor;
}

.sidebar-group-chevron {
  width: 8px;
  height: 8px;
  border-right: 1.5px solid #95a2b4;
  border-bottom: 1.5px solid #95a2b4;
  transform: rotate(45deg);
  transition: transform 0.16s ease, border-color 0.16s ease;
}

.sidebar-group-trigger-active .sidebar-group-chevron,
.sidebar-group-trigger:hover .sidebar-group-chevron {
  border-color: #1f6fe5;
}

.sidebar-group-chevron-open {
  transform: rotate(225deg);
}

.sidebar-group-body {
  margin-left: 14px;
  padding: 4px 0 4px 12px;
  border-left: 1px solid #d9e3f0;
  display: grid;
  gap: 2px;
}

.sidebar-menu-item {
  min-height: 34px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 10px;
  border-radius: 6px;
  color: #506071;
  transition: background-color 0.16s ease, color 0.16s ease;
}

.sidebar-menu-item:hover,
.sidebar-menu-item-active {
  background: linear-gradient(180deg, #f5f9ff 0%, #edf4ff 100%);
  color: #1f6fe5;
}

.sidebar-menu-item-active {
  font-weight: 700;
}

.sidebar-menu-marker {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c2cede;
  box-shadow: 0 0 0 1px #dde5f0;
  flex: 0 0 auto;
}

.sidebar-menu-item-active .sidebar-menu-marker {
  background: #2f7df4;
  box-shadow: 0 0 0 1px #9ebff4;
}

.sidebar-menu-item-disabled {
  color: #9aa6b5;
  background: transparent;
}

.sidebar-menu-item-disabled:hover {
  background: #f7f8fa;
  color: #8a97a7;
}

.sidebar-menu-item-disabled .sidebar-menu-marker {
  background: #d5dde8;
  box-shadow: 0 0 0 1px #e7edf4;
}

.sidebar-menu-text {
  font-size: 13px;
}

.content-shell {
  grid-area: content;
  display: grid;
  grid-template-rows: 34px minmax(0, 1fr);
  min-width: 0;
  background: #dfe4ec;
}

.route-bar {
  min-height: 34px;
  padding: 0 10px;
  background: #dfe4ec;
  border-bottom: 1px solid #d4dae3;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-shell {
  padding: 8px 10px 10px;
  min-width: 0;
}

.route-breadcrumb {
  font-size: 13px;
  color: #7d8896;
}

.route-breadcrumb strong {
  color: #303945;
}

.route-time {
  font-size: 12px;
  color: #99a5b5;
}

.shell-toggle {
  display: none;
}

@media (max-width: 920px) {
  .app-shell {
    grid-template-columns: 1fr;
    grid-template-areas:
      "topbar"
      "content";
  }

  .shell-toggle {
    display: inline-flex;
  }

  .sidebar-panel {
    position: fixed;
    top: 46px;
    left: 0;
    bottom: 0;
    width: 190px;
    transform: translateX(-110%);
    transition: transform 0.2s ease;
    z-index: 40;
  }

  .sidebar-panel.sidebar-open {
    transform: translateX(0);
  }

  .shell-user-meta {
    display: none;
  }
}
</style>
