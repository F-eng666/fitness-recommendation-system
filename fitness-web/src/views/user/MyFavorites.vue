<template>
  <div class="my-favorites-page">
    <h2 class="page-title">我的收藏</h2>

    <!-- 类型筛选标签 -->
    <div class="filter-tabs">
      <el-radio-group v-model="activeType" @change="fetchFavorites">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="VENUE">场馆</el-radio-button>
        <el-radio-button value="COURSE">课程</el-radio-button>
        <el-radio-button value="COACH">教练</el-radio-button>
        <el-radio-button value="ARTICLE">文章</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 收藏列表 -->
    <div class="favorites-grid" v-loading="loading">
      <template v-if="favorites.length > 0">
        <el-card
          v-for="item in favorites"
          :key="item.id"
          class="favorite-card"
          shadow="hover"
          @click="handleItemClick(item)"
        >
          <div class="favorite-cover" :class="getCoverClass(item.targetType)">
            <el-icon :size="32" color="rgba(255,255,255,0.6)">
              <component :is="getTypeIcon(item.targetType)" />
            </el-icon>
          </div>
          <div class="favorite-info">
            <div class="favorite-header">
              <h4 class="favorite-name text-ellipsis">{{ item.targetName || '未命名' }}</h4>
              <el-tag size="small" effect="plain" :type="getTypeTag(item.targetType)">
                {{ getTypeLabel(item.targetType) }}
              </el-tag>
            </div>
            <p class="favorite-desc text-ellipsis-2" v-if="item.description">
              {{ item.description }}
            </p>
            <div class="favorite-bottom">
              <span class="favorite-time">{{ item.createTime || '--' }}</span>
              <el-button
                type="danger"
                size="small"
                text
                @click.stop="handleUnfavorite(item)"
              >
                <el-icon><Delete /></el-icon> 取消收藏
              </el-button>
            </div>
          </div>
        </el-card>
      </template>

      <!-- 空状态 -->
      <div v-else-if="!loading" class="empty-state">
        <el-icon :size="64" color="#ddd"><Star /></el-icon>
        <p>暂无收藏内容</p>
        <p class="empty-tip">快去浏览感兴趣的内容吧</p>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="fetchFavorites"
        @size-change="fetchFavorites"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { Place, Reading, UserFilled, ChatDotRound, Star, Delete } from '@element-plus/icons-vue'
import { getMyFavorites, toggleFavorite } from '@/api/user'

const router = useRouter()

const loading = ref(false)
const favorites = ref([])
const activeType = ref('')
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

onMounted(() => {
  fetchFavorites()
})

async function fetchFavorites() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (activeType.value) {
      params.targetType = activeType.value
    }
    const res = await getMyFavorites(params)
    if (res.code === 200) {
      const data = res.data
      favorites.value = data.records || data || []
      total.value = data.total || (Array.isArray(data) ? data.length : 0)
    }
  } catch (e) {
    console.error('获取收藏列表失败', e)
  } finally {
    loading.value = false
  }
}

function getTypeIcon(type) {
  const map = {
    VENUE: Place,
    COURSE: Reading,
    COACH: UserFilled,
    ARTICLE: ChatDotRound
  }
  return map[type] || Star
}

function getTypeLabel(type) {
  const map = {
    VENUE: '场馆',
    COURSE: '课程',
    COACH: '教练',
    ARTICLE: '文章'
  }
  return map[type] || type
}

function getTypeTag(type) {
  const map = {
    VENUE: 'warning',
    COURSE: 'primary',
    COACH: 'success',
    ARTICLE: 'info'
  }
  return map[type] || 'info'
}

function getCoverClass(type) {
  const map = {
    VENUE: 'cover-venue',
    COURSE: 'cover-course',
    COACH: 'cover-coach',
    ARTICLE: 'cover-article'
  }
  return map[type] || 'cover-default'
}

function handleItemClick(item) {
  const routeMap = {
    VENUE: `/venue/${item.targetId}`,
    COURSE: `/course/${item.targetId}`,
    COACH: `/coach/${item.targetId}`,
    ARTICLE: `/community/article/${item.targetId}`
  }
  const path = routeMap[item.targetType]
  if (path) {
    router.push(path)
  }
}

async function handleUnfavorite(item) {
  try {
    const res = await toggleFavorite(item.targetType, item.targetId)
    if (res.code === 200) {
      ElMessage.success('已取消收藏')
      fetchFavorites()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败，请稍后重试')
  }
}
</script>

<style scoped>
.my-favorites-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #409eff;
}

.filter-tabs {
  margin-bottom: 20px;
}

/* 收藏网格 */
.favorites-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  min-height: 200px;
}

.favorite-card {
  cursor: pointer;
  border-radius: 12px;
  overflow: hidden;
  transition: transform 0.2s;
}

.favorite-card:hover {
  transform: translateY(-4px);
}

.favorite-cover {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-venue { background: linear-gradient(135deg, #e6a23c, #f0c46c); }
.cover-course { background: linear-gradient(135deg, #409eff, #66b1ff); }
.cover-coach { background: linear-gradient(135deg, #67c23a, #85ce61); }
.cover-article { background: linear-gradient(135deg, #909399, #b4b4b4); }
.cover-default { background: linear-gradient(135deg, #667eea, #764ba2); }

.favorite-info {
  padding: 16px;
}

.favorite-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.favorite-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  flex: 1;
}

.favorite-desc {
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
  margin-bottom: 12px;
}

.favorite-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.favorite-time {
  font-size: 12px;
  color: #c0c4cc;
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-state .el-icon {
  font-size: 64px;
  color: #ddd;
  margin-bottom: 16px;
}

.empty-tip {
  font-size: 13px;
  color: #c0c4cc;
  margin-top: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 16px 0;
}

@media (max-width: 1000px) {
  .favorites-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .favorites-grid {
    grid-template-columns: 1fr;
  }
}
</style>
