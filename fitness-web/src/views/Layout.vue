<template>
  <el-container class="layout">
    <!--  -->
    <el-header class="header" height="60px">
      <div class="header-inner">
        <!-- Logo -->
        <div class="header-logo" @click="$router.push('/home')">
          <el-icon :size="28" color="#667eea"><PictureFilled /></el-icon>
          <span class="logo-text">FitPro</span>
        </div>

        <!-- ?-->
        <el-menu
          :default-active="activeMenu"
          mode="horizontal"
          class="desktop-nav"
          :ellipsis="false"
          @select="handleMenuSelect"
        >
          <el-menu-item index="/home">首页</el-menu-item>
          <el-menu-item index="/fitness/venue">场馆</el-menu-item>
          <el-menu-item index="/fitness/course">课程</el-menu-item>
          <el-menu-item index="/fitness/coach">教练</el-menu-item>
          <el-menu-item index="/community">社区</el-menu-item>
          <el-menu-item index="/fitness/ai-plan">AI计划生成</el-menu-item>
          <el-menu-item index="/fitness/recommend">推荐</el-menu-item>
          <el-menu-item index="/user/my">我的</el-menu-item>
          <el-menu-item
            v-if="userStore.isAdmin"
            index="/system/dashboard">管理</el-menu-item>
        </el-menu>

        <!--  -->
        <div class="header-user">
          <!-- ?-->
          <template v-if="!userStore.isLoggedIn">
            <el-button type="primary" size="small" @click="$router.push('/login')">登录 / 注册</el-button>
          </template>

          <!-- ?-->
          <template v-else>
            <el-dropdown trigger="click" @command="handleUserCommand">
              <div class="user-info-trigger">
                <el-avatar :size="36" :icon="UserFilled" :src="userStore.user?.avatar" />
                <span class="user-nickname">{{ userStore.user?.nickname || '' }}</span>
                <el-icon><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="/user/profile">
                    <el-icon><User /></el-icon>个人资料
                  </el-dropdown-item>
                  <el-dropdown-item command="/user/orders">
                    <el-icon><Tickets /></el-icon>我的订单
                  </el-dropdown-item>
                  <el-dropdown-item command="/user/wallet">
                    <el-icon><Wallet /></el-icon>我的钱包
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </div>

        <!-- ?-->
        <div class="mobile-menu-btn" @click="mobileMenuVisible = !mobileMenuVisible">
          <el-icon :size="24"><Fold v-if="mobileMenuVisible" /><Expand v-else /></el-icon>
        </div>
      </div>

      <!-- ?-->
      <transition name="mobile-slide">
        <div v-show="mobileMenuVisible" class="mobile-nav">
          <div
            v-for="item in mobileMenuItems"
            :key="item.index"
            class="mobile-nav-item"
            :class="{ active: activeMenu === item.index }"
            @click="handleMobileNavItem(item.index)"
          >
            <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
            {{ item.label }}
          </div>
          <div v-if="!userStore.isLoggedIn" class="mobile-nav-login">
            <el-button type="primary" block @click="$router.push('/login')">
               / 
            </el-button>
          </div>
          <div v-else class="mobile-nav-logout" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>退出登录</div>
        </div>
      </transition>
    </el-header>

    <!--  -->
    <el-main class="main">
      <router-view />
    </el-main>

    <!--  -->
    <el-footer class="footer" height="auto">
      <div class="footer-content">
        <div class="footer-links">
          <span @click="$router.push('/home')"></span>
          <span @click="$router.push('/fitness/venue')"></span>
          <span @click="$router.push('/fitness/course')"></span>
          <span @click="$router.push('/fitness/coach')"></span>
          <span @click="$router.push('/community')"></span>
        </div>
        <p class="footer-copy">FitPro &#8212; 您的智能健身伙伴</p>
        <p class="footer-copy">&copy; {{ new Date().getFullYear() }} FitPro. All rights reserved.</p>
      </div>
    </el-footer>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import {
  PictureFilled, UserFilled, ArrowDown, User, Tickets,
  CircleCheckFilled, SwitchButton, Fold, Expand, Wallet,
  HomeFilled, Place, Reading, UserFilled as CoachIcon, ChatDotRound, MagicStick, DataAnalysis
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeMenu = ref('/home')
const mobileMenuVisible = ref(false)

const mobileMenuItems = computed(() => {
  const items = [
    { index: '/home', label: '首页', icon: HomeFilled },
    { index: '/fitness/venue', label: '场馆', icon: Place },
    { index: '/fitness/course', label: '课程', icon: Reading },
    { index: '/fitness/coach', label: '教练', icon: CoachIcon },
    { index: '/community', label: '社区', icon: ChatDotRound },
    { index: '/fitness/ai-plan', label: 'AI计划生成', icon: MagicStick },
    { index: '/fitness/recommend', label: '推荐', icon: DataAnalysis },
    { index: '/user/my', label: '我的', icon: User },
    { index: '/diet/plan', label: '打卡', icon: CircleCheckFilled },
    { index: '/diet/record', label: '饮食', icon: Tickets }
  ]
  if (userStore.isAdmin) {
    items.push({ index: '/system/dashboard', label: '管理', icon: null })
  }
  return items
})

onMounted(() => {
  updateActiveMenu()

  if (!userStore.isLoggedIn) {
    userStore.restoreLogin()
  }

  if (userStore.isLoggedIn && !userStore.user) {
    userStore.fetchUserInfo()
  }
})

function updateActiveMenu() {
  const path = route.path
  if (path.startsWith('/fitness/venue')) activeMenu.value = '/fitness/venue'
  else if (path.startsWith('/fitness/course')) activeMenu.value = '/fitness/course'
  else if (path.startsWith('/fitness/coach')) activeMenu.value = '/fitness/coach'
  else if (path.startsWith('/community')) activeMenu.value = '/community'
  else if (path.startsWith('/fitness/ai-plan')) activeMenu.value = '/fitness/ai-plan'
  else if (path.startsWith('/fitness/recommend')) activeMenu.value = '/fitness/recommend'
  else if (path.startsWith('/user/my')) activeMenu.value = '/user/my'
  else if (path.startsWith('/system/dashboard')) activeMenu.value = '/system/dashboard'
  else activeMenu.value = '/home'
}

function handleMenuSelect(index) {
  mobileMenuVisible.value = false
  router.push(index)
}

function handleMobileNavItem(index) {
  mobileMenuVisible.value = false
  router.push(index)
}

function handleUserCommand(command) {
  if (command === 'logout') {
    handleLogout()
  } else {
    router.push(command)
  }
}

function handleLogout() {
  userStore.logout()
  mobileMenuVisible.value = false
  router.push('/home')
}
</script>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* Header */
.header {
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  padding: 0 20px;
  height: 60px;
}

.header-inner {
  display: flex;
  align-items: center;
  height: 100%;
  max-width: 1400px;
  margin: 0 auto;
}

.header-logo {
  display: flex;
  align-items: center;
  cursor: pointer;
  flex-shrink: 0;
  margin-right: 32px;
}

.logo-text {
  font-size: 22px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-left: 8px;
}

.desktop-nav {
  flex: 1;
  border-bottom: none !important;
}

.desktop-nav .el-menu-item {
  height: 60px;
  line-height: 60px;
  font-size: 15px;
  font-weight: 500;
  border-bottom: 2px solid transparent;
}

.desktop-nav .el-menu-item.is-active {
  border-bottom-color: #667eea;
  color: #667eea;
  font-weight: 600;
}

.header-user {
  flex-shrink: 0;
  margin-left: 16px;
}

.user-info-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-info-trigger:hover {
  background: #f5f7fa;
}

.user-nickname {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ?*/
.mobile-menu-btn {
  display: none;
  cursor: pointer;
  padding: 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.mobile-menu-btn:hover {
  background: #f5f7fa;
}

/* ?*/
.mobile-nav {
  display: none;
  position: absolute;
  top: 60px;
  left: 0;
  right: 0;
  background: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 12px 0;
  z-index: 999;
}

.mobile-nav-item {
  padding: 12px 24px;
  font-size: 15px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: background 0.2s;
}

.mobile-nav-item:hover {
  background: #f5f7fa;
}

.mobile-nav-item.active {
  color: #667eea;
  background: #ecf5ff;
  font-weight: 600;
}

.mobile-nav-login {
  padding: 12px 24px;
}

.mobile-nav-logout {
  padding: 12px 24px;
  font-size: 15px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #f56c6c;
  transition: background 0.2s;
}

.mobile-nav-logout:hover {
  background: #fef0f0;
}

.mobile-slide-enter-active,
.mobile-slide-leave-active {
  transition: all 0.3s ease;
}

.mobile-slide-enter-from,
.mobile-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* Main */
.main {
  padding: 80px 20px 20px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  min-height: calc(100vh - 120px);
}

/* Footer */
.footer {
  background: #1a1a2e;
  color: #fff;
  text-align: center;
  padding: 20px;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
}

.footer-links {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.footer-links span {
  cursor: pointer;
  font-size: 14px;
  opacity: 0.8;
  transition: opacity 0.2s;
}

.footer-links span:hover {
  opacity: 1;
}

.footer-copy {
  font-size: 13px;
  opacity: 0.5;
  margin: 0;
  line-height: 1.8;
}

/* ?*/
@media (max-width: 900px) {
  .desktop-nav {
    display: none;
  }

  .mobile-menu-btn {
    display: block;
    margin-left: auto;
  }

  .mobile-nav {
    display: block;
  }

  .header-user {
    margin-left: auto;
  }

  .user-nickname {
    display: none;
  }
}

@media (max-width: 600px) {
  .header {
    padding: 0 12px;
  }

  .header-logo {
    margin-right: 8px;
  }

  .logo-text {
    font-size: 18px;
  }

  .main {
    padding: 72px 8px 12px;
  }
}
</style>