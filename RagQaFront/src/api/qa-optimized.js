import request from './request'

// API缓存管理
class APICache {
  constructor() {
    this.cache = new Map()
    this.expiryTime = 5 * 60 * 1000 // 5分钟缓存
  }

  get(key) {
    const item = this.cache.get(key)
    if (item && Date.now() < item.expiry) {
      return item.data
    }
    this.cache.delete(key)
    return null
  }

  set(key, data) {
    this.cache.set(key, {
      data,
      expiry: Date.now() + this.expiryTime
    })
  }

  clear() {
    this.cache.clear()
  }
}

const apiCache = new APICache()

// 防抖函数
function debounce(func, wait) {
  let timeout
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout)
      func(...args)
    }
    clearTimeout(timeout)
    timeout = setTimeout(later, wait)
  }
}

// 节流函数
function throttle(func, limit) {
  let inThrottle
  return function() {
    const args = arguments
    const context = this
    if (!inThrottle) {
      func.apply(context, args)
      inThrottle = true
      setTimeout(() => inThrottle = false, limit)
    }
  }
}

export const qaApi = {
  // 提交问题（无上下文）
  askQuestion: (data) => {
    return request({
      url: '/rag/ask',
      method: 'post',
      params: data
    })
  },

  // 带上下文的对话
  chat: (data) => {
    return request({
      url: '/rag/chat',
      method: 'post',
      params: data
    })
  },

  // 获取问答历史
  getHistory: (params) => {
    const cacheKey = `history_${JSON.stringify(params)}`
    const cached = apiCache.get(cacheKey)
    if (cached) {
      return Promise.resolve(cached)
    }
    
    return request({
      url: '/qa/history',
      method: 'get',
      params
    }).then(res => {
      if (res.code === 200) {
        apiCache.set(cacheKey, res.data)
      }
      return res
    })
  },

  // 获取问答详情
  getQADetail: (id) => {
    return request({
      url: `/qa/${id}`,
      method: 'get'
    })
  },

  // 删除问答记录
  deleteQA: (id) => {
    return request({
      url: `/qa/${id}`,
      method: 'delete'
    })
  },

  // 清空问答历史
  clearHistory: (userId) => {
    apiCache.clear()
    return request({
      url: `/qa/clear/${userId}`,
      method: 'delete'
    })
  },

  // 搜索问答记录
  searchQA: (keyword, params) => {
    return request({
      url: '/qa/search',
      method: 'get',
      params: { keyword, ...params }
    })
  },

  // ========== 会话相关 ==========

  // 创建新会话
  createConversation: (userId) => {
    return request({
      url: '/rag/conversation/create',
      method: 'post',
      params: { userId }
    })
  },

  // 获取用户的会话列表
  getConversations: (userId) => {
    const cacheKey = `conversations_${userId}`
    const cached = apiCache.get(cacheKey)
    if (cached) {
      return Promise.resolve({ code: 200, data: cached })
    }
    
    return request({
      url: '/rag/conversation/list',
      method: 'get',
      params: { userId }
    }).then(res => {
      if (res.code === 200) {
        apiCache.set(cacheKey, res.data)
      }
      return res
    })
  },

  // 获取会话消息历史
  getConversationMessages: (conversationId) => {
    const cacheKey = `conversation_messages_${conversationId}`
    const cached = apiCache.get(cacheKey)
    if (cached) {
      return Promise.resolve({ code: 200, data: cached })
    }
    
    return request({
      url: '/rag/conversation/messages',
      method: 'get',
      params: { conversationId }
    }).then(res => {
      if (res.code === 200) {
        apiCache.set(cacheKey, res.data)
      }
      return res
    })
  },

  // 删除会话
  deleteConversation: (conversationId) => {
    apiCache.clear()
    return request({
      url: `/rag/conversation/${conversationId}`,
      method: 'delete'
    })
  },

  // 清空会话消息历史
  clearConversationMessages: (conversationId) => {
    apiCache.clear()
    return request({
      url: `/rag/conversation/${conversationId}/messages`,
      method: 'delete'
    })
  },

  // 获取会话消息数量
  getMessageCount: (conversationId) => {
    return request({
      url: '/rag/conversation/messageCount',
      method: 'get',
      params: { conversationId }
    })
  },

  // 删除最早的N轮对话
  deleteOldestRounds: (conversationId, rounds = 10) => {
    return request({
      url: `/rag/conversation/${conversationId}/oldest`,
      method: 'delete',
      params: { rounds }
    })
  }
}

// 导出防抖和节流版本的方法
export const debouncedQA = {
  chat: debounce(qaApi.chat, 300),
  searchQA: debounce(qaApi.searchQA, 500)
}

export const throttledQA = {
  getConversations: throttle(qaApi.getConversations, 1000),
  getHistory: throttle(qaApi.getHistory, 1000)
}