<template>
  <div class="admin-dashboard">
    <div class="dashboard-container">
      <h2 class="page-title">平台管理后台</h2>

      <!-- Stats Overview -->
      <div class="stats-row">
        <div class="stat-card clickable" @click="$router.push('/admin/users')">
          <div class="stat-icon users-icon">
            <el-icon :size="30"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalUsers }}</div>
            <div class="stat-label">用户总数 <el-icon><ArrowRight /></el-icon></div>
          </div>
        </div>

        <div class="stat-card clickable" @click="$router.push('/admin/venues')">
          <div class="stat-icon venues-icon">
            <el-icon :size="30"><OfficeBuilding /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalVenues }}</div>
            <div class="stat-label">场馆总数 <el-icon><ArrowRight /></el-icon></div>
          </div>
        </div>

        <div class="stat-card pending-highlight clickable" @click="$router.push('/admin/venues')">
          <div class="stat-icon pending-icon">
            <el-icon :size="30"><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value warning">{{ stats.pendingVenues }}</div>
            <div class="stat-label">待审核场馆 <el-icon><ArrowRight /></el-icon></div>
          </div>
        </div>

        <div class="stat-card clickable" @click="$router.push('/admin/courses')">
          <div class="stat-icon courses-icon">
            <el-icon :size="30"><Reading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalCourses }}</div>
            <div class="stat-label">课程总数 <el-icon><ArrowRight /></el-icon></div>
          </div>
        </div>

        <div class="stat-card clickable" @click="$router.push('/admin/coaches')">
          <div class="stat-icon coaches-icon">
            <el-icon :size="30"><UserFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalCoaches }}</div>
            <div class="stat-label">教练管理 <el-icon><ArrowRight /></el-icon></div>
          </div>
        </div>

        <div class="stat-card clickable" @click="$router.push('/admin/orders')">
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

      <!-- Charts / Data Display Area -->
      <div class="data-row">
        <!-- Venue Approval Rate -->
        <div class="data-card">
          <h3 class="data-card-title">场馆审核率</h3>
          <div class="data-card-body">
            <div class="big-number">
              <span class="big-number-value">{{ approvalRate }}</span>
              <span class="big-number-unit">%</span>
            </div>
            <div class="data-detail">
              <div class="detail-item">
                <span class="detail-label">已通过</span>
                <span class="detail-value success">{{ stats.approvedVenues || 0 }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">待审核</span>
                <span class="detail-value warning">{{ stats.pendingVenues || 0 }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">已拒绝</span>
                <span class="detail-value danger">{{ stats.rejectedVenues || 0 }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- User Growth Trend -->
        <div class="data-card">
          <h3 class="data-card-title">用户增长趋势</h3>
          <div class="data-card-body">
            <div class="growth-summary">
              <div class="growth-row">
                <span class="growth-label">本周新增</span>
                <span class="growth-value primary">+{{ stats.weekNewUsers || 0 }}</span>
              </div>
              <div class="growth-row">
                <span class="growth-label">本月新增</span>
                <span class="growth-value primary">+{{ stats.monthNewUsers || 0 }}</span>
              </div>
              <div class="growth-row">
                <span class="growth-label">总活跃用户</span>
                <span class="growth-value">{{ stats.activeUsers || 0 }}</span>
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
import { ref, reactive, computed, onMounted } from 'vue'
import {
  User, UserFilled, OfficeBuilding, Clock, Reading, Tickets, Checked, ArrowRight
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

const approvalRate = computed(() => {
  const total = (stats.approvedVenues || 0) + (stats.pendingVenues || 0) + (stats.rejectedVenues || 0)
  if (total === 0) return '0'
  return ((stats.approvedVenues / total) * 100).toFixed(1)
})

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

.pending-highlight {
  border-color: #fa8c16;
  background: #fffbe6;
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
  display: flex;
  align-items: center;
  gap: 40px;
}

.big-number {
  text-align: center;
}

.big-number-value {
  font-size: 48px;
  font-weight: 700;
  color: #667eea;
}

.big-number-unit {
  font-size: 20px;
  color: #909399;
}

.data-detail {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.detail-label {
  font-size: 14px;
  color: #909399;
}

.detail-value {
  font-size: 14px;
  font-weight: 600;
}

.detail-value.success { color: #67c23a; }
.detail-value.warning { color: #fa8c16; }
.detail-value.danger { color: #f56c6c; }

/* Growth summary */
.growth-summary {
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
}

.growth-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.growth-row:last-child {
  border-bottom: none;
}

.growth-label {
  font-size: 14px;
  color: #909399;
}

.growth-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.growth-value.primary {
  color: #667eea;
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
