  <route>
{
  name: "statistics-shop"
}
</route>

<template>
  <div class="custom-table-container">
    <div class="custom-table">
      <!-- 表头 -->
      <div class="table-header">
        <div class="header-cell index-cell">#</div>
        <div class="header-cell shop-cell">店铺</div>
        <div class="header-cell value-cell sortable" @click="sortBy('totalSales')">
          总销售额
          <span class="sort-icon" :class="getSortIconClass('totalSales')"></span>
        </div>
        <div class="header-cell value-cell sortable" @click="sortBy('totalProfit')">
          总利润
          <span class="sort-icon" :class="getSortIconClass('totalProfit')"></span>
        </div>
        <div class="header-cell value-cell sortable" @click="sortBy('averageMonthlyProfit')">
          月平均利润
          <span class="sort-icon" :class="getSortIconClass('averageMonthlyProfit')"></span>
        </div>
        <div class="header-cell expand-cell"></div>
      </div>

      <div class="table-body">
        <div v-for="(row, index) in sortedShopData" :key="index">
          <!-- 商家主行 -->
          <div
            class="table-row"
            :class="getRowClass(row)"
          >
            <div class="table-cell index-cell">{{ index + 1 }}</div>
            <div class="table-cell shop-cell">
              <span class="shop-link" @click="handleClick(row.shopId)">{{ row.shopName }}</span>
            </div>
            <div class="table-cell value-cell">{{ formatNumber(row.totalSales) }}</div>
            <div class="table-cell value-cell">{{ formatNumber(row.totalProfit) }}</div>
            <div class="table-cell value-cell">{{ formatNumber(row.averageMonthlyProfit) }}</div>
            <div class="table-cell expand-cell">
              <button class="expand-btn" @click="row.expanded = !row.expanded">
                {{ row.expanded ? '收起' : '展开' }}
              </button>
            </div>
          </div>

          <!-- 商品明细嵌套表 -->
          <div v-if="row.expanded" class="product-detail">
            <div class="product-header">
              <div class="product-cell">商品名称</div>
              <div class="product-cell">平均月销量</div>
              <div class="product-cell">平均月销售额</div>
              <div class="product-cell">平均月利润</div>
            </div>
            <div
              v-for="(p, pi) in row.productMonthlySalesList"
              :key="pi"
              class="product-row"
            >
              <div class="product-cell">{{ p.productName }}</div>
              <div class="product-cell">{{ p.quantity }}</div>
              <div class="product-cell">{{ formatNumber(p.totalSales) }}</div>
              <div class="product-cell">{{ formatNumber(p.totalProfit) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 无数据提示 -->
      <div class="no-data" v-if="shopDataList.length === 0 && !loading">
        <p>暂无店铺数据</p>
      </div>
    </div>

    <!-- 加载指示器 -->
    <div class="loading-overlay" v-if="loading">
      <div class="loading-spinner"></div>
    </div>
  </div>
</template>

<script>
import api from "@/api";
import {ROUTE_NAMES} from "@/constants/routeNames.js";

export default {
  name: "ShopData",
  data() {
    return {
      loading: true,
      shopDataList: [],
      sortKey: 'totalSales', // 默认按总销售额排序
      sortOrder: 'desc' // 默认降序
    }
  },
  computed: {
    sortedShopData() {
      if (!this.sortKey || !this.shopDataList.length) return this.shopDataList;
      
      const data = [...this.shopDataList];
      return data.sort((a, b) => {
        const aValue = parseFloat(a[this.sortKey]) || 0;
        const bValue = parseFloat(b[this.sortKey]) || 0;
        
        if (this.sortOrder === 'asc') {
          return aValue - bValue;
        } else {
          return bValue - aValue;
        }
      });
    }
  },
  mounted() {
    this.getShopDataList();
  },
  methods: {
    async getShopDataList() {
      try {
        this.shopDataList = await api.statistics.getShopStatistics();
      } catch (error) {
        console.error('获取店铺数据失败', error);
      } finally {
        this.loading = false;
      }
    },
    
    formatNumber(value) {
      if (!value && value !== 0) return '0';
      // 将数字格式化为带千位分隔符的形式
      return new Intl.NumberFormat('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      }).format(value);
    },
    
    sortBy(key) {
      if (this.sortKey === key) {
        // 如果已经在按这个键排序，则切换排序方向
        this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc';
      } else {
        // 否则更改排序键并设置为降序（通常用户想先看大的数值）
        this.sortKey = key;
        this.sortOrder = 'desc';
      }
    },
    
    getSortIconClass(key) {
      if (this.sortKey !== key) return 'unsorted';
      return this.sortOrder === 'asc' ? 'sort-asc' : 'sort-desc';
    },
    
    getRowClass(row) {
      if (row.days_since_last_order >= 15) {
        return 'warning-row';
      }
      return '';
    },
    
    handleClick(id) {
      this.$router.push({
        name: ROUTE_NAMES.SHOP_DETAIL,
        query: {
          id: id
        }
      });
    }
  }
}
</script>

<style scoped>
.custom-table-container {
  position: relative;
  margin: 20px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  background-color: #fff;
}

.custom-table {
  width: 100%;
  border-collapse: collapse;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  color: #333;
}

/* 表头样式 */
.table-header {
  display: flex;
  background-color: #f9fafc;
  border-bottom: 1px solid #ebeef5;
}

.header-cell {
  padding: 16px 12px;
  font-weight: 600;
  color: #606266;
  font-size: 14px;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
}

.expand-cell {
  flex: 0 0 70px;
}

.expand-btn {
  padding: 4px 10px;
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  border: 1px solid #b3d8ff;
  border-radius: 4px;
  cursor: pointer;
}

.expand-btn:hover {
  background: #d9ecff;
}

/* 排序箭头 */
.sortable {
  cursor: pointer;
  transition: background-color 0.2s;
  user-select: none;
}

.sortable:hover {
  background-color: #f0f2f5;
}

.sort-icon {
  margin-left: 6px;
  font-style: normal;
  display: inline-block;
  width: 16px;
  height: 16px;
  position: relative;
}

.sort-icon.sort-asc:after {
  content: '↑';
  color: #409eff;
}

.sort-icon.sort-desc:after {
  content: '↓';
  color: #409eff;
}

.sort-icon.unsorted:after {
  content: '↕';
  color: #c0c4cc;
  opacity: 0.5;
}

/* 表格内容样式 */
.table-body {
  max-height: calc(100vh - 150px);
  overflow-y: auto;
}

.table-row {
  display: flex;
  border-bottom: 1px solid #ebeef5;
  transition: background-color 0.2s;
}

.table-row:hover {
  background-color: #f5f7fa;
}
.warning-row {
  background-color: rgba(245, 108, 108, 0.1);
}

/* 列宽控制，保证表头和行对齐 */
.index-cell {
  flex: 0 0 50px;
}

.shop-cell {
  flex: 2;
  justify-content: flex-start;
  text-align: left;
}

.value-cell {
  flex: 1;
}

.expand-cell {
  flex: 0 0 70px;
}

.warning-row:hover {
  background-color: rgba(245, 108, 108, 0.15);
}

.table-cell {
  padding: 14px 12px;
  font-size: 14px;
  text-align: center;
}

/* 特殊列样式 */
.index-cell {
  flex: 0 0 50px;
}

.shop-cell {
  flex: 2;
  text-align: left;
  justify-content: flex-start;
}

.shop-link {
  color: #474cb6;
  cursor: pointer;
  font-weight: 500;
  transition: color 0.2s;
}

.shop-link:hover {
  color: #6366f1;
  text-decoration: underline;
}

/* 商品明细嵌套表 */
.product-detail {
  background: #fafbfc;
  padding: 10px 20px;
  border-bottom: 1px solid #ebeef5;
}

.product-header,
.product-row {
  display: flex;
}

.product-header {
  border-bottom: 1px solid #dfe4ed;
  font-weight: bold;
  background: #f3f4f7;
}

.product-cell {
  flex: 1;
  padding: 8px 6px;
  font-size: 13px;
  text-align: center;
}

/* 无数据状态 */
.no-data {
  padding: 40px 0;
  text-align: center;
  color: #909399;
  background-color: #fff;
  font-size: 14px;
}

/* 加载状态 */
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(64, 158, 255, 0.2);
  border-radius: 50%;
  border-top-color: #409eff;
  animation: spin 1s ease-in-out infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .custom-table-container {
    margin: 10px;
  }
  
  .header-cell, .table-cell {
    padding: 12px 8px;
    font-size: 13px;
  }
  
  .shop-cell {
    flex: 1.5;
  }
}
</style>
