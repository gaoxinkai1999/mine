<route>
{
  name: "mine"
}
</route>

<template>
  <van-pull-refresh v-model="loading" @refresh="onRefresh">
    <div class="statistics-dashboard">
      <!-- 数据卡片组 -->
      <div class="stats-groups">
        <!-- 今日数据 -->
        <div v-if="today" :class="{ 'expanded': expandedCard === 'today' }" class="stats-card"
             @click="toggleCard('today')">
          <div class="card-header">
            <h3 class="card-title">今日数据</h3>
            <span class="date-tag">{{ formatDate(new Date()) }}</span>
          </div>
          <div class="stats-grid">
            <div class="stat-item">
              <van-icon class="stat-icon" name="orders-o"/>
              <div class="stat-content">
                <div class="stat-label">订单数</div>
                <div class="stat-value">{{ formatNumber(today.orderCount) }}</div>
              </div>
            </div>
            <div class="stat-item">
              <van-icon class="stat-icon" name="gold-coin-o"/>
              <div class="stat-content">
                <div class="stat-label">销售额</div>
                <div class="stat-value">¥{{ today.totalSales }}</div>
              </div>
            </div>
            <div class="stat-item">
              <van-icon class="stat-icon" name="chart-trending-o"/>
              <div class="stat-content">
                <div class="stat-label">总利润</div>
                <div class="stat-value">¥{{ formatMoney(today.totalProfit) }}</div>
              </div>
            </div>
          </div>
          <!-- 详细信息展开部分 -->
          <div v-show="expandedCard === 'today'" class="details-section">
            <div class="details-header">
              <h4>商品销售详情</h4>
              <div class="total-cost">总成本: ¥{{ formatMoney(today.totalCost) }}</div>
            </div>
            <div class="product-table">
              <table>
                <thead>
                <tr>
                  <th>商品名称</th>
                  <th>销售数量</th>
                  <th>销售额</th>
                  <th>利润</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="product in today.productSalesInfoDTOS" :key="product.productId">
                  <td>{{ product.productName }}</td>
                  <td>{{ product.quantity }}</td>
                  <td>¥{{ formatMoney(product.totalSales) }}</td>
                  <td>¥{{ formatMoney(product.totalProfit) }}</td>
                </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- 昨日数据 -->
        <div v-if="yesterday" :class="{ 'expanded': expandedCard === 'yesterday' }" class="stats-card"
             @click="toggleCard('yesterday')">
          <div class="card-header">
            <h3 class="card-title">昨日数据</h3>
            <span class="date-tag">{{ formatDate(getYesterday()) }}</span>
          </div>
          <div class="stats-grid">
            <div class="stat-item">
              <van-icon class="stat-icon" name="orders-o"/>
              <div class="stat-content">
                <div class="stat-label">订单数</div>
                <div class="stat-value">{{ formatNumber(yesterday.orderCount) }}</div>
              </div>
            </div>
            <div class="stat-item">
              <van-icon class="stat-icon" name="gold-coin-o"/>
              <div class="stat-content">
                <div class="stat-label">销售额</div>
                <div class="stat-value">¥{{ formatMoney(yesterday.totalSales) }}</div>
              </div>
            </div>
            <div class="stat-item">
              <van-icon class="stat-icon" name="chart-trending-o"/>
              <div class="stat-content">
                <div class="stat-label">总利润</div>
                <div class="stat-value">¥{{ formatMoney(yesterday.totalProfit) }}</div>
              </div>
            </div>
          </div>
          <!-- 详细信息展开部分 -->
          <div v-show="expandedCard === 'yesterday'" class="details-section">
            <div class="details-header">
              <h4>商品销售详情</h4>
              <div class="total-cost">总成本: ¥{{ formatMoney(yesterday.totalCost) }}</div>
            </div>
            <div class="product-table">
              <table>
                <thead>
                <tr>
                  <th>商品名称</th>
                  <th>销售数量</th>
                  <th>销售额</th>
                  <th>利润</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="product in yesterday.productSalesInfoDTOS" :key="product.productId">
                  <td>{{ product.productName }}</td>
                  <td>{{ product.quantity }}</td>
                  <td>¥{{ formatMoney(product.totalSales) }}</td>
                  <td>¥{{ formatMoney(product.totalProfit) }}</td>
                </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- 当月数据 -->
        <div v-if="thisMonth" :class="{ 'expanded': expandedCard === 'month' }" class="stats-card"
             @click="toggleCard('month')">
          <div class="card-header">
            <h3 class="card-title">当月数据</h3>
            <span class="date-tag">{{ getCurrentMonth() }}</span>
          </div>
          <div class="stats-grid">
            <div class="stat-item">
              <van-icon class="stat-icon" name="orders-o"/>
              <div class="stat-content">
                <div class="stat-label">订单数</div>
                <div class="stat-value">{{ formatNumber(thisMonth.orderCount) }}</div>
              </div>
            </div>
            <div class="stat-item">
              <van-icon class="stat-icon" name="gold-coin-o"/>
              <div class="stat-content">
                <div class="stat-label">销售额</div>
                <div class="stat-value">¥{{ formatMoney(thisMonth.totalSales) }}</div>
              </div>
            </div>
            <div class="stat-item">
              <van-icon class="stat-icon" name="chart-trending-o"/>
              <div class="stat-content">
                <div class="stat-label">总利润</div>
                <div class="stat-value">¥{{ formatMoney(thisMonth.totalProfit) }}</div>
              </div>
            </div>
          </div>
          <!-- 详细信息展开部分 -->
          <div v-show="expandedCard === 'month'" class="details-section">
            <div class="details-header">
              <h4>商品销售详情</h4>
              <div class="total-cost">总成本: ¥{{ formatMoney(thisMonth.totalCost) }}</div>
            </div>
            <div class="product-table">
              <table>
                <thead>
                <tr>
                  <th>商品名称</th>
                  <th>销售数量</th>
                  <th>销售额</th>
                  <th>利润</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="product in thisMonth.productSalesInfoDTOS" :key="product.productId">
                  <td>{{ product.productName }}</td>
                  <td>{{ product.quantity }}</td>
                  <td>¥{{ formatMoney(product.totalSales) }}</td>
                  <td>¥{{ formatMoney(product.totalProfit) }}</td>
                </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  </van-pull-refresh>
