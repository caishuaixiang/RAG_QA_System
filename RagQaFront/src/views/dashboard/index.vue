<template>
  <div class="dashboard">
    <div class="page-header">
      <h1 class="page-title">欢迎回来，{{ userStore.nickname }}</h1>
      <p class="page-subtitle">开始您的智能问答之旅</p >
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
            <el-icon><FolderOpened /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.knowledgeCount }}</div>
            <div class="stat-label">知识库</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.documentCount }}</div>
            <div class="stat-label">文档数量</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.qaCount }}</div>
            <div class="stat-label">问答次数</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.chunkCount }}</div>
            <div class="stat-label">知识切片</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <el-row :gutter="20" class="quick-actions">
      <el-col :xs="24" :md="12">
        <el-card class="action-card" shadow="hover" @click="$router.push('/qa')">
          <div class="action-content">
            <el-icon class="action-icon"><ChatDotRound /></el-icon>
            <div class="action-info">
              <h3>开始问答</h3>
              <p>基于知识库进行智能问答</p >
            </div>
            <el-icon class="action-arrow"><ArrowRight /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card class="action-card" shadow="hover" @click="$router.push('/document/upload')">
          <div class="action-content">
            <el-icon class="action-icon"><Upload /></el-icon>
            <div class="action-info">
              <h3>上传文档</h3>
              <p>上传文档到知识库</p >
            </div>
            <el-icon class="action-arrow"><ArrowRight /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近问答 -->
    <el-card class="recent-qa">
      <template #header>
        <div class="card-header">
          <span>最近问答</span>
          <el-button type="primary" link @click="$router.push('/history')">
            查看全部
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </template>
      <el-table :data="recentQA" v-loading="loading" empty-text="暂无问答记录">
        <el-table-column prop="question" label="问题" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewQA(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { qaApi } from '@/api/qa'
import { knowledgeApi } from '@/api/knowledge'
import { documentApi } from '@/api/document'
import {
  FolderOpened,
  Document,
  ChatDotRound,
  TrendCharts,
  ArrowRight,
  Upload
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const stats = reactive({
  knowledgeCount: 0,
  documentCount: 0,
  qaCount: 0,
  chunkCount: 0
})
const recentQA = ref([])

onMounted(() => {
  loadStats()
  loadRecentQA()
})

const loadStats = async () => {
  try {
    // 并行请求统计数据
    const [knowledgeRes, documentRes] = await Promise.all([
      knowledgeApi.getKnowledgeList({ page: 1, size: 1 }),
      documentApi.getDocumentList({ page: 1, size: 1 })
    ])

    if (knowledgeRes.code === 200) {
      stats.knowledgeCount = knowledgeRes.data?.total || 0
    }
    if (documentRes.code === 200) {
      stats.documentCount = documentRes.data?.total || 0
    }

    // 模拟数据
    stats.qaCount = 128
    stats.chunkCount = 3560
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

const loadRecentQA = async () => {
  loading.value = true
  try {
    const res = await qaApi.getHistory({ page: 1, size: 5 })
    if (res.code === 200) {
      recentQA.value = res.data?.list || []
    }
  } catch (error) {
    console.error('加载问答历史失败', error)
  } finally {
    loading.value = false
  }
}

const viewQA = (row) => {
  router.push(`/history?id=${row.id}`)
}
</script>

<style lang="scss" scoped>
.dashboard {
  .stat-cards {
    margin-bottom: 20px;

    .stat-card {
      background: #fff;
      border-radius: 8px;
      padding: 20px;
      display: flex;
      align-items: center;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

      .stat-icon {
        width: 56px;
        height: 56px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 16px;

        .el-icon {
          font-size: 28px;
          color: #fff;
        }
      }

      .stat-info {
        .stat-value {
          font-size: 28px;
          font-weight: 600;
          color: #303133;
        }

        .stat-label {
          font-size: 14px;
          color: #909399;
          margin-top: 4px;
        }
      }
    }
  }

  .quick-actions {
    margin-bottom: 20px;

    .action-card {
      cursor: pointer;
      margin-bottom: 10px;

      .action-content {
        display: flex;
        align-items: center;

        .action-icon {
          font-size: 40px;
          color: #409eff;
          margin-right: 16px;
        }

        .action-info {
          flex: 1;

          h3 {
            font-size: 18px;
            color: #303133;
            margin-bottom: 4px;
          }

          p {
            font-size: 14px;
            color: #909399;
          }
        }

        .action-arrow {
          font-size: 20px;
          color: #c0c4cc;
        }
      }

      &:hover {
        .action-arrow {
          color: #409eff;
          transform: translateX(4px);
          transition: all 0.3s;
        }
      }
    }
  }

  .recent-qa {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }
}
</style>