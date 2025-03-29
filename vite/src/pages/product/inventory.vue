<route>
{
  name: "product-inventory"
}
</route>

<template>
  <div class="inventory-container">
    <!-- 顶部导航栏 -->
    <div class="top-navbar">
      <div class="title">产品库存管理</div>
      <div class="action-buttons">
        <van-button size="small" type="primary" @click="toggleExpand" class="expand-btn">
          {{ isExpandAll ? '收起批次' : '展开批次' }}
        </van-button>
      </div>
    </div>
    
    <!-- 库存总计卡片 -->
    <div class="stats-card">
      <div class="stats-header">
        <van-icon name="bar-chart-o" size="18" class="stats-icon" />
        <span>库存统计</span>
      </div>
      <div class="stats-content">
        <div class="stats-item">
          <div class="stats-value">{{ getTotalQuantity() }}</div>
          <div class="stats-label">总库存数量</div>
        </div>
        <div class="stats-divider"></div>
        <div class="stats-item">
          <div class="stats-value highlight">¥{{ getTotalAmount().toFixed(2) }}</div>
          <div class="stats-label">总库存金额</div>
        </div>
      </div>
    </div>
    
    <!-- 搜索过滤 -->
    <div class="search-bar">
      <van-search
        v-model="searchQuery"
        placeholder="搜索产品名称"
        shape="round"
        background="transparent"
        @search="handleSearch"
      />
    </div>
    
    <!-- 产品列表 -->
    <div class="product-list">
      <div
        v-for="item in filteredProducts" 
        :key="item.id" 
        class="product-card"
        :class="{'with-batch': item.batchManaged}"
      >
        <div class="product-header" @click="toggleRow(item)">
          <div class="product-info">
            <div class="product-name">{{ item.name }}
              <template v-if="item.children && item.batchManaged">
                <van-tag type="primary" size="small" class="batch-tag">批次管理</van-tag>
              </template>
            </div>
            <div class="product-meta">
              <template v-if="item.children && item.batchManaged">
                <span class="batch-count">{{ item.batchCount }}个批次</span>
              </template>
            </div>
          </div>
          
          <div class="product-data">
            <div class="stock-info">
              <div class="quantity-label">库存:</div>
              <div class="quantity-value">{{ item.quantity }}</div>
            </div>
            <div class="amount-info">
              <div class="amount-value">¥{{ formatAmount(item) }}</div>
            </div>
          </div>
          
          <div class="product-actions">
            <template v-if="!item.children">
              <van-button type="primary" size="small" @click.stop="openEditDialog(item)" class="edit-btn">
                <van-icon name="edit" />
                <span>修改</span>
              </van-button>
            </template>
            <template v-else-if="item.batchManaged">
              <van-icon 
                :name="expandedRows.includes(item.id) ? 'arrow-up' : 'arrow-down'" 
                class="expand-icon"
              />
            </template>
          </div>
        </div>
        
        <!-- 批次详情部分 -->
        <div 
          v-if="item.children && item.batchManaged && expandedRows.includes(item.id)" 
          class="batch-container"
        >
          <div class="batch-list">
            <div class="batch-list-header">
              <div class="batch-cell batch-number">批次号</div>
              <div class="batch-cell batch-date">生产/过期</div>
              <div class="batch-cell batch-quantity">数量</div>
              <div class="batch-cell batch-amount">金额</div>
              <div class="batch-cell batch-action">操作</div>
            </div>
            
            <div 
              v-for="(batch, index) in item.children" 
              :key="index"
              class="batch-list-item"
              :class="{'even-row': index % 2 === 0}"
            >
              <div class="batch-cell batch-number">
                <div class="batch-number-text">{{ batch.batchNumber || '无批次' }}</div>
              </div>
              
              <div class="batch-cell batch-date">
                <div class="date-info">
                  <div class="production-date">
                    <van-icon name="calendar-o" size="12" />
                    <span>{{ formatDate(batch.productionDate) }}</span>
                  </div>
                  <div class="expiration-date">
                    <van-icon name="underway-o" size="12" />
                    <span>{{ formatDate(batch.expirationDate) }}</span>
                  </div>
                </div>
              </div>
              
              <div class="batch-cell batch-quantity">
                <div class="quantity-badge">{{ batch.quantity }}</div>
              </div>
              
              <div class="batch-cell batch-amount">
                <div class="amount-text">¥{{ formatAmount(batch) }}</div>
              </div>
              
              <div class="batch-cell batch-action">
                <van-button 
                  type="primary" 
                  size="mini" 
                  @click.stop="openEditDialog(batch, item.id.split('-')[0])"
                  class="batch-edit-btn"
                >
                  修改
                </van-button>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 空状态 -->
      <div v-if="filteredProducts.length === 0" class="empty-state">
        <van-empty 
          description="没有找到相关产品" 
          image="search"
        />
      </div>
    </div>

    <!-- 修改库存弹窗 -->
    <van-dialog
      v-model:show="editDialogVisible"
      title="修改库存"
      :show-confirm-button="false"
      close-on-click-overlay
      class="custom-dialog"
    >
      <van-form @submit="submitInventoryUpdate">
        <van-cell-group inset>
          <van-field
            label="产品名称"
            :model-value="editForm.productName"
            readonly
            label-width="90px"
            class="form-field"
          />
          <van-field
            v-if="editForm.batchNumber !== undefined"
            label="批次号"
            :model-value="editForm.batchNumber"
            readonly
            label-width="90px"
            class="form-field"
          />
          <van-field
            name="quantity"
            label="库存数量"
            label-width="90px"
            required
            class="form-field"
          >
            <template #input>
              <van-stepper
                v-model="editForm.quantity"
                :min="0"
                integer
                input-width="80px"
                button-size="28px"
                theme="round"
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
import { showToast, showLoadingToast, closeToast } from 'vant';
import 'vant/lib/index.css';

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

