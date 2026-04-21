<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <img src="@/assets/logo.svg" alt="logo" class="logo" />
        <h1 class="title">注册账号</h1>
        <p class="subtitle">创建您的RAG智能问答系统账号</p>
      </div>

      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        class="register-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="请输入邮箱"
            prefix-icon="Message"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请确认密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item prop="nickname">
          <el-input
            v-model="registerForm.nickname"
            placeholder="请输入昵称（选填）"
            prefix-icon="UserFilled"
            size="large"
          />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="registerForm.agree">
            我已阅读并同意
            <el-link type="primary" @click.prevent="showAgreement('user')">用户协议</el-link>
            和
            <el-link type="primary" @click.prevent="showAgreement('privacy')">隐私政策</el-link>
          </el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="register-btn"
            @click="handleRegister"
          >
            注 册
          </el-button>
        </el-form-item>

        <div class="register-footer">
          <span>已有账号？</span>
          <router-link to="/login" class="login-link">立即登录</router-link>
        </div>
      </el-form>

      <!-- 协议对话框 -->
      <el-dialog
        v-model="agreementDialog.visible"
        :title="agreementDialog.title"
        width="600px"
      >
        <div class="agreement-content" v-html="agreementDialog.content"></div>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
// ------------ 这里修复了 ------------
import { useUserStore } from '../../stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const registerFormRef = ref(null)
const loading = ref(false)

// 表单数据
const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  agree: false
})

// 表单验证规则
const registerRules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  agree: [
    {
      validator: (rule, value, callback) => {
        if (!value) {
          callback(new Error('请阅读并同意用户协议和隐私政策'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
})

// 协议弹窗
const agreementDialog = reactive({
  visible: false,
  title: '',
  content: ''
})

const agreements = {
  user: {
    title: '用户协议',
    content: `
      <h4>一、服务条款</h4>
      <p>欢迎使用RAG智能问答系统。在使用本系统前，请您仔细阅读以下服务条款。</p>
      <h4>二、用户注册</h4>
      <p>1. 用户应按照注册页面提示完成注册流程。</p>
      <p>2. 用户应提供真实、准确、完整的个人资料。</p>
      <p>3. 用户应妥善保管账号和密码，因账号密码泄露造成的损失由用户自行承担。</p>
      <h4>三、使用规范</h4>
      <p>1. 用户不得利用本系统从事违法违规活动。</p>
      <p>2. 用户不得干扰本系统的正常运行。</p>
      <p>3. 用户上传的内容不得侵犯他人知识产权或其他合法权益。</p>
      <h4>四、免责声明</h4>
      <p>本系统提供的问答结果仅供参考，不构成任何形式的建议或承诺。</p>
    `
  },
  privacy: {
    title: '隐私政策',
    content: `
      <h4>一、信息收集</h4>
      <p>我们收集您注册时提供的个人信息，包括用户名、邮箱等。</p>
      <h4>二、信息使用</h4>
      <p>1. 我们使用您的信息来提供、维护和改进我们的服务。</p>
      <p>2. 我们不会将您的个人信息出售或出租给第三方。</p>
      <h4>三、信息安全</h4>
      <p>我们采取合理的安全措施来保护您的个人信息，包括数据加密存储等。</p>
      <h4>四、Cookie使用</h4>
      <p>我们使用Cookie来改善用户体验，您可以通过浏览器设置管理Cookie。</p>
      <h4>五、信息更新</h4>
      <p>您有权访问、更正或删除您的个人信息。如有需要，请联系管理员。</p>
    `
  }
}

// 显示协议
const showAgreement = (type) => {
  agreementDialog.title = agreements[type].title
  agreementDialog.content = agreements[type].content
  agreementDialog.visible = true
}

// 注册提交
const handleRegister = async () => {
  const valid = await registerFormRef.value.validate()
  if (!valid) return

  try {
    loading.value = true
    // 调用注册接口
    await userStore.register(registerForm)
    ElMessage.success('注册成功')
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.register-card {
  width: 450px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo {
  width: 60px;
  height: 60px;
  margin-bottom: 16px;
}

.title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.register-form {
  margin-top: 20px;
}

.register-btn {
  width: 100%;
  margin-top: 10px;
}

.register-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
}

.login-link {
  color: #409eff;
  margin-left: 4px;
  cursor: pointer;
  text-decoration: none;
}

.login-link:hover {
  text-decoration: underline;
}

.agreement-content {
  max-height: 400px;
  overflow-y: auto;
  line-height: 1.6;
}
</style>