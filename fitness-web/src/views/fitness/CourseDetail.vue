<template>
  <div class="course-detail-page" v-loading="loading">
    <!-- 错误状态 -->
    <template v-if="error">
      <el-result icon="error" title="加载失败" sub-title="无法获取课程信息">
        <template #extra>
          <el-button type="primary" @click="fetchCourseDetail">重新加载</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </template>
      </el-result>
    </template>

    <template v-else-if="!loading && course">
      <div class="detail-layout">
        <!-- 主内容区 -->
        <div class="main-content">
          <!-- 头部信息 -->
          <div class="course-header-card">
            <div class="header-top">
              <h1 class="course-title">{{ course.name }}</h1>
              <div class="header-tags">
                <el-tag size="large" :style="{ background: getTypeColor(course.type), borderColor: getTypeColor(course.type), color: '#fff' }">
                  {{ typeMap[course.type] || course.type || '未分类' }}
                </el-tag>
                <el-tag size="large" :type="getDifficultyType(course.difficulty)">
                  {{ diffMap[course.difficulty] || course.difficulty || '未知难度' }}
                </el-tag>
                <el-tag size="large" :type="getStatusType(course.status)">
                  {{ statusMap[course.status] || course.status || '未知状态' }}
                </el-tag>
              </div>
              <div class="header-price">¥{{ course.price || 0 }}</div>
            </div>
          </div>

          <!-- 信息描述列表 -->
          <el-card class="info-card" shadow="never">
            <template #header>
              <span class="card-header-title">课程信息</span>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="所属场馆">
                <el-link type="primary" @click="goToVenue(course.venueId)">
                  {{ course.venueName || '-' }}
                </el-link>
              </el-descriptions-item>
              <el-descriptions-item label="授课教练">
                <el-link type="primary" @click="goToCoach(course.coachId)">
                  {{ course.coachName || '-' }}
                </el-link>
              </el-descriptions-item>
              <el-descriptions-item label="上课时间">
                {{ fmtClassTime(course.startTime, course.endTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="最大人数">
                {{ course.maxStudents || 0 }} 人
              </el-descriptions-item>
              <el-descriptions-item label="已报名">
                {{ course.enrolledCount || 0 }} 人
              </el-descriptions-item>
              <el-descriptions-item label="剩余名额">
                {{ Math.max(0, (course.maxStudents || 0) - (course.enrolledCount || 0)) }} 人
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 课程描述 -->
          <el-card class="info-card" shadow="never">
            <template #header>
              <span class="card-header-title">课程介绍</span>
            </template>
            <div class="description-content">
              {{ course.description || '暂无详细介绍' }}
            </div>
          </el-card>

          <!-- 教练信息 -->
          <el-card class="info-card" shadow="never" v-if="course.coachName">
            <template #header>
              <span class="card-header-title">授课教练</span>
            </template>
            <div class="coach-info-section">
              <div class="coach-brief">
                <el-avatar :size="56" :src="course.coachAvatar">
                  {{ (course.coachName || '教').charAt(0) }}
                </el-avatar>
                <div class="coach-brief-text">
                  <h4>{{ course.coachName }}</h4>
                  <p>{{ course.coachSpecialty || '-' }}</p>
                  <p class="coach-experience">{{ course.coachExperience || '' }}</p>
                </div>
                <el-button type="primary" plain size="small" @click="goToCoach(course.coachId)">
                  查看详情
                </el-button>
              </div>
            </div>
          </el-card>

          <!-- 场馆信息 -->
          <el-card class="info-card" shadow="never" v-if="course.venueName">
            <template #header>
              <span class="card-header-title">所在场馆</span>
            </template>
            <div class="venue-info-section">
              <div class="venue-brief">
                <div class="venue-brief-text">
                  <h4>{{ course.venueName }}</h4>
                  <p>
                    <el-icon><Location /></el-icon>
                    {{ course.venueAddress || '暂无地址' }}
                  </p>
                </div>
                <el-button type="primary" plain size="small" @click="goToVenue(course.venueId)">
                  查看详情
                </el-button>
              </div>
            </div>
          </el-card>
        </div>

        <!-- 侧边栏操作卡 -->
        <div class="sidebar">
          <el-card class="action-card" shadow="hover">
            <div class="action-price">
              <span class="price-label">课程价格</span>
              <span class="price-value">¥{{ course.price || 0 }}</span>
            </div>
            <el-divider />
            <div class="enrollment-section">
              <div class="enroll-stats">
                <span>已报名 {{ selectedEnrollCount }} / {{ selectedMaxStudents }}</span>
                <span class="enroll-percent">
                  {{ selectedEnrollPercent }}%
                </span>
              </div>
              <el-progress
                :percentage="selectedEnrollPercent"
                :status="selectedEnrollPercent >= 100 ? 'exception' : ''"
                :stroke-width="12"
              />
            </div>
            <!-- 每周重复课程：日期选择 -->
            <div v-if="course.recurrence === 'WEEKLY' && courseDates.length > 1" class="date-select-section">
              <el-divider />
              <h4 class="section-subtitle">选择上课日期</h4>
              <div class="date-chips">
                <div
                  v-for="d in courseDates"
                  :key="d.id"
                  class="date-chip"
                  :class="{ active: selectedDateId === d.id }"
                  @click="selectDate(d)"
                >
                  <div class="chip-date">{{ formatDateShort(d.startTime) }}</div>
                  <div class="chip-time">{{ formatTimeShort(d.startTime) }}~{{ formatTimeShort(d.endTime) }}</div>
                  <div class="chip-spots">{{ d.enrolledCount }}/{{ d.maxStudents }}人</div>
                </div>
              </div>
            </div>
            <el-divider />
            <div class="action-buttons">
              <el-button
                type="primary"
                size="large"
                class="action-btn-primary"
                :disabled="!canBook"
                :loading="bookingLoading"
                @click="handleBook"
              >
                {{ bookButtonText }}
              </el-button>
              <div class="action-btn-row">
                <el-button :icon="Share" circle @click="handleShare" />
                <el-button
                  :icon="isFavorited ? StarFilled : Star"
                  circle
                  :loading="favLoading"
                  @click="toggleFavorite"
                />
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Location, Share, Star, StarFilled } from '@element-plus/icons-vue'
import { getCourseDetail, getCourseDates } from '@/api/course'
import { bookCourse, addFavorite, removeFavorite } from '@/api/user'

const route = useRoute()
const router = useRouter()

const courseId = computed(() => route.params.id)

const typeMap = {
  YOGA: '瑜伽', STRENGTH: '力量训练', CARDIO: '有氧运动', DANCE: '舞蹈',
  SWIMMING: '游泳', BASKETBALL: '篮球', BOXING: '搏击', PILATES: '普拉提',
  SPINNING: '动感单车', OTHER: '其他'
}

const diffMap = {
  BEGINNER: '初级', INTERMEDIATE: '中级', ADVANCED: '高级'
}

const statusMap = {
  UPCOMING: '即将开始', ONGOING: '进行中', FINISHED: '已结束', CANCELLED: '已取消'
}

const loading = ref(false)
const error = ref(false)
const course = ref(null)
const courseDates = ref([])
const selectedDateId = ref(null)

const isFavorited = ref(false)
const favLoading = ref(false)
const bookingLoading = ref(false)

const isLoggedIn = computed(() => {
  try {
    return !!(localStorage.getItem('fitness_token'))
  } catch { return false }
})

const selectedDate = computed(() => {
  if (selectedDateId.value && courseDates.value.length > 0) {
    return courseDates.value.find(d => d.id === selectedDateId.value) || null
  }
  return null
})

const selectedEnrollCount = computed(() => {
  if (selectedDate.value) return selectedDate.value.enrolledCount || 0
  return course.value?.enrolledCount || 0
})

const selectedMaxStudents = computed(() => {
  if (selectedDate.value) return selectedDate.value.maxStudents || 1
  return course.value?.maxStudents || 1
})

const selectedEnrollPercent = computed(() => {
  const max = selectedMaxStudents.value || 1
  const enrolled = selectedEnrollCount.value || 0
  return Math.min(100, Math.round((enrolled / max) * 100))
})

const isFull = computed(() => {
  return selectedEnrollCount.value >= selectedMaxStudents.value
})

const isAlreadyBooked = computed(() => {
  return course.value?.isBooked || false
})

const canBook = computed(() => {
  return isLoggedIn.value && !isFull.value && !isAlreadyBooked.value
})

const bookButtonText = computed(() => {
  if (isAlreadyBooked.value) return '已预约'
  if (isFull.value) return '已满员'
  return '立即预约'
})

function getTypeColor(type) {
  const map = { '瑜伽': '#67c23a', '力量训练': '#f56c6c', '有氧': '#409eff', '有氧运动': '#409eff', '舞蹈': '#e6a23c', '游泳': '#36cfc9', '篮球': '#f759ab', '搏击': '#9254de', '普拉提': '#597ef7', '动感单车': '#ff9c6e', '其他': '#8c8c8c', YOGA: '#67c23a', STRENGTH: '#f56c6c', CARDIO: '#409eff', DANCE: '#e6a23c', SWIMMING: '#36cfc9', BASKETBALL: '#f759ab', BOXING: '#9254de', PILATES: '#597ef7', SPINNING: '#ff9c6e', OTHER: '#8c8c8c' }
  return map[type] || '#8c8c8c'
}

function getDifficultyType(d) {
  const map = { '初级': 'success', '中级': 'warning', '高级': 'danger', BEGINNER: 'success', INTERMEDIATE: 'warning', ADVANCED: 'danger' }
  return map[d] || 'info'
}

function getStatusType(s) {
  const map = { UPCOMING: 'warning', ONGOING: 'success', FINISHED: 'info', CANCELLED: 'danger', OPEN: 'success', CLOSED: 'info', FULL: 'warning', '进行中': 'success', '已结束': 'info', '已取消': 'danger', '已满员': 'warning', '即将开始': 'warning' }
  return map[s] || 'info'
}

async function fetchCourseDetail() {
  loading.value = true
  error.value = false
  try {
    const res = await getCourseDetail(courseId.value)
    course.value = res?.data || res
  } catch (err) {
    console.error('获取课程详情失败:', err)
    error.value = true
  } finally {
    loading.value = false
  }
}

function fixDateStr(str) {
  if (!str) return ''
  return str.replace('T', ' ')
}

function fmtClassTime(start, end) {
  if (!start || !end) return '-'
  const s = new Date(fixDateStr(start)), e = new Date(fixDateStr(end))
  const pad = n => String(n).padStart(2, '0')
  return `${s.getMonth() + 1}月${s.getDate()}日 ${pad(s.getHours())}:${pad(s.getMinutes())}~${pad(e.getHours())}:${pad(e.getMinutes())}`
}

function formatDateShort(time) {
  if (!time) return ''
  const d = new Date(fixDateStr(time))
  return `${d.getMonth()+1}/${d.getDate()} 周${['日','一','二','三','四','五','六'][d.getDay()]}`
}

function formatTimeShort(time) {
  if (!time) return ''
  return new Date(fixDateStr(time)).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function selectDate(d) {
  selectedDateId.value = d.id
}

async function fetchCourseDates() {
  try {
    const res = await getCourseDates(courseId.value)
    courseDates.value = res?.data || []
    if (courseDates.value.length > 0) {
      selectedDateId.value = courseDates.value[0].id
    }
  } catch { courseDates.value = [] }
}

async function handleBook() {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    return
  }
  if (isFull.value) {
    ElMessage.warning(selectedDate.value ? '该日期已满员' : '课程已满员')
    return
  }
  // 使用选中的日期 ID
  const targetId = selectedDateId.value || courseId.value
  bookingLoading.value = true
  try {
    const res = await bookCourse({ courseId: targetId })
    if (res.code === 200) {
      ElMessage.success('预约成功')
      course.value.enrolledCount = (course.value.enrolledCount || 0) + 1
      course.value.isBooked = true
    } else if (res.code === 402) {
      ElMessage.error(res.message || '余额不足，请先充值')
    } else {
      ElMessage.error(res.message || '预约失败')
    }
  } catch (err) {
    console.error('预约失败:', err)
    ElMessage.error('预约失败，请重试')
  } finally {
    bookingLoading.value = false
  }
}

async function toggleFavorite() {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    return
  }
  favLoading.value = true
  try {
    if (isFavorited.value) {
      await removeFavorite('COURSE', courseId.value)
      isFavorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite('COURSE', courseId.value)
      isFavorited.value = true
      ElMessage.success('已收藏')
    }
  } catch (err) {
    console.error('收藏操作失败:', err)
    ElMessage.error('操作失败')
  } finally {
    favLoading.value = false
  }
}

function handleShare() {
  const url = window.location.href
  navigator.clipboard?.writeText(url).then(() => {
    ElMessage.success('课程链接已复制到剪贴板')
  }).catch(() => {
    ElMessage.info('分享链接: ' + url)
  })
}

function goToVenue(id) {
  if (id) router.push(`/fitness/venue/${id}`)
}

function goToCoach(id) {
  if (id) router.push(`/fitness/coach/${id}`)
}

onMounted(() => {
  fetchCourseDetail()
  fetchCourseDates()
})
</script>

<style scoped>
.course-detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px;
}

