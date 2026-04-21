<template>
  <div class="qa-page">
    <div class="qa-container">
      <!-- 问答区域 -->
      <div class="qa-main">
        <div class="qa-header">
          <h1>智能问答</h1>
          <p>基于知识库的智能问答系统，为您精准解答问题</p >
        </div>

        <!-- 对话区域 -->
        <div class="chat-container" ref="chatContainer">
          <div class="chat-messages">
            <div
              v-for="(msg, index) in messages"
              :key="index"
              :class="['message', msg.role]"
            >
              <div class="message-avatar">
                <el-avatar v-if="msg.role === 'user'" :icon="UserFilled" />
                <el-avatar v-else class="ai-avatar">
                  <el-icon><ChatDotRound /></el-icon>
                </el-avatar>
              </div>
              <div class="message-content">
                <div class="message-text" v-html="formatMessage(msg.content)"></div>
                <div class="message-sources" v-if="msg.sources && msg.sources.length > 0">
                  <div class="sources-header">
                    <el-icon><Document /></el-icon>
                    <span>答案来源</span>
                  </div>
                  <div class="sources-list">
                    <div
                      v-for="(source, sIdx) in msg.sources"
                      :key="sIdx"
                      class="source-item"
                    >
                      <div class="source-name">{{ source.document_name }}</div>
                      <div class="source-location" v-if="source.location">
                        <span v-if="source.location.page_number">第{{ source.location.page_number }}页</span>
                        <span v-if="source.location.section_title"> / {{ source.location.section_title }}</span>
                        <span v-if="source.location.line_range"> / 第{{ source.location.line_range }}行</span>
                      </div>
                      <div class="source-similarity">
                        相关度：{{ source.similarity }}%
                      </div>
                    </div>
                  </div>
                </div>
                <div class="message-time">{{ msg.time }}</div>
              </div>
            </div>

            <div v-if="loading" class="message assistant loading">
              <div class="message-avatar">
                <el-avatar class="ai-avatar">
                  <el-icon><ChatDotRound /></el-icon>
                </el-avatar>
              </div>
              <div class="message-content">
                <div class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-container">
          <el-select
            v-model="selectedKnowledge"
            placeholder="选择知识库"
            class="knowledge-select"
          >
            <el-option
              v-for="item in knowledgeList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <div class="input-wrapper">
            <el-input
              v-model="question"
              type="textarea"
              :rows="2"
              placeholder="请输入您的问题..."
              @keyup.enter.ctrl="handleSubmit"
            />
            <el-button
              type="primary"
              :loading="loading"
              :disabled="!question.trim()"
              @click="handleSubmit"
            >
              <el-icon><Promotion /></el-icon>
              发送
            </el-button>
          </div>
          <div class="input-tip">按 Ctrl + Enter 快速发送</div>
        </div>
      </div>

      <!-- 侧边栏 -->
      <div class="qa-sidebar">
        <el-card class="history-card">
          <template #header>
            <div class="card-header">
              <span>历史记录</span>
              <el-button type="primary" link @click="clearHistory">
                清空
              </el-button>
            </div>
          </template>
          <div class="history-list">
            <div
              v-for="item in historyList"
              :key="item.id"
              class="history-item"
              @click="selectHistoryItem(item)"
            >
              <div class="history-question">{{ item.question }}</div>
              <div class="history-time">{{ item.createTime }}</div>
            </div>
            <el-empty v-if="historyList.length === 0" description="暂无历史记录" />
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { qaApi } from '@/api/qa'
import { knowledgeApi } from '@/api/knowledge'
import { useUserStore } from '@/stores/user'
import {
  UserFilled,
  ChatDotRound,
  Document,
  Promotion
} from '@element-plus/icons-vue'

const userStore = useUserStore()

const question = ref('')
const selectedKnowledge = ref(null)
const loading = ref(false)
const messages = ref([])
const historyList = ref([])
const knowledgeList = ref([])
const chatContainer = ref(null)

onMounted(() => {
  loadKnowledgeList()
  loadHistory()
})

const loadKnowledgeList = async () => {
  try {
    const res = await knowledgeApi.getKnowledgeList({ page: 1, size: 100 })
    if (res.code === 200) {
      knowledgeList.value = res.data?.list || res.data || []
      if (knowledgeList.value.length > 0) {
        selectedKnowledge.value = knowledgeList.value[0].id
      }
    }
  } catch (error) {
    console.error('加载知识库列表失败', error)
  }
}

const loadHistory = async () => {
  try {
    const res = await qaApi.getHistory({ page: 1, size: 10 })
    if (res.code === 200) {
      historyList.value = res.data?.list || []
    }
  } catch (error) {
    console.error('加载历史记录失败', error)
  }
}

