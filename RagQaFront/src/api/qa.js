import request from '@/utils/request'

export const qaApi = {
  // 提交问题（无上下文）
  askQuestion(data) {
    return request({
      url: '/rag/ask',
      method: 'post',
      params: data
    })
  },

  // 带上下文的对话
  chat(data) {
    return request({
      url: '/rag/chat',
      method: 'post',
      params: data
    })
  },

  // 获取问答历史
  getHistory(params) {
    return request({
      url: '/qa/history',
      method: 'get',
      params
    })
  },

  // 获取问答详情
  getQADetail(id) {
    return request({
      url: `/qa/${id}`,
      method: 'get'
    })
  },

  // 删除问答记录
  deleteQA(id) {
    return request({
      url: `/qa/${id}`,
      method: 'delete'
    })
  },

  // 清空问答历史
  clearHistory(userId) {
    return request({
      url: `/qa/clear/${userId}`,
      method: 'delete'
    })
  },

  // 搜索问答记录
  searchQA(keyword, params) {
    return request({
      url: '/qa/search',
      method: 'get',
      params: { keyword, ...params }
    })
  },

  // ========== 会话相关 ==========

  // 创建新会话
  createConversation(userId) {
    return request({
      url: '/rag/conversation/create',
      method: 'post',
      params: { userId }
    })
  },

  // 获取用户的会话列表
  getConversations(userId) {
    return request({
      url: '/rag/conversation/list',
      method: 'get',
      params: { userId }
    })
  },

  // 获取会话消息历史
  getConversationMessages(conversationId) {
    return request({
      url: '/rag/conversation/messages',
      method: 'get',
      params: { conversationId }
    })
  },

  // 删除会话
  deleteConversation(conversationId) {
    return request({
      url: `/rag/conversation/${conversationId}`,
      method: 'delete'
    })
  },

  // 清空会话消息历史
  clearConversationMessages(conversationId) {
    return request({
      url: `/rag/conversation/${conversationId}/messages`,
      method: 'delete'
    })
  },

  // 获取会话消息数量
  getMessageCount(conversationId) {
    return request({
      url: '/rag/conversation/messageCount',
      method: 'get',
      params: { conversationId }
    })
  },

  // 删除最早的N轮对话
  deleteOldestRounds(conversationId, rounds = 10) {
    return request({
      url: `/rag/conversation/${conversationId}/oldest`,
      method: 'delete',
      params: { rounds }
    })
  }
}