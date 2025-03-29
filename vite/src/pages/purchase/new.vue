<route>
{
  name: "purchase-new"
}
</route>

<template>
  <div class="purchase-page">
    <!-- 顶部导航 -->
    <div class="header">
      <van-nav-bar
        title="新建采购单"
        left-arrow
        right-text="自动生成"
        fixed
        placeholder
        safe-area-inset-top
        @click-left="handleBack"
        @click-right="handleRightClick"
      >
      </van-nav-bar>
    </div>

    <div class="container">
      <!-- 商品分类列表 -->
      <div class="main-content">
        <van-sidebar v-model="activeCategory">
          <van-sidebar-item title="全部商品" />
          <van-sidebar-item 
            v-for="category in categories" 
            :key="category.id"
            :title="category.name"
          />
        </van-sidebar>
        
        <div class="product-container">
          <product-list 
            :products="currentProducts" 
            :selected-products="purchaseList"
            @product-selected="handleProductSelected"
          />
        </div>
      </div>

      <!-- 购物车浮动按钮 -->
      <div class="cart-button" v-show="cartCount > 0" @click="showCartPanel = true">
        <van-badge :content="cartCount" :max="99">
          <van-icon name="cart-o" size="24" />
        </van-badge>
        <span class="cart-amount">¥{{ formatAmount(totalAmount) }}</span>
      </div>
    </div>


    <!-- 购物车面板 -->
    <van-popup 
      v-model:show="showCartPanel" 
      position="bottom" 
      round 
      class="cart-panel"
    >
      <div class="cart-header">
        <div class="cart-title">已选商品</div>
        <van-button 
          plain 
          type="danger" 
          size="small" 
          icon="delete" 
          @click="clearCart"
        >
          清空
        </van-button>
      </div>

      <div class="cart-items">
        <van-empty v-if="!purchaseList.length" description="暂无采购商品" />
        <template v-else>
          <div
            v-for="item in purchaseList"
            :key="item.productId"
            class="cart-item"
          >
            <div class="item-info">
              <div class="item-name">{{ item.productName }}</div>
              <div class="item-price">¥{{ formatAmount(item.totalAmount / item.quantity) }} × {{ item.quantity }}</div>
            </div>
            <div class="item-actions">
              <span class="item-total">¥{{ formatAmount(item.totalAmount) }}</span>
              <van-stepper 
                v-model="item.quantity" 
                :min="0" 
                :max="getProductStock(item.productId)" 
                theme="round"
                button-size="22"
                integer
                @change="handleStepperChange(item)" 
              />
            </div>
          </div>
        </template>
      </div>

      <div class="cart-footer">
        <div class="cart-total">
          <span>合计:</span>
          <span class="cart-total-amount">¥{{ formatAmount(totalAmount) }}</span>
        </div>
        <van-button
            type="primary"
            round
            :disabled="!purchaseList.length"
            @click="showBudgetDialog=true"
        >
          设置预算
        </van-button>
        <van-button 
          type="primary" 

          round 
          :disabled="!purchaseList.length"
          @click="submitOrder"
        >
          提交采购单
        </van-button>
        <van-dialog v-model:show="showBudgetDialog" title="采购预算" show-cancel-button @confirm="handleAutoSetQuantity">
          <van-field v-model="budget" type="digit" label="预算 " />
        </van-dialog>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { showToast, showDialog } from 'vant';
import { usePurchaseStore } from '@/stores/purchase.js';
import ProductList from '@/pages/purchase/components/ProductList.vue';

// 路由与状态
const router = useRouter();
const store = usePurchaseStore();

// 状态
const activeCategory = ref(0);
const showFilterPanel = ref(false);
const showCartPanel = ref(false);
const sortType = ref('default');

// Store数据
const products = computed(() => store.products);
const categories = computed(() => store.categories);
const purchaseList = computed(() => store.purchaseList);
const totalAmount = computed(() => store.totalAmount);

// 计算属性
const cartCount = computed(() => 
  purchaseList.value.reduce((total, item) => total + item.quantity, 0)
);

const currentProducts = computed(() => {
  let result = [...products.value];
  
  // 分类过滤
  if (activeCategory.value > 0) {
    const category = categories.value[activeCategory.value - 1];
    result = result.filter(product => product.categoryId === category.id);
  }
  

  return result;
});

// 方法
function formatAmount(amount) {
  return Number(amount || 0).toFixed(2);
}

function getProductStock(productId) {
  const product = products.value.find(p => p.id === productId);
  return product ? product.stock : 0;
}

function handleBack() {
  if (purchaseList.value.length) {
    showDialog({
      title: '提示',
      message: '确定要离开当前页面吗？',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    }).then((action) => {
      if (action === 'confirm') {
        router.back();
      }
    });
  } else {
    router.back();
  }
}



