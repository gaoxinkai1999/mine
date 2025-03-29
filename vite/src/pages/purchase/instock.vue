<route>
{
  name: "purchase-instock"
}
</route>

<template>
  <div class="purchase-instock-page">
    <van-nav-bar
      fixed
      safe-area-inset-top
      title="采购入库"
      left-text="返回"
      left-arrow
      @click-left="$router.back()"
    />

    <div class="content-container">
      <template v-if="loading">
        <div class="loading-container">
          <van-loading type="spinner" color="#1989fa" />
          <p>加载中...</p>
        </div>
      </template>
      
      <template v-else>
        <div class="purchase-info">
          <div class="info-item">
            <span class="label">采购单号:</span>
            <span class="value">#{{ purchase?.id }}</span>
          </div>
          <div class="info-item">
            <span class="label">创建时间:</span>
            <span class="value">{{ formatDateTime(purchase?.createTime) }}</span>
          </div>
          <div class="info-item">
            <span class="label">总金额:</span>
            <span class="value price">¥{{ formatAmount(purchase?.totalAmount) }}</span>
          </div>
        </div>

        <van-divider/>

        <div class="product-list">
          <h3>采购商品清单</h3>
          
          <div v-for="detail in purchase?.purchaseDetails" :key="detail.id" class="product-item">
            <div class="product-info">
              <div class="product-name">{{ detail.productName }}</div>
              <div class="product-spec">
                <span class="quantity">数量: {{ detail.quantity }}</span>
                <span class="amount">金额: ¥{{ formatAmount(detail.totalAmount) }}</span>
              </div>
            </div>

            <!-- 批次管理区块 -->
            <div v-if="detail && detail.isBatchManaged" class="batch-management">
              <div class="batch-title">
                <van-tag type="primary">批次管理</van-tag>
              </div>
              
              <div class="batch-form">
                <van-cell
                  title="生产日期"
                  :value="formatDisplayDate(getBatchInfo(detail.id).productionDate)"
                  is-link
                  @click="openDatePicker('production', detail.id)"
                />
              </div>
            </div>
          </div>
        </div>

        <div class="action-buttons">
          <van-button 
            type="primary" 
            block 
            round 
            :disabled="!canSubmit"
            @click="confirmInStock"
          >
            确认入库
          </van-button>
          
          <van-button 
            type="danger" 
            plain
            block 
            round 
            class="cancel-button"
            @click="cancelPurchase"
          >
            取消采购单
          </van-button>
        </div>
      </template>
    </div>

    <!-- 日期选择器 -->
    <van-calendar
      v-model:show="showCalendar"
      :min-date="new Date(2025, 0, 1)"
      :max-date="new Date(2027, 11, 31)"
      @confirm="onCalendarConfirm"
      color="#1989fa"
      :default-date="calendarDefaultDate"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { showToast, showLoadingToast, closeToast, showDialog } from 'vant';
import api from '@/api';

const route = useRoute();
const router = useRouter();
const purchaseId = route.query.id;

// 数据状态
const purchase = ref(null);
const loading = ref(true);
const batchInfoMap = reactive({});

// 日期选择器状态
const showCalendar = ref(false);
const currentDateType = ref('');  // 'production' 或 'expiration'
const currentDetailId = ref(null);
const calendarDefaultDate = ref(new Date());

// 计算属性
const canSubmit = computed(() => {
  if (!purchase.value || !purchase.value.purchaseDetails) return false;
  
  // 检查所有批次管理商品是否都填写了生产日期
  for (const detail of purchase.value.purchaseDetails) {
    if (detail && detail.isBatchManaged) {
      const info = batchInfoMap[detail.id];
      if (!info || !info.productionDate) {
        return false;
      }
    }
  }
  
  return true;
});

// 方法
const loadPurchaseDetail = async () => {
  try {
    loading.value = true;
    
    // 加载采购单详情
    const response = await api.purchase.getPurchaseDetail({ id: purchaseId });
    purchase.value = response;
    
    // 初始化批次信息
    initBatchInfo();
    
  } catch (error) {
    console.error('加载采购单详情失败:', error);
    showToast('加载失败，请重试');
  } finally {
    loading.value = false;
  }
};

const initBatchInfo = () => {
  if (!purchase.value || !purchase.value.purchaseDetails) return;
  
  purchase.value.purchaseDetails.forEach(detail => {
    if (detail && detail.id && detail.isBatchManaged) {
      batchInfoMap[detail.id] = {
        purchaseDetailId: detail.id,
        productionDate: null
      };
    }
  });
};

// 获取批次信息，确保对象存在
const getBatchInfo = (detailId) => {
  if (!batchInfoMap[detailId]) {
    batchInfoMap[detailId] = {
      purchaseDetailId: detailId,
      productionDate: null
    };
  }
  return batchInfoMap[detailId];
};

// 打开日期选择器
const openDatePicker = (type, detailId) => {
  if (!detailId) return;
  
  currentDateType.value = type;
  currentDetailId.value = detailId;
  
  // 设置默认日期
  calendarDefaultDate.value = batchInfoMap[detailId]?.productionDate || new Date();
  
  showCalendar.value = true;
};

