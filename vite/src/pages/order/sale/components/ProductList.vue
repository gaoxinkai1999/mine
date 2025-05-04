<template>
  <div class="product-list">
    <van-empty v-if="store.currentFoods.length === 0" description="没有找到商品"/>
    <van-card
        v-for="(item) in store.currentFoods"
        :key="item.id"
        :price="`${item.price}`"
        :title="item.name"
    >
      <template #tags>
        <!-- 显示总库存 -->
        <van-tag v-if="item.productStockDTO.totalInventory < 10" plain type="danger">剩{{ item.productStockDTO.totalInventory }}件</van-tag>
        <van-tag v-else plain type="success">库存充足剩{{ item.productStockDTO.totalInventory }}件</van-tag>
        <!-- 如果是批次商品且已选批次，可以考虑显示批次信息 -->
         <div v-if="item.batchManaged && item.count > 0 && cartItemBatches(item.id)" class="selected-batches-summary">
           <van-tag type="primary" plain size="mini">已选批次:</van-tag>
           <van-tag
             v-for="bd in cartItemBatches(item.id)"
             :key="bd.batchId"
             type="warning"
             plain
             size="mini"
             class="batch-summary-tag"
           >
             {{ bd.batchNumber }}: {{ bd.quantity }}
           </van-tag>
         </div>
      </template>

      <template #footer>
        <van-stepper
            v-model="item.count"
            :max="item.productStockDTO.totalInventory"
            button-size="22"
            min="0"
            theme="round"
            integer
            @change="handleStepperChange(item)"
            :disabled="item.batchManaged"
            :input-readonly="item.batchManaged"
        />
        <!-- 为批次管理的商品添加一个明确的“选择批次”按钮 -->
        <van-button
          v-if="item.batchManaged"
          size="mini"
          type="primary"
          icon="add-o"
          @click="openBatchSelector(item)"
          class="batch-select-btn"
        >
          {{ item.count > 0 ? '修改批次' : '选择批次' }}
        </van-button>
      </template>
    </van-card>

    <!-- 批次选择器弹窗 -->
    <BatchSelector
      v-model:visible="showBatchSelector"
      :product-name="currentBatchItem?.name || ''"
      :batch-stocks="currentBatchItem?.productStockDTO?.batchStocks || []"
      :initial-selection="getInitialBatchSelection(currentBatchItem?.id)"
      @confirm="handleBatchConfirm"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'; // 引入 ref 和 computed
import { useOrderStore } from '@/stores/order';
import BatchSelector from './BatchSelector.vue'; // 引入批次选择器组件

const store = useOrderStore();

const showBatchSelector = ref(false);
const currentBatchItem = ref(null); // 用于存储当前操作的批次商品

// 获取购物车中特定商品的已选批次信息 (用于显示和初始化选择器)
const cartItemBatches = computed(() => (productId) => {
  const cartItem = store.cart.find(item => item.id === productId);
  return cartItem?.batchDetails || null;
});

// 为 BatchSelector 提供初始选中值
const getInitialBatchSelection = (productId) => {
  if (!productId) return [];
  const details = cartItemBatches.value(productId);
  // BatchSelector 需要的是 { batchId: quantity } 格式
  if (!details) return {};
  return details.reduce((acc, detail) => {
    acc[detail.batchId] = detail.quantity;
    return acc;
  }, {});
};


// 处理步进器值变化事件（仅非批次商品）
const handleStepperChange = (item) => {
  // @change 事件触发时，v-model 已经更新了 item.count
  // 只处理非批次商品
  if (!item.batchManaged) {
     store.updateCart({
        ...item, // 传递当前 item 的所有信息
        count: item.count // 传递最新的 count
    });
  }
  // 批次商品的数量由 handleBatchConfirm 更新，这里不需要处理
};


// 点击 "选择/修改批次" 按钮
const openBatchSelector = (item) => {
  currentBatchItem.value = item;
  showBatchSelector.value = true;
};

// 处理批次选择器确认事件
const handleBatchConfirm = (batchDetails) => {
  // batchDetails 从 BatchSelector 传来，格式为 { batchId: quantity }
  // 需要转换成 store 需要的格式 [{ batchId, batchNumber, quantity }]
  if (currentBatchItem.value) {
    const productBatches = currentBatchItem.value.productStockDTO?.batchStocks || [];
    const formattedDetails = Object.entries(batchDetails)
      .map(([batchId, quantity]) => {
        const batchInfo = productBatches.find(b => b.batchId === parseInt(batchId)); // ID 可能是字符串
        return {
          batchId: parseInt(batchId),
          batchNumber: batchInfo?.batchNumber || '未知批号', // 获取批号
          quantity: quantity
        };
      })
      .filter(detail => detail.quantity > 0); // 过滤掉数量为0的

    store.updateCartWithBatch({
      productId: currentBatchItem.value.id,
      batchDetails: formattedDetails
    });
  }
  showBatchSelector.value = false; // 关闭弹窗
};

