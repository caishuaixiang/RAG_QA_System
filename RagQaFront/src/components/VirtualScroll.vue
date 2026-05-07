<template>
  <div class="virtual-scroll-container" ref="container" @scroll="handleScroll">
    <div class="scroll-content" :style="{ height: totalHeight + 'px' }">
      <div
        v-for="item in visibleItems"
        :key="item.id"
        :class="['virtual-item', item.className]"
        :style="{
          position: 'absolute',
          top: item.offset + 'px',
          width: '100%'
        }"
      >
        <slot :item="item.data"></slot>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'

const props = defineProps({
  items: {
    type: Array,
    required: true
  },
  itemHeight: {
    type: Number,
    default: 60
  },
  bufferSize: {
    type: Number,
    default: 5
  }
})

const emit = defineEmits(['scroll'])

const container = ref(null)
const scrollTop = ref(0)
const containerHeight = ref(0)

// 计算总高度
const totalHeight = computed(() => props.items.length * props.itemHeight)

// 计算可见项目
const visibleItems = computed(() => {
  if (!container.value) return []
  
  const startIndex = Math.max(0, Math.floor(scrollTop.value / props.itemHeight) - props.bufferSize)
  const endIndex = Math.min(
    props.items.length - 1,
    Math.ceil((scrollTop.value + containerHeight.value) / props.itemHeight) + props.bufferSize
  )
  
  const visible = []
  for (let i = startIndex; i <= endIndex; i++) {
    const item = props.items[i]
    visible.push({
      id: i,
      data: item,
      offset: i * props.itemHeight,
      className: item.className || ''
    })
  }
  
  return visible
})

// 处理滚动事件
const handleScroll = (event) => {
  scrollTop.value = event.target.scrollTop
  emit('scroll', { scrollTop: scrollTop.value, visibleItems: visibleItems.value })
}

// 获取容器高度
const updateContainerHeight = () => {
  if (container.value) {
    containerHeight.value = container.value.clientHeight
  }
}

// 滚动到指定位置
const scrollToIndex = (index) => {
  if (container.value) {
    container.value.scrollTop = index * props.itemHeight
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (container.value) {
    container.value.scrollTop = totalHeight.value
  }
}

onMounted(() => {
  nextTick(() => {
    updateContainerHeight()
    window.addEventListener('resize', updateContainerHeight)
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', updateContainerHeight)
})

defineExpose({
  scrollToIndex,
  scrollToBottom,
  updateContainerHeight
})
</script>

<style lang="scss" scoped>
.virtual-scroll-container {
  height: 100%;
  overflow-y: auto;
  position: relative;
  
  .scroll-content {
    position: relative;
  }
  
  .virtual-item {
    will-change: transform;
  }
}
</style>