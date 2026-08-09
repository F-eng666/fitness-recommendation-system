<template>
  <div class="admin-dashboard">
    <div class="dashboard-container">
      <h2 class="page-title">平台管理后台</h2>

      <!-- Stats Overview -->
      <div class="stats-row">
        <div class="stat-card clickable" @click="$router.push('/system/users')">
          <div class="stat-icon users-icon">
            <el-icon :size="30"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalUsers }}</div>
            <div class="stat-label">用户总数 <el-icon><ArrowRight /></el-icon></div>
          </div>
        </div>

        <div class="stat-card clickable" @click="$router.push('/system/venues')">
          <div class="stat-icon venues-icon">
            <el-icon :size="30"><OfficeBuilding /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalVenues }}</div>
            <div class="stat-label">场馆总数 <el-icon><ArrowRight /></el-icon></div>
          </div>
        </div>

        <div class="stat-card clickable" @click="$router.push('/system/courses')">
          <div class="stat-icon courses-icon">
            <el-icon :size="30"><Reading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalCourses }}</div>
            <div class="stat-label">课程总数 <el-icon><ArrowRight /></el-icon></div>
          </div>
        </div>

        <div class="stat-card clickable" @click="$router.push('/system/coaches')">
          <div class="stat-icon coaches-icon">
            <el-icon :size="30"><UserFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalCoaches }}</div>
            <div class="stat-label">教练管理 <el-icon><ArrowRight /></el-icon></div>
          </div>
        </div>

        <div class="stat-card clickable" @click="$router.push('/system/orders')">
          <div class="stat-icon orders-icon">
            <el-icon :size="30"><Tickets /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.todayOrders }}</div>
            <div class="stat-label">今日订单 <el-icon><ArrowRight /></el-icon></div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon checkin-icon">
            <el-icon :size="30"><Checked /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.todayCheckins }}</div>
            <div class="stat-label">今日签到 <el-icon><ArrowRight /></el-icon></div>
          </div>
        </div>
      </div>

      <!-- 数据统计面板 -->
      <div class="data-row">
        <!-- 营收统计 -->
        <div class="data-card">
          <h3 class="data-card-title">营收统计</h3>
          <div class="data-card-body revenue-body">
            <div class="revenue-main">
              <span class="revenue-big">¥{{ formatMoney(stats.totalRevenue || 0) }}</span>
              <span class="revenue-label">累计营收</span>
            </div>
            <div class="revenue-breakdown">
              <div class="rev-item">
                <span class="rev-label">今日营收</span>
                <span class="rev-value">¥{{ formatMoney(stats.todayRevenue || 0) }}</span>
              </div>
              <div class="rev-item">
                <span class="rev-label">本月营收</span>
                <span class="rev-value">¥{{ formatMoney(stats.monthRevenue || 0) }}</span>
              </div>
              <div class="rev-item">
                <span class="rev-label">今日订单</span>
                <span class="rev-value">{{ stats.todayOrders || 0 }} 单</span>
              </div>
              <div class="rev-item">
                <span class="rev-label">本周订单</span>
                <span class="rev-value">{{ stats.weekOrders || 0 }} 单</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 用户统计 -->
        <div class="data-card">
          <h3 class="data-card-title">用户统计</h3>
          <div class="data-card-body user-stats-body">
            <div class="user-stat-grid">
              <div class="user-stat-item">
                <span class="us-value primary">{{ stats.totalUsers || 0 }}</span>
                <span class="us-label">总用户</span>
              </div>
              <div class="user-stat-item">
                <span class="us-value success">{{ stats.weekNewUsers || 0 }}</span>
                <span class="us-label">本周新增</span>
              </div>
              <div class="user-stat-item">
                <span class="us-value warning">{{ stats.monthNewUsers || 0 }}</span>
                <span class="us-label">本月新增</span>
              </div>
              <div class="user-stat-item">
                <span class="us-value">{{ stats.activeUsers || 0 }}</span>
                <span class="us-label">活跃用户</span>
              </div>
            </div>
            <div class="user-stat-extras">
              <div class="extra-row">
                <span>今日签到</span>
                <span class="extra-val">{{ stats.todayCheckins || 0 }} 次</span>
              </div>
              <div class="extra-row">
                <span>社区文章</span>
                <span class="extra-val">{{ stats.totalArticles || 0 }} 篇</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Recent Registrations -->
      <div class="registrations-card">
        <h3 class="section-title">最近注册用户</h3>
        <el-table
          v-loading="regLoading"
          :data="recentUsers"
          border
          stripe
          style="width: 100%"
          empty-text="暂无数据"
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column prop="nickname" label="昵称" min-width="120" />
          <el-table-column prop="phone" label="手机号" min-width="130" />
          <el-table-column label="角色" width="100">
            <template #default="{ row }">
              <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'" size="small">
                {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="注册时间" min-width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  User, UserFilled, OfficeBuilding, Reading, Tickets, Checked, ArrowRight
} from '@element-plus/icons-vue'
import { getAdminStats } from '@/api/admin'
import { getUserList } from '@/api/user'

