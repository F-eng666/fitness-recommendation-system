<template>
  <div class="diet-plan-container">
    <div class="page-header">
      <h2>��ʳ�Ƽ��ƻ�</h2>
      <p class="subtitle">AI�����Ƽ�����ѧ����ÿ��Ӫ��</p>
    </div>
    <el-card class="plan-card">
      <template #header>
        <span>������ʳ�Ƽ�</span>
        <el-button type="primary" size="small" style="float:right" @click="generatePlan">
          AI���ɼƻ�
        </el-button>
      </template>
      <el-empty description="����Ҳఴť���ɽ�����ʳ�ƻ�" v-if="!planData.length" />
      <el-timeline v-else>
        <el-timeline-item v-for="item in planData" :key="item.id" :timestamp="item.mealType" placement="top">
          <el-card shadow="hover">
            <h4>{{ item.foodName }}</h4>
            <p>����: {{ item.calories }}kcal | ������: {{ item.protein }}g | ֬��: {{ item.fat }}g</p>
            <p>{{ item.recommendation }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getToken } from '@/utils/auth'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const planData = ref([])

const generatePlan = async () => {
  try {
    const res = await request.post('/diet/plan', { date: new Date().toISOString().split('T')[0] })
    if (res.code === 200) {
      ElMessage.success('�ƻ����ɳɹ�')
      loadPlan()
    }
  } catch (e) {
    ElMessage.error('����ʧ��')
  }
}

const loadPlan = async () => {
  try {
    const res = await request.get('/diet/today-plan')
    if (res.code === 200) planData.value = res.data || []
  } catch (e) {}
}
loadPlan()
</script>
