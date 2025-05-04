<template>
  <van-popup
      v-model:show="dialogVisible"
      position="bottom"
      round
      :close-on-click-overlay="false"
      teleport="body"
      z-index="2005"
      :style="{ height: '70%' }"
  >
    <div class="batch-selector">
      <!-- 标题栏 -->
      <div class="popup-header">
        <div class="popup-title">选择 {{ productName }} 的批次</div>
        <van-icon name="cross" @click="dialogVisible = false" />
      </div>

      <!-- 批次列表 -->
      <div class="batch-list">
        <div v-if="!batchStocks || batchStocks.length === 0" class="van-empty">
          <van-empty description="暂无可用的批次" />
        </div>
        <div v-else v-for="batch in batchStocks" :key="batch.batchId" class="batch-item">
          <div class="batch-info">
            <div class="batch-number">批次 {{ batch.batchNumber }}</div>
            <div class="batch-stock">可用库存: {{ batch.quantity }}</div>
          </div>
          <van-stepper
              v-model="form[batch.batchId]"
              :min="0"
              :max="batch.quantity"
              integer
              theme="round"
              button-size="24"
              input-width="40px"
          />
        </div>
      </div>

      <!-- 底部确认按钮 -->
      <div class="popup-footer">
        <van-button
          type="primary"
          block
          round
          class="confirm-button"
          @click="handleConfirm"
        >
          确认
        </van-button>
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
  batchStocks: { // 可用批次库存 [{ batchId, batchNumber, quantity }]
    type: Array,
    required: true,
    default: () => []
  },
  initialSelection: { // 初始选中的批次数量 { batchId: quantity }
    type: Object,
    default: () => ({})
  }
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
    // 使用 initialSelection 初始化，如果未提供或该批次未选中，则为 0
    form.value[batch.batchId] = props.initialSelection[batch.batchId] || 0;
  });
};




// 处理确认
const handleConfirm = () => {
  // ProductList 需要的格式是 { batchId: quantity }
  const selection = {};
  let totalSelected = 0;
  props.batchStocks.forEach(batch => {
    const selectedQuantity = form.value[batch.batchId] || 0;
    if (selectedQuantity > 0) {
      selection[batch.batchId] = selectedQuantity;
      totalSelected += selectedQuantity;
    }
  });

  // 可以在这里添加校验，例如总选择量是否超过总需求量（如果适用）
  // 但根据当前流程，用户是在这里确定总量的，所以不需要校验

  emit('confirm', selection); // 发出 { batchId: quantity } 格式
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
  padding: 12px 16px; /* 调整内边距 */
  border-bottom: 1px solid #eee;
  flex-shrink: 0; /* 防止头部被压缩 */
}

.popup-title {
  font-size: 16px;
  font-weight: bold;
}

.popup-header .van-icon {
  font-size: 20px; /* 增大关闭图标 */
  color: #969799;
  cursor: pointer;
}

/* 移除 quantity-summary 相关样式 */

.batch-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 16px 16px; /* 调整内边距 */
  -webkit-overflow-scrolling: touch; /* 优化移动端滚动 */
}

.batch-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0; /* 增加垂直内边距 */
  border-bottom: 1px solid #f5f5f5;
}
.batch-item:last-child {
  border-bottom: none; /* 最后一项无下边框 */
}

.batch-info {
  flex: 1;
  padding-right: 16px; /* 与步进器保持距离 */
}

.batch-number {
  font-size: 15px;
  font-weight: 500; /* 加粗批号 */
  margin-bottom: 4px;
  color: #323233;
}

.batch-stock {
  font-size: 13px;
  color: #646566; /* 调整库存颜色 */
}

/* 底部确认按钮区域 */
.popup-footer {
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #eee;
  flex-shrink: 0; /* 防止底部被压缩 */
  box-shadow: 0 -2px 4px rgba(0, 0, 0, 0.03); /* 添加阴影 */
}

/* 确认按钮 */
.confirm-button {
  width: 100%;
  height: 40px; /* 调整按钮高度 */
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

/* 添加底部安全区域适配 */
.popup-footer {
   padding-bottom: calc(12px + env(safe-area-inset-bottom, 0px));
}
.batch-list {
   padding-bottom: calc(16px + env(safe-area-inset-bottom, 0px)); /* 列表底部也加安全区 */
}

</style>