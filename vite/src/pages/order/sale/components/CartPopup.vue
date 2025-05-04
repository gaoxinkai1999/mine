<template>
  <div class="cart-popup-container">
    <!-- 自定义遮罩层，不遮挡底部购物车栏 -->
    <div
        v-if="store.showCart"
        class="custom-overlay"
        @click="store.showCart = false"
    ></div>

    <!-- 购物车弹窗内容 -->
    <van-popup
        :close-on-click-overlay="false"
        :overlay="false"
        :show="store.showCart"
        :z-index="2600"
        class="cart-popup"
        position="bottom"
        @update:show="store.showCart = $event"
    >
      <div class="cart-header">
        <span class="cart-title">购物车</span>
        <van-button
            :disabled="!store.cart.length"
            plain
            size="small"
            type="danger"
            @click="store.clearCart()"
        >
          清空
        </van-button>
      </div>

      <div class="cart-content">
        <van-empty v-if="!store.cart.length" description="购物车是空的"/>
        <div v-else class="cart-items-list">
          <div
              v-for="item in store.cart"
              :key="item.id"
              class="cart-item"
          >
            <div class="cart-item-info">
              <div class="cart-item-name">{{ item.name }}</div>
              <div class="cart-item-price-row">
                <div class="price-tag unit-price">
                  <span class="price-label">单价:</span>
                  <span>¥{{ item.price.toFixed(2) }}</span>
                </div>
                <div class="price-tag total-price">
                  <span class="price-label">总价:</span>
                  <span>¥{{ (item.price * item.count).toFixed(2) }}</span>
                </div>
              </div>
              <!-- 新增：显示已选批次详情 -->
              <div v-if="item.batchManaged && item.batchDetails && item.batchDetails.length > 0" class="batch-details-display">
                <span class="batch-label">已选批次:</span>
                <span v-for="bd in item.batchDetails" :key="bd.batchId" class="batch-tag">
                  {{ bd.batchNumber }}: {{ bd.quantity }}
                </span>
                <!-- 可选：添加编辑批次按钮 -->
                <!-- <van-button size="mini" plain type="warning" @click="editItemBatches(item)" class="edit-batch-btn">编辑</van-button> -->
              </div>
            </div>
            <div class="cart-item-actions">
              <van-button
                  class="price-button"
                  :class="{'price-modified': item.price !== item.defaultSalePrice}"
                  plain
                  size="mini"
                  type="primary"
                  @click.stop="showPriceDialog(item)"
              >
                修改
              </van-button>
              <van-stepper
                  v-model="item.count"
                  button-size="22"
                  min="0"
                  theme="round"
                  @change="() => store.updateCart(item)"
              />
            </div>
          </div>
        </div>
      </div>


      <!-- 结算操作栏 -->
      <div class="cart-actions-footer">
        <div class="footer-total-price">
          合计: <span>¥{{ totalPrice.toFixed(2) }}</span>
        </div>
        <van-button
          class="footer-checkout-btn"
          type="primary"
          :disabled="totalCount === 0"
          @click="handleSubmitOrder"
        >
          去结算 ({{ totalCount }})
        </van-button>
      </div>

      <div class="safe-area-bottom"></div>
    </van-popup>
    <!-- 价格编辑对话框 -->

    <van-dialog
      v-model:show="showPriceEdit"
      :title="`修改价格 - ${editingItem?.name || '商品'}`"
      show-cancel-button
      @confirm="updatePrice"
      :z-index="2700"
    >
      <van-field
        v-model="newPrice"
        label="新价格"
        type="number"
        placeholder="请输入新价格"
      />
    </van-dialog>

  </div>
</template>

<script setup>
import {ref} from 'vue';
import {useOrderStore} from '@/stores/order';
import { showConfirmDialog } from 'vant'; // Import showConfirmDialog
import { storeToRefs } from 'pinia'; // Import storeToRefs

