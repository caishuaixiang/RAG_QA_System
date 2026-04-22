<template>
  <div class="document-upload">
    <div class="page-header">
      <el-page-header @back="$router.back()">
        <template #content>
          <span class="page-title">上传文档</span>
        </template>
      </el-page-header>
    </div>

    <el-card class="upload-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="upload-form"
      >
        <el-form-item label="选择知识库" prop="knowledgeId">
          <el-select v-model="form.knowledgeId" placeholder="请选择知识库">
            <el-option
              v-for="item in knowledgeList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <div class="form-tip">选择知识库后，文档将上传到该知识库中</div>
        </el-form-item>

        <el-form-item label="上传文档" prop="files">
          <el-upload
            ref="uploadRef"
            class="upload-dragger"
            drag
            :auto-upload="false"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="fileList"
            multiple
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">
                支持的文件格式：PDF、Word、Excel、PPT、TXT、Markdown等<br>
                单个文件大小不超过50MB，一次最多上传10个文件
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="处理选项">
          <el-checkbox v-model="form.autoProcess">上传后自动处理</el-checkbox>
          <div class="form-tip">自动处理将解析文档内容、切片并向量化存储</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="uploading" :disabled="fileList.length === 0" @click="handleUpload">
            开始上传
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { knowledgeApi } from '@/api/knowledge'
import { documentApi } from '@/api/document'
import { UploadFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const formRef = ref(null)
const knowledgeList = ref([])
const fileList = ref([])
const uploading = ref(false)

const form = reactive({
  knowledgeId: null,
  autoProcess: true
})

const rules = {
  knowledgeId: [{ required: true, message: '请选择知识库', trigger: 'change' }]
}

onMounted(() => {
  loadKnowledgeList()
})

const loadKnowledgeList = async () => {
  try {
    const res = await knowledgeApi.getKnowledgeList({ page: 1, size: 100 })
    if (res.code === 200) {
      knowledgeList.value = res.data?.list || res.data || []
      if (knowledgeList.value.length > 0) {
        form.knowledgeId = knowledgeList.value[0].id
      }
    }
  } catch (error) {
    console.error('加载知识库列表失败', error)
  }
}

const handleFileChange = (file, files) => {
  if (file.size / 1024 / 1024 > 50) {
    ElMessage.error(`文件 ${file.name} 大小超过50MB`)
    return false
  }
  if (files.length > 10) {
    ElMessage.warning('一次最多上传10个文件')
    return false
  }
  fileList.value = files
}

const handleFileRemove = (file, files) => {
  fileList.value = files
}

const handleUpload = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid && fileList.value.length > 0) {
      uploading.value = true
      let successCount = 0
      for (const file of fileList.value) {
        const formData = new FormData()
        formData.append('file', file.raw)
        formData.append('knowledgeDomain', form.knowledgeId)
        // 添加 userId
        const userId = userStore.userInfo?.id
        if(userId){
          formData.append('userId',userId)
        }
        try {
          const res = await documentApi.uploadDocument(formData)
          if (res.code === 200) {
            if (form.autoProcess && res.data?.id) {
              await documentApi.processDocument(res.data.id)
            }
            successCount++
          }
        } catch (error) {
          console.error('上传失败', error)
        }
      }
      uploading.value = false
      if (successCount === fileList.value.length) {
        ElMessage.success('全部上传成功')
      } else if (successCount > 0) {
        ElMessage.warning(`成功上传${successCount}个文件`)
      } else {
        ElMessage.error('上传失败')
      }
    }
  })
}

const handleReset = () => {
  fileList.value = []
  formRef.value?.resetFields()
}
</script>

<style lang="scss" scoped>
.document-upload {
  .page-header { margin-bottom: 20px; }
  .upload-card { max-width: 800px; }
  .upload-dragger { width: 100%; :deep(.el-upload-dragger) { width: 100%; } }
  .form-tip { font-size: 12px; color: #909399; margin-top: 4px; }
}
</style>