<template>
  <van-pull-refresh v-model="loading" @refresh="onRefresh">
    <div class="month-statistics">
      <div class="page-header">
        <h2 class="title">月度销售数据</h2>
        <span class="date-range">{{ startDateDisplay }} 至 {{ formatDate(new Date()) }}</span>
      </div>

      <!-- 数据卡片组 -->
      <div class="stats-cards">
        <van-empty v-if="!tableData || tableData.length === 0" description="暂无数据" />
        
        <div v-else v-for="(data, index) in tableData" :key="index" 
             :class="{ 'expanded': expandedCard === index }" 
             class="stats-card" 
             @click="toggleCard(index)">
          <div class="card-header">
            <h3 class="card-title">{{ data.monthName }}</h3>
            <span :class="['growth-tag', getGrowthClass(data.growthRate)]">
              {{ data.growthRate > 0 ? '+' : '' }}{{ (data.growthRate * 100).toFixed(2) }}%
            </span>
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
          <div v-show="expandedCard === index" class="details-section">
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
  name: "MonthData",
  data() {
    return {
      tableData: [],
      expandedCard: null,
      loading: false,
      startDate: "2024-02-01"
    }
  },
  computed: {
    startDateDisplay() {
      return this.startDate;
    }
  },
  mounted() {
    this.getData();
  },
  methods: {
    getGrowthClass(growth) {
      if (growth > 0) return 'growth-positive';
      if (growth < 0) return 'growth-negative';
      return 'growth-neutral';
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
    toggleCard(index) {
      if (this.expandedCard === index) {
        this.expandedCard = null;
      } else {
        this.expandedCard = index;
      }
    },
    async onRefresh() {
      await this.getData();
      setTimeout(() => {
        showToast("刷新成功");
        this.loading = false;
      }, 1000);
    },
    // 获取月份的第一天和最后一天
    getMonthBounds(year, month) {
      const firstDay = new Date(year, month - 1, 1);
      const lastDay = new Date(year, month, 0);
      return {
        firstDay,
        lastDay
      };
    },
    // 获取当前月份的名称
    getMonthName(year, month) {
      return `${year}年${month}月`;
    },
    async getData() {
      try {
        const startDateObj = new Date(this.startDate);
        const today = new Date();
        
        // 准备月份数据结构
        const monthsData = [];
        let currentYear = startDateObj.getFullYear();
        let currentMonth = startDateObj.getMonth() + 1;
        let endYear = today.getFullYear();
        let endMonth = today.getMonth() + 1;
        
        while (
          currentYear < endYear || 
          (currentYear === endYear && currentMonth <= endMonth)
        ) {
          const { firstDay, lastDay } = this.getMonthBounds(currentYear, currentMonth);
          
          // 如果是当前月份，使用今天作为结束日期
          const endDate = (currentYear === endYear && currentMonth === endMonth) 
            ? today 
            : lastDay;
          
          // 调用API获取该月数据
          const monthData = await api.statistics.getDateRangeStatistics({
            startDate: this.formatDate(firstDay),
            endDate: this.formatDate(endDate)
          });
          
          // 计算环比增长率
          let growthRate = 0;
          if (monthsData.length > 0) {
            const prevMonthProfit = monthsData[monthsData.length - 1].totalProfit;
            if (prevMonthProfit && prevMonthProfit > 0) {
              growthRate = (monthData.totalProfit - prevMonthProfit) / prevMonthProfit;
            }
          }
          
          // 添加月份名称和增长率
          monthsData.push({
            ...monthData,
            monthName: this.getMonthName(currentYear, currentMonth),
            growthRate: growthRate
          });
          
          // 移动到下个月
          if (currentMonth === 12) {
            currentYear++;
            currentMonth = 1;
          } else {
            currentMonth++;
          }
        }
        
        // 倒序排列，最近的月份在前面
        this.tableData = monthsData.reverse();
      } catch (error) {
        showToast("获取数据失败");
        console.error('获取月度统计数据失败:', error);
      }
    }
  }
}
</script>

<style scoped>
.month-statistics {
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

.growth-tag {
  font-size: 14px;
  padding: 2px 8px;
  border-radius: 4px;
}

.growth-positive {
  color: #07c160;
  background-color: rgba(7, 193, 96, 0.1);
}

.growth-negative {
  color: #ee0a24;
  background-color: rgba(238, 10, 36, 0.1);
}

.growth-neutral {
  color: #969799;
  background-color: #f2f3f5;
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