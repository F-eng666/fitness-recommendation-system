<template>
  <div class="venue-detail-page">
    <!-- 加载状态 -->
    <template v-if="loading">
      <div class="loading-wrapper">
        <div class="header-skeleton">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="rect" style="width: 100%; height: 300px; border-radius: 8px" />
            </template>
          </el-skeleton>
        </div>
        <div class="content-skeleton">
          <el-skeleton animated :rows="6" />
        </div>
      </div>
    </template>

    <!-- 错误状态 -->
    <template v-else-if="error">
      <el-result icon="error" title="加载失败" sub-title="无法获取场馆信息，请稍后重试">
        <template #extra>
          <el-button type="primary" @click="fetchVenueDetail">重新加载</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </template>
      </el-result>
    </template>

    <!-- 正常内容 -->
    <template v-else-if="venue">
      <!-- 封面头部 -->
      <div class="venue-header">
        <div class="header-cover" :style="{ background: getHeaderGradient() }">
          <div class="header-overlay">
            <div class="header-content">
              <div class="header-left">
                <h1 class="venue-title">{{ venue.name }}</h1>
                <div class="header-meta">
                  <div class="meta-row">
                    <el-rate
                      :model-value="venue.rating || 0"
                      disabled
                      show-score
                      text-color="#fff"
                    />
                    <span class="rating-text">{{ venue.rating || 0 }} 分</span>
                  </div>
                  <div class="meta-row">
                    <el-icon><Location /></el-icon>
                    <span>{{ venue.address || '暂无地址' }}</span>
                  </div>
                  <div class="meta-row" v-if="venue.phone">
                    <el-icon><Phone /></el-icon>
                    <span>{{ venue.phone }}</span>
                  </div>
                  <div class="meta-row" v-if="venue.businessHours">
                    <el-icon><Clock /></el-icon>
                    <span>{{ venue.businessHours }}</span>
                  </div>
                </div>
              </div>
              <div class="header-right">
                <el-button
                  type="warning"
                  :icon="isFavorited ? StarFilled : Star"
                  circle
                  size="large"
                  :loading="favLoading"
                  @click="toggleFavorite"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 标签页内容 -->
      <div class="venue-tabs-wrapper">
        <el-tabs v-model="activeTab" type="border-card">
          <!-- 场馆介绍 -->
          <el-tab-pane label="场馆介绍" name="intro">
            <div class="tab-content">
              <div class="intro-section">
                <h3>场馆简介</h3>
                <p class="description-text">{{ venue.description || '暂无简介' }}</p>
              </div>
              <div class="intro-section" v-if="parsedFacilities.length">
                <h3>设施标签</h3>
                <div class="facilities-list">
                  <el-tag
                    v-for="f in parsedFacilities"
                    :key="f"
                    size="large"
                    effect="plain"
                    class="facility-tag"
                  >
                    {{ f }}
                  </el-tag>
                </div>
              </div>
              <div class="intro-section">
                <h3>场馆地址</h3>
                <p class="map-address">{{ venue.address }}</p>
              </div>
            </div>
          </el-tab-pane>

          <!-- 课程列表 -->
          <el-tab-pane label="课程列表" name="courses">
            <div class="tab-content">
              <div v-if="courseLoading" class="skeleton-grid">
                <div v-for="n in 3" :key="n" class="small-skeleton">
                  <el-skeleton animated />
                </div>
              </div>
              <el-empty v-else-if="courseList.length === 0" description="暂无课程" />
              <div v-else class="course-grid">
                <el-card
                  v-for="course in courseList"
                  :key="course.id"
                  class="course-small-card"
                  shadow="hover"
                  @click="$router.push(`/fitness/course/${course.id}`)"
                >
                  <div class="course-card-top" :style="{ background: getTypeColor(course.type) }" />
                  <div class="course-card-body">
                    <h4 class="course-name">{{ course.name }}</h4>
                    <div class="course-tags">
                      <el-tag size="small" type="info">{{ typeMap[course.type] || course.type }}</el-tag>
                      <el-tag size="small" :type="getDifficultyType(course.difficulty)">
                        {{ diffMap[course.difficulty] || course.difficulty }}
                      </el-tag>
                    </div>
                    <div class="course-info-row">
                      <el-icon><Clock /></el-icon>
                      <span>
                        <template v-if="course.recurrence === 'WEEKLY'">
                          每周{{ weekDayLabel(course.startTime) }} {{ timeOnly(course.startTime) }}~{{ timeOnly(course.endTime) }}
                        </template>
                        <template v-else>
                          {{ fixTime(course.startTime) }} ~ {{ fixTime(course.endTime) }}
                        </template>
                      </span>
                    </div>
                    <div class="course-price">¥{{ course.price || 0 }}</div>
                    <el-button
                      v-if="isLoggedIn"
                      type="primary"
                      size="small"
                      class="book-btn"
                      @click.stop="handleCourseBook(course)"
                    >
                      预约
                    </el-button>
                  </div>
                </el-card>
              </div>
            </div>
          </el-tab-pane>

          <!-- 教练团队 -->
          <el-tab-pane label="教练团队" name="coaches">
            <div class="tab-content">
              <div v-if="coachLoading" class="skeleton-grid">
                <div v-for="n in 4" :key="n" class="small-skeleton">
                  <el-skeleton animated />
                </div>
              </div>
              <el-empty v-else-if="coachList.length === 0" description="暂无教练" />
              <div v-else class="coach-row">
                <el-card
                  v-for="coach in coachList"
                  :key="coach.id"
                  class="coach-card"
                  shadow="hover"
                  @click="$router.push(`/fitness/coach/${coach.id}`)"
                >
                  <el-avatar :size="80" :src="coach.avatar">
                    {{ coach.nickname || coach.name || '教' }}
                  </el-avatar>
                  <h4>{{ coach.nickname || coach.name }}</h4>
                  <div class="coach-tags">
                    <el-tag
                      v-for="tag in (coach.specialtyList || coach.specialties || [])"
                      :key="tag"
                      size="small"
                      type="info"
                    >
                      {{ tag }}
                    </el-tag>
                  </div>
                  <div class="coach-rating">
                    <el-rate :model-value="coach.rating || 0" disabled size="small" />
                  </div>
                </el-card>
              </div>
            </div>
          </el-tab-pane>

          <!-- 用户评价 -->
          <el-tab-pane label="用户评价" name="reviews">
            <div class="tab-content">
              <div class="review-header-actions" v-if="isLoggedIn">
                <el-button type="primary" @click="showReviewDialog = true">
                  写评价
                </el-button>
              </div>
              <div v-if="reviewLoading" class="review-loading">
                <el-skeleton animated :rows="3" v-for="n in 3" :key="n" style="margin-bottom:16px" />
              </div>
              <el-empty v-else-if="reviewList.length === 0" description="暂无评价" />
              <div v-else class="review-list">
                <div v-for="review in reviewList" :key="review.id" class="review-item">
                  <div class="review-header">
                    <el-avatar :size="40" :src="review.userAvatar">
                      {{ review.userName?.charAt(0) || '用' }}
                    </el-avatar>
                    <div class="review-user-info">
                      <span class="review-username">{{ review.userName || '匿名用户' }}</span>
                      <el-rate :model-value="review.rating || 0" disabled size="small" />
                    </div>
                    <span class="review-time">{{ review.createTime || review.createdAt }}</span>
                  </div>
                  <p class="review-content">{{ review.content }}</p>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </template>
  </div>

  <!-- 写评价对话框 -->
  <el-dialog v-model="showReviewDialog" title="写评价" width="480px" destroy-on-close>
    <el-form :model="reviewForm" label-width="80px">
      <el-form-item label="评分" required>
        <el-rate v-model="reviewForm.rating" />
      </el-form-item>
      <el-form-item label="评价内容" required>
        <el-input
          v-model="reviewForm.content"
          type="textarea"
          :rows="4"
          placeholder="分享你的体验..."
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showReviewDialog = false">取消</el-button>
      <el-button type="primary" @click="submitReview" :loading="submittingReview">提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Location, Phone, Clock, Star, StarFilled } from '@element-plus/icons-vue'
