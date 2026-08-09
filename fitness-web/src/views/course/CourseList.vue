<template>
  <div class="course-list-page">
    <!-- 搜索筛选栏 -->
    <div class="search-filter-bar">
      <el-form :inline="true" class="filter-form">
        <el-form-item>
          <el-input
            v-model="keyword"
            placeholder="搜索课程名称"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-select v-model="typeFilter" placeholder="课程类型" clearable @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option label="瑜伽" value="YOGA" />
            <el-option label="力量训练" value="STRENGTH" />
            <el-option label="有氧运动" value="CARDIO" />
            <el-option label="舞蹈" value="DANCE" />
            <el-option label="游泳" value="SWIMMING" />
            <el-option label="篮球" value="BASKETBALL" />
            <el-option label="搏击" value="BOXING" />
            <el-option label="普拉提" value="PILATES" />
            <el-option label="动感单车" value="SPINNING" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="difficultyFilter" placeholder="难度" clearable @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option label="初级" value="BEGINNER" />
            <el-option label="中级" value="INTERMEDIATE" />
            <el-option label="高级" value="ADVANCED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select
            v-model="venueFilter"
            placeholder="选择场馆"
            clearable
            filterable
            @change="handleSearch"
          >
            <el-option
              v-for="v in venueOptions"
              :key="v.id"
              :label="v.name"
              :value="v.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="loading">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 课程列表 -->
    <div v-loading="loading" class="course-grid-wrapper">
      <template v-if="loading">
        <div class="course-grid">
          <div v-for="n in 6" :key="n" class="card-skeleton">
            <el-skeleton animated>
              <template #template>
                <el-skeleton-item variant="image" style="width: 100%; height: 8px; border-radius: 4px 4px 0 0" />
                <div style="padding: 20px 16px 0">
                  <el-skeleton-item variant="text" style="width: 60%" />
                  <el-skeleton-item variant="text" style="width: 40%" />
                  <el-skeleton-item variant="text" style="width: 80%" />
                  <el-skeleton-item variant="text" style="width: 30%" />
                </div>
              </template>
            </el-skeleton>
          </div>
        </div>
      </template>

      <template v-else-if="error">
        <el-empty description="加载失败，请重试">
          <el-button type="primary" @click="fetchCourseList">重新加载</el-button>
        </el-empty>
      </template>

      <template v-else-if="courseList.length === 0">
        <el-empty description="暂无课程数据">
          <template #image>
            <el-icon :size="80" color="#c0c4cc"><Search /></el-icon>
          </template>
        </el-empty>
      </template>

      <template v-else>
        <div class="course-grid">
          <el-card
            v-for="course in courseList"
            :key="course.id"
            class="course-card"
            shadow="hover"
            @click="goToDetail(course.id)"
          >
            <div
              class="card-color-bar"
              :style="{ background: getTypeBarColor(course.type) }"
            />
            <div class="card-content">
              <h3 class="course-name">{{ course.name }}</h3>
              <p class="course-meta">
                <span class="venue-name-el">{{ course.venueName || '-' }}</span>
              </p>
              <p class="course-coach" v-if="course.coachName">
                教练：{{ course.coachName }}
              </p>
              <div class="course-time">
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
              <div class="card-footer-row">
                <span class="course-price">
                  {{ course.price === 0 ? '免费' : `¥${course.price}` }}
                </span>
                <div class="card-tags">
                  <el-tag :type="getTypeTagType(course.type)" size="small">
                    {{ typeMap[course.type] || course.type || '综合训练' }}
                  </el-tag>
                  <el-tag :type="getDifficultyType(course.difficulty)" size="small">
                    {{ diffMap[course.difficulty] || course.difficulty || '未知' }}
                  </el-tag>
                </div>
                <span class="course-spots">
                  剩余 {{ (course.maxStudents || 0) - (course.enrolledCount || 0) }}
                  / {{ course.maxStudents || 0 }}
                </span>
              </div>
              <el-button type="primary" size="small" class="book-btn" @click.stop="handleBook(course)">
                预约
              </el-button>
            </div>
          </el-card>
        </div>
      </template>
    </div>

    <!-- 分页 -->
    <div v-if="total > 0 && !error" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[9, 18, 30]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Clock } from '@element-plus/icons-vue'
import { getCourseList } from '@/api/course'
import { getVenueList } from '@/api/venue'

const router = useRouter()

// 筛选
const keyword = ref('')
const typeFilter = ref('')
const difficultyFilter = ref('')
const venueFilter = ref('')
const venueOptions = ref([])

// 状态
const loading = ref(false)
const error = ref(false)

// 分页
const currentPage = ref(1)
const pageSize = ref(9)
const total = ref(0)

