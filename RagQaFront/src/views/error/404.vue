<template>
  <div class="error-page">
    <div class="error-content">
      <h1 class="error-code">404</h1>
      <h2 class="error-title">页面不存在</h2>
      <p class="error-desc">抱歉，您访问的页面不存在或已被移除</p >
      <el-button type="primary" @click="$router.push('/')">
        <el-icon><HomeFilled /></el-icon>
        返回首页
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { HomeFilled } from '@element-plus/icons-vue'
</script>

<style lang="scss" scoped>
.error-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  .error-content {
    text-align: center;
    color: #fff;
    .error-code {
      font-size: 120px;
      font-weight: 700;
      margin: 0;
      line-height: 1;
      text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
    }
    .error-title {
      font-size: 32px;
      margin: 20px 0 10px;
    }
    .error-desc {
      font-size: 16px;
      opacity: 0.8;
      margin-bottom: 30px;
    }
  }
}
</style>