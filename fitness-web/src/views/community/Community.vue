<template>
  <div class="community-page">
    <!-- Hero Banner -->
    <div class="hero-banner">
      <div class="hero-content">
        <h1 class="hero-title">健身知识社区</h1>
        <p class="hero-subtitle">分享健身经验，一起成长进步</p>
      </div>
    </div>

    <div class="community-container">
      <!-- Search & Filters -->
      <div class="filters-bar">
        <div class="search-row">
          <el-input
            v-model="searchParams.keyword"
            placeholder="搜索文章..."
            clearable
            :prefix-icon="Search"
            class="search-input"
            @keyup.enter="handleSearch"
          />
          <el-select
            v-model="searchParams.category"
            placeholder="文章分类"
            clearable
            class="filter-select"
          >
            <el-option label="全部" value="" />
            <el-option label="健身入门" value="健身入门" />
            <el-option label="营养饮食" value="营养饮食" />
            <el-option label="训练技巧" value="训练技巧" />
            <el-option label="达人分享" value="达人分享" />
            <el-option label="伤病预防" value="伤病预防" />
          </el-select>
          <el-select
            v-model="searchParams.sort"
            placeholder="排序方式"
            class="filter-select sort-select"
          >
            <el-option label="最新发布" value="latest" />
            <el-option label="最热" value="hot" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="handleSearch">
            搜索
          </el-button>
        </div>
      </div>

      <!-- Article List -->
      <div v-loading="loading" class="article-list">
        <el-empty v-if="!loading && articleList.length === 0" description="暂无文章" />

        <div
          v-for="article in articleList"
          :key="article.id"
          class="article-card"
          @click="goToDetail(article.id)"
        >
          <div class="article-cover">
            <el-image
              :src="article.coverImage || defaultCover"
              fit="cover"
              class="cover-img"
            >
              <template #error>
                <div class="cover-placeholder">
                  <el-icon :size="48"><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </div>
          <div class="article-info">
            <h3 class="article-title">{{ article.title }}</h3>
            <p class="article-summary">{{ article.summary }}</p>
            <div class="article-meta">
              <div class="author-info">
                <el-avatar :size="28" :src="article.authorAvatar" />
                <span class="author-name">{{ article.authorName }}</span>
                <el-tag
                  v-if="article.isExpert"
                  size="small"
                  type="warning"
                  class="expert-badge"
                >
                  达人
                </el-tag>
              </div>
              <div class="article-tags-stats">
                <el-tag
                  :type="getCategoryTagType(article.category)"
                  size="small"
                  class="category-tag"
                >
                  {{ categoryMap[article.category] || article.category }}
                </el-tag>
                <div class="stats-row">
                  <span class="stat-item">
                    <el-icon><View /></el-icon>
                    {{ article.viewCount }}
                  </span>
                  <span class="stat-item">
                    <el-icon><Star /></el-icon>
                    {{ article.likeCount }}
                  </span>
                  <span class="stat-item">
                    <el-icon><ChatDotRound /></el-icon>
                    {{ article.commentCount }}
                  </span>
                </div>
                <span class="article-time">{{ formatTime(article.createTime) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="total > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="searchParams.page"
          v-model:page-size="searchParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 30]"
          layout="total, sizes, prev, pager, next"
          @change="fetchArticleList"
        />
      </div>
    </div>

    <!-- Write Article FAB -->
    <el-button
      v-if="userStore.isLoggedIn"
      type="primary"
      size="large"
      circle
      class="write-fab"
      @click="goToWrite"
    >
      <el-icon :size="22"><Edit /></el-icon>
    </el-button>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Picture, View, Star, ChatDotRound, Edit } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getArticleList } from '@/api/article'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const articleList = ref([])
const total = ref(0)
const defaultCover = 'https://via.placeholder.com/200x140?text=No+Image'

const searchParams = reactive({
  keyword: '',
  category: '',
  sort: 'latest',
  page: 1,
  pageSize: 10
})

const categoryMap = {
  BEGINNER: '健身入门',
  NUTRITION: '营养饮食',
  TECHNIQUE: '训练技巧',
  EXPERT_SHARE: '达人分享',
  INJURY_PREVENTION: '伤病预防'
}

const categoryTagTypeMap = {
  BEGINNER: 'success',
  NUTRITION: 'warning',
  TECHNIQUE: 'primary',
  EXPERT_SHARE: 'danger',
  INJURY_PREVENTION: 'info'
}

function getCategoryTagType(category) {
  return categoryTagTypeMap[category] || ''
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days === 0) {
    const hours = Math.floor(diff / (1000 * 60 * 60))
    if (hours === 0) {
      const minutes = Math.floor(diff / (1000 * 60))
      return minutes <= 1 ? '刚刚' : `${minutes}分钟前`
    }
    return `${hours}小时前`
  }
  if (days < 30) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

async function fetchArticleList() {
  loading.value = true
  try {
    const res = await getArticleList(searchParams)
    articleList.value = res.data.records || res.data.list || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('加载文章列表失败')
    articleList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  searchParams.page = 1
  fetchArticleList()
}

function goToDetail(id) {
  router.push(`/community/article/${id}`)
}

function goToWrite() {
  router.push('/community/write')
}

onMounted(() => {
  fetchArticleList()
})
</script>

<style scoped>
.community-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.hero-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 60px 0;
  text-align: center;
  color: #fff;
}

.hero-title {
  font-size: 36px;
  font-weight: 700;
  margin: 0 0 12px;
}

.hero-subtitle {
  font-size: 18px;
  opacity: 0.9;
  margin: 0;
}

.community-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.filters-bar {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  margin-bottom: 20px;
  border: 1px solid #e8e8e8;
}

.search-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  flex: 1;
  min-width: 200px;
}

.filter-select {
  width: 150px;
}

.sort-select {
  width: 130px;
}

.article-list {
  min-height: 400px;
}

.article-card {
  display: flex;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  margin-bottom: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.3s, transform 0.2s;
}

.article-card:hover {
  border-color: #667eea;
  transform: translateY(-2px);
}

.article-cover {
  width: 200px;
  min-width: 200px;
  height: 140px;
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
  border-radius: 8px 0 0 8px;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f0f0;
  color: #c0c4cc;
}

.article-info {
  flex: 1;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}

.article-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.article-summary {
  font-size: 14px;
  color: #909399;
  margin: 0 0 12px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-name {
  font-size: 14px;
  color: #606266;
}

.expert-badge {
  font-size: 12px;
  height: 20px;
  line-height: 20px;
}

.article-tags-stats {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.stats-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
}

.article-time {
  font-size: 13px;
  color: #c0c4cc;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.write-fab {
  position: fixed;
  right: 40px;
  bottom: 60px;
  width: 56px;
  height: 56px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  z-index: 100;
}
</style>
