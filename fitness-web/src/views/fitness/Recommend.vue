<template>
  <div class="recommend-page">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <div class="welcome-bg" :style="{ background: welcomeGradient }">
        <div class="welcome-content">
          <div class="welcome-left">
            <h1 class="welcome-title">
              你好，{{ userNickname }}！
            </h1>
            <p class="welcome-desc">
              根据你的偏好和身体数据，为你推荐以下课程和教练
            </p>
          </div>
          <div class="welcome-illustration">
            <div class="mascot" v-html="mascotSvg"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 用户画像摘要卡片 -->
    <div class="profile-summary" v-if="userProfile && !profileEmpty">
      <el-card class="summary-card" shadow="hover">
        <div class="summary-grid">
          <div class="summary-item">
            <span class="summary-label">BMI</span>
            <span class="summary-value">{{ userProfile.bmi || '-' }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">健身目标</span>
            <span class="summary-value">{{ goalMap[userProfile.fitnessGoal] || userProfile.fitnessGoal || '-' }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">运动频率</span>
            <span class="summary-value">{{ freqMap[userProfile.exerciseFrequency] || userProfile.exerciseFrequency || '-' }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">身体类型</span>
            <span class="summary-value">{{ bodyTypeLabel }}</span>
          </div>
        </div>
        <div class="summary-tags" v-if="behaviorTags.length || userProfile.tags">
          <el-tag
            v-for="tag in behaviorTags"
            :key="tag"
            size="small"
            effect="plain"
            type="info"
          >
            {{ tag }}
          </el-tag>
        </div>
      </el-card>
    </div>

    <div class="profile-summary" v-else-if="!profileLoading">
      <el-card class="summary-card summary-empty" shadow="hover">
        <el-empty description="请完善你的个人信息和健身目标，以获得更精准的推荐">
          <el-button type="primary" @click="$router.push('/user/profile')">
            完善个人信息
          </el-button>
        </el-empty>
      </el-card>
    </div>

    <div class="profile-summary" v-else>
      <el-card class="summary-card" shadow="hover">
        <el-skeleton animated :rows="3" />
      </el-card>
    </div>

    <!-- 推荐标签页 -->
    <div class="recommend-tabs-wrapper">
      <el-tabs v-model="activeTab" type="border-card" @tab-change="handleTabChange">
        <!-- 推荐课程 -->
        <el-tab-pane label="推荐课程" name="course">
          <div v-loading="courseLoading" class="tab-content-wrapper">
            <!-- 加载态 -->
            <template v-if="courseLoading">
              <div class="recommend-grid">
                <div v-for="n in 6" :key="n" class="skeleton-item">
                  <el-skeleton animated>
                    <template #template>
                      <el-skeleton-item variant="image" style="width:100%;height:140px" />
                      <div style="padding:16px">
                        <el-skeleton-item variant="text" style="width:60%" />
                        <el-skeleton-item variant="text" style="width:40%" />
                        <el-skeleton-item variant="text" style="width:80%" />
                      </div>
                    </template>
                  </el-skeleton>
                </div>
              </div>
            </template>

            <!-- 错误 -->
            <template v-else-if="courseError">
              <el-empty description="推荐加载失败">
                <el-button type="primary" @click="fetchRecommendedCourses">重新加载</el-button>
              </el-empty>
            </template>

            <!-- 空状态 -->
            <template v-else-if="recommendCourses.length === 0">
              <el-empty description="暂无推荐课程">
                <template #image>
                  <el-icon :size="80" color="#c0c4cc"><svg viewBox="0 0 1024 1024" width="80" height="80"><path d="M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64zm0 820c-205.4 0-372-166.6-372-372s166.6-372 372-372 372 166.6 372 372-166.6 372-372 372z" fill="currentColor"/></svg></el-icon>
                </template>
                <p class="empty-tip">多浏览和预约课程，系统会不断学习你的偏好</p>
              </el-empty>
            </template>

            <!-- 课程推荐卡片 -->
            <template v-else>
              <div class="recommend-grid">
                <el-card
                  v-for="item in recommendCourses"
                  :key="item.courseId || item.id"
                  class="recommend-card course-recommend-card"
                  shadow="hover"
                  @click="$router.push(`/fitness/course/${item.courseId || item.id}`)"
                >
                  <div class="card-score-badge" :style="{ background: getScoreColor(item.score) }">
                    推荐度 {{ formatScore(item.score) }}%
                  </div>
                  <div class="card-type-bar" :style="{ background: getCourseTypeColor(item.type || item.courseType) }" />
                  <div class="card-body-content">
                    <h3 class="rec-card-title">{{ item.courseName || item.name }}</h3>
                    <p class="rec-card-meta">{{ item.venueName || '-' }}</p>
                    <div class="rec-card-tags">
                      <el-tag size="small" type="info">{{ rcTypeMap[item.type || item.courseType] || item.type || item.courseType || '-' }}</el-tag>
                      <el-tag size="small" :type="getDifficultyType(item.difficulty)">
                        {{ rcDiffMap[item.difficulty] || item.difficulty || '-' }}
                      </el-tag>
                    </div>
                    <div class="rec-card-footer">
                      <span class="rec-card-price">
                        {{ (item.price || 0) === 0 ? '免费' : `¥${item.price}` }}
                      </span>
                      <span class="rec-card-coach" v-if="item.coachName">教练：{{ item.coachName }}</span>
                    </div>
                    <el-progress
                      :percentage="Math.min(100, Math.round((item.score || 0) * 100))"
                      :stroke-width="6"
                      :show-text="false"
                      class="match-bar"
                      :color="getScoreColor(item.score)"
                    />
                  </div>
                </el-card>
              </div>
            </template>
          </div>
        </el-tab-pane>

        <!-- 推荐教练 -->
        <el-tab-pane label="推荐教练" name="coach">
          <div v-loading="coachLoading" class="tab-content-wrapper">
            <!-- 加载态 -->
            <template v-if="coachLoading">
              <div class="recommend-coach-grid">
                <div v-for="n in 4" :key="n" class="skeleton-item">
                  <el-skeleton animated>
                    <template #template>
                      <div style="display:flex;flex-direction:column;align-items:center;padding:24px">
                        <el-skeleton-item variant="circle" style="width:72px;height:72px" />
                        <el-skeleton-item variant="text" style="width:50%;margin-top:12px" />
                        <el-skeleton-item variant="text" style="width:60%;margin-top:8px" />
                      </div>
                    </template>
                  </el-skeleton>
                </div>
              </div>
            </template>

            <!-- 错误 -->
            <template v-else-if="coachError">
              <el-empty description="推荐加载失败">
                <el-button type="primary" @click="fetchRecommendedCoaches">重新加载</el-button>
              </el-empty>
            </template>

            <!-- 空状态 -->
            <template v-else-if="recommendCoaches.length === 0">
              <el-empty description="暂无推荐教练">
                <p class="empty-tip">多浏览和预约课程，系统会不断学习你的偏好</p>
              </el-empty>
            </template>

            <!-- 教练推荐卡片 -->
            <template v-else>
              <div class="recommend-coach-grid">
                <el-card
                  v-for="item in recommendCoaches"
                  :key="item.coachId || item.id"
                  class="recommend-card coach-recommend-card"
                  shadow="hover"
                  @click="$router.push(`/fitness/coach/${item.coachId || item.id}`)"
                >
                  <div class="card-score-badge" :style="{ background: getScoreColor(item.score) }">
                    推荐度 {{ formatScore(item.score) }}%
                  </div>
                  <div class="coach-card-body">
                    <el-avatar :size="72" :src="item.avatar" class="rec-coach-avatar">
                      {{ (item.coachName || item.name || '教').charAt(0) }}
                    </el-avatar>
                    <h3 class="rec-card-title">
                      {{ item.coachName || item.name }}
                    </h3>
                    <div class="rec-card-tags coach-specialty-tags">
                      <el-tag
                        v-for="tag in getSpecialtyTags(item)"
                        :key="tag"
                        size="small"
                        type="info"
                      >
                        {{ tag }}
                      </el-tag>
                    </div>
                    <div class="rec-coach-rating">
                      <el-rate :model-value="item.rating || 0" disabled size="small" />
                    </div>
                    <el-progress
                      :percentage="Math.min(100, Math.round((item.score || 0) * 100))"
                      :stroke-width="6"
                      :show-text="false"
                      class="match-bar"
                      :color="getScoreColor(item.score)"
                    />
                  </div>
                </el-card>
              </div>
            </template>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 底部提示 -->
    <div class="bottom-tip">
      <el-alert
        type="info"
        :closable="false"
        show-icon
      >
        <template #title>
          推荐不够精准？多浏览和预约课程，系统会不断学习你的偏好，提供更准确的推荐~
        </template>
      </el-alert>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRecommendedCourses, getRecommendedCoaches } from '@/api/recommend'
import { getUserProfile } from '@/api/user'

const router = useRouter()

// 中英文映射
const goalMap = { LOSE_WEIGHT: '减脂', BUILD_MUSCLE: '增肌', SHAPE: '塑形', GAIN_WEIGHT: '增重', KEEP_HEALTHY: '保持健康' }
const freqMap = { DAILY: '每天', FIVE_TIMES_WEEK: '每周五次', EVERY_OTHER_DAY: '隔天', TWICE_WEEK: '每周两次', WEEKLY: '每周一次', RARELY: '偶尔' }
const rcTypeMap = { YOGA: '瑜伽', STRENGTH: '力量训练', CARDIO: '有氧运动', DANCE: '舞蹈', SWIMMING: '游泳', BASKETBALL: '篮球', BOXING: '搏击', PILATES: '普拉提', SPINNING: '动感单车', OTHER: '其他' }
const rcDiffMap = { BEGINNER: '初级', INTERMEDIATE: '中级', ADVANCED: '高级' }

// 标签
const activeTab = ref('course')

// 用户信息
const profileLoading = ref(false)
const userProfile = ref(null)
const profileEmpty = ref(false)

// 推荐课程
const courseLoading = ref(false)
const courseError = ref(false)
const recommendCourses = ref([])

// 推荐教练
const coachLoading = ref(false)
const coachError = ref(false)
const recommendCoaches = ref([])

// 用户昵称
const userNickname = computed(() => {
  if (userProfile.value?.nickname || userProfile.value?.name) {
    return userProfile.value.nickname || userProfile.value.name
  }
  try {
    const userStr = localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo')
    if (userStr) {
      const user = JSON.parse(userStr)
      return user.nickname || user.username || '健身达人'
    }
  } catch { /* ignore */ }
  return '健身达人'
})

// 身体类型（根据BMI推算）
const bodyTypeLabel = computed(() => {
  const bmi = parseFloat(userProfile.value?.bmi) || 0
  if (bmi <= 0) return '-'
  if (bmi < 18.5) return '偏瘦'
  if (bmi < 24) return '标准'
  if (bmi < 28) return '偏胖'
  return '肥胖'
})

// 行为标签
const behaviorTags = computed(() => {
  const tags = userProfile.value?.tags || userProfile.value?.behaviorTags || []
  return Array.isArray(tags) ? tags : (typeof tags === 'string' ? tags.split(/[,，、]/) : [])
})

// 欢迎区域渐变
const welcomeGradient = 'linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%)'

const mascotSvg = computed(() => {
  const goal = userProfile.value?.fitnessGoal || ''
  const bmi = userProfile.value?.bmi || 22

  // 减脂 — 跑步者 3D 风格
  if (goal === 'LOSE_WEIGHT' || bmi >= 25) {
    return `<svg viewBox="0 0 160 180" width="120" height="135" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <radialGradient id="gHead1" cx="50%" cy="40%"><stop offset="0%" stop-color="#fff"/><stop offset="100%" stop-color="#d4c5b0"/></radialGradient>
        <radialGradient id="gBody1" cx="50%" cy="30%"><stop offset="0%" stop-color="#ff6b6b"/><stop offset="100%" stop-color="#c0392b"/></radialGradient>
        <radialGradient id="gLeg1" cx="50%" cy="30%"><stop offset="0%" stop-color="#34495e"/><stop offset="100%" stop-color="#1a252f"/></radialGradient>
        <filter id="shadow1"><feDropShadow dx="0" dy="2" stdDeviation="2" flood-opacity="0.3"/></filter>
      </defs>
      <ellipse cx="80" cy="158" rx="60" ry="8" fill="rgba(255,255,255,0.15)"/>
      <circle cx="80" cy="34" r="16" fill="url(#gHead1)" filter="url(#shadow1)"/>
      <rect x="68" y="50" width="24" height="42" rx="12" fill="url(#gBody1)" filter="url(#shadow1)"/>
      <line x1="55" y1="50" x2="30" y2="40" stroke="#ff6b6b" stroke-width="9" stroke-linecap="round"/>
      <line x1="105" y1="50" x2="130" y2="38" stroke="#ff6b6b" stroke-width="9" stroke-linecap="round"/>
      <line x1="68" y1="90" x2="48" y2="138" stroke="url(#gLeg1)" stroke-width="10" stroke-linecap="round"/>
      <line x1="92" y1="90" x2="118" y2="135" stroke="url(#gLeg1)" stroke-width="10" stroke-linecap="round"/>
      <circle cx="48" cy="140" r="7" fill="#e74c3c"/>
      <circle cx="118" cy="137" r="7" fill="#e74c3c"/>
      <text x="105" y="28" font-size="28">🔥</text>
    </svg>`
  }

  // 增肌 — 力量训练者 3D 风格
  if (goal === 'BUILD_MUSCLE' || bmi < 18.5) {
    return `<svg viewBox="0 0 160 180" width="120" height="135" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <radialGradient id="gHead2" cx="50%" cy="40%"><stop offset="0%" stop-color="#fff"/><stop offset="100%" stop-color="#d4c5b0"/></radialGradient>
        <radialGradient id="gBody2" cx="50%" cy="30%"><stop offset="0%" stop-color="#3498db"/><stop offset="100%" stop-color="#1a5276"/></radialGradient>
        <radialGradient id="gLeg2" cx="50%" cy="30%"><stop offset="0%" stop-color="#2c3e50"/><stop offset="100%" stop-color="#1a252f"/></radialGradient>
        <filter id="shadow2"><feDropShadow dx="0" dy="2" stdDeviation="2" flood-opacity="0.3"/></filter>
      </defs>
      <ellipse cx="80" cy="158" rx="60" ry="8" fill="rgba(255,255,255,0.15)"/>
      <circle cx="80" cy="30" r="17" fill="url(#gHead2)" filter="url(#shadow2)"/>
      <rect x="64" y="47" width="32" height="48" rx="14" fill="url(#gBody2)" filter="url(#shadow2)"/>
      <line x1="52" y1="55" x2="22" y2="40" stroke="#3498db" stroke-width="11" stroke-linecap="round"/>
      <rect x="20" y="34" width="22" height="22" rx="5" fill="#2c3e50" stroke="#1a5276" stroke-width="2"/>
      <line x1="108" y1="55" x2="138" y2="40" stroke="#3498db" stroke-width="11" stroke-linecap="round"/>
      <rect x="116" y="34" width="22" height="22" rx="5" fill="#2c3e50" stroke="#1a5276" stroke-width="2"/>
      <line x1="64" y1="93" x2="44" y2="140" stroke="url(#gLeg2)" stroke-width="11" stroke-linecap="round"/>
      <line x1="96" y1="93" x2="116" y2="140" stroke="url(#gLeg2)" stroke-width="11" stroke-linecap="round"/>
      <circle cx="44" cy="142" r="7" fill="#e74c3c"/>
      <circle cx="116" cy="142" r="7" fill="#e74c3c"/>
      <text x="108" y="152" font-size="26">💪</text>
    </svg>`
  }

  // 塑形 — 瑜伽者 3D 风格
  if (goal === 'SHAPE') {
    return `<svg viewBox="0 0 160 180" width="120" height="135" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <radialGradient id="gHead3" cx="50%" cy="40%"><stop offset="0%" stop-color="#fff"/><stop offset="100%" stop-color="#d4c5b0"/></radialGradient>
        <radialGradient id="gBody3" cx="50%" cy="30%"><stop offset="0%" stop-color="#9b59b6"/><stop offset="100%" stop-color="#6c3483"/></radialGradient>
        <radialGradient id="gLeg3" cx="50%" cy="30%"><stop offset="0%" stop-color="#2ecc71"/><stop offset="100%" stop-color="#1e8449"/></radialGradient>
        <filter id="shadow3"><feDropShadow dx="0" dy="2" stdDeviation="2" flood-opacity="0.3"/></filter>
      </defs>
      <ellipse cx="80" cy="160" rx="55" ry="8" fill="rgba(255,255,255,0.15)"/>
      <circle cx="80" cy="28" r="15" fill="url(#gHead3)" filter="url(#shadow3)"/>
      <rect x="68" y="43" width="24" height="40" rx="12" fill="url(#gBody3)" filter="url(#shadow3)"/>
      <line x1="68" y1="52" x2="35" y2="70" stroke="#9b59b6" stroke-width="8" stroke-linecap="round"/>
      <line x1="92" y1="52" x2="125" y2="72" stroke="#9b59b6" stroke-width="8" stroke-linecap="round"/>
      <line x1="68" y1="82" x2="50" y2="128" stroke="url(#gLeg3)" stroke-width="10" stroke-linecap="round"/>
      <line x1="92" y1="82" x2="110" y2="130" stroke="url(#gLeg3)" stroke-width="10" stroke-linecap="round"/>
      <circle cx="50" cy="130" r="6" fill="#27ae60"/>
      <circle cx="110" cy="132" r="6" fill="#27ae60"/>
      <text x="105" y="25" font-size="24">🧘</text>
    </svg>`
  }

  // 增重 — 吃货小人 3D 风格
  if (goal === 'GAIN_WEIGHT') {
    return `<svg viewBox="0 0 160 180" width="120" height="135" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <radialGradient id="gHead4" cx="50%" cy="40%"><stop offset="0%" stop-color="#fff"/><stop offset="100%" stop-color="#f5d5a0"/></radialGradient>
        <radialGradient id="gBody4" cx="50%" cy="30%"><stop offset="0%" stop-color="#f39c12"/><stop offset="100%" stop-color="#d68910"/></radialGradient>
        <radialGradient id="gLeg4" cx="50%" cy="30%"><stop offset="0%" stop-color="#7d3c98"/><stop offset="100%" stop-color="#4a235a"/></radialGradient>
        <filter id="shadow4"><feDropShadow dx="0" dy="2" stdDeviation="2" flood-opacity="0.3"/></filter>
      </defs>
      <ellipse cx="80" cy="158" rx="60" ry="8" fill="rgba(255,255,255,0.15)"/>
      <circle cx="80" cy="32" r="16" fill="url(#gHead4)" filter="url(#shadow4)"/>
      <rect x="66" y="48" width="28" height="44" rx="14" fill="url(#gBody4)" filter="url(#shadow4)"/>
      <line x1="54" y1="56" x2="30" y2="48" stroke="#f39c12" stroke-width="9" stroke-linecap="round"/>
      <line x1="106" y1="56" x2="130" y2="48" stroke="#f39c12" stroke-width="9" stroke-linecap="round"/>
      <line x1="66" y1="90" x2="48" y2="138" stroke="url(#gLeg4)" stroke-width="10" stroke-linecap="round"/>
      <line x1="94" y1="90" x2="112" y2="138" stroke="url(#gLeg4)" stroke-width="10" stroke-linecap="round"/>
      <circle cx="48" cy="140" r="6" fill="#8e44ad"/>
      <circle cx="112" cy="140" r="6" fill="#8e44ad"/>
      <text x="110" y="52" font-size="22">🍗</text>
      <text x="30" y="145" font-size="20">🍚</text>
    </svg>`
  }

  // 默认：保持健康 — 阳光小人 3D 风格
  return `<svg viewBox="0 0 160 180" width="120" height="135" xmlns="http://www.w3.org/2000/svg">
    <defs>
      <radialGradient id="gHead5" cx="50%" cy="40%"><stop offset="0%" stop-color="#fff"/><stop offset="100%" stop-color="#d4c5b0"/></radialGradient>
      <radialGradient id="gBody5" cx="50%" cy="30%"><stop offset="0%" stop-color="#2ecc71"/><stop offset="100%" stop-color="#1e8449"/></radialGradient>
      <radialGradient id="gLeg5" cx="50%" cy="30%"><stop offset="0%" stop-color="#3498db"/><stop offset="100%" stop-color="#1a5276"/></radialGradient>
      <filter id="shadow5"><feDropShadow dx="0" dy="2" stdDeviation="2" flood-opacity="0.3"/></filter>
    </defs>
    <ellipse cx="80" cy="158" rx="60" ry="8" fill="rgba(255,255,255,0.15)"/>
    <circle cx="80" cy="32" r="16" fill="url(#gHead5)" filter="url(#shadow5)"/>
    <rect x="68" y="48" width="24" height="44" rx="12" fill="url(#gBody5)" filter="url(#shadow5)"/>
    <line x1="56" y1="56" x2="30" y2="44" stroke="#2ecc71" stroke-width="9" stroke-linecap="round"/>
    <line x1="104" y1="56" x2="130" y2="44" stroke="#2ecc71" stroke-width="9" stroke-linecap="round"/>
    <line x1="68" y1="90" x2="48" y2="138" stroke="url(#gLeg5)" stroke-width="10" stroke-linecap="round"/>
    <line x1="92" y1="90" x2="112" y2="138" stroke="url(#gLeg5)" stroke-width="10" stroke-linecap="round"/>
    <circle cx="48" cy="140" r="6" fill="#2980b9"/>
    <circle cx="112" cy="140" r="6" fill="#2980b9"/>
    <text x="50" y="25" font-size="26">☀️</text>
  </svg>`
})

function getScoreColor(score) {
  const s = typeof score === 'number' ? score : 0
  if (s >= 0.8) return '#67c23a'
  if (s >= 0.6) return '#409eff'
  if (s >= 0.4) return '#e6a23c'
  return '#909399'
}

function formatScore(score) {
  if (score === undefined || score === null) return 0
  return Math.round((typeof score === 'number' ? score : 0) * 100)
}

function getCourseTypeColor(type) {
  const map = {
    '瑜伽': '#67c23a', '力量训练': '#f56c6c', '有氧': '#409eff', '有氧运动': '#409eff', '舞蹈': '#e6a23c',
    '游泳': '#36cfc9', '篮球': '#f759ab', '搏击': '#9254de', '普拉提': '#597ef7',
    '动感单车': '#ff9c6c', '其他': '#8c8c8c',
    YOGA: '#67c23a', STRENGTH: '#f56c6c', CARDIO: '#409eff', DANCE: '#e6a23c',
    SWIMMING: '#36cfc9', BASKETBALL: '#f759ab', BOXING: '#9254de', PILATES: '#597ef7',
    SPINNING: '#ff9c6c', OTHER: '#8c8c8c'
  }
  return map[type] || '#8c8c8c'
}

function getDifficultyType(d) {
  const map = { '初级': 'success', '中级': 'warning', '高级': 'danger', BEGINNER: 'success', INTERMEDIATE: 'warning', ADVANCED: 'danger' }
  return map[d] || 'info'
}

function getSpecialtyTags(item) {
  const val = item?.specialtyList || item?.specialties || item?.specialty || []
  if (!val) return []
  if (Array.isArray(val)) return val.slice(0, 3)
  if (typeof val === 'string') return val.split(/[,，、]/).slice(0, 3)
  return []
}

// 获取用户画像
async function fetchUserProfile() {
  profileLoading.value = true
  try {
    const res = await getUserProfile()
    const data = res?.data || res
    if (data && (data.bmi || data.fitnessGoal || data.bodyType || data.exerciseFrequency)) {
      userProfile.value = data
      profileEmpty.value = false
    } else {
      profileEmpty.value = true
    }
  } catch (err) {
    console.error('获取用户画像失败:', err)
    profileEmpty.value = true
  } finally {
    profileLoading.value = false
  }
}

// 获取推荐课程
async function fetchRecommendedCourses() {
  courseLoading.value = true
  courseError.value = false
  try {
    const res = await getRecommendedCourses()
    // 后端返回 {course: {...}, score, venueName}，扁平化供模板使用
    const raw = res?.data || []
    // 归一化分数（后端原始分 0~15+，归一化到 0~1）
    const maxScore = Math.max(...raw.map(r => r.score || 0), 1)
    recommendCourses.value = raw.map(item => ({
      ...item.course,
      id: item.course?.id || item.id,
      courseId: item.course?.id,
      name: item.course?.name,
      courseName: item.course?.name,
      type: item.course?.type,
      difficulty: item.course?.difficulty,
      price: item.course?.price,
      score: item.score ? item.score / maxScore : 0,
      venueName: item.venueName
    }))
  } catch (err) {
    console.error('获取推荐课程失败:', err)
    courseError.value = true
    recommendCourses.value = []
  } finally {
    courseLoading.value = false
  }
}

// 获取推荐教练
async function fetchRecommendedCoaches() {
  coachLoading.value = true
  coachError.value = false
  try {
    const res = await getRecommendedCoaches()
    // 后端返回 {coach: {...}, score}，扁平化供模板使用
    const raw = res?.data || []
    const maxScore = Math.max(...raw.map(r => r.score || 0), 1)
    recommendCoaches.value = raw.map(item => ({
      ...item.coach,
      id: item.coach?.id || item.id,
      name: item.coach?.name,
      specialtyList: item.coach?.specialty ? item.coach.specialty.split(',').filter(Boolean) : [],
      rating: item.coach?.rating || 0,
      score: item.score ? item.score / maxScore : 0
    }))
  } catch (err) {
    console.error('获取推荐教练失败:', err)
    coachError.value = true
    recommendCoaches.value = []
  } finally {
    coachLoading.value = false
  }
}

function handleTabChange(tabName) {
  if (tabName === 'course' && recommendCourses.value.length === 0 && !courseLoading.value) {
    fetchRecommendedCourses()
  } else if (tabName === 'coach' && recommendCoaches.value.length === 0 && !coachLoading.value) {
    fetchRecommendedCoaches()
  }
}

onMounted(() => {
  fetchUserProfile()
  fetchRecommendedCourses()
  fetchRecommendedCoaches()
})
</script>

<style scoped>
.recommend-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px 40px;
}

/* 欢迎区域 */
.welcome-section {
  margin-bottom: 20px;
}

.welcome-bg {
  border-radius: 12px;
  overflow: hidden;
}

.welcome-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 36px 40px;
  min-height: 140px;
}

