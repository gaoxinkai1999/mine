<route>
{
  name: "order-home"
}
</route>

<template>
  <div class="order-list-page">
    <!-- 头部区域 -->
    <div class="page-header">
      <!-- 导航栏 -->
      <van-nav-bar
          title="全部订单"
          safe-area-inset-top
          :z-index="102" 
          class="custom-nav-bar"
      >
      <template #left>
        <van-button
          v-if="!orderListStore.isSelectionMode"
          type="primary"
          size="small"
          class="action-btn filter-btn"
          icon="filter-o"
          @click="goToChooseOrder()"
        >
          筛选
        </van-button>
        <van-button
          v-else
          plain
          type="default"
          size="small"
          class="action-btn"
          icon="cross"
          @click="orderListStore.cancelSelection()"
        >
          取消
        </van-button>
      </template>
      <template #right>
        <div class="nav-right-btns">
          <van-button
            v-if="!orderListStore.isSelectionMode"
            plain
            type="primary"
            size="small"
            class="action-btn"
            icon="checked"
            @click="orderListStore.startSelection()"
          >
            多选
          </van-button>
          <!-- 多选模式下的操作按钮，使用 Popover -->
          <van-popover
            v-else
            v-model:show="showPopover"
            :actions="popoverActions"
            @select="handlePopoverSelect"
            placement="bottom-end"
            theme="light"
            :offset="[8, 8]"
          >
            <template #reference>
              <van-button
                plain
                type="primary"
                size="small"
                class="action-btn"
                icon="ellipsis"
              >
                操作
              </van-button>
            </template>
          </van-popover>
        </div>
      </template>
      </van-nav-bar>

      <!-- 筛选标签展示区域 -->
      <div
        class="filter-tags"
        v-if="orderListStore.hasFilters"
      >
        <div class="filter-tag-scroll">
          <van-tag
            v-if="orderListStore.filterParams.shopName"
            type="primary"
            size="medium"
            style="cursor: pointer;"
            class="filter-tag shop-tag"
            title="点击商店名浏览该商店订单"
          >
            <van-icon name="shop-o" />
            {{ orderListStore.filterParams.shopName }}
          </van-tag>
          <van-tag
            v-if="orderListStore.dateRangeText"
            type="success"
            size="medium"
            class="filter-tag date-tag"
            title="订单日期范围"
          >
            <van-icon name="calendar-o" />
            {{ orderListStore.dateRangeText }}
          </van-tag>
          <van-tag
            type="danger"
            size="medium"
            @click="clearAllFilters"
            style="cursor: pointer;"
            class="filter-tag clear-tag"
            title="清除所有筛选条件"
          >
            <van-icon name="delete-o" />
            清除筛选
          </van-tag>
        </div>
      </div>

      <!-- Tabs 容器 -->
      <div class="tabs-container">
        <van-tabs 
          v-model:active="orderListStore.activeTab" 
          sticky 
          :offset-top="46"
          animated
          swipeable
          class="custom-tabs"
        >
          <van-tab title="销售订单" name="0"></van-tab>
          <van-tab title="退货订单" name="1"></van-tab>
        </van-tabs>
      </div>
    </div> <!-- 结束 .page-header -->

    <!-- 主要内容区域 -->
    <div class="main-content-area">
      <!-- 不使用keep-alive，每次都重新创建组件 -->
      <salesOrderList
        ref="salesOrderList"
        v-if="orderListStore.activeTab === '0' || orderListStore.activeTab === 0"
        :selection-mode="orderListStore.isSelectionMode"
        @order-selected="handleOrderSelected"
        @filter-by-shop="handleFilterByShop"
      ></salesOrderList>
      <returnOrderList
        ref="returnOrderList"
        v-if="orderListStore.activeTab === '1' || orderListStore.activeTab === 1"
        :selection-mode="orderListStore.isSelectionMode"
        @order-selected="handleOrderSelected"
        @filter-by-shop="handleFilterByShop"
      ></returnOrderList>
    </div>

  </div>
</template>

<script>
import salesOrderList from "@/pages/order/sale/index.vue";
import returnOrderList from "@/pages/order/return/index.vue";
import { useOrderListStore } from "@/stores/orderList";
import { ROUTE_NAMES } from '@/constants/routeNames';
import { onMounted, ref, onUnmounted, computed } from 'vue'; // 引入 computed
import { useRoute, useRouter } from 'vue-router';

