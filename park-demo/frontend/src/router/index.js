import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/parking-lots',
    name: 'ParkingLots',
    component: () => import('../views/ParkingLots.vue')
  },
  {
    path: '/parking-spots',
    name: 'ParkingSpots',
    component: () => import('../views/ParkingSpots.vue')
  },
  {
    path: '/areas',
    name: 'Areas',
    component: () => import('../views/Areas.vue')
  },
  {
    path: '/sensors',
    name: 'Sensors',
    component: () => import('../views/Sensors.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