const stats = reactive({
  totalUsers: 0,
  totalVenues: 0,
  pendingVenues: 0,
  approvedVenues: 0,
  rejectedVenues: 0,
  totalCourses: 0,
  totalCoaches: 0,
  todayOrders: 0,
  todayCheckins: 0,
  weekNewUsers: 0,
  monthNewUsers: 0,
  activeUsers: 0
})

const regLoading = ref(false)
const recentUsers = ref([])

function formatMoney(val) {
  const num = Number(val) || 0
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDateTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

async function fetchStats() {
  try {
    const res = await getAdminStats()
    if (res.data) {
      Object.assign(stats, res.data)
    }
  } catch {
    // silently use defaults
  }
}

async function fetchRecentUsers() {
  regLoading.value = true
  try {
    const res = await getUserList({ page: 1, pageSize: 10, sort: 'latest' })
    recentUsers.value = res.data?.records || res.data?.list || []
  } catch {
    recentUsers.value = []
  } finally {
    regLoading.value = false
  }
}

onMounted(() => {
  fetchStats()
  fetchRecentUsers()
})
</script>

<style scoped>
.admin-dashboard {
  min-height: 100vh;
  background: #f5f7fa;
}

.dashboard-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 24px;
}

/* Stats Row */
.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: box-shadow 0.3s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.stat-card.clickable {
  cursor: pointer;
}

.stat-card.clickable:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.users-icon { background: linear-gradient(135deg, #667eea, #764ba2); }
.venues-icon { background: linear-gradient(135deg, #43e97b, #38f9d7); }
.pending-icon { background: linear-gradient(135deg, #fa709a, #fee140); }
.courses-icon { background: linear-gradient(135deg, #30cfd0, #330867); }
.coaches-icon { background: linear-gradient(135deg, #fa709a, #fee140); }
.orders-icon { background: linear-gradient(135deg, #a8edea, #fed6e3); }
.checkin-icon { background: linear-gradient(135deg, #f093fb, #f5576c); }

.stat-info {
  min-width: 0;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-value.warning {
  color: #fa8c16;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

/* Data Row */
.data-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.data-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 20px 24px;
}

.data-card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px;
}

.data-card-body {
  flex-direction: column;
  align-items: stretch;
  gap: 20px;
}

/* Revenue stats */
.revenue-body {
  gap: 16px;
}

.revenue-main {
  text-align: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.revenue-big {
  display: block;
  font-size: 36px;
  font-weight: 800;
  color: #f56c6c;
  font-family: 'DIN', 'Helvetica Neue', sans-serif;
}

.revenue-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
  display: block;
}

.revenue-breakdown {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.rev-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.rev-label {
  font-size: 12px;
  color: #909399;
}

.rev-value {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

/* User stats */
.user-stats-body {
  gap: 16px;
}

.user-stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.user-stat-item {
  text-align: center;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.us-value {
  display: block;
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.us-value.primary { color: #667eea; }
.us-value.success { color: #67c23a; }
.us-value.warning { color: #e6a23c; }

.us-label {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.user-stat-extras {
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.extra-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #606266;
}

.extra-val {
  font-weight: 600;
  color: #303133;
}

/* Recent Registrations */
.registrations-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 20px 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px;
}

@media (max-width: 1000px) {
  .stats-row {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .data-row {
    grid-template-columns: 1fr;
  }
}
</style>