function handleProductSelected(product) {
  const existing = purchaseList.value.find(item => item.productId === product.id);
  
  if (existing) {
    handleStepperChange({
      ...existing,
      quantity: existing.quantity + 1
    });
  } else {
    store.updatePurchaseList({
      productId: product.id,
      productName: product.name,
      quantity: 1,
      totalAmount: product.purchasePrice
    });
    
    showToast({
      type: 'success',
      message: '已添加到采购单',
      position: 'bottom'
    });
  }
}

function handleStepperChange(item) {
  const product = products.value.find(p => p.id === item.productId);
  if (!product) return;
  
  if (item.quantity === 0) {
    showDialog({
      title: '提示',
      message: '确定要从采购单中移除该商品吗？',
      showCancelButton: true
    }).then((action) => {
      if (action === 'confirm') {
        store.updatePurchaseList({
          ...item,
          quantity: 0
        });
      } else {
        // 取消删除，恢复数量为1
        item.quantity = 1;
        store.updatePurchaseList({
          ...item,
          quantity: 1,
          totalAmount: product.purchasePrice
        });
      }
    });
  } else {
    store.updatePurchaseList({
      ...item,
      quantity: item.quantity,
      totalAmount: item.quantity * product.purchasePrice
    });
  }
}
function handleRightClick(){
  showToast('功能开发中')
}

function clearCart() {
  if (!purchaseList.value.length) return;
  
  showDialog({
    title: '提示',
    message: '确定要清空采购单吗？',
    showCancelButton: true
  }).then((action) => {
    if (action === 'confirm') {
      store.clearPurchaseList();
      showToast('已清空采购单');
    }
  });
}

function submitOrder() {
  showDialog({
    title: '提示',
    message: '确定要提交采购单吗？',
    showCancelButton: true
  }).then((action) => {
    if (action === 'confirm') {
      store.submitPurchase().then((success) => {
        if (success) {
          router.back();
        }
      });
    }
  });
}

//预算设置弹出框状态
const showBudgetDialog = ref(false);
//采购预算
const budget = ref(0);

//根据预算自动设置采购数量
function handleAutoSetQuantity(){
  showToast('功能开发中')
}

// 初始化
onMounted(() => {
  store.init();
});
</script>

<style scoped>
.purchase-page {
  min-height: 100vh;
  height: 100vh;
  background-color: #f7f8fa;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  width: 100%;
}

.container {
  flex: 1;
  position: relative;
  overflow: hidden;
  width: 100%;
  box-sizing: border-box;
}

.main-content {
  display: flex;
  height: calc(100% - 46px);
  width: 100%;
  box-sizing: border-box;
}

.product-container {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  width: 0; /* 防止内容撑开容器 */
  min-width: 0; /* 确保flex布局正常工作 */
}

.cart-button {
  position: fixed;
  right: 16px;
  bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: auto;
  min-width: 56px;
  height: 56px;
  padding: 0 16px;
  border-radius: 28px;
  background: var(--van-primary-color);
  color: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 99;
  box-sizing: border-box;
}

.cart-amount {
  margin-left: 8px;
  font-weight: 500;
}

/* 筛选面板样式 */
.filter-panel {
  width: 80%;
  height: 100%;
  max-width: 375px;
  overflow-y: auto;
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f5f5f5;
}

.filter-title {
  font-size: 16px;
  font-weight: 500;
}

.filter-actions {
  position: sticky;
  bottom: 0;
  padding: 16px;
  background: white;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
}

/* 购物车面板样式 */
.cart-panel {
  height: 70%;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f5f5f5;
}

.cart-title {
  font-size: 16px;
  font-weight: 500;
}

.cart-items {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px;
}

.cart-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.item-info {
  flex: 1;
}

.item-name {
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 4px;
}

.item-price {
  font-size: 13px;
  color: #999;
}

.item-actions {
  display: flex;
  align-items: center;
}

.item-total {
  color: #ee0a24;
  font-weight: 500;
  margin-right: 16px;
}

.cart-footer {
  padding: 16px;
  background: white;
  border-top: 1px solid #f5f5f5;
}

.cart-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.cart-total-amount {
  font-size: 20px;
  color: #ee0a24;
  font-weight: 500;
}

:deep(.van-sidebar) {
  width: 85px;
  height: 100%;
  flex: none; /* 替换 flex-shrink: 0 */
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

:deep(.van-sidebar-item) {
  padding: 14px 12px;
  box-sizing: border-box;
}

:deep(.van-empty) {
  padding: 32px 0;
}
</style>