<template>
  <div class="inventory-container">
    <!-- 顶部导航栏 -->
    <inventory-header
      :is-expand-all="isExpandAll"
      @toggle-expand="toggleExpand"
    />
    
    <!-- 库存总计卡片 -->
    <inventory-stats
      :total-quantity="getTotalQuantity()"
      :total-amount="getTotalAmount()"
    />
    
    <!-- 搜索过滤 -->
    <inventory-search
      v-model:search-query="searchQuery"
      @search="handleSearch"
    />
    
    <!-- 产品列表 -->
    <inventory-list
      :products="filteredProducts"
      :expanded-rows="expandedRows"
      @toggle-row="toggleRow"
      @edit-inventory="openEditDialog"
      @edit-batch-info="openBatchInfoEditDialog"
    />

    <!-- 修改库存弹窗 -->
    <inventory-edit-dialog
      v-model:visible="editDialogVisible"
      :edit-form="editForm"
      @submit="submitInventoryUpdate"
    />

    <!-- 编辑批次信息弹窗 -->
    <batch-info-dialog
      v-model:visible="showBatchInfoDialog"
      :form="batchEditForm"
      :production-date="currentProductionDate"
      :expiration-date="currentExpirationDate"
      :show-production-date-picker="showProductionDatePicker"
      :show-expiration-date-picker="showExpirationDatePicker"
      @update:show-production-date-picker="showProductionDatePicker = $event"
      @update:show-expiration-date-picker="showExpirationDatePicker = $event"
      @production-date-confirm="onProductionDateConfirm"
      @expiration-date-confirm="onExpirationDateConfirm"
      @submit="submitBatchInfoUpdate"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import api from "@/api/index.js";
import { showToast, showLoadingToast, closeToast } from 'vant';
import 'vant/lib/index.css';

// 导入拆分的组件
import InventoryHeader from './components/InventoryHeader.vue';
import InventoryStats from './components/InventoryStats.vue';
import InventorySearch from './components/InventorySearch.vue';
import InventoryList from './components/InventoryList.vue';
import InventoryEditDialog from './components/InventoryEditDialog.vue';
import BatchInfoDialog from './components/BatchInfoDialog.vue';

const isExpandAll = ref(false);
const editDialogVisible = ref(false);
const expandedRows = ref([]);
const searchQuery = ref('');

// 编辑表单数据
const editForm = ref({
  id: null,
  productId: null,
  batchId: null,
  quantity: 0,
  productName: '',
  batchNumber: undefined
});

// 批次信息编辑对话框状态
const showBatchInfoDialog = ref(false);
const showProductionDatePicker = ref(false);
const showExpirationDatePicker = ref(false);
const currentProductionDate = ref([]);
const currentExpirationDate = ref([]);

// 批次信息编辑表单数据
const batchEditForm = ref({
  productId: null,
  batchId: null,
  productName: '',
  batchNumber: '',
  productionDate: '',
  productionDateFormatted: '',
  expirationDate: '',
  expirationDateFormatted: '',
  status: true,
  remark: ''
});

// 表格数据
const tableData = ref([]);

// 过滤后的产品列表
const filteredProducts = computed(() => {
  if (!searchQuery.value) return processedTableData.value;
  
  return processedTableData.value.filter(item => 
    item.name.toLowerCase().includes(searchQuery.value.toLowerCase())
  );
});

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

// 格式化日期（提供给子组件使用）
const formatDate = (date) => {
  if (!date) return '-';
  return new Date(date).toLocaleDateString('zh-CN');
};

// 处理搜索
const handleSearch = () => {
  // 搜索逻辑已通过计算属性自动处理
};

// 获取数据
const getInventoryList = async () => {
  try {
    const loadingToast = showLoadingToast({
      message: '加载库存数据...',
      forbidClick: true,
      duration: 0
    });
    
    tableData.value = await api.product.getProducts();
    closeToast();
  } catch (error) {
    console.error('加载库存数据失败:', error);
    tableData.value = [];
    
    showToast({
      message: '加载库存数据失败',
      type: 'fail'
    });
  }
};

// 展开/收起单行
const toggleRow = (row) => {
  if (!row.children || !row.batchManaged) return;
  
  const index = expandedRows.value.indexOf(row.id);
  if (index > -1) {
    expandedRows.value.splice(index, 1);
  } else {
    expandedRows.value.push(row.id);
  }
};

