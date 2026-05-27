import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import Register from '@/views/Register.vue'
import Layout from '@/views/Layout.vue'
import Home from '@/views/Home.vue'
import Check from '@/views/Check.vue'
import VenueConfig from '@/views/VenueConfig.vue'
import VenueManage from '@/views/VenueManage.vue'
import ActivityManage from '@/views/ActivityManage.vue'
import NoticeManage from '@/views/NoticeManage.vue'
import Blacklist from '@/views/Blacklist.vue'

const routes = [
  { path: '/', redirect: '/admin/auth/login' }, // 根路径重定向到登录页
  // 管理员认证相关路由（对应API的/admin/auth）
  { path: '/admin/auth/login', component: Login, meta: { title: '管理员登录' } },
  { path: '/admin/auth/register', component: Register, meta: { title: '管理员注册' } },
  {
    path: '/admin',
    component: Layout,
    children: [
      { path: 'home', component: Home, meta: { title: '首页总览' } },
      // 预约核销管理（对应API的/admin/join）
      { path: 'join', component: Check, meta: { title: '预约核销' } },
      // 场馆配置与管理（对应API的/museum）
      { path: 'museum-config', component: VenueConfig, meta: { title: '场馆配置' } },
      { path: 'museum', component: VenueManage, meta: { title: '场馆管理' } },
      // 活动管理（对应API的/activity）
      { path: 'activity', component: ActivityManage, meta: { title: '活动管理' } },
      // 公告管理（对应API的/news）
      { path: 'news', component: NoticeManage, meta: { title: '场馆公告管理' } },
      // 黑名单管理（对应API的/admin/blacklist）
      { path: 'blacklist', component: Blacklist, meta: { title: '黑名单管理' } },
      // 消息模版管理
      { path: 'message-template', component: () => import('@/views/MessageTemplate.vue'), meta: { title: '消息模版' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫（未登录拦截）
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  // 排除登录和注册页（这两个路径不需要token）
  if (!token && !to.path.startsWith('/admin/auth/')) {
    next('/admin/auth/login')
  } else {
    next()
  }
})

export default router