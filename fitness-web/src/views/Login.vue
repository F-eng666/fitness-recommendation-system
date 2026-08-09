<template>
  <div class="login-page">
    <div class="login-bg"></div>
    <div class="login-card">
      <div class="login-logo">
        <el-icon :size="48" color="#667eea"><PictureFilled /></el-icon>
        <h1>FitPro 智能健身</h1>
        <p>个性化AI健身推荐平台</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs" :stretch="true">
        <el-tab-pane label="登录" name="login">
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            label-position="top"
            @keyup.enter="handleLogin"
          >
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                size="large"
                :prefix-icon="User"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                show-password
                :prefix-icon="Lock"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                class="login-btn"
                :loading="loginLoading"
                @click="handleLogin"
              >
                登 录
              </el-button>
            </el-form-item>
            <div class="forgot-link">
              <el-button type="primary" link @click="$router.push('/forgot-password')">
                忘记密码？
              </el-button>
            </div>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-position="top"
            @keyup.enter="handleRegister"
          >
            <el-form-item prop="username">
              <el-input
                v-model="registerForm.username"
                placeholder="请输入用户名"
                size="large"
                :prefix-icon="User"
              />
            </el-form-item>
            <el-form-item prop="phone">
              <el-input
                v-model="registerForm.phone"
                placeholder="请输入手机号"
                size="large"
                :prefix-icon="Phone"
                maxlength="11"
              />
            </el-form-item>
            <el-form-item prop="nickname">
              <el-input
                v-model="registerForm.nickname"
                placeholder="请输入昵称"
                size="large"
                :prefix-icon="EditPen"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                show-password
                :prefix-icon="Lock"
              />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="请确认密码"
                size="large"
                show-password
                :prefix-icon="Lock"
              />
            </el-form-item>
            <!-- 注册身份默认为普通用户，管理员由后台设定 -->
            <input type="hidden" v-model="registerForm.role" />
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                class="login-btn"
                :loading="registerLoading"
                @click="handleRegister"
              >
                注 册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
    <div class="login-footer">
      &copy; {{ new Date().getFullYear() }} FitPro 智能健身推荐系统
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, EditPen, PictureFilled, Phone } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeTab = ref('login')
const loginLoading = ref(false)
const registerLoading = ref(false)
const loginFormRef = ref(null)
const registerFormRef = ref(null)

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  phone: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  role: 'USER'
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请确认密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3到20个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return

  loginLoading.value = true
  try {
    const success = await userStore.login(loginForm)
    if (success) {
      ElMessage.success('登录成功')
      const redirect = route.query.redirect || '/home'
      router.push(redirect)
    } else {
      ElMessage.error('登录失败，请检查用户名和密码')
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '登录失败，请稍后重试')
  } finally {
    loginLoading.value = false
  }
}

async function handleRegister() {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return

  registerLoading.value = true
  try {
    const res = await userStore.register({
      username: registerForm.username,
      phone: registerForm.phone,
      nickname: registerForm.nickname,
      password: registerForm.password,
      role: registerForm.role
    })
    if (res.code === 200) {
      ElMessage.success('注册成功，请登录')
      // 切换到登录tab
      activeTab.value = 'login'
      loginForm.username = registerForm.username
      registerFormRef.value.resetFields()
    } else {
      ElMessage.error(res.message || '注册失败')
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '注册失败，请稍后重试')
  } finally {
    registerLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 30%, #0f3460 60%, #533483 100%);
  z-index: 0;
}

.login-bg::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at 30% 50%, rgba(102, 126, 234, 0.3) 0%, transparent 50%),
              radial-gradient(circle at 70% 50%, rgba(118, 75, 162, 0.3) 0%, transparent 50%);
  animation: bgFloat 8s ease-in-out infinite alternate;
}

@keyframes bgFloat {
  0% { transform: translate(0, 0); }
  100% { transform: translate(-2%, -2%); }
}

.login-card {
  position: relative;
  z-index: 1;
  width: 420px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  padding: 40px;
  backdrop-filter: blur(10px);
}

.login-logo {
  text-align: center;
  margin-bottom: 24px;
}

.login-logo h1 {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 12px 0 8px;
}

.login-logo p {
  font-size: 14px;
  color: #909399;
}

.login-tabs {
  margin-top: 8px;
}

.login-tabs :deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 500;
}

.login-tabs :deep(.el-tabs__active-bar) {
  background: linear-gradient(135deg, #667eea, #764ba2);
  height: 3px;
  border-radius: 2px;
}

.login-tabs :deep(.el-tabs__item.is-active) {
  color: #667eea;
}

.login-btn {
  width: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  font-size: 16px;
  letter-spacing: 4px;
  height: 44px;
  border-radius: 8px;
}

.forgot-link {
  text-align: right;
  margin-top: -8px;
  margin-bottom: 4px;
}

.login-btn:hover {
  opacity: 0.9;
}

.login-btn:active {
  opacity: 0.85;
}

.login-footer {
  position: relative;
  z-index: 1;
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
  margin-top: 24px;
}
</style>
