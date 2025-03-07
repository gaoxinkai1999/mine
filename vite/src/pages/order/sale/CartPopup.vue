<template>
  <div class="cart-popup-container">
    <!-- 自定义遮罩层，不遮挡底部购物车栏 -->
    <div 
      v-if="show" 
      class="custom-overlay" 
      @click="$emit('update:show', false)"
    ></div>
    
    <!-- 购物车弹窗内容 -->
    <van-popup
      :show="show"
      position="bottom"
      class="cart-popup"
      :close-on-click-overlay="false"
      :overlay="false"
      :z-index="2600"
      @update:show="$emit('update:show', $event)"
    >
      <div class="cart-header">
        <span class="cart-title">购物车</span>
        <van-button
          size="small"
          type="danger"
          plain
          :disabled="!cartItems.length"
          @click="$emit('clear')"
        >
          清空
        </van-button>
      </div>

      <div class="cart-content">
        <van-empty v-if="!cartItems.length" description="购物车是空的" />
        <van-cell
          v-for="item in cartItems"
          :key="item.id"
          :title="item.name"
          :label="`¥${(item.price * item.count).toFixed(2)}`"
        >
          <template #right-icon>
            <van-stepper
              v-model="item.count"
              button-size="22"
              theme="round"
              min="0"
              @change="() => $emit('update', item)"
            />
          </template>
        </van-cell>
      </div>
      
      <div class="safe-area-bottom"></div>
    </van-popup>
  </div>
</template>

<script setup>
// 定义props
const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  cartItems: {
    type: Array,
    default: () => []
  }
});

// 定义事件
defineEmits(['update:show', 'update', 'clear']);
</script>

<style scoped>
.cart-popup-container {
  position: relative;
  height: 0;
  width: 100%;
  box-sizing: border-box;
  z-index: 2000; /* 确保比App.vue中的元素更高 */
}

.custom-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 50px; /* 购物车栏的高度 */
  background-color: rgba(0, 0, 0, 0.7);
  z-index: 2500;
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
  border-bottom: 1px solid #eee;
  width: 100%;
  box-sizing: border-box;
}

.cart-title {
  font-size: 16px;
  font-weight: 500;
}

.cart-content {
  max-height: calc(70vh - 50px - 56px); /* 减去购物车栏和头部高度 */
  overflow-y: auto;
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch;
  width: 100%;
  box-sizing: border-box;
}

.safe-area-bottom {
  height: env(safe-area-inset-bottom, 0);
}

/* 优化购物车项目布局 */
:deep(.van-cell) {
  padding: 12px 16px;
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

:deep(.van-stepper) {
  --van-stepper-input-width: 32px;
  --van-stepper-button-icon-color: #1989fa;
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
</style>