const courseList = ref([])

const typeMap = {
  YOGA: '瑜伽', STRENGTH: '力量训练', CARDIO: '有氧运动', DANCE: '舞蹈',
  SWIMMING: '游泳', BASKETBALL: '篮球', BOXING: '搏击', PILATES: '普拉提',
  SPINNING: '动感单车', OTHER: '其他'
}

const diffMap = {
  BEGINNER: '初级', INTERMEDIATE: '中级', ADVANCED: '高级'
}

const typeTagTypeMap = {
  YOGA: 'success', STRENGTH: 'danger', CARDIO: 'primary', DANCE: 'warning',
  SWIMMING: 'primary', BASKETBALL: 'danger', BOXING: 'warning', PILATES: 'primary',
  SPINNING: 'success', OTHER: 'info'
}

const typeColorMap = {
  '瑜伽': '#67c23a', '力量训练': '#f56c6c', '有氧': '#409eff', '有氧运动': '#409eff', '舞蹈': '#e6a23c',
  '游泳': '#36cfc9', '篮球': '#f759ab', '搏击': '#9254de', '普拉提': '#597ef7',
  '动感单车': '#ff9c6e', '其他': '#8c8c8c',
  YOGA: '#67c23a', STRENGTH: '#f56c6c', CARDIO: '#409eff', DANCE: '#e6a23c',
  SWIMMING: '#36cfc9', BASKETBALL: '#f759ab', BOXING: '#9254de', PILATES: '#597ef7',
  SPINNING: '#ff9c6e', OTHER: '#8c8c8c'
}

function getTypeTagType(type) {
  return typeTagTypeMap[type] || typeTagTypeMap[typeMap[type]] || 'info'
}

function getTypeBarColor(type) {
  return typeColorMap[type] || '#8c8c8c'
}

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

function getDifficultyType(d) {
  const map = { '初级': 'success', '中级': 'warning', '高级': 'danger', BEGINNER: 'success', INTERMEDIATE: 'warning', ADVANCED: 'danger' }
  return map[d] || 'info'
}

async function fetchVenueOptions() {
  try {
    const res = await getVenueList({ page: 1, pageSize: 1000 })
    venueOptions.value = res?.data?.records || res?.data?.list || res?.data || []
  } catch { /* ignore */ }
}

async function fetchCourseList() {
  loading.value = true
  error.value = false
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      type: typeFilter.value || undefined,
      difficulty: difficultyFilter.value || undefined,
      venueId: venueFilter.value || undefined
    }
    const res = await getCourseList(params)
    if (res && res.data) {
      courseList.value = res.data.records || res.data.list || res.data || []
      total.value = res.data.total || courseList.value.length
    } else {
      courseList.value = []
      total.value = 0
    }
  } catch (err) {
    console.error('获取课程列表失败:', err)
    error.value = true
    courseList.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchCourseList()
}

function handlePageChange() {
  fetchCourseList()
}

function handleSizeChange() {
  currentPage.value = 1
  fetchCourseList()
}

function goToDetail(id) {
  router.push(`/course/${id}`)
}

function handleBook(course) {
  const token = localStorage.getItem('fitness_token')
  if (!token) {
    ElMessage.warning('请先登录再预约课程')
    return
  }
  const enrolled = course.enrolledCount || 0
  const maxStudents = course.maxStudents || 1
  if (enrolled >= maxStudents) {
    ElMessage.warning('该课程已满员')
    return
  }
  router.push(`/fitness/course/${course.id}`)
}

onMounted(() => {
  fetchVenueOptions()
  fetchCourseList()
})
</script>

<style scoped>
.course-list-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px;
}

.search-filter-bar {
  margin-bottom: 24px;
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.filter-form {
  margin-bottom: 0;
}

.course-grid-wrapper {
  min-height: 300px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.card-skeleton {
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  background: #fff;
}

.course-card {
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  overflow: hidden;
  padding: 0;
}

.course-card:hover {
  transform: translateY(-4px);
}

.card-color-bar {
  height: 6px;
  margin: 0;
}

.card-content {
  padding: 18px 16px 16px;
}

.course-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.course-meta {
  margin: 0 0 4px 0;
  font-size: 13px;
}

.venue-name-el {
  color: #606266;
}

.course-coach {
  font-size: 12px;
  color: #909399;
  margin: 0 0 8px 0;
}

.course-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}

.card-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.card-footer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.course-price {
  font-size: 16px;
  font-weight: 700;
  color: #f56c6c;
}

.course-spots {
  font-size: 12px;
  color: #909399;
}

.book-btn {
  width: 100%;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

@media (max-width: 900px) {
  .course-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .course-grid {
    grid-template-columns: 1fr;
  }
}
</style>
