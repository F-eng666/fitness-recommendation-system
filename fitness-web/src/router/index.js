import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken } from '@/utils/auth'

const routes = [
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/home',
    children: [
      { path: '/home', name: 'Home', component: () => import('@/views/system/Home.vue'), meta: { title: 'Home' } },
      { path: '/community', name: 'Community', component: () => import('@/views/system/Community.vue'), meta: { title: 'Community' } },
      { path: '/community/article/:id', name: 'ArticleDetail', component: () => import('@/views/system/ArticleDetail.vue'), meta: { title: 'Article' } },
      { path: '/community/write', name: 'ArticleWrite', component: () => import('@/views/system/ArticleWrite.vue'), meta: { title: 'Write', auth: true } },
      { path: '/fitness/venue', name: 'Venue', component: () => import('@/views/fitness/VenueList.vue'), meta: { title: 'Venues' } },
      { path: '/fitness/venue/:id', name: 'VenueDetail', component: () => import('@/views/fitness/VenueDetail.vue'), meta: { title: 'Venue Detail' } },
      { path: '/fitness/course', name: 'Course', component: () => import('@/views/fitness/CourseList.vue'), meta: { title: 'Courses' } },
      { path: '/fitness/course/:id', name: 'CourseDetail', component: () => import('@/views/fitness/CourseDetail.vue'), meta: { title: 'Course Detail' } },
      { path: '/fitness/coach', name: 'Coach', component: () => import('@/views/fitness/CoachList.vue'), meta: { title: 'Coaches' } },
      { path: '/fitness/coach/:id', name: 'CoachDetail', component: () => import('@/views/fitness/CoachDetail.vue'), meta: { title: 'Coach Detail' } },
      { path: '/fitness/ai-plan', name: 'AIPlan', component: () => import('@/views/fitness/AIPlan.vue'), meta: { title: 'AI Plan', auth: true } },
      { path: '/fitness/recommend', name: 'Recommend', component: () => import('@/views/fitness/Recommend.vue'), meta: { title: 'Recommend', auth: true } },
      { path: '/diet/plan', name: 'DietPlan', component: () => import('@/views/diet/DietPlan.vue'), meta: { title: 'Diet', auth: true } },
      { path: '/diet/record', name: 'DietRecord', component: () => import('@/views/diet/DietRecord.vue'), meta: { title: 'Diet Record', auth: true } },
      { path: '/statistics/dashboard', name: 'Statistics', component: () => import('@/views/statistics/Dashboard.vue'), meta: { title: 'Stats', auth: true, role: 'ADMIN' } },
      { path: '/user/profile', name: 'Profile', component: () => import('@/views/user/Profile.vue'), meta: { title: 'Profile', auth: true } },
      { path: '/user/orders', name: 'MyOrders', component: () => import('@/views/user/MyOrders.vue'), meta: { title: 'Orders', auth: true } },
      { path: '/user/favorites', name: 'MyFavorites', component: () => import('@/views/user/MyFavorites.vue'), meta: { title: 'Favorites', auth: true } },
      { path: '/user/my', name: 'My', component: () => import('@/views/user/My.vue'), meta: { title: 'My', auth: true } },
      { path: '/user/checkins', name: 'MyCheckins', component: () => import('@/views/user/MyCheckins.vue'), meta: { title: 'Checkins', auth: true } },
      { path: '/user/wallet', name: 'MyWallet', component: () => import('@/views/user/MyWallet.vue'), meta: { title: 'Wallet', auth: true } },
      { path: '/system/dashboard', name: 'SystemDashboard', component: () => import('@/views/system/Dashboard.vue'), meta: { title: 'Admin', auth: true, role: 'ADMIN' } },
      { path: '/system/users', name: 'SystemUsers', component: () => import('@/views/system/UserManage.vue'), meta: { title: 'Users', auth: true, role: 'ADMIN' } },
      { path: '/system/venues', name: 'SystemVenues', component: () => import('@/views/system/VenueAudit.vue'), meta: { title: 'Venues', auth: true, role: 'ADMIN' } },
      { path: '/system/announcements', name: 'SystemAnnouncements', component: () => import('@/views/system/AnnouncementManage.vue'), meta: { title: 'Announce', auth: true, role: 'ADMIN' } },
      { path: '/system/courses', name: 'SystemCourses', component: () => import('@/views/system/CourseManage.vue'), meta: { title: 'Courses', auth: true, role: 'ADMIN' } },
      { path: '/system/coaches', name: 'SystemCoaches', component: () => import('@/views/system/CoachManage.vue'), meta: { title: 'Coaches', auth: true, role: 'ADMIN' } },
      { path: '/system/orders', name: 'SystemOrders', component: () => import('@/views/system/OrderManage.vue'), meta: { title: 'Orders', auth: true, role: 'ADMIN' } },
      { path: '/system/reviews', name: 'SystemReviews', component: () => import('@/views/system/ReviewManage.vue'), meta: { title: 'Reviews', auth: true, role: 'ADMIN' } },
      { path: '/system/venue-dashboard', name: 'VenueDashboard', component: () => import('@/views/system/VenueDashboard.vue'), meta: { title: 'Venue Dashboard', auth: true, role: 'ADMIN' } },
    ]
  },
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { title: 'Login' } },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue'), meta: { title: 'Register' } },
  { path: '/forgot-password', name: 'ForgotPassword', component: () => import('@/views/ForgotPassword.vue'), meta: { title: 'Forgot Password' } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? to.meta.title + ' - FitPro' : 'FitPro - smart fitness system'
  const token = getToken()
  if (to.meta.auth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }
  if (to.meta.role && token) {
    const userRole = localStorage.getItem('userRole')
    if (to.meta.role === 'ADMIN' && userRole !== 'ADMIN') {
      next({ name: 'Home' })
      return
    }
  }
  next()
})

export default router