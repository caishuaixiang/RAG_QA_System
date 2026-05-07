<template>
  <div class="message-item" :class="[message.role]">
    <div class="message-avatar">
      <el-avatar v-if="message.role === 'user'" :icon="UserFilled" />
      <el-avatar v-else class="ai-avatar">
        <el-icon><ChatDotRound /></el-icon>
      </el-avatar>
    </div>
    <div class="message-content">
      <div class="message-text">{{ message.content }}</div>
      <MessageSources v-if="message.sources && message.sources.length > 0" :sources="message.sources" />
      <div class="message-time">{{ message.time }}</div>
    </div>
  </div>
</template>

<script setup>
import { UserFilled, ChatDotRound } from '@element-plus/icons-vue'

const props = defineProps({
  message: {
    type: Object,
    required: true
  }
})
</script>

<style lang="scss" scoped>
.message-item {
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

    .message-time {
      font-size: 12px;
      color: #c0c4cc;
      margin-top: 4px;
    }
  }
}
</style>