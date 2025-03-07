<template>
  <div class="page">
    <!-- 顶部导航 -->
    <van-nav-bar
      title="新建采购单"
      left-arrow
      @click-left="onClickLeft"
    />

    <div class="content">
      <!-- 商品清单 -->
      <van-cell-group title="商品清单" class="purchase-list">
        <template v-if="purchaseList.length">
          <van-swipe-cell 
            v-for="item in purchaseList" 
            :key="item.productId"
            class="purchase-item"
          >
            <van-cell>
              <template #title>
                <div class="product-item">
                  <div class="product-info">
                    <span class="product-name">{{ item.productName }}</span>
                    <span class="product-amount">¥{{ formatAmount(item.totalAmount) }}</span>
                  </div>
                  <div class="product-controls">
                    <van-stepper 
                      v-model="item.quantity" 
                      :min="1"
                      :max="getProductStock(item.productId)"
                      @change="(value) => updateQuantity(item, value)" 
                    />
                    <van-button 
                      type="danger" 
                      size="small"
                      class="delete-btn"
                      @click="handleDelete(item)"
                    >删除</van-button>
                  </div>
                </div>
              </template>
            </van-cell>
          </van-swipe-cell>
        </template>
        
        <van-empty v-else description="暂无采购商品" />
      </van-cell-group>

      <!-- 添加商品按钮 -->
      <van-button 
        type="primary" 
        size="large" 
        icon="plus"
        class="add-button"
        @click="showProductSelector = true"
      >
        添加商品
      </van-button>

      <!-- 合计金额 -->
      <div v-if="purchaseList.length" class="total-section">
        <span class="total-label">合计金额</span>
        <span class="total-amount">¥{{ formatAmount(totalAmount) }}</span>
      </div>
    </div>

    <!-- 底部提交按钮 -->
    <div class="footer">
      <van-button 
        type="primary" 
        block 
        :disabled="!purchaseList.length"
        @click="handleSubmit"
      >
        提交采购单
      </van-button>
    </div>

    <!-- 商品选择弹窗 -->
    <van-popup
      v-model:show="showProductSelector"
      position="bottom"
      :style="{ height: '70%' }"
      class="product-selector-popup"
      closeable
      round
    >
      <div class="product-selector">
        <div class="selector-header">
          <span class="selector-title">选择商品</span>
          <van-search
            v-model="searchKeyword"
            placeholder="搜索商品名称"
            shape="round"
            @search="onSearch"
            @clear="onSearchClear"
          />
        </div>

        <div class="selector-content">
          <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
            <van-list
              v-model:loading="loading"
              :finished="finished"
              :error="error"
              error-text="请求失败，点击重试"
              finished-text="没有更多了"
              @load="onLoad"
            >
              <van-cell
                v-for="product in filteredProducts"
                :key="product.id"
                :class="['product-cell', { selected: isSelected(product.id) }]"
                @click="quickAdd(product)"
              >
                <template #title>
                  <div class="product-info">
                    <div class="product-title">
                      {{ product.name }}
                      <span class="product-spec">{{ product.specification }}</span>
                    </div>
                    <div class="product-detail">
                      <span class="product-price">¥{{ formatAmount(product.price) }}</span>
                      <span class="product-stock">库存：{{ product.stock }}</span>
                    </div>
                  </div>
                </template>
              </van-cell>
            </van-list>
          </van-pull-refresh>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showDialog, showToast } from 'vant'
import { usePurchaseStore } from '@/stores/purchase'

const router = useRouter()
const store = usePurchaseStore()

// 状态管理
const showProductSelector = ref(false)
const searchKeyword = ref('')
const loading = ref(false)
const finished = ref(false)
const error = ref(false)
const refreshing = ref(false)

// Store数据
const products = computed(() => store.products)
const purchaseList = computed(() => store.purchaseList)
const totalAmount = computed(() => store.totalAmount)

// 过滤后的商品列表
const filteredProducts = computed(() => {
  if (!searchKeyword.value) return products.value
  
  const keyword = searchKeyword.value.toLowerCase()
  return products.value.filter(product => 
    product.name.toLowerCase().includes(keyword) ||
    product.specification.toLowerCase().includes(keyword)
  )
})

// 方法
function onClickLeft() {
  if (purchaseList.value.length) {
    showDialog({
      title: '提示',
      message: '确定要离开吗？已自动保存为草稿',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    }).then((action) => {
      if (action === 'confirm') {
        router.back()
      }
    })
  } else {
    router.back()
  }
}

