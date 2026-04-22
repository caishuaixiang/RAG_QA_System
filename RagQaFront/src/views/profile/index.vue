<template>
  <div class="profile-page">
    <div class="page-header">
      <h1 class="page-title">个人中心</h1>
    </div>

    <el-row :gutter="20">
      <!-- 个人信息卡片 -->
      <el-col :span="8">
        <el-card class="user-card">
          <div class="user-avatar">
            <el-avatar :size="100" :icon="UserFilled" />
            <el-button type="primary" link class="change-avatar">
              更换头像
            </el-button>
          </div>
          <div class="user-info">
            <h2>{{ userStore.nickname }}</h2>
            <p class="user-role">
              <el-tag :type="userStore.isAdmin ? 'danger' : 'info'">
                {{ userStore.isAdmin ? '管理员' : '普通用户' }}
              </el-tag>
            </p >
            <div class="user-stats">
              <div class="stat-item">
                <div class="stat-value">{{ stats.qaCount }}</div>
                <div class="stat-label">问答次数</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ stats.knowledgeCount }}</div>
                <div class="stat-label">知识库</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ stats.documentCount }}</div>
                <div class="stat-label">文档</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 信息编辑 -->
      <el-col :span="16">
        <el-card class="info-card">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="基本信息" name="info">
              <el-form
                ref="infoFormRef"
                :model="infoForm"
                :rules="infoRules"
                label-width="80px"
                class="info-form"
              >
                <el-form-item label="用户名">
                  <el-input v-model="userStore.userInfo.username" disabled />
                </el-form-item>
                <el-form-item label="昵称" prop="nickname">
                  <el-input v-model="infoForm.nickname" placeholder="请输入昵称" />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="infoForm.email" placeholder="请输入邮箱" />
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="infoForm.phone" placeholder="请输入手机号" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="infoLoading" @click="saveInfo">
                    保存修改
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="修改密码" name="password">
              <el-form
                ref="passwordFormRef"
                :model="passwordForm"
                :rules="passwordRules"
                label-width="100px"
                class="password-form"
              >
                <el-form-item label="当前密码" prop="oldPassword">
                  <el-input
                    v-model="passwordForm.oldPassword"
                    type="password"
                    placeholder="请输入当前密码"
                    show-password
                  />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input
                    v-model="passwordForm.newPassword"
                    type="password"
                    placeholder="请输入新密码"
                    show-password
                  />
                </el-form-item>
                <el-form-item label="确认新密码" prop="confirmPassword">
                  <el-input
                    v-model="passwordForm.confirmPassword"
                    type="password"
                    placeholder="请确认新密码"
                    show-password
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="passwordLoading" @click="changePassword">
                    修改密码
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="账号注销" name="delete">
              <div class="delete-account">
                <el-alert
                  title="警告：账号注销后将无法恢复"
                  type="error"
                  :closable="false"
                  show-icon
                >
                  <p>账号注销后，以下数据将被永久删除：</p >
                  <ul>
                    <li>您的所有个人信息</li>
                    <li>您创建的所有知识库</li>
                    <li>您上传的所有文档</li>
                    <li>您的所有问答记录</li>
                  </ul>
                </el-alert>
                <div class="delete-actions">
                  <el-button type="danger" @click="showDeleteConfirm">
                    申请注销账号
                  </el-button>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'
import { UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('info')
const infoFormRef = ref(null)
const passwordFormRef = ref(null)
const infoLoading = ref(false)
const passwordLoading = ref(false)

const stats = reactive({
  qaCount: 0,
  knowledgeCount: 0,
  documentCount: 0
})

const infoForm = reactive({
  nickname: '',
  email: '',
  phone: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const infoRules = {
  nickname: [
    { min: 2, max: 20, message: '昵称长度为2-20个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

onMounted(() => {
  if (userStore.userInfo) {
    infoForm.nickname = userStore.userInfo.nickname || ''
    infoForm.email = userStore.userInfo.email || ''
    infoForm.phone = userStore.userInfo.phone || ''
    loadStats()
  }
})

const loadStats = async () => {
  try {
    if (userStore.userInfo?.id) {
      const res = await userApi.getUserStats(userStore.userInfo.id)
      if (res.code === 200) {
        stats.qaCount = res.data?.qaCount || 0
        stats.knowledgeCount = res.data?.knowledgeCount || 0
        stats.documentCount = res.data?.documentCount || 0
      }
    }
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

const saveInfo = async () => {
  if (!infoFormRef.value) return

  await infoFormRef.value.validate(async (valid) => {
    if (valid) {
      infoLoading.value = true
      try {
        const result = await userStore.updateUserInfo({
          nickname: infoForm.nickname,
          email: infoForm.email,
          phone: infoForm.phone
        })
        if (result.success) {
          ElMessage.success('保存成功')
        } else {
          ElMessage.error(result.message || '保存失败')
        }
      } catch (error) {
        ElMessage.error('保存失败')
      } finally {
        infoLoading.value = false
      }
    }
  })
}

const changePassword = async () => {
  if (!passwordFormRef.value) return

  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      passwordLoading.value = true
      try {
        const result = await userStore.changePassword({
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword
        })
        if (result.success) {
          ElMessage.success('密码修改成功，请重新登录')
          userStore.logout()
          router.push('/login')
        } else {
          ElMessage.error(result.message || '修改失败')
        }
      } catch (error) {
        ElMessage.error('修改失败')
      } finally {
        passwordLoading.value = false
      }
    }
  })
}

const showDeleteConfirm = () => {
  ElMessageBox.prompt('请输入"确认注销"以继续', '账号注销确认', {
    confirmButtonText: '确认注销',
    cancelButtonText: '取消',
    inputPattern: /^确认注销$/,
    inputErrorMessage: '请输入"确认注销"'
  }).then(({ value }) => {
    deleteAccount()
  }).catch(() => {})
}

const deleteAccount = async () => {
  try {
    const result = await userStore.deleteAccount()
    if (result.success) {
      ElMessage.success('账号已注销')
      router.push('/login')
    } else {
      ElMessage.error(result.message || '注销失败')
    }
  } catch (error) {
    ElMessage.error('注销失败')
  }
}
</script>

<style lang="scss" scoped>
.profile-page {
  .user-card {
    text-align: center;

    .user-avatar {
      padding: 20px 0;

      .change-avatar {
        display: block;
        margin-top: 10px;
      }
    }

    .user-info {
      h2 {
        font-size: 20px;
        margin-bottom: 8px;
      }

      .user-stats {
        display: flex;
        justify-content: space-around;
        margin-top: 20px;
        padding-top: 20px;
        border-top: 1px solid #ebeef5;

        .stat-item {
          .stat-value {
            font-size: 24px;
            font-weight: 600;
            color: #303133;
          }

          .stat-label {
            font-size: 13px;
            color: #909399;
            margin-top: 4px;
          }
        }
      }
    }
  }

  .info-card {
    .info-form,
    .password-form {
      max-width: 500px;
      margin-top: 20px;
    }
  }

  .delete-account {
    padding: 20px 0;

    ul {
      margin: 10px 0;
      padding-left: 20px;

      li {
        margin: 5px 0;
        color: #f56c6c;
      }
    }

    .delete-actions {
      margin-top: 20px;
    }
  }
}
</style>