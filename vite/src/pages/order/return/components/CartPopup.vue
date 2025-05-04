<template>
  <div class="cart-popup-container">
    <!-- 自定义遮罩层，不遮挡底部购物车栏 -->
    <div
        v-if="showCart"
        class="custom-overlay"
        @click="closeCart"
    ></div>

    <!-- 购物车弹窗内容 -->
    <van-popup
        :overlay="false"
        :show="showCart"
        :z-index="2600"
        class="cart-popup"
        position="bottom"
        @update:show="$emit('update:show', $event)"
    >
      <div class="cart-header">
        <span class="cart-title">退货车</span>
        <van-button
            :disabled="!cartItems.length"
            plain
            size="small"
            type="danger"
            @click="clearCart"
        >
          清空
        </van-button>
      </div>

      <div class="cart-content">
        <van-empty v-if="!cartItems.length" description="退货车是空的"/>

        <van-cell-group inset>
          <div v-for="(item, index) in cartItems" :key="index" class="cart-item">
            <van-cell>
              <template #title>
                <div class="item-title">
                  <span>{{ item.name }}</span>
                  <van-tag :type="getReturnTypeTag(item.type)" size="mini">{{ item.type }}</van-tag>
                </div>
              </template>
              <template #value>
                <van-button icon="cross" size="small" type="primary" @click="removeCartItem(item)" />
              </template>
            </van-cell>
            <van-cell>
              <template #title>
                <div class="item-details">
                  <div class="detail-row">
                    <span class="detail-label">退款金额：</span>
                    <span class="detail-value">¥{{ item.amount }}</span>
                  </div>
                  <div v-if="item.type === '退货退款'" class="detail-row">
                    <span class="detail-label">退货数量：</span>
                    <span class="detail-value">{{ item.quantity }}</span>
                  </div>
                </div>
              </template>
            </van-cell>
            <!-- 显示批次号 -->
            <van-cell v-if="item.isBatchManaged && item.batchId">
              <template #title><span>批次：</span></template>
              <template #value>
                <!-- 这里暂时显示 batchId，如果需要显示 batchNumber，可能需要调整数据结构或 store -->
                <span>{{ item.batchNumber }}</span>
              </template>
            </van-cell>
          </div>
        </van-cell-group>
      </div>

      <div class="safe-area-bottom"></div>
    </van-popup>
  </div>
</template>

<script setup>
import { useReturnOrderStore } from "@/stores/returnOrder.js";
import { computed } from "vue";
import { storeToRefs } from "pinia";

const store = useReturnOrderStore()
const cartItems = computed(() => store.cart)
const {showCart} = storeToRefs(store)
const clearCart = () => {
  store.clearCart()
}

const closeCart = () => {
  showCart.value = false
}

const removeCartItem = (item) => {
  store.removeCartItem(item)
}

const getReturnTypeTag = (type) => {
  return type === '仅退款' ? 'primary' : 'warning';
};
</script>

<style scoped>
.cart-popup-container {
  position: relative;
  height: 0;
  width: 100%;
  box-sizing: border-box;
  z-index: 2500; /* 提高z-index确保在页面元素之上 */
}

.custom-overlay {
  position: fixed; /* 改回fixed确保全屏覆盖 */
  top: 0;
  left: 0;
  right: 0;
  bottom: 50px; /* 留出底部购物车栏的高度 */
  background: rgba(0, 0, 0, 0.6);
  z-index: 2500; /* 确保遮罩层与弹出层同级 */
}

.cart-popup {
  border-radius: 16px 16px 0 0;
  max-height: calc(70vh - 50px); /* 减去购物车栏高度 */
  width: 100%;
  bottom: 50px !important; /* 确保在购物车栏上方 */
  box-sizing: border-box;
}

.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #ffe4cc; /* 退货系统使用浅橙色边框 */
  width: 100%;
  box-sizing: border-box;
  background-color: #fff7ee; /* 退货系统使用浅橙色背景 */
}

.cart-title {
  font-size: 16px;
  font-weight: 500;
  color: #ff8800; /* 退货系统使用橙色标题 */
}

.cart-content {
  max-height: calc(70vh - 50px - 56px); /* 减去购物车栏和头部高度 */
  overflow-y: auto;
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch;
  width: 100%;
  box-sizing: border-box;
}

.cart-item {
  margin-bottom: 8px;
  border-bottom: 1px dashed #ffe4cc;
}

.item-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-details {
  padding: 5px 0;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.detail-label {
  color: #666;
  font-size: 13px;
}

.detail-value {
  font-weight: 500;
  color: #ff8800;
}

.safe-area-bottom {
  height: env(safe-area-inset-bottom, 0);
}

/* 优化购物车项目布局 */
:deep(.van-cell) {
  padding: 10px 16px;
  align-items: center;
  width: 100%;
  box-sizing: border-box;
}

:deep(.van-cell__title) {
  flex: 3;
  overflow: hidden;
  text-overflow: ellipsis;
}

:deep(.van-cell__value) {
  flex: none;
  min-width: 110px;
}

/* 防止内容溢出 */
:deep(.van-cell__right-icon) {
  max-width: 110px;
  overflow: visible;
}

/* 空购物车样式 */
:deep(.van-empty) {
  padding: 32px 0;
}

:deep(.van-cell__label) {
  color: #ff8800; /* 退货系统使用橙色价格 */
}

/* 滚动条样式 */
.cart-content::-webkit-scrollbar {
  width: 4px;
}

.cart-content::-webkit-scrollbar-thumb {
  background: #ffbb66; /* 退货系统使用橙色滚动条 */
  border-radius: 2px;
}

.cart-content::-webkit-scrollbar-track {
  background: transparent;
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
}
</style> 