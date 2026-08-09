<template>
  <div class="coach-detail-page" v-loading="loading">
    <!-- 错误状态 -->
    <template v-if="error">
      <el-result icon="error" title="加载失败" sub-title="无法获取教练信息">
        <template #extra>
          <el-button type="primary" @click="fetchCoachDetail">重新加载</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </template>
      </el-result>
    </template>

    <template v-else-if="!loading && coach">
      <!-- 头部信息 -->
      <div class="coach-header">
        <div class="header-bg" :style="{ background: getHeaderGradient(coach.id) }">
          <div class="header-overlay">
            <div class="header-content">
              <el-avatar :size="100" :src="coach.avatar" class="header-avatar">
                {{ (coach.nickname || coach.name || '教').charAt(0) }}
              </el-avatar>
              <div class="header-info">
                <h1 class="coach-title">
                  {{ coach.nickname || coach.name || '未知教练' }}
                  <span v-if="coach.gender" class="gender-badge">
                    {{ coach.gender === 1 || coach.gender === 'MALE' ? '男' : '女' }}
                  </span>
                </h1>
                <div class="header-meta">
                  <span v-if="coach.age">{{ coach.age }}岁</span>
                  <span v-if="coach.experience">{{ coach.experience }}</span>
                </div>
                <div class="header-rating">
                  <el-rate :model-value="coach.rating || 0" disabled show-score text-color="#fff" />
                </div>
              </div>
              <div class="header-actions">
                <el-button
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

      <!-- 详细信息 -->
      <div class="coach-detail-content">
        <!-- 左侧主内容 -->
        <div class="detail-main">
          <el-card class="info-card" shadow="never">
            <template #header>
              <span class="card-title">基本信息</span>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="经验">
                {{ coach.experience || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="年龄">
                {{ coach.age || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="所属场馆">
                <el-link v-if="coach.venueName" type="primary" @click="$router.push(`/fitness/venue/${coach.venueId}`)">
                  {{ coach.venueName }}
                </el-link>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="场馆地址">
                {{ coach.venueAddress || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="专长领域" :span="2">
                <el-tag
                  v-for="tag in specialtyList"
                  :key="tag"
                  size="small"
                  type="info"
                  class="specialty-tag"
                >
                  {{ tag }}
                </el-tag>
                <span v-if="specialtyList.length === 0">-</span>
              </el-descriptions-item>
              <el-descriptions-item label="认证资质" :span="2">
                <div v-if="certList.length">
                  <el-tag
                    v-for="cert in certList"
                    :key="cert"
                    size="small"
                    effect="plain"
                    class="cert-tag"
                  >
                    {{ cert }}
                  </el-tag>
                </div>
                <span v-else>-</span>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 个人介绍 -->
          <el-card class="info-card" shadow="never">
            <template #header>
              <span class="card-title">个人介绍</span>
            </template>
            <div class="intro-content">
              {{ coach.introduction || coach.description || '暂无介绍' }}
            </div>
          </el-card>

          <!-- 相关课程 -->
          <el-card class="info-card" shadow="never">
            <template #header>
              <span class="card-title">相关课程 ({{ courseList.length }})</span>
            </template>
            <div v-if="courseLoading" class="section-loading">
              <el-skeleton animated :rows="2" />
            </div>
            <el-empty v-else-if="courseList.length === 0" description="暂无相关课程" />
            <div v-else class="related-course-list">
              <div
                v-for="course in courseList"
                :key="course.id"
                class="related-course-item"
                @click="$router.push(`/fitness/course/${course.id}`)"
              >
                <div class="course-item-info">
                  <h4>{{ course.name }}</h4>
                  <div class="course-item-meta">
                    <el-tag size="small">{{ typeMap[course.type] || course.type }}</el-tag>
                    <el-tag size="small" :type="getDifficultyType(course.difficulty)">
                      {{ diffMap[course.difficulty] || course.difficulty }}
                    </el-tag>
                    <span class="course-item-price">¥{{ course.price || 0 }}</span>
                  </div>
                </div>
                <el-icon><ArrowRight /></el-icon>
              </div>
            </div>
          </el-card>
        </div>

        <!-- 右侧 -->
        <div class="detail-side">
          <!-- 私教预约 -->
          <el-card class="info-card booking-card" shadow="never">
            <template #header>
              <span class="card-title">私教预约</span>
            </template>
            <div v-if="coach">
              <p class="booking-venue-info">
                {{ coach.venueName }}
                <span v-if="venueHours" class="venue-hours">营业：{{ venueHours }}</span>
              </p>
              <el-button type="warning" size="large" style="width:100%" @click="openBookingDialog">
                预约私教课
              </el-button>
            </div>
          </el-card>

          <!-- 用户评价 -->
          <el-card class="info-card review-card" shadow="never">
            <template #header>
              <span class="card-title">用户评价 ({{ reviewList.length }})</span>
            </template>
            <div v-if="reviewLoading" class="section-loading">
              <el-skeleton animated :rows="3" v-for="n in 2" :key="n" style="margin-bottom: 12px" />
            </div>
            <el-empty v-else-if="reviewList.length === 0" description="暂无评价" />
            <div v-else class="review-list-side">
              <div v-for="review in reviewList" :key="review.id" class="side-review-item">
                <div class="side-review-header">
                  <el-avatar :size="32" :src="review.userAvatar">
                    {{ (review.userName || '用').charAt(0) }}
                  </el-avatar>
                  <div class="side-review-user">
                    <span class="side-review-name">{{ review.userName || '匿名用户' }}</span>
                    <el-rate :model-value="review.rating || 0" disabled size="small" />
                  </div>
                  <span class="side-review-time">{{ review.createTime || review.createdAt }}</span>
                </div>
                <p class="side-review-content">{{ review.content }}</p>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </template>

    <!-- 私教预约弹窗 -->
    <el-dialog v-model="bookingVisible" title="私教预约" width="480px" :close-on-click-modal="false">
      <div v-if="coach">
        <p style="margin-bottom:12px;color:#606266">
          教练：<strong>{{ coach.nickname || coach.name }}</strong> |
          场馆：{{ coach.venueName }}
          <span v-if="venueHours" style="margin-left:8px;font-size:12px;color:#909399">
            营业时间：{{ venueHours }}
          </span>
        </p>
        <el-form label-width="80px">
          <el-form-item label="选择日期">
            <el-date-picker
              v-model="bookingDate"
              type="date"
              placeholder="请选择日期"
              :disabled-date="disabledDate"
              @change="fetchSlots"
              style="width:100%"
            />
          </el-form-item>
          <el-form-item label="可选时段" v-if="bookingDate">
            <div v-if="slotLoading" style="text-align:center;padding:20px">
              <el-icon class="is-loading"><Loading /></el-icon> 加载中...
            </div>
            <div v-else-if="slots.length === 0" style="color:#909399;text-align:center;padding:20px">
              该日期暂无可用时段
            </div>
            <div v-else class="slot-grid">
              <el-button
                v-for="s in slots"
                :key="s.start"
                :type="selectedSlot === s.start ? 'primary' : ''"
                size="small"
                @click="selectedSlot = s.start"
                style="margin:4px"
              >
                {{ s.label }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item label="价格" v-if="selectedSlot">
            <span style="font-size:20px;font-weight:700;color:#f56c6c">¥200.00</span>
            <span style="font-size:12px;color:#909399;margin-left:4px">/小时</span>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="bookingVisible = false">取消</el-button>
        <el-button type="primary" :loading="bookingLoading" :disabled="!selectedSlot" @click="submitBooking">
          确认预约
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled, ArrowRight, Loading } from '@element-plus/icons-vue'
import { getCoachDetail } from '@/api/coach'
import { getCourseList } from '@/api/course'
import { getReviewList, addFavorite, removeFavorite } from '@/api/user'
import { getAvailableSlots, bookTraining } from '@/api/pt'
import { getVenueDetail } from '@/api/venue'

const route = useRoute()
const router = useRouter()

const coachId = computed(() => route.params.id)

const typeMap = { YOGA: '瑜伽', STRENGTH: '力量训练', CARDIO: '有氧运动', DANCE: '舞蹈', SWIMMING: '游泳', BASKETBALL: '篮球', BOXING: '搏击', PILATES: '普拉提', SPINNING: '动感单车', OTHER: '其他' }
const diffMap = { BEGINNER: '初级', INTERMEDIATE: '中级', ADVANCED: '高级' }

const loading = ref(false)
const error = ref(false)
const coach = ref(null)

const isFavorited = ref(false)
const favLoading = ref(false)

const courseList = ref([])
const courseLoading = ref(false)

const reviewList = ref([])
const reviewLoading = ref(false)

const isLoggedIn = computed(() => {
  try {
    return !!(localStorage.getItem('fitness_token'))
  } catch { return false }
})

const specialtyList = computed(() => {
  const val = coach.value?.specialtyList || coach.value?.specialties || coach.value?.specialty
  if (!val) return []
  return Array.isArray(val) ? val : (typeof val === 'string' ? val.split(/[,，、]/) : [])
})

const certList = computed(() => {
  const val = coach.value?.certifications || coach.value?.certs || coach.value?.certification
  if (!val) return []
  return Array.isArray(val) ? val : (typeof val === 'string' ? val.split(/[,，、]/) : [])
})

function getHeaderGradient(id) {
  const gradients = [
    'linear-gradient(135deg, #0c3483 0%, #a2b6df 100%)',
    'linear-gradient(135deg, #1a2a6c 0%, #b21f1f 50%, #fdbb2d 100%)'
  ]
  const index = typeof id === 'string' ? id.charCodeAt(0) : (id || 0)
  return gradients[index % gradients.length]
}

function getDifficultyType(d) {
  const map = { '初级': 'success', '中级': 'warning', '高级': 'danger', BEGINNER: 'success', INTERMEDIATE: 'warning', ADVANCED: 'danger' }
  return map[d] || 'info'
}

async function fetchCoachDetail() {
  loading.value = true
  error.value = false
  try {
    const res = await getCoachDetail(coachId.value)
    coach.value = res?.data || res
  } catch (err) {
    console.error('获取教练详情失败:', err)
    error.value = true
  } finally {
    loading.value = false
  }
}

async function fetchCoachCourses() {
  courseLoading.value = true
  try {
    const res = await getCourseList({ venueId: null, page: 1, size: 50 })
    // 按教练ID筛选课程
    const allCourses = res?.data?.records || res?.data?.list || []
    courseList.value = allCourses.filter(c => c.coachId == coachId.value)
  } catch (err) {
    console.error('获取教练课程失败:', err)
  } finally {
    courseLoading.value = false
  }
}

async function fetchCoachReviews() {
  reviewLoading.value = true
  try {
    const res = await getReviewList({ targetType: 'COACH', targetId: coachId.value, page: 1, size: 30 })
    reviewList.value = res?.data?.records || res?.data?.list || res?.data || []
  } catch (err) {
    console.error('获取教练评价失败:', err)
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
      await removeFavorite('COACH', coachId.value)
      isFavorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite('COACH', coachId.value)
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

// 私教预约
const bookingVisible = ref(false)
const bookingDate = ref(null)
const bookingLoading = ref(false)
const slotLoading = ref(false)
const slots = ref([])
const selectedSlot = ref(null)
const venueHours = ref('')
function disabledDate(time) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const max = new Date()
  max.setDate(max.getDate() + 30)
  return time.getTime() < today.getTime() || time.getTime() > max.getTime()
}

async function openBookingDialog() {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    return
  }
  bookingVisible.value = true
  bookingDate.value = null
  slots.value = []
  selectedSlot.value = null
  if (coach.value?.venueId) {
    try {
      const res = await getVenueDetail(coach.value.venueId)
      const venue = res?.data || res
      venueHours.value = venue.businessHours || ''
    } catch { venueHours.value = '' }
  }
}

async function fetchSlots() {
  if (!bookingDate.value || !coach.value) return
  slotLoading.value = true
  selectedSlot.value = null
  try {
    const dateStr = bookingDate.value.toISOString().split('T')[0]
    const res = await getAvailableSlots({
      coachId: coach.value.id,
      venueId: coach.value.venueId,
      date: dateStr
    })
    if (res.code === 200 && res.data) {
      slots.value = res.data.slots || []
      venueHours.value = res.data.openTime + '-' + res.data.closeTime
    } else {
      slots.value = []
    }
  } catch (err) {
    ElMessage.error('获取可用时段失败')
    slots.value = []
  } finally {
    slotLoading.value = false
  }
}

async function submitBooking() {
  if (!selectedSlot.value || !coach.value) return
  bookingLoading.value = true
  try {
    const dateStr = bookingDate.value.toISOString().split('T')[0]
    const startTime = dateStr + 'T' + selectedSlot.value
    const endTime = dateStr + 'T' + slots.value.find(s => s.start === selectedSlot.value)?.end
    const res = await bookTraining({
      coachId: coach.value.id,
      venueId: coach.value.venueId,
      startTime,
      endTime
    })
    if (res.code === 200) {
      const data = res.data
      ElMessage.success(`预约成功！已支付 ¥${data.price || '200'}，余额 ¥${data.balanceAfter || '0'}`)
      bookingVisible.value = false
    } else {
      ElMessage.error(res.message || '预约失败')
    }
  } catch (err) {
    const msg = err?.response?.data?.message || err?.message || '预约失败'
    if (msg.includes('余额不足')) {
      ElMessageBox.confirm(msg, '余额不足', {
        confirmButtonText: '去充值',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        bookingVisible.value = false
        router.push('/user/wallet')
      }).catch(() => {})
    } else {
      ElMessage.error(msg)
    }
  } finally {
    bookingLoading.value = false
  }
}

onMounted(() => {
  fetchCoachDetail()
  fetchCoachCourses()
  fetchCoachReviews()
})
</script>

<style scoped>
.coach-detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 0 40px 0;
}

.coach-header {
  margin-bottom: 24px;
}

.header-bg {
  border-radius: 12px;
  overflow: hidden;
}

.header-overlay {
  background: linear-gradient(180deg, rgba(0,0,0,0.05) 0%, rgba(0,0,0,0.55) 100%);
  padding: 48px 40px;
  display: flex;
  min-height: 200px;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 24px;
  width: 100%;
  color: #fff;
}

.header-avatar {
  border: 4px solid rgba(255, 255, 255, 0.4);
  flex-shrink: 0;
}

.header-info {
  flex: 1;
}

.coach-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px 0;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 10px;
}

.gender-badge {
  font-size: 13px;
  font-weight: 400;
  background: rgba(255,255,255,0.25);
  padding: 2px 10px;
  border-radius: 12px;
}

.header-meta {
  display: flex;
  gap: 16px;
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.header-actions {
  flex-shrink: 0;
}

.coach-detail-content {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 20px;
  align-items: start;
}

.detail-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-side {
  position: sticky;
  top: 80px;
  align-self: start;
}

.info-card {
  border-radius: 8px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.specialty-tag,
.cert-tag {
  margin-right: 8px;
  margin-bottom: 4px;
}

.intro-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
  min-height: 60px;
}

.section-loading {
  padding: 12px 0;
}

.related-course-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.related-course-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  border: 1px solid #ebeef5;
}

.related-course-item:hover {
  background: #f5f7fa;
}

.course-item-info h4 {
  margin: 0 0 6px 0;
  font-size: 14px;
  color: #303133;
}

.course-item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.course-item-price {
  font-size: 14px;
  font-weight: 600;
  color: #f56c6c;
}

/* 评价侧栏 */
.side-review-item {
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.side-review-item:last-child {
  border-bottom: none;
}

.side-review-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.side-review-user {
  flex: 1;
}

.side-review-name {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  display: block;
  margin-bottom: 2px;
}

.side-review-time {
  font-size: 11px;
  color: #c0c4cc;
}

.side-review-content {
  margin: 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.review-card {
  max-height: 500px;
  overflow-y: auto;
}

.booking-card {
  background: linear-gradient(135deg, #fff7e6 0%, #fffbe6 100%);
  border: 1px solid #ffe58f;
}

.booking-venue-info {
  font-size: 13px;
  color: #606266;
  margin: 0 0 12px;
}

.venue-hours {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}

.slot-grid {
  display: flex;
  flex-wrap: wrap;
}


@media (max-width: 900px) {
  .coach-detail-content {
    grid-template-columns: 1fr;
  }
  .detail-side {
    position: static;
  }
  .header-overlay {
    padding: 32px 20px;
    min-height: 160px;
  }
  .header-content {
    flex-wrap: wrap;
  }
  .coach-title {
    font-size: 22px;
  }
}
</style>