// 表格数据
const tableData = ref([]);

// 过滤后的产品列表
const filteredProducts = computed(() => {
  if (!searchQuery.value) return processedTableData.value;
  
  return processedTableData.value.filter(item => 
    item.name.toLowerCase().includes(searchQuery.value.toLowerCase())
  );
});

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

// 计算总金额
const getTotalAmount = () => {
  return processedTableData.value.reduce((sum, row) => {
    // 只计算无子项的行或总计行
    if (!row.children) {
      return sum + (Number(row.amount) || 0);
    }
    return sum;
  }, 0);
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
}

/* 顶部导航栏 */
.top-navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #3f51b5, #2196f3);
  color: #fff;
  padding: 12px 16px;
  position: sticky;
  top: 0;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.title {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.expand-btn {
  border-radius: 20px;
  font-weight: 500;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background-color: rgba(255, 255, 255, 0.2);
  color: white;
}

/* 统计卡片 */
.stats-card {
  margin: 16px;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.stats-header {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  display: flex;
  align-items: center;
}

.stats-icon {
  margin-right: 8px;
  color: #3f51b5;
}

.stats-content {
  display: flex;
  padding: 16px;
}

.stats-item {
  flex: 1;
  text-align: center;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.stats-divider {
  width: 1px;
  background-color: #eee;
  margin: 0 16px;
}

.stats-value {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin-bottom: 4px;
}

.stats-value.highlight {
  color: #f5222d;
}

.stats-label {
  font-size: 13px;
  color: #999;
}

/* 搜索栏 */
.search-bar {
  padding: 0 16px 8px;
}

/* 产品列表 */
.product-list {
  padding: 0 16px 16px;
}

.product-card {
  background-color: #fff;
  border-radius: 10px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  transition: all 0.3s ease;
}

.product-card.with-batch {
  border-left: 3px solid #3f51b5;
}

.product-header {
  display: flex;
  padding: 14px 16px;
  position: relative;
  transition: background-color 0.2s;
  cursor: pointer;
}

.product-header:active {
  background-color: #f9f9f9;
}

.product-info {
  flex: 3;
  overflow: hidden;
}

.product-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  line-height: 1.4;
}

.batch-tag {
  margin-left: 8px;
  font-size: 10px;
  padding: 0 6px;
  height: 18px;
  line-height: 18px;
  vertical-align: middle;
  border-radius: 10px;
}

.product-meta {
  font-size: 12px;
  color: #999;
}

.batch-count {
  display: inline-flex;
  align-items: center;
}

.product-data {
  flex: 2;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  text-align: right;
}

.stock-info {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}

.quantity-label {
  font-size: 13px;
  color: #666;
  margin-right: 6px;
}

.quantity-value {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.amount-info {
  font-size: 14px;
  font-weight: 600;
  color: #f5222d;
}

.product-actions {
  margin-left: 12px;
  display: flex;
  align-items: center;
}

.edit-btn {
  border-radius: 20px;
  font-size: 12px;
}

.edit-btn :deep(.van-icon) {
  margin-right: 4px;
  font-size: 14px;
}

.expand-icon {
  font-size: 16px;
  padding: 8px;
  color: #666;
}

/* 批次详情 */
.batch-container {
  border-top: 1px solid #f0f0f0;
  overflow: hidden;
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from {
    max-height: 0;
    opacity: 0;
  }
  to {
    max-height: 1000px;
    opacity: 1;
  }
}

.batch-list {
  padding: 12px 16px 16px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.batch-list-header {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 12px;
  font-weight: 600;
  color: #666;
}

.batch-list-item {
  display: flex;
  border-bottom: 1px solid #f5f5f5;
  font-size: 13px;
  transition: background-color 0.2s;
}

.batch-list-item:last-child {
  border-bottom: none;
}

.even-row {
  background-color: #f9f9f9;
}

.batch-cell {
  padding: 10px 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.batch-number {
  flex: 2;
  justify-content: flex-start;
}

.batch-number-text {
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100px;
}

.batch-date {
  flex: 3;
}

.date-info {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.production-date,
.expiration-date {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #666;
  margin-bottom: 2px;
}

.production-date :deep(.van-icon),
.expiration-date :deep(.van-icon) {
  margin-right: 4px;
}

.batch-quantity {
  flex: 1;
}

.quantity-badge {
  background-color: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 500;
  font-size: 12px;
  min-width: 40px;
  text-align: center;
}

.batch-amount {
  flex: 2;
}

.amount-text {
  color: #f5222d;
  font-weight: 500;
}

.batch-action {
  flex: 1;
}

.batch-edit-btn {
  border-radius: 15px;
  font-size: 11px;
  height: 24px;
  line-height: 24px;
}

/* 空状态 */
.empty-state {
  margin-top: 32px;
}

/* 弹窗样式 */
:deep(.custom-dialog .van-dialog__header) {
  padding: 16px;
  text-align: center;
  font-size: 18px;
  color: #333;
  font-weight: 600;
}

.form-field {
  margin-bottom: 8px;
}

.dialog-footer {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.submit-btn {
  margin-bottom: 4px;
  height: 44px;
  font-size: 16px;
  font-weight: 500;
}

.cancel-btn {
  height: 44px;
  font-size: 16px;
}

/* 适配小屏设备 */
@media screen and (max-width: 360px) {
  .product-name {
    font-size: 14px;
  }
  
  .batch-date {
    display: none;
  }
  
  .batch-number {
    flex: 3;
  }
}
</style>