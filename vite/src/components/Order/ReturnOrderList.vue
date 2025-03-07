<template>
  <div class="return-order-list">
    <van-list v-model:loading="loading" :finished="finished" finished-text="没有更多了" @load="onLoad">
      <van-cell-group v-for="(item, index) in orders" :key="index" class="order-card">
        <!-- 订单头部 -->
        <van-cell class="order-header">
          <template #icon v-if="selectionMode">
            <van-checkbox 
              :model-value="isOrderSelected(item)" 
              @update:model-value="toggleOrderSelection(item, $event)"
              style="margin-right: 10px;"
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
              <span class="shop-name">{{ item.shop.name }}</span>
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
import api from "@/api";
import { showFailToast, showSuccessToast, showConfirmDialog } from "vant";
import { Clipboard } from '@capacitor/clipboard';
import { formatReturnReceipt, printReturnOrder } from "@/utils/printService";

export default {
  name: 'ReturnOrderList',
  props: {
    selectionMode: {
      type: Boolean,
      default: false
    }
  },
  emits: ['order-selected'],
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
        { name: '单个操作', subname: '针对当前选中退货单的操作', disabled: true },
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
      selectedOrders: [], // 多选模式下选中的订单
      // 筛选条件
      filterParams: {
        shopId: null,
        startDate: null,
        endDate: null
      }
    }
  },
  mounted() {
    // 从路由中获取筛选参数
    this.filterParams.shopId = this.$route.query.shopId || null;
    this.filterParams.startDate = this.$route.query.startDate || null;
    this.filterParams.endDate = this.$route.query.endDate || null;
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
          console.log(status); // 或者更新UI显示状态
        });
      } catch (error) {
        console.error('打印失败', error);
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
        console.error('复制失败:', err);
        showFailToast('复制失败，错误信息:'+err);
      }
    },
    handleDeleteOrder(id) {
      showConfirmDialog({
        title: '确认删除',
        message: '确定要删除这笔退货订单吗？'
      }).then(() => {
        api.returnorder.deleteReturnOrder(id).then(() => {
          this.orders = this.orders.filter(item => item.id !== id);
          showSuccessToast('删除成功');
        }).catch(err => {
          showFailToast('删除失败: ' + err.message);
        });
      }).catch(() => {
        // 取消删除
      });
    },
    async onLoad() {
      this.loading = true;
      try {
        // 使用分页API
        const response = await api.returnorder.getReturnOrders({
          shopId: this.filterParams.shopId,
          startDate: this.filterParams.startDate,
          endDate: this.filterParams.endDate,
          page: this.pageIndex,
          size: this.pageSize
        });
        
        if (response && response.content && Array.isArray(response.content)) {
          this.orders.push(...response.content);
          this.pageIndex += 1;
          this.finished = !response.hasNext;
        } else {
          console.error('获取退货订单数据格式错误:', response);
          this.finished = true;
        }
      } catch (error) {
        console.error('获取退货订单失败', error);
        showFailToast('获取退货订单失败');
      } finally {
        this.loading = false;
      }
    },
    
    // 判断订单是否被选中
    isOrderSelected(order) {
      return this.selectedOrders.some(item => item.id === order.id);
    },
    
    // 切换订单选中状态
    toggleOrderSelection(order, selected) {
      if (selected) {
        this.selectedOrders.push(order);
      } else {
        this.selectedOrders = this.selectedOrders.filter(item => item.id !== order.id);
      }
      
      // 触发事件通知父组件
      this.$emit('order-selected', order, selected);
    },
    
    // 清除选择状态
    clearSelection() {
      this.selectedOrders = [];
    }
  }
}
</script>

<style lang="scss" scoped>
.return-order-list {
  padding: 10px;
  background-color: #f7f8fa;

  .order-card {
    margin-bottom: 15px;
    border-radius: 8px;
    overflow: hidden;
    background-color: #ffffff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    transition: box-shadow 0.3s;

    &:hover {
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
    }

    .order-header {
      &-content {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 10px 0;
        width: 100%;

        .order-header-left {
          .order-id {
            font-size: 16px;
            font-weight: 600;
            color: #333;
          }

          .order-time {
            font-size: 12px;
            color: #888;
            margin-left: 10px;
          }
        }
      }
    }

    .shop-info {
      .shop-title {
        display: flex;
        align-items: center;
        gap: 8px;

        .shop-name {
          font-size: 15px;
          font-weight: 600;
        }
      }

      .shop-location {
        font-size: 13px;
        color: #666;
      }
    }

    .order-details {
      padding: 12px;
      background-color: #f8f8f8;

      .order-detail-item {
        display: flex;
        justify-content: space-between;
        margin-bottom: 8px;
        padding-bottom: 8px;
        border-bottom: 1px solid #eee;

        &:last-child {
          margin-bottom: 0;
          padding-bottom: 0;
          border-bottom: none;
        }

        .product-info {
          display: flex;
          align-items: center;
          gap: 8px;

          .product-name {
            font-size: 14px;
            color: #333;
          }
        }

        .quantity-price {
          display: flex;
          align-items: center;
          gap: 12px;

          .quantity {
            font-size: 13px;
            color: #666;
            background-color: #f5f5f5;
            padding: 2px 6px;
            border-radius: 4px;
          }

          .price {
            font-size: 14px;
            font-weight: 600;
            color: #ff6b6b;
          }
        }
      }
    }

    .order-summary {
      padding: 12px;
      border-top: 1px solid #eee;

      .summary-item {
        display: flex;
        justify-content: space-between;
        margin-bottom: 8px;

        &:last-child {
          margin-bottom: 0;
        }

        .label {
          font-size: 14px;
          color: #666;
        }

        .value {
          font-size: 14px;
          font-weight: 600;
          color: #ff6b6b;
        }
      }
    }
  }
}
</style>