<template>
  <div class="top-navbar">
    <div class="title">产品库存管理</div>
    <div class="action-buttons">
      <van-button size="small" type="primary" @click="$emit('toggle-expand')" class="expand-btn">
        {{ isExpandAll ? '收起批次' : '展开批次' }}
      </van-button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  isExpandAll: {
    type: Boolean,
    default: false
  }
});

defineEmits(['toggle-expand']);
</script>

<style scoped>
/* 顶部导航栏 */
.top-navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #3f51b5, #2196f3);
  color: #fff;
  padding: 12px 16px;
  position: sticky;
  top: 0;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.title {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.expand-btn {
  border-radius: 20px;
  font-weight: 500;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background-color: rgba(255, 255, 255, 0.2);
  color: white;
}
</style> 