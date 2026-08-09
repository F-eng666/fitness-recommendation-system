<template>
  <div class="my-page">
    <h2 class="page-title">我的</h2>

    <!-- 打卡统计 -->
    <div class="stats-row">
      <div class="stat-card today" @click="openCheckinDialog">
        <div class="stat-num">{{ stats.todayCount }}</div>
        <div class="stat-label">今日打卡</div>
        <div class="stat-action">点击打卡</div>
      </div>
      <div class="stat-card streak">
        <div class="stat-num">{{ stats.streakDays }}</div>
        <div class="stat-label">连续天数</div>
      </div>
      <div class="stat-card total">
        <div class="stat-num">{{ stats.totalCount }}</div>
        <div class="stat-label">累计打卡</div>
      </div>
    </div>

    <!-- 待上课程 -->
    <div class="section" v-if="upcomingCourses.length > 0">
      <div class="section-header">
        <h3>待上课程</h3>
        <el-tag type="warning" size="small">{{ upcomingCourses.length }} 节</el-tag>
      </div>
      <div class="upcoming-list">
        <div
          v-for="c in upcomingCourses"
          :key="c.id"
          class="upcoming-item"
          @click="$router.push(`/fitness/course/${c.courseId}`)"
        >
          <div class="upcoming-left">
            <span class="upcoming-name">{{ c.courseName }}</span>
            <span class="upcoming-venue">{{ c.venueName }}</span>
          </div>
          <div class="upcoming-right">
            <span class="upcoming-time">{{ formatCourseTime(c.courseTime) }}</span>
            <el-tag size="small" type="primary" effect="plain">{{ c.status === 'PAID' ? '已支付' : '已确认' }}</el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 我的会员卡 -->
    <div class="section">
      <div class="section-header">
        <h3>我的会员卡</h3>
        <el-button text type="primary" @click="$router.push('/home')">
          购买更多 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
      </div>
      <div v-if="cardLoading" class="cards-loading">
        <el-skeleton animated :rows="2" v-for="n in 2" :key="n" style="margin-bottom:12px" />
      </div>
      <el-empty v-else-if="memberships.length === 0" description="暂无会员卡，去首页选购吧~" :image-size="80" />
      <div v-else class="cards-list">
        <div
          v-for="m in memberships"
          :key="m.id"
          class="member-card"
          :class="'card-status-' + (m.status || '').toLowerCase()"
        >
          <div class="card-left" :class="'card-left-' + (m.cardType || '').toLowerCase()">
            <span class="card-icon">{{ cardEmoji(m.cardType) }}</span>
            <span class="card-type-name">{{ typeName(m.cardType) }}</span>
          </div>
          <div class="card-center">
            <h4>{{ m.cardName }}</h4>
            <div class="card-meta">
              <!-- 次卡：剩余次数 -->
              <div v-if="m.cardType === 'SESSION'" class="remain-info">
                <span class="remain-num">{{ m.remainingSessions ?? 0 }}</span>
                <span class="remain-unit">次剩余</span>
              </div>
              <!-- 时间卡：剩余天数 -->
              <div v-else class="remain-info">
                <span class="remain-num">{{ calcRemainDays(m.expireTime) }}</span>
                <span class="remain-unit">天剩余</span>
                <span class="expire-date" v-if="m.expireTime">有效期至 {{ formatDate(m.expireTime) }}</span>
              </div>
            </div>
          </div>
          <div class="card-right">
            <el-tag
              :type="statusTagType(m.status)"
              size="small"
              effect="plain"
            >
              {{ statusLabel(m) }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 打卡弹窗 -->
    <el-dialog v-model="checkinDialogVisible" title="立即打卡" width="420px" destroy-on-close>
      <el-form label-width="60px">
        <el-form-item label="场馆" required>
          <el-select v-model="checkinVenueId" placeholder="请选择打卡场馆" style="width:100%">
            <el-option
              v-for="v in venueList"
              :key="v.id"
              :label="v.name"
              :value="v.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="checkinNote"
            placeholder="记录今天的训练内容（选填）"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkinDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="checkingIn" :disabled="!checkinVenueId" @click="doCheckin">
          确认打卡
        </el-button>
      </template>
    </el-dialog>

    <!-- 最近打卡 -->
    <div class="section">
      <div class="section-header">
        <h3>最近打卡</h3>
        <el-button text type="primary" @click="$router.push('/user/checkins')">
          查看全部 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
      </div>
      <div class="table-card" v-loading="checkinLoading">
        <el-table :data="recentCheckins" stripe empty-text="暂无打卡记录" style="width:100%">
          <el-table-column label="日期" width="120">
            <template #default="{ row }">{{ (row.checkTime || '').substring(0, 10) }}</template>
          </el-table-column>
          <el-table-column label="内容" min-width="180">
            <template #default="{ row }">
              {{ row.courseName || row.note || '-' }}
              <div v-if="row.courseTime" style="font-size:12px;color:#909399;margin-top:2px">{{ row.courseTime }}</div>
            </template>
          </el-table-column>
          <el-table-column label="场馆" width="140">
            <template #default="{ row }">{{ row.venueName || '-' }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import { getMyCards } from '@/api/membership'
import { getMyCheckIns, getMyOrders, getTodayCheckInCount, getCheckInStreak, checkIn } from '@/api/user'
import { getVenueList } from '@/api/venue'

const stats = reactive({ todayCount: 0, streakDays: 0, totalCount: 0 })
const memberships = ref([])
const cardLoading = ref(false)
const recentCheckins = ref([])
const checkinLoading = ref(false)

// Check-in dialog
const checkinDialogVisible = ref(false)
const checkinVenueId = ref(null)
const checkinNote = ref('')
const checkingIn = ref(false)
const venueList = ref([])

// 待上课程
const upcomingCourses = ref([])

function formatCourseTime(str) {
  if (!str) return ''
  return str.replace('T', ' ').substring(0, 16)
}

const typeName = (type) => {
  const map = { SESSION: '次卡', WEEKLY: '周卡', MONTHLY: '月卡', QUARTERLY: '季卡' }
  return map[type] || type
}

const cardEmoji = (type) => {
  const map = { SESSION: '🎫', WEEKLY: '📅', MONTHLY: '🥇', QUARTERLY: '🏆' }
  return map[type] || '💳'
}

const calcRemainDays = (expireTime) => {
  if (!expireTime) return 0
  const now = new Date()
  const exp = new Date(expireTime)
  const diff = Math.ceil((exp - now) / (1000 * 60 * 60 * 24))
  return Math.max(0, diff)
}

const formatDate = (str) => {
  if (!str) return ''
  return str.substring(0, 10)
}

const statusLabel = (m) => {
  if (m.status === 'EXPIRED') return '已过期'
  if (m.status === 'USED_UP') return '已用完'
  if (m.cardType === 'SESSION') {
    const remain = m.remainingSessions ?? 0
    return remain > 0 ? '使用中' : '已用完'
  }
  const days = calcRemainDays(m.expireTime)
  if (days <= 0) return '已过期'
  if (days <= 3) return '即将过期'
  return '使用中'
}

const statusTagType = (status) => {
  if (status === 'EXPIRED' || status === 'USED_UP') return 'info'
  return 'success'
}

onMounted(() => {
  fetchStats()
  fetchUpcoming()
  fetchMemberships()
  fetchRecentCheckins()
})

async function fetchUpcoming() {
  try {
    const res = await getMyOrders({ page: 1, size: 50 })
    const orders = res.data?.records || []
    const now = new Date()
    upcomingCourses.value = orders
      .filter(o => {
        if (o.status !== 'PAID' && o.status !== 'CONFIRMED') return false
        return true
      })
      .sort((a, b) => (b.createTime || '').localeCompare(a.createTime || ''))
      .slice(0, 5)
  } catch { upcomingCourses.value = [] }
}

async function fetchStats() {
  try {
    const [t, s, c] = await Promise.all([
      getTodayCheckInCount(),
      getCheckInStreak(),
      getMyCheckIns({ page: 1, size: 1 })
    ])
    stats.todayCount = t.data?.count || 0
    stats.streakDays = s.data?.streak || 0
    stats.totalCount = c.data?.total || 0
  } catch (e) { console.error('获取打卡统计失败', e) }
}

async function fetchMemberships() {
  cardLoading.value = true
  try {
    const res = await getMyCards({ page: 1, size: 50 })
    if (res.code === 200) {
      memberships.value = res.data?.records || []
    }
  } catch (e) {
    console.error('获取会员卡失败', e)
  } finally {
    cardLoading.value = false
  }
}

async function fetchRecentCheckins() {
  checkinLoading.value = true
  try {
    const res = await getMyCheckIns({ page: 1, size: 5 })
    if (res.code === 200) {
      recentCheckins.value = res.data?.records || []
    }
  } catch (e) {
    console.error('获取打卡记录失败', e)
  } finally {
    checkinLoading.value = false
  }
}

async function openCheckinDialog() {
  checkinVenueId.value = null
  checkinNote.value = ''
  checkinDialogVisible.value = true
  // Lazy load venue list
  if (venueList.value.length === 0) {
    try {
      const res = await getVenueList({ page: 1, size: 100, status: 'APPROVED' })
      venueList.value = res.data?.records || res.data || []
    } catch { /* ignore */ }
  }
}

async function doCheckin() {
  if (!checkinVenueId.value) return
  checkingIn.value = true
  try {
    const params = { venueId: checkinVenueId.value }
    if (checkinNote.value.trim()) {
      params.note = checkinNote.value.trim()
    }
    const res = await checkIn(params)
    if (res.code === 200) {
      ElMessage.success(res.message || '打卡成功！')
      checkinDialogVisible.value = false
      fetchStats()
      fetchRecentCheckins()
    } else {
      ElMessage.error(res.message || '打卡失败')
    }
  } catch (e) {
    ElMessage.error('网络错误，请重试')
  } finally {
    checkingIn.value = false
  }
}
</script>

<style scoped>
.my-page {
  max-width: 900px;
  margin: 0 auto;
}
.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 24px;
}