import { getVenueDetail } from '@/api/venue'
import { getCourseList } from '@/api/course'
import { getCoachList } from '@/api/coach'
import { getReviewList, submitReview as apiSubmitReview, addFavorite, removeFavorite } from '@/api/user'

const route = useRoute()
const router = useRouter()

const venueId = computed(() => route.params.id)

const typeMap = { YOGA: '瑜伽', STRENGTH: '力量训练', CARDIO: '有氧运动', DANCE: '舞蹈', SWIMMING: '游泳', BASKETBALL: '篮球', BOXING: '搏击', PILATES: '普拉提', SPINNING: '动感单车', OTHER: '其他' }
const diffMap = { BEGINNER: '初级', INTERMEDIATE: '中级', ADVANCED: '高级' }

// 状态
const loading = ref(false)
const error = ref(false)
const venue = ref(null)
const activeTab = ref('intro')

// 收藏
const isFavorited = ref(false)
const favLoading = ref(false)

// 课程
const courseList = ref([])
const courseLoading = ref(false)

// 教练
const coachList = ref([])
const coachLoading = ref(false)

// 评价
const reviewList = ref([])
const reviewLoading = ref(false)
const showReviewDialog = ref(false)
const submittingReview = ref(false)
const reviewForm = ref({ rating: 0, content: '' })

