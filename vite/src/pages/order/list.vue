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
          v-if="!isSelectionMode"
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
          @click="cancelSelection()"
        >
          取消
        </van-button>
      </template>
      <template #right>
        <div class="nav-right-btns">
          <van-button
            v-if="!isSelectionMode"
            plain
            type="primary"
            size="small"
            @click="startSelection()"
          >
            多选
          </van-button>
          <template v-else>
            <van-button
              :disabled="selectedOrders.length === 0 || !isSameShop"
              type="success"
              size="small"
              @click="handleMergedPrint()"
              style="margin-right: 10px;"
            >
              合并打印
            </van-button>
            <van-button
              :disabled="selectedOrders.length === 0 || !isSameShop"
              type="primary"
              size="small"
              @click="handleMergedCopy()"
            >
              合并复制
            </van-button>
          </template>
        </div>
      </template>
    </van-nav-bar>
    
    <!-- 筛选标签展示区域 -->
    <div class="filter-tags" v-if="hasFilters">
      <div class="filter-tag-scroll">
        <van-tag
          v-if="shopName"
          closeable
          type="primary"
          size="medium"
          @close="clearShopFilter"
        >
          商店: {{ shopName }}
        </van-tag>
        <van-tag
          v-if="dateRange"
          closeable
          type="success"
          size="medium"
          @close="clearDateFilter"
        >
          日期: {{ dateRange }}
        </van-tag>
        <van-button
          v-if="hasFilters"
          type="default"
          size="mini"
          @click="clearAllFilters"
        >
          清除全部筛选
        </van-button>
      </div>
    </div>
    
    <div class="tabs-container" :class="{'has-filter-tags': hasFilters}">
      <van-tabs v-model="active" sticky>
        <van-tab title="销售订单">
          <div class="tab-content-container">
            <SalesOrderList ref="salesOrderList" :selection-mode="isSelectionMode" @order-selected="handleOrderSelected"></SalesOrderList>
          </div>
        </van-tab>
        <van-tab title="退货订单">
          <div class="tab-content-container">
            <ReturnOrderList ref="returnOrderList" :selection-mode="isSelectionMode" @order-selected="handleOrderSelected"></ReturnOrderList>
          </div>
        </van-tab>
      </van-tabs>
    </div>
  </div>
</template>

<script>
import SalesOrderList from "@/components/Order/SalesOrderList.vue";
import ReturnOrderList from "@/components/Order/ReturnOrderList.vue";
import { formatMergedReceipt, printMergedOrders } from "@/utils/printService";
import { Clipboard } from '@capacitor/clipboard';
import { showSuccessToast, showFailToast } from "vant";

