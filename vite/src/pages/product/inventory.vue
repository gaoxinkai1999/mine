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
                      <th>操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(batch, index) in row.children" :key="index">
                      <td>{{ batch.batchNumber || '无批次' }}</td>
                      <td>{{ formatDate(batch.productionDate) }}</td>
                      <td>{{ formatDate(batch.expirationDate) }}</td>
                      <td class="text-right">{{ batch.quantity }}</td>
                      <td class="text-right">{{ formatAmount(batch) }}</td>
                      <td>
                        <el-button type="primary" size="small" @click="openEditDialog(batch, row.id.split('-')[0])">
                          修改库存
                        </el-button>
                      </td>
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
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="openEditDialog(row)" 
              v-if="!row.children">
              修改库存
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Vant移动端库存修改弹窗 -->
    <van-dialog
      v-model:show="editDialogVisible"
      title="修改库存"
      :show-confirm-button="false"
      close-on-click-overlay
      :style="{ width: '90%', maxWidth: '500px' }"
    >
      <van-form @submit="submitInventoryUpdate">
        <van-cell-group inset>
          <van-field
            label="产品名称"
            :model-value="editForm.productName"
            readonly
            label-width="80px"
          />
          <van-field
            v-if="editForm.batchNumber !== undefined"
            label="批次号"
            :model-value="editForm.batchNumber"
            readonly
            label-width="80px"
          />
          <van-field
            name="quantity"
            label="库存数量"
            label-width="80px"
            required
          >
            <template #input>
              <van-stepper
                v-model="editForm.quantity"
                :min="0"
                integer
                input-width="70px"
                button-size="28px"
              />
            </template>
          </van-field>
        </van-cell-group>
        <div class="dialog-footer">
          <van-button round block type="primary" native-type="submit" class="submit-btn">确认修改</van-button>
          <van-button round block plain @click="editDialogVisible = false" class="cancel-btn">取消</van-button>
        </div>
      </van-form>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import api from "@/api/index.js";
import { ElMessage } from 'element-plus';
import { showToast } from 'vant';
import 'vant/lib/index.css';

const tableRef = ref(null);
const isExpandAll = ref(false);
const editDialogVisible = ref(false);
const editFormRef = ref(null);

// 编辑表单数据
const editForm = ref({
  id: null,
  productId: null,
  batchId: null,
  quantity: 0,
  productName: '',
  batchNumber: undefined
});

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
          amount: (product.costPrice || 0) * (batch.quantity || 0),
          batchId: batch.batchId
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

// 打开编辑对话框
const openEditDialog = (row, productId = null) => {
  // 如果是批次项
  if (row.batchId) {
    editForm.value = {
      productId: parseInt(productId),
      batchId: row.batchId,
      quantity: row.quantity,
      productName: tableData.value.find(p => p.id == productId)?.name || '',
      batchNumber: row.batchNumber || '无批次'
    };
  } else {
    // 非批次管理的产品
    editForm.value = {
      productId: parseInt(row.id),
      batchId: null,
      quantity: row.quantity,
      productName: row.name,
      batchNumber: undefined
    };
  }
  
  editDialogVisible.value = true;
};

// 提交库存更新
const submitInventoryUpdate = async () => {
  try {
    // 构建更新数据
    const updateData = {
      productId: editForm.value.productId,
      batchId: editForm.value.batchId,
      quantity: editForm.value.quantity
    };

    console.log('提交库存更新数据:', updateData)
    // 调用API更新库存
    await api.inventory.update(updateData);
    
    // 关闭对话框
    editDialogVisible.value = false;
    
    // 重新加载数据
    await getInventoryList();
    
    // 使用Vant的Toast替代ElementUI的Message
    showToast({
      message: '库存更新成功',
      type: 'success',
      position: 'top'
    });
  } catch (error) {
    console.error('更新库存失败:', error);
    showToast({
      message: '更新库存失败: ' + (error.message || '未知错误'),
      type: 'fail',
      position: 'top'
    });
  }
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

/* Vant弹窗样式 */
.dialog-footer {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.submit-btn {
  margin-bottom: 10px;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  :deep(.van-dialog) {
    width: 90% !important;
    max-width: none !important;
    border-radius: 12px;
  }
  
  :deep(.van-cell-group--inset) {
    margin: 0;
  }
  
  :deep(.van-field__label) {
    width: 80px !important;
  }
}
</style>