<template>
  <div class="product-order-container">
    <!-- 顶部导航栏 -->
    <van-nav-bar
        :title="productName ? `${productName}销售记录` : '商品销售记录'"
        fixed
        left-arrow
        left-text="返回"
        @click-left="goBack"
    />

    <!-- 搜索区域 -->
    <div class="search-form">
      <van-cell is-link title="时间范围" @click="showCalendar = true">
        <template #value>
          <span v-if="searchForm.startDate && searchForm.endDate">
            {{ searchForm.startDate }} 至 {{ searchForm.endDate }}
          </span>
          <span v-else class="text-gray">选择日期范围</span>
        </template>
      </van-cell>

      <div class="search-buttons">
        <van-button block type="primary" @click="loadData">查询</van-button>
        <van-button block plain @click="resetSearch">重置</van-button>
      </div>
    </div>

    <!-- 日期选择弹出层 -->
    <van-calendar
        v-model:show="showCalendar"
        :max-date="maxDate"
        :min-date="minDate"
        color="#1989fa"
        first-day-of-week="1"
        type="range"
        @confirm="onDateRangeConfirm"
    />


    <!-- 订单列表 -->
    <div class="order-list">
      <van-empty v-if="orderList.length === 0 && !loading" description="暂无销售记录"/>
      <van-list
          v-else
          v-model:loading="loadingMore"
          :finished="finished"
          :immediate-check="false"
          finished-text="没有更多订单了"
          @load="onLoadMore"
      >
        <div v-for="(item, index) in orderList" :key="index" class="order-item">
          <div class="order-info">
            <div class="order-header">
              <span class="order-time">{{ formatDateTime(item.orderTime) }}</span>
              <span class="order-id">订单号: {{ item.orderId }}</span>
            </div>
            <div class="order-shop">{{ item.shopName }}</div>
          </div>
          <div class="order-content">
            <div class="order-quantity">{{ item.quantity }} 个</div>
            <div class="order-price">
              <div class="unit-price">单价: ¥{{ formatPrice(item.salePrice) }}</div>
              <div class="total-price">¥{{ formatPrice(item.totalSalesAmount) }}</div>
            </div>
          </div>
        </div>
      </van-list>
    </div>

    <!-- 加载指示器 -->
    <van-loading v-if="loading" class="page-loading" color="#1989fa" type="spinner"/>
  </div>
</template>

<script setup>
import {ref, reactive, onMounted} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {showToast, showFailToast} from 'vant'
import productOrderApi from '@/api/productOrder'
import productApi from '@/api/product'

// 页面状态
const loading = ref(false)
const loadingMore = ref(false)
const finished = ref(false)
const showCalendar = ref(false)
const orderList = ref([])
const productName = ref('')
const currentPage = ref(0)
const pageSize = ref(10)
const minDate = new Date(new Date().getFullYear() - 1, 0, 1)
const maxDate = new Date()

const searchForm = reactive({
  productId: null,
  startDate: null,
  endDate: null,
  page: 0,
  size: 10
})


// 路由和参数
const route = useRoute()
const router = useRouter()

// 从路由获取商品ID
const getProductId = () => {
  const id = route.params.id || route.query.productId


  productName.value = route.query.productName
  if (!id) {
    showFailToast('缺少商品ID参数')
    goBack()
    return null
  }
  return Number(id)
}

// 初始化数据
onMounted(async () => {
  const productId = getProductId()
  if (productId) {
    searchForm.productId = productId
    loadData()
  }
})


// 加载订单数据
const loadData = async () => {
  loading.value = true
  currentPage.value = 0
  orderList.value = []
  finished.value = false

  try {
    const response = await productOrderApi.getProductOrderList({
      productId: searchForm.productId,
      startDate: searchForm.startDate,
      endDate: searchForm.endDate,
      page: currentPage.value,
      size: pageSize.value
    })

    orderList.value = response.content || []
    finished.value = !response.hasNext

  } catch (error) {
    console.error('获取订单列表失败:', error)
    showFailToast('获取数据失败，请稍后重试')
    finished.value = true
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 加载更多数据
const onLoadMore = async () => {
  if (finished.value) return

  loadingMore.value = true
  currentPage.value++

  try {
    const response = await productOrderApi.getProductOrderList({
      productId: searchForm.productId,
      startDate: searchForm.startDate,
      endDate: searchForm.endDate,
      page: currentPage.value,
      size: pageSize.value
    })

    if (response.content && response.content.length > 0) {
      orderList.value = [...orderList.value, ...response.content]

    }

    finished.value = !response.hasNext
  } catch (error) {
    console.error('加载更多数据失败:', error)
    showFailToast('加载更多数据失败')
    finished.value = true
  } finally {
    loadingMore.value = false
  }
}

// 日期选择确认
const onDateRangeConfirm = ([start, end]) => {
  const formatDate = (date) => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  searchForm.startDate = formatDate(start)
  searchForm.endDate = formatDate(end)
  showCalendar.value = false
}



// 重置搜索
const resetSearch = () => {
  searchForm.startDate = null
  searchForm.endDate = null
  currentPage.value = 0
  loadData()
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 格式化日期时间
const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return ''
  const date = new Date(dateTimeStr)

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')

  return `${year}-${month}-${day} ${hour}:${minute}`
}

// 格式化价格
const formatPrice = (price) => {
  return parseFloat(price).toFixed(2)
}
</script>

<style scoped>
.product-order-container {
  padding: 56px 0 0; /* 为固定导航栏腾出空间 */
  background-color: #f7f8fa;
  min-height: 100vh;
}

.search-form {
  margin: 10px;
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.search-buttons {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  padding: 10px 16px 16px;
}

.text-gray {
  color: #969799;
}


.order-list {
  padding: 0 10px;
}

.order-item {
  background: #fff;
  border-radius: 8px;
  margin-bottom: 10px;
  padding: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.order-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.order-time {
  font-size: 14px;
  color: #323233;
  font-weight: 500;
}

.order-id {
  font-size: 12px;
  color: #969799;
}

.order-shop {
  font-size: 13px;
  color: #646566;
  margin-bottom: 8px;
}

.order-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f2f3f5;
  padding-top: 10px;
  margin-top: 6px;
}

.order-quantity {
  font-size: 15px;
  color: #323233;
}

.order-price {
  text-align: right;
}

.unit-price {
  font-size: 12px;
  color: #969799;
  margin-bottom: 2px;
}

.total-price {
  font-size: 18px;
  font-weight: bold;
  color: #ff6000;
}

.page-loading {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 100;
}

/* 适配 iPhone 等小屏幕设备 */
@media (max-width: 375px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .stat-value {
    font-size: 18px;
  }

  .order-time, .order-shop {
    font-size: 13px;
  }

  .total-price {
    font-size: 16px;
  }
}
</style> 