import request from '@/utils/request'

export const qaApi = {
  // 提交问题
  askQuestion(data) {
    return request({
      url: '/rag/ask',
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
  }
}