const handleSubmit = async () => {
  if (!question.value.trim() || loading.value) return

  const userQuestion = question.value.trim()
  question.value = ''

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: userQuestion,
    time: formatTime(new Date())
  })

  scrollToBottom()
  loading.value = true

  try {
    const res = await qaApi.askQuestion({
      question: userQuestion,
      userId: userStore.userInfo?.id,
      knowledgeId: selectedKnowledge.value
    })

    if (res.code === 200) {
      const data = res.data
      messages.value.push({
        role: 'assistant',
        content: data.answer,
        sources: data.sources,
        time: formatTime(new Date())
      })
    } else {
      messages.value.push({
        role: 'assistant',
        content: '抱歉，我无法回答您的问题。请稍后再试。',
        time: formatTime(new Date())
      })
    }
  } catch (error) {
    messages.value.push({
      role: 'assistant',
      content: '抱歉，服务出现异常，请稍后再试。',
      time: formatTime(new Date())
    })
  } finally {
    loading.value = false
    scrollToBottom()
    loadHistory()
  }
}

const selectHistoryItem = (item) => {
  // 加载历史问答
  messages.value = [
    {
      role: 'user',
      content: item.question,
      time: item.createTime
    },
    {
      role: 'assistant',
      content: item.answer,
      sources: item.sources ? JSON.parse(item.sources) : [],
      time: item.createTime
    }
  ]
}

const clearHistory = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有历史记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await qaApi.clearHistory(userStore.userInfo?.id)
    if (res.code === 200) {
      ElMessage.success('清空成功')
      historyList.value = []
    }
  } catch (error) {
    // 用户取消
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

const formatMessage = (content) => {
  // 简单的Markdown渲染
  return content
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
}

const formatTime = (date) => {
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style lang="scss" scoped>
.qa-page {
  height: calc(100vh - 100px);
  padding: 0;
}

.qa-container {
  display: flex;
  height: 100%;
  gap: 20px;
}

.qa-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;

  .qa-header {
    padding: 20px;
    border-bottom: 1px solid #ebeef5;

    h1 {
      font-size: 20px;
      margin-bottom: 4px;
    }

    p {
      font-size: 14px;
      color: #909399;
    }
  }

  .chat-container {
    flex: 1;
    overflow-y: auto;
    padding: 20px;

    .message {
      display: flex;
      margin-bottom: 20px;

      &.user {
        flex-direction: row-reverse;

        .message-content {
          align-items: flex-end;
        }

        .message-text {
          background: #409eff;
          color: #fff;
        }
      }

      &.assistant {
        .message-text {
          background: #f5f7fa;
        }
      }

      .message-avatar {
        margin: 0 12px;

        .ai-avatar {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        }
      }

      .message-content {
        display: flex;
        flex-direction: column;
        max-width: 70%;

        .message-text {
          padding: 12px 16px;
          border-radius: 8px;
          line-height: 1.6;
          word-break: break-word;
        }

        .message-sources {
          margin-top: 12px;
          padding: 12px;
          background: #fafafa;
          border-radius: 8px;
          border: 1px solid #ebeef5;

          .sources-header {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 13px;
            font-weight: 600;
            color: #606266;
            margin-bottom: 8px;
          }

          .sources-list {
            .source-item {
              padding: 8px;
              background: #fff;
              border-radius: 4px;
              margin-bottom: 8px;

              &:last-child {
                margin-bottom: 0;
              }

              .source-name {
                font-size: 13px;
                color: #303133;
                font-weight: 500;
              }

              .source-location {
                font-size: 12px;
                color: #909399;
                margin-top: 4px;
              }

              .source-similarity {
                font-size: 12px;
                color: #67c23a;
                margin-top: 4px;
              }
            }
          }
        }

        .message-time {
          font-size: 12px;
          color: #c0c4cc;
          margin-top: 4px;
        }
      }

      &.loading {
        .typing-indicator {
          display: flex;
          gap: 4px;
          padding: 12px 16px;
          background: #f5f7fa;
          border-radius: 8px;

          span {
            width: 8px;
            height: 8px;
            background: #909399;
            border-radius: 50%;
            animation: typing 1.4s infinite;

            &:nth-child(2) {
              animation-delay: 0.2s;
            }

            &:nth-child(3) {
              animation-delay: 0.4s;
            }
          }
        }
      }
    }
  }

    .input-container {
    padding: 20px;
    border-top: 1px solid #ebeef5;

    .knowledge-select {
      width: 200px;
      margin-bottom: 12px;
    }

    .input-wrapper {
      display: flex;
      gap: 12px;

      .el-textarea {
        flex: 1;
      }
    }

    .input-tip {
      font-size: 12px;
      color: #c0c4cc;
      margin-top: 8px;
    }
  }
}

.qa-sidebar {
  width: 300px;

  .history-card {
    height: 100%;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .history-list {
      max-height: calc(100vh - 250px);
      overflow-y: auto;

      .history-item {
        padding: 12px;
        border-radius: 8px;
        cursor: pointer;
        transition: background 0.3s;

        &:hover {
          background: #f5f7fa;
        }

        .history-question {
          font-size: 14px;
          color: #303133;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .history-time {
          font-size: 12px;
          color: #c0c4cc;
          margin-top: 4px;
        }
      }
    }
  }
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-4px);
  }
}
</style>