<template>
  <!-- 将 $attrs 绑定到这个根 div 上 -->
  <div class="product-list" v-bind="$attrs">
    <van-empty v-if="products.length === 0" description="没有找到商品"/>
    <van-card
        v-for="(item, index) in products"
        :key="index"
        :title="item.name"
    >

      <template #footer>
        <van-button icon="plus" size="small" type="primary" @click="openDialog(item)"/>
      </template>
    </van-card>
  </div>

  <!-- 将对话框移到DOM树的更高层级，使其遮罩层可覆盖整个页面 -->
  <teleport to="body">
    <van-dialog
        v-model:show="showDialog"
        :overlay-style="{ zIndex: 3090 }"
        :title="selectedProduct?.name || '产品详情'"
        :z-index="3100"
        class="product-dialog"
        show-cancel-button
        @confirm="handleConfirm(selectedProduct)"
        :close-on-click-overlay="true"
    >
      <template v-if="selectedProduct">
        <van-field name="radio" label="退货类型" required>
          <template #input>
            <van-radio-group v-model="checked" direction="horizontal" @change="handleReturnTypeChange">
              <van-radio name="仅退款">仅退款</van-radio>
              <van-radio name="退货退款">退货退款</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <van-field 
          v-model="number" 
          input-align="center" 
          label="退货金额" 
          required 
          type="number"/>
        <van-field 
          v-if="checked === '退货退款'" 
          v-model="quantity" 
          input-align="center" 
          label="退货数量" 
          required 
          type="number"/>
      </template>

      <!-- 添加批次选择 -->
      <van-field
          v-if="selectedProduct && selectedProduct.isBatchManaged && checked === '退货退款'"
          name="batch"
          label="批次"
          required
          readonly
          :value="selectedProduct.batchId ? (batchLists[selectedProduct.id]?.find(b => b.id === selectedProduct.batchId)?.batchNumber || '请选择批次') : '请选择批次'"
          @click="showBatchPopover = true"
      >
        <template #input>
          <van-popover
              v-model:show="showBatchPopover"
              :actions="batchLists[selectedProduct.id]?.map(batch => ({ text: batch.batchNumber, value: batch })) || []"
              @select="onBatchSelect"
              placement="bottom-start"
              :z-index="3101" 
          >
            <template #reference>
              <div class="batch-select-text">
                {{ selectedProduct.batch.id ? (batchLists[selectedProduct.id]?.find(b => b.id === selectedProduct.batch.id)?.batchNumber || '请选择批次') : '请选择批次' }}
              </div>
            </template>
          </van-popover>
        </template>
      </van-field>
    </van-dialog>
  </teleport>
</template>


<script setup>
import {ref, computed} from 'vue'
import api from "@/api"; // 导入 api
import { showSuccessToast, showFailToast, Popover } from "vant"; // 导入 Popover 和 Toast 组件
// 定义组件接收的 props 和发出的 emits
defineProps({
  products: {
    type: Array,
    required: true
  }
})
defineEmits(['update-cart'])
// 引入store
import {useReturnOrderStore} from '@/stores/returnOrder.js'

const store = useReturnOrderStore()
// 不再需要从 store 获取 products，直接使用 props
// const products = computed(() => store.currentFoods)
const cart = computed(() => store.cart)
const showDialog = ref(false)
const number = ref(0)
const quantity = ref(1) // 默认数量为1
const selectedProduct = ref(null)
const checked = ref('仅退款');

// 使用 ref 来存储批次列表，以便在模板中响应式更新
const batchLists = ref({}); // key: productId, value: batches array
const showBatchPopover = ref(false); // 控制批次 Popover 的显示

// 异步获取批次函数
const fetchBatches = async (item) => {
  // 检查 item 是否有效以及是否需要获取批次
  if (!item || !item.id || !item.isBatchManaged) return;
  // 仅当需要且未获取或获取失败时才重新获取
  if (!batchLists.value[item.id] || batchLists.value[item.id].length === 0) {
    try {
      console.log(`Fetching batches for product ${item.id}`);
      // *** 确保 API 调用路径正确 ***
      const batches = await api.returnorder.getReturnableBatches({ productId: item.id });
      batchLists.value[item.id] = batches || []; // 确保是数组
      // 如果获取到了批次，默认选中第一个
      if (batches && batches.length > 0) {
        // 直接修改 selectedProduct 的 batchId
        selectedProduct.value.batch = batches[0];
      } else {
         // 如果没有批次，将 batchId 设置为 null 或 undefined
         selectedProduct.value.batch = null;
      }
    } catch (e) {
      console.error(`获取批次列表失败 for product ${item.id}:`, e);
      batchLists.value[item.id] = []; // 出错时设置为空数组
      selectedProduct.value.batch = null; // 出错时也设置 batchId 为 null
    }
  }
};


