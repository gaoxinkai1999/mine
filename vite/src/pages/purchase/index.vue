<template>
  <div class="purchase-list-page">
    <van-nav-bar
        fixed
        safe-area-inset-top
        title="采购记录"
        right-text="新建采购单"
        @click-right="goToCreate"
    />

    <van-pull-refresh v-model="refreshing" class="content-container" @refresh="onRefresh">
      <van-list
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="onLoad"
      >
        <div v-for="(purchaseOrder, index) in purchaseList" :key="index" class="purchase-card">
          <div class="card-header">
            <div class="order-id">订单 #{{ purchaseOrder.id }}</div>
            <van-tag :type="purchaseOrder.state==='采购下单'?'warning':'success'" class="status-tag">
              {{ purchaseOrder.state }}
            </van-tag>
          </div>

          <div class="card-info">
            <div class="info-item">
              <van-icon name="clock-o"/>
              <span>创建: {{ purchaseOrder.createTime }}</span>
            </div>
            <div class="info-item">
              <van-icon name="checked"/>
              <span>完成: {{ purchaseOrder.inTime === null ? '未完成' : purchaseOrder.inTime }}</span>
            </div>
          </div>

          <div class="divider"></div>

          <div class="product-list">
            <div v-for="purchaseDetail in purchaseOrder.purchaseDetails" :key="purchaseDetail.id" class="product-item">
              <div class="product-info">
                <span class="product-name">{{ purchaseDetail.productName }}</span>
                <span class="product-quantity">x{{ purchaseDetail.quantity }}</span>
              </div>
              <span class="product-amount">¥{{ purchaseDetail.totalAmount }}</span>
            </div>
          </div>

          <div class="card-footer">
            <div class="total-amount">
              <span>总计</span>
              <span class="amount">¥{{ purchaseOrder.totalAmount }}</span>
            </div>

            <div class="action-buttons">
              <van-button v-if="purchaseOrder.state==='已下单'" size="small" type="primary"
                          @click="start(purchaseOrder.id)">
                入库
              </van-button>
              <van-button plain size="small" type="danger" @click="cancelPurchaseOrder(purchaseOrder.id)">
                取消
              </van-button>
            </div>
          </div>
        </div>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script>
import {showDialog, showToast} from "vant";
import api from "@/api";

export default {
  name: "StockInData",
  data() {
    return {
      purchaseList: [],
      loading: false,
      finished: false,
      refreshing: false,
      pageIndex: 0,
      pageSize: 10
    }
  },
  computed: {
    queryParams() {
      return {
        page: this.pageIndex,
        size: this.pageSize
      };
    }
  },
  methods: {
    onLoad() {
      api.purchase.getPurchaseList(this.queryParams).then(res => {
        const {content, empty, last} = res;

        if (this.refreshing) {
          this.purchaseList = content;
          this.refreshing = false;
        } else {
          this.purchaseList = [...this.purchaseList, ...content];
        }

        this.loading = false;

        if (empty || last) {
          this.finished = true;
        } else {
          this.pageIndex++;
        }

      }).catch(err => {
        console.error('加载采购列表失败', err);
        this.loading = false;
        this.refreshing = false;
        showToast('加载失败，请重试');
      });
    },

    onRefresh() {
      this.finished = false;
      this.pageIndex = 0;
      this.refreshing = true;
      this.onLoad();
    },
    goToCreate() {
      this.$router.push({name: 'purchase-new'});
    },

    cancelPurchaseOrder(purchaseId) {
      // 获取采购单状态
      const purchaseOrder = this.purchaseList.find(p => p.id === purchaseId);
      if (!purchaseOrder) return;
      
      // 根据不同状态显示不同的提示信息
      const message = purchaseOrder.state === '已下单' 
        ? '确定要取消该采购单吗？此操作将直接删除采购记录。' 
        : '确定要取消该采购单吗？此操作将回退库存并删除采购记录。';
      
      showDialog({
        title: '取消采购单',
        message,
        confirmButtonText: '确定取消',
        confirmButtonColor: '#ee0a24',
        showCancelButton: true
      }).then((action) => {
        if (action === 'confirm') {
          api.purchase.cancelPurchaseOrder({purchaseId}).then(() => {
            showToast('采购单已取消');
            this.onRefresh();
          }).catch(err => {
            console.error('取消采购单失败', err);
            showToast('取消失败，请重试');
          });
        }
      });
    },
    
    start(purchaseId) {
      // 跳转到采购入库页面
      this.$router.push({
        path: '/purchase/instock',
        query: { id: purchaseId }
      });
    },
  }
}
</script>

<style scoped>
.purchase-page-list {
}

.purchase-list-page {
  position: relative;
  height: 100vh;
  background-color: #f7f8fa;
  padding-top: calc(46px + env(safe-area-inset-top));
}

.content-container {
  overflow-y: auto;
  box-sizing: border-box;
  padding: 12px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
  height: calc(100vh - 46px - env(safe-area-inset-top));
}

/* 移除顶部安全区适配，van-nav-bar 已处理 */

/* 保留或添加底部安全区适配 (如果上面没加 calc) */
/* @supports (padding-bottom: env(safe-area-inset-bottom)) {
  .content-container {
     padding-bottom: env(safe-area-inset-bottom);
  }
} */

.purchase-card {
  background-color: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 16px;
  overflow: hidden;
}

.card-header {
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fcfcfc;
  border-bottom: 1px solid #eaeaea;
}

.order-id {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.status-tag {
  font-weight: 500;
}

.card-info {
  padding: 12px 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 150px;
  color: #666;
  font-size: 14px;
}

.info-item .van-icon {
  margin-right: 6px;
  color: #999;
}

.divider {
  height: 1px;
  background-color: #eaeaea;
  margin: 0 16px;
}

.product-list {
  padding: 12px 16px;
}

.product-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #eaeaea;
}

.product-item:last-child {
  border-bottom: none;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.product-name {
  font-weight: 500;
  color: #333;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-quantity {
  color: #666;
  background-color: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

.product-amount {
  color: #ff6b6b;
  font-weight: 500;
}

.card-footer {
  padding: 20px;
  background-color: #fafafa;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-top: 1px solid #eaeaea;
}

.total-amount {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 15px;
}

.amount {
  font-size: 18px;
  font-weight: 600;
  color: #ff6b6b;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

</style>
