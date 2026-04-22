<template>
  <div class="history-page">
    <div class="page-header">
      <h1 class="page-title">问答历史</h1>
      <el-button type="danger" @click="clearAllHistory">
        <el-icon><Delete /></el-icon>
        清空历史
      </el-button>
    </div>

    <el-card>
      <el-table :data="historyList" v-loading="loading" empty-text="暂无问答记录">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content">
              <div class="answer-section">
                <div class="section-label">回答：</div>
                <div class="answer-text">{{ row.answer }}</div>
              </div>
              <div class="sources-section" v-if="row.source">
                <div class="section-label">来源：</div>
                <div class="sources-text">{{ formatSources(row.source) }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="question" label="问题" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">查看</el-button>
            <el-popconfirm title="确定要删除此记录吗？" @confirm="deleteHistory(row)">
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
          @current-change="loadHistory"
        />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="问答详情" width="60%">
      <div class="detail-content" v-if="currentQA">
        <div class="detail-item">
          <div class="detail-label">问题：</div>
          <div class="detail-value">{{ currentQA.question }}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">回答：</div>
          <div class="detail-value answer">{{ currentQA.answer }}</div>
        </div>
        <div class="detail-item" v-if="currentQA.source">
          <div class="detail-label">来源：</div>
          <div class="detail-value">{{ formatSources(currentQA.source) }}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">时间：</div>
          <div class="detail-value">{{ currentQA.createTime }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { qaApi } from '@/api/qa'
import { useUserStore } from '@/stores/user'
import { Delete } from '@element-plus/icons-vue'

const userStore = useUserStore()
const loading = ref(false)
const historyList = ref([])
const total = ref(0)
const detailDialogVisible = ref(false)
const currentQA = ref(null)

const pagination = reactive({ page: 1, size: 10 })

onMounted(() => {
  loadHistory()
})

const loadHistory = async () => {
  loading.value = true
  try {
    const res = await qaApi.getHistory({ ...pagination, userId: userStore.userInfo?.id })
    if (res.code === 200) {
      historyList.value = res.data?.list || []
      total.value = res.data?.total || 0
    }
  } catch (error) {
    console.error('加载历史记录失败', error)
  } finally {
    loading.value = false
  }
}

const viewDetail = (row) => {
  currentQA.value = row
  detailDialogVisible.value = true
}

const deleteHistory = async (row) => {
  try {
    const res = await qaApi.deleteQA(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadHistory()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const clearAllHistory = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有问答历史吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await qaApi.clearHistory(userStore.userInfo?.id)
    if (res.code === 200) {
      ElMessage.success('清空成功')
      historyList.value = []
      total.value = 0
    }
  } catch (error) {
    // 用户取消
  }
}

const formatSources = (source) => {
  try {
    const sources = typeof source === 'string' ? JSON.parse(source) : source
    return sources.map(s => s.document_name || '未知文档').join('、')
  } catch {
    return source
  }
}
</script>

<style lang="scss" scoped>
.history-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }
  .expand-content {
    padding: 16px 20px;
    background: #f5f7fa;
    .section-label {
      font-weight: 600;
      margin-bottom: 8px;
      color: #606266;
    }
    .answer-text {
      line-height: 1.6;
      white-space: pre-wrap;
    }
    .sources-section {
      margin-top: 16px;
    }
  }
  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
  .detail-content {
    .detail-item {
      margin-bottom: 20px;
      .detail-label {
        font-weight: 600;
        margin-bottom: 8px;
        color: #606266;
      }
      .detail-value {
        line-height: 1.6;
        &.answer {
          white-space: pre-wrap;
          background: #f5f7fa;
          padding: 12px;
          border-radius: 4px;
        }
      }
    }
  }
}
</style>