const store = useOrderStore();
const { totalPrice, totalCount } = storeToRefs(store); // Get totalPrice and totalCount
const { submitOrder } = store; // Get submitOrder action

// 价格编辑相关
const editingItem = ref(null);
const showPriceEdit = ref(false);
const newPrice = ref('');

// 显示修改价格的对话框
const showPriceDialog = (item) => {
  showPriceEdit.value = true;
  editingItem.value = item;
  newPrice.value = item.price; // 初始化为当前价格，而非默认价
};
const updatePrice = () => {
  if (!editingItem.value) return;

  const price = parseFloat(newPrice.value);
  if (isNaN(price) || price < 0) { // 允许价格为0？根据业务调整
     showToast('请输入有效的价格');
    return;
  }
  
  // 可以在这里添加价格校验逻辑，例如不能低于成本价等
  // if (price < editingItem.value.costPrice) { ... }

  // 直接修改 store 中的 cart item price
  const cartItem = store.cart.find(i => i.id === editingItem.value.id);
  if (cartItem) {
      cartItem.price = price;
      // 如果需要，也同步更新 foods 列表中的价格
      const foodItem = store.foods.find(f => f.id === editingItem.value.id);
      if (foodItem) {
          foodItem.price = price;
      }
  }


  showPriceEdit.value = false;
};


// 可选: 添加编辑批次的方法
// import BatchSelector from './BatchSelector.vue'; // 需要引入
// const showBatchEdit = ref(false);
// const editingBatchItem = ref(null);
// const editItemBatches = (item) => {
//   editingBatchItem.value = item;
//   showBatchEdit.value = true;
// }
// const handleBatchEditConfirm = (batchDetails) => {
//   // ... 类似 ProductList 中的 handleBatchConfirm 逻辑 ...
//   // 调用 store.updateCartWithBatch
//   showBatchEdit.value = false;
// }


// 处理提交订单 (New function)
const handleSubmitOrder = () => {
  // 显示确认弹窗
  showConfirmDialog({
    title: '确认提交订单',
    message: `总计金额：¥${totalPrice.value.toFixed(2)}，确定要提交订单吗？`,
    confirmButtonText: '确认提交',
    cancelButtonText: '再想想',
    zIndex: 2800 // Ensure dialog is above popup
  })
  .then(() => {
    // 用户点击确认，执行提交订单
    // 可以在这里先关闭弹窗，避免用户重复点击
    store.showCart = false;
    submitOrder().then(success => {
      // 可以在成功后执行其他操作，例如跳转页面
      // if (success) { ... }
    });
  })
  .catch(() => {
    // 用户取消，不执行任何操作
  });
};
</script>

<style scoped>
.cart-popup-container {
  position: relative;
  height: 0;
  width: 100%;
  box-sizing: border-box;
  z-index: 2500;
}

/* 自定义遮罩层 */
.custom-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 50px;
  background: rgba(0, 0, 0, 0.6);
  z-index: 2500;
}

.cart-popup {
  border-radius: 16px 16px 0 0;
  max-height: calc(70vh - 50px);
  width: 100%;
  bottom: 50px !important;
  box-sizing: border-box;
}

.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #eee;
  width: 100%;
  box-sizing: border-box;
}

.cart-title {
  font-size: 16px;
  font-weight: 500;
}

.cart-content {
  max-height: calc(70vh - 50px - 56px);
  overflow-y: auto;
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch;
  width: 100%;
  box-sizing: border-box;
}

.cart-items-list {
  padding: 8px 0;
}

.cart-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;
}

.cart-item-info {
  flex: 1;
  min-width: 0;
  padding-right: 8px;
}

.cart-item-name {
  font-size: 14px;
  font-weight: 500;
  color: #323233;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cart-item-price-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
}

.price-tag {
  display: flex;
  align-items: center;
  font-size: 12px;
  border-radius: 4px;
  padding: 2px 4px;
}

