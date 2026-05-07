<template>
  <div class="chat-container" ref="chatContainer">
    <div class="chat-messages" v-if="messages.length > 0">
      <MessageItem
        v-for="(msg, index) in messages"
        :key="index"
        :message="msg"
      />
      
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
        <el-button type="primary" @click="$emit('focus-input')">开始提问</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, watch } from 'vue'
import MessageItem from './MessageItem.vue'
import { ChatDotRound } from '@element-plus/icons-vue'

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['focus-input'])

const chatContainer = ref(null)

// 监听消息变化，自动滚动到底部
watch(() => props.messages, () => {
  scrollToBottom()
}, { deep: true })

watch(() => props.loading, () => {
  scrollToBottom()
})

const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}
</script>

<style lang="scss" scoped>
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

  .loading {
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

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-4px);
  }
}
</style>