/* Stats Row */
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}
.stat-card {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  padding: 20px;
  text-align: center;
  transition: transform 0.2s;
}
.stat-card.today {
  border-top: 3px solid #409eff;
  cursor: pointer;
}
.stat-card.today:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(64,158,255,0.15);
}
.stat-card.streak { border-top: 3px solid #e6a23c; }
.stat-card.total { border-top: 3px solid #67c23a; }
.stat-num { font-size: 36px; font-weight: 800; color: #1a1a2e; line-height: 1.2; }
.stat-label { font-size: 14px; color: #909399; margin-top: 4px; }
.stat-action { font-size: 12px; color: #409eff; margin-top: 6px; font-weight: 500; }
.stat-action.checking { color: #909399; }

/* Section */
.section {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  padding: 20px 24px;
  margin-bottom: 24px;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.section-header h3 {
  font-size: 17px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

/* Membership Cards */
.cards-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.member-card {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  transition: all 0.2s;
}
.member-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.card-status-expired {
  opacity: 0.55;
  background: #fafafa;
}
.card-left {
  width: 72px;
  height: 72px;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 16px;
}
.card-left-session { background: linear-gradient(135deg, #667eea, #764ba2); }
.card-left-weekly { background: linear-gradient(135deg, #43e97b, #38f9d7); }
.card-left-monthly { background: linear-gradient(135deg, #f093fb, #f5576c); }
.card-left-quarterly { background: linear-gradient(135deg, #fa709a, #fee140); }

.card-icon { font-size: 22px; line-height: 1; }
.card-type-name {
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
  margin-top: 2px;
}

.card-center { flex: 1; min-width: 0; }
.card-center h4 { font-size: 15px; font-weight: 600; color: #1a1a2e; margin: 0 0 6px; }
.card-meta { display: flex; align-items: baseline; gap: 2px; }
.remain-info { display: flex; align-items: baseline; gap: 4px; flex-wrap: wrap; }
.remain-num { font-size: 24px; font-weight: 800; color: #409eff; line-height: 1; }
.remain-unit { font-size: 13px; color: #909399; }
.expire-date { font-size: 12px; color: #c0c4cc; margin-left: 8px; }

.card-right { flex-shrink: 0; margin-left: 12px; }

.cards-loading { padding: 8px 0; }

/* Upcoming Courses */
.upcoming-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.upcoming-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: 10px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.2s;
}
.upcoming-item:hover {
  background: #f0f4ff;
  border-color: #d0d9f0;
}
.upcoming-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.upcoming-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.upcoming-venue {
  font-size: 13px;
  color: #909399;
}
.upcoming-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.upcoming-time {
  font-size: 13px;
  color: #606266;
}

.table-card { margin-top: 0; }

/* Responsive */
@media (max-width: 768px) {
  .stats-row { grid-template-columns: repeat(3, 1fr); gap: 10px; }
  .stat-card { padding: 14px 8px; }
  .stat-num { font-size: 26px; }
  .member-card { flex-wrap: wrap; }
  .card-right { margin-left: auto; }
}
</style>