// 展开/收起全部
const toggleExpand = () => {
  isExpandAll.value = !isExpandAll.value;
  
  if (isExpandAll.value) {
    // 展开所有批次管理的行
    expandedRows.value = processedTableData.value
      .filter(row => row.children && row.batchManaged)
      .map(row => row.id);
  } else {
    // 收起所有行
    expandedRows.value = [];
  }
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
    const loadingToast = showLoadingToast({
      message: '更新库存中...',
      forbidClick: true,
      duration: 0
    });
    
    // 构建更新数据
    const updateData = {
      productId: editForm.value.productId,
      batchId: editForm.value.batchId,
      quantity: editForm.value.quantity
    };

    // 调用API更新库存
    await api.inventory.update(updateData);
    
    // 关闭对话框
    editDialogVisible.value = false;
    
    // 重新加载数据
    await getInventoryList();
    
    // 显示成功提示
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

// 计算总数量
const getTotalQuantity = () => {
  return processedTableData.value.reduce((sum, row) => {
    // 只计算无子项的行或总计行
    if (!row.children) {
      return sum + (Number(row.quantity) || 0);
    }
    return sum;
  }, 0);
};

// 格式化显示日期
const formatDisplayDate = (dateStr) => {
  try {
    if (!dateStr) return '';
    const [year, month, day] = dateStr.split('-');
    return `${year}年${month.padStart(2, '0')}月${day.padStart(2, '0')}日`;
  } catch (e) {
    console.error('日期格式化错误:', e);
    return '';
  }
};

// 计算总金额
const getTotalAmount = () => {
  return processedTableData.value.reduce((sum, row) => {
    // 计算所有行的金额，无论是否有子项
    return sum + (Number(row.amount) || 0);
  }, 0);
};

// 打开批次信息编辑对话框
const openBatchInfoEditDialog = (batch, productId) => {
  const product = tableData.value.find(p => p.id == productId);
  // 初始化表单数据
  batchEditForm.value = {
    productId: parseInt(productId),
    batchId: batch.batchId,
    productName: product ? product.name : '',
    batchNumber: batch.batchNumber || '无批次',
    productionDate: batch.productionDate || '',
    productionDateFormatted: batch.productionDate ? formatDisplayDate(batch.productionDate) : '',
    expirationDate: batch.expirationDate || '',
    expirationDateFormatted: batch.expirationDate ? formatDisplayDate(batch.expirationDate) : '',
    status: true,
    remark: ''
  };

  // 设置日期选择器初始值
  currentProductionDate.value = batch.productionDate
    ? [
        new Date(batch.productionDate).getFullYear(),
        new Date(batch.productionDate).getMonth() + 1,
        new Date(batch.productionDate).getDate()
      ]
    : [new Date().getFullYear(), new Date().getMonth() + 1, new Date().getDate()];

  currentExpirationDate.value = batch.expirationDate
    ? [
        new Date(batch.expirationDate).getFullYear(),
        new Date(batch.expirationDate).getMonth() + 1,
        new Date(batch.expirationDate).getDate()
      ]
    : [new Date().getFullYear(), new Date().getMonth() + 1, new Date().getDate()];
  
  showBatchInfoDialog.value = true;
};

// 确认选择生产日期
const onProductionDateConfirm = ({ selectedValues }) => {
  try {
    const [year, month, day] = selectedValues;
    const formattedDate = `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`;
    
    batchEditForm.value.productionDate = formattedDate;
    batchEditForm.value.productionDateFormatted = `${year}年${month}月${day}日`;
    currentProductionDate.value = selectedValues;
    showProductionDatePicker.value = false;
  } catch (e) {
    console.error('生产日期选择错误:', e);
    batchEditForm.value.productionDate = '';
    batchEditForm.value.productionDateFormatted = '';
    showProductionDatePicker.value = false;
  }
};

// 确认选择过期日期
const onExpirationDateConfirm = ({ selectedValues }) => {
  try {
    const [year, month, day] = selectedValues;
    const formattedDate = `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`;
    
    batchEditForm.value.expirationDate = formattedDate;
    batchEditForm.value.expirationDateFormatted = `${year}年${month}月${day}日`;
    currentExpirationDate.value = selectedValues;
    showExpirationDatePicker.value = false;
  } catch (e) {
    console.error('过期日期选择错误:', e);
    batchEditForm.value.expirationDate = '';
    batchEditForm.value.expirationDateFormatted = '';
    showExpirationDatePicker.value = false;
  }
};

// 提交批次信息更新
const submitBatchInfoUpdate = async () => {
  try {
    const loadingToast = showLoadingToast({
      message: '更新批次信息中...',
      forbidClick: true,
      duration: 0
    });
    
    // 构建更新数据
    const updateData = {
      batchNumber: batchEditForm.value.batchNumber,
      id: batchEditForm.value.batchId,
      productionDate: batchEditForm.value.productionDate || null,
      expirationDate: batchEditForm.value.expirationDate || null,
      status: batchEditForm.value.status ? 1 : 0,
      remark: batchEditForm.value.remark || ''
    };
    
    // 调用API更新批次信息
    await api.batch.batchUpdate([updateData]);
    
    // 关闭对话框
    showBatchInfoDialog.value = false;
    
    // 重新加载数据
    await getInventoryList();
    
    // 显示成功提示
    showToast({
      message: '批次信息更新成功',
      type: 'success',
      position: 'top'
    });
  } catch (error) {
    console.error('更新批次信息失败:', error);
    showToast({
      message: '更新批次信息失败: ' + (error.message || '未知错误'),
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
  padding: 0;
  background-color: #f5f6fa;
  min-height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  -webkit-overflow-scrolling: touch;
  max-width: 100%;
  overflow-x: hidden;
}

/* 安卓设备特殊处理 */
@media screen and (max-width: 420px) {
  .inventory-container {
    padding-bottom: 20px; /* 为底部添加额外空间，避免被导航栏遮挡 */
  }
  
  /* 确保toast消息在安卓上正确显示 */
  :deep(.van-toast) {
    z-index: 3000 !important;
  }
  
  /* 改善按钮点击体验 */
  :deep(.van-button) {
    -webkit-tap-highlight-color: transparent;
  }
  
  /* 修复安卓弹窗在某些设备上的显示问题 */
  :deep(.van-dialog), :deep(.van-popup) {
    max-height: 90vh;
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
  }
}
</style>