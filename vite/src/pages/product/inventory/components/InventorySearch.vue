<template>
  <div class="search-bar">
    <van-search
      :model-value="searchQuery"
      @update:model-value="$emit('update:search-query', $event)"
      placeholder="搜索产品名称"
      shape="round"
      background="transparent"
      @search="$emit('search')"
      clearable
      :style="{ '--van-search-input-height': '32px' }"
    />
  </div>
</template>

<script setup>
defineProps({
  searchQuery: {
    type: String,
    default: ''
  }
});

defineEmits(['update:search-query', 'search']);
</script>

<style scoped>
/* 搜索栏 */
.search-bar {
  padding: 0 16px 8px;
}

/* 适配安卓 */
:deep(.van-search) {
  padding: 8px 12px;
}

:deep(.van-search__content) {
  background-color: #f5f5f5;
}

@media screen and (max-width: 375px) {
  .search-bar {
    padding: 0 12px 8px;
  }
  
  :deep(.van-search) {
    padding: 6px 8px;
  }
}

@media screen and (max-width: 320px) {
  .search-bar {
    padding: 0 8px 6px;
  }
}
</style> 