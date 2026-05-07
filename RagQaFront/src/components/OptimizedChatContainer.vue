<template>
  <div class="chat-container" ref="container">
    <VirtualScroll
      :items="processedMessages"
      :item-height="80"
      :buffer-size="3"
      @scroll="handleScroll"
      ref="virtualScroll"
    >
      <template #default="{ item }">
        <MessageItem :message="item.data" />
      </template>
    </VirtualScroll>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-indicator">
        <div class="typing-indicator">
          <span></span>
          <span></span>
          <span></span>
        </div>
        <span>AI 正在思考...</span>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="messages.length === 0 && !loading" class="empty-chat">
      <el-empty description="开始新对话">
        <el-button type="primary" @click="$emit('focus-input')">开始提问</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import VirtualScroll from './VirtualScroll.vue'
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

const container = ref(null)
const virtualScroll = ref(null)

// 处理消息数据，添加虚拟滚动需要的属性
const processedMessages = computed(() => {
  return props.messages.map((msg, index) => ({
    ...msg,
    id: index,
    className: msg.role
  }))
})

// 监听消息变化，自动滚动到底部
watch(() => props.messages, () => {
  scrollToBottom()
}, { deep: true })

watch(() => props.loading, () => {
  scrollToBottom()
})

// 处理滚动事件
const handleScroll = ({ scrollTop }) => {
  // 可以在这里添加滚动相关的逻辑
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (virtualScroll.value) {
      virtualScroll.value.scrollToBottom()
    }
  })
}

// 暴露方法给父组件
defineExpose({
  scrollToBottom,
  scrollToTop: () => {
    if (virtualScroll.value) {
      virtualScroll.value.scrollToIndex(0)
    }
  }
})
</script>

<style lang="scss" scoped>
.chat-container {
  height: 100%;
  position: relative;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.loading-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  
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

.empty-chat {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
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