</template>

<script>
import api from "@/api/index.js";
import {showToast} from "vant";

export default {
  name: "MyMine",
  data() {
    return {
      today: null,
      yesterday: null,
      thisMonth: null,
      expandedCard: null, // 控制展开的卡片
      loading: false
    }
  },

  methods: {
    async onRefresh() {
      await this.getToday();
      setTimeout(() => {
        showToast("刷新成功")
        this.loading = false
      }, 1000);

    },
    async getToday() {
      const today = new Date();
      // 使用本地时区格式化日期为YYYY-MM-DD
      const todayStr = today.getFullYear() + '-' +
          String(today.getMonth() + 1).padStart(2, '0') + '-' +
          String(today.getDate()).padStart(2, '0');

      const yesterday = new Date(today);
      yesterday.setDate(today.getDate() - 1);
      const yesterdayStr = yesterday.getFullYear() + '-' +
          String(yesterday.getMonth() + 1).padStart(2, '0') + '-' +
          String(yesterday.getDate()).padStart(2, '0');

      const firstDayOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
      const firstDayOfMonthStr = firstDayOfMonth.getFullYear() + '-' +
          String(firstDayOfMonth.getMonth() + 1).padStart(2, '0') + '-' +
          String(firstDayOfMonth.getDate()).padStart(2, '0');

      try {
        const [todayData, yesterdayData, monthData] = await Promise.all([
          api.statistics.getDateRangeStatistics({startDate: todayStr, endDate: todayStr}),
          api.statistics.getDateRangeStatistics({startDate: yesterdayStr, endDate: yesterdayStr}),
          api.statistics.getDateRangeStatistics({startDate: firstDayOfMonthStr, endDate: todayStr})
        ]);

        this.today = todayData;
        this.yesterday = yesterdayData;
        this.thisMonth = monthData;
      } catch (error) {
        console.error('获取统计数据失败:', error);
      }
    },

    formatNumber(num) {
      return new Intl.NumberFormat().format(num);
    },

    formatMoney(amount) {
      return new Intl.NumberFormat('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      }).format(amount);
    },

    formatDate(date) {
      return new Intl.DateTimeFormat('zh-CN', {
        month: 'long',
        day: 'numeric',
        weekday: 'long'
      }).format(date);
    },

    getYesterday() {
      const yesterday = new Date();
      yesterday.setDate(yesterday.getDate() - 1);
      return yesterday;
    },

    getCurrentMonth() {
      return new Intl.DateTimeFormat('zh-CN', {
        year: 'numeric',
        month: 'long'
      }).format(new Date());
    },

    toggleCard(cardType) {
      this.expandedCard = this.expandedCard === cardType ? null : cardType;
    }
  },
  mounted() {
    this.getToday();
  }
}
</script>

<style scoped>
.header {
  text-align: center;
  margin-bottom: 24px;
  padding: 20px 0;
  background-color: #ffffff;
  border-bottom: 1px solid #ebedf0;
}

.title {
  font-size: 28px;
  color: #303133;
  margin: 0;
  font-weight: 600;
  letter-spacing: 1px;
}

.version {
  font-size: 14px;
  color: #909399;
  margin: 8px 0 0;
}

.statistics-dashboard {
  padding: 16px;
  background-color: #f5f7fa;
  min-height: 100vh;
  position: relative;
  box-sizing: border-box;
  overflow-y: auto;
  padding-bottom: var(--van-tabbar-height);
}

@supports (padding-bottom: env(safe-area-inset-bottom)) {
  .statistics-dashboard {
    padding-bottom: calc(var(--van-tabbar-height) + env(safe-area-inset-bottom, 0px));
  }
}

.stats-groups {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 800px;
  margin: 0 auto;
  margin-bottom: 20px;
}

.stats-card {
  position: relative;
  background: #ffffff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #ebedf0;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
}

.stats-card.expanded {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #ffffff;
  border: 1px solid #f0f2f5;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-title {
  font-size: 16px;
  color: #2c3e50;
  margin: 0;
  font-weight: 600;
}

.date-tag {
  font-size: 13px;
  color: #909399;
  background-color: #f5f7fa;
  padding: 4px 8px;
  border-radius: 4px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.stat-icon {
  font-size: 24px;
  color: #409eff;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 18px;
  color: #303133;
  font-weight: 600;
}

.details-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebedf0;
}

.details-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.details-header h4 {
  margin: 0;
  font-size: 14px;
  color: #606266;
}

.total-cost {
  font-size: 14px;
  color: #606266;
}

.product-table {
  width: 100%;
  overflow-x: auto;
}

.product-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.product-table th,
.product-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #ebedf0;
}

.product-table th {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 600;
}

.product-table tr:hover td {
  background-color: #f5f7fa;
}

@media (max-width: 480px) {
  .statistics-dashboard {
    padding: 12px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .stat-item {
    padding: 10px;
  }

  .stat-value {
    font-size: 16px;
  }

  .product-table {
    font-size: 13px;
  }

  .product-table th,
  .product-table td {
    padding: 8px;
  }
}
</style>
