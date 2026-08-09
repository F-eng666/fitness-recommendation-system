<template>
  <div class="article-detail-page">
    <div v-loading="loading" class="detail-container">
      <template v-if="!loading && article">
        <!-- Back Button -->
        <div class="back-row">
          <el-button text :icon="ArrowLeft" @click="goBack">返回社区</el-button>
        </div>

        <!-- Article Header -->
        <div class="article-header">
          <h1 class="article-title">{{ article.title }}</h1>
          <div class="header-meta">
            <div class="author-row">
              <el-avatar :size="40" :src="article.authorAvatar" />
              <div class="author-detail">
                <div class="author-name-row">
                  <span class="author-name">{{ article.authorName }}</span>
                  <el-tag v-if="article.isExpert" size="small" type="warning">达人</el-tag>
                </div>
                <span class="publish-time">{{ formatDateTime(article.createTime) }}</span>
              </div>
            </div>
            <div class="header-stats">
              <el-tag :type="categoryTagType" size="small">{{ categoryMap[article.category] || article.category }}</el-tag>
              <span class="stat"><el-icon><View /></el-icon> {{ article.viewCount }}</span>
              <span class="stat"><el-icon><Star /></el-icon> {{ article.likeCount }}</span>
              <span class="stat"><el-icon><ChatDotRound /></el-icon> {{ article.commentCount }}</span>
            </div>
          </div>
        </div>

        <!-- Cover Image -->
        <div v-if="article.coverImage" class="cover-wrapper">
          <el-image :src="article.coverImage" fit="cover" class="cover-img" />
        </div>

        <!-- Article Content (Markdown) -->
        <div class="article-content markdown-body" v-html="renderedContent"></div>

        <!-- Action Bar -->
        <div class="action-bar">
          <el-button
            :type="liked ? 'danger' : 'default'"
            :icon="Star"
            @click="handleToggleLike"
            :loading="likeLoading"
          >
            {{ liked ? '已点赞' : '点赞' }} ({{ likeCount }})
          </el-button>
          <el-button
            :type="favorited ? 'warning' : 'default'"
            :icon="Collection"
            @click="handleToggleFavorite"
            :loading="favLoading"
          >
            {{ favorited ? '已收藏' : '收藏' }}
          </el-button>
          <el-button :icon="Share" @click="handleShare">分享</el-button>
        </div>

        <!-- Divider -->
        <el-divider />

        <!-- Comments Section -->
        <div class="comments-section">
          <h3 class="section-title">
            评论 ({{ commentTotal }})
          </h3>

          <!-- Comment Input -->
          <div v-if="userStore.isLoggedIn" class="comment-input-box">
            <el-input
              v-model="commentText"
              type="textarea"
              :rows="3"
              placeholder="写下你的评论..."
              maxlength="500"
              show-word-limit
            />
            <div class="comment-submit-row">
              <el-button type="primary" :loading="submitCommentLoading" @click="handleSubmitComment">
                发表评论
              </el-button>
            </div>
          </div>
          <el-alert
            v-else
            title="请登录后发表评论"
            type="info"
            show-icon
            :closable="false"
            class="login-alert"
          />

          <!-- Comment List -->
          <div v-if="commentList.length > 0" class="comment-list">
            <div
              v-for="comment in commentList"
              :key="comment.id"
              class="comment-item"
            >
              <el-avatar :size="36" :src="comment.userAvatar" class="comment-avatar" />
              <div class="comment-body">
                <div class="comment-header">
                  <span class="comment-user">{{ comment.userName }}</span>
                  <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                </div>
                <p class="comment-content">{{ comment.content }}</p>
                <div class="comment-actions">
                  <el-button text size="small" :icon="ChatDotRound" @click="toggleReplyBox(comment.id)">
                    回复
                  </el-button>
                </div>

                <!-- Reply Input Box -->
                <div v-if="replyTarget === comment.id" class="reply-input-box">
                  <el-input
                    v-model="replyText"
                    type="textarea"
                    :rows="2"
                    :placeholder="`回复 ${comment.userName}...`"
                    maxlength="300"
                  />
                  <div class="reply-buttons">
                    <el-button size="small" @click="cancelReply">取消</el-button>
                    <el-button size="small" type="primary" :loading="replyLoading" @click="handleSubmitReply(comment.id)">
                      回复
                    </el-button>
                  </div>
                </div>

                <!-- Replies -->
                <div v-if="comment.replies && comment.replies.length > 0" class="replies-list">
                  <div
                    v-for="reply in comment.replies"
                    :key="reply.id"
                    class="reply-item"
                  >
                    <el-avatar :size="28" :src="reply.userAvatar" class="reply-avatar" />
                    <div class="reply-body">
                      <div class="reply-header">
                        <span class="reply-user">{{ reply.userName }}</span>
                        <span v-if="reply.replyToUserName" class="reply-to">
                          回复 <span class="reply-to-name">@{{ reply.replyToUserName }}</span>
                        </span>
                        <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
                      </div>
                      <p class="reply-content">{{ reply.content }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <el-empty v-else-if="!loading" description="暂无评论，快来抢沙发吧~" />

          <!-- Comment Pagination -->
          <div v-if="commentTotal > commentPageSize" class="comment-pagination">
            <el-pagination
              v-model:current-page="commentPage"
              :page-size="commentPageSize"
              :total="commentTotal"
              layout="prev, pager, next"
              @change="fetchComments"
            />
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft, View, Star, ChatDotRound, Share, Collection
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import {
  getArticleDetail,
  toggleArticleLike,
  getArticleComments,
  createArticleComment
} from '@/api/article'
import { checkFavorite, toggleFavorite } from '@/api/user'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const categoryMap = {
  BEGINNER: '健身入门',
  NUTRITION: '营养饮食',
  TECHNIQUE: '训练技巧',
  EXPERT_SHARE: '达人分享',
  INJURY_PREVENTION: '伤病预防'
}

const loading = ref(false)
const article = ref(null)
const liked = ref(false)
const likeCount = ref(0)
const likeLoading = ref(false)
const favorited = ref(false)
const favLoading = ref(false)

const commentText = ref('')
const submitCommentLoading = ref(false)
const commentList = ref([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentPageSize = ref(10)

const replyTarget = ref(null)
const replyText = ref('')
const replyLoading = ref(false)

const renderedContent = computed(() => {
  if (!article.value || !article.value.content) return ''
  return marked(article.value.content)
})

const categoryTagType = computed(() => {
  const map = {
    BEGINNER: 'success',
    NUTRITION: 'warning',
    TECHNIQUE: 'primary',
    EXPERT_SHARE: 'danger',
    INJURY_PREVENTION: 'info'
  }
  return article.value ? (map[article.value.category] || '') : ''
})

function formatDateTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
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

function goBack() {
  router.push('/community')
}

async function fetchArticle() {
  loading.value = true
  try {
    const id = route.params.id
    const res = await getArticleDetail(id)
    // 后端返回 {article: {...}, author: {...}}，需要解构
    const data = res.data
    article.value = { ...data.article, author: data.author }
    likeCount.value = data.article.likeCount || 0
    liked.value = data.isLiked || false

    // Check favorite status
    if (userStore.isLoggedIn) {
      try {
        const favRes = await checkFavorite('ARTICLE', id)
        favorited.value = favRes.data?.favorited || false
      } catch {
        favorited.value = false
      }
    }

    fetchComments()
  } catch (error) {
    ElMessage.error('加载文章失败')
  } finally {
    loading.value = false
  }
}

async function handleToggleLike() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  likeLoading.value = true
  try {
    await toggleArticleLike(article.value.id)
    liked.value = !liked.value
    likeCount.value += liked.value ? 1 : -1
    ElMessage.success(liked.value ? '点赞成功' : '已取消点赞')
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    likeLoading.value = false
  }
}

async function handleToggleFavorite() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  favLoading.value = true
  try {
    await toggleFavorite('ARTICLE', article.value.id)
    favorited.value = !favorited.value
    ElMessage.success(favorited.value ? '收藏成功' : '已取消收藏')
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    favLoading.value = false
  }
}

function handleShare() {
  const url = window.location.href
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('链接已复制到剪贴板')
  }).catch(() => {
    ElMessage.info('请手动复制链接分享')
  })
}

