<template>
  <div class="expiry-monitor-page">
    <van-nav-bar title="临期批次监控" left-arrow @click-left="goBack" right-text="已处理历史" @click-right="goToProcessedHistory" fixed placeholder />
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
        <div v-else class="batch-card-group">
          <div
            v-for="item in nearExpiryBatches"
            :key="item.batchId"
            class="batch-card"
          >
            <div class="batch-card-header">
              <van-tag type="primary" size="medium">{{ item.shopName }}</van-tag>
              <span class="product-name">{{ item.productName ?? '未知产品' }}</span>
            </div>
            <div class="batch-card-body">
              <div class="batch-info-row">
                <van-icon name="calendar-o" />
                <span>生产日期：</span>
                <span class="info-strong">{{ formatDate(item.productionDate) ?? '未知' }}</span>
              </div>
              <div class="batch-info-row">
                <van-icon name="clock-o" />
                <span :class="{'overdue': item.daysSinceProduction >= item.expiryMonitoringThresholdDays}">
                  已过天数：{{ formatDays(item.daysSinceProduction) }}
                </span>
                <span> / 阈值：{{ item.expiryMonitoringThresholdDays }} 天</span>
              </div>
              <div class="batch-info-row">
                <van-icon name="orders-o" />
                <span>订单数量：</span>
                <span class="info-strong">{{ item.productQuantity ?? 'N/A' }}</span>
              </div>
              <div class="batch-info-row">
                <van-icon name="idcard" />
                <span>订单 ID：</span>
                <span>{{ item.orderId ?? 'N/A' }}</span>
              </div>
              <div class="batch-info-row latest-order-time">
                <van-icon name="underway-o" />
                <span>最新相关订单：</span>
                <span>{{ formatDateTime(item.latestOrderTime) }}</span>
              </div>
              <div class="batch-card-actions">
                <van-button
                  size="small"
                  type="primary"
                  icon="success"
                  plain
                  @click="handleMarkAsProcessed(item.saleBatchDetailId, index)"
                  :disabled="item.processing"
                >
                  {{ item.processing ? '处理中...' : '标记处理' }}
                </van-button>
              </div>
            </div>
          </div>
        </div>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'; // 导入 nextTick
import { useRouter } from 'vue-router';
import api from '@/api'; // 假设 api 实例已配置
import dayjs from 'dayjs'; // 用于日期格式化
import { ROUTE_NAMES } from '@/constants/routeNames'; // 导入路由名称常量

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
    let result = await api.monitoring.getNearExpiryBatches();
    // 按订单日期降序排序
    result = result.sort((a, b) => {
      const t1 = a.latestOrderTime ? new Date(a.latestOrderTime).getTime() : 0;
      const t2 = b.latestOrderTime ? new Date(b.latestOrderTime).getTime() : 0;
      return t1 - t2;
    });
    // 为每个项目添加 processing 状态
    nearExpiryBatches.value = result.map(item => ({ ...item, processing: false }));
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

const handleMarkAsProcessed = async (saleBatchDetailId, index) => {
  if (!saleBatchDetailId) {
    showFailToast('无效的项目ID');
    return;
  }
  const item = nearExpiryBatches.value[index];
  if (item) {
    item.processing = true;
  }

  try {
    await showConfirmDialog ({
      title: '确认操作',
      message: '确定要将此条目标记为已处理吗？',
    });
    // 用户确认后执行
    await api.monitoring.markExpiryItemAsProcessed(saleBatchDetailId);
    showSuccessToast('标记成功');
    // 从列表中移除该项
    nearExpiryBatches.value.splice(index, 1);
    // 如果列表为空，确保显示 Empty 状态，可能需要 nextTick
    await nextTick();
    if (nearExpiryBatches.value.length === 0) {
        // 可以在这里强制更新 van-list 的某些状态，如果它没有自动更新为空状态
    }
  } catch (err) {
  } finally {
    if (item) {
      item.processing = false;
    }
  }
};

onMounted(() => {
  fetchNearExpiryBatches(); // 调用新的获取函数
});

const goToProcessedHistory = () => {
  router.push({ name: ROUTE_NAMES.MONITOR_PROCESSED_HISTORY });
};
</script>

<style scoped>
.expiry-monitor-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f7f8fa 60%, #e3e6ed 100%);
  padding-bottom: 24px;
}

.batch-list {
  padding: 0 0 60px 0;
}

.batch-card-group {
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin: 18px 12px 20px 12px;
}

.batch-card {
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 4px 24px 0 rgba(44, 62, 80, 0.10), 0 1.5px 4px 0 rgba(44, 62, 80, 0.04);
  padding: 22px 20px 18px 20px;
  transition: box-shadow 0.2s, transform 0.15s;
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: relative;
  border: 1px solid #f0f1f5;
}

.batch-card-actions {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e8e8e8;
  display: flex;
  justify-content: flex-end;
}

.batch-card:hover {
  box-shadow: 0 8px 32px 0 rgba(44, 62, 80, 0.18);
  transform: translateY(-2px) scale(1.01);
}

.batch-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 2px;
}
.product-name {
  font-size: 18px;
  font-weight: 700;
  color: #1a2233;
  letter-spacing: 0.5px;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.batch-card-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.batch-info-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  color: #4a5568;
  line-height: 1.7;
}
.batch-info-row .van-icon {
  font-size: 18px;
  color: #7b8db0;
}
.info-strong {
  color: #222;
  font-weight: 600;
}
.latest-order-time {
  font-size: 14px;
  color: #7b8db0;
  margin-top: 2px;
}
.overdue {
  color: #ee0a24;
  font-weight: bold;
  letter-spacing: 0.5px;
}

/* 状态小圆点 */
.batch-card::before {
  content: '';
  position: absolute;
  top: 22px;
  right: 22px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffb300, #ee0a24);
  box-shadow: 0 0 8px 2px #ee0a2433;
  opacity: 0.7;
}

/* 响应式适配 */
@media (max-width: 600px) {
  .batch-card-group {
    gap: 14px;
    margin: 10px 2px 16px 2px;
  }
  .batch-card {
    padding: 14px 6px 10px 10px;
    border-radius: 12px;
  }
  .product-name {
    font-size: 15px;
  }
  .batch-info-row {
    font-size: 13px;
  }
}

/* 错误提示美化 */
.error-message {
  padding: 24px 10px;
  text-align: center;
  color: #ee0a24;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  background: #fff0f0;
  border-radius: 12px;
  margin: 18px 12px;
  box-shadow: 0 2px 8px 0 rgba(238,10,36,0.06);
}

/* 暗色模式 */
@media (prefers-color-scheme: dark) {
  .expiry-monitor-page {
    background: linear-gradient(135deg, #23242a 60%, #181a20 100%);
  }
  .batch-card {
    background: #23242a;
    border: 1px solid #2c2d34;
    box-shadow: 0 4px 24px 0 rgba(44, 62, 80, 0.18);
  }
  .product-name {
    color: #fff;
  }
  .batch-info-row {
    color: #bfc4d1;
  }
  .info-strong {
    color: #fff;
  }
  .latest-order-time {
    color: #8e99b3;
  }
  .batch-card::before {
    background: linear-gradient(135deg, #ffb300, #ee0a24);
    box-shadow: 0 0 8px 2px #ee0a2499;
  }
  .error-message {
    background: #2c2d34;
    color: #ffb3b3;
    box-shadow: 0 2px 8px 0 rgba(238,10,36,0.12);
  }
}
</style>
