<template>
  <div class="venue-admin-dashboard">
    <div class="dashboard-container">
      <h2 class="page-title">场馆管理后台</h2>

      <!-- Stats Cards -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-icon venues-icon">
            <el-icon :size="28"><OfficeBuilding /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.venueCount }}</div>
            <div class="stat-label">场馆数量</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon courses-icon">
            <el-icon :size="28"><Reading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.courseCount }}</div>
            <div class="stat-label">课程总数</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon orders-icon">
            <el-icon :size="28"><Tickets /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.pendingOrders }}</div>
            <div class="stat-label">待处理订单</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon checkin-icon">
            <el-icon :size="28"><Checked /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.todayCheckins }}</div>
            <div class="stat-label">今日签到</div>
          </div>
        </div>
      </div>

      <!-- Quick Actions -->
      <div class="quick-actions">
        <h3 class="section-title">快捷操作</h3>
        <div class="actions-row">
          <el-button type="primary" :icon="Plus" @click="goTo('/venue-admin/courses')">
            发布新课程
          </el-button>
          <el-button type="success" :icon="Plus" @click="goTo('/venue-admin/coaches')">
            添加教练
          </el-button>
          <el-button type="warning" :icon="Edit" @click="$router.push('/system/courses')">
            管理课程
          </el-button>
        </div>
      </div>

      <!-- Multiple Venues Link -->
      <div v-if="hasMultipleVenues" class="multi-venue-link">
        <el-alert
          title="您管理多个场馆"
          type="info"
          show-icon
          :closable="false"
        >
          <template #default>
            <el-button type="primary" link @click="goTo('/venue-admin/switch')">
              进入场馆管理
            </el-button>
          </template>
        </el-alert>
      </div>

      <!-- Recent Orders Table -->
      <div class="orders-section">
        <h3 class="section-title">近期订单</h3>
        <el-table
          v-loading="ordersLoading"
          :data="recentOrders"
          border
          stripe
          style="width: 100%"
          empty-text="暂无订单"
        >
          <el-table-column prop="orderNo" label="订单编号" min-width="160" />
          <el-table-column prop="userName" label="用户" min-width="100" />
          <el-table-column prop="courseName" label="课程名称" min-width="150" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getOrderStatusType(row.status)" size="small">
                {{ getOrderStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" min-width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'PENDING'"
                type="success" size="small"
                :loading="actionLoading === row.id"
                @click="handleConfirm(row)"
              >确认</el-button>
              <el-button
                v-if="row.status === 'PENDING' || row.status === 'CONFIRMED'"
                type="danger" size="small"
                :loading="actionLoading === row.id"
                @click="handleCancel(row)"
              >取消</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted} from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  OfficeBuilding, Reading, Tickets, Checked, Plus, Edit
} from '@element-plus/icons-vue'
import { getMyVenues } from '@/api/venue'
import { getCourseList } from '@/api/course'
import { getVenueOrders, updateOrderStatus } from '@/api/order'

const router = useRouter()

const stats = reactive({
  venueCount: 0,
  courseCount: 0,
  pendingOrders: 0,
  todayCheckins: 0
})

const recentOrders = ref([])
const ordersLoading = ref(false)
const actionLoading = ref(null)
const hasMultipleVenues = ref(false)

const orderStatusMap = {
  PENDING: { type: 'warning', text: '待确认' },
  CONFIRMED: { type: 'primary', text: '已确认' },
  COMPLETED: { type: 'success', text: '已完成' },
  CANCELLED: { type: 'info', text: '已取消' }
}

function getOrderStatusType(status) {
  return orderStatusMap[status]?.type || 'info'
}

function getOrderStatusText(status) {
  return orderStatusMap[status]?.text || status
}

function goTo(path) { router.push(path) }

function formatDateTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

async function fetchStats() {
  try {
    // 获取我的场馆数量
    const venueRes = await getMyVenues()
    const venues = venueRes.data || []
    stats.venueCount = Array.isArray(venues) ? venues.length : (venues.records?.length || 0)
    hasMultipleVenues.value = stats.venueCount > 1
    // 获取课程数量
    const courseRes = await getCourseList({ page: 1, size: 1 })
    stats.courseCount = courseRes.data?.total || 0
  } catch { /* use defaults */ }
}

async function fetchRecentOrders() {
  ordersLoading.value = true
  try {
    const res = await getVenueOrders({ page: 1, size: 10 })
    recentOrders.value = res.data?.records || []
    // 统计待确认订单数
    stats.pendingOrders = recentOrders.value.filter(o => o.status === 'PENDING').length
  } catch {
    recentOrders.value = []
  } finally {
    ordersLoading.value = false
  }
}

async function handleConfirm(row) {
  actionLoading.value = row.id
  try {
    await updateOrderStatus(row.id, 'CONFIRMED')
    row.status = 'CONFIRMED'
    stats.pendingOrders = Math.max(0, stats.pendingOrders - 1)
    ElMessage.success('订单已确认')
  } catch {
    ElMessage.error('操作失败')
  } finally { actionLoading.value = null }
}

async function handleCancel(row) {
  actionLoading.value = row.id
  try {
    await updateOrderStatus(row.id, 'CANCELLED')
    row.status = 'CANCELLED'
    stats.pendingOrders = Math.max(0, stats.pendingOrders - 1)
    ElMessage.success('订单已取消')
  } catch {
    ElMessage.error('操作失败')
  } finally { actionLoading.value = null }
}

onMounted(() => {
  fetchStats()
  fetchRecentOrders()
})
</script>

<style scoped>
.venue-admin-dashboard {
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

/* Stats Cards */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 28px;
}

.stat-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: box-shadow 0.3s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.venues-icon { background: linear-gradient(135deg, #667eea, #764ba2); }
.courses-icon { background: linear-gradient(135deg, #43e97b, #38f9d7); }
.orders-icon { background: linear-gradient(135deg, #fa709a, #fee140); }
.checkin-icon { background: linear-gradient(135deg, #30cfd0, #330867); }

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

/* Quick Actions */
.quick-actions {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 20px 24px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px;
}

.actions-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

/* Multi Venue Link */
.multi-venue-link {
  margin-bottom: 20px;
}

/* Orders Section */
.orders-section {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 20px 24px;
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
