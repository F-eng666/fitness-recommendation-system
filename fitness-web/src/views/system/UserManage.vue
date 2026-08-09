<template>
  <div class="user-manage-page">
    <div class="manage-container">
      <h2 class="page-title">用户管理</h2>

      <!-- Search & Filter -->
      <div class="filter-bar">
        <div class="search-row">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索用户名/昵称/手机号"
            clearable
            :prefix-icon="Search"
            class="search-input"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select
            v-model="roleFilter"
            placeholder="角色筛选"
            clearable
            class="filter-select"
            @change="handleSearch"
          >
            <el-option label="全部" value="" />
            <el-option label="管理员" value="admin" />
            <el-option label="普通用户" value="user" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="handleSearch">
            搜索
          </el-button>
        </div>
      </div>

      <!-- User Table -->
      <div class="table-card">
        <el-table
          v-loading="loading"
          :data="userList"
          border
          stripe
          style="width: 100%"
          empty-text="暂无用户"
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column prop="nickname" label="昵称" min-width="120" />
          <el-table-column prop="phone" label="手机号" min-width="130" />
          <el-table-column label="角色" width="100">
            <template #default="{ row }">
              <el-tag
                v-if="row.role === 'ADMIN'"
                type="danger"
                size="small"
              >
                管理员
              </el-tag>
              <el-tag
                v-else
                type="primary"
                size="small"
              >
                用户
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag
                :type="row.status === 1 ? 'success' : 'info'"
                size="small"
              >
                {{ row.status === 1 ? '正常' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="注册时间" min-width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="180" fixed="right">
            <template #default="{ row }">
              <el-switch
                :model-value="row.status === 1"
                :loading="toggleLoading === row.id"
                active-text="启用"
                inactive-text="禁用"
                inline-prompt
                @change="(val) => handleToggleStatus(row, val)"
              />
              <el-button
                text
                type="primary"
                size="small"
                style="margin-left: 12px"
                @click="openDetailDialog(row)"
              >
                详情
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
            @change="fetchUserList"
          />
        </div>
      </div>
    </div>

    <!-- User Detail Dialog -->
    <el-dialog
      v-model="detailDialogVisible"
      title="用户详情"
      width="500px"
      :close-on-click-modal="false"
    >
      <template v-if="detailUser">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ detailUser.id }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ detailUser.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ detailUser.nickname }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detailUser.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detailUser.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="性别">
            {{ detailUser.gender || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag v-if="detailUser.role === 'admin'" type="danger" size="small">管理员</el-tag>
            <el-tag v-else type="primary" size="small">用户</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detailUser.status === 'active' ? 'success' : 'info'" size="small">
              {{ detailUser.status === 'active' ? '正常' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间" :span="2">
            {{ formatDateTime(detailUser.createTime) }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getUserList, updateUserStatus } from '@/api/user'

const loading = ref(false)
const userList = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const roleFilter = ref('')
const toggleLoading = ref(null)

const detailDialogVisible = ref(false)
const detailUser = ref(null)

function formatDateTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

function handleSearch() {
  page.value = 1
  fetchUserList()
}

async function fetchUserList() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value
    }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (roleFilter.value) params.role = roleFilter.value

    const res = await getUserList(params)
    userList.value = res.data?.records || res.data?.list || []
    total.value = res.data?.total || 0
  } catch {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

async function handleToggleStatus(row, enable) {
  toggleLoading.value = row.id
  try {
    await updateUserStatus(row.id, enable ? 1 : 0)
    row.status = enable ? 1 : 0
    ElMessage.success(enable ? '用户已启用' : '用户已禁用')
  } catch {
    ElMessage.error('操作失败')
  } finally {
    toggleLoading.value = null
  }
}

function openDetailDialog(row) {
  detailUser.value = row
  detailDialogVisible.value = true
}

onMounted(() => {
  fetchUserList()
})
</script>

<style scoped>
.user-manage-page {
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

.filter-bar {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 16px 20px;
  margin-bottom: 20px;
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
  width: 140px;
}

.table-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 24px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