async function fetchComments() {
  try {
    const res = await getArticleComments(article.value.id, {
      page: commentPage.value,
      size: commentPageSize.value
    })
    commentList.value = res.data.records || res.data.list || []
    commentTotal.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('加载评论失败')
  }
}

async function handleSubmitComment() {
  if (!commentText.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  submitCommentLoading.value = true
  try {
    await createArticleComment(article.value.id, {
      content: commentText.value.trim()
    })
    ElMessage.success('评论发表成功')
    commentText.value = ''
    commentPage.value = 1
    fetchComments()
  } catch (error) {
    ElMessage.error('评论发表失败')
  } finally {
    submitCommentLoading.value = false
  }
}

function toggleReplyBox(commentId) {
  replyTarget.value = replyTarget.value === commentId ? null : commentId
  replyText.value = ''
}

function cancelReply() {
  replyTarget.value = null
  replyText.value = ''
}

async function handleSubmitReply(parentId) {
  if (!replyText.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replyLoading.value = true
  try {
    await createArticleComment(article.value.id, {
      parentId: parentId,
      content: replyText.value.trim()
    })
    ElMessage.success('回复成功')
    replyText.value = ''
    replyTarget.value = null
    fetchComments()
  } catch (error) {
    ElMessage.error('回复失败')
  } finally {
    replyLoading.value = false
  }
}

onMounted(() => {
  fetchArticle()
})
</script>

<style scoped>
.article-detail-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.detail-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.back-row {
  margin-bottom: 16px;
}

.article-header {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 32px;
  margin-bottom: 20px;
}

.article-title {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 20px;
  line-height: 1.4;
}

.header-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.author-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.publish-time {
  font-size: 13px;
  color: #909399;
}

.header-stats {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
}

.cover-wrapper {
  margin-bottom: 20px;
  border-radius: 8px;
  overflow: hidden;
}

.cover-img {
  width: 100%;
  max-height: 400px;
}

.article-content {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 32px;
  margin-bottom: 20px;
  line-height: 1.8;
  font-size: 16px;
  color: #303133;
}

/* Markdown content styling */
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
}

.markdown-body :deep(p) {
  margin-bottom: 16px;
}

.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 4px;
}

