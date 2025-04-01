<route>
{
  name: "order-sale-new"
}
</route>

<template>
  <div class="order-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      title="新建订单"
      left-arrow
      fixed
      placeholder
      @click-left="goBack"
      class="page-nav"
    />

    <!-- 主要内容区域 - 改用flex布局而非绝对定位 -->
    <div class="order-scroll-container">
      <!-- 商店信息 -->
      <div class="shop-container">
        <ShopInfo />
      </div>

      <!-- 商品列表区域 - 使用flex布局，确保不被遮挡 -->
      <div class="product-container">
        <!-- 分类和商品列表都将在自身内部滚动 -->
        <CategorySidebar class="category-sidebar" />
        <ProductList class="product-list-component" />
      </div>
    </div>

    <!-- 购物车浮动条 - 固定在视口底部 -->
    <div class="cart-wrapper">
      <CartBar @click="store.showCart = !store.showCart" />
    </div>

    <!-- 购物车弹出层 -->
    <CartPopup
      :show="showCart"
    />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useOrderStore } from '@/stores/order'
import { storeToRefs } from 'pinia'
import ShopInfo from './components/ShopInfo.vue'
import CategorySidebar from './components/CategorySidebar.vue'
import ProductList from './components/ProductList.vue'
import CartBar from './components/CartBar.vue'
import CartPopup from './components/CartPopup.vue'
import { onMounted, onBeforeUnmount } from 'vue'
import { showConfirmDialog } from 'vant'

const router = useRouter()
const store = useOrderStore()

// 从 store 获取响应式状态
const { showCart } = storeToRefs(store)
const { init, submitOrder } = store

// 返回上一页
const goBack = () => {
  router.back()
}

// 处理提交订单
const handleSubmitOrder = () => {
  // 显示确认弹窗
  showConfirmDialog({
    title: '确认提交订单',
    message: `总计金额：¥${totalPrice.value.toFixed(2)}，确定要提交订单吗？`,
    confirmButtonText: '确认提交',
    cancelButtonText: '再想想'
  })
  .then(() => {
    // 用户点击确认，执行提交订单
    submitOrder()
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
  // 确保使用store中的方法清空购物车
  store.clearCart()
})
</script>

<style scoped>
/* 使用固定布局确保页面正确显示且防止横向滚动 */
.order-page {
  position: absolute; /* 修改为absolute，避免fixed无法卸载 */
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
  z-index: 10; /* 确保新订单页面高于App.vue中的元素 */
}

/* 可滚动区域 - 调整为flex布局 */
.order-scroll-container {
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

.shop-container {
  padding: 8px 0;
  background: #fff;
  margin-bottom: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
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
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.03);
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
  background-color: #fafafa;
  border-right: 1px solid #f1f1f1;
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
  .order-scroll-container {
    bottom: calc(50px + env(safe-area-inset-bottom, 0));
  }
  
  .cart-wrapper {
    padding-bottom: env(safe-area-inset-bottom, 0);
  }
}

/* 小屏幕适配 */
@media (max-width: 375px) {
  .order-scroll-container {
    bottom: 48px; /* 小屏幕购物车高度调整 */
  }
  
  @supports (padding-bottom: env(safe-area-inset-bottom)) {
    .order-scroll-container {
      bottom: calc(48px + env(safe-area-inset-bottom, 0));
    }
  }
}

/* 大屏幕优化 */
@media (min-width: 769px) {
  .order-page {
    max-width: 1024px;
    margin: 0 auto;
    left: 50%;
    transform: translateX(-50%);
  }
  
  .order-scroll-container {
    bottom: 56px; /* 大屏幕购物车高度调整 */
  }
  
  .cart-wrapper {
    max-width: 1024px;
    margin: 0 auto;
    left: 50%;
    transform: translateX(-50%);
  }
  
  @supports (padding-bottom: env(safe-area-inset-bottom)) {
    .order-scroll-container {
      bottom: calc(56px + env(safe-area-inset-bottom, 0));
    }
  }
}

/* 修复Safari滚动问题 */
@supports (-webkit-touch-callout: none) {
  .order-scroll-container {
    -webkit-overflow-scrolling: touch;
  }
}
</style>