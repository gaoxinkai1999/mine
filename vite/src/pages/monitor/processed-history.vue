<template>
  <div class="processed-history-page">
    <van-nav-bar title="已处理临期历史" left-arrow @click-left="goBack" fixed placeholder />
    <van-pull-refresh v-model="loading" @refresh="fetchProcessedHistory">
      <div v-if="error" class="error-message">
        <van-icon name="warning-o" /> {{ error }}
        <van-button size="small" type="primary" plain @click="fetchProcessedHistory">重试</van-button>
      </div>
      <van-list
        v-else
        v-model:loading="listLoading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoadMore"
        class="history-list"
      >
        <van-empty v-if="!loading && processedItems.length === 0 && !listLoading" description="暂无已处理记录" />
        <div v-else class="history-card-group">
          <div
            v-for="(item, index) in processedItems"
            :key="item.saleBatchDetailId"
            class="history-card"
          >
            <div class="history-card-header">
              <van-tag type="success" size="medium">{{ item.shopName }}</van-tag>
              <span class="product-name">{{ item.productName ?? '未知产品' }}</span>
            </div>
            <div class="history-card-body">
              <div class="info-row"><van-icon name="idcard" /> 订单 ID: {{ item.orderId ?? 'N/A' }}</div>
              <div class="info-row"><van-icon name="label-o" /> 批次 ID: {{ item.batchId ?? 'N/A' }}</div>
              <div class="info-row"><van-icon name="calendar-o" /> 生产日期: {{ formatDate(item.productionDate) ?? '未知' }}</div>
              <div class="info-row"><van-icon name="passed" /> 处理时间: {{ formatDateTime(item.processedAt) ?? '未知' }}</div>
            </div>
            <div class="history-card-actions">
              <van-button
                size="small"
                type="warning"
                icon="revoke"
                plain
                @click="handleUnmarkProcessed(item.saleBatchDetailId, index)"
                :disabled="item.processing"
              >
                {{ item.processing ? '撤销中...' : '撤销处理' }}
              </van-button>
            </div>
          </div>
        </div>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import api from '@/api';
import dayjs from 'dayjs';

const router = useRouter();
const processedItems = ref([]);
const loading = ref(false); // For pull-to-refresh
const listLoading = ref(false); // For van-list loading state
const error = ref(null);
const finished = ref(false); // For van-list: true if all data is loaded

const currentPage = ref(0);
const pageSize = ref(15); // Or your preferred page size

const fetchProcessedHistory = async (isRefresh = false) => {
  if (isRefresh) {
    currentPage.value = 0;
    processedItems.value = [];
    finished.value = false;
  }
  loading.value = isRefresh; // Set pull-refresh loading state
  listLoading.value = true; // Set list loading state
  error.value = null;

  try {
    const params = { page: currentPage.value, size: pageSize.value };
    const response = await api.monitoring.getProcessedItemHistory(params);
    const newItems = response.content.map(item => ({ ...item, processing: false }));

    if (newItems.length === 0 && !isRefresh) {
      finished.value = true;
    } else {
      processedItems.value.push(...newItems);
    }
    currentPage.value++; // Increment page for next load

    if (processedItems.value.length >= response.totalElements || response.last) {
      finished.value = true;
    }

  } catch (err) {
    console.error('获取已处理历史失败:', err);
    error.value = '加载数据失败，请稍后重试';
    if (!isRefresh) showFailToast(error.value); // Show toast only if not a pull-refresh error display
    finished.value = true; // Stop further loading on error
  } finally {
    loading.value = false;
    listLoading.value = false;
  }
};

const onLoadMore = () => {
  if (!finished.value && !listLoading.value) { // Prevent multiple loads
    fetchProcessedHistory();
  }
};

const handleUnmarkProcessed = async (saleBatchDetailId, index) => {
  if (!saleBatchDetailId) {
    showFailToast('无效的项目ID');
    return;
  }
  const item = processedItems.value[index];
  if (item) {
    item.processing = true;
  }

  try {
    await showConfirmDialog ({
      title: '确认操作',
      message: '确定要撤销对该条目的处理吗？撤销后它可能会重新出现在临期列表中。',
    });
    await api.monitoring.unmarkExpiryItemAsProcessed(saleBatchDetailId);
    showSuccessToast('撤销成功');
    processedItems.value.splice(index, 1);
    if (processedItems.value.length === 0 && currentPage.value === 0) { // Check if list becomes empty on first page
        finished.value = true; // If no items left, mark as finished
    }
  } finally {
    if (item) {
      item.processing = false;
    }
  }
};

const goBack = () => {
  router.back();
};

const formatDate = (date) => {
  if (!date) return 'N/A';
  return dayjs(date).format('YYYY-MM-DD');
};

const formatDateTime = (dateTime) => {
  if (!dateTime) return 'N/A';
  return dayjs(dateTime).format('YYYY-MM-DD HH:mm:ss');
};

onMounted(() => {
  fetchProcessedHistory(true);
});
</script>

<style scoped>
.processed-history-page {
  min-height: 100vh;
  background-color: #f7f8fa; /* Or your preferred background */
}
.history-list {
  padding-bottom: 20px;
}
.history-card-group {
  padding: 10px;
}
.history-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 10px;
  padding: 15px;
}
.history-card-header {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}
.history-card-header .van-tag {
  margin-right: 8px;
}
.product-name {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}
.history-card-body {
  font-size: 13px;
  color: #666;
  line-height: 1.8;
}
.info-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.info-row .van-icon {
  color: #999;
}
.history-card-actions {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebedf0;
  display: flex;
  justify-content: flex-end;
}
.error-message {
  padding: 20px;
  text-align: center;
  color: #ee0a24;
}
</style>