.markdown-body :deep(blockquote) {
  border-left: 4px solid #667eea;
  padding-left: 16px;
  color: #606266;
  margin: 16px 0;
}

.markdown-body :deep(code) {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 14px;
}

.markdown-body :deep(pre) {
  background: #282c34;
  color: #abb2bf;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
}

.markdown-body :deep(pre code) {
  background: none;
  padding: 0;
}

.action-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.comments-section {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 24px 32px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 20px;
}

.comment-input-box {
  margin-bottom: 24px;
}

.comment-submit-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.login-alert {
  margin-bottom: 24px;
}

.comment-list {
  margin-top: 16px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.comment-user {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.comment-time {
  font-size: 12px;
  color: #c0c4cc;
}

.comment-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 8px;
}

.comment-actions {
  margin-bottom: 8px;
}

.reply-input-box {
  margin: 8px 0 12px;
}

.reply-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.replies-list {
  margin-top: 12px;
  padding-left: 24px;
  border-left: 2px solid #f0f0f0;
}

.reply-item {
  display: flex;
  gap: 10px;
  padding: 10px 0;
}

.reply-avatar {
  flex-shrink: 0;
}

.reply-body {
  flex: 1;
  min-width: 0;
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}

.reply-user {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.reply-to {
  font-size: 12px;
  color: #909399;
}

.reply-to-name {
  color: #667eea;
}

.reply-time {
  font-size: 12px;
  color: #c0c4cc;
}

.reply-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
  margin: 0;
}

.comment-pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
