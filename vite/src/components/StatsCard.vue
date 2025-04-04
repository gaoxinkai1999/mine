<template>
  <div :class="{ 'expanded': expanded }" class="stats-card" @click="toggleExpand">
    <div class="card-header">
      <h3 class="card-title">{{ title }}</h3>
      <span v-if="tag" :class="['tag', tagClass]">{{ tag }}</span>
    </div>
    
    <div class="stats-grid">
      <div class="stat-item">
        <van-icon class="stat-icon" name="orders-o"/>
        <div class="stat-content">
          <div class="stat-label">订单数</div>
          <div class="stat-value">{{ formatNumber(stats.orderCount) }}</div>
        </div>
      </div>
      <div class="stat-item">
        <van-icon class="stat-icon" name="gold-coin-o"/>
        <div class="stat-content">
          <div class="stat-label">销售额</div>
          <div class="stat-value">¥{{ formatMoney(stats.totalSales) }}</div>
        </div>
      </div>
      <div class="stat-item">
        <van-icon class="stat-icon" name="chart-trending-o"/>
        <div class="stat-content">
          <div class="stat-label">总利润</div>
          <div class="stat-value">¥{{ formatMoney(stats.totalProfit) }}</div>
        </div>
      </div>
    </div>
    
    <div v-show="expanded" class="details-section">
      <div class="details-header">
        <h4>商品销售详情</h4>
        <div class="total-cost">总成本: ¥{{ formatMoney(stats.totalCost) }}</div>
      </div>
      <div class="product-table">
        <table v-if="stats.productSalesInfoDTOS &amp;&amp; stats.productSalesInfoDTOS.length > 0">
          <thead>
            <tr>
              <th>商品名称</th>
              <th @click.stop="sortBy('quantity')" class="sortable-header">
                销售数量
                <van-icon v-if="sortKey === 'quantity'" :name="sortAscending ? 'arrow-up' : 'arrow-down'" />
              </th>
              <th @click.stop="sortBy('totalSales')" class="sortable-header">
                销售额
                <van-icon v-if="sortKey === 'totalSales'" :name="sortAscending ? 'arrow-up' : 'arrow-down'" />
              </th>
              <th @click.stop="sortBy('totalProfit')" class="sortable-header">
                利润
                <van-icon v-if="sortKey === 'totalProfit'" :name="sortAscending ? 'arrow-up' : 'arrow-down'" />
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="product in sortedProductSales" :key="product.productId">
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
</template>

<script>
export default {
  name: "StatsCard",
  props: {
    title: {
      type: String,
      required: true
    },
    tag: {
      type: String,
      default: ""
    },
    tagClass: {
      type: String,
      default: ""
    },
    stats: {
      type: Object,
      required: true
    },
    expanded: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      sortKey: null, // 'quantity', 'totalSales', 'totalProfit'
      sortAscending: true,
    };
  },
  computed: {
    sortedProductSales() {
      if (!this.stats.productSalesInfoDTOS) {
        return [];
      }
      if (!this.sortKey) {
        return this.stats.productSalesInfoDTOS; // No sort applied
      }
      
      // Create a shallow copy to avoid mutating the original prop array
      const sortedArray = [...this.stats.productSalesInfoDTOS];
      
      sortedArray.sort((a, b) => {
        const valA = a[this.sortKey];
        const valB = b[this.sortKey];
        
        if (valA < valB) {
          return this.sortAscending ? -1 : 1;
        }
        if (valA > valB) {
          return this.sortAscending ? 1 : -1;
        }
        return 0;
      });
      
      return sortedArray;
    }
  },
  methods: {
    sortBy(key) {
      if (this.sortKey === key) {
        // If clicking the same key, reverse the direction
        this.sortAscending = !this.sortAscending;
      } else {
        // If clicking a new key, set it and default to ascending
        this.sortKey = key;
        this.sortAscending = true;
      }
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
    toggleExpand() {
      this.$emit('toggle-expand');
    }
  }
}
</script>

<style scoped>
.stats-card {
  background-color: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(100, 101, 102, 0.08);
  transition: all 0.3s ease;
  cursor: pointer;
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

.tag {
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

.sortable-header {
  cursor: pointer;
}

.sortable-header .van-icon {
  margin-left: 4px;
  vertical-align: middle;
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