<template>
  <div class="tool-group">
    <h2 class="group-title">{{ title }}</h2>
    <van-cell-group inset>
      <van-cell
          v-for="item in items"
          :key="item.name"
          :title="item.title"
          :to="{ name: item.name, params: item.params }"
          is-link
          class="tool-item"
      />
    </van-cell-group>
  </div>
</template>

<script>
export default {
  name: 'ToolGroup',
  props: {
    title: String,
    items: {
      type: Array,
      default: () => []
    }
  }
}
</script>

<style scoped>
.tool-group {
  background-color: #ffffff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  overflow: hidden;
  margin-bottom: 12px;
}

.group-title {
  font-size: 14px;
  color: #909399;
  padding: 8px 16px;
  margin: 0;
  background-color: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.tool-item {
  transition: background-color 0.2s;
}

.tool-item:not(:last-child) {
  border-bottom: 1px solid #ebeef5;
}

.tool-item:active {
  background-color: #f5f7fa;
}

/* 覆盖van-cell的默认样式 */
:deep(.van-cell) {
  align-items: center;
  padding: 12px 16px;
}

:deep(.van-cell__title) {
  font-size: 14px;
  color: #303133;
}
</style>