export default {
  name: "AllOrder",
  components: {SalesOrderList, ReturnOrderList},
  data() {
    return {
      active: 0, //默认显示销售订单
      isSelectionMode: false, // 是否处于多选模式
      selectedOrders: [], // 已选择的订单
      shopName: '', // 用于显示的店铺名称
      dateRange: '', // 用于显示的日期范围
    }
  },
  computed: {
    // 判断所有选中的订单是否来自同一个商店
    isSameShop() {
      if (this.selectedOrders.length <= 1) return true;
      
      const firstShopId = this.selectedOrders[0].shop.id;
      return this.selectedOrders.every(order => order.shop.id === firstShopId);
    },
    // 判断是否有筛选条件
    hasFilters() {
      return this.shopName || this.dateRange;
    }
  },
  mounted() {
    // 从路由获取筛选条件并显示
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
    // 更新筛选标签显示
    updateFiltersFromRoute() {
      const { shopId, shopName, startDate, endDate } = this.$route.query;
      
      // 更新商店名称
      this.shopName = shopName || '';
      
      // 更新日期范围显示
      if (startDate && endDate) {
        this.dateRange = `${startDate} 至 ${endDate}`;
      } else if (startDate) {
        this.dateRange = `从 ${startDate} 开始`;
      } else if (endDate) {
        this.dateRange = `至 ${endDate}`;
      } else {
        this.dateRange = '';
      }
    },
    
    // 清除商店筛选
    clearShopFilter() {
      const query = {...this.$route.query};
      delete query.shopId;
      delete query.shopName;
      this.$router.replace({ query });
      this.refreshOrderLists();
    },
    
    // 清除日期筛选
    clearDateFilter() {
      const query = {...this.$route.query};
      delete query.startDate;
      delete query.endDate;
      this.$router.replace({ query });
      this.refreshOrderLists();
    },
    
    // 清除所有筛选
    clearAllFilters() {
      this.$router.replace({ query: {} });
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
          shopId: this.$route.query.shopId || null,
          startDate: this.$route.query.startDate || null,
          endDate: this.$route.query.endDate || null
        };
        this.$refs.salesOrderList.onLoad();
      }
      
      // 重置退货订单列表
      if (this.$refs.returnOrderList) {
        this.$refs.returnOrderList.orders = [];
        this.$refs.returnOrderList.pageIndex = 0;
        this.$refs.returnOrderList.finished = false;
        this.$refs.returnOrderList.filterParams = {
          shopId: this.$route.query.shopId || null,
          startDate: this.$route.query.startDate || null,
          endDate: this.$route.query.endDate || null
        };
        this.$refs.returnOrderList.onLoad();
      }
    },
    
    goToChooseOrder() {
      this.$router.push({
        path: '/order/OrderPage',
      })
    },
    
    // 开始多选模式
    startSelection() {
      this.isSelectionMode = true;
      this.selectedOrders = [];
    },
    
    // 取消多选模式
    cancelSelection() {
      this.isSelectionMode = false;
      this.selectedOrders = [];
      
      // 清除组件中的选中状态
      if (this.$refs.salesOrderList) {
        this.$refs.salesOrderList.clearSelection();
      }
      if (this.$refs.returnOrderList) {
        this.$refs.returnOrderList.clearSelection();
      }
    },
    
    // 处理订单选中事件
    handleOrderSelected(order, isSelected) {
      if (isSelected) {
        this.selectedOrders.push(order);
      } else {
        this.selectedOrders = this.selectedOrders.filter(item => item.id !== order.id);
      }
    },
    
    // 合并打印
    async handleMergedPrint() {
      if (this.selectedOrders.length === 0) {
        showFailToast('请先选择订单');
        return;
      }
      
      if (!this.isSameShop) {
        showFailToast('只能合并来自同一商店的订单');
        return;
      }
      
      try {
        await printMergedOrders(this.selectedOrders, (status) => {
          console.log(status);
        });
        this.cancelSelection(); // 打印成功后退出选择模式
      } catch (error) {
        console.error('合并打印失败', error);
      }
    },
    
    // 合并复制
    async handleMergedCopy() {
      if (this.selectedOrders.length === 0) {
        showFailToast('请先选择订单');
        return;
      }
      
      if (!this.isSameShop) {
        showFailToast('只能合并来自同一商店的订单');
        return;
      }
      
      try {
        const receipt = formatMergedReceipt(this.selectedOrders, false);
        await Clipboard.write({
          string: receipt
        });
        showSuccessToast('合并订单已复制到剪切板');
        this.cancelSelection(); // 复制成功后退出选择模式
      } catch (err) {
        console.error('复制失败:', err);
        showFailToast('复制失败，错误信息:' + err);
      }
    }
  },
}
</script>

<style scoped>
.order-list-page {
  position: relative;
  height: 100vh;
  background-color: #f7f8fa;
}

.filter-tags {
  position: fixed;
  top: 46px;
  left: 0;
  right: 0;
  z-index: 10;
  background-color: white;
  padding: 8px 12px;
  border-bottom: 1px solid #ebedf0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.filter-tag-scroll {
  display: flex;
  align-items: center;
  overflow-x: auto;
  white-space: nowrap;
  -webkit-overflow-scrolling: touch;
  padding-bottom: 4px;
}

.filter-tag-scroll .van-tag {
  margin-right: 8px;
  padding: 2px 10px;
}

.filter-tag-scroll .van-button {
  margin-left: auto;
  flex-shrink: 0;
}

.tabs-container {
  padding-top: 46px; /* 导航栏的高度 */
  height: calc(100vh - 46px);
  overflow: hidden;
}

.tabs-container.has-filter-tags {
  padding-top: 90px; /* 导航栏 + 筛选标签栏 */
  height: calc(100vh - 90px);
}

.tab-content-container {
  height: calc(100vh - 46px - 44px); /* 总高度减去导航栏高度和标签栏高度 */
  overflow-y: auto;
}

.tabs-container.has-filter-tags .tab-content-container {
  height: calc(100vh - 90px - 44px); /* 加上筛选标签栏的高度调整 */
}

.nav-right-btns {
  display: flex;
  align-items: center;
}

/* 使用安全区适配 */
@supports (padding-top: env(safe-area-inset-top)) {
  .tabs-container {
    padding-top: calc(46px + env(safe-area-inset-top));
    height: calc(100vh - 46px - env(safe-area-inset-top));
  }
  
  .tabs-container.has-filter-tags {
    padding-top: calc(90px + env(safe-area-inset-top));
    height: calc(100vh - 90px - env(safe-area-inset-top));
  }
  
  .tab-content-container {
    height: calc(100vh - 46px - 44px - env(safe-area-inset-top));
  }
  
  .tabs-container.has-filter-tags .tab-content-container {
    height: calc(100vh - 90px - 44px - env(safe-area-inset-top));
  }
  
  .filter-tags {
    top: calc(46px + env(safe-area-inset-top));
  }
}
</style>