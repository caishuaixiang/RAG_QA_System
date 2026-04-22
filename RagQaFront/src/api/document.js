import request from '@/utils/request'

export const documentApi = {
  // 上传文档 —— 完全正确版本
  uploadDocument(formData, onProgress) {
    return request({
      url: '/document/upload',
      method: 'post',
      data: formData,
      onUploadProgress: onProgress
      // 这里：不要加任何 headers！！！
    })
  },

  // 获取文档列表
  getDocumentList(params) {
    return request({
      url: '/document/list',
      method: 'get',
      params
    })
  },

  // 获取文档详情
  getDocumentDetail(id) {
    return request({
      url: `/document/${id}`,
      method: 'get'
    })
  },

  // 删除文档
  deleteDocument(id) {
    return request({
      url: `/document/${id}`,
      method: 'delete'
    })
  },

  // 处理文档（解析、切片、向量化）
  processDocument(id) {
    return request({
      url: `/document/process/${id}`,
      method: 'post'
    })
  },

  // 获取文档内容预览
  getDocumentContent(id) {
    return request({
      url: `/document/${id}/content`,
      method: 'get'
    })
  },

  // 获取文档切片列表
  getDocumentChunks(id, params) {
    return request({
      url: `/document/${id}/chunks`,
      method: 'get',
      params
    })
  },

  // 批量删除文档
  batchDeleteDocuments(ids) {
    return request({
      url: '/document/batch-delete',
      method: 'post',
      data: { ids }
    })
  },

  // 搜索文档
  searchDocuments(keyword, params) {
    return request({
      url: '/document/search',
      method: 'get',
      params: { keyword, ...params }
    })
  },

  // 获取文档预览信息
  getPreviewInfo(id) {
    return request({
      url: `/document/${id}/preview-info`,
      method: 'get'
    })
  },

  // 获取文档预览URL
  getPreviewUrl(id) {
    return `/api/document/${id}/preview`
  },

  // 获取文档下载URL
  getDownloadUrl(id) {
    return `/api/document/${id}/download`
  },

  // 获取文档Base64编码
  getDocumentBase64(id) {
    return request({
      url: `/document/${id}/base64`,
      method: 'get'
    })
  }
}