.welcome-title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px 0;
}

.welcome-desc {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.75);
  margin: 0;
}

.welcome-illustration {
  flex-shrink: 0;
}

.mascot {
  width: 100px;
  height: 100px;
  animation: mascotFloat 3s ease-in-out infinite;
}

@keyframes mascotFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

/* 用户画像 */
.profile-summary {
  margin-bottom: 20px;
}

.summary-card {
  border-radius: 8px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 12px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-label {
  font-size: 12px;
  color: #909399;
  text-transform: uppercase;
}

.summary-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.summary-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.summary-empty {
  min-height: 120px;
  display: flex;
  align-items: center;
}

/* 推荐标签页 */
.recommend-tabs-wrapper {
  margin-bottom: 20px;
}

.tab-content-wrapper {
  min-height: 300px;
}

/* 课程推荐网格 */
.recommend-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.recommend-card {
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  position: relative;
  overflow: hidden;
  padding: 0;
}

.recommend-card:hover {
  transform: translateY(-4px);
}

.card-score-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 3px 12px;
  border-radius: 12px;
  z-index: 1;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.card-type-bar {
  height: 6px;
  margin: 0;
}

.card-body-content {
  padding: 18px 16px 16px;
}

.rec-card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 6px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rec-card-meta {
  font-size: 13px;
  color: #909399;
  margin: 0 0 8px 0;
}

.rec-card-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.rec-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.rec-card-price {
  font-size: 16px;
  font-weight: 700;
  color: #f56c6c;
}

.rec-card-coach {
  font-size: 12px;
  color: #909399;
}

.match-bar {
  margin-top: 4px;
}

/* 教练推荐网格 */
.recommend-coach-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.coach-recommend-card {
  text-align: center;
}

.coach-card-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 28px 16px 20px;
}

.rec-coach-avatar {
  margin-bottom: 12px;
}

.coach-specialty-tags {
  justify-content: center;
  min-height: 28px;
  margin-bottom: 8px;
}

.rec-coach-rating {
  margin-bottom: 10px;
}

/* 骨架 */
.skeleton-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

/* 空状态提示 */
.empty-tip {
  color: #909399;
  font-size: 13px;
  margin-top: 8px;
}

/* 底部提示 */
.bottom-tip {
  margin-top: 8px;
}

@media (max-width: 900px) {
  .recommend-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .recommend-coach-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .summary-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .welcome-content {
    padding: 24px;
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }
  .welcome-title {
    font-size: 22px;
  }
}

@media (max-width: 600px) {
  .recommend-grid,
  .recommend-coach-grid {
    grid-template-columns: 1fr;
  }
  .summary-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
