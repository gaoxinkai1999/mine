<route>
{
  name: "order-home"
}
</route>

<template>
  <div class="order-list-page">
    <van-nav-bar
        title="全部订单"
        fixed
        placeholder
        safe-area-inset-top
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
    <div class="filter-tags" v-if="orderListStore.hasFilters">
      <div class="filter-tag-scroll">
        <van-tag
          v-if="orderListStore.filterParams.shopName"
          type="primary"
          size="medium"
        >
          商店: {{ orderListStore.filterParams.shopName }}
        </van-tag>
        <van-tag
          v-if="orderListStore.dateRangeText"
          type="success"
          size="medium"
        >
          日期: {{ orderListStore.dateRangeText }}
        </van-tag>
        <van-tag
          type="danger"
          size="medium"
          @click="clearAllFilters"
        >
          清除
        </van-tag>

      </div>
    </div>
    
    <div class="tabs-container" :class="{'has-filter-tags': orderListStore.hasFilters}">
      <van-tabs v-model="orderListStore.activeTab" sticky offset-top="46px">
        <van-tab title="销售订单">
          <div class="tab-content-container">
            <salesOrderList
              ref="salesOrderList" 
              :selection-mode="orderListStore.isSelectionMode" 
              @order-selected="handleOrderSelected"
            ></salesOrderList>
          </div>
        </van-tab>
        <van-tab title="退货订单">
          <div class="tab-content-container">
            <returnOrderList
              ref="returnOrderList" 
              :selection-mode="orderListStore.isSelectionMode" 
              @order-selected="handleOrderSelected"
            ></returnOrderList>
          </div>
        </van-tab>
      </van-tabs>
    </div>
  </div>
</template>

<script>
import salesOrderList  from "@/pages/order/sale/index.vue";
import returnOrderList  from "@/pages/order/return/index.vue";
import { useOrderListStore } from "@/stores/orderList";
import { ROUTE_NAMES } from '@/constants/routeNames';

export default {
  name: "AllOrder",
  components: {salesOrderList, returnOrderList,},
  setup() {
    const orderListStore = useOrderListStore();
    
    return {
      orderListStore
    };
  },
  mounted() {
    // 从路由获取筛选条件并更新到store
    this.updateFiltersFromRoute();
  },

  methods: {
    // 更新筛选参数从路由到store
    updateFiltersFromRoute() {
      const { shopId, shopName, startDate, endDate } = this.$route.query;
      
      this.orderListStore.setFilterParams({
        shopId: shopId || null,
        shopName: shopName || '',
        startDate: startDate || null,
        endDate: endDate || null
      });
    },

    
    // 清除所有筛选
    clearAllFilters() {
      this.$router.replace({ query: {} });
      this.orderListStore.clearAllFilters();
    },
    

    
    // 跳转到筛选页面
    goToChooseOrder() {
      this.$router.push({
        name: ROUTE_NAMES.ORDER_LIMIT,
      });
    },
    
    // 处理订单选中事件
    handleOrderSelected(order, isSelected) {
      this.orderListStore.handleOrderSelected(order, isSelected);
    }
  },
}
</script>

<style scoped>
.filter-tags {
  padding: 8px 16px;
  background-color: #fff;
  position: sticky;
  top: 46px;
  z-index: 1;
  border-bottom: 1px solid #ebedf0;
}

.filter-tag-scroll {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.tabs-container.has-filter-tags {
  padding-top: 10px;
}
</style>