</script>
<style scoped>
.product-list {
  flex: 1;
  min-width: 0; /* 允许内容收缩 */
  padding: 12px;
  overflow-y: auto;
  overflow-x: hidden; /* 明确禁止水平滚动 */
  -webkit-overflow-scrolling: touch;
  width: calc(100% - 80px); /* 减去侧边栏宽度 */
  box-sizing: border-box;
  height: 100%; /* 确保高度为100% */
  position: relative; /* 添加相对定位 */
}

/* 卡片样式优化 */
.product-list :deep(.van-card) {
  margin-bottom: 12px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  position: relative;
  width: 100%;
  box-sizing: border-box;
  padding: 12px;
  min-height: 115px; /* 增加最小高度以容纳批次按钮 */
  background-color: #fff; /* 确保背景是白色 */
}

/* 内容区域布局优化 */
.product-list :deep(.van-card__content) {
  /* 调整右边距以适应步进器和批次按钮 */
  padding-right: 95px;
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-sizing: border-box;
}

/* 标题样式优化 */
.product-list :deep(.van-card__title) {
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  margin-bottom: 4px;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2; /* 最多显示两行 */
  -webkit-box-orient: vertical;
  word-break: break-word;
  box-sizing: border-box;
}

/* 标签容器样式 */
.product-list :deep(.van-card__tags) {
  width: 100%;
  margin-top: 4px;
  display: flex;
  flex-direction: column; /* 改为纵向排列 */
  align-items: flex-start; /* 左对齐 */
  gap: 4px;
  min-height: 20px; /* 确保有足够空间显示标签 */
  box-sizing: border-box;
}

/* 标签样式 */
.product-list :deep(.van-tag) {
  margin: 0;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  box-sizing: border-box;
}

/* 已选批次概要样式 */
.selected-batches-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
  margin-top: 4px; /* 与库存标签保持间距 */
}
.batch-summary-tag {
  margin-left: 2px; /* 批次标签之间的小间距 */
}


/* 价格样式 */
.product-list :deep(.van-card__price) {
  color: #ff4444;
  font-size: 16px;
  font-weight: bold;
  margin-top: 4px;
  box-sizing: border-box;
}

/* 底部操作区（步进器和按钮） */
.product-list :deep(.van-card__footer) {
  position: absolute;
  right: 12px;
  bottom: 12px;
  display: flex; /* 使用 flex 布局 */
  flex-direction: column; /* 垂直排列 */
  align-items: flex-end; /* 右对齐 */
  gap: 8px; /* 步进器和按钮之间的间距 */
  width: auto;
  z-index: 1;
  box-sizing: border-box;
}

/* 步进器样式优化 */
.product-list :deep(.van-stepper) {
  /* 移除 transform 以避免影响布局 */
  /* transform: scale(0.9); */
  /* transform-origin: right bottom; */
  box-sizing: border-box;
}
/* 禁用状态下的步进器样式 */
.product-list :deep(.van-stepper--disabled) {
  opacity: 0.5; /* 降低透明度以示禁用 */
}
.product-list :deep(.van-stepper--disabled) .van-stepper__input {
  background-color: #f7f8fa; /* 输入框背景变灰 */
  color: #c8c9cc;
}
.product-list :deep(.van-stepper--disabled) .van-stepper__plus,
.product-list :deep(.van-stepper--disabled) .van-stepper__minus {
   background-color: #f2f3f5; /* 按钮背景变灰 */
   color: #c8c9cc; /* 图标颜色变灰 */
}


/* 选择批次按钮样式 */
.batch-select-btn {
  height: 24px; /* 调整按钮高度 */
  padding: 0 8px; /* 调整内边距 */
  font-size: 12px; /* 调整字体大小 */
  border-radius: 4px;
}


/* 滚动条样式 */
.product-list::-webkit-scrollbar {
  width: 4px;
}

.product-list::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 2px;
}

.product-list::-webkit-scrollbar-track {
  background: transparent;
}

/* 小屏幕适配 */
@media (max-width: 375px) {
  .product-list {
    width: calc(100% - 70px); /* 小屏幕调整侧边栏宽度 */
    padding: 8px;
  }
  
  .product-list :deep(.van-card) {
    padding: 8px;
    min-height: 110px; /* 调整最小高度 */
  }
  
  .product-list :deep(.van-card__content) {
    padding-right: 85px; /* 调整右边距 */
  }

  .product-list :deep(.van-card__title) {
    font-size: 14px;
  }
  
  .product-list :deep(.van-card__footer) {
    right: 8px;
    bottom: 8px;
    gap: 6px; /* 调整间距 */
  }

  .batch-select-btn {
    height: 22px;
    padding: 0 6px;
    font-size: 11px;
  }
}
</style>

