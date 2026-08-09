<template>
  <div class="order-manage-page">
    <div class="manage-container">
      <h2 class="page-title">订单管理</h2>

      <div class="filter-tabs">
        <el-radio-group v-model="statusFilter" @change="handleFilterChange">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="PAID">已支付</el-radio-button>
          <el-radio-button value="REFUNDING">退款中</el-radio-button>
          <el-radio-button value="CONFIRMED">已确认</el-radio-button>
          <el-radio-button value="COMPLETED">已完成</el-radio-button>
          <el-radio-button value="CANCELLED">已取消</el-radio-button>
        </el-radio-group>
      </div>

      <div class="table-card">
        <el-table v-loading="loading" :data="orderList" border stripe style="width: 100%" empty-text="暂无订单">
          <el-table-column prop="orderNo" label="订单编号" min-width="160" />
          <el-table-column prop="userName" label="用户" min-width="100" />
          <el-table-column prop="courseName" label="课程名称" min-width="150" />
          <el-table-column prop="venueName" label="场馆" min-width="100" />
          <el-table-column label="金额" width="90" align="right">
            <template #default="{ row }">
              <span class="price-cell">¥{{ Number(row.coursePrice || 0).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="课程时间" min-width="160">
            <template #default="{ row }">{{ row.courseTime || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="150">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" min-width="200" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 'PAID'" type="success" size="small" :loading="actionLoading === row.id" @click="handleConfirm(row)">确认上课</el-button>
              <el-button v-if="row.status === 'CONFIRMED'" type="primary" size="small" :loading="actionLoading === row.id" @click="handleComplete(row)">完成</el-button>
              <el-button v-if="row.status === 'REFUNDING'" type="success" size="small" :loading="actionLoading === row.id" @click="handleApproveRefund(row)">同意退款</el-button>
              <el-button v-if="row.status === 'REFUNDING'" type="danger" size="small" :loading="actionLoading === row.id" @click="handleRejectRefund(row)">拒绝</el-button>
              <el-button v-if="row.status === 'PAID' || row.status === 'CONFIRMED'" type="warning" size="small" :loading="actionLoading === row.id" @click="handleRefund(row)">退款</el-button>
              <el-button v-if="row.status === 'PAID' || row.status === 'CONFIRMED'" type="danger" size="small" :loading="actionLoading === row.id" @click="handleCancel(row)">取消</el-button>
              <span v-if="row.status === 'COMPLETED' || row.status === 'CANCELLED'" class="no-action">--</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="page" v-model:page-size="size" :total="total"
            :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
            @change="fetchOrderList"
          />
        </div>
      </div>
    </div>

    <!-- 取消/退款确认 -->
    <el-dialog v-model="actionDialog.visible" :title="actionDialog.title" width="420px">
      <p>{{ actionDialog.message }}</p>
      <template #footer>
        <el-button @click="actionDialog.visible = false">返回</el-button>
        <el-button :type="actionDialog.btnType" :loading="actionLoading" @click="actionDialog.onConfirm">
          {{ actionDialog.btnText }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getVenueOrders, updateOrderStatus, approveRefund, rejectRefund } from '@/api/order'

const loading = ref(false)
const orderList = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const statusFilter = ref('')
const actionLoading = ref(null)

const actionDialog = reactive({
  visible: false, title: '', message: '', btnType: 'primary', btnText: '', onConfirm: () => {}
})

const statusMap = {
  PAID: { type: 'success', text: '已支付' },
  REFUNDING: { type: 'warning', text: '退款中' },
  REFUNDED: { type: 'info', text: '已退款' },
  CONFIRMED: { type: 'primary', text: '已确认' },
  COMPLETED: { type: 'success', text: '已完成' },
  CANCELLED: { type: 'info', text: '已取消' }
}

function getStatusType(s) { return statusMap[s]?.type || 'info' }
function getStatusText(s) { return statusMap[s]?.text || s }

function formatDateTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

function handleFilterChange() { page.value = 1; fetchOrderList() }

async function fetchOrderList() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (statusFilter.value) params.status = statusFilter.value
    const res = await getVenueOrders(params)
    orderList.value = res.data?.records || res.data?.list || []
    total.value = res.data?.total || 0
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

async function handleConfirm(row) {
  actionLoading.value = row.id
  try {
    await updateOrderStatus(row.id, 'CONFIRMED')
    ElMessage.success('已确认上课')
    fetchOrderList()
  } catch { ElMessage.error('操作失败') }
  finally { actionLoading.value = null }
}

async function handleComplete(row) {
  actionLoading.value = row.id
  try {
    await updateOrderStatus(row.id, 'COMPLETED')
    ElMessage.success('已完成')
    fetchOrderList()
  } catch { ElMessage.error('操作失败') }
  finally { actionLoading.value = null }
}

function handleCancel(row) {
  actionDialog.title = '取消订单'
  actionDialog.message = `确定取消订单 "${row.orderNo}" 吗？`
  actionDialog.btnType = 'danger'
  actionDialog.btnText = '确认取消'
  actionDialog.onConfirm = async () => {
    actionLoading.value = row.id
    try {
      await updateOrderStatus(row.id, 'CANCELLED')
      ElMessage.success('已取消')
      actionDialog.visible = false
      fetchOrderList()
    } catch { ElMessage.error('操作失败') }
    finally { actionLoading.value = null }
  }
  actionDialog.visible = true
}

function handleRefund(row) { /* Admin manually refunds - same as before */ }

function handleApproveRefund(row) {
  actionDialog.title = '审批退款'
  actionDialog.message = `同意退款 "${row.courseName}"？¥${Number(row.coursePrice || 0).toFixed(2)} 将退回用户钱包。`
  actionDialog.btnType = 'success'
  actionDialog.btnText = '同意退款'
  actionDialog.onConfirm = async () => {
    actionLoading.value = row.id
    try {
      await approveRefund(row.id)
      ElMessage.success('退款成功，金额已退回用户钱包')
      actionDialog.visible = false
      fetchOrderList()
    } catch { ElMessage.error('操作失败') }
    finally { actionLoading.value = null }
  }
  actionDialog.visible = true
}

function handleRejectRefund(row) {
  actionDialog.title = '拒绝退款'
  actionDialog.message = `拒绝 "${row.courseName}" 的退款申请？订单将恢复为已支付状态。`
  actionDialog.btnType = 'danger'
  actionDialog.btnText = '拒绝退款'
  actionDialog.onConfirm = async () => {
    actionLoading.value = row.id
    try {
      await rejectRefund(row.id)
      ElMessage.success('已拒绝退款申请')
      actionDialog.visible = false
      fetchOrderList()
    } catch { ElMessage.error('操作失败') }
    finally { actionLoading.value = null }
  }
  actionDialog.visible = true
}

onMounted(() => { fetchOrderList() })
</script>

<style scoped>
.order-manage-page { min-height: 100vh; background: #f5f7fa; }
.manage-container { max-width: 1200px; margin: 0 auto; padding: 24px 20px 60px; }
.page-title { font-size: 24px; font-weight: 700; color: #303133; margin: 0 0 20px; }
.filter-tabs { margin-bottom: 20px; }
.table-card { background: #fff; border: 1px solid #e8e8e8; border-radius: 10px; padding: 24px; }
.pagination-wrapper { display: flex; justify-content: center; margin-top: 20px; }
.price-cell { font-weight: 600; color: #f56c6c; }
.no-action { color: #c0c4cc; font-size: 13px; }
</style>
