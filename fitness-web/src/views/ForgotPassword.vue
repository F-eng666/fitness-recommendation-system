<template>
  <div class="forgot-page">
    <div class="forgot-card">
      <div class="card-header">
        <el-icon :size="48" color="#667eea"><svg viewBox="0 0 1024 1024" width="48" height="48"><path d="M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64zm-32 232c0-4.4 3.6-8 8-8h48c4.4 0 8 3.6 8 8v272c0 4.4-3.6 8-8 8h-48c-4.4 0-8-3.6-8-8V296zm32 440c-26.5 0-48-21.5-48-48s21.5-48 48-48 48 21.5 48 48-21.5 48-48 48z" fill="currentColor"/></svg></el-icon>
        <h1>找回密码</h1>
        <p class="subtitle">输入注册时使用的手机号，获取验证码重置密码</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        class="forgot-form"
        @submit.prevent
      >
        <el-form-item prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="请输入注册手机号"
            size="large"
            :prefix-icon="Phone"
            maxlength="11"
          />
        </el-form-item>

        <el-form-item prop="code">
          <div class="code-row">
            <el-input
              v-model="form.code"
              placeholder="验证码"
              size="large"
              :prefix-icon="Key"
              maxlength="6"
              class="code-input"
            />
            <el-button
              size="large"
              :disabled="countdown > 0"
              :loading="sending"
              class="send-btn"
              @click="sendCode"
            >
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="新密码（至少6位）"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="确认新密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="submitting"
            @click="handleReset"
          >
            重置密码
          </el-button>
        </el-form-item>
      </el-form>

      <div class="card-footer">
        <el-button type="primary" link @click="$router.push('/login')">
          返回登录
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Phone, Key, Lock } from '@element-plus/icons-vue'
import { sendResetCode, resetPassword } from '@/api/auth'

const router = useRouter()
const formRef = ref(null)

const form = reactive({
  phone: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (_rule, value, callback) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const sending = ref(false)
const submitting = ref(false)
const countdown = ref(0)
let timer = null

async function sendCode() {
  try {
    await formRef.value.validateField('phone')
  } catch {
    return
  }
  sending.value = true
  try {
    await sendResetCode(form.phone)
    ElMessage.success('验证码已发送（请查看服务端控制台日志）')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (err) {
    console.error(err)
  } finally {
    sending.value = false
  }
}

async function handleReset() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await resetPassword(form.phone, form.code, form.newPassword)
    ElMessage.success('密码重置成功，请使用新密码登录')
    router.push('/login')
  } catch (err) {
    console.error(err)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.forgot-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.forgot-card {
  width: 420px;
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.card-header h1 {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin: 12px 0 8px;
}

.subtitle {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.forgot-form {
  margin-bottom: 0;
}

.code-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.code-input {
  flex: 1;
}

.send-btn {
  min-width: 120px;
  white-space: nowrap;
}

.submit-btn {
  width: 100%;
}

.card-footer {
  text-align: center;
  margin-top: 8px;
}
</style>
