<template>
  <div class="statistics-container">
    <div class="page-header">
      <h2>����ͳ��</h2>
      <p class="subtitle">ϵͳ���ݸ��������</p>
    </div>
    <el-row :gutter="20">
      <el-col :span="6" v-for="stat in stats" :key="stat.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const stats = ref([
  { label: '���û���', value: 0 },
  { label: '�ܿγ���', value: 0 },
  { label: '�ܶ�����', value: 0 },
  { label: '�ܳ�����', value: 0 },
])

onMounted(async () => {
  try {
    const res = await request.get('/statistics/dashboard')
    if (res.code === 200 && res.data) {
      stats.value = [
        { label: '���û���', value: res.data.totalUsers || 0 },
        { label: '�ܿγ���', value: res.data.totalCourses || 0 },
        { label: '�ܶ�����', value: res.data.totalOrders || 0 },
        { label: '�ܳ�����', value: res.data.totalVenues || 0 },
      ]
    }
  } catch (e) {}
})
</script>
