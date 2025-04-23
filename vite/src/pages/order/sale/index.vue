<template>
  <div class="sales-order-list">
    <van-list v-model:loading="loading" :finished="finished" finished-text="没有更多了" @load="onLoad">
      <van-cell-group v-for="(item, index) in orders" :key="index" class="order-card">
        <!-- 订单头部 -->
        <van-cell class="order-header">
          <template v-if="selectionMode" #icon>
            <van-checkbox
                :model-value="isOrderSelected(item)"
                style="margin-right: 10px;"
                @update:model-value="toggleOrderSelection(item, $event)"
            />
          </template>
          <template #title>
            <div class="order-header-content">
              <div class="order-header-left">
                <span class="order-id">ID: {{ item.id }}</span>
                <span class="order-time">{{ item.createTime }}</span>
              </div>
              <van-button size="small" type="primary" class="action-button" plain icon="more-o" @click="openActionSheet(item)">
                操作
              </van-button>
            </div>
          </template>
        </van-cell>

        <!-- 商家信息 -->
        <van-cell class="shop-info" icon="shop-o" size="large">
          <template #title>
            <div class="shop-title">
              <span class="shop-name" @click.stop="filterByShop(item.shop)" style="cursor: pointer;">{{ item.shop.name }}</span>
              <van-tag v-if="item.shop.del" type="danger" round>弃用</van-tag>
            </div>
          </template>
          <template #label>
            <span class="shop-location"><van-icon name="location-o" /> {{ item.shop.location }}</span>
          </template>
        </van-cell>

        <!-- 订单详情 -->
        <div class="order-details">
          <div v-for="(orderDetail, detailIndex) in item.orderDetails"
               :key="detailIndex"
               class="order-detail-item"
          >
            <div class="product-info">
              <span class="product-name">{{ orderDetail.product.name }}</span>
              <van-tag v-if="!orderDetail.defaultPrice" size="mini" type="danger" round>特价</van-tag>
            </div>
            <div class="quantity-price">
              <span class="quantity">x{{ orderDetail.quantity }}</span>
              <span class="price">¥{{ orderDetail.totalSalesAmount }}</span>
            </div>
          </div>
        </div>

        <!-- 订单汇总 -->
        <div class="order-summary">
          <div class="summary-item">
            <span class="label">总价</span>
            <span class="value total-amount">¥{{ item.totalSalesAmount }}</span>
          </div>
          <div class="summary-item">
            <span class="label">利润</span>
            <span class="value profit-amount">¥{{ item.totalProfit }}</span>
          </div>
        </div>

        <!-- 快捷操作按钮 -->
        <div class="quick-actions">
          <van-button type="primary" size="small" icon="printer" plain class="quick-action-btn" @click="handlePrintOrder()">打印</van-button>
          <van-button type="info" size="small" icon="description" plain class="quick-action-btn" @click="handleCopyOrder(item)">复制</van-button>
          <van-button type="danger" size="small" icon="delete" plain class="quick-action-btn" @click="handleDeleteOrder(item.id)">删除</van-button>
        </div>
      </van-cell-group>
    </van-list>

    <!-- 操作面板 -->
    <van-action-sheet
        v-model:show="showActionSheet"
        :actions="actions"
        cancel-text="取消"
        close-on-click-action
        round
    />
  </div>
</template>

<script>
import {Clipboard} from '@capacitor/clipboard';
import api from "@/api/index.js";
import {formatReceipt, printOrder} from "@/utils/printService.js";
import {showFailToast, showSuccessToast, showConfirmDialog} from "vant";
import {useOrderListStore} from "@/stores/orderList.js";
import {watch} from 'vue';

