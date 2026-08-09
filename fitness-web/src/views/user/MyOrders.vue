<template>
  <div class="my-orders-page">
    <!-- 钱包余额条 -->
    <div class="wallet-bar">
      <span class="wallet-label">账户余额</span>
      <span class="wallet-balance">¥{{ walletBalance.toFixed(2) }}</span>
      <el-button size="small" type="primary" @click="$router.push('/user/wallet')">充值</el-button>
    </div>

    <h2 class="page-title">我的订单</h2>

    <el-table
      :data="orderList"
      v-loading="loading"
      stripe
      style="width: 100%"
      :header-cell-style="{ background: '#fafafa', fontWeight: 600 }"
      empty-text="暂无订单记录"
    >
      <el-table-column prop="orderNo" label="订单编号" width="170">
        <template #default="{ row }">
          <span class="order-no">{{ row.orderNo }}</span>
        </template>
      </el-table-column>

      <el-table-column label="类型" width="85" align="center">
        <template #default="{ row }">
          <el-tag
            :type="row.orderType === 'CARD' ? 'warning' : 'primary'"
            effect="plain"
            size="small"
          >
            {{ row.orderType === 'CARD' ? '会员卡' : '课程' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="内容" min-width="160">
        <template #default="{ row }">
          <template v-if="row.orderType === 'CARD'">
            <span>{{ row.itemName }}</span>
            <div class="card-sub-info">
              <el-tag
                :type="getCardTypeTag(row.cardType)"
                size="small"
                effect="plain"
              >
                {{ cardTypeLabel(row.cardType) }}
              </el-tag>
            </div>
          </template>
          <template v-else>
            <el-link type="primary" :underline="false" @click="$router.push(row.itemLink)">
              {{ row.itemName }}
            </el-link>
          </template>
        </template>
      </el-table-column>

      <el-table-column prop="venueName" label="场馆" min-width="100" />

      <el-table-column label="金额" width="95" align="right">
        <template #default="{ row }">
          <span class="price-cell">¥{{ row.price.toFixed(2) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="时间/有效期" min-width="170">
        <template #default="{ row }">
          <template v-if="row.orderType === 'CARD'">
            <div class="card-time-info">
              <template v-if="row.cardType === 'SESSION'">
                <span class="remain-info">剩余 {{ row.remainingSessions ?? 0 }} 次</span>
              </template>
              <template v-else>
                <span class="remain-info" v-if="row.expireTime">
                  有效期至 {{ formatDate(row.expireTime) }}
                </span>
              </template>
            </div>
          </template>
          <template v-else>
            <div class="course-time">
              <el-icon><Clock /></el-icon>
              {{ row.courseTime || '待定' }}
            </div>
          </template>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row)" effect="plain" size="small">
            {{ getStatusLabel(row) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="createTime" label="下单时间" min-width="140">
        <template #default="{ row }">
          {{ formatDate(row.createTime) || '--' }}
        </template>
      </el-table-column>

      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <template v-if="row.orderType !== 'CARD' && (row.status === 'PAID' || row.status === 'CONFIRMED')">
            <el-button type="warning" size="small" text @click="handleRefund(row)">
              退款
            </el-button>
          </template>
          <template v-else-if="row.orderType !== 'CARD' && row.status === 'PENDING'">
            <el-button type="danger" size="small" text @click="handleCancel(row)">
              取消
            </el-button>
          </template>
          <span v-else class="no-action">--</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { Clock } from '@element-plus/icons-vue'
import { getMyOrders, cancelOrder, requestRefund } from '@/api/user'
import { getMyWallet } from '@/api/wallet'
import { getMyCards } from '@/api/membership'

const router = useRouter()

const walletBalance = ref(0)
const loading = ref(false)
const orderList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

onMounted(() => {
  fetchWallet()
  fetchAllOrders()
})

async function fetchWallet() {
  try {
    const res = await getMyWallet()
    walletBalance.value = res?.data?.balance || 0
  } catch { /* ignore */ }
}

async function fetchAllOrders() {
  loading.value = true
  try {
    const [courseRes, cardRes] = await Promise.all([
      getMyOrders({ page: 1, size: 200 }),
      getMyCards({ page: 1, size: 200 })
    ])

    const merged = []

    // Course orders — normalize to unified fields
    const courseOrders = courseRes.code === 200 ? (courseRes.data?.records || []) : []
    courseOrders.forEach(o => {
      merged.push({
        id: o.id,
        orderNo: o.orderNo || '',
        orderType: 'COURSE',
        itemName: o.courseName || '--',
        itemLink: `/fitness/course/${o.courseId}`,
        venueName: o.venueName || '--',
        price: Number(o.coursePrice || 0),
        cardType: null,
        courseId: o.courseId,
        courseTime: o.courseTime || '待定',
        expireTime: null,
        remainingSessions: null,
        status: o.status || 'PENDING',
        createTime: o.createTime || ''
      })
    })

    // Membership card orders — same unified fields
    const cardOrders = cardRes.code === 200 ? (cardRes.data?.records || []) : []
    cardOrders.forEach(c => {
      merged.push({
        id: c.id,
        orderNo: c.orderNo || '',
        orderType: 'CARD',
        itemName: c.cardName || '--',
        itemLink: null,
        venueName: c.venueName || '--',
        price: Number(c.price || 0),
        cardType: c.cardType,
        courseId: null,
        courseTime: null,
        expireTime: c.expireTime || null,
        remainingSessions: c.remainingSessions ?? null,
        status: c.status || 'ACTIVE',
        createTime: c.createTime || ''
      })
    })

    // Store merged list for pagination
    allOrdersCache.value = merged
    allOrdersCache.value.sort((a, b) => (b.createTime || '').localeCompare(a.createTime || ''))
    total.value = allOrdersCache.value.length
    slicePage()
  } catch (e) {
    console.error('获取订单列表失败', e)
  } finally {
    loading.value = false
  }
}

const allOrdersCache = ref([])
function slicePage() {
  const start = (currentPage.value - 1) * pageSize.value
  orderList.value = allOrdersCache.value.slice(start, start + pageSize.value)
}

function onPageChange() {
  slicePage()
}

function onSizeChange() {
  currentPage.value = 1
  slicePage()
}

function cardTypeLabel(type) {
  const map = { SESSION: '次卡', WEEKLY: '周卡', MONTHLY: '月卡', QUARTERLY: '季卡' }
  return map[type] || type
}

function getCardTypeTag(type) {
  const map = { SESSION: '', WEEKLY: 'success', MONTHLY: 'warning', QUARTERLY: 'danger' }
  return map[type] || 'info'
}

function formatDate(str) {
  if (!str) return '--'
  return str.substring(0, 10)
}

function getStatusType(row) {
  if (row.orderType === 'CARD') {
    const map = { ACTIVE: 'success', EXPIRED: 'info', USED_UP: 'info' }
    return map[row.status] || 'info'
  }
  const map = { PENDING: 'warning', PAID: 'success', CONFIRMED: 'primary', REFUNDING: 'warning', REFUNDED: 'info', CANCELLED: 'info', COMPLETED: 'success' }
  return map[row.status] || 'info'
}

function getStatusLabel(row) {
  if (row.orderType === 'CARD') {
    if (row.status === 'EXPIRED') return '已过期'
    if (row.status === 'USED_UP') return '已用完'
    if (row.cardType === 'SESSION') {
      return (row.remainingSessions ?? 0) > 0 ? '使用中' : '已用完'
    }
    if (row.expireTime) {
      const days = Math.ceil((new Date(row.expireTime) - new Date()) / (1000 * 60 * 60 * 24))
      return days > 0 ? '使用中' : '已过期'
    }
    return '使用中'
  }
  const map = { PENDING: '待支付', PAID: '已支付', CONFIRMED: '已确认', REFUNDING: '退款中', REFUNDED: '已退款', CANCELLED: '已取消', COMPLETED: '已完成' }
  return map[row.status] || row.status
}

async function handleCancel(row) {
  try {
    await ElMessageBox.confirm(`确定取消 "${row.itemName}" 吗？`, '取消预约', {
      confirmButtonText: '确定', cancelButtonText: '再想想', type: 'warning'
    })
  } catch { return }
  try {
    await cancelOrder(row.id)
    ElMessage.success('已取消')
    fetchAllOrders()
    fetchWallet()
  } catch (e) { ElMessage.error('取消失败') }
}

async function handleRefund(row) {
  let reason = ''
  try {
    const { value } = await ElMessageBox.prompt(
      `申请退款 "${row.itemName}"\n退款金额 ¥${row.price.toFixed(2)}`,
      '申请退款',
      { confirmButtonText: '提交申请', cancelButtonText: '再想想', inputPlaceholder: '请填写退款理由', inputType: 'textarea', inputValidator: (v) => v && v.trim() ? true : '请填写退款理由' }
    )
    if (!value || !value.trim()) return
    reason = value.trim()
  } catch { return }
  try {
    const res = await requestRefund(row.id, reason)
    ElMessage.success(res?.message || '退款申请已提交，请等待管理员审核')
    fetchAllOrders()
    fetchWallet()
  } catch (e) { ElMessage.error('申请失败') }
}
</script>

<style scoped>
.my-orders-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #409eff;
}

.wallet-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  padding: 12px 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  color: #fff;
}

.wallet-label {
  font-size: 14px;
  opacity: 0.85;
}

.wallet-balance {
  font-size: 20px;
  font-weight: 700;
  flex: 1;
}

.price-cell {
  font-weight: 600;
  color: #f56c6c;
}

.order-no {
  font-family: monospace;
  font-size: 13px;
  color: #606266;
}

.course-time {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 13px;
}

.no-action {
  color: #c0c4cc;
  font-size: 13px;
}

.card-sub-info {
  margin-top: 4px;
}

.card-time-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #606266;
}

.remain-info {
  color: #409eff;
  font-weight: 500;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 16px 0;
}
</style>
