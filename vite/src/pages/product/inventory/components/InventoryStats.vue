<template>
  <div class="stats-card">
    <div class="stats-header">
      <van-icon name="bar-chart-o" size="18" class="stats-icon" />
      <span>库存统计</span>
    </div>
    <div class="stats-content">
      <div class="stats-item">
        <div class="stats-value">{{ totalQuantity }}</div>
        <div class="stats-label">总库存数量</div>
      </div>
      <div class="stats-divider"></div>
      <div class="stats-item">
        <div class="stats-value highlight">¥{{ totalAmount.toFixed(2) }}</div>
        <div class="stats-label">总库存金额</div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  totalQuantity: {
    type: Number,
    default: 0
  },
  totalAmount: {
    type: Number,
    default: 0
  }
});
</script>

<style scoped>
/* 统计卡片 */
.stats-card {
  margin: 12px 16px;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.stats-header {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  display: flex;
  align-items: center;
}

.stats-icon {
  margin-right: 8px;
  color: #3f51b5;
}

.stats-content {
  display: flex;
  padding: 16px;
}

.stats-item {
  flex: 1;
  text-align: center;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.stats-divider {
  width: 1px;
  background-color: #eee;
  margin: 0 16px;
}

.stats-value {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin-bottom: 4px;
  word-break: break-all;
}

.stats-value.highlight {
  color: #f5222d;
}

.stats-label {
  font-size: 13px;
  color: #999;
}

/* 适配小屏设备 */
@media screen and (max-width: 375px) {
  .stats-card {
    margin: 10px 12px;
  }
  
  .stats-header {
    padding: 10px 14px;
    font-size: 15px;
  }
  
  .stats-content {
    padding: 14px 12px;
  }
  
  .stats-value {
    font-size: 20px;
  }
  
  .stats-label {
    font-size: 12px;
  }
  
  .stats-divider {
    margin: 0 12px;
  }
}

/* 极小屏设备 */
@media screen and (max-width: 320px) {
  .stats-card {
    margin: 8px 10px;
  }
  
  .stats-header {
    padding: 8px 12px;
    font-size: 14px;
  }
  
  .stats-content {
    padding: 12px 8px;
  }
  
  .stats-value {
    font-size: 18px;
  }
  
  .stats-divider {
    margin: 0 8px;
  }
}
</style> 