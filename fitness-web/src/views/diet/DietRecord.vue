<template>
  <div class="diet-record-container">
    <div class="page-header">
      <h2>��ʳ��¼</h2>
      <p class="subtitle">��¼ÿ����ʳ��׷��Ӫ������</p>
    </div>
    <el-card class="record-card">
      <template #header>
        <span>��ʳ��¼</span>
        <el-button type="primary" size="small" style="float:right" @click="showDialog = true">
          ��Ӽ�¼
        </el-button>
      </template>
      <el-table :data="records" stripe style="width:100%">
        <el-table-column prop="recordDate" label="����" width="120" />
        <el-table-column prop="mealType" label="�ʹ�" width="100" />
        <el-table-column prop="foodName" label="ʳ��" />
        <el-table-column prop="amount" label="����" width="80" />
        <el-table-column prop="calories" label="����(kcal)" width="100" />
        <el-table-column prop="createTime" label="��¼ʱ��" width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import request from '@/utils/request'

const records = ref([])
const showDialog = ref(false)

const loadRecords = async () => {
  try {
    const res = await request.get('/diet/records', {
      params: { startDate: '', endDate: '' }
    })
    if (res.code === 200) records.value = res.data || []
  } catch (e) {}
}
loadRecords()
</script>