.detail-layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 24px;
  align-items: start;
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.course-header-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.header-top {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.course-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}

.header-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.header-price {
  font-size: 28px;
  font-weight: 700;
  color: #f56c6c;
}

.info-card {
  border-radius: 8px;
}

.card-header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.description-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
}

.coach-info-section,
.venue-info-section {
  padding: 4px 0;
}

.coach-brief,
.venue-brief {
  display: flex;
  align-items: center;
  gap: 16px;
}

.coach-brief-text,
.venue-brief-text {
  flex: 1;
}

.coach-brief-text h4,
.venue-brief-text h4 {
  margin: 0 0 4px 0;
  font-size: 15px;
  color: #303133;
}

.coach-brief-text p,
.venue-brief-text p {
  margin: 0;
  font-size: 13px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}

.coach-experience {
  font-size: 12px;
  color: #c0c4cc !important;
}

/* 侧边栏 */
.sidebar {
  position: sticky;
  top: 80px;
}

.action-card {
  border-radius: 8px;
}

.action-price {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.price-label {
  font-size: 14px;
  color: #909399;
}

.price-value {
  font-size: 28px;
  font-weight: 700;
  color: #f56c6c;
}

.enrollment-section {
  padding: 4px 0;
}

.enroll-stats {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

.enroll-percent {
  font-weight: 600;
  color: #409eff;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-subtitle { font-size: 14px; font-weight: 600; color: #303133; margin: 0 0 12px 0; }
.date-select-section { margin-bottom: 4px; }
.date-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.date-chip {
  padding: 10px 14px; border: 2px solid #e8e8e8; border-radius: 8px; cursor: pointer;
  text-align: center; transition: all 0.2s; min-width: 100px;
}
.date-chip:hover { border-color: #409eff; background: #ecf5ff; }
.date-chip.active { border-color: #409eff; background: #ecf5ff; box-shadow: 0 0 0 2px rgba(64,158,255,0.2); }
.chip-date { font-size: 13px; font-weight: 700; color: #303133; }
.chip-time { font-size: 12px; color: #909399; margin-top: 2px; }
.chip-spots { font-size: 11px; color: #c0c4cc; margin-top: 2px; }

.action-btn-primary {
  width: 100%;
}

.action-btn-row {
  display: flex;
  justify-content: center;
  gap: 12px;
}

@media (max-width: 900px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
  .sidebar {
    position: static;
  }
  .course-title {
    font-size: 20px;
  }
}
</style>
