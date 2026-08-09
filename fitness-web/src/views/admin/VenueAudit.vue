<template>
  <div class="venue-audit-page">
    <div class="manage-container">
      <h2 class="page-title">场馆审核</h2>

      <!-- Status Tabs -->
      <div class="tabs-bar">
        <el-radio-group v-model="auditTab" @change="handleTabChange">
          <el-radio-button value="PENDING">
            <el-badge :value="counts.pending" :hidden="counts.pending === 0" class="tab-badge">
              待审核
            </el-badge>
          </el-radio-button>
          <el-radio-button value="APPROVED">已通过</el-radio-button>
          <el-radio-button value="REJECTED">已拒绝</el-radio-button>
        </el-radio-group>
      </div>

      <!-- Empty State -->
      <el-empty v-if="!loading && venueList.length === 0" :description="emptyText" />

      <!-- Venue Card Grid -->
      <div v-loading="loading" class="venue-grid">
        <div
          v-for="venue in venueList"
          :key="venue.id"
          class="venue-card"
        >
          <!-- Cover Image -->
          <div class="venue-cover">
            <el-image
              v-if="venue.coverImage"
              :src="venue.coverImage"
              fit="cover"
              class="cover-img"
            >
              <template #error>
                <div class="cover-placeholder">
                  <el-icon :size="36"><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <div v-else class="cover-placeholder">
              <el-icon :size="36"><Picture /></el-icon>
            </div>
            <!-- Status Badge -->
            <el-tag
              :type="getStatusTagType(venue.status)"
              class="status-badge"
              effect="dark"
            >
              {{ getStatusText(venue.status) }}
            </el-tag>
          </div>

          <!-- Venue Info -->
          <div class="venue-info">
            <h4 class="venue-name">{{ venue.name }}</h4>
            <p class="venue-address">
              <el-icon><Location /></el-icon>
              {{ venue.address }}
            </p>
            <p class="venue-phone" v-if="venue.phone">
              <el-icon><Phone /></el-icon>
              {{ venue.phone }}
            </p>
            <p class="venue-owner">
              管理者：{{ venue.ownerName || venue.ownerUsername || '-' }}
            </p>
            <p class="venue-time">
              提交时间：{{ formatDateTime(venue.createTime) }}
            </p>
            <p class="venue-desc" :title="venue.description">
              {{ truncateText(venue.description, 80) }}
            </p>

            <!-- Actions -->
            <div class="audit-actions">
              <el-button v-if="venue.status === 'PENDING'"
                type="success" size="small"
                :loading="actionLoading === venue.id"
                @click="handleApprove(venue)">通过</el-button>
              <el-button v-if="venue.status === 'PENDING'"
                type="danger" size="small"
                :loading="actionLoading === venue.id"
                @click="openRejectDialog(venue)">拒绝</el-button>
              <el-button type="primary" size="small" @click="openEditDialog(venue)">编辑</el-button>
            </div>

            <!-- Rejected: Show reason -->
            <div v-if="venue.status === 'REJECTED' && venue.rejectReason" class="reject-reason">
              <span class="reason-label">拒绝原因：</span>
              <span class="reason-text">{{ venue.rejectReason }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="total > pageSize" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @change="fetchVenueList"
        />
      </div>
    </div>

    <!-- Reject Dialog -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="拒绝审核"
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules">
        <el-form-item label="拒绝原因" prop="reason">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请填写拒绝原因..."
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejectLoading" @click="confirmReject">
          确认拒绝
        </el-button>
      </template>
    </el-dialog>

    <!-- Edit Venue Dialog -->
    <el-dialog v-model="editDialogVisible" title="编辑场馆信息" width="600px" :close-on-click-modal="false">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="场馆名称" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入场馆名称" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="editForm.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="editForm.phone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="营业时间">
          <div class="business-hours-picker">
            <el-time-picker
              v-model="editForm.openTime"
              format="HH:mm"
              value-format="HH:mm"
              placeholder="开门时间"
              style="width: 150px"
            />
            <span class="time-sep">—</span>
            <el-time-picker
              v-model="editForm.closeTime"
              format="HH:mm"
              value-format="HH:mm"
              placeholder="关门时间"
              style="width: 150px"
            />
          </div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="场馆介绍" />
        </el-form-item>
        <el-form-item label="设施">
          <el-input v-model="editForm.facilities" placeholder="逗号分隔，如：跑步机,游泳池" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleEditSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Picture, Location, Phone } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getVenueList, updateVenueStatus, updateVenue } from '@/api/venue'

const loading = ref(false)
const venueList = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const auditTab = ref('PENDING')
const actionLoading = ref(null)

const counts = reactive({
  pending: 0,
  approved: 0,
  rejected: 0
})

const rejectDialogVisible = ref(false)
const rejectTarget = ref(null)
const rejectLoading = ref(false)
const rejectFormRef = ref(null)
const rejectForm = reactive({ reason: '' })
const rejectRules = {
  reason: [{ required: true, message: '请填写拒绝原因', trigger: 'blur' }]
}

