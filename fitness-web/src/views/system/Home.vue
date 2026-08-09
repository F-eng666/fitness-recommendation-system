<template>
  <div class="home">
    <!-- 英雄横幅 -->
    <section class="hero">
      <div class="hero-content">
        <h1 class="hero-title">智能健身 &#183; 科学推荐</h1>
        <p class="hero-subtitle">
          基于AI的个性化健身方案，让每一次训练都精准高效
        </p>
        <el-button class="hero-btn" size="large" round @click="handleStartExplore">
          开始探索
          <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
      </div>
      <div class="hero-decoration">
        <div class="hero-circle circle-1"></div>
        <div class="hero-circle circle-2"></div>
        <div class="hero-circle circle-3"></div>
      </div>
    </section>

    <!-- 特色功能 -->
    <section class="features section">
      <div class="section-header">
        <h2>为什么选择 FitPro</h2>
        <p>智能科技赋能健身，打造专属你的运动体验</p>
      </div>
      <div class="features-grid">
        <div class="feature-card" v-for="item in featureItems" :key="item.title">
          <div class="feature-icon">
            <el-icon :size="40" :color="item.color">
              <component :is="item.icon" />
            </el-icon>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
        </div>
      </div>
    </section>

    <!-- 快捷打卡 -->
    <section class="checkin-banner" v-if="isLoggedIn">
      <div class="banner-content">
        <div class="banner-text">
          <span class="banner-icon">🏋️</span>
          <span>今天训练了吗？坚持打卡，见证改变！</span>
        </div>
        <el-button type="primary" size="large" round @click="$router.push('/user/checkins')">
          立即打卡
        </el-button>
      </div>
    </section>

    <!-- 会员卡 -->
    <section class="membership-cards section" v-if="membershipCards.length > 0 || membershipsLoading">
      <div class="section-header">
        <h2>会员卡</h2>
        <span class="section-subtitle">开通会员卡，畅享优惠</span>
      </div>
      <div class="cards-grid" v-loading="membershipsLoading">
        <el-card
          v-for="group in membershipCards"
          :key="group.cardType"
          class="card-card"
          shadow="hover"
        >
          <div class="card-header" :class="getCardHeaderClass(group.cardType)">
            <div class="card-icon-wrapper">
              <span class="card-emoji">{{ getCardEmoji(group.cardType) }}</span>
            </div>
            <span class="card-type-label">{{ group.typeName }}</span>
          </div>
          <div class="card-body">
            <h4 class="card-name">{{ group.typeName }}</h4>
            <p class="card-desc">{{ getGroupDesc(group) }}</p>
            <div class="card-price-row">
              <span class="card-price-from">¥</span>
              <span class="card-price-value">{{ group.minPrice }}</span>
              <span class="card-price-suffix">起</span>
            </div>
            <el-button
              class="card-buy-btn"
              :class="getCardBtnClass(group.cardType)"
              round
              @click="handleBuyCard(group)"
            >
              立即购买
            </el-button>
          </div>
        </el-card>

        <!-- 占位卡片 -->
        <template v-if="!membershipsLoading && membershipCards.length === 0">
          <div class="card-placeholder" v-for="i in 4" :key="'cph-'+i">
            <div class="card-placeholder-header"></div>
            <div class="card-placeholder-body">
              <div class="placeholder-line w-60"></div>
              <div class="placeholder-line w-80"></div>
              <div class="placeholder-line w-40"></div>
            </div>
          </div>
        </template>
      </div>
    </section>

    <!-- 会员卡弹窗 -->
    <el-dialog v-model="buyDialogVisible" title="购买会员卡" width="440px" destroy-on-close>
      <div class="buy-dialog-body" v-if="selectedGroup">
        <div class="buy-card-info">
          <div class="buy-card-header" :class="getCardHeaderClass(selectedGroup.cardType)">
            <span class="buy-card-type">{{ selectedGroup.typeName }}</span>
          </div>
          <div class="buy-card-detail">
            <h3>{{ selectedGroup.typeName }}</h3>
            <p class="buy-desc">选择场馆后查看具体价格</p>
          </div>
        </div>
        <el-divider />
        <!-- 场馆选择 -->
        <div class="venue-select-area">
          <span class="venue-select-label">选择场馆：</span>
          <el-select
            v-model="selectedVenueId"
            placeholder="请选择场馆"
            style="width: 100%; margin-top: 8px"
            @change="onVenueChange"
          >
            <el-option
              v-for="v in selectedGroup.venues"
              :key="v.venueId"
              :label="v.venueName + (v.available ? ' — ¥' + v.price : ' — 暂未开通')"
              :value="v.venueId"
              :disabled="!v.available"
            />
          </el-select>
        </div>
        <!-- 选中场馆的详情 -->
        <div class="selected-venue-info" v-if="selectedVenue">
          <div class="venue-detail-row">
            <span class="venue-detail-label">场馆：</span>
            <span>{{ selectedVenue.venueName }}</span>
          </div>
          <div class="venue-detail-row">
            <span class="venue-detail-label">说明：</span>
            <span>{{ selectedVenue.description }}</span>
          </div>
          <template v-if="selectedVenue.available">
            <div class="buy-price-row">
              <span class="buy-price-label">价格：</span>
              <span class="buy-price">¥{{ selectedVenue.price }}</span>
            </div>
          </template>
          <el-alert
            v-else
            title="该场馆暂未开通此卡型，请选择其他场馆"
            type="info"
            show-icon
            :closable="false"
            style="margin-top:8px"
          />
        </div>
        <el-divider />
        <div class="buy-wallet-info">
          <span>支付方式：</span>
          <el-tag type="warning" effect="plain">钱包余额</el-tag>
          <span class="wallet-balance" v-if="walletBalance !== null">
            余额：<strong>¥{{ walletBalance }}</strong>
          </span>
        </div>
        <el-alert
          v-if="walletBalance !== null && selectedVenue && walletBalance < selectedVenue.price"
          title="余额不足，请先充值"
          type="error"
          show-icon
          :closable="false"
          style="margin-top:12px"
        />
      </div>
      <template #footer>
        <el-button @click="buyDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          @click="confirmBuy"
          :loading="buying"
          :disabled="!selectedVenueId || !selectedVenue?.available || (walletBalance !== null && selectedVenue && walletBalance < selectedVenue.price)"
        >
          确认支付 {{ selectedVenue ? '¥' + selectedVenue.price : '' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 热门课程 -->
    <section class="hot-courses section">
      <div class="section-header">
        <h2>热门课程</h2>
        <el-button text type="primary" @click="$router.push('/fitness/course')">
          查看更多 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
      </div>
      <div class="courses-grid" v-loading="coursesLoading">
        <el-card
          v-for="course in courses"
          :key="course.id"
          class="course-card"
          shadow="hover"
          @click="$router.push(`/fitness/course/${course.id}`)"
        >
          <div class="course-cover" :class="getCourseCoverClass(course.type)">
            <el-icon :size="36" color="rgba(255,255,255,0.7)"><Reading /></el-icon>
          </div>
          <div class="course-info">
            <div class="course-header">
              <el-tag
                :type="getCourseTypeTag(course.type)"
                size="small"
                effect="plain"
              >
                {{ typeMap[course.type] || course.type || '综合训练' }}
              </el-tag>
              <el-tag
                :type="getDifficultyTag(course.difficulty)"
                size="small"
                effect="plain"
              >
                {{ diffMap[course.difficulty] || course.difficulty || '初级' }}
              </el-tag>
            </div>
            <h4 class="course-name text-ellipsis">{{ course.name }}</h4>
            <p class="course-venue text-ellipsis">{{ course.venueName || '暂无场馆' }}</p>
            <div class="course-bottom">
              <span class="course-price" v-if="course.price">¥{{ course.price }}</span>
              <span class="course-price free" v-else>免费</span>
              <span class="course-rating">
                <el-icon color="#f7ba2a"><StarFilled /></el-icon>
                {{ course.rating || '--' }}
              </span>
            </div>
          </div>
        </el-card>

        <!-- 占位卡片（无数据时） -->
        <template v-if="!coursesLoading && courses.length === 0">
          <div class="course-card-placeholder" v-for="i in 4" :key="'ph-'+i">
            <div class="course-cover-placeholder">
              <el-icon :size="36" color="#ddd"><Reading /></el-icon>
            </div>
            <div class="course-info-placeholder">
              <div class="placeholder-line w-60"></div>
              <div class="placeholder-line w-80"></div>
              <div class="placeholder-line w-40"></div>
            </div>
          </div>
        </template>
      </div>
    </section>

    <!-- 热门文章 -->
    <section class="hot-articles section">
      <div class="section-header">
        <h2>热门文章</h2>
        <el-button text type="primary" @click="$router.push('/community')">
          查看更多 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
      </div>
      <div class="articles-grid" v-loading="articlesLoading">
        <el-card
          v-for="article in articles"
          :key="article.id"
          class="article-card"
          shadow="hover"
          @click="$router.push(`/community/article/${article.id}`)"
        >
          <div class="article-cover">
            <el-icon :size="32" color="rgba(255,255,255,0.5)"><ChatDotRound /></el-icon>
          </div>
          <div class="article-info">
            <h4 class="article-title text-ellipsis-2">{{ article.title }}</h4>
            <p class="article-excerpt text-ellipsis-2">{{ article.summary || '暂无摘要' }}</p>
            <div class="article-meta">
              <span class="article-author">
                <el-icon><User /></el-icon> {{ article.authorName || '匿名' }}
              </span>
              <span class="article-stats">
                <el-icon><View /></el-icon> {{ article.viewCount || 0 }}
                <el-icon style="margin-left:8px"><Star /></el-icon> {{ article.likeCount || 0 }}
              </span>
            </div>
          </div>
        </el-card>

        <!-- 占位卡片 -->
        <template v-if="!articlesLoading && articles.length === 0">
          <div class="article-card-placeholder" v-for="i in 3" :key="'aph-'+i">
            <div class="article-cover-placeholder">
              <el-icon :size="32" color="#ddd"><ChatDotRound /></el-icon>
            </div>
            <div class="article-info-placeholder">
              <div class="placeholder-line w-80"></div>
              <div class="placeholder-line w-100"></div>
              <div class="placeholder-line w-50"></div>
            </div>
          </div>
        </template>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCourseList } from '@/api/course'
import { getArticleList } from '@/api/article'
import { getHotCards, purchaseCard } from '@/api/membership'
import { getMyWallet } from '@/api/wallet'
import {
  MagicStick, DataAnalysis, ChatDotRound, ArrowRight,
  Reading, StarFilled, User, View, Star
} from '@element-plus/icons-vue'

const router = useRouter()

const courses = ref([])
const articles = ref([])
const coursesLoading = ref(false)
const articlesLoading = ref(false)
const isLoggedIn = computed(() => !!localStorage.getItem('fitness_token'))

// 会员卡
const membershipCards = ref([])
const membershipsLoading = ref(false)
const buyDialogVisible = ref(false)
const selectedGroup = ref(null)
const selectedVenueId = ref(null)
const selectedVenue = ref(null)
const buying = ref(false)
const walletBalance = ref(null)

const getCardEmoji = (type) => {
  const map = { SESSION: '🎫', WEEKLY: '📅', MONTHLY: '🥇', QUARTERLY: '🏆' }
  return map[type] || '💳'
}

const getCardHeaderClass = (type) => {
  return 'card-header-' + (type || '').toLowerCase()
}

const getCardBtnClass = (type) => {
  return 'card-btn-' + (type || '').toLowerCase()
}

const getGroupDesc = (group) => {
  // Show venue count and the cheapest description
  const count = group.venues?.length || 0
  if (group.cardType === 'SESSION') return `${count}家场馆可选，低至¥${group.minPrice}/次`
  if (group.cardType === 'WEEKLY') return `${count}家场馆可选，7天无限次`
  if (group.cardType === 'MONTHLY') return `${count}家场馆可选，30天无限次`
  if (group.cardType === 'QUARTERLY') return `${count}家场馆可选，90天无限次`
  return `${count}家场馆可选`
}

const onVenueChange = (venueId) => {
  selectedVenue.value = selectedGroup.value?.venues?.find(v => v.venueId === venueId) || null
}

const typeMap = {
  YOGA: '瑜伽', STRENGTH: '力量训练', CARDIO: '有氧运动', DANCE: '舞蹈',
  SWIMMING: '游泳', BASKETBALL: '篮球', BOXING: '搏击', PILATES: '普拉提',
  SPINNING: '动感单车', OTHER: '其他', HIIT: 'HIIT'
}

const diffMap = {
  BEGINNER: '初级', INTERMEDIATE: '中级', ADVANCED: '高级'
}

const featureItems = [
  {
    icon: MagicStick,
    title: 'AI智能推荐',
    desc: '基于你的身体数据和健身目标，智能推荐最适合的课程、教练和训练计划',
    color: '#667eea'
  },
  {
    icon: DataAnalysis,
    title: '科学训练计划',
    desc: 'AI为你定制个性化训练方案，科学分配训练强度、频率和休息时间',
    color: '#764ba2'
  },
  {
    icon: ChatDotRound,
    title: '健身知识社区',
    desc: '分享健身经验，交流训练心得，与志同道合的健身伙伴一起进步',
    color: '#e6a23c'
  }
]

onMounted(() => {
  fetchMemberships()
  fetchCourses()
  fetchArticles()
})

async function fetchMemberships() {
  membershipsLoading.value = true
  try {
    const res = await getHotCards()
    if (res.code === 200) {
      membershipCards.value = res.data || []
    }
  } catch (e) {
    console.error('获取会员卡失败', e)
  } finally {
    membershipsLoading.value = false
  }
}

async function handleBuyCard(group) {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  selectedGroup.value = group
  selectedVenueId.value = null
  selectedVenue.value = null
  buyDialogVisible.value = true
  // Fetch wallet balance
  try {
    const res = await getMyWallet()
    if (res.code === 200) {
      walletBalance.value = res.data?.balance ?? 0
    }
  } catch {
    walletBalance.value = null
  }
}

async function confirmBuy() {
  if (!selectedVenue.value) return
  buying.value = true
  try {
    const res = await purchaseCard({
      cardId: selectedVenue.value.cardId,
      venueId: selectedVenue.value.venueId
    })
    if (res.code === 200) {
      ElMessage.success(`购买成功！${res.data?.cardName || ''}，余额剩余 ¥${res.data?.balance || 0}`)
      buyDialogVisible.value = false
      selectedGroup.value = null
      selectedVenueId.value = null
      selectedVenue.value = null
      walletBalance.value = res.data?.balance ?? walletBalance.value
    } else {
      ElMessage.error(res.message || '购买失败')
    }
  } catch (e) {
    console.error('购买会员卡失败', e)
    ElMessage.error('购买失败，请稍后再试')
  } finally {
    buying.value = false
  }
}

async function fetchCourses() {
  coursesLoading.value = true
  try {
    const res = await getCourseList({ size: 4 })
    if (res.code === 200) {
      courses.value = res.data?.records || res.data || []
    }
  } catch (e) {
    console.error('获取热门课程失败', e)
  } finally {
    coursesLoading.value = false
  }
}

async function fetchArticles() {
  articlesLoading.value = true
  try {
    const res = await getArticleList({ size: 3, orderBy: 'hot' })
    if (res.code === 200) {
      articles.value = res.data?.records || res.data || []
    }
  } catch (e) {
    console.error('获取热门文章失败', e)
  } finally {
    articlesLoading.value = false
  }
}

function handleStartExplore() {
  router.push('/fitness/recommend')
}

function getCourseCoverClass(type) {
  const typeMap = {
    YOGA: 'cover-yoga', STRENGTH: 'cover-strength', CARDIO: 'cover-cardio',
    HIIT: 'cover-hiit', PILATES: 'cover-pilates', DANCE: 'cover-dance',
    BOXING: 'cover-boxing', SWIMMING: 'cover-default', BASKETBALL: 'cover-default',
    SPINNING: 'cover-default'
  }
  return typeMap[type] || 'cover-default'
}

function getCourseTypeTag(type) {
  const typeMap = {
    YOGA: 'success', STRENGTH: 'danger', CARDIO: 'warning',
    HIIT: 'danger', PILATES: 'success', DANCE: '',
    BOXING: 'info', SWIMMING: '', SPINNING: 'success'
  }
  return typeMap[type] || 'info'
}

function getDifficultyTag(difficulty) {
  const map = {
    '初级': 'success',
    '中级': 'warning',
    '高级': 'danger',
    BEGINNER: 'success',
    INTERMEDIATE: 'warning',
    ADVANCED: 'danger'
  }
  return map[difficulty] || 'info'
}
</script>

<style scoped>
/* Hero */
.hero {
  position: relative;
  height: 400px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  margin: -20px -20px 0;
  border-radius: 0 0 24px 24px;
}

.hero-content {
  text-align: center;
  color: #fff;
  z-index: 1;
}

.hero-title {
  font-size: 48px;
  font-weight: 800;
  margin-bottom: 16px;
  letter-spacing: 2px;
}

.hero-subtitle {
  font-size: 18px;
  opacity: 0.9;
  margin-bottom: 32px;
  line-height: 1.6;
}

.hero-btn {
  background: #fff;
  color: #667eea;
  border: none;
  font-size: 16px;
  font-weight: 600;
  padding: 14px 40px;
  border-radius: 30px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.hero-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.hero-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.hero-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -80px;
  right: -60px;
}

.circle-2 {
  width: 200px;
  height: 200px;
  bottom: -40px;
  left: 10%;
}

.circle-3 {
  width: 100px;
  height: 100px;
  top: 30%;
  left: 20%;
}

/* Section */
.section {
  max-width: 1200px;
  margin: 60px auto 0;
  padding: 0 20px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.section-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
}

.section-header p {
  font-size: 14px;
  color: #909399;
  margin-top: 6px;
}

/* Features */
.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.feature-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px 28px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: transform 0.3s, box-shadow 0.3s;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.feature-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.feature-card h3 {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 10px;
  color: #1a1a2e;
}

.feature-card p {
  font-size: 14px;
  color: #909399;
  line-height: 1.7;
}

/* Checkin Banner */
.checkin-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  margin: 0 auto 32px;
  max-width: 1200px;
}
.banner-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 32px;
}
.banner-text {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}
.banner-icon { font-size: 28px; }

