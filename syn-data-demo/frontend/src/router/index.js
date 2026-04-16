import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'

const routes = [
  {
    path: '/',
    redirect: '/login'
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
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue')
      },
      {
        path: 'datasource',
        name: 'DataSource',
        component: () => import('../views/DataSource.vue')
      },
      {
        path: 'mapping',
        name: 'MappingConfig',
        component: () => import('../views/MappingConfig.vue')
      },
      {
        path: 'sync-task',
        name: 'SyncTask',
        component: () => import('../views/SyncTask.vue')
      },
      {
        path: 'task-log',
        name: 'TaskLog',
        component: () => import('../views/TaskLog.vue')
      },
      {
        path: 'data-quality',
        name: 'DataQuality',
        component: () => import('../views/DataQuality.vue')
      },
      {
        path: 'realtime-monitor',
        name: 'RealtimeMonitor',
        component: () => import('../views/RealtimeMonitor.vue')
      },
      {
        path: 'codegen',
        name: 'CodeGen',
        component: () => import('../views/CodeGen.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
