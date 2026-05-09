<template>
  <div class="message-sources">
    <div class="sources-header">
      <el-icon><Document /></el-icon>
      <span>答案来源</span>
    </div>
    <div class="sources-list">
      <div
        v-for="(source, sIdx) in sources"
        :key="sIdx"
        class="source-item"
      >
        <div class="source-header">
          <div class="source-info">
            <div class="source-icon">
              <el-icon>
                <component :is="getDocumentIcon(source.document_name)" />
              </el-icon>
            </div>
            <div class="source-name">{{ source.document_name }}</div>
          </div>
          <div class="source-similarity">
            <el-tag v-if="source.similarity !== undefined && source.similarity !== null" :type="getSimilarityTagType(source.similarity)" size="small">
              相关度：{{ source.similarity }}%
            </el-tag>
            <el-tag v-else-if="source.bm25_only" type="info" size="small">
              关键词命中
            </el-tag>
            <el-tag v-else type="info" size="small">
              -
            </el-tag>
          </div>
        </div>
        <div class="source-location" v-if="getSourceTitle(source)">
          <span class="source-title">{{ getSourceTitle(source) }}</span>
        </div>
        <div class="source-content-preview" v-if="source.content_preview">
          <div class="preview-label">内容预览：</div>
          <div class="preview-text">{{ source.content_preview }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Document } from '@element-plus/icons-vue'
import { Files, Reading, FileText } from '@element-plus/icons-vue'

const props = defineProps({
  sources: {
    type: Array,
    required: true
  }
})

// 获取相关度标签类型
const getSimilarityTagType = (similarity) => {
  if (similarity >= 80) return 'success'
  if (similarity >= 60) return ''
  if (similarity >= 40) return 'warning'
  return 'danger'
}

// 获取文档图标
const getDocumentIcon = (documentName) => {
  if (!documentName) return Document
  
  const name = documentName.toLowerCase()
  
  // PDF文档
  if (name.includes('.pdf') || name.includes('pdf')) {
    return FileText
  }
  
  // Word文档
  if (name.includes('.doc') || name.includes('.docx') || name.includes('word')) {
    return Reading
  }
  
  // Excel表格
  if (name.includes('.xls') || name.includes('.xlsx') || name.includes('excel')) {
    return Files
  }
  
  // 纯文本
  if (name.includes('.txt') || name.includes('text')) {
    return Document
  }
  
  // 默认图标
  return Document
}

// 获取来源标题（优先显示有意义的标题）
const getSourceTitle = (source) => {
  if (!source) return ''

  // 优先显示内容预览（第一行内容）
  if (source.content_preview) {
    return source.content_preview
  }

  const sectionTitle = source.location?.section_title
  const pageNumber = source.location?.page_number
  const lineRange = source.location?.line_range

  // 判断章节标题是否有效（不是太长的句子，且包含关键词）
  const isValidTitle = (title) => {
    if (!title || title.length > 50) return false
    // 包含这些关键词的标题更可能是有效标题
    const keywords = ['制度', '规定', '办法', '条例', '守则', '手册', '指南', '章', '节', '条', '附录', '附则']
    return keywords.some(kw => title.includes(kw))
  }

  // 构建位置信息
  let locationInfo = ''

  // 优先显示页码
  if (pageNumber) {
    locationInfo = `第${pageNumber}页`
  } else if (lineRange) {
    // 没有页码时显示行号
    locationInfo = `第${lineRange}行`
  }

  // 如果有有效的章节标题
  if (isValidTitle(sectionTitle)) {
    if (locationInfo) {
      return `${locationInfo} ${sectionTitle}`
    }
    return sectionTitle
  }

  // 如果只有位置信息
  if (locationInfo) {
    return locationInfo
  }

  return ''
}
</script>

<style lang="scss" scoped>
.message-sources {
  margin-top: 12px;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #ebeef5;

  .sources-header {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    font-weight: 600;
    color: #606266;
    margin-bottom: 8px;
  }

  .sources-list {
    .source-item {
      padding: 8px;
      background: #fff;
      border-radius: 4px;
      margin-bottom: 8px;

      &:last-child {
        margin-bottom: 0;
      }

      .source-name {
        font-size: 13px;
        color: #303133;
        font-weight: 500;
      }

      .source-location {
        font-size: 12px;
        color: #909399;
        margin-top: 4px;
      }

      .source-title {
        color: #409eff;
        font-weight: 500;
      }

      .source-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;
      }

      .source-info {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .source-icon {
        color: #409eff;
        font-size: 16px;
      }

      .source-name {
        font-size: 14px;
        color: #303133;
        font-weight: 500;
      }

      .source-location {
        font-size: 12px;
        color: #909399;
        margin-top: 4px;
      }

      .source-title {
        color: #409eff;
        font-weight: 500;
      }

      .source-content-preview {
        margin-top: 8px;
        padding: 8px;
        background: #f8f9fa;
        border-radius: 4px;
        border-left: 3px solid #409eff;
      }

      /* 相关度颜色样式 */
      :deep(.el-tag--success) {
        background-color: #f0f9ff !important;
        border-color: #67c23a !important;
        color: #67c23a !important;
      }

      :deep(.el-tag--warning) {
        background-color: #fdf6ec !important;
        border-color: #e6a23c !important;
        color: #e6a23c !important;
      }

      :deep(.el-tag--danger) {
        background-color: #fef0f0 !important;
        border-color: #f56c6c !important;
        color: #f56c6c !important;
      }

      .preview-label {
        font-size: 11px;
        color: #909399;
        font-weight: 500;
        margin-bottom: 4px;
      }

      .preview-text {
        font-size: 12px;
        color: #606266;
        line-height: 1.4;
      }
    }
  }
}
</style>