const isLoggedIn = computed(() => {
  try {
    return !!localStorage.getItem('fitness_token')
  } catch { return false }
})

const parsedFacilities = computed(() => {
  const val = venue.value?.facilities
  if (!val) return []
  if (Array.isArray(val)) return val
  try { const parsed = JSON.parse(val); if (Array.isArray(parsed)) return parsed } catch {}
  return val.split(/[,，、]/).map(s => s.trim()).filter(Boolean)
})

function fixTime(str) {
  if (!str) return '-'
  return str.replace('T', ' ').substring(0, 16)
}

function timeOnly(str) {
  if (!str) return ''
  return str.replace('T', ' ').substring(11, 16)
}

function weekDayLabel(str) {
  if (!str) return ''
  const days = ['日', '一', '二', '三', '四', '五', '六']
  return days[new Date(str.replace('T', ' ')).getDay()]
}

function getHeaderGradient() {
  const gradients = [
    'linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%)',
    'linear-gradient(135deg, #141e30 0%, #243b55 100%)'
  ]
  const index = typeof venueId.value === 'string' ? venueId.value.charCodeAt(0) : (venueId.value || 0)
  return gradients[index % gradients.length]
}

function getTypeColor(type) {
  const map = { '瑜伽': '#e8f5e9', '力量训练': '#fce4ec', '有氧': '#e3f2fd', '有氧运动': '#e3f2fd', '舞蹈': '#fff3e0', '游泳': '#e0f7fa', '篮球': '#f3e5f5', '搏击': '#efebe9', '普拉提': '#f1f8e9', '动感单车': '#e8eaf6', '其他': '#f5f5f5', YOGA: '#e8f5e9', STRENGTH: '#fce4ec', CARDIO: '#e3f2fd', DANCE: '#fff3e0', SWIMMING: '#e0f7fa', BASKETBALL: '#f3e5f5', BOXING: '#efebe9', PILATES: '#f1f8e9', SPINNING: '#e8eaf6', OTHER: '#f5f5f5' }
  return map[type] || '#f0f0f0'
}

function getDifficultyType(d) {
  const map = { '初级': 'success', '中级': 'warning', '高级': 'danger', BEGINNER: 'success', INTERMEDIATE: 'warning', ADVANCED: 'danger' }
  return map[d] || 'info'
}

async function fetchVenueDetail() {
  loading.value = true
  error.value = false
  try {
    const res = await getVenueDetail(venueId.value)
    const data = res?.data || res
    // 后端返回 { venue: {...}, avgRating: 4.5 } 结构，需要展开
    if (data && data.venue) {
      venue.value = { ...data.venue, rating: data.avgRating ?? data.venue.rating }
    } else {
      venue.value = data
    }
  } catch (err) {
    console.error('获取场馆详情失败:', err)
    error.value = true
  } finally {
    loading.value = false
  }
}

async function fetchCourses() {
  if (!venueId.value) return
  courseLoading.value = true
  try {
    const res = await getCourseList({ venueId: venueId.value, page: 1, pageSize: 100 })
    courseList.value = res?.data?.records || res?.data?.list || res?.data || []
  } catch (err) {
    console.error('获取课程列表失败:', err)
  } finally {
    courseLoading.value = false
  }
}

async function fetchCoaches() {
  if (!venueId.value) return
  coachLoading.value = true
  try {
    const res = await getCoachList({ venueId: venueId.value, page: 1, pageSize: 100 })
    coachList.value = res?.data?.records || res?.data?.list || res?.data || []
  } catch (err) {
    console.error('获取教练列表失败:', err)
  } finally {
    coachLoading.value = false
  }
}

