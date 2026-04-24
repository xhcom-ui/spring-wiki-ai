import { createRouter, createWebHistory } from 'vue-router'
import { canAccessPath, clearCurrentUser, clearToken, getCurrentUser, getToken } from '../api/http'

const AppLayout = () => import('../layouts/AppLayout.vue')
const LoginView = () => import('../views/LoginView.vue')
const DashboardView = () => import('../views/DashboardView.vue')
const ProcessStudioView = () => import('../views/ProcessStudioView.vue')
const LeaveView = () => import('../views/LeaveView.vue')
const TaskCenterView = () => import('../views/TaskCenterView.vue')
const UserManagementView = () => import('../views/UserManagementView.vue')
const RoleManagementView = () => import('../views/RoleManagementView.vue')
const MenuPermissionView = () => import('../views/MenuPermissionView.vue')
const FormCatalogManagementView = () => import('../views/FormCatalogManagementView.vue')
const VersionCompareView = () => import('../views/VersionCompareView.vue')
const MonitoringView = () => import('../views/MonitoringView.vue')

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior() {
    return { top: 0 }
  },
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { guestOnly: true, title: '登录' }
    },
    {
      path: '/',
      component: AppLayout,
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'dashboard', component: DashboardView, meta: { title: '工作台首页', permission: 'dashboard:view' } },
        { path: 'designer', name: 'designer', component: ProcessStudioView, meta: { title: '流程设计', permission: 'process:design' } },
        { path: 'leave', name: 'leave', component: LeaveView, meta: { title: '请假发起', permission: 'leave:submit' } },
        { path: 'tasks', name: 'tasks', component: TaskCenterView, meta: { title: '待办任务', permission: 'task:approve' } },
        { path: 'users', name: 'users', component: UserManagementView, meta: { title: '用户管理', permission: 'user:manage' } },
        { path: 'roles', name: 'roles', component: RoleManagementView, meta: { title: '角色管理', permission: 'role:manage' } },
        { path: 'menu-permissions', name: 'menu-permissions', component: MenuPermissionView, meta: { title: '菜单权限', permission: 'menu:manage' } },
        { path: 'form-catalog', name: 'form-catalog', component: FormCatalogManagementView, meta: { title: '表单目录', permission: 'form:manage' } },
        { path: 'versions', name: 'versions', component: VersionCompareView, meta: { title: '版本历史对比', permission: 'process:compare' } },
        { path: 'monitoring', name: 'monitoring', component: MonitoringView, meta: { title: '流程监控中心', permission: 'monitoring:view' } }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const loggedIn = Boolean(getToken())
  const currentUser = getCurrentUser()
  if (to.meta.requiresAuth && !loggedIn) {
    return { name: 'login' }
  }
  if (loggedIn && !currentUser) {
    clearToken()
    clearCurrentUser()
    return { name: 'login', query: { reason: 'expired' } }
  }
  if (to.meta.guestOnly && loggedIn) {
    return { name: 'dashboard' }
  }
  if (loggedIn && to.meta.permission && !canAccessPath(to.path, to.meta.permission)) {
    return { name: 'dashboard', query: { denied: '1' } }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta?.title ? `${to.meta.title} · Flowable Workflow Console` : 'Flowable Workflow Console'
})

export default router
