<template>
  <div class="product-list">
    <div
      v-for="item in products" 
      :key="item.id" 
      class="product-card"
      :class="{'with-batch': item.batchManaged}"
    >
      <div class="product-header" @click="$emit('toggle-row', item)">
        <div class="product-info">
          <div class="product-name">{{ item.name }}
            <template v-if="item.children && item.batchManaged">
              <van-tag type="primary" size="small" class="batch-tag">批次管理</van-tag>
            </template>
          </div>
          <div class="product-meta">
            <template v-if="item.children && item.batchManaged">
              <span class="batch-count">{{ item.batchCount }}个批次</span>
            </template>
          </div>
        </div>
        
        <div class="product-data">
          <div class="stock-info">
            <div class="quantity-label">库存:</div>
            <div class="quantity-value">{{ item.quantity }}</div>
          </div>
          <div class="amount-info">
            <div class="amount-value">¥{{ formatAmount(item) }}</div>
          </div>
        </div>
        
        <div class="product-actions">
          <template v-if="!item.children">
            <van-button type="primary" size="small" @click.stop="$emit('edit-inventory', item)" class="edit-btn">
              <van-icon name="edit" />
              <span>修改</span>
            </van-button>
          </template>
          <template v-else-if="item.batchManaged">
            <van-icon 
              :name="expandedRows.includes(item.id) ? 'arrow-up' : 'arrow-down'" 
              class="expand-icon"
            />
          </template>
        </div>
      </div>
      
      <!-- 批次详情部分 -->
      <div 
        v-if="item.children && item.batchManaged && expandedRows.includes(item.id)" 
        class="batch-container"
      >
        <div class="batch-list">
          <div class="batch-list-header">
            <div class="batch-cell batch-number">批次号</div>
            <div class="batch-cell batch-date">生产/过期</div>
            <div class="batch-cell batch-quantity">数量</div>
            <div class="batch-cell batch-amount">金额</div>
            <div class="batch-cell batch-action">操作</div>
          </div>
          
          <div 
            v-for="(batch, index) in item.children" 
            :key="index"
            class="batch-list-item"
            :class="{'even-row': index % 2 === 0}"
          >
            <div class="batch-cell batch-number">
              <div class="batch-number-text">{{ batch.batchNumber || '无批次' }}</div>
            </div>
            
            <div class="batch-cell batch-date">
              <div class="date-info">
                <div class="production-date">
                  <van-icon name="calendar-o" size="12" />
                  <span>{{ formatDate(batch.productionDate) }}</span>
                </div>
                <div class="expiration-date">
                  <van-icon name="underway-o" size="12" />
                  <span>{{ formatDate(batch.expirationDate) }}</span>
                </div>
              </div>
            </div>
            
            <div class="batch-cell batch-quantity">
              <div class="quantity-badge">{{ batch.quantity }}</div>
            </div>
            
            <div class="batch-cell batch-amount">
              <div class="amount-text">¥{{ formatAmount(batch) }}</div>
            </div>
            
            <div class="batch-cell batch-action">
              <div class="action-buttons">
                <van-button
                  type="primary"
                  plain
                  size="mini"
                  @click.stop="$emit('edit-inventory', batch, item.id.split('-')[0])"
                  class="batch-edit-btn"
                >
                  数量
                </van-button>
                <van-button
                  type="warning"
                  size="mini"
                  icon="setting-o"
                  @click.stop="$emit('edit-batch-info', batch, item.id.split('-')[0])"
                  class="batch-info-edit-btn"
                >
                  <!-- 信息 -->
                </van-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 空状态 -->
    <div v-if="products.length === 0" class="empty-state">
      <van-empty 
        description="没有找到相关产品" 
        image="search"
      />
    </div>
  </div>
</template>

<script setup>
defineProps({
  products: {
    type: Array,
    default: () => []
  },
  expandedRows: {
    type: Array,
    default: () => []
  }
});

defineEmits(['toggle-row', 'edit-inventory', 'edit-batch-info']);

// 格式化金额
const formatAmount = (row) => {
  const amount = Number(row?.amount); // 尝试将 amount 转换为数字
  if (!isNaN(amount)) { // 检查是否为有效数字
    return amount.toFixed(2);
  }
  return '0.00'; // 如果不是有效数字，返回默认值
};

// 格式化日期
const formatDate = (date) => {
  if (!date) return '-';
  return new Date(date).toLocaleDateString('zh-CN');
};
</script>

<style scoped>
/* 产品列表 */
.product-list {
  padding: 0 12px 16px;
}

.product-card {
  background-color: #fff;
  border-radius: 10px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  transition: all 0.3s ease;
}

.product-card.with-batch {
  border-left: 3px solid #3f51b5;
}

.product-header {
  display: flex;
  padding: 14px 12px;
  position: relative;
  transition: background-color 0.2s;
  cursor: pointer;
  -webkit-tap-highlight-color: rgba(0, 0, 0, 0.05);
}

.product-header:active {
  background-color: rgba(0, 0, 0, 0.05);
}

.product-info {
  flex: 3;
  overflow: hidden;
}

.product-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  line-height: 1.4;
}

.batch-tag {
  margin-left: 8px;
  font-size: 10px;
  padding: 0 6px;
  height: 18px;
  line-height: 18px;
  vertical-align: middle;
  border-radius: 10px;
}

.product-meta {
  font-size: 12px;
  color: #999;
}

.batch-count {
  display: inline-flex;
  align-items: center;
}