.price-label {
  color: #969799;
  margin-right: 4px;
}

.unit-price {
  color: #1989fa;
  background-color: rgba(25, 137, 250, 0.1);
}

.total-price {
  color: #ee0a24;
  background-color: rgba(238, 10, 36, 0.1);
}

.cart-item-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.price-button {
  height: 28px;
  line-height: 26px;
  padding: 0 8px;
  font-size: 12px;
  border-radius: 4px;
}

.price-modified {
  color: #ff6b00 !important;
  border-color: #ff6b00 !important;
  background-color: rgba(255, 107, 0, 0.05);
}


/* 新增批次详情显示样式 */
.batch-details-display {
  margin-top: 8px;
  font-size: 12px;
  color: #646566;
  display: flex;
  flex-wrap: wrap; /* 允许换行 */
  align-items: center;
  gap: 4px; /* 标签之间的间距 */
}

.batch-label {
  font-weight: 500;
  margin-right: 4px;
  flex-shrink: 0; /* 防止标签被压缩 */
}

.batch-tag {
  background-color: #ecf5ff; /* 淡蓝色背景 */
  color: #409eff; /* 蓝色文字 */
  padding: 2px 5px;
  border-radius: 4px;
  white-space: nowrap; /* 防止批次信息内部换行 */
}

.edit-batch-btn {
  margin-left: 8px; /* 与批次标签保持距离 */
  height: 20px;
  padding: 0 6px;
  font-size: 11px;
}


/* 空购物车样式 */
:deep(.van-empty) {
  padding: 32px 0;
}

/* 小屏幕适配 */
@media (max-width: 375px) {
  .custom-overlay {
    bottom: 48px;
  }

  .cart-popup {
    bottom: 48px !important;
    max-height: calc(70vh - 48px);
  }

  .cart-content {
    max-height: calc(70vh - 48px - 56px);
  }

  .cart-item-price-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}

/* 大屏幕适配 */
@media (min-width: 769px) {
  .custom-overlay {
    bottom: 56px;
  }

  .cart-popup {
    bottom: 56px !important;
    max-height: calc(70vh - 56px);
    max-width: 1024px;
    margin: 0 auto;
    left: 50%;
    transform: translateX(-50%);
  }

  .cart-content {
    max-height: calc(70vh - 56px - 56px);
  }
}

/* iOS底部安全区域适配 */
@supports (padding-bottom: env(safe-area-inset-bottom)) {
  .custom-overlay {
    bottom: calc(50px + env(safe-area-inset-bottom, 0px));
  }

  .cart-popup {
    bottom: calc(50px + env(safe-area-inset-bottom, 0px)) !important;
  }

  @media (max-width: 375px) {
    .custom-overlay {
      bottom: calc(48px + env(safe-area-inset-bottom, 0px));
    }

    .cart-popup {
      bottom: calc(48px + env(safe-area-inset-bottom, 0px)) !important;
    }
  }

  @media (min-width: 769px) {
    .custom-overlay {
      bottom: calc(56px + env(safe-area-inset-bottom, 0px));
    }

    .cart-popup {
      bottom: calc(56px + env(safe-area-inset-bottom, 0px)) !important;
    }
  }
}

:deep(.van-stepper) {
  --van-stepper-input-width: 32px;
  --van-stepper-button-icon-color: #1989fa;
}

.safe-area-bottom {
  height: env(safe-area-inset-bottom, 0);
}

/* 新增结算操作栏样式 */
.cart-actions-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-top: 1px solid #f5f5f5;
  background-color: #fff; /* Ensure background */
}

.footer-total-price {
  font-size: 14px;
  color: #323233;
}

.footer-total-price span {
  font-size: 18px;
  font-weight: 500;
  color: #ee0a24;
  margin-left: 4px;
}

.footer-checkout-btn {
  height: 36px;
  font-weight: 500;
  border-radius: 18px;
  min-width: 110px;
}

</style>

