<template>
  <div class="qa-page">
    <div class="qa-container">
      <!-- 问答区域 -->
      <div class="qa-main">
        <div class="qa-header">
          <div class="header-left">
            <h1>智能问答</h1>
            <p>基于知识库的智能问答系统，为您精准解答问题</p>
          </div>
          <div class="header-actions">
            <el-button type="primary" @click="createNewConversation">
              <el-icon><Plus /></el-icon>
              新对话
            </el-button>
            <el-button 
              type="info" 
              @click="toggleSettings"
              :icon="Settings"
            >
              设置
            </el-button>
          </div>
        </div>

        <!-- 优化的聊天容器 -->
        <OptimizedChatContainer 
          :messages="messages" 
          :loading="loading"
          @focus-input="focusInput"
        />

        <!-- 输入区域 -->
        <div class="input-container">
          <!-- 知识库选择 -->
          <div class="knowledge-section">
            <el-select
              v-model="selectedKnowledge"
              placeholder="选择知识库"
              class="knowledge-select"
              @change="handleKnowledgeChange"
              loading="knowledgeLoading"
            >
              <el-option
                v-for="item in knowledgeList"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              >
                <div class="knowledge-option">
                  <span class="knowledge-name">{{ item.name }}</span>
                  <span class="knowledge-count">({{ item.documentCount || 0 }}篇文档)</span>
                </div>
              </el-option>
            </el-select>
            <div class="knowledge-tip" v-if="knowledgeList.length === 0">
              暂无知识库，请先创建知识库
            </div>
          </div>

          <!-- 输入区域 -->
          <div class="input-wrapper">
            <el-input
              ref="inputRef"
              v-model="question"
              type="textarea"
              :rows="2"
              placeholder="请输入您的问题..."
              @keyup.enter.ctrl="handleSubmit"
              @input="handleInput"
              maxlength="1000"
              show-word-limit
            />
            <div class="input-actions">
              <el-button
                type="primary"
                :loading="loading"
                :disabled="!canSubmit"
                @click="handleSubmit"
                size="large"
              >
                <el-icon><Promotion /></el-icon>
                发送
              </el-button>
              <el-button
                @click="createNewConversation"
                size="large"
              >
                <el-icon><Refresh /></el-icon>
                清空
              </el-button>
            </div>
          </div>
          
          <!-- 输入提示 -->
          <div class="input-tips">
            <div class="tip-item">
              <el-icon><Key /></el-icon>
              <span>按 Ctrl + Enter 快速发送</span>
            </div>
            <div class="tip-item">
              <el-icon><InfoFilled /></el-icon>
              <span>最多支持1000字提问</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 侧边栏 -->
      <div class="qa-sidebar">
        <ConversationHistory
          :conversations="conversationList"
          :current-conversation-id="currentConversationId"
          @select-conversation="selectConversation"
          @delete-conversation="deleteConversation"
          @clear-current="clearCurrentConversation"
          @load-more="loadMoreConversations"
        />
      </div>

      <!-- 设置面板 -->
      <div v-if="showSettings" class="settings-panel">
        <div class="settings-header">
          <h3>设置</h3>
          <el-button @click="toggleSettings" icon="Close">
            关闭
          </el-button>
        </div>
        <div class="settings-content">
          <div class="setting-item">
            <label>消息显示数量</label>
            <el-slider
              v-model="messageLimit"
              :min="10"
              :max="100"
              :step="10"
              @change="updateMessageLimit"
            />
            <span>{{ messageLimit }}条</span>
          </div>
          <div class="setting-item">
            <label>自动滚动</label>
            <el-switch
              v-model="autoScroll"
              @change="updateAutoScroll"
            />
          </div>
          <div class="setting-item">
            <label>显示来源详情</label>
            <el-switch
              v-model="showSources"
              @change="updateShowSources"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { qaApi, debouncedQA } from '@/api/qa-optimized'
import { knowledgeApi } from '@/api/knowledge'
import { useUserStore } from '@/stores/user'
import { formatMessage, formatTime, formatDateTime } from '@/utils/format'
import OptimizedChatContainer from '@/components/OptimizedChatContainer.vue'
import ConversationHistory from '@/components/ConversationHistory.vue'
import {
  Plus,
  Promotion,
  Settings,
  Refresh,
  Key,
  InfoFilled,
  Close
} from '@element-plus/icons-vue'

const userStore = useUserStore()

// 响应式数据
const question = ref('')
const selectedKnowledge = ref(null)
const loading = ref(false)
const knowledgeLoading = ref(false)
const messages = ref([])
const conversationList = ref([])
const knowledgeList = ref([])
const currentConversationId = ref(null)
const inputRef = ref(null)

// 设置相关
const showSettings = ref(false)
const messageLimit = ref(50)
const autoScroll = ref(true)
const showSources = ref(true)
const knowledgeCache = ref(null)

// 计算属性
const canSubmit = computed(() => {
  return question.value.trim() && 
         !loading.value && 
         selectedKnowledge.value && 
         userStore.userInfo?.id
})

// 加载状态管理
const isLoading = ref(false)

onMounted(() => {
  loadKnowledgeList()
  loadConversations()
})

