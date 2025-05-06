<template>
  <div class="return-order-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      title="新建退货单"
      left-arrow
      fixed
      placeholder
      @click-left="goBack"
      class="page-nav return-theme"
    />


    <!-- 主要内容区域 - 改用flex布局而非绝对定位 -->
    <div class="return-scroll-container">
      <!-- 商店信息 -->
      <div class="shop-container">
        <ShopInfo :shop="nearbyShops[0]" />
      </div>

      <!-- 商品列表区域 - 使用flex布局，确保不被遮挡 -->
      <div class="product-container">
        <!-- 分类和商品列表都将在自身内部滚动 -->
        <CategorySidebar
          v-model="activeCategory"
          :categories="categories"
          class="category-sidebar"
        />

        <ProductList
          :products="currentFoods"
          @update-cart="updateCart"
          class="product-list-component"
        />
      </div>
    </div>

    <!-- 购物车浮动条 - 固定在视口底部 -->
    <div class="cart-wrapper">
      <CartBar
        @show-cart="showCart = true"
        @submit="handleSubmitReturn"
      />
    </div>

    <!-- 购物车弹出层 -->
    <CartPopup
      :show="showCart"
      @update:show="showCart = $event"
    />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useReturnOrderStore } from '@/stores/returnOrder.js'
import { storeToRefs } from 'pinia'
import ShopInfo from './components/ShopInfo.vue'
import CategorySidebar from './components/CategorySidebar.vue'
import ProductList from './components/ProductList.vue'
import CartBar from './components/CartBar.vue'
import CartPopup from './components/CartPopup.vue'
import { onMounted, onBeforeUnmount } from 'vue'
import { showConfirmDialog } from 'vant'

const router = useRouter()
const store = useReturnOrderStore()

// 从 store 获取响应式状态
const {
  activeCategory,
  categories,
  currentFoods,
  cart,
  nearbyShops,
  totalPrice,
  showCart
} = storeToRefs(store)

// 从 store 获取方法
const { init, updateCart, clearCart, submitReturnOrder } = store

// 返回上一页
const goBack = () => {
  router.back()
}

// 处理提交退货订单
const handleSubmitReturn = () => {
  // 显示确认弹窗
  showConfirmDialog({
    title: '确认提交退货单',
    message: `退货金额：¥${totalPrice.value.toFixed(2)}，确定要提交退货单吗？`,
    confirmButtonText: '确认提交',
    cancelButtonText: '再想想'
  })
  .then(() => {
    // 用户点击确认，执行提交退货单
    submitReturnOrder()
      .then(success => {
        if (success) {
          // 成功提交后会自动返回订单列表页
        }
      });
  })
  .catch(() => {
    // 用户取消，不执行任何操作
  })
}

// 生命周期钩子
onMounted(() => {
  init()
})

onBeforeUnmount(() => {
  clearCart()
})
</script>

<style scoped>
/* 使用固定布局确保页面正确显示且防止横向滚动 */
.return-order-page {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: #f8f8fa;
  display: flex;
  flex-direction: column;
  width: 100%;
  overflow: hidden;
  box-sizing: border-box;
  z-index: 10; /* 确保退货页面高于App.vue中的元素 */
}

/* 可滚动区域 - 调整为flex布局 */
.return-scroll-container {
  position: absolute;
  top: 46px; /* 导航栏高度 */
  left: 0;
  right: 0;
  bottom: 50px; /* 购物车高度 */
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 改为隐藏，不再允许整体滚动 */
  z-index: 1;
  padding: 0; /* 重置内边距 */
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.page-nav {
  z-index: 12; /* 确保导航栏在最上层 */
}

/* 为退货系统设置橙色主题 */
.return-theme {
  --van-nav-bar-background: #ff8800;
  --van-nav-bar-title-text-color: #fff;
  --van-nav-bar-icon-color: #fff;
  --van-nav-bar-text-color: #fff;
}

.shop-container {
  padding: 8px 0;
  background: #fff;
  margin-bottom: 8px;
  box-shadow: 0 2px 8px rgba(245, 166, 35, 0.1); /* 退货系统使用橙色阴影 */
  position: relative;
  z-index: 2; /* 确保在正确的层级 */
  width: 100%;
  box-sizing: border-box;
  flex-shrink: 0; /* 防止压缩 */
}

/* 产品容器，作为分类和列表的父容器 */
.product-container {
  display: flex;
  background: #fff;
  border-radius: 8px 8px 0 0;
  box-shadow: 0 -2px 8px rgba(245, 166, 35, 0.05); /* 退货系统使用橙色阴影 */
  width: 100%;
  box-sizing: border-box;
  overflow: hidden; /* 防止内容溢出 */
  flex: 1; /* 占据剩余空间 */
  z-index: 1;
}

/* 重写侧边栏样式以确保独立滚动 */
.category-sidebar {
  flex-shrink: 0;
  width: 80px;
  height: 100%;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  box-sizing: border-box;
  background-color: #fff7ee; /* 退货系统使用浅橙色背景 */
  border-right: 1px solid #ffe4cc; /* 退货系统使用浅橙色边框 */
}

/* 重写商品列表样式以确保独立滚动 */
.product-list-component {
  flex: 1;
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch;
  box-sizing: border-box;
}

.cart-wrapper {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 11; /* 确保购物车在App.vue元素之上 */
  background: #fff;
  width: 100%;
  box-sizing: border-box;
}

/* 支持iOS底部安全区调整 */
@supports (padding-bottom: env(safe-area-inset-bottom)) {
  .return-scroll-container {
    bottom: calc(50px + env(safe-area-inset-bottom, 0));
  }
  
  .cart-wrapper {
    padding-bottom: env(safe-area-inset-bottom, 0);
  }
}

/* 小屏幕适配 */
@media (max-width: 375px) {
  .return-scroll-container {
    bottom: 48px; /* 小屏幕购物车高度调整 */
  }
  
  @supports (padding-bottom: env(safe-area-inset-bottom)) {
    .return-scroll-container {
      bottom: calc(48px + env(safe-area-inset-bottom, 0));
    }
  }
}

/* 大屏幕优化 */
@media (min-width: 769px) {
  .return-order-page {
    max-width: 1024px;
    margin: 0 auto;
    left: 50%;
    transform: translateX(-50%);
  }
  
  .return-scroll-container {
    bottom: 56px; /* 大屏幕购物车高度调整 */
  }
  
  .cart-wrapper {
    max-width: 1024px;
    margin: 0 auto;
    left: 50%;
    transform: translateX(-50%);
  }
  
  @supports (padding-bottom: env(safe-area-inset-bottom)) {
    .return-scroll-container {
      bottom: calc(56px + env(safe-area-inset-bottom, 0));
    }
  }
}

/* 修复Safari滚动问题 */
@supports (-webkit-touch-callout: none) {
  .return-scroll-container {
    -webkit-overflow-scrolling: touch;
  }
}
</style>