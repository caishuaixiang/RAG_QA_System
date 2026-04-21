<template>
  <div class="knowledge-list">
    <div class="page-header">
      <h1 class="page-title">知识库管理</h1>
      <el-button type="primary" @click="$router.push('/knowledge/create')">
        <el-icon><Plus /></el-icon>
        新建知识库
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="知识库名称">
          <el-input
            v-model="searchForm.name"
            placeholder="请输入知识库名称"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
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

    <!-- 知识库列表 -->
    <el-card class="list-card">
      <el-row :gutter="20" v-loading="loading">
        <el-col
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
          v-for="item in knowledgeList"
          :key="item.id"
          class="knowledge-item"
        >
          <el-card class="knowledge-card" shadow="hover" @click="viewDetail(item)">
            <div class="knowledge-icon">
              <el-icon><FolderOpened /></el-icon>
            </div>
            <div class="knowledge-info">
              <h3 class="knowledge-name">{{ item.name }}</h3>
              <p class="knowledge-desc">{{ item.description || '暂无描述' }}</p>
              <div class="knowledge-meta">
                <span>
                  <el-icon><Document /></el-icon>
                  {{ item.documentCount || 0 }} 个文档
                </span>
                <el-tag :type="item.status === 1 ? 'success' : 'info'" size="small">
                  {{ item.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </div>
            </div>
            <div class="knowledge-actions" @click.stop>
              <el-button type="primary" link @click="editKnowledge(item)">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-popconfirm
                title="确定要删除此知识库吗？"
                @confirm="deleteKnowledge(item)"
              >
                <template #reference>
                  <el-button type="danger" link>
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-if="!loading && knowledgeList.length === 0" description="暂无知识库">
        <el-button type="primary" @click="$router.push('/knowledge/create')">
          新建知识库
        </el-button>
      </el-empty>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[12, 24, 36, 48]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 编辑对话框 -->
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
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="editForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useKnowledgeStore } from '@/stores/knowledge'
import {
  Plus,
  Search,
  Refresh,
  FolderOpened,
  Document,
  Edit,
  Delete
} from '@element-plus/icons-vue'

const router = useRouter()
const knowledgeStore = useKnowledgeStore()

const loading = ref(false)
const knowledgeList = ref([])
const total = ref(0)

const searchForm = reactive({
  name: '',
  status: null
})

const pagination = reactive({
  page: 1,
  size: 12
})

// 编辑对话框
const editDialogVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref(null)
const editForm = reactive({
  id: null,
  name: '',
  description: '',
  status: 1
})

const editRules = {
  name: [
    { required: true, message: '请输入知识库名称', trigger: 'blur' },
    { min: 2, max: 50, message: '名称长度为2-50个字符', trigger: 'blur' }
  ]
}

onMounted(() => {
  loadKnowledgeList()
})

const loadKnowledgeList = async () => {
  loading.value = true
  try {
    const res = await knowledgeStore.fetchKnowledgeList({
      ...searchForm,
      ...pagination
    })
    if (res.success) {
      knowledgeList.value = knowledgeStore.knowledgeList
      total.value = knowledgeStore.total
    }
  } catch (error) {
    console.error('加载知识库列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadKnowledgeList()
}

const handleReset = () => {
  searchForm.name = ''
  searchForm.status = null
  handleSearch()
}

const handleSizeChange = () => {
  loadKnowledgeList()
}

const handlePageChange = () => {
  loadKnowledgeList()
}

const viewDetail = (item) => {
  router.push(`/knowledge/detail/${item.id}`)
}

const editKnowledge = (item) => {
  editForm.id = item.id
  editForm.name = item.name
  editForm.description = item.description
  editForm.status = item.status
  editDialogVisible.value = true
}

const saveEdit = async () => {
  if (!editFormRef.value) return

  editFormRef.value.validate(async (valid) => {
    if (!valid) return

    editLoading.value = true
    try {
      const res = await knowledgeStore.updateKnowledge(editForm.id, editForm)
      if (res.success) {
        ElMessage.success('保存成功')
        editDialogVisible.value = false
        loadKnowledgeList()
      } else {
        ElMessage.error(res.message || '保存失败')
      }
    } catch (error) {
      ElMessage.error('保存失败')
    } finally {
      editLoading.value = false
    }
  })
}

const deleteKnowledge = async (item) => {
  try {
    const res = await knowledgeStore.deleteKnowledge(item.id)
    if (res.success) {
      ElMessage.success('删除成功')
      loadKnowledgeList()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}
</script>

<style lang="scss" scoped>
.knowledge-list {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }

  .search-card {
    margin-bottom: 20px;
  }

  .knowledge-item {
    margin-bottom: 20px;
  }

  .knowledge-card {
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-4px);
    }

    .knowledge-icon {
      width: 48px;
      height: 48px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 12px;

      .el-icon {
        font-size: 24px;
        color: #fff;
      }
    }

    .knowledge-info {
      .knowledge-name {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
        margin-bottom: 8px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .knowledge-desc {
        font-size: 13px;
        color: #909399;
        margin-bottom: 12px;
        height: 40px;
        overflow: hidden;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        line-clamp: 2; 
        -webkit-box-orient: vertical;
      }

      .knowledge-meta {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 13px;
        color: #909399;

        span {
          display: flex;
          align-items: center;
          gap: 4px;
        }
      }
    }

    .knowledge-actions {
      display: flex;
      justify-content: flex-end;
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px solid #ebeef5;
    }
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
}
</style>