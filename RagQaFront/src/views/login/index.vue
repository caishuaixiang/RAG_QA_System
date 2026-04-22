<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <img src="@/assets/logo.svg" alt="logo" class="logo" />
        <h1 class="title">RAG智能问答系统</h1>
        <p class="subtitle">基于检索增强生成的知识问答平台</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>

        <div class="login-footer">
          <span>还没有账号？</span>
          <router-link to="/register" class="register-link">立即注册</router-link>
        </div>
      </el-form>
    </div>

    <div class="login-bg">
      <div class="bg-content">
        <h2>智能知识问答</h2>
        <p>上传文档，构建专属知识库</p>
        <p>智能检索，精准回答问题</p>
        <div class="features">
          <div class="feature-item">
            <el-icon><Document /></el-icon>
            <span>多格式文档支持</span>
          </div>
          <div class="feature-item">
            <el-icon><Search /></el-icon>
            <span>智能语义检索</span>
          </div>
          <div class="feature-item">
            <el-icon><ChatDotRound /></el-icon>
            <span>精准答案生成</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
// 🔥 只改这一行：把 @ 换成相对路径
import { useUserStore } from '../../stores/user'
import { ElMessage } from 'element-plus'
import { Document, Search, ChatDotRound } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const result = await userStore.login(loginForm)
        if (result.success) {
          ElMessage.success('登录成功')
          const redirect = route.query.redirect || '/dashboard'
          router.push(redirect)
        } else {
          ElMessage.error(result.message || '登录失败')
        }
      } catch (error) {
        ElMessage.error('登录失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.login-container {
  display: flex;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  background: #fff;
  padding: 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
    .login-header {
    text-align: center;
    margin-bottom: 40px;

    .logo {
      width: 60px;
      height: 60px;
      margin-bottom: 16px;
    }

    .title {
      font-size: 24px;
      color: #303133;
      margin-bottom: 8px;
    }

    .subtitle {
      font-size: 14px;
      color: #909399;
    }
  }

  .login-form {
    .login-btn {
      width: 100%;
    }
  }

  .login-footer {
    text-align: center;
    color: #909399;
    font-size: 14px;

    .register-link {
      color: #409eff;
      margin-left: 4px;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

.login-bg {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;

  .bg-content {
    text-align: center;
    padding: 40px;

    h2 {
      font-size: 36px;
      margin-bottom: 20px;
    }

    p {
      font-size: 18px;
      margin-bottom: 10px;
      opacity: 0.9;
    }

    .features {
      display: flex;
      gap: 30px;
      margin-top: 40px;

      .feature-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 10px;

        .el-icon {
          font-size: 32px;
        }

        span {
          font-size: 14px;
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
  }

  .login-card {
    width: 100%;
    min-height: 100vh;
  }

  .login-bg {
    display: none;
  }
}
</style>