<route>
  {
    name: 'monitor-expiry', // 保持路由名称一致性
    meta: {
      title: '临期批次监控' // 更新标题
    }
  }
</route>

<template>
  <div class="expiry-monitor-page">
    <van-nav-bar title="临期批次监控" left-arrow @click-left="goBack" fixed placeholder />

    <van-pull-refresh v-model="loading" @refresh="fetchNearExpiryBatches">
      <div v-if="error" class="error-message">
        <van-icon name="warning-o" /> {{ error }}
        <van-button size="small" type="primary" plain @click="fetchNearExpiryBatches">重试</van-button>
      </div>

      <van-list
        v-else
        v-model:loading="listLoading"
        :finished="true"
        finished-text="没有更多了"
        class="batch-list"
      >
        <van-empty v-if="!loading && nearExpiryBatches.length === 0" description="暂无临期批次" />

        <van-cell-group v-else inset class="batch-group">
          <van-cell
            v-for="item in nearExpiryBatches"
            :key="item.batchId"
            class="batch-item"
          >
            <template #title>
              <div class="item-title">
                <van-tag type="primary" size="medium">{{ item.shopName }}</van-tag>
                <span class="product-name">{{ item.productName }}</span>
                <van-tag type="warning" plain size="medium" class="batch-number-tag">批次: {{ item.batchNumber }}</van-tag>
              </div>
            </template>
            <template #label>
              <div class="item-details">
                <div>生产日期: {{ formatDate(item.productionDate) ?? '未知' }}</div>
                <div>
                  <span :class="{'overdue': item.daysSinceProduction >= item.thresholdDays}">
                    已过天数: {{ formatDays(item.daysSinceProduction) }}
                  </span>
                  <span> / 阈值: {{ item.thresholdDays }} 天</span>
                </div>
                <!-- 可以选择性显示最新相关订单时间 -->
                <!-- <div class="latest-order-time">最新相关订单: {{ formatDateTime(item.latestRelevantOrderTime) }}</div> -->
              </div>
            </template>
          </van-cell>
        </van-cell-group>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import api from '@/api'; // 假设 api 实例已配置
import { showFailToast } from 'vant';
import dayjs from 'dayjs'; // 用于日期格式化

const router = useRouter();
const nearExpiryBatches = ref([]); // 重命名 ref
const loading = ref(false);
const listLoading = ref(false);
const error = ref(null);

// 重命名函数并更新 API 调用
const fetchNearExpiryBatches = async () => {
  loading.value = true;
  listLoading.value = true;
  error.value = null;
  try {
    // 假设新的 API 方法是 api.monitor.getNearExpiryBatches
    nearExpiryBatches.value = await api.monitoring.getNearExpiryBatches();
  } catch (err) {
    console.error('获取临期批次失败:', err);
    error.value = '加载数据失败，请稍后重试';
    showFailToast(error.value);
  } finally {
    loading.value = false;
    listLoading.value = false;
  }
};

const goBack = () => {
  router.back();
};

const formatDate = (date) => {
  if (!date) return 'N/A'; // 处理空日期
  return dayjs(date).format('YYYY-MM-DD');
};

// 新增：格式化日期时间
const formatDateTime = (dateTime) => {
    if (!dateTime) return 'N/A';
    return dayjs(dateTime).format('YYYY-MM-DD HH:mm');
}

// 格式化天数，移除对无限期的特殊处理，因为后端逻辑已改
const formatDays = (days) => {
    if (days === null || days === undefined) {
        return '未知'; // 如果天数计算不出来
    }
    // 不再需要检查 Long.MAX_VALUE 或超大数
    return `${days} 天`;
}


onMounted(() => {
  fetchNearExpiryBatches(); // 调用新的获取函数
});
</script>

<style scoped>
.expiry-monitor-page {
  min-height: 100vh;
  background-color: #f7f8fa;
}

.batch-list { /* 重命名 class */
  padding: 10px;
  padding-bottom: 60px;
}

.batch-group { /* 重命名 class */
  margin-bottom: 10px;
}

.batch-item .item-title { /* 重命名 class */
  display: flex;
  align-items: center;
  flex-wrap: wrap; /* 允许换行 */
  gap: 8px; /* 元素间距 */
  margin-bottom: 8px;
}

.batch-item .product-name { /* 重命名 class */
  /* margin-left: 8px; */ /* 使用 gap 代替 */
  font-weight: bold;
  color: #323233;
}

.batch-number-tag {
   /* margin-left: 8px; */ /* 使用 gap 代替 */
}

.item-details div {
  margin-bottom: 4px;
  color: #646566;
  font-size: 13px;
}
.item-details div:last-child {
  margin-bottom: 0;
}
.latest-order-time {
  font-size: 12px;
  color: #969799;
  margin-top: 4px;
}


.overdue {
  color: var(--van-danger-color);
  font-weight: bold;
}

.error-message {
  padding: 20px;
  text-align: center;
  color: #ee0a24;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

/* 暗色模式 */
@media (prefers-color-scheme: dark) {
  .expiry-monitor-page {
    background-color: #1c1c1e;
  }
  .batch-item .product-name { /* 重命名 class */
     color: #fff;
  }
   .item-details div {
      color: #8e8e93;
   }
   .latest-order-time {
       color: #636366;
   }
}
</style>