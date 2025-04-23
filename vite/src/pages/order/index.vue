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
      >
      <template #left>
        <van-button
          v-if="!orderListStore.isSelectionMode"
          type="primary"
          size="small"
          @click="goToChooseOrder()"
        >
          筛选
        </van-button>
        <van-button
          v-else
          plain
          type="default"
          size="small"
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
            @click="orderListStore.startSelection()"
          >
            多选
          </van-button>
          <template v-else>
            <van-button
              :disabled="orderListStore.selectedOrders.length === 0 || !orderListStore.isSameShop"
              type="success"
              size="small"
              @click="orderListStore.handleMergedPrint()"
              style="margin-right: 10px;"
            >
              合并打印
            </van-button>
            <van-button
              :disabled="orderListStore.selectedOrders.length === 0 || !orderListStore.isSameShop"
              type="primary"
              size="small"
              @click="orderListStore.handleMergedCopy()"
            >
              合并复制
            </van-button>
          </template>
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
            title="点击商店名浏览该商店订单"
          >
            商店: {{ orderListStore.filterParams.shopName }}
          </van-tag>
          <van-tag
            v-if="orderListStore.dateRangeText"
            type="success"
            size="medium"
            title="订单日期范围"
          >
            日期: {{ orderListStore.dateRangeText }}
          </van-tag>
          <van-tag
            type="danger"
            size="medium"
            @click="clearAllFilters"
            style="cursor: pointer;"
            title="清除所有筛选条件"
          >
            清除
          </van-tag>
        </div>
      </div>

      <!-- Tabs 容器 -->
      <div class="tabs-container">
        <van-tabs v-model:active="orderListStore.activeTab" sticky :offset-top="46">
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
import { onMounted, ref,onUnmounted } from 'vue';
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
      returnOrderList
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
  height: 100vh; /* 使用 vh 单位确保占满整个视口高度 */
  overflow: hidden; /* 防止整体滚动 */
  background-color: #f7f8fa;
}

.page-header {
  flex-shrink: 0; /* 防止头部被压缩 */
  background-color: #fff; /* 给头部一个背景色 */
  z-index: 1; /* 确保头部在内容之上 */
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.filter-tags {
  padding: 8px 16px;
  border-bottom: 1px solid #ebedf0;
  overflow-x: auto; /* 添加横向滚动支持 */
  -webkit-overflow-scrolling: touch; /* 提升移动端滚动体验 */
  max-width: 100%; /* 限制最大宽度 */
}

.filter-tag-scroll {
  display: flex;
  align-items: center;
  flex-wrap: nowrap; /* 改为不换行 */
  gap: 8px;
  min-width: min-content; /* 保证内容不被压缩 */
  padding-bottom: 4px; /* 为可能的滚动条留出空间 */
}

/* 为筛选标签添加样式 */
.filter-tag-scroll .van-tag {
  flex-shrink: 0; /* 防止被压缩 */
}

.tabs-container {
  position: relative;
  z-index: 1;
}

.tabs-container .van-tabs {
  width: 100%;
}

/* 增强选项卡的样式 */
.tabs-container .van-tab {
  font-size: 15px;
  font-weight: 500;
}

.tabs-container .van-tab--active {
  font-weight: 700;
  color: #1989fa;
}

.tabs-container .van-tabs__line {
  background-color: #1989fa;
  height: 3px;
}

.main-content-area {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  position: relative;
  background-color: #f7f8fa;
  padding-bottom: calc(50px + env(safe-area-inset-bottom, 0px)); /* 确保在有Tabbar的页面底部有足够空间 */
}

.nav-right-btns {
  display: flex;
  align-items: center;
}
</style>
