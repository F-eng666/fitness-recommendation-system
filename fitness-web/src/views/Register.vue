<template>
  <div class="redirect-placeholder">
    <p>正在跳转到登录页面...</p>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
router.replace('/login')
</script>

<style scoped>
.redirect-placeholder {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #909399;
  font-size: 16px;
}
</style>
