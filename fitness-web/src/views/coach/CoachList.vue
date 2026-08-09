<template>
  <div class="coach-list-page">
    <!-- 搜索筛选栏 -->
    <div class="search-filter-bar">
      <div class="search-row">
        <el-input
          v-model="keyword"
          placeholder="搜索教练姓名或专长"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="venueFilter"
          placeholder="选择场馆"
          clearable
          filterable
          class="venue-select"
          @change="handleSearch"
        >
          <el-option
            v-for="v in venueOptions"
            :key="v.id"
            :label="v.name"
            :value="v.id"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch" :loading="loading">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
      </div>
    </div>

    <!-- 教练列表 -->
    <div v-loading="loading" class="coach-grid-wrapper">
      <template v-if="loading">
        <div class="coach-grid">
          <div v-for="n in 8" :key="n" class="coach-skeleton-card">
            <el-skeleton animated>
              <template #template>
                <div style="display:flex; flex-direction:column; align-items:center; padding:24px 16px 16px">
                  <el-skeleton-item variant="circle" style="width:80px;height:80px" />
                  <el-skeleton-item variant="text" style="width:50%;margin-top:12px" />
                  <el-skeleton-item variant="text" style="width:60%;margin-top:8px" />
                  <el-skeleton-item variant="text" style="width:70%;margin-top:8px" />
                </div>
              </template>
            </el-skeleton>
          </div>
        </div>
      </template>

      <template v-else-if="error">
        <el-empty description="加载失败，请重试">
          <el-button type="primary" @click="fetchCoachList">重新加载</el-button>
        </el-empty>
      </template>

      <template v-else-if="coachList.length === 0">
        <el-empty description="暂无教练数据" />
      </template>

      <template v-else>
        <div class="coach-grid">
          <el-card
            v-for="coach in coachList"
            :key="coach.id"
            class="coach-card"
            shadow="hover"
          >
            <div class="coach-card-inner">
              <el-avatar :size="80" :src="coach.avatar" class="coach-avatar">
                {{ (coach.nickname || coach.name || '教').charAt(0) }}
              </el-avatar>
              <h3 class="coach-name">
                {{ coach.nickname || coach.name || '未知教练' }}
                <el-icon v-if="coach.gender === 'male' || coach.gender === 'MALE'" color="#409eff">
                  <svg viewBox="0 0 1024 1024" width="14" height="14"><path d="M854.6 288.6L639.4 73.4c-6-6-14.1-9.4-22.6-9.4H192c-17.7 0-32 14.3-32 32v832c0 17.7 14.3 32 32 32h640c17.7 0 32-14.3 32-32V311.3c0-8.5-3.4-16.7-9.4-22.7zM790.2 326H602V137.8L790.2 326zm1.8 562H232V136h302v216c0 23.2 18.8 42 42 42h216v494z" fill="currentColor"/></svg>
                </el-icon>
                <el-icon v-else-if="coach.gender === 'female' || coach.gender === 'FEMALE'" color="#f56c6c">
                  <svg viewBox="0 0 1024 1024" width="14" height="14"><path d="M854.6 288.6L639.4 73.4c-6-6-14.1-9.4-22.6-9.4H192c-17.7 0-32 14.3-32 32v832c0 17.7 14.3 32 32 32h640c17.7 0 32-14.3 32-32V311.3c0-8.5-3.4-16.7-9.4-22.7zM790.2 326H602V137.8L790.2 326zm1.8 562H232V136h302v216c0 23.2 18.8 42 42 42h216v494z" fill="currentColor"/></svg>
                </el-icon>
              </h3>
              <div class="coach-basic-info">
                <span v-if="coach.age">年龄：{{ coach.age }}</span>
                <span v-if="coach.experience">{{ coach.experience }}</span>
              </div>
              <div class="coach-tags">
                <el-tag
                  v-for="tag in (coach.specialtyList || coach.specialties || coach.specialty || [])"
                  :key="tag"
                  size="small"
                  type="info"
                >
                  {{ tag }}
                </el-tag>
              </div>
              <div class="coach-rating-row">
                <el-rate
                  :model-value="coach.rating || 0"
                  disabled
                  size="small"
                />
                <span class="rating-number">{{ coach.rating || 0 }}</span>
              </div>
              <el-button
                type="primary"
                class="detail-btn"
                @click="goToDetail(coach.id)"
              >
                查看详情
              </el-button>
            </div>
          </el-card>
        </div>
      </template>
    </div>

    <!-- 分页 -->
    <div v-if="total > 0 && !error" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[8, 16, 32]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getCoachList } from '@/api/user'
import { getVenueList } from '@/api/venue'

const router = useRouter()

const keyword = ref('')
const venueFilter = ref('')
const venueOptions = ref([])

const loading = ref(false)
const error = ref(false)

const currentPage = ref(1)
const pageSize = ref(8)
const total = ref(0)

const coachList = ref([])

async function fetchVenueOptions() {
  try {
    const res = await getVenueList({ page: 1, pageSize: 1000 })
    venueOptions.value = res?.data?.records || res?.data?.list || res?.data || []
  } catch { /* ignore */ }
}

async function fetchCoachList() {
  loading.value = true
  error.value = false
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      venueId: venueFilter.value || undefined
    }
    const res = await getCoachList(params)
    if (res && res.data) {
      coachList.value = res.data.records || res.data.list || res.data || []
      total.value = res.data.total || coachList.value.length
    } else {
      coachList.value = []
      total.value = 0
    }
  } catch (err) {
    console.error('获取教练列表失败:', err)
    error.value = true
    coachList.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchCoachList()
}

function handlePageChange() {
  fetchCoachList()
}

function handleSizeChange() {
  currentPage.value = 1
  fetchCoachList()
}

function goToDetail(id) {
  router.push(`/coach/${id}`)
}

onMounted(() => {
  fetchVenueOptions()
  fetchCoachList()
})
</script>

<style scoped>
.coach-list-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px;
}

.search-filter-bar {
  margin-bottom: 24px;
}

.search-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  max-width: 360px;
}

.venue-select {
  width: 200px;
}

.coach-grid-wrapper {
  min-height: 300px;
}

.coach-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.coach-skeleton-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.coach-card {
  transition: transform 0.2s ease;
}

.coach-card:hover {
  transform: translateY(-4px);
}

.coach-card-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 28px 16px 20px;
}

.coach-avatar {
  margin-bottom: 12px;
}

.coach-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.coach-basic-info {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}

.coach-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
  margin-bottom: 10px;
  min-height: 28px;
}

.coach-rating-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.rating-number {
  font-size: 13px;
  color: #f7ba2a;
  font-weight: 600;
}

.detail-btn {
  width: 100%;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

@media (max-width: 1000px) {
  .coach-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 750px) {
  .coach-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 500px) {
  .coach-grid {
    grid-template-columns: 1fr;
  }
  .search-row {
    flex-direction: column;
  }
  .search-input {
    max-width: 100%;
  }
}
</style>
