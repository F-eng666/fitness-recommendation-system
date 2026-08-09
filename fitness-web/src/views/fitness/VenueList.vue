<template>
  <div class="venue-list-page">
    <!-- 搜索和筛选区域 -->
    <div class="search-filter-bar">
      <div class="search-row">
        <el-input
          v-model="keyword"
          placeholder="搜索场馆名称或地址"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-if="isAdmin"
          v-model="statusFilter"
          placeholder="审核状态"
          clearable
          class="status-select"
          @change="handleSearch"
        >
          <el-option label="全部" value="" />
          <el-option label="审核通过" value="APPROVED" />
          <el-option label="待审核" value="PENDING" />
        </el-select>
        <el-button type="primary" @click="handleSearch" :loading="loading">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
      </div>
    </div>

    <!-- 场馆卡片网格 -->
    <div v-loading="loading" class="venue-grid-wrapper">
      <template v-if="loading">
        <div class="venue-grid">
          <div v-for="n in 6" :key="n" class="card-skeleton">
            <el-skeleton animated>
              <template #template>
                <el-skeleton-item variant="image" style="width: 100%; height: 180px" />
                <div style="padding: 16px">
                  <el-skeleton-item variant="text" style="width: 60%" />
                  <el-skeleton-item variant="text" style="width: 80%" />
                  <el-skeleton-item variant="text" style="width: 40%" />
                </div>
              </template>
            </el-skeleton>
          </div>
        </div>
      </template>

      <template v-else-if="error">
        <el-empty description="加载失败，请重试">
          <el-button type="primary" @click="fetchVenueList">重新加载</el-button>
        </el-empty>
      </template>

      <template v-else-if="venueList.length === 0">
        <el-empty description="暂无场馆数据" />
      </template>

      <template v-else>
        <div class="venue-grid">
          <el-card
            v-for="venue in venueList"
            :key="venue.id"
            class="venue-card"
            shadow="hover"
            @click="goToDetail(venue.id)"
          >
            <div class="card-cover" :style="{ background: getCoverGradient(venue.id) }">
              <span class="cover-text">{{ venue.name?.charAt(0) || '场' }}</span>
            </div>
            <div class="card-body">
              <h3 class="venue-name">{{ venue.name }}</h3>
              <p class="venue-address">
                <el-icon><Location /></el-icon>
                {{ venue.address || '暂无地址' }}
              </p>
              <div class="venue-rating">
                <el-rate
                  :model-value="venue.rating || 0"
                  disabled
                  show-score
                  text-color="#ff9900"
                  size="small"
                />
              </div>
              <div class="card-footer">
                <div class="facility-tags">
                  <el-tag
                    v-for="facility in parseFacilities(venue.facilities).slice(0, 3)"
                    :key="facility"
                    size="small"
                    type="info"
                    class="facility-tag"
                  >
                    {{ facility }}
                  </el-tag>
                </div>
              </div>
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
        :page-sizes="[9, 18, 30]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Location } from '@element-plus/icons-vue'
import { getVenueList } from '@/api/venue'

const router = useRouter()

// 搜索与筛选
const keyword = ref('')
const statusFilter = ref('')
const loading = ref(false)
const error = ref(false)

// 分页
const currentPage = ref(1)
const pageSize = ref(9)
const total = ref(0)

// 数据
const venueList = ref([])

// 权限判断 - 简化处理：检查localStorage中是否存有admin角色
const isAdmin = computed(() => {
  try {
    const userStr = localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo')
    if (userStr) {
      const user = JSON.parse(userStr)
      return user.role === 'ADMIN'
    }
  } catch { /* ignore */ }
  return false
})

// 解析场馆设施（兼容JSON字符串和数组）
function parseFacilities(val) {
  if (!val) return []
  if (Array.isArray(val)) return val
  try { const parsed = JSON.parse(val); if (Array.isArray(parsed)) return parsed } catch {}
  return val.split(/[,，、]/).map(s => s.trim()).filter(Boolean)
}

// 根据场馆ID生成渐变色背景
function getCoverGradient(id) {
  const gradients = [
    'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
    'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
    'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)',
    'linear-gradient(135deg, #fccb90 0%, #d57eeb 100%)',
    'linear-gradient(135deg, #e0c3fc 0%, #8ec5fc 100%)'
  ]
  const index = typeof id === 'number' ? id : (id || '').toString().charCodeAt(0) || 0
  return gradients[index % gradients.length]
}

// 获取场馆列表
async function fetchVenueList() {
  loading.value = true
  error.value = false
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined
    }
    const res = await getVenueList(params)
    if (res && res.data) {
      venueList.value = res.data.records || res.data.list || res.data || []
      total.value = res.data.total || venueList.value.length
    } else {
      venueList.value = []
      total.value = 0
    }
  } catch (err) {
    console.error('获取场馆列表失败:', err)
    error.value = true
    venueList.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchVenueList()
}

function handlePageChange() {
  fetchVenueList()
}

function handleSizeChange() {
  currentPage.value = 1
  fetchVenueList()
}

function goToDetail(id) {
  router.push(`/fitness/venue/${id}`)
}

onMounted(() => {
  fetchVenueList()
})
</script>

<style scoped>
.venue-list-page {
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
  max-width: 400px;
}

.status-select {
  width: 160px;
}

.venue-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.card-skeleton {
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #ebeef5;
}

.venue-card {
  cursor: pointer;
  transition: transform 0.2s ease;
}

.venue-card:hover {
  transform: translateY(-4px);
}

.card-cover {
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px 4px 0 0;
}

.cover-text {
  font-size: 48px;
  font-weight: bold;
  color: rgba(255, 255, 255, 0.8);
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.card-body {
  padding: 4px 0;
}

.venue-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.venue-address {
  font-size: 13px;
  color: #909399;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.venue-rating {
  margin-bottom: 10px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.facility-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.facility-tag {
  font-size: 11px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

.venue-grid-wrapper {
  min-height: 300px;
}

@media (max-width: 900px) {
  .venue-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .venue-grid {
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