/* Membership Cards */
.membership-cards .section-header {
  position: relative;
}
.section-subtitle {
  font-size: 13px;
  color: #909399;
  margin-left: 12px;
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.card-card {
  border-radius: 16px;
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
  cursor: default;
  border: none;
}
.card-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
}
.card-card :deep(.el-card__body) {
  padding: 0;
}

.card-header {
  height: 110px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
}
.card-header-session { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.card-header-weekly { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }
.card-header-monthly { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.card-header-quarterly { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }

.card-icon-wrapper {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}
.card-emoji {
  font-size: 28px;
  line-height: 1;
}
.card-type-label {
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 1px;
}

.card-body {
  padding: 20px 18px 18px;
  text-align: center;
}
.card-name {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 6px 0;
}
.card-desc {
  font-size: 13px;
  color: #909399;
  margin: 0 0 14px 0;
  min-height: 20px;
}
.card-price-row {
  margin-bottom: 14px;
}
.card-price-from {
  font-size: 16px;
  font-weight: 600;
  color: #f56c6c;
  vertical-align: top;
}
.card-price-value {
  font-size: 32px;
  font-weight: 800;
  color: #f56c6c;
  line-height: 1;
}
.card-price-suffix {
  font-size: 14px;
  color: #909399;
  margin-left: 2px;
}
.card-buy-btn {
  width: 100%;
  font-weight: 600;
  border: none;
  color: #fff;
}
.card-btn-session { background: linear-gradient(135deg, #667eea, #764ba2); }
.card-btn-weekly { background: linear-gradient(135deg, #43e97b, #38f9d7); color: #1a1a2e !important; }
.card-btn-monthly { background: linear-gradient(135deg, #f093fb, #f5576c); }
.card-btn-quarterly { background: linear-gradient(135deg, #fa709a, #fee140); color: #1a1a2e !important; }

/* Card Placeholder */
.card-placeholder {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.card-placeholder-header {
  height: 110px;
  background: #f5f7fa;
}
.card-placeholder-body {
  padding: 20px 18px;
}
.card-placeholder-body .placeholder-line {
  margin-bottom: 10px;
}

/* Buy Dialog */
.buy-card-info {
  border-radius: 12px;
  overflow: hidden;
}
.buy-card-header {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.buy-card-type {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 2px;
}
.buy-card-detail {
  padding: 16px 20px;
  text-align: center;
}
.buy-card-detail h3 {
  margin: 0 0 6px 0;
  font-size: 18px;
  color: #1a1a2e;
}
.buy-desc {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #909399;
}
.buy-price {
  font-size: 28px;
  font-weight: 800;
  color: #f56c6c;
}
.buy-wallet-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}
.wallet-balance {
  margin-left: auto;
  font-size: 13px;
  color: #909399;
}

/* Venue Select in Dialog */
.venue-select-area {
  margin-bottom: 4px;
}
.venue-select-label {
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}
.selected-venue-info {
  margin-top: 14px;
  padding: 14px 16px;
  background: #f5f7fa;
  border-radius: 10px;
}
.venue-detail-row {
  display: flex;
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
}
.venue-detail-label {
  color: #909399;
  min-width: 50px;
  flex-shrink: 0;
}
.buy-price-row {
  display: flex;
  align-items: center;
  margin-top: 8px;
}
.buy-price-label {
  color: #909399;
  font-size: 13px;
  min-width: 50px;
  flex-shrink: 0;
}
.buy-price {
  font-size: 24px;
  font-weight: 800;
  color: #f56c6c;
}

/* Courses Grid */
.courses-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.course-card {
  cursor: pointer;
  border-radius: 12px;
  overflow: hidden;
  transition: transform 0.2s;
}

.course-card:hover {
  transform: translateY(-4px);
}

.course-cover {
  height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-yoga { background: linear-gradient(135deg, #67c23a, #85ce61); }
.cover-strength { background: linear-gradient(135deg, #f56c6c, #f89880); }
.cover-cardio { background: linear-gradient(135deg, #e6a23c, #ebb563); }
.cover-hiit { background: linear-gradient(135deg, #f56c6c, #e65d5d); }
.cover-pilates { background: linear-gradient(135deg, #67c23a, #95d475); }
.cover-dance { background: linear-gradient(135deg, #409eff, #79bbff); }
.cover-boxing { background: linear-gradient(135deg, #909399, #b4b4b4); }
.cover-default { background: linear-gradient(135deg, #409eff, #66b1ff); }

.course-info {
  padding: 16px;
}

.course-header {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.course-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 6px;
  color: #1a1a2e;
}

.course-venue {
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
}

.course-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.course-price {
  font-size: 18px;
  font-weight: 700;
  color: #f56c6c;
}

.course-price.free {
  color: #67c23a;
}

.course-rating {
  font-size: 13px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 2px;
}

/* Course Placeholder */
.course-card-placeholder {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.course-cover-placeholder {
  height: 140px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.course-info-placeholder {
  padding: 16px;
}

/* Articles Grid */
.articles-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.article-card {
  cursor: pointer;
  border-radius: 12px;
  overflow: hidden;
  transition: transform 0.2s;
}

.article-card:hover {
  transform: translateY(-4px);
}

.article-cover {
  height: 140px;
  background: linear-gradient(135deg, #a8d8ea, #aa96da);
  display: flex;
  align-items: center;
  justify-content: center;
}

.article-info {
  padding: 16px;
}

.article-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 8px;
  line-height: 1.5;
}

.article-excerpt {
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
  margin-bottom: 12px;
}

.article-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #c0c4cc;
}

.article-author {
  display: flex;
  align-items: center;
  gap: 4px;
}

.article-stats {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* Article Placeholder */
.article-card-placeholder {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.article-cover-placeholder {
  height: 140px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.article-info-placeholder {
  padding: 16px;
}

/* Placeholder lines */
.placeholder-line {
  height: 12px;
  background: #f0f0f0;
  border-radius: 6px;
  margin-bottom: 10px;
}

.w-60 { width: 60%; }
.w-80 { width: 80%; }
.w-40 { width: 40%; }
.w-100 { width: 100%; }
.w-50 { width: 50%; }

/* Responsive */
@media (max-width: 1000px) {
  .courses-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .cards-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hero {
    height: 320px;
  }

  .hero-title {
    font-size: 32px;
  }

  .hero-subtitle {
    font-size: 15px;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .courses-grid {
    grid-template-columns: 1fr;
  }
  .cards-grid {
    grid-template-columns: 1fr;
  }

  .articles-grid {
    grid-template-columns: 1fr;
  }
}
</style>
