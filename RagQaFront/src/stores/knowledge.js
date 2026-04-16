import { defineStore } from 'pinia'
import { ref } from 'vue'
import { knowledgeApi } from '@/api/knowledge'

export const useKnowledgeStore = defineStore('knowledge', () => {
  // 状态
  const knowledgeList = ref([])
  const currentKnowledge = ref(null)
  const loading = ref(false)
  const total = ref(0)

  // 方法
  async function fetchKnowledgeList(params = {}) {
    loading.value = true
    try {
      const res = await knowledgeApi.getKnowledgeList(params)
      if (res.code === 200) {
        knowledgeList.value = res.data.list || res.data
        total.value = res.data.total || knowledgeList.value.length
        return { success: true, data: res.data }
      }
      return { success: false, message: res.message }
    } catch (error) {
      return { success: false, message: error.message }
    } finally {
      loading.value = false
    }
  }

  async function fetchKnowledgeDetail(id) {
    loading.value = true
    try {
      const res = await knowledgeApi.getKnowledgeDetail(id)
      if (res.code === 200) {
        currentKnowledge.value = res.data
        return { success: true, data: res.data }
      }
      return { success: false, message: res.message }
    } catch (error) {
      return { success: false, message: error.message }
    } finally {
      loading.value = false
    }
  }

  async function createKnowledge(data) {
    try {
      const res = await knowledgeApi.createKnowledge(data)
      if (res.code === 200) {
        return { success: true, data: res.data }
      }
      return { success: false, message: res.message }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  async function updateKnowledge(id, data) {
    try {
      const res = await knowledgeApi.updateKnowledge(id, data)
      if (res.code === 200) {
        return { success: true }
      }
      return { success: false, message: res.message }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  async function deleteKnowledge(id) {
    try {
      const res = await knowledgeApi.deleteKnowledge(id)
      if (res.code === 200) {
        knowledgeList.value = knowledgeList.value.filter(item => item.id !== id)
        return { success: true }
      }
      return { success: false, message: res.message }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  function clearCurrentKnowledge() {
    currentKnowledge.value = null
  }

  return {
    knowledgeList,
    currentKnowledge,
    loading,
    total,
    fetchKnowledgeList,
    fetchKnowledgeDetail,
    createKnowledge,
    updateKnowledge,
    deleteKnowledge,
    clearCurrentKnowledge
  }
})