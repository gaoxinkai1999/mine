<template>
  <div class="inventory-container">
    <div class="table-operations">
      <el-button size="small" @click="toggleExpand">
        {{ isExpandAll ? '收起全部' : '展开全部' }}
      </el-button>
    </div>
    <div class="table-wrapper">
      <el-table
          ref="tableRef"
          :data="processedTableData"
          :summary-method="getSummaries"
          row-key="id"
          show-summary
          border
          size="small"
          @expand-change="handleExpandChange"
          :row-class-name="getRowClassName"
      >
        <el-table-column type="expand" width="50">
          <template #default="{ row }">
            <div v-if="row.children && row.batchManaged" class="expand-content">
              <div class="batch-table-container">
                <table class="batch-table">
                  <thead>
                    <tr>
                      <th>批次号</th>
                      <th>生产日期</th>
                      <th>过期日期</th>
                      <th>库存数量</th>
                      <th>库存金额</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(batch, index) in row.children" :key="index">
                      <td>{{ batch.batchNumber || '无批次' }}</td>
                      <td>{{ formatDate(batch.productionDate) }}</td>
                      <td>{{ formatDate(batch.expirationDate) }}</td>
                      <td class="text-right">{{ batch.quantity }}</td>
                      <td class="text-right">{{ formatAmount(batch) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
            <div v-else class="no-batch-info">
              该产品不进行批次管理
            </div>
          </template>
        </el-table-column>
        <el-table-column label="产品名称" prop="name" min-width="120">
          <template #default="{ row }">
            {{ row.name }}
            <template v-if="row.children && row.batchManaged">
              <span class="total-count">(共{{ row.batchCount }}个批次)</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="库存数量" prop="quantity" align="right" min-width="80"/>
        <el-table-column label="库存金额" align="right" min-width="90">
          <template #default="{ row }">
            {{ formatAmount(row) }}
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import api from "@/api/index.js";

const tableRef = ref(null);
const isExpandAll = ref(false);

// 表格数据
const tableData = ref([]);

// 格式化日期
const formatDate = (date) => {
  if (!date) return '-';
  return new Date(date).toLocaleDateString('zh-CN');
};

// 处理数据为树形结构
const processedTableData = computed(() => {
  const result = [];

  // 处理每个产品
  tableData.value.forEach(product => {
    if (!product || !product.productStockDTO) return;
    
    const stockInfo = product.productStockDTO;
    const isBatchManaged = product.batchManaged === true;
    const hasBatches = stockInfo.batchStocks && stockInfo.batchStocks.length > 0;
    
    // 批次管理且有批次信息，多于1个批次或明确设置为批次管理
    if (isBatchManaged && hasBatches) {
      // 添加产品总计行
      result.push({
        id: `${product.id}-total`,
        name: product.name,
        batchManaged: isBatchManaged,
        batchCount: stockInfo.batchStocks.length,
        quantity: stockInfo.totalInventory || 0,
        amount: (product.costPrice || 0) * (stockInfo.totalInventory || 0),
        children: stockInfo.batchStocks.map(batch => ({
          id: `${product.id}-batch-${batch.batchId}`,
          name: '',
          batchNumber: batch.batchNumber,
          productionDate: batch.productionDate,
          expirationDate: batch.expirationDate,
          quantity: batch.quantity || 0,
          amount: (product.costPrice || 0) * (batch.quantity || 0)
        }))
      });
    } else {
      // 非批次管理或没有批次数据
      result.push({
        id: product.id,
        name: product.name,
        batchManaged: isBatchManaged,
        quantity: stockInfo.totalInventory || 0,
        amount: (product.costPrice || 0) * (stockInfo.totalInventory || 0)
      });
    }
  });

  return result;
});

// 格式化金额
const formatAmount = (row) => {
  return row.amount.toFixed(2);
};

// 计算汇总行
const getSummaries = (param) => {
  const { columns, data } = param;
  const sums = [];

  columns.forEach((column, index) => {
    if (index === 0) {
      sums[index] = '总计';
      return;
    }

    // 批次信息列，跳过汇总
    if (index === 0 || !column.property && !column.label) {
      sums[index] = '';
      return;
    }

    // 根据列的 property 或 label 来判断
    const prop = column.property;
    const label = column.label;
    
    if (prop === 'quantity' || label === '库存数量') {
      const quantityTotal = data.reduce((sum, row) => {
        // 只计算无子项的行或合计行
        if (!row.children) {
          return sum + (Number(row.quantity) || 0);
        }
        return sum;
      }, 0);
      sums[index] = quantityTotal;
    } else if (label === '库存金额') {
      const amountTotal = data.reduce((sum, row) => {
        // 只计算无子项的行或合计行
        if (!row.children) {
          return sum + (Number(row.amount) || 0);
        }
        return sum;
      }, 0);
      sums[index] = amountTotal.toFixed(2);
    } else {
      sums[index] = '';
    }
  });

  return sums;
};

// 获取数据
const getInventoryList = async () => {
  try {
    tableData.value = await api.product.getProducts();
  } catch (error) {
    console.error('加载库存数据失败:', error);
    tableData.value = [];
  }
};

// 处理展开行事件
const handleExpandChange = (row, expanded) => {
  // 如果不是批次管理的商品，立即收起
  if (!row.batchManaged && expanded) {
    tableRef.value.toggleRowExpansion(row, false);
  }
};

// 展开/收起控制
const toggleExpand = () => {
  isExpandAll.value = !isExpandAll.value;
  processedTableData.value.forEach(row => {
    if (row.children && row.batchManaged) {
      tableRef.value.toggleRowExpansion(row, isExpandAll.value);
    }
  });
};

// 定义行的类名
const getRowClassName = ({ row }) => {
  return row.batchManaged ? 'row-batch-managed' : 'row-no-batch';
};

// 初始化
onMounted(() => {
  getInventoryList();
});
</script>

<style scoped>
.inventory-container {
  padding: 12px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  overflow-x: hidden;
}

.table-operations {
  margin-bottom: 12px;
  display: flex;
  flex-wrap: wrap;
}

.table-wrapper {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.expand-content {
  padding: 8px;
  width: 100%;
}

/* 批次表格样式 */
.batch-table-container {
  width: 100%;
  overflow-x: auto;
}

.batch-table {
  width: 100%;
  border-collapse: collapse;
  border: 1px solid #EBEEF5;
}

.batch-table th,
.batch-table td {
  padding: 8px 12px;
  border: 1px solid #EBEEF5;
  text-align: left;
  font-size: 13px;
}

.batch-table th {
  background-color: #F5F7FA;
  color: #606266;
  font-weight: 500;
}

.batch-table .text-right {
  text-align: right;
}

.total-count {
  color: #909399;
  font-size: 12px;
  margin-left: 4px;
}

.no-batch-info {
  color: #909399;
  font-style: italic;
  padding: 8px;
  text-align: center;
}

/* 表格布局 */
:deep(.el-table) {
  width: 100% !important;
  table-layout: auto !important;
}

:deep(.el-table__body),
:deep(.el-table__header) {
  width: 100% !important;
}

/* 单元格样式 */
:deep(.el-table .cell) {
  padding: 5px !important;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.5;
}

/* 表头样式 */
:deep(.el-table th) {
  padding: 6px 0;
  font-size: 13px;
  background-color: #f5f7fa;
  word-break: break-word;
  white-space: normal;
  height: auto;
}

/* 展开行样式 */
:deep(.el-table__expanded-cell) {
  padding: 8px !important;
}

/* 非批次管理行隐藏展开图标 */
:deep(.row-no-batch .el-table__expand-icon) {
  visibility: hidden;
}

/* 表格底部固定宽度 */
:deep(.el-table__footer-wrapper),
:deep(.el-table__body-wrapper) {
  width: 100% !important;
}

/* 响应式调整 */
@media screen and (max-width: 576px) {
  .inventory-container {
    padding: 8px 4px;
  }
  
  :deep(.el-table .cell) {
    padding: 4px !important;
    font-size: 12px;
  }
  
  .batch-table th,
  .batch-table td {
    padding: 6px 8px;
    font-size: 12px;
  }

  .no-batch-info {
    font-size: 12px;
    padding: 4px;
  }
  
  /* 固定表头 */
  :deep(.el-table__header-wrapper) {
    position: sticky;
    top: 0;
    z-index: 1;
  }
  
  /* 调整行高 */
  :deep(.el-table__row) {
    height: auto !important;
  }
}

/* 滚动条样式 */
.table-wrapper::-webkit-scrollbar,
.batch-table-container::-webkit-scrollbar {
  height: 6px;
  width: 6px;
}

.table-wrapper::-webkit-scrollbar-thumb,
.batch-table-container::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.table-wrapper::-webkit-scrollbar-track,
.batch-table-container::-webkit-scrollbar-track {
  background: #f5f7fa;
}
</style>