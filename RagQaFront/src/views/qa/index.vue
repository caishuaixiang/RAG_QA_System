<template>
  <div class="qa-page">
    <div class="qa-container">
      <!-- 问答区域 -->
      <div class="qa-main">
        <div class="qa-header">
          <div class="header-left">
            <h1>智能问答</h1>
            <p>基于知识库的智能问答系统，为您精准解答问题</p >
          </div>
          <el-button type="primary" @click="createNewConversation">
            <el-icon><Plus /></el-icon>
            新对话
          </el-button>
        </div>

        <!-- 对话区域 -->
        <div class="chat-container" ref="chatContainer">
          <div class="chat-messages" v-if="messages.length > 0">
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
<div class="source-header">
                         <div class="source-info">
                           <div class="source-icon">
                             <el-icon>
                               <component :is="getDocumentIcon(source.document_name)" />
                             </el-icon>
                           </div>
                           <div class="source-name">{{ source.document_name }}</div>
                         </div>
                         <div class="source-similarity">
                           <el-tag :type="getSimilarityTagType(source.similarity)" size="small">
                             相关度：{{ source.similarity }}%
                           </el-tag>
                         </div>
                       </div>
                       <div class="source-location" v-if="getSourceTitle(source)">
                         <span class="source-title">{{ getSourceTitle(source) }}</span>
                       </div>
                       <div class="source-content-preview" v-if="source.content_preview">
                         <div class="preview-label">内容预览：</div>
                         <div class="preview-text">{{ source.content_preview }}</div>
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

          <!-- 空状态 -->
          <div class="empty-chat" v-else>
            <el-empty description="开始新对话">
              <el-button type="primary" @click="focusInput">开始提问</el-button>
            </el-empty>
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
              ref="inputRef"
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
              <span>会话列表</span>
              <el-button type="primary" link @click="clearCurrentConversation" :disabled="!currentConversationId">
                清空当前
              </el-button>
            </div>
          </template>
          <div class="history-list">
            <div
              v-for="item in conversationList"
              :key="item.id"
              :class="['history-item', { active: currentConversationId === item.id }]"
              @click="selectConversation(item)"
            >
              <div class="history-title">{{ item.title }}</div>
              <div class="history-time">{{ formatDateTime(item.updateTime) }}</div>
              <el-button
                type="danger"
                link
                size="small"
                class="delete-btn"
                @click.stop="deleteConversation(item)"
              >
                删除
              </el-button>
            </div>
            <el-empty v-if="conversationList.length === 0" description="暂无会话" />
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
  Promotion,
  Plus,
  Files,
  Reading,
  Folder
} from '@element-plus/icons-vue'

const userStore = useUserStore()

const question = ref('')
const selectedKnowledge = ref(null)
const loading = ref(false)
const messages = ref([])
const conversationList = ref([])
const historyList = ref([])
const knowledgeList = ref([])
const chatContainer = ref(null)
const inputRef = ref(null)
const currentConversationId = ref(null)

