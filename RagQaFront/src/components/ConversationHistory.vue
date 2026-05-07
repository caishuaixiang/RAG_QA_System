<template>
  <div class="conversation-history">
    <div class="history-header">
      <div class="header-title">
        <el-icon><ChatDotRound /></el-icon>
        <span>会话列表</span>
      </div>
      <el-button 
        type="primary" 
        link 
        @click="$emit('clear-current')" 
        :disabled="!currentConversationId"
        size="small"
      >
        清空当前
      </el-button>
    </div>
    <div class="history-list">
      <div
        v-for="item in conversations"
        :key="item.id"
        :class="['history-item', { active: currentConversationId === item.id }]"
        @click="$emit('select-conversation', item)"
      >
        <div class="history-content">
          <div class="history-title">{{ item.title || '新对话' }}</div>
          <div class="history-time">{{ formatDateTime(item.updateTime) }}</div>
        </div>
        <el-button
          type="danger"
          link
          size="small"
          class="delete-btn"
          @click.stop="$emit('delete-conversation', item)"
        >
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>
      <div v-if="conversations.length === 0" class="empty-state">
        <el-empty description="暂无会话" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { formatDateTime } from '@/utils/format'
import { ChatDotRound, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  conversations: {
    type: Array,
    default: () => []
  },
  currentConversationId: {
    type: String,
    default: null
  }
})

const emit = defineEmits(['select-conversation', 'delete-conversation', 'clear-current'])
</script>

<style lang="scss" scoped>
.conversation-history {
  height: 100%;
  display: flex;
  flex-direction: column;
  
  .history-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px;
    border-bottom: 1px solid #ebeef5;
    
    .header-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
  }
  
  .history-list {
    flex: 1;
    overflow-y: auto;
    padding: 8px;
    
    .history-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.3s ease;
      margin-bottom: 4px;
      position: relative;
      
      &:hover {
        background: #f5f7fa;
        transform: translateX(2px);
      }
      
      &.active {
        background: #ecf5ff;
        border: 1px solid #409eff;
        box-shadow: 0 2px 4px rgba(64, 158, 255, 0.1);
      }
      
      .history-content {
        flex: 1;
        min-width: 0;
        
        .history-title {
          font-size: 14px;
          color: #303133;
          font-weight: 500;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          margin-bottom: 4px;
        }
        
        .history-time {
          font-size: 12px;
          color: #909399;
        }
      }
      
      .delete-btn {
        opacity: 0;
        transition: opacity 0.3s ease;
        margin-left: 8px;
        
        &:hover {
          opacity: 1;
        }
      }
      
      &:hover .delete-btn {
        opacity: 1;
      }
    }
    
    .empty-state {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 200px;
    }
  }
}
</style>