export default {
  name: "AllOrder",
  components: { salesOrderList, returnOrderList },
  setup() {
    const orderListStore = useOrderListStore();
    const route = useRoute();
    const router = useRouter();
    const salesOrderList = ref(null);
    const returnOrderList = ref(null);
    const showPopover = ref(false); // 控制 Popover 显示

    // Popover 操作项
    const popoverActions = computed(() => [
      {
        text: '合并打印',
        icon: 'printer',
        disabled: orderListStore.selectedOrders.length === 0 || !orderListStore.isSameShop,
        actionKey: 'print' // 添加唯一标识
      },
      {
        text: '合并复制',
        icon: 'description',
        disabled: orderListStore.selectedOrders.length === 0 || !orderListStore.isSameShop,
        actionKey: 'copy' // 添加唯一标识
      },
    ]);

    // 处理 Popover 选择事件
    const handlePopoverSelect = (selectedAction) => {
      if (selectedAction.actionKey === 'print') {
        orderListStore.handleMergedPrint();
      } else if (selectedAction.actionKey === 'copy') {
        orderListStore.handleMergedCopy();
      }
      showPopover.value = false; // 选择后关闭 Popover
    };

    // 更新筛选参数从路由到store
    const updateFiltersFromRoute = () => {
      const { shopId, shopName, startDate, endDate } = route.query;
      orderListStore.setFilterParams({
        shopId: shopId || null,
        shopName: shopName || '',
        startDate: startDate || null,
        endDate: endDate || null
      });
    };

    // 清除所有筛选
    const clearAllFilters = () => {
      router.replace({ query: {} });
      orderListStore.clearAllFilters();
      // 刷新当前激活的列表组件
      if (orderListStore.activeTab === '0' || orderListStore.activeTab === 0) {
        if (salesOrderList.value) {
          salesOrderList.value.resetAndLoad();
        }
      } else if (orderListStore.activeTab === '1' || orderListStore.activeTab === 1) {
        if (returnOrderList.value) {
          returnOrderList.value.resetAndLoad();
        }
      }
      
    };

    // 跳转到筛选页面
    const goToChooseOrder = () => {
      router.push({
        name: ROUTE_NAMES.ORDER_LIMIT,
      });
    };

    // 处理订单选中事件
    const handleOrderSelected = (order, isSelected) => {
      orderListStore.handleOrderSelected(order, isSelected);
    };

    // 处理子组件发出的按商店筛选事件
    const handleFilterByShop = (shop) => {
      // console.log('[handleFilterByShop] Received shop:', JSON.stringify(shop)); // 移除日志
      if (!shop || !shop.id || !shop.name) {
        console.warn('无效的商店信息，无法筛选:', shop); // 保留警告
        return;
      }
      const currentStartDate = orderListStore.filterParams.startDate;
      const currentEndDate = orderListStore.filterParams.endDate;
      const newFilterParams = {
        shopId: shop.id,
        shopName: shop.name,
        startDate: currentStartDate,
        endDate: currentEndDate
      };
      orderListStore.setFilterParams(newFilterParams);
      router.replace({
        query: {
          shopId: shop.id,
          shopName: shop.name,
          ...(currentStartDate && { startDate: currentStartDate }),
          ...(currentEndDate && { endDate: currentEndDate })
        }
      });
      
      // 刷新当前激活的列表组件
      if (orderListStore.activeTab === '0' || orderListStore.activeTab === 0) {
        if (salesOrderList.value) {
          salesOrderList.value.resetAndLoad();
        }
      } else if (orderListStore.activeTab === '1' || orderListStore.activeTab === 1) {
        if (returnOrderList.value) {
          returnOrderList.value.resetAndLoad();
        }
      }
    };

    onMounted(() => {
      updateFiltersFromRoute();
    });
    onUnmounted(() => {
      orderListStore.clearAllFilters()
    });

    return {
      orderListStore,
      clearAllFilters,
      goToChooseOrder,
      handleOrderSelected,
      handleFilterByShop,
      salesOrderList,
      returnOrderList,
      showPopover,        // 导出 Popover 显示状态
      popoverActions,     // 导出 Popover 操作项
      handlePopoverSelect // 导出 Popover 选择处理函数
    };
  }
}
</script>

<style scoped>
.order-list-page {
  display: flex;
  flex-direction: column;
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #f6f9fc 0%, #eef2f5 100%);
}

.page-header {
  flex-shrink: 0; /* 防止头部被压缩 */
  background-color: #fff; /* 给头部一个背景色 */
  z-index: 10; /* 确保头部在内容之上 */
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.custom-nav-bar {
  background: linear-gradient(135deg, #4a6cf7 0%, #2541b2 100%);
  box-shadow: 0 4px 20px rgba(37, 65, 178, 0.2);
}

.custom-nav-bar :deep(.van-nav-bar__title) {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

.action-btn {
  border-radius: 8px;
  padding: 0 16px;
  height: 36px;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.action-btn:active {
  opacity: 0.8;
}

.filter-btn {
  background-color: rgba(255, 255, 255, 0.2);
  border-color: transparent;
  color: white;
}

.print-btn {
  margin-right: 8px;
}

.copy-btn {
  background-color: #1989fa;
}

.filter-tags {
  padding: 12px 16px;
  border-bottom: none;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  max-width: 100%;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.filter-tag-scroll {
  display: flex;
  align-items: center;
  flex-wrap: nowrap; /* 改为不换行 */
  gap: 10px;
  min-width: min-content; /* 保证内容不被压缩 */
  padding-bottom: 4px; /* 为可能的滚动条留出空间 */
}

/* 为筛选标签添加样式 */
.filter-tag {
  flex-shrink: 0;
  border-radius: 20px;
  padding: 6px 16px;
  font-size: 13px;
  display: flex;
  align-items: center;
  transition: all 0.2s ease;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.filter-tag :deep(.van-icon) {
  margin-right: 4px;
  font-size: 14px;
}

.shop-tag {
  background-color: #1989fa;
  border-color: #1989fa;
}

.date-tag {
  background-color: #07c160;
  border-color: #07c160;
}

.clear-tag {
  background-color: #f56c6c;
  border-color: #f56c6c;
}

.tabs-container {
  position: relative;
  z-index: 1;
}

.custom-tabs {
  width: 100%;
}

.custom-tabs :deep(.van-tab) {
  font-size: 15px;
  font-weight: 500;
  padding: 16px 0;
}

.custom-tabs :deep(.van-tab--active) {
  font-weight: 600;
  color: #1989fa;
}

.custom-tabs :deep(.van-tabs__line) {
  background-color: #1989fa;
  height: 3px;
  border-radius: 3px;
  bottom: 15px;
}

.main-content-area {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  position: relative;
  background: white;
  border-radius: 24px 24px 0 0;
  margin-top: -12px;
  padding: 24px 16px calc(50px + env(safe-area-inset-bottom, 0px));
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.05);
}

.nav-right-btns {
  display: flex;
  align-items: center;
}
</style>
