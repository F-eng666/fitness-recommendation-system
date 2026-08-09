<template>
  <div class="my-checkins-page">
    <div class="page-header">
      <h2 class="page-title">打卡记录</h2>
      <el-button type="primary" size="large" @click="openCheckinDialog">
        <el-icon><CircleCheck /></el-icon> 立即打卡
      </el-button>
    </div>

    <!-- 课程提醒 -->
    <div class="upcoming-section" v-if="upcomingCourses.length > 0">
      <div class="upcoming-header">
        <h3><el-icon><AlarmClock /></el-icon> 课程提醒</h3>
        <el-tag type="warning" size="small">{{ upcomingCourses.length }} 节</el-tag>
      </div>
      <div class="upcoming-list">
        <div v-for="c in upcomingCourses" :key="c.courseId || c.id" class="upcoming-item" :class="{ 'is-now': isCourseNow(c), 'is-ended': isCourseEnded(c) }">
          <div class="upcoming-time">
            <span class="time-text" :class="{ 'time-passed': isCourseEnded(c) }">
              {{ isCourseEnded(c) ? '已结束' : formatCourseTime(c) }}
            </span>
            <el-tag v-if="isCourseNow(c)" type="danger" size="small" effect="dark">进行中</el-tag>
            <el-tag v-else-if="isCourseSoon(c)" type="warning" size="small">即将开始</el-tag>
            <el-tag v-else-if="isCourseEnded(c)" type="info" size="small">时间已过</el-tag>
          </div>
          <div class="upcoming-info">
            <span class="upcoming-name">{{ c.courseName || c.name }}</span>
            <span class="upcoming-venue">{{ c.venueName }}</span>
          </div>
          <el-button type="primary" size="small" @click="checkInForCourse(c)">打卡</el-button>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="checkin-stats">
      <div class="stat-card today">
        <div class="stat-num">{{ stats.todayCount }}</div>
        <div class="stat-text">今日打卡</div>
      </div>
      <div class="stat-card streak">
        <div class="stat-num">{{ stats.streakDays }}</div>
        <div class="stat-text">连续天数</div>
      </div>
      <div class="stat-card total">
        <div class="stat-num">{{ total }}</div>
        <div class="stat-text">累计打卡</div>
      </div>
    </div>

    <!-- 日历 + 排名 -->
    <div class="calendar-rank-row">
      <div class="calendar-card">
        <div class="calendar-header">
          <el-button text @click="prevMonth"><el-icon><ArrowLeft /></el-icon></el-button>
          <span class="calendar-month">{{ currentYear }}年 {{ currentMonth }}月</span>
          <el-button text @click="nextMonth"><el-icon><ArrowRight /></el-icon></el-button>
        </div>
        <div class="calendar-grid">
          <div class="calendar-weekday" v-for="d in weekDays" :key="d">{{ d }}</div>
          <div v-for="(day, idx) in calendarDays" :key="idx"
            class="calendar-day"
            :class="{'is-today':day.isToday,'is-checkin':day.isCheckin,'is-other-month':!day.isCurrentMonth}">
            <span class="day-num">{{ day.date }}</span>
          </div>
        </div>
        <div class="calendar-legend">
          <span class="legend-item"><span class="dot checked"></span>已打卡</span>
          <span class="legend-item"><span class="dot today"></span>今天</span>
        </div>
      </div>

      <div class="rank-card">
        <div class="rank-tabs">
          <span :class="{active:rankType==='all'}" @click="switchRank('all')">总榜</span>
          <span :class="{active:rankType==='year'}" @click="switchRank('year')">年榜</span>
          <span :class="{active:rankType==='month'}" @click="switchRank('month')">月榜</span>
        </div>
        <div class="rank-list" v-if="rankList.length>0">
          <div v-for="item in rankList" :key="item.userId" class="rank-item">
            <span class="rank-num" :class="'rank-'+item.rank">{{ item.rank }}</span>
            <el-avatar :size="28" :src="item.avatar" class="rank-avatar">
              {{ (item.nickname || '?').charAt(0) }}
            </el-avatar>
            <span class="rank-name">{{ item.nickname }}</span>
            <span class="rank-count">{{ item.count }}天</span>
          </div>
        </div>
        <el-empty v-else description="暂无数据" :image-size="30" />
      </div>
    </div>

    <!-- 打卡表格 -->
    <div class="table-card" v-loading="loading">
      <el-table :data="checkins" stripe empty-text="暂无打卡记录，快去打卡吧~" style="width:100%">
        <el-table-column label="打卡日期" width="130">
          <template #default="{ row }">
            <span class="date-cell">{{ (row.checkTime || '').substring(0, 10) }}</span>
          </template>
        </el-table-column>
            <el-table-column label="内容" min-width="200">
          <template #default="{ row }">
            <span>{{ row.courseName || row.note || '-' }}</span>
            <div v-if="row.courseTime" class="course-date-sub">{{ fixDateStr(row.courseTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="场馆" width="120">
          <template #default="{ row }">
            <span v-if="row.venueName">{{ row.venueName }}</span>
            <span v-else style="color:#c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="打卡时间" width="180">
          <template #default="{ row }">{{ fixDateStr(row.checkTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ row }">
            <el-button
              v-if="isToday(row.checkTime)"
              type="primary"
              size="small"
              text
              @click="openEditDialog(row)"
            >
              编辑
            </el-button>
            <span v-else style="color:#c0c4cc;font-size:12px">--</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="fetchCheckins"
          @size-change="fetchCheckins"
        />
      </div>
    </div>

    <!-- 打卡弹窗 -->
    <el-dialog v-model="dialogVisible" title="立即打卡" width="400px">
      <el-form label-width="60px">
        <el-form-item label="备注">
          <el-input v-model="checkinNote" placeholder="记录今天的训练内容（选填）" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="checkingIn" @click="doCheckIn">确认打卡</el-button>
      </template>
    </el-dialog>

    <!-- 编辑打卡弹窗 -->
    <el-dialog v-model="editVisible" title="修改打卡记录" width="420px" destroy-on-close>
      <el-form label-width="60px">
        <el-form-item label="场馆">
          <el-select v-model="editVenueId" placeholder="选择场馆" style="width:100%">
            <el-option v-for="v in venueOptions" :key="v.id" :label="v.name" :value="v.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editNote" placeholder="训练内容（选填）" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="doEdit">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { CircleCheck, ArrowLeft, ArrowRight, AlarmClock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getMyCheckIns, getTodayCheckInCount, getCheckInStreak, getCheckInRank, checkIn, updateCheckIn } from '@/api/user'
import { getVenueList } from '@/api/venue'

const loading = ref(false)
const checkingIn = ref(false)
const checkins = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const checkinNote = ref('')
const checkinDates = ref(new Set())

// 编辑打卡
const editVisible = ref(false)
const editTarget = ref(null)
const editVenueId = ref(null)
const editNote = ref('')
const editSaving = ref(false)
const venueOptions = ref([])

function isToday(timeStr) {
  if (!timeStr) return false
  const d = new Date(timeStr).toDateString()
  return d === new Date().toDateString()
}
const rankList = ref([])
const rankType = ref('all')

const now = new Date()
const currentYear = ref(now.getFullYear())
const currentMonth = ref(now.getMonth() + 1)
const weekDays = ['日', '一', '二', '三', '四', '五', '六']

const calendarDays = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value
  const today = new Date()
  const todayStr = `${today.getFullYear()}-${String(today.getMonth()+1).padStart(2,'0')}-${String(today.getDate()).padStart(2,'0')}`

  const firstDay = new Date(year, month - 1, 1)
  const startDayOfWeek = firstDay.getDay()
  const daysInMonth = new Date(year, month, 0).getDate()
  const daysInPrevMonth = new Date(year, month - 1, 0).getDate()

  const days = []
  // 上月填充
  for (let i = startDayOfWeek - 1; i >= 0; i--) {
    const d = daysInPrevMonth - i
    const dateStr = `${year}-${String(month-1||12).padStart(2,'0')}-${String(d).padStart(2,'0')}`
    days.push({ date: d, isCurrentMonth: false, isToday: false, isCheckin: checkinDates.value.has(dateStr) })
  }
  // 当月
  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = `${year}-${String(month).padStart(2,'0')}-${String(d).padStart(2,'0')}`
    days.push({
      date: d,
      isCurrentMonth: true,
      isToday: dateStr === todayStr,
      isCheckin: checkinDates.value.has(dateStr)
    })
  }
  // 下月填充
  const remaining = 7 - (days.length % 7)
  if (remaining < 7) {
    for (let d = 1; d <= remaining; d++) {
      days.push({ date: d, isCurrentMonth: false, isToday: false, isCheckin: false })
    }
  }
  return days
})