.product-data {
  flex: 2;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  text-align: right;
}

.stock-info {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}

.quantity-label {
  font-size: 13px;
  color: #666;
  margin-right: 6px;
}

.quantity-value {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.amount-info {
  font-size: 14px;
  font-weight: 600;
  color: #f5222d;
}

.product-actions {
  margin-left: 10px;
  display: flex;
  align-items: center;
}

.edit-btn {
  border-radius: 20px;
  font-size: 12px;
  height: 28px;
  min-width: 64px;
}

.edit-btn :deep(.van-icon) {
  margin-right: 4px;
  font-size: 14px;
}

.expand-icon {
  font-size: 16px;
  padding: 8px;
  color: #666;
  margin-right: -8px;
}

/* 批次详情 */
.batch-container {
  border-top: 1px solid #f0f0f0;
  overflow: hidden;
  animation: slideDown 0.3s ease;
  -webkit-overflow-scrolling: touch;
}

@keyframes slideDown {
  from {
    max-height: 0;
    opacity: 0;
  }
  to {
    max-height: 1000px;
    opacity: 1;
  }
}

.batch-list {
  padding: 8px 12px 12px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.batch-list-header {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 12px;
  font-weight: 600;
  color: #666;
}

.batch-list-item {
  display: flex;
  border-bottom: 1px solid #f5f5f5;
  font-size: 13px;
  transition: background-color 0.2s;
}

.batch-list-item:last-child {
  border-bottom: none;
}

.even-row {
  background-color: #f9f9f9;
}

.batch-cell {
  padding: 10px 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.batch-number {
  flex: 2;
  justify-content: flex-start;
}

.batch-number-text {
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 90px;
}

.batch-date {
  flex: 3;
}

.date-info {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.production-date,
.expiration-date {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #666;
  margin-bottom: 2px;
}

.production-date :deep(.van-icon),
.expiration-date :deep(.van-icon) {
  margin-right: 4px;
  flex-shrink: 0;
}

.batch-quantity {
  flex: 1;
}

.quantity-badge {
  background-color: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 500;
  font-size: 12px;
  min-width: 40px;
  text-align: center;
}

.batch-amount {
  flex: 2;
  padding-left: 2px;
  padding-right: 2px;
}

.amount-text {
  color: #f5222d;
  font-weight: 500;
}

.batch-action {
  flex: 2;
}

.action-buttons {
  display: flex;
  flex-wrap: nowrap;
  gap: 4px;
  justify-content: center;
}

.batch-edit-btn,
.batch-info-edit-btn {
  border-radius: 15px;
  font-size: 11px;
  height: 24px;
  line-height: 22px;
  padding: 0 4px;
  flex-shrink: 0;
}

/* 空状态 */
.empty-state {
  margin-top: 32px;
}

/* 适配小屏和安卓设备 */
@media screen and (max-width: 375px) {
  .product-list {
    padding: 0 8px 16px;
  }
  
  .product-name {
    font-size: 14px;
  }
  
  .product-header {
    padding: 12px 10px;
  }
  
  .product-info {
    flex: 2.5;
  }
  
  .product-data {
    flex: 1.5;
  }
  
  .quantity-value {
    font-size: 15px;
  }
  
  .batch-date {
    flex: 2.5;
    font-size: 11px;
  }
  
  .batch-number {
    flex: 2;
  }
  
  .batch-number-text {
    max-width: 70px;
    font-size: 12px;
  }
  
  .batch-quantity {
    flex: 1;
  }
  
  .batch-amount {
    flex: 1.5;
    font-size: 12px;
  }
  
  .batch-action {
    flex: 2;
  }
  
  .action-buttons {
    flex-direction: row;
    justify-content: center;
  }
  
  .batch-edit-btn, .batch-info-edit-btn {
    padding: 0 4px;
    font-size: 10px;
  }
  
  .batch-list {
    padding: 6px 8px 10px;
  }
  
  .batch-cell {
    padding: 8px 2px;
  }
}

/* 极小屏设备适配 */
@media screen and (max-width: 320px) {
  .product-list {
    padding: 0 4px 16px;
  }
  
  .product-header {
    padding: 10px 8px;
  }
  
  .product-name {
    font-size: 13px;
  }
  
  .product-info {
    flex: 2;
  }
  
  .product-data {
    flex: 1.2;
  }
  
  .quantity-value {
    font-size: 14px;
  }
  
  .amount-info {
    font-size: 13px;
  }
  
  .batch-date {
    flex: 2;
    font-size: 10px;
  }
  
  .batch-number {
    flex: 1.5;
  }
  
  .batch-number-text {
    max-width: 60px;
  }
  
  .action-buttons {
    flex-direction: column;
    gap: 3px;
  }
  
  .batch-edit-btn, .batch-info-edit-btn {
    width: 100%;
    font-size: 10px;
    height: 22px;
    line-height: 20px;
  }
}

/* 针对安卓设备的特殊优化 */
@supports (-webkit-touch-callout: none) {
  .product-header {
    -webkit-tap-highlight-color: transparent;
  }
  
  .batch-edit-btn, .batch-info-edit-btn {
    /* 安卓按钮点击区域优化 */
    position: relative;
    overflow: visible;
  }
  
  .batch-edit-btn::after, .batch-info-edit-btn::after {
    content: '';
    position: absolute;
    top: -5px;
    left: -5px;
    right: -5px;
    bottom: -5px;
    z-index: 1;
  }
}
</style> 