<template>
  <div class="ai-plan-page">
    <!-- Page Header -->
    <div class="page-header">
      <h2 class="page-title">AI 智能健身计划</h2>
      <p class="page-desc">基于DeepSeek AI大模型，为你量身定制科学健身方案</p>
    </div>

    <div class="plan-layout">
      <!-- Left: Input Form -->
      <div class="form-column">
        <div class="form-card">
          <h3 class="card-title">输入你的信息</h3>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            class="plan-form"
          >
            <!-- Height -->
            <el-form-item label="身高 (cm)" prop="height">
              <el-input-number
                v-model="form.height"
                :min="100"
                :max="250"
                :step="0.1"
                placeholder="请输入身高"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>

            <!-- Weight -->
            <el-form-item label="体重 (kg)" prop="weight">
              <el-input-number
                v-model="form.weight"
                :min="30"
                :max="300"
                :step="0.1"
                placeholder="请输入体重"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>

            <!-- Age -->
            <el-form-item label="年龄" prop="age">
              <el-input-number
                v-model="form.age"
                :min="10"
                :max="100"
                placeholder="请输入年龄"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>

            <!-- Gender -->
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio value="男">男</el-radio>
                <el-radio value="女">女</el-radio>
              </el-radio-group>
            </el-form-item>

            <!-- Fitness Goal -->
            <el-form-item label="健身目标" prop="goal">
              <el-select v-model="form.goal" placeholder="请选择健身目标" style="width: 100%">
                <el-option label="减脂" value="减脂" />
                <el-option label="增肌" value="增肌" />
                <el-option label="塑形" value="塑形" />
                <el-option label="增重" value="增重" />
                <el-option label="保持健康" value="保持健康" />
              </el-select>
            </el-form-item>

            <!-- Exercise Frequency -->
            <el-form-item label="运动频率" prop="frequency">
              <el-select v-model="form.frequency" placeholder="请选择运动频率" style="width: 100%">
                <el-option label="每天" value="每天" />
                <el-option label="每周五次" value="每周五次" />
                <el-option label="隔天" value="隔天" />
                <el-option label="每周两次" value="每周两次" />
                <el-option label="每周一次" value="每周一次" />
              </el-select>
            </el-form-item>

            <!-- Available Equipment -->
            <el-form-item label="可用器材">
              <el-input
                v-model="form.equipment"
                type="textarea"
                :rows="2"
                placeholder="如：哑铃、弹力带、瑜伽垫"
              />
            </el-form-item>

            <!-- Diet Preference -->
            <el-form-item label="饮食偏好">
              <el-input
                v-model="form.dietPreference"
                type="textarea"
                :rows="2"
                placeholder="如：不吃猪肉、喜欢清淡"
              />
            </el-form-item>

            <!-- Additional Notes -->
            <el-form-item label="其他说明">
              <el-input
                v-model="form.notes"
                type="textarea"
                :rows="2"
                placeholder="任何需要补充的信息..."
              />
            </el-form-item>

            <!-- Generate Button -->
            <el-button
              type="primary"
              size="large"
              class="generate-btn"
              :loading="generating"
              @click="handleGenerate"
            >
              {{ generating ? '' : '🚀 生成我的专属计划' }}
            </el-button>

            <p class="ai-note">基于DeepSeek AI大模型，为你量身定制科学健身方案</p>
          </el-form>
        </div>
      </div>

      <!-- Right: Result -->
      <div class="result-column">
        <div class="result-card">
          <h3 class="card-title">你的专属计划</h3>

          <!-- Loading State -->
          <div v-if="generating" class="generating-state">
            <div class="loading-dots">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
            </div>
            <p class="generating-text">AI正在为你定制专属计划...</p>
          </div>

          <!-- Empty State -->
          <div v-else-if="!planResult" class="empty-state">
            <div class="empty-icon">🤖</div>
            <p class="empty-text">填写信息后点击生成按钮</p>
          </div>

          <!-- Result State -->
          <div v-else class="result-content">
            <!-- BMI Display -->
            <div v-if="bmi" class="bmi-display">
              <span class="bmi-label">你的BMI：</span>
              <span class="bmi-value">{{ bmi }}</span>
              <span class="bmi-tag" :class="bmiLevel">{{ bmiLevelText }}</span>
            </div>

            <!-- Plan Content -->
            <div class="plan-markdown markdown-body" v-html="renderedPlan"></div>

            <!-- Action Buttons -->
            <div class="result-actions">
              <el-button type="primary" :icon="CopyDocument" @click="handleCopy">
                复制计划
              </el-button>
              <el-button :icon="Refresh" @click="handleRegenerate">
                重新生成
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { CopyDocument, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import { generatePlan } from '@/api/ai'
import { getUserProfile } from '@/api/user'

const SESSION_KEY = 'fitness_ai_plan'

const formRef = ref(null)
const generating = ref(false)
const planResult = ref(null)
const bmi = ref(null)

const form = reactive({
  height: null,
  weight: null,
  age: null,
  gender: '男',
  goal: '',
  frequency: '',
  equipment: '',
  dietPreference: '',
  notes: ''
})

// 健身目标映射（数据库值 → 显示值）
const goalReverseMap = {
  'LOSE_WEIGHT': '减脂', 'BUILD_MUSCLE': '增肌', 'SHAPE': '塑形',
  'GAIN_WEIGHT': '增重', 'KEEP_HEALTHY': '保持健康'
}
// 运动频率映射（数据库值 → 显示值）
const freqReverseMap = {
  'DAILY': '每天', 'FIVE_TIMES_WEEK': '每周五次', 'EVERY_OTHER_DAY': '隔天',
  'TWICE_WEEK': '每周两次', 'WEEKLY': '每周一次', 'RARELY': '偶尔'
}

// 初始化：加载用户信息自动填充 + 恢复已生成的计划
onMounted(async () => {
  // 恢复之前生成的计划
  try {
    const saved = sessionStorage.getItem(SESSION_KEY)
    if (saved) {
      const data = JSON.parse(saved)
      planResult.value = data.plan
      bmi.value = data.bmi
    }
  } catch (e) { /* ignore */ }

  // 从个人信息自动填充表单
  try {
    const res = await getUserProfile()
    if (res.code === 200 && res.data) {
      const u = res.data
      if (u.height) form.height = u.height
      if (u.weight) form.weight = u.weight
      if (u.age) form.age = u.age
      if (u.gender) form.gender = u.gender === 1 ? '男' : u.gender === 2 ? '女' : '男'
      if (u.fitnessGoal) form.goal = goalReverseMap[u.fitnessGoal] || u.fitnessGoal
      if (u.exerciseFrequency) form.frequency = freqReverseMap[u.exerciseFrequency] || u.exerciseFrequency
    }
  } catch (e) { /* 加载失败也不影响使用 */ }
})

const rules = {
  height: [{ required: true, message: '请输入身高', trigger: 'blur' }],
  weight: [{ required: true, message: '请输入体重', trigger: 'blur' }],
  age: [{ required: true, message: '请输入年龄', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  goal: [{ required: true, message: '请选择健身目标', trigger: 'change' }],
  frequency: [{ required: true, message: '请选择运动频率', trigger: 'change' }]
}

const renderedPlan = computed(() => {
  if (!planResult.value) return ''
  return typeof planResult.value === 'string'
    ? marked(planResult.value)
    : marked(planResult.value.plan || JSON.stringify(planResult.value))
})

const bmiLevel = computed(() => {
  if (!bmi.value) return ''
  const v = parseFloat(bmi.value)
  if (v < 18.5) return 'underweight'
  if (v < 24) return 'normal'
  if (v < 28) return 'overweight'
  return 'obese'
})

const bmiLevelText = computed(() => {
  const map = {
    underweight: '偏瘦',
    normal: '正常',
    overweight: '偏重',
    obese: '肥胖'
  }
  return map[bmiLevel.value] || ''
})

async function handleGenerate() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  generating.value = true
  planResult.value = null
  bmi.value = null

  try {
    // 映射前端表单字段到后端API字段格式
    const requestData = {
      height: form.height,
      weight: form.weight,
      age: form.age,
      gender: form.gender === '男' ? 1 : 2,
      fitnessGoal: form.goal,
      exerciseFrequency: form.frequency,
      availableEquipment: form.equipment || '',
      dietPreference: form.dietPreference || '',
      additionalNotes: form.notes || ''
    }
    const res = await generatePlan(requestData)
    if (res.data) {
      planResult.value = res.data.plan || res.data
      bmi.value = res.data.bmi || null
    }
    if (res.code === 200) {
      ElMessage.success('专属计划已生成！')
      // 保存到 sessionStorage，切换页面后不丢失
      sessionStorage.setItem(SESSION_KEY, JSON.stringify({
        plan: planResult.value,
        bmi: bmi.value
      }))
    } else {
      ElMessage.error(res.message || '生成计划失败')
    }
  } catch (error) {
    ElMessage.error('生成计划失败，请稍后重试')
  } finally {
    generating.value = false
  }
}

async function handleCopy() {
  const text = typeof planResult.value === 'string'
    ? planResult.value
    : (planResult.value.plan || JSON.stringify(planResult.value, null, 2))
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('计划已复制到剪贴板')
  } catch {
    ElMessage.info('请手动复制计划内容')
  }
}

function handleRegenerate() {
  handleGenerate()
}
</script>

<style scoped>
.ai-plan-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  padding: 32px 20px 60px;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 8px;
}

