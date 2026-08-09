<template>
  <div class="profile-page">
    <!-- 左侧个人信息卡片 -->
    <div class="profile-sidebar">
      <div class="profile-info-card">
        <div class="avatar-section">
          <el-avatar :size="100" :icon="UserFilled" :src="profile.avatar" />
          <h3 class="profile-nickname">{{ profile.nickname || '未设置昵称' }}</h3>
          <el-tag
            :type="userStore.isAdmin ? 'danger' : 'primary'"
            effect="dark"
            size="small"
          >
            {{ roleLabel }}
          </el-tag>
        </div>

        <el-divider />

        <div class="profile-meta">
          <div class="meta-item">
            <el-icon><Calendar /></el-icon>
            <span>加入时间：{{ profile.createTime || '--' }}</span>
          </div>
        </div>

        <el-divider />

        <div class="profile-stats">
          <div class="stat-item" @click="$router.push('/user/orders')">
            <span class="stat-value">{{ stats.orderCount }}</span>
            <span class="stat-label">我的预约</span>
          </div>
          <div class="stat-item" @click="$router.push('/user/checkins')">
            <span class="stat-value">{{ stats.checkinCount }}</span>
            <span class="stat-label">打卡次数</span>
          </div>
          <div class="stat-item" @click="$router.push('/user/favorites')">
            <span class="stat-value">{{ stats.favoriteCount }}</span>
            <span class="stat-label">我的收藏</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧编辑区 -->
    <div class="profile-main">
      <el-tabs v-model="activeTab">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="basic">
          <el-form
            ref="basicFormRef"
            :model="basicForm"
            :rules="basicRules"
            label-width="100px"
            label-position="right"
            class="profile-form"
          >
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="昵称" prop="nickname">
                  <el-input v-model="basicForm.nickname" placeholder="请输入昵称" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="basicForm.phone" placeholder="请输入手机号" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="basicForm.email" placeholder="请输入邮箱" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="性别">
                  <el-radio-group v-model="basicForm.gender">
                    <el-radio value="MALE">男</el-radio>
                    <el-radio value="FEMALE">女</el-radio>
                    <el-radio value="OTHER">其他</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="身高(cm)">
                  <el-input-number
                    v-model="basicForm.height"
                    :min="100"
                    :max="250"
                    :step="1"
                    placeholder="身高"
                    controls-position="right"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="体重(kg)">
                  <el-input-number
                    v-model="basicForm.weight"
                    :min="30"
                    :max="200"
                    :step="0.1"
                    :precision="1"
                    placeholder="体重"
                    controls-position="right"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="年龄">
                  <el-input-number
                    v-model="basicForm.age"
                    :min="10"
                    :max="100"
                    :step="1"
                    placeholder="年龄"
                    controls-position="right"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="健身目标">
                  <el-select v-model="basicForm.fitnessGoal" placeholder="请选择健身目标" style="width: 100%">
                    <el-option label="减脂" value="LOSE_WEIGHT" />
                    <el-option label="增肌" value="BUILD_MUSCLE" />
                    <el-option label="塑形" value="SHAPE" />
                    <el-option label="增重" value="GAIN_WEIGHT" />
                    <el-option label="保持健康" value="KEEP_HEALTHY" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="训练频率">
                  <el-select v-model="basicForm.exerciseFrequency" placeholder="请选择训练频率" style="width: 100%">
                    <el-option label="每天" value="DAILY" />
                    <el-option label="每周五次" value="FIVE_TIMES_WEEK" />
                    <el-option label="隔天" value="EVERY_OTHER_DAY" />
                    <el-option label="每周两次" value="TWICE_WEEK" />
                    <el-option label="每周一次" value="WEEKLY" />
                    <el-option label="偶尔" value="RARELY" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item>
              <el-button
                type="primary"
                :loading="basicSaving"
                class="save-btn"
                @click="handleSaveBasic"
              >
                保存修改
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 修改密码 -->
        <el-tab-pane label="修改密码" name="password">
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="120px"
            label-position="right"
            class="profile-form password-form"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                placeholder="请输入原密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="请输入新密码（6-20位）"
                show-password
              />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请确认新密码"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="passwordSaving"
                class="save-btn"
                @click="handleChangePassword"
              >
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UserFilled, Calendar } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getUserProfile, updateUserProfile, changePassword } from '@/api/user'
import { useRouter } from 'vue-router'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('basic')
const basicSaving = ref(false)
const passwordSaving = ref(false)
const basicFormRef = ref(null)
const passwordFormRef = ref(null)

const profile = ref({})
const stats = ref({
  orderCount: 0,
  checkinCount: 0,
  favoriteCount: 0
})

const roleLabel = computed(() => {
  if (userStore.isAdmin) return '管理员'
  return '普通用户'
})

const basicForm = reactive({
  nickname: '',
  phone: '',
  email: '',
  gender: 'MALE',
  height: 170,
  weight: 65,
  age: 25,
  fitnessGoal: '',
  exerciseFrequency: ''
})

const basicRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateNewPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请输入新密码'))
  } else if (value.length < 6 || value.length > 20) {
    callback(new Error('密码长度在6到20个字符'))
  } else {
    callback()
  }
}

const validateConfirmPwd = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请确认新密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ validator: validateNewPassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPwd, trigger: 'blur' }]
}

onMounted(() => {
  loadProfile()
})

async function loadProfile() {
  try {
    const res = await getUserProfile()
    if (res.code === 200) {
      const data = res.data
      profile.value = data

      // 填充表单
      basicForm.nickname = data.nickname || ''
      basicForm.phone = data.phone || ''
      basicForm.email = data.email || ''
      basicForm.gender = data.gender === 1 ? 'MALE' : data.gender === 2 ? 'FEMALE' : 'OTHER'
      basicForm.height = data.height || 170
      basicForm.weight = data.weight || 65
      basicForm.age = data.age || 25
      basicForm.fitnessGoal = data.fitnessGoal || ''
      basicForm.exerciseFrequency = data.exerciseFrequency || ''

      // 填充统计
      stats.value.orderCount = data.orderCount || 0
      stats.value.checkinCount = data.checkinCount || 0
      stats.value.favoriteCount = data.favoriteCount || 0
    }
  } catch (e) {
    console.error('获取用户信息失败', e)
  }
}

async function handleSaveBasic() {
  const valid = await basicFormRef.value.validate().catch(() => false)
  if (!valid) return

  basicSaving.value = true
  try {
    // 映射前端值到后端格式
    const genderMap = { 'MALE': 1, 'FEMALE': 2, 'OTHER': 0 }
    const goalMap = { 'WEIGHT_LOSS': 'LOSE_WEIGHT', 'MUSCLE_GAIN': 'BUILD_MUSCLE', 'FITNESS': 'SHAPE', 'FLEXIBILITY': 'SHAPE', 'HEALTH': 'KEEP_HEALTHY', 'STRESS_RELIEF': 'KEEP_HEALTHY' }
    const freqMap = { 'RARELY': 'RARELY', 'LOW': 'TWICE_WEEK', 'MEDIUM': 'EVERY_OTHER_DAY', 'HIGH': 'FIVE_TIMES_WEEK', 'VERY_HIGH': 'DAILY' }

    const submitData = {
      nickname: basicForm.nickname,
      phone: basicForm.phone,
      email: basicForm.email,
      gender: genderMap[basicForm.gender] || 0,
      height: basicForm.height,
      weight: basicForm.weight,
      age: basicForm.age,
      fitnessGoal: goalMap[basicForm.fitnessGoal] || basicForm.fitnessGoal,
      exerciseFrequency: freqMap[basicForm.exerciseFrequency] || basicForm.exerciseFrequency
    }

    const res = await updateUserProfile(submitData)
    if (res.code === 200) {
      ElMessage.success('个人信息更新成功')
      // 更新store中的用户信息
      userStore.updateUser(basicForm)
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } catch (e) {
    ElMessage.error('更新失败，请稍后重试')
  } finally {
    basicSaving.value = false
  }
}

async function handleChangePassword() {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  passwordSaving.value = true
  try {
    const res = await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    if (res.code === 200) {
      ElMessage.success('密码修改成功，请重新登录')
      passwordFormRef.value.resetFields()
      // 退出登录
      setTimeout(() => {
        userStore.logout()
        router.push('/login')
      }, 1500)
    } else {
      ElMessage.error(res.message || '密码修改失败')
    }
  } catch (e) {
    ElMessage.error('密码修改失败，请稍后重试')
  } finally {
    passwordSaving.value = false
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 24px;
  padding: 8px 0;
}

/* 左侧卡片 */
.profile-sidebar {
  position: sticky;
  top: 80px;
  align-self: start;
}

.profile-info-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 32px 24px;
  text-align: center;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.profile-nickname {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
}

.profile-meta {
  text-align: left;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #909399;
}

.profile-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.stat-item {
  cursor: pointer;
  padding: 8px 4px;
  border-radius: 8px;
  transition: background 0.2s;
}

.stat-item:hover {
  background: #f5f7fa;
}

.stat-value {
  display: block;
  font-size: 22px;
  font-weight: 700;
  color: #667eea;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

/* 右侧表单 */
.profile-main {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 24px 32px 32px;
}

.profile-main :deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 500;
}

.profile-form {
  margin-top: 16px;
  max-width: 800px;
}

.profile-form .el-input-number {
  width: 100%;
}

.password-form {
  max-width: 480px;
}

.save-btn {
  margin-top: 8px;
  padding: 10px 32px;
  border-radius: 8px;
}

@media (max-width: 768px) {
  .profile-page {
    grid-template-columns: 1fr;
  }

  .profile-sidebar {
    position: static;
  }

  .profile-main {
    padding: 16px;
  }
}
</style>
