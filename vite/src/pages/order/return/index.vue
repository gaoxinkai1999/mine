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
              <van-button size="small" type="danger" class="action-button" plain icon="more-o" @click="openActionSheet(item)">
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
              <van-tag v-if="item.shop.del" type="danger" round>弃用</van-tag>
            </div>
          </template>
          <template #label>
            <span class="shop-location"><van-icon name="location-o" /> {{ item.shop.location }}</span>
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
              <van-tag :type="getReturnTypeTag(orderDetail.type)" size="mini" round>{{ orderDetail.type }}</van-tag>
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
            <span class="value refund-amount">¥{{ item.amount }}</span>
          </div>
        </div>

        <!-- 快捷操作按钮区域移除，操作统一放入 ActionSheet -->
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
    <!-- 修改欠款弹窗已移除 -->
  </div>
</template>

<script>
import {Clipboard} from '@capacitor/clipboard';
import api from "@/api/index.js";
import {formatReturnReceipt, printReturnOrder} from "@/utils/printService.js";
import {showFailToast, showSuccessToast, showConfirmDialog} from "vant"; // 移除 showLoadingToast
import {useOrderListStore} from "@/stores/orderList.js";
import {watch} from 'vue'; // 移除 nextTick

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
      // showArrearsPopup 和 arrearsEditData 已移除
      actions: [
        {name: '单个操作', subname: '针对当前选中订单的操作', disabled: true}, // 恢复原始标题
        {
          name: '打印退货单', // 恢复原始名称
          color: '#07c160',
          callback: () => this.handlePrintOrder()
        },
        {
          name: '复制退货单', // 恢复原始名称
          color: '#1989fa',
          callback: () => this.handleCopyOrder(this.selectedOrder)
        },
        {
          name: '删除退货单', // 恢复原始名称
          color: '#ee0a24',
          callback: () => this.handleDeleteOrder(this.selectedOrder.id)
        }
        // 移除商家操作和修改欠款项
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
      // 移除动态修改 ActionSheet 按钮状态的逻辑
      this.showActionSheet = true;
    },
    async handlePrintOrder() {
      if (!this.selectedOrder) { // 检查 selectedOrder
          showFailToast('无法确定要打印的退货单');
          return;
      }
      try {
        await printReturnOrder(this.selectedOrder, (status) => { // 使用 this.selectedOrder
        });
      } catch (error) {
        showFailToast('打印失败: ' + (error.message || '未知错误'));
      }
    },
    async handleCopyOrder(order) { // ActionSheet 回调会传入 selectedOrder
      if (!order) {
          showFailToast('无法确定要复制的退货单');
          return;
      }
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
       if (!id) {
          showFailToast('无法确定要删除的退货单');
          return;
      }
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
    // --- 移除修改欠款相关方法 openArrearsPopup 和 saveArrears ---
  }
}
</script>

<style scoped>
/* --- 移除复用的弹窗样式 --- */

.return-order-list {
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
  background-color: #fef0f0;
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
  color: #ee0a24;
}

.order-summary {
  padding: 12px 16px;
  background-color: #fafafa;
  display: flex;
  justify-content: flex-end;
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

.refund-amount {
  color: #ee0a24;
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
