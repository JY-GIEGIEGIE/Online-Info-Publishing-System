import { createRouter, createWebHistory } from 'vue-router'

// TODO: 懒加载视图组件
// const Portal = () => import('../views/Portal.vue')
// const StockDetail = () => import('../views/StockDetail.vue')

const routes = [
  // TODO: { path: '/', name: 'Portal', component: Portal }
  // TODO: { path: '/stock/:code', name: 'StockDetail', component: StockDetail }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