onMounted(() => {
  loadKnowledgeList()
  loadConversations()
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

const loadConversations = async () => {
  if (!userStore.userInfo?.id) {
    console.warn('用户未登录，无法加载会话列表')
    return
  }
  try {
    const res = await qaApi.getConversations(userStore.userInfo.id)
    if (res.code === 200) {
      conversationList.value = res.data || []
    }
  } catch (error) {
    console.error('加载会话列表失败', error)
  }
}

const createNewConversation = async () => {
  messages.value = []
  currentConversationId.value = null
  focusInput()
}

const selectConversation = async (item) => {
  currentConversationId.value = item.id
  messages.value = []

  try {
    const res = await qaApi.getConversationMessages(item.id)
    if (res.code === 200) {
      const msgs = res.data || []
      // 将消息转换为显示格式
      messages.value = msgs.map(msg => {
        // 解析sources（仅assistant消息有）
        let sources = []
        if (msg.role === 'assistant' && msg.sources) {
          try {
            sources = typeof msg.sources === 'string' ? JSON.parse(msg.sources) : msg.sources
          } catch (e) {
            console.error('解析sources失败', e)
          }
        }
        return {
          role: msg.role,
          content: msg.content,
          time: formatDateTime(msg.createTime),
          sources: sources
        }
      })
      scrollToBottom()
    }
  } catch (error) {
    console.error('加载会话消息失败', error)
  }
}

const deleteConversation = async (item) => {
  try {
    await ElMessageBox.confirm('确定要删除此会话吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await qaApi.deleteConversation(item.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      conversationList.value = conversationList.value.filter(c => c.id !== item.id)
      if (currentConversationId.value === item.id) {
        messages.value = []
        currentConversationId.value = null
      }
    }
  } catch (error) {
    // 用户取消
  }
}

const clearCurrentConversation = async () => {
  if (!currentConversationId.value) return

  try {
    // 先检查消息数量
    const countRes = await qaApi.getMessageCount(currentConversationId.value)
    if (countRes.code === 200) {
      const { count, rounds } = countRes.data

      if (rounds > 10) {
        // 超过10轮，提示用户清理
        await ElMessageBox.confirm(
          `当前会话有 ${rounds} 轮对话，建议清理最早的对话以释放空间。是否清理最早的10轮？`,
          '提示',
          {
            confirmButtonText: '清理',
            cancelButtonText: '取消',
            type: 'info'
          }
        )

        const res = await qaApi.deleteOldestRounds(currentConversationId.value, 10)
        if (res.code === 200) {
          ElMessage.success(`已清理10轮对话，剩余 ${res.data.remainingRounds} 轮`)
          // 重新加载消息
          const currentConv = conversationList.value.find(c => c.id === currentConversationId.value)
          if (currentConv) {
            selectConversation(currentConv)
          }
        }
      } else {
        // 不多，直接清空
        await ElMessageBox.confirm('确定要清空当前会话的所有消息吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        const res = await qaApi.clearConversationMessages(currentConversationId.value)
        if (res.code === 200) {
          ElMessage.success('清空成功')
          messages.value = []
        }
      }
    }
  } catch (error) {
    // 用户取消
  }
}

const handleSubmit = async () => {
  if (!question.value.trim() || loading.value) return

  if (!userStore.userInfo?.id) {
    ElMessage.error('请先登录')
    return
  }

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
    const res = await qaApi.chat({
      question: userQuestion,
      userId: userStore.userInfo.id,
      conversationId: currentConversationId.value,
      knowledgeBaseId: selectedKnowledge.value
    })

    if (res.code === 200) {
      const data = res.data

      // 更新会话ID（如果是新会话）
      if (!currentConversationId.value) {
        currentConversationId.value = data.conversationId
        // 刷新会话列表
        loadConversations()
      }

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
  }
}

const focusInput = () => {
  nextTick(() => {
    inputRef.value?.focus()
  })
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

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取相关度标签类型
const getSimilarityTagType = (similarity) => {
  if (similarity >= 80) return 'success'
  if (similarity >= 60) return ''
  if (similarity >= 40) return 'warning'
  return 'danger'
}

// 获取文档图标
const getDocumentIcon = (documentName) => {
  if (!documentName) return Document
  
  const name = documentName.toLowerCase()
  
  // PDF文档
  if (name.includes('.pdf') || name.includes('pdf')) {
    return Folder
  }
  
  // Word文档
  if (name.includes('.doc') || name.includes('.docx') || name.includes('word')) {
    return Reading
  }
  
  // Excel表格
  if (name.includes('.xls') || name.includes('.xlsx') || name.includes('excel')) {
    return Files
  }
  
  // 纯文本
  if (name.includes('.txt') || name.includes('text')) {
    return Document
  }
  
  // 默认图标
  return Document
}

// 获取来源标题（优先显示有意义的标题）
const getSourceTitle = (source) => {
  if (!source) return ''

  // 优先显示内容预览（第一行内容）
  if (source.content_preview) {
    return source.content_preview
  }

  const sectionTitle = source.location?.section_title
  const pageNumber = source.location?.page_number
  const lineRange = source.location?.line_range

  // 判断章节标题是否有效（不是太长的句子，且包含关键词）
  const isValidTitle = (title) => {
    if (!title || title.length > 50) return false
    // 包含这些关键词的标题更可能是有效标题
    const keywords = ['制度', '规定', '办法', '条例', '守则', '手册', '指南', '章', '节', '条', '附录', '附则']
    return keywords.some(kw => title.includes(kw))
  }

  // 构建位置信息
  let locationInfo = ''

  // 优先显示页码
  if (pageNumber) {
    locationInfo = `第${pageNumber}页`
  } else if (lineRange) {
    // 没有页码时显示行号
    locationInfo = `第${lineRange}行`
  }

  // 如果有有效的章节标题
  if (isValidTitle(sectionTitle)) {
    if (locationInfo) {
      return `${locationInfo} ${sectionTitle}`
    }
    return sectionTitle
  }

  // 如果只有位置信息
  if (locationInfo) {
    return locationInfo
  }

  return ''
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
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-left {
      h1 {
        font-size: 20px;
        margin-bottom: 4px;
      }

      p {
        font-size: 14px;
        color: #909399;
        margin: 0;
      }
    }
  }

  .chat-container {
    flex: 1;
    overflow-y: auto;
    padding: 20px;

    .empty-chat {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100%;
    }

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

              .source-title {
                  color: #409eff;
                  font-weight: 500;
              }

.source-header {
                 display: flex;
                 justify-content: space-between;
                 align-items: center;
                 margin-bottom: 8px;
               }

               .source-info {
                 display: flex;
                 align-items: center;
                 gap: 8px;
               }

               .source-icon {
                 color: #409eff;
                 font-size: 16px;
               }

               .source-name {
                 font-size: 14px;
                 color: #303133;
                 font-weight: 500;
               }

               .source-location {
                 font-size: 12px;
                 color: #909399;
                 margin-top: 4px;
               }

               .source-title {
                   color: #409eff;
                   font-weight: 500;
               }

               .source-content-preview {
                 margin-top: 8px;
                 padding: 8px;
                 background: #f8f9fa;
                 border-radius: 4px;
                 border-left: 3px solid #409eff;
               }

               /* 相关度颜色样式 */
               :deep(.el-tag--success) {
                 background-color: #f0f9ff !important;
                 border-color: #67c23a !important;
                 color: #67c23a !important;
               }

               :deep(.el-tag--warning) {
                 background-color: #fdf6ec !important;
                 border-color: #e6a23c !important;
                 color: #e6a23c !important;
               }

               :deep(.el-tag--danger) {
                 background-color: #fef0f0 !important;
                 border-color: #f56c6c !important;
                 color: #f56c6c !important;
               }

               .preview-label {
                 font-size: 11px;
                 color: #909399;
                 font-weight: 500;
                 margin-bottom: 4px;
               }

               .preview-text {
                 font-size: 12px;
                 color: #606266;
                 line-height: 1.4;
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
        position: relative;

        &:hover {
          background: #f5f7fa;
        }

        &.active {
          background: #ecf5ff;
          border: 1px solid #409eff;
        }

        .history-title {
          font-size: 14px;
          color: #303133;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          padding-right: 40px;
        }

        .history-time {
          font-size: 12px;
          color: #c0c4cc;
          margin-top: 4px;
        }

        .delete-btn {
          position: absolute;
          right: 8px;
          top: 50%;
          transform: translateY(-50%);
          opacity: 0;
          transition: opacity 0.3s;
        }

        &:hover .delete-btn {
          opacity: 1;
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