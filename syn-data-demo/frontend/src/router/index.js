import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'
import { canAccessRoles, getFirstAccessibleHome, getStoredUser } from '../utils/auth'

const routes = [
  {
    path: '/',
    redirect: '/home/datasource'
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/home',
    name: 'Home',
    component: Layout,
    redirect: '/home/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        meta: { title: '仪表盘' },
        component: () => import('../views/Dashboard.vue')
      },
      {
        path: 'datasource',
        name: 'DataSource',
        meta: { title: '数据源管理', roles: ['admin'] },
        component: () => import('../views/DataSource.vue')
      },
      {
        path: 'mapping',
        name: 'MappingConfig',
        meta: { title: '映射配置', roles: ['admin'] },
        component: () => import('../views/MappingConfig.vue')
      },
      {
        path: 'sync-task',
        name: 'SyncTask',
        meta: { title: '同步任务' },
        component: () => import('../views/SyncTask.vue')
      },
      {
        path: 'task-log',
        name: 'TaskLog',
        meta: { title: '任务日志' },
        component: () => import('../views/TaskLog.vue')
      },
      {
        path: 'data-quality',
        name: 'DataQuality',
        meta: { title: '数据质量' },
        component: () => import('../views/DataQuality.vue')
      },
      {
        path: 'realtime-monitor',
        name: 'RealtimeMonitor',
        meta: { title: '实时监控', roles: ['admin'] },
        component: () => import('../views/RealtimeMonitor.vue')
      },
      {
        path: 'codegen',
        name: 'CodeGen',
        meta: { title: 'Watcher 配置', roles: ['admin'] },
        component: () => import('../views/CodeGen.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('so-token')
  const user = getStoredUser()
  if (to.meta.requiresAuth && !token) {
    next('/login')
    return
  }
  if (to.path === '/login' && token) {
    next(getFirstAccessibleHome(user))
    return
  }
  if (to.meta.requiresAuth && !user) {
    next('/login')
    return
  }
  if (to.meta.roles && !canAccessRoles(to.meta.roles, user)) {
    next(getFirstAccessibleHome(user))
    return
  }
  next()
})

export default router
