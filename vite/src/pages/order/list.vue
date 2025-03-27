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
          closeable
          type="primary"
          size="medium"
          @close="clearShopFilter"
        >
          商店: {{ orderListStore.filterParams.shopName }}
        </van-tag>
        <van-tag
          v-if="orderListStore.dateRangeText"
          closeable
          type="success"
          size="medium"
          @close="clearDateFilter"
        >
          日期: {{ orderListStore.dateRangeText }}
        </van-tag>
        <van-button
          v-if="orderListStore.hasFilters"
          type="default"
          size="mini"
          @click="clearAllFilters"
        >
          清除全部筛选
        </van-button>
      </div>
    </div>
    
    <div class="tabs-container" :class="{'has-filter-tags': orderListStore.hasFilters}">
      <van-tabs v-model="orderListStore.activeTab" sticky offset-top="46px">
        <van-tab title="销售订单">
          <div class="tab-content-container">
            <SalesOrderList 
              ref="salesOrderList" 
              :selection-mode="orderListStore.isSelectionMode" 
              @order-selected="handleOrderSelected"
            ></SalesOrderList>
          </div>
        </van-tab>
        <van-tab title="退货订单">
          <div class="tab-content-container">
            <ReturnOrderList 
              ref="returnOrderList" 
              :selection-mode="orderListStore.isSelectionMode" 
              @order-selected="handleOrderSelected"
            ></ReturnOrderList>
          </div>
        </van-tab>
      </van-tabs>
    </div>
  </div>
</template>

<script>
import SalesOrderList from "@/components/Order/SalesOrderList.vue";
import ReturnOrderList from "@/components/Order/ReturnOrderList.vue";
import { useOrderListStore } from "@/stores/orderList";

export default {
  name: "AllOrder",
  components: {SalesOrderList, ReturnOrderList},
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
  watch: {
    // 监听路由变化，更新筛选标签
    '$route.query': {
      handler() {
        this.updateFiltersFromRoute();
      },
      deep: true
    }
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
    
    // 清除商店筛选
    clearShopFilter() {
      const query = {...this.$route.query};
      delete query.shopId;
      delete query.shopName;
      this.$router.replace({ query });
      this.orderListStore.clearShopFilter();
      this.refreshOrderLists();
    },
    
    // 清除日期筛选
    clearDateFilter() {
      const query = {...this.$route.query};
      delete query.startDate;
      delete query.endDate;
      this.$router.replace({ query });
      this.orderListStore.clearDateFilter();
      this.refreshOrderLists();
    },
    
    // 清除所有筛选
    clearAllFilters() {
      this.$router.replace({ query: {} });
      this.orderListStore.clearAllFilters();
      this.refreshOrderLists();
    },
    
    // 刷新订单列表
    refreshOrderLists() {
      // 重置销售订单列表
      if (this.$refs.salesOrderList) {
        this.$refs.salesOrderList.orders = [];
        this.$refs.salesOrderList.pageIndex = 0;
        this.$refs.salesOrderList.finished = false;
        this.$refs.salesOrderList.filterParams = {
          shopId: this.orderListStore.filterParams.shopId || null,
          startDate: this.orderListStore.filterParams.startDate || null,
          endDate: this.orderListStore.filterParams.endDate || null
        };
        this.$refs.salesOrderList.onLoad();
      }
      
      // 重置退货订单列表
      if (this.$refs.returnOrderList) {
        this.$refs.returnOrderList.orders = [];
        this.$refs.returnOrderList.pageIndex = 0;
        this.$refs.returnOrderList.finished = false;
        this.$refs.returnOrderList.filterParams = {
          shopId: this.orderListStore.filterParams.shopId || null,
          startDate: this.orderListStore.filterParams.startDate || null,
          endDate: this.orderListStore.filterParams.endDate || null
        };
        this.$refs.returnOrderList.onLoad();
      }
    },
    
    // 跳转到筛选页面
    goToChooseOrder() {
      this.$router.push({
        path: '/order/OrderPage',
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