// 日期选择器确认回调
const onCalendarConfirm = (date) => {
  const detailId = currentDetailId.value;
  if (!detailId) return;
  
  const info = getBatchInfo(detailId);
  info.productionDate = date;
  
  showCalendar.value = false;
};

// 确认入库
const confirmInStock = async () => {
  try {
    const loadingToast = showLoadingToast({
      message: '处理中...',
      forbidClick: true,
      duration: 0
    });
    
    // 准备批次信息列表
    const batchInfoList = Object.values(batchInfoMap)
      .filter(info => info && info.purchaseDetailId && info.productionDate)
      .map(info => ({
        purchaseDetailId: info.purchaseDetailId,
        productionDate: formatApiDate(info.productionDate)
      }));
    
    // 调用API处理入库
    await api.purchase.processPurchaseInStock({
      purchaseId: purchaseId,
      batchInfoList: batchInfoList
    });
    
    closeToast();
    
    showToast({
      message: '入库成功',
      type: 'success'
    });
    
    // 返回采购单列表
    setTimeout(() => {
      router.replace('/purchase');
    }, 1000);
  } catch (error) {
    console.error('入库处理失败:', error);
    showToast({
      message: '入库失败: ' + (error.message || '未知错误'),
      type: 'fail'
    });
  }
};

// 取消采购订单
const cancelPurchase = () => {
  // 根据不同状态显示不同的提示信息
  const message = purchase.value.state === '已下单' 
    ? '确定要取消该采购单吗？此操作将直接删除采购记录。' 
    : '确定要取消该采购单吗？此操作将回退库存并删除采购记录。';
  
  showDialog({
    title: '取消采购单',
    message,
    confirmButtonText: '确定取消',
    confirmButtonColor: '#ee0a24',
    showCancelButton: true
  }).then(action => {
    if (action === 'confirm') {
      performCancelPurchase();
    }
  });
};

// 执行取消采购单的操作
const performCancelPurchase = async () => {
  try {
    const loadingToast = showLoadingToast({
      message: '处理中...',
      forbidClick: true,
      duration: 0
    });
    
    await api.purchase.cancelPurchaseOrder({ purchaseId });
    
    closeToast();
    
    showToast({
      message: '采购单已取消',
      type: 'success'
    });
    
    // 返回采购单列表
    setTimeout(() => {
      router.replace('/purchase');
    }, 1000);
  } catch (error) {
    console.error('取消采购单失败:', error);
    showToast({
      message: '取消失败: ' + (error.message || '未知错误'),
      type: 'fail'
    });
  }
};

// 工具函数
const formatAmount = (amount) => {
  return Number(amount || 0).toFixed(2);
};

const formatDateTime = (dateTime) => {
  if (!dateTime) return '-';
  const date = new Date(dateTime);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
};

// 格式化显示日期
const formatDisplayDate = (date) => {
  if (!date) return '请选择';
  
  if (typeof date === 'string') {
    // 如果是字符串，尝试解析
    const dateObj = new Date(date);
    if (isNaN(dateObj.getTime())) return '日期无效';
    return `${dateObj.getFullYear()}-${String(dateObj.getMonth() + 1).padStart(2, '0')}-${String(dateObj.getDate()).padStart(2, '0')}`;
  }
  
  if (date instanceof Date) {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }
  
  return '日期格式错误';
};

// 格式化日期为API需要的字符串格式
const formatApiDate = (date) => {
  if (!date) return null;
  
  if (typeof date === 'string') {
    // 检查字符串格式是否正确
    if (/^\d{4}-\d{2}-\d{2}$/.test(date)) {
      return date;
    }
    // 尝试解析字符串为日期
    const dateObj = new Date(date);
    if (isNaN(dateObj.getTime())) return null;
    return `${dateObj.getFullYear()}-${String(dateObj.getMonth() + 1).padStart(2, '0')}-${String(dateObj.getDate()).padStart(2, '0')}`;
  }
  
  if (date instanceof Date) {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }
  
  return null;
};

// 生命周期钩子
onMounted(() => {
  if (!purchaseId) {
    showToast('无效的采购单ID');
    router.back();
    return;
  }
  
  loadPurchaseDetail();
});
</script>

<style scoped>
.purchase-instock-page {
  padding-top: 46px;
  min-height: 100vh;
  background-color: #f7f8fa;
}

.content-container {
  padding: 16px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
}

.purchase-info {
  background-color: white;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.info-item .label {
  color: #666;
}

.info-item .value {
  font-weight: 500;
}

.info-item .price {
  color: #ee0a24;
  font-weight: bold;
}

.product-list {
  background-color: white;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.product-list h3 {
  margin-top: 0;
  margin-bottom: 16px;
  font-size: 16px;
  color: #333;
}

.product-item {
  padding: 12px 0;
  border-bottom: 1px solid #ebedf0;
}

.product-item:last-child {
  border-bottom: none;
}

.product-info {
  margin-bottom: 12px;
}

.product-name {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
}

.product-spec {
  display: flex;
  justify-content: space-between;
  color: #666;
  font-size: 14px;
}

.batch-management {
  background-color: #f7f8fa;
  border-radius: 8px;
  padding: 12px;
  margin-top: 8px;
}

.batch-title {
  margin-bottom: 12px;
}

.action-buttons {
  margin-top: 24px;
  padding: 0 16px;
}

.cancel-button {
  margin-top: 12px;
}
</style> 