async function fetchReviews() {
  if (!venueId.value) return
  reviewLoading.value = true
  try {
    const res = await getReviewList({ targetType: 'VENUE', targetId: venueId.value, page: 1, pageSize: 50 })
    reviewList.value = res?.data?.records || res?.data?.list || res?.data || []
  } catch (err) {
    console.error('获取评价失败:', err)
  } finally {
    reviewLoading.value = false
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
      await removeFavorite('VENUE', venueId.value)
      isFavorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite('VENUE', venueId.value)
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

function handleCourseBook(course) {
  router.push(`/fitness/course/${course.id}`)
}

async function submitReview() {
  if (!reviewForm.value.rating || !reviewForm.value.content.trim()) {
    ElMessage.warning('请填写评分和评价内容')
    return
  }
  submittingReview.value = true
  try {
    await apiSubmitReview({ venueId: venueId.value, rating: reviewForm.value.rating, content: reviewForm.value.content })
    ElMessage.success('评价提交成功')
    showReviewDialog.value = false
    reviewForm.value = { rating: 0, content: '' }
    fetchReviews()
  } catch (err) {
    console.error('提交评价失败:', err)
    ElMessage.error('提交失败')
  } finally {
    submittingReview.value = false
  }
}

onMounted(() => {
  fetchVenueDetail()
  fetchCourses()
  fetchCoaches()
  fetchReviews()
})
</script>

<style scoped>
.venue-detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 0 40px 0;
}

.venue-header {
  margin-bottom: 24px;
}

.header-cover {
  min-height: 300px;
  border-radius: 8px;
  overflow: hidden;
}

.header-overlay {
  min-height: 300px;
  background: linear-gradient(180deg, rgba(0,0,0,0.1) 0%, rgba(0,0,0,0.6) 100%);
  display: flex;
  align-items: flex-end;
  padding: 40px;
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  color: #fff;
}

.venue-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 16px 0;
  color: #fff;
}

.header-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  opacity: 0.9;
}

.venue-tabs-wrapper {
  padding: 0 4px;
}

.tab-content {
  padding: 8px 0;
  min-height: 300px;
}

.intro-section {
  margin-bottom: 28px;
}

.intro-section h3 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-left: 10px;
  border-left: 3px solid #409eff;
}

.description-text {
  color: #606266;
  line-height: 1.8;
  font-size: 14px;
}

.facilities-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.facility-tag {
  font-size: 13px;
}

.map-address {
  font-size: 13px;
  color: #c0c4cc;
  margin-top: 8px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.course-small-card {
  cursor: pointer;
}

.course-card-top {
  height: 6px;
  border-radius: 4px 4px 0 0;
  margin: -1px -1px 0 -1px;
}

.course-card-body {
  padding: 12px 0 0 0;
}

.course-name {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 8px 0;
}

.course-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}

.course-info-row {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.course-price {
  font-size: 16px;
  font-weight: 700;
  color: #f56c6c;
  margin-bottom: 8px;
}

.book-btn {
  width: 100%;
}

.coach-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.coach-card {
  text-align: center;
  cursor: pointer;
  transition: transform 0.2s;
}

.coach-card:hover {
  transform: translateY(-3px);
}

.coach-card h4 {
  margin: 10px 0 8px 0;
}

.coach-tags {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 8px;
}

.review-header-actions {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}

.review-item {
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}

.review-item:last-child {
  border-bottom: none;
}

.review-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.review-user-info {
  flex: 1;
}

.review-username {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  display: block;
  margin-bottom: 2px;
}

.review-time {
  font-size: 12px;
  color: #c0c4cc;
}

.review-content {
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}

.loading-wrapper {
  padding: 0;
}

.header-skeleton {
  margin-bottom: 24px;
}

.content-skeleton {
  padding: 0 4px;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

@media (max-width: 768px) {
  .header-cover { min-height: 220px; }
  .header-overlay { padding: 24px; min-height: 220px; }
  .header-content { flex-direction: column; gap: 16px; align-items: flex-start; }
  .venue-title { font-size: 24px; }
  .course-grid { grid-template-columns: repeat(2, 1fr); }
  .coach-row { grid-template-columns: repeat(2, 1fr); }
}
</style>