export default {
  name: 'SalesOrderList',
  props: {
    selectionMode: {
      type: Boolean,
      default: false
    },
    shopId: {
      type: [String, Number],
      default: null
    }
  },
  emits: ['order-selected', 'filter-by-shop'],
  setup() {
    const orderListStore = useOrderListStore();
    return {
      orderListStore
    };
  },
  data() {
    return {
      orders: [],
      loading: false,
      finished: false,
      pageIndex: 0,
      pageSize: 10,
      showActionSheet: false,
      selectedOrder: null,
      actions: [
        {name: '单个操作', subname: '针对当前选中订单的操作', disabled: true},
        {
          name: '打印订单',
          color: '#07c160',
          callback: () => this.handlePrintOrder()
        },
        {
          name: '复制订单',
          color: '#1989fa',
          callback: () => this.handleCopyOrder(this.selectedOrder)
        },
        {
          name: '删除订单',
          color: '#ee0a24',
          callback: () => this.handleDeleteOrder(this.selectedOrder.id)
        }
      ],
      selectedOrders: []
    }
  },
  mounted() {
    // 监听store中的选择模式变化
    watch(() => this.orderListStore.isSelectionMode, (newValue) => {
      if (!newValue) {
        this.clearSelection();
      }
    });

    
    this.resetAndLoad(); // 保留初始加载
  },
  
  methods: {
    openActionSheet(order) {
      this.selectedOrder = order;
      this.showActionSheet = true;
    },
    async handlePrintOrder() {
      try {
        await printOrder(this.selectedOrder, (status) => {
        });
      } catch (error) {
      }
    },
    async handleCopyOrder(order) {
      try {
        const receipt = formatReceipt(order, false);
        await Clipboard.write({
          string: receipt
        });
        showSuccessToast('订单小票已复制到剪切板');
      } catch (err) {
        showFailToast('复制失败，错误信息:' + err);
      }
    },
    handleDeleteOrder(id) {
      showConfirmDialog({
        title: '确认删除',
        message: '确定要删除这个订单吗？'
      }).then(() => {
        api.order.cancelOrder({orderId: id}).then(() => {
          showSuccessToast('删除成功')
          this.orders = this.orders.filter(item => item.id !== id);
        })
      })
    },
    async onLoad() {
      this.loading = true;
      try {
        const params = {
          shopId: this.shopId || this.orderListStore.filterParams.shopId,
          startDate: this.orderListStore.filterParams.startDate,
          endDate: this.orderListStore.filterParams.endDate,
          page: this.pageIndex,
          size: this.pageSize
        };
        const response = await api.order.getOrders(params);

        if (response && response.content && Array.isArray(response.content)) {
          this.orders.push(...response.content);
          this.pageIndex += 1;
          this.finished = !response.hasNext;
        } else {
          this.finished = true;
        }
      } catch (error) {
        showFailToast('获取订单失败');
      } finally {
        this.loading = false;
      }
    },
    resetAndLoad() {
      this.orders = [];
      this.pageIndex = 0;
      this.finished = false;
      this.loading = false; // Keep loading false, van-list should trigger load

      this.onLoad()
    },
    isOrderSelected(order) {
      return this.selectedOrders.some(item => item.id === order.id);
    },
    toggleOrderSelection(order, selected) {
      if (selected) {
        this.selectedOrders.push(order);
      } else {
        this.selectedOrders = this.selectedOrders.filter(item => item.id !== order.id);
      }

      this.$emit('order-selected', order, selected);
    },
    clearSelection() {
      this.selectedOrders = [];
    },
    // 确保 filterByShop 方法存在
    filterByShop(shop) {
      this.$emit('filter-by-shop', shop);
    }
  }
}
</script>

<style scoped>
.sales-order-list {
  width: 100%;
  height: 100%;
  background-color: transparent;
  padding: 12px;
  box-sizing: border-box;
}

.order-card {
  margin-bottom: 16px;
  border-radius: 12px;
  overflow: hidden;
  background-color: #fff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s, box-shadow 0.2s;
}

.order-card:active {
  transform: translateY(2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.order-header {
  background-color: #f8f9fa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.order-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.order-header-left {
  display: flex;
  flex-direction: column;
}

.order-id {
  font-weight: 600;
  font-size: 15px;
  color: #323233;
}

.order-time {
  font-size: 12px;
  color: #969799;
  margin-top: 4px;
}

.action-button {
  border-radius: 4px;
}

.shop-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.shop-name {
  font-weight: 600;
  color: #1989fa;
  font-size: 15px;
}

.shop-location {
  color: #969799;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.order-details {
  padding: 12px 16px;
  background-color: #fff;
  border-bottom: 1px solid #f2f3f5;
}

.order-detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed #f2f3f5;
}

.order-detail-item:last-child {
  margin-bottom: 0;
  border-bottom: none;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.product-name {
  color: #323233;
  font-weight: 500;
}

.quantity-price {
  display: flex;
  align-items: center;
  gap: 12px;
}

.quantity {
  color: #969799;
  background-color: #f2f3f5;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 12px;
}

.price {
  font-weight: 600;
  color: #323233;
}

.order-summary {
  padding: 12px 16px;
  background-color: #fafafa;
  display: flex;
  justify-content: flex-end;
  gap: 24px;
  border-top: 1px solid #f2f3f5;
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.summary-item .label {
  font-size: 12px;
  color: #969799;
  margin-bottom: 2px;
}

.summary-item .value {
  font-weight: 600;
  font-size: 16px;
}

/* 针对总价值 */
.total-amount {
  color: #ee0a24;
}

/* 针对利润 */
.profit-amount {
  color: #07c160;
}

/* 快捷操作按钮 */
.quick-actions {
  display: flex;
  justify-content: flex-end;
  padding: 12px 16px;
  gap: 8px;
  background-color: #fff;
  border-top: 1px solid #f5f5f5;
}

.quick-action-btn {
  border-radius: 4px;
  font-size: 12px;
}
</style>
