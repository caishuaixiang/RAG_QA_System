<template>
  <div class="document-list">
    <div class="page-header">
      <h1 class="page-title">文档管理</h1>
      <el-button type="primary" @click="$router.push('/document/upload')">
        <el-icon><Upload /></el-icon>
        上传文档
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="文档名称">
          <el-input
            v-model="searchForm.keyword"
            placeholder="请输入文档名称"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="知识库">
          <el-select v-model="searchForm.knowledgeId" placeholder="请选择知识库" clearable>
            <el-option
              v-for="item in knowledgeList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="待处理" :value="0" />
            <el-option label="已处理" :value="1" />
            <el-option label="处理失败" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 文档列表 -->
    <el-card class="list-card">
      <el-table
        :data="documents"
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="name" label="文档名称" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="doc-name">
              <el-icon class="doc-icon" :style="{ color: getFileColor(row.type) }">
                <Document />
              </el-icon>
              <span>{{ row.name || row.originalName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="knowledgeDomain" label="知识库" width="150" />
        <el-table-column prop="type" label="类型" width="80">
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

      <div class="table-footer">
        <div class="batch-actions">
          <el-button
            type="danger"
            :disabled="selectedDocs.length === 0"
            @click="batchDelete"
          >
            批量删除
          </el-button>
        </div>
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 文档预览对话框 -->
    <el-dialog
      v-model="previewDialogVisible"
      :title="previewDoc?.name"
      width="70%"
      top="5vh"
    >
      <div class="preview-content">
        <el-tabs v-model="previewTab">
          <el-tab-pane label="文档内容" name="content">
            <div class="content-box">
              <pre>{{ previewContent }}</pre>
            </div>
          </el-tab-pane>
          <el-tab-pane label="切片列表" name="chunks">
            <el-table :data="chunks" v-loading="chunksLoading" max-height="500">
              <el-table-column prop="chunkIndex" label="序号" width="80" />
              <el-table-column prop="chunkContent" label="内容" show-overflow-tooltip />
              <el-table-column prop="sectionTitle" label="章节" width="150" />
              <el-table-column prop="lineRange" label="行号" width="100" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { documentApi } from '@/api/document'
import { knowledgeApi } from '@/api/knowledge'
import { Document, Upload, Search, Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const documents = ref([])
const knowledgeList = ref([])
const total = ref(0)
const selectedDocs = ref([])

const searchForm = reactive({
  keyword: '',
  knowledgeId: null,
  status: null
})

const pagination = reactive({
  page: 1,
  size: 10
})

// 预览相关
const previewDialogVisible = ref(false)
const previewDoc = ref(null)
const previewContent = ref('')
const previewTab = ref('content')
const chunks = ref([])
const chunksLoading = ref(false)

onMounted(() => {
  loadKnowledgeList()
  loadDocuments()
})

const loadKnowledgeList = async () => {
  try {
    const res = await knowledgeApi.getKnowledgeList({ page: 1, size: 100 })
    if (res.code === 200) {
      knowledgeList.value = res.data?.list || res.data || []
    }
  } catch (error) {
    console.error('加载知识库列表失败', error)
  }
}

const loadDocuments = async () => {
  loading.value = true
  try {
    const params = {
      keyword: searchForm.keyword,
      status: searchForm.status,
      ...pagination
    }
    // 如果选择了知识库，传递 knowledgeDomain 参数
    if (searchForm.knowledgeId) {
      params.knowledgeDomain = String(searchForm.knowledgeId)
    }

    const res = await documentApi.getDocumentList(params)
    if (res.code === 200) {
      documents.value = res.data?.list || res.data || []
      total.value = res.data?.total || 0
    }
  } catch (error) {
    console.error('加载文档列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadDocuments()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.knowledgeId = null
  searchForm.status = null
  handleSearch()
}

const handleSizeChange = () => {
  loadDocuments()
}

const handlePageChange = () => {
  loadDocuments()
}

const handleSelectionChange = (selection) => {
  selectedDocs.value = selection
}

const previewDocument = async (row) => {
  previewDoc.value = row
  previewDialogVisible.value = true
  previewContent.value = '加载中...'
  previewTab.value = 'content'

  // 加载文档内容
  try {
    const res = await documentApi.getDocumentContent(row.id)
    if (res.code === 200) {
      previewContent.value = res.data?.content || '暂无内容'
    }
  } catch (error) {
    previewContent.value = '加载内容失败'
  }

  // 加载切片列表
  loadChunks(row.id)
}

const loadChunks = async (documentId) => {
  chunksLoading.value = true
  try {
    const res = await documentApi.getDocumentChunks(documentId, { page: 1, size: 100 })
    if (res.code === 200) {
      chunks.value = res.data?.list || []
    }
  } catch (error) {
    console.error('加载切片列表失败', error)
    chunks.value = []
  } finally {
    chunksLoading.value = false
  }
}

const processDocument = async (row) => {
  try {
    const res = await documentApi.processDocument(row.id)
    if (res.code === 200) {
      ElMessage.success('文档处理中，请稍后刷新查看')
      loadDocuments()
    } else {
      ElMessage.error(res.message || '处理失败')
    }
  } catch (error) {
    ElMessage.error('处理失败')
  }
}

const deleteDocument = async (row) => {
  try {
    const res = await documentApi.deleteDocument(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadDocuments()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const batchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedDocs.value.length} 个文档吗？`,
      '批量删除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )

    const ids = selectedDocs.value.map(doc => doc.id)
    const res = await documentApi.batchDeleteDocuments(ids)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadDocuments()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    // 用户取消
  }
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const getStatusType = (status) => {
  const types = { 0: 'warning', 1: 'success', 2: 'danger' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { 0: '待处理', 1: '已处理', 2: '处理失败' }
  return texts[status] || '未知'
}
const getFileColor = (type) => {
  const colors = {
    pdf: '#f56c6c',
    doc: '#409eff',
    docx: '#409eff',
    xls: '#67c23a',
    xlsx: '#67c23a',
    ppt: '#e6a23c',
    pptx: '#e6a23c',
    txt: '#909399'
  }
  return colors[type?.toLowerCase()] || '#909399'
}
</script>

<style lang="scss" scoped>
.document-list {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }

  .search-card {
    margin-bottom: 20px;
  }

  .doc-name {
    display: flex;
    align-items: center;
    gap: 8px;

    .doc-icon {
      font-size: 18px;
    }
  }

  .table-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 20px;
  }

  .preview-content {
    .content-box {
      max-height: 500px;
      overflow: auto;
      padding: 16px;
      background: #f5f7fa;
      border-radius: 4px;

      pre {
        white-space: pre-wrap;
        word-wrap: break-word;
        margin: 0;
        font-family: inherit;
        line-height: 1.6;
      }
    }
  }
}
</style>