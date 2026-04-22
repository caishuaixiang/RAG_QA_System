<template>
  <div class="knowledge-create">
    <div class="page-header">
      <el-page-header @back="$router.back()">
        <template #content>
          <span class="page-title">新建知识库</span>
        </template>
      </el-page-header>
    </div>

    <el-card class="form-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="create-form"
      >
        <el-form-item label="知识库名称" prop="name">
          <el-input
            v-model="form.name"
            placeholder="请输入知识库名称，如：学生手册"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="知识库描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入知识库描述，简要说明知识库的内容和用途"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="知识库分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类">
            <el-option label="规章制度" value="regulation" />
            <el-option label="操作手册" value="manual" />
            <el-option label="技术文档" value="technical" />
            <el-option label="产品说明" value="product" />
            <el-option label="培训资料" value="training" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>

        <el-form-item label="标签" prop="tags">
          <el-select
            v-model="form.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入标签后按回车添加"
          />
        </el-form-item>

        <el-form-item label="访问权限" prop="isPublic">
          <el-radio-group v-model="form.isPublic">
            <el-radio :label="1">公开</el-radio>
            <el-radio :label="0">私有</el-radio>
          </el-radio-group>
          <div class="form-tip">
            公开知识库对所有用户可见，私有知识库仅自己可见
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">
            创建知识库
          </el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { knowledgeApi } from '@/api/knowledge'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  name: '',
  description: '',
  category: '',
  tags: [],
  isPublic: 0
})

const rules = {
  name: [
    { required: true, message: '请输入知识库名称', trigger: 'blur' },
    { min: 2, max: 50, message: '名称长度为2-50个字符', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择知识库分类', trigger: 'change' }
  ]
}

const handleSubmit = async () => {
  if (!formRef.value) return

  if(!userStore.userInfo?.id){
    ElMessage.error('请先登录')
    return
  }

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await knowledgeApi.createKnowledge({
          userId: userStore.userInfo.id,
          name: form.name,
          description: form.description,
          category: form.category,
          tags: form.tags.join(','),
          isPublic: form.isPublic
        })
        if (res.code === 200) {
          ElMessage.success('创建成功')
          router.push('/knowledge/list')
        } else {
          ElMessage.error(res.message || '创建失败')
        }
      } catch (error) {
        ElMessage.error('创建失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.knowledge-create {
  .page-header {
    margin-bottom: 20px;
  }

  .form-card {
    max-width: 600px;
  }

  .create-form {
    .form-tip {
      font-size: 12px;
      color: #909399;
      margin-top: 4px;
    }
  }
}
</style>