function prevMonth() {
  if (currentMonth.value === 1) { currentYear.value--; currentMonth.value = 12 }
  else { currentMonth.value-- }
}
function nextMonth() {
  if (currentMonth.value === 12) { currentYear.value++; currentMonth.value = 1 }
  else { currentMonth.value++ }
}

const stats = reactive({
  todayCount: 0,
  streakDays: 0
})

const upcomingCourses = ref([])

function fixDateStr(str) {
  if (!str) return ''
  return str.replace('T', ' ')
}

function isCourseNow(c) {
  if (!c.startTime || !c.endTime) return false
  const now = new Date()
  return new Date(c.startTime) <= now && new Date(c.endTime) >= now
}

function isCourseSoon(c) {
  if (!c.startTime) return false
  const diff = new Date(c.startTime) - new Date()
  return diff > 0 && diff < 30 * 60 * 1000
}

function isCourseEnded(c) {
  if (!c.endTime && !c.startTime) return false
  const end = c.endTime ? new Date(c.endTime) : new Date(c.startTime)
  return end < new Date()
}

function formatCourseTime(c) {
  const raw = c.courseTime || ''
  if (raw) return fixDateStr(raw)
  const s = c.startTime ? new Date(c.startTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : ''
  const e = c.endTime ? new Date(c.endTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : ''
  return s && e ? `${s} ~ ${e}` : (s || '--')
}

function checkInForCourse(c) {
  checkinNote.value = c.courseName || c.name || ''
  dialogVisible.value = true
}

async function loadUpcomingCourses() {
  try {
    const res = await getMyOrders({ page: 1, size: 50 })
    const orders = res.data?.records || []
    const now = new Date()
    upcomingCourses.value = orders
      .filter(o => {
        if (o.status !== 'PAID' && o.status !== 'CONFIRMED') return false
        if (!o.courseTime) return false
        // 取课程结束时间，如果还没结束就显示
        const parts = o.courseTime.split(' ~ ')
        const endStr = parts[1] || parts[0]
        const endDate = new Date(fixDateStr(endStr))
        return !isNaN(endDate) && endDate > now
      })
      .sort((a, b) => new Date(fixDateStr(a.courseTime)) - new Date(fixDateStr(b.courseTime)))
  } catch { upcomingCourses.value = [] }
}

onMounted(() => { fetchCheckins(); fetchStats(); loadUpcomingCourses() })

async function fetchCheckins() {
  loading.value = true
  try {
    const res = await getMyCheckIns({ page: currentPage.value, size: pageSize.value })
    if (res.code === 200) {
      checkins.value = res.data?.records || res.data || []
      total.value = res.data?.total || 0
      // 收集所有打卡日期
      const dates = new Set()
      checkins.value.forEach(c => {
        if (c.checkTime) dates.add(c.checkTime.substring(0, 10))
      })
      checkinDates.value = dates
    }
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function fetchStats() {
  try {
    const [t, s] = await Promise.all([getTodayCheckInCount(), getCheckInStreak()])
    stats.todayCount = t.data?.count || 0
    stats.streakDays = s.data?.streak || 0
  } catch (e) { /* ignore */ }
  fetchRank()
}

async function fetchRank() {
  try {
    const res = await getCheckInRank({ type: rankType.value })
    rankList.value = res.data || []
  } catch { rankList.value = [] }
}
function switchRank(type) {
  rankType.value = type
  fetchRank()
}

function openCheckinDialog() {
  checkinNote.value = ''
  dialogVisible.value = true
}

async function doCheckIn() {
  checkingIn.value = true
  try {
    const res = await checkIn()
    if (res.code === 200) {
      ElMessage.success(res.message || '打卡成功！')
      dialogVisible.value = false
      fetchCheckins()
      fetchStats()
    } else {
      ElMessage.error(res.message || '打卡失败')
    }
  } catch (e) {
    ElMessage.error('网络错误，请重试')
  } finally { checkingIn.value = false }
}

async function openEditDialog(row) {
  editTarget.value = row
  editVenueId.value = row.venueId || null
  editNote.value = row.note || ''
  editVisible.value = true
  // Lazy load venues
  if (venueOptions.value.length === 0) {
    try {
      const res = await getVenueList({ page: 1, size: 100, status: 'APPROVED' })
      venueOptions.value = res.data?.records || res.data || []
    } catch { /* ignore */ }
  }
}

async function doEdit() {
  if (!editTarget.value) return
  editSaving.value = true
  try {
    const params = {}
    if (editVenueId.value) params.venueId = editVenueId.value
    params.note = editNote.value || ''
    const res = await updateCheckIn(editTarget.value.id, params)
    if (res.code === 200) {
      ElMessage.success('打卡记录已更新')
      editVisible.value = false
      fetchCheckins()
    } else {
      ElMessage.error(res.message || '修改失败')
    }
  } catch (e) {
    ElMessage.error('网络错误，请重试')
  } finally {
    editSaving.value = false
  }
}
</script>

<style scoped>
.my-checkins-page { max-width: 1100px; margin: 0 auto; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.page-title { font-size: 24px; font-weight: 700; color: #1a1a2e; margin: 0; }

.calendar-rank-row { display: flex; gap: 16px; margin-bottom: 20px; align-items: flex-start; }
.calendar-rank-row .calendar-card { flex: 1; min-width: 0; margin-bottom: 0; }

.rank-card { background: #fff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); padding: 14px; width: 180px; flex-shrink: 0; }
.rank-tabs { display: flex; gap: 4px; margin-bottom: 10px; justify-content: center; }
.rank-tabs span { font-size: 12px; padding: 2px 8px; border-radius: 10px; cursor: pointer; color: #909399; transition: all 0.2s; }
.rank-tabs span.active { background: #409eff; color: #fff; font-weight: 600; }
.rank-item { display: flex; align-items: center; gap: 6px; padding: 4px 0; border-bottom: 1px solid #f5f5f5; font-size: 12px; }
.rank-item:last-child { border-bottom: none; }
.rank-num { width: 18px; height: 18px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 10px; font-weight: 700; color: #fff; background: #909399; flex-shrink: 0; }
.rank-num.rank-1 { background: #f56c6c; }
.rank-num.rank-2 { background: #e6a23c; }
.rank-num.rank-3 { background: #409eff; }
.rank-avatar { flex-shrink: 0; }
.rank-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #303133; }
.rank-count { color: #67c23a; font-weight: 600; font-size: 11px; flex-shrink: 0; }

.checkin-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 24px; }
.upcoming-section { background: #fff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); padding: 16px 20px; margin-bottom: 16px; }
.upcoming-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.upcoming-header h3 { font-size: 15px; font-weight: 600; color: #303133; margin: 0; display: flex; align-items: center; gap: 6px; }
.upcoming-list { display: flex; flex-direction: column; gap: 8px; }
.upcoming-item { display: flex; align-items: center; gap: 12px; padding: 10px 12px; border-radius: 8px; background: #fafafa; border: 1px solid #f0f0f0; }
.upcoming-item.is-now { background: #fef0f0; border-color: #fde2e2; }
.upcoming-item.is-ended { opacity: 0.55; }
.upcoming-time { display: flex; align-items: center; gap: 6px; min-width: 170px; }
.time-text { font-weight: 600; font-size: 13px; color: #303133; }
.time-text.time-passed { color: #c0c4cc; text-decoration: line-through; }
.upcoming-info { flex: 1; min-width: 0; }
.upcoming-name { font-size: 14px; font-weight: 600; color: #303133; display: block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.upcoming-venue { font-size: 12px; color: #909399; }
.course-date-sub { font-size: 12px; color: #909399; margin-top: 2px; }

.stat-card { background: #fff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); padding: 20px; text-align: center; }
.stat-card.today { border-top: 3px solid #409eff; }
.stat-card.streak { border-top: 3px solid #e6a23c; }
.stat-card.total { border-top: 3px solid #67c23a; }
.stat-num { font-size: 32px; font-weight: 800; color: #1a1a2e; }
.stat-text { font-size: 13px; color: #909399; margin-top: 4px; }

.table-card { background: #fff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); padding: 20px; }
.date-cell { font-weight: 600; color: #303133; }
.pagination-wrapper { display: flex; justify-content: center; margin-top: 20px; }

/* Calendar */
.calendar-card { background: #fff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); padding: 16px; margin-bottom: 20px; }
.calendar-header { display: flex; align-items: center; justify-content: center; gap: 8px; margin-bottom: 10px; }
.calendar-month { font-size: 13px; font-weight: 600; color: #303133; min-width: 90px; text-align: center; }
.calendar-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 2px; text-align: center; }
.calendar-weekday { font-size: 11px; font-weight: 500; color: #909399; padding: 4px 0; }
.calendar-day { aspect-ratio: 1; display: flex; align-items: center; justify-content: center; border-radius: 4px; font-size: 12px; }
.calendar-day.is-other-month .day-num { color: #e0e0e0; }
.calendar-day.is-today { border: 2px solid #409eff; border-radius: 6px; }
.calendar-day.is-today .day-num { color: #409eff; font-weight: 700; }
.calendar-day.is-checkin { background: #67c23a; border-radius: 6px; }
.calendar-day.is-checkin .day-num { color: #fff; font-weight: 600; }
.calendar-day.is-checkin.is-today { background: #67c23a; border: 2px solid #409eff; }
.calendar-day.is-checkin.is-today .day-num { color: #fff; }
.calendar-legend { display: flex; gap: 14px; justify-content: center; margin-top: 8px; font-size: 11px; color: #909399; }
.legend-item { display: flex; align-items: center; gap: 4px; }
.dot { width: 10px; height: 10px; border-radius: 3px; display: inline-block; }
.dot.checked { background: #67c23a; }
.dot.today { background: #ecf5ff; border: 1px solid #409eff; }
</style>
