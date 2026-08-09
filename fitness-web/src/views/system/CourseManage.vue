<template>
  <div class="course-manage-page">
    <div class="manage-container">
      <!-- Header -->
      <div class="page-header">
        <h2 class="page-title">课程管理</h2>
        <el-button type="primary" :icon="Plus" @click="openAddDialog">
          添加课程
        </el-button>
      </div>

      <!-- Tabs -->
      <el-tabs v-model="publishFilter" @tab-change="onFilterChange" class="filter-tabs">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="已上架" name="1" />
        <el-tab-pane label="已下架" name="0" />
      </el-tabs>

      <!-- Table -->
      <div class="table-card">
        <el-table
          v-loading="loading"
          :data="courseList"
          border
          stripe
          style="width: 100%"
          empty-text="暂无课程"
        >
          <el-table-column prop="name" label="课程名称" min-width="150" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="getTypeTagType(row.type)" size="small">
                {{ typeMap[row.type] || row.type }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="难度" width="80">
            <template #default="{ row }">
              <el-tag :type="getDifficultyTagType(row.difficulty)" size="small">
                {{ diffMap[row.difficulty] || row.difficulty }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="上课时间" min-width="180">
            <template #default="{ row }">
              {{ formatClassTime(row.startTime, row.endTime) }}
            </template>
          </el-table-column>
          <el-table-column label="报名人数" width="110">
            <template #default="{ row }">
              {{ row.enrolled }}/{{ row.maxStudents }}
            </template>
          </el-table-column>
          <el-table-column label="重复" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.recurrence === 'WEEKLY'" type="success" size="small">每周</el-tag>
              <span v-else style="color:#c0c4cc">-</span>
            </template>
          </el-table-column>
          <el-table-column label="上架状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.published === 0 ? 'info' : 'success'" size="small">
                {{ row.published === 0 ? '已下架' : '已上架' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ statusMap[row.status] || row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button text size="small"
                :type="row.recurrence === 'WEEKLY' ? 'warning' : 'success'"
                @click="toggleRecurrence(row)">
                {{ row.recurrence === 'WEEKLY' ? '取消重复' : '每周重复' }}
              </el-button>
              <el-button text size="small"
                :type="row.published === 0 ? 'success' : 'warning'"
                @click="handleTogglePublished(row)">
                {{ row.published === 0 ? '上架' : '下架' }}
              </el-button>
              <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- Pagination -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @change="fetchCourseList"
          />
        </div>
      </div>
    </div>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑课程' : '添加课程'"
      width="650px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入课程名称" maxlength="50" />
        </el-form-item>

        <el-form-item label="课程描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入课程描述"
          />
        </el-form-item>

        <el-form-item label="所属场馆" prop="venueId">
          <el-select v-model="form.venueId" placeholder="请选择场馆" style="width: 100%">
            <el-option
              v-for="venue in venueList"
              :key="venue.id"
              :label="venue.name"
              :value="venue.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="课程类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择课程类型" style="width: 100%">
            <el-option label="瑜伽" value="YOGA" />
            <el-option label="力量训练" value="STRENGTH" />
            <el-option label="有氧运动" value="CARDIO" />
            <el-option label="舞蹈" value="DANCE" />
            <el-option label="游泳" value="SWIMMING" />
            <el-option label="搏击" value="BOXING" />
            <el-option label="普拉提" value="PILATES" />
            <el-option label="动感单车" value="SPINNING" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>

        <el-form-item label="难度等级" prop="difficulty">
          <el-select v-model="form.difficulty" placeholder="请选择难度" style="width: 100%">
            <el-option label="初级" value="BEGINNER" />
            <el-option label="中级" value="INTERMEDIATE" />
            <el-option label="高级" value="ADVANCED" />
          </el-select>
        </el-form-item>

        <el-form-item label="最大人数" prop="maxStudents">
          <el-input-number
            v-model="form.maxStudents"
            :min="1"
            :max="500"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="授课教练" prop="coachId">
          <el-select v-model="form.coachId" placeholder="请选择教练" style="width: 100%">
            <el-option
              v-for="coach in coachList"
              :key="coach.id"
              :label="coach.name"
              :value="coach.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="课程价格" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0"
            :precision="2"
            style="width: 100%"
          >
            <template #suffix>元</template>
          </el-input-number>
        </el-form-item>

        <el-form-item label="上课时间" prop="classTime">
          <el-date-picker
            v-model="form.classTime"
            type="datetimerange"
            range-separator="~"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="M月D日 HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="课程标签">
          <el-input v-model="form.tags" placeholder="多个标签用逗号分隔" />
        </el-form-item>

        <el-form-item label="重复设置">
          <el-radio-group v-model="form.recurrence">
            <el-radio value="">不重复</el-radio>
            <el-radio value="WEEKLY">每周重复</el-radio>
          </el-radio-group>
          <div style="font-size:12px;color:#909399;margin-top:4px">设为每周重复后，系统自动生成未来4周的课程</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          {{ isEdit ? '更新' : '添加' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 重新上架：修改日期对话框 -->
    <el-dialog
      v-model="republishDateDialog.visible"
      title="修改上课日期后重新上架"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="republish-hint">
        <el-icon color="#e6a23c"><WarningFilled /></el-icon>
        <span>该课程原定上课时间已过，请设置新的上课日期后再上架。</span>
      </div>
      <el-form :model="republishDateDialog" label-width="90px" style="margin-top: 16px">
        <el-form-item label="课程名称">
          <span class="republish-course-name">{{ republishDateDialog.courseName }}</span>
        </el-form-item>
        <el-form-item label="原定时间">
          <span class="republish-old-time">{{ republishDateDialog.oldTime }}</span>
        </el-form-item>
        <el-form-item label="新上课时间" required>
          <el-date-picker
            v-model="republishDateDialog.classTime"
            type="datetimerange"
            range-separator="~"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="M月D日 HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="republishDateDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="republishDateDialog.loading" @click="handleRepublishWithDates">
          更新日期并上架
        </el-button>
      </template>
    </el-dialog>

    <!-- Delete Confirm Dialog -->
    <el-dialog v-model="deleteDialogVisible" title="确认删除" width="400px">
      <p>确定要删除课程 "{{ deleteTarget?.name }}" 吗？此操作不可恢复。</p>
      <template #footer>
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="deleteLoading" @click="confirmDelete">
          确认删除
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import { getCourseList, createCourse, updateCourse, deleteCourse, setCourseRecurrence, toggleCoursePublished } from '@/api/course'
import { getCoachList } from '@/api/user'
import { getVenueList } from '@/api/venue'

const loading = ref(false)
const courseList = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const publishFilter = ref('')

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitLoading = ref(false)
const formRef = ref(null)
const coachList = ref([])
const venueList = ref([])

const deleteDialogVisible = ref(false)
const deleteTarget = ref(null)
const deleteLoading = ref(false)

// 重新上架日期修改对话框
const republishDateDialog = reactive({
  visible: false,
  loading: false,
  courseId: null,
  courseName: '',
  oldTime: '',
  classTime: null
})

const form = reactive({
  name: '',
  description: '',
  type: '',
  difficulty: '',
  maxStudents: 20,
  coachId: '',
  price: 0,
  classTime: null,
  tags: '',
  recurrence: '',
  venueId: null
})

const rules = {
  name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  venueId: [{ required: true, message: '请选择场馆', trigger: 'change' }],
  type: [{ required: true, message: '请选择课程类型', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度等级', trigger: 'change' }],
  maxStudents: [{ required: true, message: '请输入最大人数', trigger: 'blur' }],
  price: [{ required: true, message: '请输入课程价格', trigger: 'blur' }],
  classTime: [
    { required: true, message: '请选择上课时间', trigger: 'change' },
    { validator: (rule, value, callback) => {
        if (value && value.length === 2) {
          const start = new Date(value[0])
          const end = new Date(value[1])
          if (start.getTime() <= Date.now()) {
            callback(new Error('开始时间不能是已过的时间'))
            return
          }
          if (end.getTime() <= start.getTime()) {
            callback(new Error('结束时间必须晚于开始时间'))
            return
          }
        }
        callback()
      }, trigger: 'change' }
  ]
}

// 中英文映射
const typeMap = { YOGA: '瑜伽', STRENGTH: '力量训练', CARDIO: '有氧运动', DANCE: '舞蹈', SWIMMING: '游泳', BASKETBALL: '篮球', BOXING: '搏击', PILATES: '普拉提', SPINNING: '动感单车', OTHER: '其他' }
const diffMap = { BEGINNER: '初级', INTERMEDIATE: '中级', ADVANCED: '高级' }
const statusMap = { UPCOMING: '即将开始', ONGOING: '进行中', FINISHED: '已结束', CANCELLED: '已取消' }
function statusTagType(s) { return s === 'UPCOMING' ? 'warning' : s === 'ONGOING' ? 'success' : s === 'CANCELLED' ? 'danger' : 'info' }

const typeTagTypeMap = {
  '瑜伽': 'success',
  '力量训练': 'danger',
  '有氧运动': 'warning',
  'HIIT': 'danger',
  '普拉提': 'primary',
  '搏击': 'warning',
  '舞蹈': 'primary',
  '其他': 'info'
}

const difficultyTagTypeMap = {
  '初级': 'success',
  '中级': 'warning',
  '高级': 'danger'
}

function getTypeTagType(type) {
  return typeTagTypeMap[type] || typeTagTypeMap[typeMap[type]] || 'info'
}

function getDifficultyTagType(difficulty) {
  return difficultyTagTypeMap[difficulty] || difficultyTagTypeMap[diffMap[difficulty]] || 'info'
}

function pad(n) { return String(n).padStart(2, '0') }
function formatClassTime(start, end) {
  if (!start || !end) return '-'
  const s = new Date(start), e = new Date(end)
  return `${s.getMonth() + 1}月${s.getDate()}日 ${pad(s.getHours())}:${pad(s.getMinutes())}~${pad(e.getHours())}:${pad(e.getMinutes())}`
}

async function fetchCourseList() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    // 全部 → 不传 published（后端会默认过滤已上架），这里传 published=-1 表示查全部
    params.published = publishFilter.value === '' ? -1 : Number(publishFilter.value)
    const res = await getCourseList(params)
    courseList.value = res.data.records || res.data.list || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '加载课程列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchCoachList(venueId) {
  try {
    const params = {}
    if (venueId) params.venueId = venueId
    const res = await getCoachList(params)
    coachList.value = res.data?.records || res.data?.list || res.data || []
  } catch {
    coachList.value = []
  }
}

function resetForm() {
  form.name = ''
  form.description = ''
  form.type = ''
  form.difficulty = ''
  form.maxStudents = 20
  form.coachId = ''
  form.price = 0
  form.classTime = null
  form.tags = ''
  form.recurrence = ''
  form.venueId = null
}

function openAddDialog() {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  form.name = row.name || ''
  form.description = row.description || ''
  form.type = row.type || ''
  form.difficulty = row.difficulty || ''
  form.maxStudents = row.maxStudents || 20
  form.coachId = row.coachId || ''
  form.price = row.price || 0
  form.classTime = (row.startTime && row.endTime) ? [row.startTime, row.endTime] : null
  form.venueId = row.venueId || null
  form.tags = Array.isArray(row.tags) ? row.tags.join(',') : (row.tags || '')
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  const [startTime, endTime] = form.classTime || []
  const { classTime, ...rest } = form
  const payload = {
    ...rest,
    startTime: startTime || '',
    endTime: endTime || '',
    tags: form.tags ? form.tags.split(',').map(t => t.trim()).filter(Boolean).join(',') : ''
  }

  try {
    const res = isEdit.value
      ? await updateCourse(editId.value, payload)
      : await createCourse(payload)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '课程更新成功' : '课程添加成功')
      dialogVisible.value = false
      fetchCourseList()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

function onFilterChange() {
  page.value = 1
  fetchCourseList()
}

function isCourseExpired(row) {
  if (!row.endTime) return false
  return new Date(row.endTime).getTime() < Date.now()
}

async function handleTogglePublished(row) {
  // 已下架 + 时间已过 → 弹出日期修改对话框
  if (row.published === 0 && isCourseExpired(row)) {
    republishDateDialog.visible = true
    republishDateDialog.courseId = row.id
    republishDateDialog.courseName = row.name
    republishDateDialog.oldTime = formatClassTime(row.startTime, row.endTime)
    republishDateDialog.classTime = null
    republishDateDialog.row = row  // 保存完整行数据，供更新时复用
    return
  }

  try {
    const res = await toggleCoursePublished(row.id)
    if (res.code === 200) {
      row.published = row.published === 0 ? 1 : 0
      ElMessage.success(res.message || (row.published === 1 ? '已上架' : '已下架'))
      // 上架后如果当前在"已下架"标签页，切换到"全部"以免看不到
      if (row.published === 1 && publishFilter.value === '0') {
        publishFilter.value = ''
      }
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) { ElMessage.error(error?.response?.data?.message || error?.message || '操作失败') }
}

async function handleRepublishWithDates() {
  const { courseId, classTime, row } = republishDateDialog
  if (!classTime || classTime.length !== 2) {
    ElMessage.warning('请选择新的上课时间')
    return
  }
  const [startTime, endTime] = classTime
  if (new Date(startTime).getTime() <= Date.now()) {
    ElMessage.warning('开始时间不能是已过的时间')
    return
  }
  if (new Date(endTime).getTime() <= new Date(startTime).getTime()) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }

  republishDateDialog.loading = true
  try {
    // 使用原课程完整数据，只更新日期，避免 @Valid 校验失败
    const payload = {
      name: row.name,
      type: row.type,
      venueId: row.venueId,
      maxStudents: row.maxStudents ?? 20,
      startTime,
      endTime,
      description: row.description || '',
      difficulty: row.difficulty || '',
      coachId: row.coachId ?? null,
      price: row.price ?? 0,
      tags: row.tags || '',
      recurrence: row.recurrence || ''
    }
    console.log('Republish payload:', JSON.stringify(payload))
    const updateRes = await updateCourse(courseId, payload)
    console.log('Update response:', updateRes)
    if (updateRes.code !== 200) {
      ElMessage.error(updateRes.message || '更新日期失败')
      republishDateDialog.loading = false
      return
    }
    // 再上架
    const publishRes = await toggleCoursePublished(courseId)
    console.log('Publish response:', publishRes)
    if (publishRes.code === 200) {
      ElMessage.success('日期已更新，课程已重新上架')
      republishDateDialog.visible = false
      // 上架成功后如果当前在"已下架"标签页，切换到"全部"
      if (publishFilter.value === '0') publishFilter.value = ''
      fetchCourseList()
    } else {
      ElMessage.error(publishRes.message || '上架失败')
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '操作失败')
  } finally {
    republishDateDialog.loading = false
  }
}

async function toggleRecurrence(row) {
  const newVal = row.recurrence === 'WEEKLY' ? '' : 'WEEKLY'
  try {
    const res = await setCourseRecurrence(row.id, newVal)
    if (res.code === 200) {
      row.recurrence = newVal
      ElMessage.success(newVal ? '已设为每周重复' : '已取消每周重复')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) { ElMessage.error(error?.response?.data?.message || error?.message || '操作失败') }
}

function handleDelete(row) {
  deleteTarget.value = row
  deleteDialogVisible.value = true
}

async function confirmDelete() {
  deleteLoading.value = true
  try {
    const res = await deleteCourse(deleteTarget.value.id)
    if (res.code === 200) {
      ElMessage.success('课程已删除')
      deleteDialogVisible.value = false
      fetchCourseList()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '删除失败')
  } finally {
    deleteLoading.value = false
  }
}

async function fetchVenueList() {
  try {
    const res = await getVenueList({ page: 1, size: 100 })
    venueList.value = res.data?.records || res.data?.list || res.data || []
  } catch { venueList.value = [] }
}

// 切换场馆时重新加载该场馆的教练
watch(() => form.venueId, (newVal) => {
  form.coachId = ''
  fetchCoachList(newVal)
})

onMounted(() => {
  fetchCourseList()
  fetchCoachList()
  fetchVenueList()
})
</script>

<style scoped>
.course-manage-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.manage-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}

.filter-tabs {
  margin-bottom: -1px;
}

.filter-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.filter-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
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

/* 重新上架日期对话框 */
.republish-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 6px;
  color: #b88230;
  font-size: 14px;
  line-height: 1.5;
}

.republish-course-name {
  font-weight: 600;
  color: #303133;
}

.republish-old-time {
  color: #909399;
  font-size: 13px;
}
</style>
