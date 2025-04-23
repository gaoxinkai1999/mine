<template>
  <van-popup
      v-model:show="dialogVisible"
      position="bottom"
      round
      z-index="9999"
      :style="{ height: '70%' }"
  >
    <div class="batch-selector">
      <!-- 标题栏 -->
      <div class="popup-header">
        <div class="popup-title">选择{{ productName }}的批次</div>
        <van-icon name="cross" @click="dialogVisible = false" />
      </div>

      <!-- 批次列表 -->
      <div class="batch-list">
        <div v-for="batch in batchStocks" :key="batch.batchId" class="batch-item">
          <div class="batch-info">
            <div class="batch-number">批次 {{ batch.batchNumber }}</div>
            <div class="batch-stock">可用库存: {{ batch.quantity }}</div>
          </div>
          <van-stepper
              v-model="form[batch.batchId]"
              :min="0"
              :max="batch.quantity"
              theme="round"
              button-size="22"
          />
        </div>
      </div>
    </div>
  </van-popup>
</template>

<script setup>
import { ref, computed, watch } from 'vue';

const props = defineProps({
  visible: {
    type: Boolean,
    required: true
  },
  productName: {
    type: String,
    required: true,
    default: '未知商品'
  },
  batchStocks: {
    type: Array,
    required: true,
    default: () => []
  },

});

const emit = defineEmits(['update:visible', 'confirm']);

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
});

// 表单数据：批次ID -> 选择数量的映射
const form = ref({});

// 初始化表单数据
const initForm = () => {
  form.value = {};
  props.batchStocks.forEach(batch => {
    form.value[batch.batchId] = 0;
  });
};




// 处理确认
const handleConfirm = () => {

  
  const batchDetails = props.batchStocks
    .map(batch => ({
      batchId: batch.batchId,
      batchNumber: batch.batchNumber,
      quantity: form.value[batch.batchId] || 0
    }))
    .filter(detail => detail.quantity > 0);

  emit('confirm', batchDetails);
  dialogVisible.value = false;
};

// 监听对话框显示状态
watch(() => props.visible, (val) => {
  if (val) {
    initForm();
  }
});
</script>

<style scoped>
.batch-selector {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #eee;
}

.popup-title {
  font-size: 16px;
  font-weight: bold;
}

.quantity-summary {
  padding: 12px 16px;
  background: #f7f8fa;
  display: flex;
  gap: 24px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.label {
  color: #666;
  font-size: 14px;
}

.value {
  font-size: 16px;
  font-weight: bold;
}

.batch-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.batch-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.batch-info {
  flex: 1;
}

.batch-number {
  font-size: 15px;
  margin-bottom: 4px;
}

.batch-stock {
  font-size: 13px;
  color: #999;
}

.popup-footer {
  padding: 16px;
  background: #fff;
  border-top: 1px solid #eee;
}

.text-danger {
  color: var(--van-danger-color);
}

/* 滚动条美化 */
.batch-list::-webkit-scrollbar {
  width: 4px;
}

.batch-list::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 2px;
}

.batch-list::-webkit-scrollbar-track {
  background: transparent;
}
</style>