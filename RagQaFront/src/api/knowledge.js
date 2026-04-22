import request from '@/utils/request'

export const knowledgeApi = {
  // 获取知识库列表
  getKnowledgeList(params) {
    return request({
      url: '/knowledge/list',
      method: 'get',
      params
    })
  },

  // 获取知识库详情
  getKnowledgeDetail(id) {
    return request({
      url: `/knowledge/${id}`,
      method: 'get'
    })
  },

  // 创建知识库
  createKnowledge(data) {
    return request({
      url: '/knowledge',
      method: 'post',
      data
    })
  },

  // 更新知识库
  updateKnowledge(id, data) {
    return request({
      url: `/knowledge/${id}`,
      method: 'put',
      data
    })
  },

  // 删除知识库
  deleteKnowledge(id) {
    return request({
      url: `/knowledge/${id}`,
      method: 'delete'
    })
  },

  // 获取知识库下的文档列表
  getKnowledgeDocuments(knowledgeId, params) {
    return request({
      url: `/knowledge/${knowledgeId}/documents`,
      method: 'get',
      params
    })
  },

  // 获取知识库统计信息
  getKnowledgeStats(id) {
    return request({
      url: `/knowledge/${id}/stats`,
      method: 'get'
    })
  }
}