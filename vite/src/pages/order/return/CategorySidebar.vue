<template>
  <div class="sidebar">
    <van-sidebar v-model="activeIndex">
      <van-sidebar-item
          v-for="category in categories"
          :key="category.id"
          :title="category.name"
      />
    </van-sidebar>
  </div>
</template>

<script>
export default {
  name: 'CategorySidebar',
  props: {
    categories: {
      type: Array,
      required: true,
      default: () => []
    },
    modelValue: {
      type: Number,
      default: 0
    }
  },
  computed: {
    activeIndex: {
      get() {
        return this.modelValue
      },
      set(value) {
        this.$emit('update:modelValue', value)
      }
    }
  }
}
</script>

<style scoped>
.sidebar {
  width: 80px; /* 固定宽度，避免使用视窗单位 */
  max-width: 80px; /* 同样设置最大宽度 */
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden; /* 禁止水平滚动 */
  flex-shrink: 0; /* 禁止侧边栏收缩 */
  box-sizing: border-box;
  background-color: #fff7ee; /* 退货系统使用浅橙色背景 */
  border-right: 1px solid #ffe4cc; /* 使用浅橙色边框 */
}

.sidebar::-webkit-scrollbar {
  width: 4px;
}

.sidebar::-webkit-scrollbar-thumb {
  background: #ffbb66; /* 退货系统使用橙色滚动条 */
  border-radius: 2px;
}

.sidebar::-webkit-scrollbar-track {
  background: transparent;
}

/* 保证标题不超出范围 */
:deep(.van-sidebar-item) {
  padding: 10px 8px;
  background-color: #fff7ee; /* 确保与父容器一致 */
}

:deep(.van-sidebar-item__text) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
  font-size: 14px; /* 调整字体大小 */
}

:deep(.van-sidebar-item--select) {
  background-color: #fff0e0; /* 更改选中项背景色为浅橙色 */
  color: #ff8800; /* 选中项文字颜色为橙色 */
  border-color: #ff8800; /* 选中项边框颜色为橙色 */
}

/* 小屏幕适配 */
@media (max-width: 320px) {
  .sidebar {
    width: 70px;
    max-width: 70px;
  }
  
  :deep(.van-sidebar-item) {
    padding: 10px 5px;
  }
  
  :deep(.van-sidebar-item__text) {
    font-size: 12px;
  }
}
</style> 