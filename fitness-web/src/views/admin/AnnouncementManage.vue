<template>
  <div class="announcement-manage-page">
    <div class="manage-container">
      <div class="page-header">
        <h2 class="page-title">公告管理</h2>
        <el-button type="primary" :icon="Plus" @click="openAddDialog">
          发布公告
        </el-button>
      </div>

      <!-- Announcement Table -->
      <div class="table-card">
        <el-table
          v-loading="loading"
          :data="announcementList"
          border
          stripe
          style="width: 100%"
          empty-text="暂无公告"
        >
          <el-table-column prop="title" label="标题" min-width="200" />
          <el-table-column label="内容预览" min-width="300">
            <template #default="{ row }">
              <span class="content-preview" :title="row.content">
                {{ truncateText(row.content, 80) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag
                :type="row.status === 1 ? 'success' : 'info'"
                size="small"
              >
                {{ row.status === 1 ? '显示中' : '已隐藏' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="发布时间" min-width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="160" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" size="small" @click="openEditDialog(row)">
                编辑
              </el-button>
              <el-button text type="danger" size="small" @click="handleDelete(row)">
                删除
              </el-button>
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
            @change="fetchAnnouncementList"
          />
        </div>
      </div>
    </div>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑公告' : '发布公告'"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item label="公告标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入公告标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="公告内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            placeholder="请输入公告内容..."
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="发布状态">
          <el-switch
            v-model="form.statusActive"
            active-text="显示"
            inactive-text="隐藏"
            :active-value="true"
            :inactive-value="false"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          {{ isEdit ? '更新' : '发布' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Delete Confirm Dialog -->
    <el-dialog v-model="deleteDialogVisible" title="确认删除" width="400px">
      <p>确定要删除公告 "{{ deleteTarget?.title }}" 吗？此操作不可恢复。</p>
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
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getAnnouncementList,
  createAnnouncement,
  updateAnnouncement,
  deleteAnnouncement
} from '@/api/announcement'

const loading = ref(false)
const announcementList = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitLoading = ref(false)
const formRef = ref(null)

const deleteDialogVisible = ref(false)
const deleteTarget = ref(null)
const deleteLoading = ref(false)

const form = reactive({
  title: '',
  content: '',
  statusActive: true
})

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

function truncateText(text, maxLen) {
  if (!text) return ''
  return text.length > maxLen ? text.slice(0, maxLen) + '...' : text
}

function formatDateTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

async function fetchAnnouncementList() {
  loading.value = true
  try {
    const res = await getAnnouncementList({
      page: page.value,
      pageSize: pageSize.value
    })
    announcementList.value = res.data?.records || res.data?.list || []
    total.value = res.data?.total || 0
  } catch {
    ElMessage.error('加载公告列表失败')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.title = ''
  form.content = ''
  form.statusActive = true
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
  form.title = row.title || ''
  form.content = row.content || ''
  form.statusActive = row.status === 1 || row.status === 1
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
  const payload = {
    title: form.title,
    content: form.content,
    status: form.statusActive ? 1 : 0
  }

  try {
    if (isEdit.value) {
      await updateAnnouncement(editId.value, payload)
      ElMessage.success('公告更新成功')
    } else {
      await createAnnouncement(payload)
      ElMessage.success('公告发布成功')
    }
    dialogVisible.value = false
    fetchAnnouncementList()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

function handleDelete(row) {
  deleteTarget.value = row
  deleteDialogVisible.value = true
}

async function confirmDelete() {
  deleteLoading.value = true
  try {
    await deleteAnnouncement(deleteTarget.value.id)
    ElMessage.success('公告已删除')
    deleteDialogVisible.value = false
    fetchAnnouncementList()
  } catch {
    ElMessage.error('删除失败')
  } finally {
    deleteLoading.value = false
  }
}

onMounted(() => {
  fetchAnnouncementList()
})
</script>

<style scoped>
.announcement-manage-page {
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

.table-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 24px;
}

.content-preview {
  color: #606266;
  cursor: default;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