const openDialog = (item) => {
  selectedProduct.value = item;
  showDialog.value = true;
  number.value = 0;
  quantity.value = 1; // 重置数量为1
  showBatchPopover.value = false; // 打开对话框时关闭 Popover
  // 在打开对话框时获取批次列表
  fetchBatches(item);
}

const handleReturnTypeChange = () => {
  // 如果退货类型改变，可以在这里添加其他逻辑
}

// 处理批次选择
const onBatchSelect = (action) => {
  if (selectedProduct.value) {
    selectedProduct.value.batch = action.value;
  }
};


const handleConfirm = (item) => {
  if (number.value <= 0) {
    showFailToast('退货金额必须大于0')
    return
  }

  if (checked.value === '退货退款' && (quantity.value <= 0 || !quantity.value)) {
    showFailToast('退货数量必须大于0')
    return
  }

  // 检查是否需要批次管理，如果需要且没有选择批次，给出提示
  if (item.isBatchManaged && checked.value === '退货退款' && (!item.batch.id || !batchLists.value[item.id] || batchLists.value[item.id].length === 0)) {
      showFailToast('请选择批次');
      return;
  }

  // 调用 store 的 updateCart 方法来更新购物车，而不是直接 push
  store.updateCart({
    id: item.id,
    name: item.name,
    amount: Number(number.value),
    type: checked.value,
    quantity: checked.value === '退货退款' ? Number(quantity.value) : null,
    // 传递 isBatchManaged 属性和 batchId
    isBatchManaged: item.isBatchManaged,
    batchId: (item.isBatchManaged && checked.value === '退货退款') ? item.batch.id : null, // 如果不需要批次管理或不是退货退款，batchId 为 null
    batchNumber: (item.isBatchManaged && checked.value === '退货退款') ?item.batch.batchNumber : null,
  })
}
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

/* 全局对话框样式 */
:deep(.product-dialog) {
  width: 80%;
  max-width: 350px;
  border-radius: 12px;
  overflow: hidden;
}

:deep(.product-dialog .van-dialog__header) {
  background-color: #ff8800; /* 退货系统使用橙色主题 */
  color: white;
  padding: 12px 16px;
}

:deep(.product-dialog .van-button--default) {
  color: #ff8800; /* 退货系统使用橙色主题 */
  border-color: #ff8800;
}

:deep(.product-dialog .van-button--danger) {
  background-color: #ff8800; /* 退货系统使用橙色主题 */
  border-color: #ff8800;
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
  min-height: 100px; /* 确保卡片有最小高度 */
  background-color: #fff; /* 确保背景是白色 */
}

/* 内容区域布局优化 */
.product-list :deep(.van-card__content) {
  padding-right: 70px; /* 为步进器留出空间 */
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
  flex-wrap: wrap;
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

/* 价格样式 */
.product-list :deep(.van-card__price) {
  color: #ff8800; /* 退货系统使用橙色主题 */
  font-size: 16px;
  font-weight: bold;
  margin-top: 4px;
  box-sizing: border-box;
}

/* 步进器位置优化 */
.product-list :deep(.van-card__footer) {
  position: absolute;
  right: 12px;
  bottom: 12px;
  width: auto; /* 调整为自动宽度 */
  max-width: 110px;
  z-index: 1; /* 确保步进器在最上层 */
  box-sizing: border-box;
}

/* 步进器样式优化 */
.product-list :deep(.van-stepper) {
  width: auto; /* 调整为自动宽度 */
  max-width: 110px;
  transform: scale(0.9); /* 稍微缩小步进器 */
  transform-origin: right bottom;
  box-sizing: border-box;
}

/* 自定义步进器颜色 */
.product-list :deep(.van-stepper__plus),
.product-list :deep(.van-stepper__minus) {
  color: #ff8800 !important; /* 退货系统使用橙色主题 */
  background-color: #fff7ee; /* 使用浅橙色背景 */
}

/* 滚动条样式 */
.product-list::-webkit-scrollbar {
  width: 4px;
}

.product-list::-webkit-scrollbar-thumb {
  background: #ffbb66; /* 退货系统使用橙色滚动条 */
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
  }

  .product-list :deep(.van-card__title) {
    font-size: 14px;
  }

  .product-list :deep(.van-card__footer) {
    right: 8px;
    bottom: 8px;
  }
}
</style> 