// 优化的知识库加载（带缓存）
const loadKnowledgeList = async (force = false) => {
  if (isLoading.value) return
  
  // 使用缓存
  if (!force && knowledgeCache.value) {
    knowledgeList.value = knowledgeCache.value
    if (knowledgeList.value.length > 0 && !selectedKnowledge.value) {
      selectedKnowledge.value = knowledgeList.value[0].id
    }
    return
  }
  
  isLoading.value = true
  knowledgeLoading.value = true
  
  try {
    const res = await knowledgeApi.getKnowledgeList({ page: 1, size: 20 })
    if (res.code === 200) {
      knowledgeList.value = res.data?.list || res.data || []
      knowledgeCache.value = knowledgeList.value
      
      if (knowledgeList.value.length > 0 && !selectedKnowledge.value) {
        selectedKnowledge.value = knowledgeList.value[0].id
      }
    }
  } catch (error) {
    console.error('加载知识库列表失败', error)
    ElMessage.error('加载知识库失败')
  } finally {
    isLoading.value = false
    knowledgeLoading.value = false
  }
}

// 优化的会话列表加载
const loadConversations = async () => {
  if (!userStore.userInfo?.id || isLoading.value) return
  
  isLoading.value = true
  try {
    const res = await qaApi.getConversations(userStore.userInfo.id)
    if (res.code === 200) {
      conversationList.value = res.data || []
    }
  } catch (error) {
    console.error('加载会话列表失败', error)
  } finally {
    isLoading.value = false
  }
}

// 加载更多会话
const loadMoreConversations = async () => {
  // 实现分页加载逻辑
  ElMessage.info('暂不支持分页加载')
}

const handleKnowledgeChange = (value) => {
  selectedKnowledge.value = value
}

const handleInput = (value) => {
  // 可以添加输入防抖或字数统计
  question.value = value
}

const createNewConversation = async () => {
  messages.value = []
  currentConversationId.value = null
  focusInput()
}

// 优化的会话选择
const selectConversation = async (item) => {
  if (currentConversationId.value === item.id) return
  
  currentConversationId.value = item.id
  messages.value = []
  loading.value = true

  try {
    const res = await qaApi.getConversationMessages(item.id)
    if (res.code === 200) {
      const msgs = res.data || []
      // 限制消息数量
      const limitedMsgs = msgs.slice(-messageLimit.value)
      
      messages.value = limitedMsgs.map(msg => {
        let sources = []
        if (msg.role === 'assistant' && msg.sources) {
          try {
            sources = typeof msg.sources === 'string' ? JSON.parse(msg.sources) : msg.sources
            // 根据设置过滤来源
            if (!showSources.value) {
              sources = sources.slice(0, 3) // 只显示前3个来源
            }
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
    }
  } catch (error) {
    console.error('加载会话消息失败', error)
    ElMessage.error('加载会话消息失败')
  } finally {
    loading.value = false
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

// 优化的清空当前会话
const clearCurrentConversation = async () => {
  if (!currentConversationId.value) return

  try {
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
  } catch (error) {
    // 用户取消
  }
}

// 优化的提交处理
const handleSubmit = async () => {
  if (!canSubmit.value) return

  const userQuestion = question.value.trim()
  question.value = ''

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: userQuestion,
    time: formatTime(new Date())
  })

  loading.value = true

  try {
    const res = await qaApi.chat({
      question: userQuestion,
      userId: userStore.userInfo.id,
      conversationId: currentConversationId.value,
      knowledgeId: selectedKnowledge.value
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
  }
}

const focusInput = () => {
  nextTick(() => {
    inputRef.value?.focus()
  })
}

// 设置相关方法
const toggleSettings = () => {
  showSettings.value = !showSettings.value
}

const updateMessageLimit = (value) => {
  messageLimit.value = value
  // 重新加载当前会话的消息
  if (currentConversationId.value) {
    selectConversation(conversationList.value.find(c => c.id === currentConversationId.value))
  }
}

const updateAutoScroll = (value) => {
  autoScroll.value = value
}

const updateShowSources = (value) => {
  showSources.value = value
  // 重新加载当前会话的消息
  if (currentConversationId.value) {
    selectConversation(conversationList.value.find(c => c.id === currentConversationId.value))
  }
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

    .header-actions {
      display: flex;
      gap: 12px;
    }
  }

  .input-container {
    padding: 20px;
    border-top: 1px solid #ebeef5;

    .knowledge-section {
      margin-bottom: 16px;

      .knowledge-select {
        width: 100%;
      }

      .knowledge-tip {
        font-size: 12px;
        color: #e6a23c;
        margin-top: 8px;
        text-align: center;
      }

      .knowledge-option {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .knowledge-name {
          font-weight: 500;
        }

        .knowledge-count {
          font-size: 12px;
          color: #909399;
        }
      }
    }

    .input-wrapper {
      display: flex;
      gap: 12px;
      margin-bottom: 12px;

      .el-textarea {
        flex: 1;
      }

      .input-actions {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }
    }

    .input-tips {
      display: flex;
      gap: 16px;
      font-size: 12px;
      color: #909399;

      .tip-item {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
}

.qa-sidebar {
  width: 300px;
}

.settings-panel {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  width: 400px;
  z-index: 1000;

  .settings-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    border-bottom: 1px solid #ebeef5;

    h3 {
      margin: 0;
      font-size: 16px;
    }
  }

  .settings-content {
    padding: 20px;

    .setting-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      label {
        font-size: 14px;
        color: #303133;
      }

      .el-slider {
        flex: 1;
        margin: 0 16px;
      }

      span {
        font-size: 12px;
        color: #909399;
        min-width: 40px;
        text-align: right;
      }
    }
  }
}
</style>