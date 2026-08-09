<template>
  <div class="coach-manage-page">
    <div class="manage-container">
      <!-- Header -->
      <div class="page-header">
        <h2 class="page-title">教练管理</h2>
        <el-button type="primary" :icon="Plus" @click="openAddDialog">
          添加教练
        </el-button>
      </div>

      <!-- Coach Cards Grid -->
      <div v-loading="loading" class="coach-grid">
        <el-empty v-if="!loading && coachList.length === 0" description="暂无教练" />

        <div
          v-for="coach in coachList"
          :key="coach.id"
          class="coach-card"
        >
          <div class="card-body">
            <el-avatar :size="72" :src="coach.avatar" class="coach-avatar" />
            <h4 class="coach-name">{{ coach.name }}</h4>
            <div class="coach-tags">
              <el-tag
                v-for="tag in parseTags(coach.specialty)"
                :key="tag"
                size="small"
                type="primary"
                class="specialty-tag"
              >
                {{ tag }}
              </el-tag>
              <el-tag v-if="!coach.specialty" size="small" type="info">暂无标签</el-tag>
            </div>
            <div class="coach-rating">
              <el-rate
                :model-value="coach.rating || 0"
                disabled
                show-score
                text-color="#ff9900"
                size="small"
              />
            </div>
            <div class="coach-actions">
              <el-button size="small" type="primary" plain @click="openEditDialog(coach)">
                编辑
              </el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(coach)">
                删除
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑教练' : '添加教练'"
      width="550px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入教练姓名" />
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="年龄" prop="age">
          <el-input-number
            v-model="form.age"
            :min="18"
            :max="80"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="所属场馆" prop="venueId">
          <el-select v-model="form.venueId" placeholder="请选择所属场馆" style="width:100%">
            <el-option v-for="v in venueList" :key="v.id" :label="v.name" :value="v.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="从业经验" prop="experience">
          <el-input
            v-model="form.experience"
            placeholder="如：5年健身教练经验"
          />
        </el-form-item>

        <el-form-item label="专长领域" prop="specialty">
          <el-input
            v-model="form.specialty"
            placeholder="多个专长用逗号分隔，如：瑜伽,普拉提,体态矫正"
          />
        </el-form-item>

        <el-form-item label="资质证书">
          <el-input
            v-model="form.certifications"
            type="textarea"
            :rows="2"
            placeholder="如：ACE认证, NASM-CPT"
          />
        </el-form-item>

        <el-form-item label="个人简介">
          <el-input
            v-model="form.introduction"
            type="textarea"
            :rows="3"
            placeholder="教练个人简介"
          />
        </el-form-item>

        <el-form-item label="头像URL">
          <el-input v-model="form.avatar" placeholder="请输入头像图片链接" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          {{ isEdit ? '更新' : '添加' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Delete Confirm Dialog -->
    <el-dialog v-model="deleteDialogVisible" title="确认删除" width="400px">
      <p>确定要删除教练 "{{ deleteTarget?.name }}" 吗？此操作不可恢复。</p>
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
import { getCoachList, createCoach, updateCoach, deleteCoach } from '@/api/user'
import { getVenueList } from '@/api/venue'

const loading = ref(false)
const coachList = ref([])
const venueList = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitLoading = ref(false)
const formRef = ref(null)

const deleteDialogVisible = ref(false)
const deleteTarget = ref(null)
const deleteLoading = ref(false)

const form = reactive({
  name: '',
  gender: 1,
  age: null,
  experience: '',
  specialty: '',
  certifications: '',
  introduction: '',
  avatar: '',
  venueId: null
})

const rules = {
  name: [{ required: true, message: '请输入教练姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  age: [{ required: true, message: '请输入年龄', trigger: 'blur' }],
  experience: [{ required: true, message: '请输入从业经验', trigger: 'blur' }],
  specialty: [{ required: true, message: '请输入专长领域', trigger: 'blur' }]
}

function parseTags(str) {
  if (!str) return []
  if (Array.isArray(str)) return str
  return str.split(',').map(t => t.trim()).filter(Boolean)
}

async function fetchCoachList() {
  loading.value = true
  try {
    const res = await getCoachList()
    coachList.value = res.data?.records || res.data?.list || res.data || []
  } catch {
    ElMessage.error('加载教练列表失败')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.name = ''
  form.gender = 1
  form.age = null
  form.experience = ''
  form.specialty = ''
  form.certifications = ''
  form.introduction = ''
  form.avatar = ''
  form.venueId = null
}

function openAddDialog() {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(coach) {
  isEdit.value = true
  editId.value = coach.id
  form.name = coach.name || ''
  form.gender = coach.gender === 1 ? 1 : coach.gender === 2 ? 2 : (coach.gender === '男' ? 1 : 2)
  form.age = coach.age || null
  form.experience = coach.experience || ''
  form.specialty = Array.isArray(coach.specialty)
    ? coach.specialty.join(',')
    : (coach.specialty || '')
  form.certifications = coach.certifications || ''
  form.introduction = coach.introduction || ''
  form.avatar = coach.avatar || ''
  form.venueId = coach.venueId || null
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
    name: form.name,
    gender: form.gender,
    age: form.age,
    experience: form.experience,
    specialty: form.specialty,
    certInfo: form.certifications,
    introduction: form.introduction,
    avatar: form.avatar,
    venueId: form.venueId
  }

  try {
    if (isEdit.value) {
      await updateCoach(editId.value, payload)
      ElMessage.success('教练信息更新成功')
    } else {
      await createCoach(payload)
      ElMessage.success('教练添加成功')
    }
    dialogVisible.value = false
    fetchCoachList()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

function handleDelete(coach) {
  deleteTarget.value = coach
  deleteDialogVisible.value = true
}

async function confirmDelete() {
  deleteLoading.value = true
  try {
    await deleteCoach(deleteTarget.value.id)
    ElMessage.success('教练已删除')
    deleteDialogVisible.value = false
    fetchCoachList()
  } catch {
    ElMessage.error('删除失败')
  } finally {
    deleteLoading.value = false
  }
}

async function fetchVenues() {
  try {
    const res = await getVenueList({ page: 1, size: 100 })
    venueList.value = res.data?.records || []
  } catch { venueList.value = [] }
}

onMounted(() => {
  fetchCoachList()
  fetchVenues()
})
</script>

<style scoped>
.coach-manage-page {
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
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}

.coach-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  min-height: 400px;
}

.coach-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  overflow: hidden;
  transition: border-color 0.3s, box-shadow 0.3s;
}

.coach-card:hover {
  border-color: #667eea;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.card-body {
  padding: 24px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.coach-avatar {
  margin-bottom: 12px;
}

.coach-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px;
}

.coach-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
  margin-bottom: 12px;
  min-height: 24px;
}

.specialty-tag {
  margin: 0;
}

.coach-rating {
  margin-bottom: 16px;
}

.coach-actions {
  display: flex;
  gap: 12px;
}

@media (max-width: 900px) {
  .coach-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .coach-grid {
    grid-template-columns: 1fr;
  }
}
</style>
