<template>
  <div class="order-manage-page">
    <div class="manage-container">
      <h2 class="page-title">订单管理</h2>

      <!-- Status Filter Tabs -->
      <div class="filter-tabs">
        <el-radio-group v-model="statusFilter" @change="handleFilterChange">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="PENDING">待确认</el-radio-button>
          <el-radio-button value="CONFIRMED">已确认</el-radio-button>
          <el-radio-button value="COMPLETED">已完成</el-radio-button>
          <el-radio-button value="CANCELLED">已取消</el-radio-button>
        </el-radio-group>
      </div>

      <!-- Orders Table -->
      <div class="table-card">
        <el-table
          v-loading="loading"
          :data="orderList"
          border
          stripe
          style="width: 100%"
          empty-text="暂无订单"
        >
          <el-table-column prop="orderNo" label="订单编号" min-width="160" />
          <el-table-column prop="userName" label="用户" min-width="100" />
          <el-table-column prop="courseName" label="课程名称" min-width="150" />
          <el-table-column label="课程时间" min-width="160">
            <template #default="{ row }">
              {{ row.courseTime || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="200" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'PENDING'"
                type="success"
                size="small"
                :loading="actionLoading === row.id"
                @click="handleConfirm(row)"
              >
                确认
              </el-button>
              <el-button
                v-if="row.status === 'CONFIRMED'"
                type="primary"
                size="small"
                :loading="actionLoading === row.id"
                @click="handleComplete(row)"
              >
                完成
              </el-button>
              <el-button
                v-if="row.status === 'PENDING' || row.status === 'CONFIRMED'"
                type="danger"
                size="small"
                :loading="actionLoading === row.id"
                @click="handleCancel(row)"
              >
                取消
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- Pagination -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @change="fetchOrderList"
          />
        </div>
      </div>
    </div>

    <!-- Cancel Confirm Dialog -->
    <el-dialog v-model="cancelDialogVisible" title="取消订单" width="400px">
      <p>确定要取消订单 "{{ cancelTarget?.orderNo }}" 吗？</p>
      <template #footer>
        <el-button @click="cancelDialogVisible = false">返回</el-button>
        <el-button type="danger" :loading="actionLoading" @click="confirmCancel">
          确认取消
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getVenueOrders, updateOrderStatus } from '@/api/order'

const loading = ref(false)
const orderList = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const statusFilter = ref('')
const actionLoading = ref(null)

const cancelDialogVisible = ref(false)
const cancelTarget = ref(null)

const statusMap = {
  PENDING: { type: 'warning', text: '待确认' },
  CONFIRMED: { type: 'primary', text: '已确认' },
  COMPLETED: { type: 'success', text: '已完成' },
  CANCELLED: { type: 'info', text: '已取消' }
}

function getStatusType(status) {
  return statusMap[status]?.type || 'info'
}

function getStatusText(status) {
  return statusMap[status]?.text || status
}

function formatDateTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

function handleFilterChange() {
  page.value = 1
  fetchOrderList()
}

async function fetchOrderList() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (statusFilter.value) params.status = statusFilter.value
    const res = await getVenueOrders(params)
    orderList.value = res.data?.records || res.data?.list || []
    total.value = res.data?.total || 0
  } catch {
    ElMessage.error('加载订单列表失败')
  } finally {
    loading.value = false
  }
}

async function handleConfirm(row) {
  actionLoading.value = row.id
  try {
    await updateOrderStatus(row.id, 'CONFIRMED')
    ElMessage.success('订单已确认')
    fetchOrderList()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    actionLoading.value = null
  }
}

async function handleComplete(row) {
  actionLoading.value = row.id
  try {
    await updateOrderStatus(row.id, 'COMPLETED')
    ElMessage.success('订单已完成')
    fetchOrderList()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    actionLoading.value = null
  }
}

function handleCancel(row) {
  cancelTarget.value = row
  cancelDialogVisible.value = true
}

async function confirmCancel() {
  actionLoading.value = cancelTarget.value?.id
  try {
    await updateOrderStatus(cancelTarget.value.id, 'CANCELLED')
    ElMessage.success('订单已取消')
    cancelDialogVisible.value = false
    fetchOrderList()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    actionLoading.value = null
  }
}

onMounted(() => {
  fetchOrderList()
})
</script>

<style scoped>
.order-manage-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.manage-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 20px;
}

.filter-tabs {
  margin-bottom: 20px;
}

.table-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 24px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