function formatAmount(amount) {
  return Number(amount || 0).toFixed(2)
}

function getProductStock(productId) {
  const product = products.value.find(p => p.id === productId)
  return product ? product.stock : 0
}

function updateQuantity(item, value) {
  const product = products.value.find(p => p.id === item.productId)
  if (!product) return
  
  store.updatePurchaseList({
    ...item,
    quantity: value,
    totalAmount: value * product.price
  })
}

function handleDelete(item) {
  showDialog({
    title: '提示',
    message: '确定要删除该商品吗？',
    showCancelButton: true
  }).then((action) => {
    if (action === 'confirm') {
      store.updatePurchaseList({...item, quantity: 0})
      showToast('已删除')
    }
  })
}

function onSearch() {
  loading.value = false
  finished.value = false
  error.value = false
}

function onSearchClear() {
  searchKeyword.value = ''
  onSearch()
}

function onRefresh() {
  // 刷新数据
  store.init().then(() => {
    refreshing.value = false
    showToast('刷新成功')
  })
}

function onLoad() {
  // 实际项目中这里应该调用API加载数据
  loading.value = false
  finished.value = true
}

function isSelected(productId) {
  return purchaseList.value.some(item => item.productId === productId)
}

function quickAdd(product) {
  const existing = purchaseList.value.find(item => item.productId === product.id)
  if (existing) {
    updateQuantity(existing, existing.quantity + 1)
  } else {
    store.updatePurchaseList({
      productId: product.id,
      productName: product.name,
      quantity: 1,
      totalAmount: product.price
    })
  }
  showToast({
    type: 'success',
    message: '已添加',
    position: 'bottom'
  })
}

function handleSubmit() {
  showDialog({
    title: '提示',
    message: '确定要提交采购单吗？',
    showCancelButton: true
  }).then((action) => {
    if (action === 'confirm') {
      store.submitPurchase().then((success) => {
        if (success) {
          router.back()
        }
      })
    }
  })
}

// 初始化
store.init()
</script>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f7f8fa;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.purchase-list {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.purchase-item {
  background: #fff;
}

.purchase-item:not(:last-child) {
  border-bottom: 1px solid #f5f5f5;
}

.product-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.product-name {
  font-size: 15px;
  font-weight: 500;
  color: #323233;
}

.product-amount {
  color: #ee0a24;
  font-weight: 500;
}

.product-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.delete-btn {
  margin-left: 12px;
}

.add-button {
  border-radius: 8px;
  font-size: 16px;
}

.total-section {
  margin-top: auto;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.total-label {
  font-size: 15px;
  color: #666;
}

.total-amount {
  font-size: 20px;
  color: #ee0a24;
  font-weight: 500;
}

.footer {
  padding: 16px;
  background: #fff;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
}

.product-selector-popup {
  max-height: 90vh;
}

.product-selector {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.selector-header {
  padding: 16px 16px 0;
}

.selector-title {
  display: block;
  text-align: center;
  font-size: 16px;
  font-weight: 500;
  color: #323233;
  margin-bottom: 8px;
}

.selector-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px;
}

.product-cell {
  margin-bottom: 8px;
  border-radius: 8px;
  background: #fff;
  transition: all 0.3s;
}

.product-cell:active {
  background: #f5f5f5;
}

.product-title {
  font-size: 15px;
  font-weight: 500;
  color: #323233;
  margin-bottom: 4px;
}

.product-spec {
  font-size: 13px;
  color: #666;
  margin-left: 8px;
  font-weight: normal;
}

.product-detail {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.product-price {
  color: #ee0a24;
  font-weight: 500;
}

.product-stock {
  color: #666;
}

:deep(.van-cell) {
  padding: 16px;
  line-height: 1.5;
}

:deep(.van-empty) {
  padding: 32px 0;
  background: #fff;
}

:deep(.van-stepper) {
  height: 28px;
}

:deep(.van-stepper__input) {
  width: 40px;
}

:deep(.van-pull-refresh) {
  min-height: 100%;
}

:deep(.van-popup) {
  border-top-left-radius: 16px;
  border-top-right-radius: 16px;
}

:deep(.van-search) {
  padding: 8px 0;
}

:deep(.van-search__content) {
  border-radius: 8px;
}
</style>