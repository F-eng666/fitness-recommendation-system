<template>
  <div class="article-write-page">
    <div class="write-container">
      <!-- Page Header -->
      <div class="page-header">
        <el-button text :icon="ArrowLeft" @click="goBack">返回</el-button>
        <h2 class="page-title">{{ isEdit ? '编辑文章' : '写文章' }}</h2>
      </div>

      <div v-loading="pageLoading" class="write-form-card">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
          label-position="top"
        >
          <!-- Title -->
          <el-form-item label="文章标题" prop="title">
            <el-input
              v-model="form.title"
              placeholder="请输入文章标题"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>

          <!-- Category -->
          <el-form-item label="文章分类" prop="category">
            <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
              <el-option label="健身入门" value="健身入门" />
              <el-option label="营养饮食" value="营养饮食" />
              <el-option label="训练技巧" value="训练技巧" />
              <el-option label="达人分享" value="达人分享" />
              <el-option label="伤病预防" value="伤病预防" />
            </el-select>
          </el-form-item>

          <!-- Tags -->
          <el-form-item label="文章标签" prop="tags">
            <el-input
              v-model="form.tags"
              placeholder="多个标签用逗号分隔，如：增肌,饮食,新手"
            />
          </el-form-item>

          <!-- Cover Image URL -->
          <el-form-item label="封面图片URL">
            <el-input v-model="form.coverImage" placeholder="请输入封面图片链接（可选）" />
            <div v-if="form.coverImage" class="cover-preview">
              <el-image
                :src="form.coverImage"
                fit="cover"
                class="preview-img"
                @error="onCoverError"
              >
                <template #error>
                  <span>图片加载失败</span>
                </template>
              </el-image>
            </div>
          </el-form-item>

          <!-- Summary -->
          <el-form-item label="文章摘要" prop="summary">
            <el-input
              v-model="form.summary"
              type="textarea"
              :rows="3"
              placeholder="简要描述文章内容，将显示在文章列表中"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>

          <!-- Status -->
          <el-form-item label="发布状态">
            <el-radio-group v-model="form.status">
              <el-radio value="published">发布</el-radio>
              <el-radio value="draft">保存为草稿</el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- Content Editor -->
          <el-form-item label="文章内容" prop="content">
            <div class="editor-tabs">
              <el-radio-group
                v-model="editorMode"
                size="small"
                class="mode-switch"
              >
                <el-radio-button value="edit">编辑</el-radio-button>
                <el-radio-button value="preview">预览</el-radio-button>
              </el-radio-group>
            </div>

            <!-- Edit Mode -->
            <div v-show="editorMode === 'edit'" class="editor-area">
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="16"
                placeholder="支持Markdown格式编写文章内容..."
              />
              <p class="editor-hint">支持Markdown格式</p>
            </div>

            <!-- Preview Mode -->
            <div
              v-show="editorMode === 'preview'"
              class="preview-area markdown-body"
            >
              <div v-if="form.content" v-html="previewHtml"></div>
              <el-empty v-else description="暂无内容可预览" />
            </div>
          </el-form-item>

          <!-- Action Buttons -->
          <div class="form-actions">
            <el-button @click="goBack">取消</el-button>
            <el-button
              type="primary"
              :loading="submitLoading"
              @click="handleSaveDraft"
            >
              保存草稿
            </el-button>
            <el-button
              type="primary"
              :loading="submitLoading"
              @click="handlePublish"
            >
              发布文章
            </el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import { createArticle, updateArticle, getArticleDetail } from '@/api/article'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)

const pageLoading = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const editorMode = ref('edit')

const form = reactive({
  title: '',
  category: '',
  tags: '',
  coverImage: '',
  summary: '',
  content: '',
  status: 'published'
})

const rules = {
  title: [{ required: true, message: '请输入文章标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择文章分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入文章内容', trigger: 'blur' }]
}

const previewHtml = computed(() => {
  if (!form.content) return ''
  return marked(form.content)
})

function onCoverError() {
  // silently ignore broken cover images
}

function goBack() {
  router.back()
}

async function loadArticle() {
  if (!isEdit.value) return
  pageLoading.value = true
  try {
    const res = await getArticleDetail(route.params.id)
    const article = res.data
    form.title = article.title || ''
    form.category = article.category || ''
    form.tags = Array.isArray(article.tags) ? article.tags.join(',') : (article.tags || '')
    form.coverImage = article.coverImage || ''
    form.summary = article.summary || ''
    form.content = article.content || ''
    form.status = article.status || 'published'
  } catch (error) {
    ElMessage.error('加载文章数据失败')
  } finally {
    pageLoading.value = false
  }
}

async function doSubmit(status) {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  form.status = status

  try {
    const payload = {
      ...form,
      tags: form.tags ? form.tags.split(',').map(t => t.trim()).filter(Boolean) : []
    }

    if (isEdit.value) {
      await updateArticle(route.params.id, payload)
      ElMessage.success('文章更新成功')
    } else {
      await createArticle(payload)
      ElMessage.success(status === 'published' ? '文章发布成功' : '草稿保存成功')
    }

    router.push('/community')
  } catch (error) {
    ElMessage.error('操作失败，请重试')
  } finally {
    submitLoading.value = false
  }
}

function handlePublish() {
  doSubmit('published')
}

function handleSaveDraft() {
  doSubmit('draft')
}

onMounted(() => {
  loadArticle()
})
</script>

<style scoped>
.article-write-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.write-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.write-form-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 32px;
}

.cover-preview {
  margin-top: 12px;
}

.preview-img {
  max-width: 300px;
  max-height: 180px;
  border-radius: 6px;
  border: 1px solid #e8e8e8;
}

.editor-tabs {
  margin-bottom: 12px;
}

.mode-switch {
  margin-bottom: 0;
}

.editor-area {
  position: relative;
}

.editor-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #909399;
}

.preview-area {
  min-height: 300px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 20px;
  background: #fafafa;
  line-height: 1.8;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin-top: 20px;
  margin-bottom: 12px;
}

.markdown-body :deep(p) {
  margin-bottom: 12px;
}

.markdown-body :deep(blockquote) {
  border-left: 4px solid #667eea;
  padding-left: 16px;
  color: #606266;
}

.markdown-body :deep(code) {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 3px;
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

.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 4px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}
</style>
