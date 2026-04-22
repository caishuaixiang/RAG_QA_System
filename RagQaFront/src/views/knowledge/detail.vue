<template>
  <div class="knowledge-detail">
    <div class="page-header">
      <el-page-header @back="$router.back()">
        <template #content>
          <span class="page-title">{{ knowledge.name || '知识库详情' }}</span>
        </template>
      </el-page-header>
    </div>

    <el-row :gutter="20">
      <!-- 知识库信息 -->
      <el-col :span="24">
        <el-card class="info-card">
          <div class="knowledge-header">
            <div class="knowledge-icon">
              <el-icon><FolderOpened /></el-icon>
            </div>
            <div class="knowledge-info">
              <h2>{{ knowledge.name }}</h2>
              <p>{{ knowledge.description || '暂无描述' }}</p>
              <div class="knowledge-meta">
                <el-tag>{{ categoryMap[knowledge.category] || '未分类' }}</el-tag>
                <el-tag :type="knowledge.isPublic ? 'success' : 'info'">
                  {{ knowledge.isPublic ? '公开' : '私有' }}
                </el-tag>
                <span>创建时间：{{ knowledge.createTime }}</span>
              </div>
            </div>
            <div class="knowledge-actions">
              <el-button type="primary" @click="showUploadDialog">
                <el-icon><Upload /></el-icon>
                上传文档
              </el-button>
              <el-button @click="editKnowledge">
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
            </div>
          </div>

          <!-- 统计信息 -->
          <el-row :gutter="20" class="stats-row">
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-value">{{ stats.documentCount }}</div>
                <div class="stat-label">文档数量</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-value">{{ stats.chunkCount }}</div>
                <div class="stat-label">知识切片</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-value">{{ stats.qaCount }}</div>
                <div class="stat-label">问答次数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-value">{{ stats.totalSize }}</div>
                <div class="stat-label">总大小</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <!-- 文档列表 -->
      <el-col :span="24" style="margin-top: 20px;">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>文档列表</span>
              <el-input
                v-model="searchKeyword"
                placeholder="搜索文档"
                style="width: 200px;"
                clearable
                @keyup.enter="loadDocuments"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </div>
          </template>

          <el-table :data="documents" v-loading="loading" empty-text="暂无文档">
            <el-table-column prop="name" label="文档名称" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="doc-name">
                  <el-icon class="doc-icon"><Document /></el-icon>
                  <span>{{ row.name || row.originalName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ row.type?.toUpperCase() }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="size" label="大小" width="100">
              <template #default="{ row }">
                {{ formatSize(row.size) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="上传时间" width="180" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="previewDocument(row)">
                  预览
                </el-button>
                <el-button
                  type="primary"
                  link
                  @click="processDocument(row)"
                  v-if="row.status === 0"
                >
                  处理
                </el-button>
                <el-popconfirm
                  title="确定要删除此文档吗？"
                  @confirm="deleteDocument(row)"
                >
                  <template #reference>
                    <el-button type="danger" link>删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper" v-if="total > 0">
            <el-pagination
              v-model:current-page="pagination.page"
              v-model:page-size="pagination.size"
              :total="total"
              layout="total, prev, pager, next"
              @current-change="loadDocuments"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 上传对话框 -->
    <el-dialog
      v-model="uploadDialogVisible"
      title="上传文档"
      width="500px"
    >
      <el-upload
        ref="uploadRef"
        class="upload-area"
        drag
        :http-request="customUpload"
        multiple
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 PDF、Word、Excel、PPT、TXT 等格式，单个文件不超过 50MB
          </div>
        </template>
      </el-upload>
    </el-dialog>

    <!-- 文档预览对话框 -->
    <el-dialog
      v-model="previewDialogVisible"
      :title="previewDoc?.name"
      width="70%"
      top="5vh"
    >
      <div class="preview-content">
        <pre>{{ previewContent }}</pre>
      </div>
    </el-dialog>

    <!-- 编辑知识库对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑知识库"
      width="500px"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="80px"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入知识库描述"
          />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="editForm.category" placeholder="请选择分类">
            <el-option label="规章制度" value="regulation" />
            <el-option label="操作手册" value="manual" />
            <el-option label="技术文档" value="technical" />
            <el-option label="产品说明" value="product" />
            <el-option label="培训资料" value="training" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="editForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
        <el-form-item label="公开" prop="isPublic">
          <el-switch
            v-model="editForm.isPublic"
            :active-value="1"
            :inactive-value="0"
            active-text="公开"
            inactive-text="私有"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="saveEdit">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { knowledgeApi } from '@/api/knowledge'
import { documentApi } from '@/api/document'
import Cookies from 'js-cookie'
import {
  FolderOpened,
  Upload,
  Edit,
  Search,
  Document,
  UploadFilled
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const knowledgeId = computed(() => route.params.id)

const loading = ref(false)
const knowledge = ref({})
const documents = ref([])
const total = ref(0)
const searchKeyword = ref('')

const pagination = reactive({
  page: 1,
  size: 10
})

const stats = reactive({
  documentCount: 0,
  chunkCount: 0,
  qaCount: 0,
  totalSize: '0 B'
})

const categoryMap = {
  regulation: '规章制度',
  manual: '操作手册',
  technical: '技术文档',
  product: '产品说明',
  training: '培训资料',
  other: '其他'
}

// 上传相关
const uploadDialogVisible = ref(false)
const uploadRef = ref(null)

// 预览相关
const previewDialogVisible = ref(false)
const previewDoc = ref(null)
const previewContent = ref('')

// 编辑对话框
const editDialogVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref(null)
const editForm = reactive({
  id: null,
  name: '',
  description: '',
  category: '',
  status: 1,
  isPublic: 0
})

const editRules = {
  name: [
    { required: true, message: '请输入知识库名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ]
}

onMounted(() => {
  loadKnowledgeDetail()
  loadDocuments()
  loadStats()
})

// 加载知识库详情
const loadKnowledgeDetail = async () => {
  try {
    const res = await knowledgeApi.getKnowledgeDetail(knowledgeId.value)
    if (res.code === 200) {
      knowledge.value = res.data
      Object.assign(editForm, res.data)
    }
  } catch (error) {
    console.error('加载详情失败', error)
  }
}

// 加载统计
const loadStats = async () => {
  try {
    const res = await knowledgeApi.getKnowledgeStats(knowledgeId.value)
    if (res.code === 200) {
      stats.documentCount = res.data.documentCount || 0
      stats.chunkCount = res.data.chunkCount || 0
      stats.qaCount = res.data.qaCount || 0
      stats.totalSize = res.data.totalSize || '0 B'
    }
  } catch (error) {
    console.error('加载统计失败', error)
  }
}

// 加载文档列表
const loadDocuments = async () => {
  loading.value = true
  try {
    const res = await documentApi.getDocumentList({
      knowledgeDomain: String(knowledgeId.value),
      keyword: searchKeyword.value,
      ...pagination
    })
    if (res.code === 200) {
      documents.value = res.data?.list || res.data || []
      total.value = res.data?.total || 0
    }
  } catch (error) {
    console.error('加载文档失败', error)
  } finally {
    loading.value = false
  }
}

// 显示上传
const showUploadDialog = () => {
  uploadDialogVisible.value = true
}

// 自定义上传（核心修复！）
const customUpload = async (options) => {
  const file = options.file
  const formData = new FormData()
  formData.append('file', file)
  formData.append('knowledgeDomain', knowledgeId.value)

  try {
    await documentApi.uploadDocument(formData)
    ElMessage.success('上传成功')
    uploadDialogVisible.value = false
    loadDocuments()
    loadStats()
  } catch (err) {
    console.error(err)
    ElMessage.error('上传失败：' + (err.message || '未知错误'))
  }
}

// 预览文档
const previewDocument = async (row) => {
  try {
    const res = await documentApi.getDocumentContent(row.id)
    previewDoc.value = row
    previewContent.value = res.data
    previewDialogVisible.value = true
  } catch (error) {
    ElMessage.error('预览失败')
  }
}

// 处理文档（向量化）
const processDocument = async (row) => {
  try {
    await documentApi.processDocument(row.id)
    ElMessage.success('已开始处理，请稍候刷新查看')
    loadDocuments()
    loadStats()
  } catch (error) {
    ElMessage.error('处理失败')
  }
}

// 删除文档
const deleteDocument = async (row) => {
  try {
    await documentApi.deleteDocument(row.id)
    ElMessage.success('删除成功')
    loadDocuments()
    loadStats()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 编辑知识库
const editKnowledge = () => {
  Object.assign(editForm, knowledge.value)
  editDialogVisible.value = true
}

// 保存编辑
const saveEdit = async () => {
  if (!editFormRef.value) return

  editFormRef.value.validate(async (valid) => {
    if (!valid) return
    editLoading.value = true
    try {
      const res = await knowledgeApi.updateKnowledge(editForm.id, editForm)
      if (res.code === 200) {
        ElMessage.success('保存成功')
        editDialogVisible.value = false
        loadKnowledgeDetail()
      }
    } catch (error) {
      ElMessage.error('保存失败')
    } finally {
      editLoading.value = false
    }
  })
}

// 文件大小格式化
const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 状态样式
const getStatusType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'danger' }
  return map[status] || 'info'
}
const getStatusText = (status) => {
  const map = { 0: '待处理', 1: '已处理', 2: '处理失败' }
  return map[status] || '未知'
}
</script>

<style lang="scss" scoped>
.knowledge-detail {
  .page-header { margin-bottom: 20px; }
  .info-card {
    .knowledge-header {
      display: flex; align-items: flex-start;
      .knowledge-icon {
        width: 64px; height: 64px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border-radius: 16px; display: flex;
        align-items: center; justify-content: center;
        margin-right: 20px;
        .el-icon { font-size: 32px; color: #fff; }
      }
      .knowledge-info { flex: 1; h2 { margin-bottom: 8px; } p { color: #909399; margin-bottom: 12px; } }
      .knowledge-actions { display: flex; gap: 10px; }
    }
    .stats-row { padding-top: 20px; border-top: 1px solid #ebeef5; .stat-item { text-align: center; .stat-value { font-size: 28px; font-weight: 600; } .stat-label { color: #909399; margin-top: 4px; } } }
  }
  .card-header { display: flex; justify-content: space-between; align-items: center; }
  .doc-name { display: flex; align-items: center; gap: 8px; .doc-icon { color: #409eff; } }
  .pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 20px; }
  .upload-area :deep(.el-upload-dragger) { width: 100%; }
  .preview-content { max-height: 60vh; overflow: auto; pre { white-space: pre-wrap; word-wrap: break-word; } }
}
</style>