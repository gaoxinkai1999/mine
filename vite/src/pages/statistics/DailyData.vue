<route>
{
  name: "statistics-daily"
}
</route>

<template>
  <van-pull-refresh v-model="loading" @refresh="onRefresh">
    <div class="daily-statistics">
      <div class="page-header">
        <h2 class="title">30天销售数据</h2>
        <span class="date-range">{{ formatDate(getThirtyDaysAgo()) }} 至 {{ formatDate(new Date()) }}</span>
      </div>

      <!-- 数据卡片组 -->
      <div class="stats-cards">
        <van-empty v-if="!tableData || Object.keys(tableData).length === 0" description="暂无数据" />
        
        <div v-else v-for="(data, date) in sortedTableData" :key="date" 
             :class="{ 'expanded': expandedCard === date }" 
             class="stats-card" 
             @click="toggleCard(date)">
          <div class="card-header">
            <h3 class="card-title">{{ formatDateDisplay(date) }}</h3>
            <span class="date-tag">{{ formatDate(new Date(date)) }}</span>
          </div>
          
          <div class="stats-grid">
            <div class="stat-item">
              <van-icon class="stat-icon" name="orders-o"/>
              <div class="stat-content">
                <div class="stat-label">订单数</div>
                <div class="stat-value">{{ formatNumber(data.orderCount) }}</div>
              </div>
            </div>
            <div class="stat-item">
              <van-icon class="stat-icon" name="gold-coin-o"/>
              <div class="stat-content">
                <div class="stat-label">销售额</div>
                <div class="stat-value">¥{{ formatMoney(data.totalSales) }}</div>
              </div>
            </div>
            <div class="stat-item">
              <van-icon class="stat-icon" name="chart-trending-o"/>
              <div class="stat-content">
                <div class="stat-label">总利润</div>
                <div class="stat-value">¥{{ formatMoney(data.totalProfit) }}</div>
              </div>
            </div>
          </div>
          
          <!-- 详细信息展开部分 -->
          <div v-show="expandedCard === date" class="details-section">
            <div class="details-header">
              <h4>商品销售详情</h4>
              <div class="total-cost">总成本: ¥{{ formatMoney(data.totalCost) }}</div>
            </div>
            <div class="product-table">
              <table v-if="data.productSalesInfoDTOS && data.productSalesInfoDTOS.length > 0">
                <thead>
                <tr>
                  <th>商品名称</th>
                  <th>销售数量</th>
                  <th>销售额</th>
                  <th>利润</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="product in data.productSalesInfoDTOS" :key="product.productId">
                  <td>{{ product.productName }}</td>
                  <td>{{ product.quantity }}</td>
                  <td>¥{{ formatMoney(product.totalSales) }}</td>
                  <td>¥{{ formatMoney(product.totalProfit) }}</td>
                </tr>
                </tbody>
              </table>
              <van-empty v-else description="暂无商品销售数据" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </van-pull-refresh>
</template>

<script>
import api from "@/api/index.js";
import { showToast } from "vant";

export default {
  name: "DailyData",
  data() {
    return {
      tableData: [],
      expandedCard: null,
      loading: false
    }
  },
  computed: {
    sortedTableData() {
      // 按日期降序排序（最新日期在前）
      const dates = Object.keys(this.tableData).sort((a, b) => new Date(b) - new Date(a));
      const sorted = {};
      dates.forEach(date => {
        sorted[date] = this.tableData[date];
      });
      return sorted;
    }
  },
  mounted() {
    this.getData()
  },
  methods: {
    formatDateDisplay(dateStr) {
      const date = new Date(dateStr);
      const today = new Date();
      const yesterday = new Date(today);
      yesterday.setDate(today.getDate() - 1);
      
      if (date.toDateString() === today.toDateString()) {
        return "今日数据";
      } else if (date.toDateString() === yesterday.toDateString()) {
        return "昨日数据";
      } else {
        return this.formatDate(date);
      }
    },
    getThirtyDaysAgo() {
      const date = new Date();
      date.setDate(date.getDate() - 30);
      return date;
    },
    formatDate(date) {
      return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
      }).replace(/\//g, '-');
    },
    formatNumber(num) {
      return new Intl.NumberFormat().format(num || 0);
    },
    formatMoney(amount) {
      return new Intl.NumberFormat('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      }).format(amount || 0);
    },
    toggleCard(date) {
      if (this.expandedCard === date) {
        this.expandedCard = null;
      } else {
        this.expandedCard = date;
      }
    },
    async onRefresh() {
      await this.getData();
      setTimeout(() => {
        showToast("刷新成功");
        this.loading = false;
      }, 1000);
    },
    async getData() {
      const today = new Date();
      const pastDate = new Date();
      pastDate.setDate(today.getDate() - 30);

      const formatDate = (date) => {
        return date.toLocaleDateString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit'
        }).replace(/\//g, '-');
      };

      console.log("今天日期:", formatDate(today));
      console.log("30 天前日期:", formatDate(pastDate));
      try {
        this.tableData = await api.statistics.getDailyStatistics({startDate: formatDate(pastDate), endDate: formatDate(today)});
      } catch (error) {
        showToast("获取数据失败");
        console.error('获取统计数据失败:', error);
      }
    },
  }
}
</script>


<style scoped>
.daily-statistics {
  padding: 16px;
  background-color: #f7f8fa;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 16px;
  text-align: center;
}

.title {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 8px;
  color: #323233;
}

.date-range {
  font-size: 14px;
  color: #969799;
}

.stats-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-card {
  background-color: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(100, 101, 102, 0.08);
  transition: all 0.3s ease;
}

.stats-card.expanded {
  box-shadow: 0 4px 16px rgba(100, 101, 102, 0.12);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: bold;
  color: #323233;
  margin: 0;
}

.date-tag {
  font-size: 14px;
  color: #969799;
  background-color: #f2f3f5;
  padding: 2px 8px;
  border-radius: 4px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
}

.stat-icon {
  font-size: 24px;
  color: #1989fa;
  margin-right: 12px;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 12px;
  color: #969799;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 16px;
  font-weight: bold;
  color: #323233;
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
  font-size: 14px;
  color: #323233;
  margin: 0;
}

.total-cost {
  font-size: 14px;
  color: #969799;
}

.product-table {
  width: 100%;
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  text-align: left;
  padding: 8px;
  border-bottom: 1px solid #ebedf0;
}

th {
  font-size: 12px;
  font-weight: normal;
  color: #969799;
}

td {
  font-size: 14px;
  color: #323233;
}
</style>