// Edit venue
const editDialogVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref(null)
const editForm = reactive({ id: null, name: '', address: '', phone: '', businessHours: '', openTime: null, closeTime: null, description: '', facilities: '' })
const editRules = {
  name: [{ required: true, message: '请输入场馆名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }]
}

const emptyText = computed(() => {
  const map = { PENDING: '暂无待审核场馆', APPROVED: '暂无已通过场馆', REJECTED: '暂无已拒绝场馆' }
  return map[auditTab.value] || '暂无数据'
})

function getStatusTagType(status) {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[status] || 'info'
}

function getStatusText(status) {
  const map = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }
  return map[status] || status
}

function truncateText(text, maxLen) {
  if (!text) return ''
  return text.length > maxLen ? text.slice(0, maxLen) + '...' : text
}

function formatDateTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

function handleTabChange() {
  page.value = 1
  fetchVenueList()
}

async function fetchVenueList() {
  loading.value = true
  try {
    const res = await getVenueList({
      page: page.value,
      pageSize: pageSize.value,
      status: auditTab.value
    })
    venueList.value = res.data?.records || res.data?.list || []
    total.value = res.data?.total || 0

    // Also fetch counts
    fetchCounts()
  } catch {
    ElMessage.error('加载场馆列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchCounts() {
  try {
    const [pendingRes, approvedRes, rejectedRes] = await Promise.allSettled([
      getVenueList({ page: 1, pageSize: 1, status: 'PENDING' }),
      getVenueList({ page: 1, pageSize: 1, status: 'APPROVED' }),
      getVenueList({ page: 1, pageSize: 1, status: 'REJECTED' })
    ])
    counts.pending = pendingRes.value?.data?.total || 0
    counts.approved = approvedRes.value?.data?.total || 0
    counts.rejected = rejectedRes.value?.data?.total || 0
  } catch {
    // silent
  }
}

async function handleApprove(venue) {
  actionLoading.value = venue.id
  try {
    await updateVenueStatus(venue.id, 'APPROVED')
    ElMessage.success('场馆已通过审核')
    fetchVenueList()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    actionLoading.value = null
  }
}

function openRejectDialog(venue) {
  rejectTarget.value = venue
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

async function confirmReject() {
  if (!rejectFormRef.value) return
  try {
    await rejectFormRef.value.validate()
  } catch {
    return
  }

  rejectLoading.value = true
  try {
    await updateVenueStatus(rejectTarget.value.id, 'REJECTED', rejectForm.reason)
    ElMessage.success('场馆已拒绝')
    rejectDialogVisible.value = false
    fetchVenueList()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    rejectLoading.value = false
  }
}

function openEditDialog(venue) {
  editForm.id = venue.id
  editForm.name = venue.name || ''
  editForm.address = venue.address || ''
  editForm.phone = venue.phone || ''
  const times = parseBusinessHours(venue.businessHours)
  editForm.openTime = times[0]
  editForm.closeTime = times[1]
  editForm.businessHours = venue.businessHours || ''
  editForm.description = venue.description || ''
  editForm.facilities = venue.facilities ? (Array.isArray(venue.facilities) ? venue.facilities.join(',') : venue.facilities) : ''
  editDialogVisible.value = true
}

function parseBusinessHours(businessHours) {
  if (!businessHours || typeof businessHours !== 'string') return [null, null]
  const parts = businessHours.split('-')
  if (parts.length === 2) return [parts[0].trim(), parts[1].trim()]
  return [null, null]
}

function combineBusinessHours(form) {
  if (form.openTime && form.closeTime) return form.openTime + '-' + form.closeTime
  return form.businessHours || ''
}

async function handleEditSave() {
  if (!editFormRef.value) return
  try { await editFormRef.value.validate() } catch { return }
  editLoading.value = true
  try {
    const data = {
      name: editForm.name,
      address: editForm.address,
      phone: editForm.phone,
      businessHours: combineBusinessHours(editForm),
      description: editForm.description,
      facilities: editForm.facilities
    }
    await updateVenue(editForm.id, data)
    ElMessage.success('场馆信息已更新')
    editDialogVisible.value = false
    fetchVenueList()
  } catch {
    ElMessage.error('更新失败')
  } finally {
    editLoading.value = false
  }
}

onMounted(() => {
  fetchVenueList()
})
</script>

<style scoped>
.business-hours-picker {
  display: flex;
  align-items: center;
  gap: 8px;
}
.time-sep {
  color: #909399;
  font-weight: 500;
}
.venue-audit-page {
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

.tabs-bar {
  margin-bottom: 24px;
}

.tab-badge {
  margin-right: 0;
}

.venue-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  min-height: 400px;
}

.venue-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  overflow: hidden;
  transition: border-color 0.3s, box-shadow 0.3s;
}

.venue-card:hover {
  border-color: #667eea;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.venue-cover {
  height: 160px;
  position: relative;
  background: #f5f7fa;
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  background: #f0f0f0;
}

.status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
}

.venue-info {
  padding: 16px 20px 20px;
}

.venue-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 10px;
}

.venue-address,
.venue-phone,
.venue-owner,
.venue-time {
  font-size: 13px;
  color: #909399;
  margin: 0 0 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.venue-desc {
  font-size: 13px;
  color: #606266;
  margin: 8px 0 12px;
  line-height: 1.5;
}

.audit-actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.reject-reason {
  margin-top: 12px;
  padding: 10px 12px;
  background: #fef0f0;
  border-radius: 6px;
  border-left: 3px solid #f56c6c;
}

.reason-label {
  font-size: 13px;
  color: #f56c6c;
  font-weight: 500;
}

.reason-text {
  font-size: 13px;
  color: #e6a23c;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .venue-grid {
    grid-template-columns: 1fr;
  }
}
</style>
