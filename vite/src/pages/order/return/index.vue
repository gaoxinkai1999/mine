<template>
  <div class="return-order-list">
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
                <span class="order-time">{{ formatTime(item.createTime) }}</span>
              </div>
              <van-button size="small" type="danger" @click="openActionSheet(item)">
                操作
              </van-button>
            </div>
          </template>
        </van-cell>

        <!-- 商家信息 -->
        <van-cell class="shop-info" icon="shop-o" size="large">
          <template #title>
            <div class="shop-title">
              <span class="shop-name" style="cursor: pointer;" @click.stop="filterByShop(item.shop)">{{
                  item.shop.name
                }}</span>
              <van-tag v-if="item.shop.del" type="danger">弃用</van-tag>
            </div>
          </template>
          <template #label>
            <span class="shop-location">{{ item.shop.location }}</span>
          </template>
        </van-cell>

        <!-- 订单详情 -->
        <div class="order-details">
          <div v-for="(orderDetail, detailIndex) in item.returnOrderDetails"
               :key="detailIndex"
               class="order-detail-item"
          >
            <div class="product-info">
              <span class="product-name">{{ orderDetail.product.name }}</span>
              <van-tag :type="getReturnTypeTag(orderDetail.type)" size="mini">{{ orderDetail.type }}</van-tag>
            </div>
            <div class="quantity-price">
              <span v-if="orderDetail.type === '退货退款'" class="quantity">
                x{{ orderDetail.quantity || 1 }}
              </span>
              <span class="price">¥{{ orderDetail.amount }}</span>
            </div>
          </div>
        </div>

        <!-- 订单汇总 -->
        <div class="order-summary">
          <div class="summary-item">
            <span class="label">退款总额</span>
            <span class="value">¥{{ item.amount }}</span>
          </div>
        </div>
      </van-cell-group>
    </van-list>

    <!-- 操作面板 -->
    <van-action-sheet
        v-model:show="showActionSheet"
        :actions="actions"
        cancel-text="取消"
        close-on-click-action
    />
  </div>
</template>

<script>
import {Clipboard} from '@capacitor/clipboard';
import api from "@/api/index.js";
import {formatReturnReceipt, printReturnOrder} from "@/utils/printService.js";
import {showFailToast, showSuccessToast, showConfirmDialog} from "vant";
import {useOrderListStore} from "@/stores/orderList.js";
import {watch} from 'vue';

export default {
  name: 'ReturnOrderList',
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
          name: '打印退货单',
          color: '#07c160',
          callback: () => this.handlePrintOrder()
        },
        {
          name: '复制退货单',
          color: '#1989fa',
          callback: () => this.handleCopyOrder(this.selectedOrder)
        },
        {
          name: '删除退货单',
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
    formatTime(timestamp) {
      if (!timestamp) return '';
      const date = new Date(timestamp);
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
    },
    getReturnTypeTag(type) {
      return type === '仅退款' ? 'primary' : 'warning';
    },
    openActionSheet(order) {
      this.selectedOrder = order;
      this.showActionSheet = true;
    },
    async handlePrintOrder() {
      try {
        await printReturnOrder(this.selectedOrder, (status) => {
        });
      } catch (error) {
      }
    },
    async handleCopyOrder(order) {
      try {
        const receipt = formatReturnReceipt(order, false);
        await Clipboard.write({
          string: receipt
        });
        showSuccessToast('退货单已复制到剪切板');
      } catch (err) {
        showFailToast('复制失败，错误信息:' + err);
      }
    },
    handleDeleteOrder(id) {
      showConfirmDialog({
        title: '确认删除',
        message: '确定要删除这个退货单吗？'
      }).then(() => {
        api.returnorder.deleteReturnOrder({orderId: id}).then(() => {
          showSuccessToast('删除成功')
          this.orders = this.orders.filter(item => item.id !== id);
        })
      })
    },
    async onLoad() {
      this.loading = true;
      try {
        const response = await api.returnorder.getReturnOrders({
          shopId: this.orderListStore.filterParams.shopId,
          startDate: this.orderListStore.filterParams.startDate,
          endDate: this.orderListStore.filterParams.endDate,
          page: this.pageIndex,
          size: this.pageSize
        });

        if (response && response.content && Array.isArray(response.content)) {
          this.orders.push(...response.content);
          this.pageIndex += 1;
          this.finished = !response.hasNext;
        } else {
          this.finished = true;
        }
      } catch (error) {
        showFailToast('获取退货单失败');
      } finally {
        this.loading = false;
      }
    },
    resetAndLoad() {
      this.orders = [];
      this.pageIndex = 0;
      this.finished = false;
      this.loading = false;
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
    filterByShop(shop) {
      this.$emit('filter-by-shop', shop);
    }
  }
}
</script>

<style scoped>
.return-order-list {
  width: 100%;
  height: 100%;
  background-color: transparent;
  padding: 10px;
  box-sizing: border-box;
}

.order-card {
  margin-bottom: 12px;
  border-radius: 8px;
  overflow: hidden;
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.order-header {
  background-color: #f8f8f8;
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
  font-weight: bold;
  font-size: 14px;
}

.order-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.shop-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.shop-name {
  font-weight: bold;
  color: #323233;
}

.shop-location {
  color: #969799;
}

.order-details {
  padding: 12px 16px;
  background-color: #fff;
  border-bottom: 1px solid #f2f3f5;
}

.order-detail-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.order-detail-item:last-child {
  margin-bottom: 0;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.product-name {
  color: #323233;
}

.quantity-price {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quantity {
  color: #969799;
}

.price {
  font-weight: bold;
  color: #ee0a24;
}

.order-summary {
  padding: 12px 16px;
  background-color: #fff;
  display: flex;
  justify-content: flex-end;
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.summary-item .label {
  font-size: 12px;
  color: #969799;
}

.summary-item .value {
  font-weight: bold;
  font-size: 14px;
  color: #ee0a24;
}
</style>
