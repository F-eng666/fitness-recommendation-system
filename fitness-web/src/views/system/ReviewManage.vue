<template>
  <div class="review-manage-page">
    <div class="manage-container">
      <h2 class="page-title">评价管理</h2>

      <!-- Review Table -->
      <div class="table-card">
        <el-table
          v-loading="loading"
          :data="reviewList"
          border
          stripe
          style="width: 100%"
          empty-text="暂无评价"
        >
          <el-table-column prop="userName" label="用户" min-width="100" />
          <el-table-column label="评价对象" min-width="150">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.targetType }}</el-tag>
              <span class="target-name">{{ row.targetName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="评分" width="150">
            <template #default="{ row }">
              <el-rate
                :model-value="row.rating"
                disabled
                show-score
                text-color="#ff9900"
                size="small"
              />
            </template>
          </el-table-column>
          <el-table-column label="评价内容" min-width="200">
            <template #default="{ row }">
              <span class="content-text" :title="row.content">
                {{ truncateText(row.content, 60) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="评价时间" min-width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="回复状态" width="100">
            <template #default="{ row }">
              <el-tag
                :type="row.replyContent ? 'success' : 'warning'"
                size="small"
              >
                {{ row.replyContent ? '已回复' : '待回复' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="140" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="!row.replyContent"
                text
                type="primary"
                size="small"
                @click="openReplyDialog(row)"
              >
                回复
              </el-button>
              <el-button
                v-else
                text
                type="warning"
                size="small"
                @click="openReplyDialog(row)"
              >
                编辑回复
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- Pagination -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @change="fetchReviewList"
          />
        </div>
      </div>
    </div>

    <!-- Reply Dialog -->
    <el-dialog
      v-model="replyDialogVisible"
      title="回复评价"
      width="550px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="reply-dialog-content">
        <!-- Original Review -->
        <div class="original-review">
          <h4 class="review-label">用户评价</h4>
          <div class="review-info">
            <span class="review-user">{{ replyTarget?.userName }}</span>
            <el-rate
              :model-value="replyTarget?.rating"
              disabled
              size="small"
            />
          </div>
          <p class="review-content">{{ replyTarget?.content }}</p>
        </div>

        <!-- Existing Reply (if any) -->
        <div v-if="replyTarget?.replyContent" class="existing-reply">
          <h4 class="review-label">已有回复</h4>
          <p class="reply-content-text">{{ replyTarget.replyContent }}</p>
        </div>

        <!-- Reply Textarea -->
        <el-form ref="replyFormRef" :model="replyForm" :rules="replyRules">
          <el-form-item label="回复内容" prop="content">
            <el-input
              v-model="replyForm.content"
              type="textarea"
              :rows="4"
              placeholder="请输入回复内容..."
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="replyLoading" @click="handleSubmitReply">
          {{ replyTarget?.replyContent ? '更新回复' : '提交回复' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getVenueReviews, replyReview } from '@/api/review'

const loading = ref(false)
const reviewList = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const replyDialogVisible = ref(false)
const replyTarget = ref(null)
const replyLoading = ref(false)
const replyFormRef = ref(null)
const replyForm = reactive({
  content: ''
})
const replyRules = {
  content: [{ required: true, message: '请输入回复内容', trigger: 'blur' }]
}

function truncateText(text, maxLen) {
  if (!text) return ''
  return text.length > maxLen ? text.slice(0, maxLen) + '...' : text
}

function formatDateTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

async function fetchReviewList() {
  loading.value = true
  try {
    const res = await getVenueReviews({ page: page.value, pageSize: pageSize.value })
    reviewList.value = res.data?.records || res.data?.list || []
    total.value = res.data?.total || 0
  } catch {
    ElMessage.error('加载评价列表失败')
  } finally {
    loading.value = false
  }
}

function openReplyDialog(row) {
  replyTarget.value = row
  replyForm.content = row.replyContent || ''
  replyDialogVisible.value = true
}

async function handleSubmitReply() {
  if (!replyFormRef.value) return
  try {
    await replyFormRef.value.validate()
  } catch {
    return
  }

  replyLoading.value = true
  try {
    await replyReview(replyTarget.value.id, { content: replyForm.content })
    ElMessage.success(replyTarget.value.replyContent ? '回复已更新' : '回复成功')
    replyDialogVisible.value = false
    fetchReviewList()
  } catch {
    ElMessage.error('回复失败')
  } finally {
    replyLoading.value = false
  }
}

onMounted(() => {
  fetchReviewList()
})
</script>

<style scoped>
.review-manage-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.manage-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 20px;
}

.table-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 24px;
}

.target-name {
  margin-left: 8px;
  font-size: 14px;
  color: #303133;
}

.content-text {
  cursor: default;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* Reply Dialog */
.original-review {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.review-label {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
}

.review-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.review-user {
  font-size: 14px;
  color: #606266;
}

.review-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  margin: 0;
}

.existing-reply {
  background: #f0f9eb;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.reply-content-text {
  font-size: 14px;
  color: #67c23a;
  line-height: 1.6;
  margin: 0;
}
</style>
