import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'nearby', component: () => import('../views/NearbyView.vue'), meta: { title: '附近商户' } },
  { path: '/seckill', name: 'seckill', component: () => import('../views/SeckillView.vue'), meta: { title: '限时秒杀' } },
  { path: '/sign', name: 'sign', component: () => import('../views/SignView.vue'), meta: { title: '每日签到' } },
  { path: '/stats', name: 'stats', component: () => import('../views/StatsView.vue'), meta: { title: '数据统计' } }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