.page-desc {
  font-size: 16px;
  color: #909399;
  margin: 0;
}

.plan-layout {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 24px;
  align-items: start;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #667eea;
  display: inline-block;
}

/* Left Form Card */
.form-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  padding: 28px;
}

.plan-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #303133;
}

.generate-btn {
  width: 100%;
  height: 48px;
  font-size: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
  margin-top: 8px;
}

.generate-btn:hover {
  background: linear-gradient(135deg, #5a6fd6 0%, #6a4190 100%);
}

.ai-note {
  margin: 12px 0 0;
  font-size: 12px;
  color: #c0c4cc;
  text-align: center;
}

/* Right Result Card */
.result-column {
  position: sticky;
  top: 24px;
}

.result-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  padding: 28px;
  min-height: 500px;
}

/* Generating State */
.generating-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}

.loading-dots {
  display: flex;
  gap: 10px;
  margin-bottom: 24px;
}

.dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  animation: pulse 1.4s ease-in-out infinite both;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }
.dot:nth-child(3) { animation-delay: 0s; }

@keyframes pulse {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.generating-text {
  font-size: 16px;
  color: #606266;
  margin: 0;
}

/* Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 15px;
  color: #909399;
  margin: 0;
}

/* Result Content */
.result-content {
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.bmi-display {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.bmi-label {
  font-size: 14px;
  color: #606266;
}

.bmi-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.bmi-tag {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 12px;
  font-weight: 500;
}

.bmi-tag.underweight {
  background: #e6f7ff;
  color: #1890ff;
}

.bmi-tag.normal {
  background: #f6ffed;
  color: #52c41a;
}

.bmi-tag.overweight {
  background: #fff7e6;
  color: #fa8c16;
}

.bmi-tag.obese {
  background: #fff2f0;
  color: #ff4d4f;
}

/* Plan Markdown */
.plan-markdown {
  line-height: 1.8;
  font-size: 15px;
  color: #303133;
}

.plan-markdown :deep(h1),
.plan-markdown :deep(h2),
.plan-markdown :deep(h3) {
  margin-top: 24px;
  margin-bottom: 12px;
  font-weight: 600;
}

.plan-markdown :deep(h2) {
  font-size: 20px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.plan-markdown :deep(p) {
  margin-bottom: 12px;
}

.plan-markdown :deep(ul),
.plan-markdown :deep(ol) {
  padding-left: 20px;
  margin-bottom: 12px;
}

.plan-markdown :deep(li) {
  margin-bottom: 6px;
}

.plan-markdown :deep(blockquote) {
  border-left: 4px solid #667eea;
  padding: 8px 16px;
  margin: 12px 0;
  background: #f5f7fa;
  border-radius: 0 6px 6px 0;
}

.plan-markdown :deep(code) {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 13px;
}

.plan-markdown :deep(pre) {
  background: #282c34;
  color: #abb2bf;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
}

.plan-markdown :deep(pre code) {
  background: none;
  padding: 0;
}

.plan-markdown :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
}

.plan-markdown :deep(th),
.plan-markdown :deep(td) {
  border: 1px solid #e8e8e8;
  padding: 8px 12px;
  text-align: left;
}

.plan-markdown :deep(th) {
  background: #f5f7fa;
  font-weight: 600;
}

.plan-markdown :deep(strong) {
  color: #667eea;
}

/* Result Actions */
.result-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

@media (max-width: 900px) {
  .plan-layout {
    grid-template-columns: 1fr;
  }

  .result-column {
    position: static;
  }
}
</style>
