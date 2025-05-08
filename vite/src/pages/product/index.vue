<template>
  <div class="app">
    <!-- 顶部导航栏 -->
    <div class="nav-bar">
      <div class="nav-title">商品管理</div>
      <van-button
        icon="plus"
        size="small"
        type="primary"
        plain
        @click.stop="showCategoryPopup = true"
      >
        管理品类
      </van-button>
    </div>

    <!-- 主体内容区 -->
    <div class="main-container">
      <!-- 品类列表 -->
      <div class="sidebar-container">
        <van-sidebar v-model="activeCategory">
          <van-sidebar-item
            v-for="category in categories"
            :key="category.id"
            :title="category.name"
          />
        </van-sidebar>
      </div>

      <!-- 商品列表 -->
      <div class="goods-list">
        <div class="goods-list-content">
          <div v-if="currentFoods.length === 0" class="empty">
            <van-empty description="暂无商品" />
            <van-button
              style="margin-top: 16px"
              type="primary"
              size="small"
              @click="showAddProductPopup()"
            >
              新增商品
            </van-button>
          </div>
          <template v-else>
            <draggable
              v-model="currentFoods"
              :animation="150"
              :delay="0"
              :force-fallback="true"
              :touch-start-threshold="5"
              :handle="'.drag-handle'"
              item-key="id"
              :disabled="false"
              :throttle="16"
              ghost-class="sortable-ghost"
              chosen-class="sortable-chosen"
              drag-class="sortable-drag"
              :sort="true"
              @start="dragStart"
              @end="onDragEnd"
            >
              <template #item="{ element }">
                <div class="goods-item">
                  <div class="drag-handle">
                    <van-icon name="bars" />
                  </div>
                  <div class="goods-content">
                    <div class="goods-header">
                      <h4 class="goods-title">{{ element.name }}</h4>
                    </div>
                    <div class="goods-details">
                      <div class="goods-detail-item">
                        <van-icon name="warehouse-o" />
                        <span>库存：{{ element.productStockDTO.totalInventory }}</span>
                      </div>
                      <div class="goods-detail-item">
                        <van-icon name="balance-o" />
                        <span>成本：￥{{ element.costPrice }}</span>
                      </div>
                      <div class="goods-detail-item">
                        <van-icon name="gold-coin-o" />
                        <span>售价：￥{{ element.defaultSalePrice }}</span>
                      </div>
                    </div>
                    <div class="goods-actions">
                      <van-button size="small" type="primary" plain @click="editItem(element)">
                        <van-icon name="edit" />
                        <span>编辑</span>
                      </van-button>
                      <van-button size="small" type="danger" plain @click="deleteItem(element.id)">
                        <van-icon name="delete" />
                        <span>删除</span>
                      </van-button>
                      <van-button size="small" type="info" plain @click="viewOrderList(element.id,element.name)">
                        <van-icon name="orders-o" />
                        <span>销售记录</span>
                      </van-button>
                      <van-button 
                        v-if="!element.batchManaged" 
                        size="small" 
                        type="warning" 
                        plain 
                        @click="showConvertToBatchPopup(element)">
                        <van-icon name="exchange" />
                        <span>批次转换</span>
                      </van-button>
                    </div>
                  </div>
                </div>
              </template>
            </draggable>

            <van-button
              block
              style="margin-top: 16px"
              type="primary"
              @click="showAddProductPopup()"
            >
              新增商品
            </van-button>
          </template>
        </div>
      </div>
    </div>

    <!-- 品类管理弹出层 -->
    <van-popup v-model:show="showCategoryPopup" :style="{ height: '70%' }" position="bottom">
      <div class="popup-header">
        <div class="popup-title">品类管理</div>
      </div>
      <div class="popup-content category-popup">
        <van-field v-model="newCategoryName" label="品类名称" placeholder="请输入品类名称" required />
        <div class="popup-actions">
          <van-button block type="primary" @click="addCategory">新增品类</van-button>
        </div>
        <div class="category-list">
          <div v-for="category in categories" :key="category.id" class="category-item">
            <span>{{ category.name }}</span>
            <van-button v-if="category.name !== '全部'" plain size="small" type="danger"
              @click="deleteCategory(category.id)">
              删除
            </van-button>
          </div>
        </div>
        <div class="popup-bottom-actions">
          <van-button block type="default" @click="showCategoryPopup = false">关闭</van-button>
        </div>
      </div>
    </van-popup>

    <!-- 新增/编辑商品弹出层 -->
    <van-popup v-model:show="showAddPopup" :style="{ height: 'auto', padding: '16px' }" position="bottom">
      <div class="popup-content">
        <van-field v-model="newItem.name" label="商品名称" placeholder="请输入商品名称" required />
        <van-field v-model="newItem.defaultSalePrice" label="默认售价" placeholder="请输入售价" required type="number" />
        <van-field v-model="newItem.costPrice" label="成本价" placeholder="请输入成本价" required type="number" />
        <van-field name="radio" label="管理批次" required>
          <template #input>
            <van-radio-group v-model="newItem.batchManaged" direction="horizontal" :disabled="isEdit">
              <van-radio :name="true">是</van-radio>
              <van-radio :name="false">否</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <!-- 在编辑模式下添加提示信息 -->
        <div v-if="isEdit" class="field-tip">
          批次管理状态不可直接修改。非批次商品需通过"批次转换"功能转为批次管理。
        </div>
        <!-- 新增：临期监控阈值 -->
        <van-field
          v-if="newItem.batchManaged"
          v-model.number="newItem.expiryMonitoringThresholdDays"
          label="临期阈值(天)"
          placeholder="请输入天数 (可选)"
          type="number"
        />
        <!-- 新增：条码输入框 -->
        <van-field v-model="newItem.barcode" label="条码" placeholder="请输入商品条码" maxlength="50" />
        <van-field v-model="newItem.category.name" clickable label="品类" placeholder="请选择品类" readonly required
          @click="showPicker = true" />
        <van-popup v-model:show="showPicker" position="bottom">
          <van-picker :columns="categoryOptions" show-toolbar @cancel="showPicker = false" @confirm="onPickerConfirm" />
        </van-popup>
        <div class="popup-actions">
          <van-button block type="primary" @click="saveNewItem">保存</van-button>
          <van-button block type="default" @click="cancelEdit">取消</van-button>
        </div>
      </div>
    </van-popup>

    <!-- 批次转换弹出层 -->
    <van-popup v-model:show="showConvertBatchPopup" :style="{ height: 'auto', padding: '16px' }" position="bottom">
      <div class="popup-content">
        <div class="popup-header">
          <div class="popup-title">转换为批次管理商品</div>
        </div>
        <div style="margin-top: 16px;">
          <p>确定要将商品 <b>{{ convertItem.name }}</b> 转换为批次管理商品吗？</p>
          <p class="warning-text">此操作不可逆，转换后商品将使用批次管理模式！</p>
          <p>若现有库存数量大于0，将创建一个初始批次。</p>
        </div>
        <van-field 
          v-model="formatProductionDate" 
          label="生产日期" 
          placeholder="点击选择生产日期" 
          readonly
          @click="showDatePicker = true" 
        />
        <van-popup v-model:show="showDatePicker" position="bottom">
          <van-date-picker
            v-model="convertProductionDate"
            title="选择生产日期"
            :min-date="minDate"
            :max-date="maxDate"
            @confirm="onDateConfirm"
            @cancel="showDatePicker = false"
          />
        </van-popup>
        <div class="popup-actions">
          <van-button block type="primary" @click="confirmConvertToBatch">确认转换</van-button>
          <van-button block type="default" @click="cancelConvertToBatch">取消</van-button>
        </div>
      </div>
    </van-popup>

  </div>
</template>

<script>

import draggable from 'vuedraggable'
import api from "@/api";
import { showFailToast, showSuccessToast } from "vant";
import {ROUTE_NAMES} from "@/constants/routeNames.js";


export default {
  components: {
    draggable,

  },

  data() {
    return {
      goodsList: [],
      categories: [],
      activeCategory: 0,
      showCategoryPopup: false,
      newCategoryName: "",
      showAddPopup: false,
      showPicker: false,
      newItem: this.getEmptyNewItem(),
      isEdit: false,
      loading: true,
      isAdmin: true,
      touchStartTime: 0,
      isDragging: false,
      draggedElement: null,
      showConvertBatchPopup: false,
      convertItem: {},
      convertProductionDate: new Date(),
      formatProductionDate: "",
      showDatePicker: false,
      minDate: new Date(new Date().setFullYear(new Date().getFullYear() - 1)),
      maxDate: new Date(),
    };
  },
  computed: {
    currentFoods: {
      get() {
        const categoryId = this.categories[this.activeCategory]?.id;
        return this.goodsList.filter(food => food?.categoryId === categoryId);
      },
      set(value) {
        const categoryId = this.categories[this.activeCategory]?.id;
        const otherFoods = this.goodsList.filter(food => food?.categoryId !== categoryId);
        this.goodsList = [...otherFoods, ...value];
      }
    },
    categoryOptions() {
      return this.categories.map(c => ({ text: c.name, value: c.id }));
    }
  },
  methods: {
    showAddProductPopup() {
      this.newItem = this.getEmptyNewItem();
      this.isEdit = false;
      this.showAddPopup = true;
    },
    async getCategories() {
      try {
        this.categories = await api.category.getCategories();
        this.activeCategory = this.categories[0].id
      } catch (error) {
        console.error('获取品类失败:', error);
      }
    },
    async getProducts() {
      try {
        this.goodsList = await api.product.getProducts();
      } catch (error) {
        console.error('获取商品失败:', error);
      } finally {
        this.loading = false;
      }
    },
    getEmptyNewItem() {
      return {
        id: null,
        name: "",
        defaultSalePrice: 0,
        categoryId: null,
        category: { id: null, name: "" },
        batchManaged: false,
        costPrice: 0,
        expiryMonitoringThresholdDays: null,
        barcode: "",
      };
    },
    onPickerConfirm({ selectedOptions }) {
      const selected = this.categories.find(c => c.id === selectedOptions[0].value);
      this.newItem.category = selected;
      this.newItem.categoryId = selected.id;
      this.showPicker = false;
    },
    dragStart(e) {
      this.isDragging = true;
      this.draggedElement = e.item;
      
      if (this.draggedElement) {
        this.draggedElement.style.willChange = 'transform';
        this.draggedElement.style.transform = 'translateZ(0)';
      }
      
      document.body.classList.add('dragging-active');
      
      document.addEventListener('touchmove', this.preventDefaultTouchMove, { passive: false });
    },
    
    preventDefaultTouchMove(e) {
      if (this.isDragging) {
        e.preventDefault();
      }
    },

    async addCategory() {
      if (!this.newCategoryName.trim()) {
        showFailToast("品类名称不能为空");
        return;
      }

      try {
        await api.category.create({
          name: this.newCategoryName,
          sort: this.categories.length
        });
        await this.getCategories();
        showSuccessToast("品类已新增");
      } catch (error) {
        console.error('新增品类失败:', error);
      }
    },
    async deleteCategory(id) {
      try {
        const hasGoods = this.goodsList.some(item => item.category?.id === id);
        if (hasGoods) {
          showFailToast("该品类下存在商品，无法删除");
          return;
        }

        await api.category.deleteCategory(id);
        this.categories = this.categories.filter(c => c.id !== id);
        showSuccessToast("品类已删除");
      } catch (error) {
        console.error('删除品类失败:', error);
      }
    },
    async deleteItem(id) {
      try {
        await api.product.deleteProduct(id);
        this.goodsList = this.goodsList.filter(item => item.id !== id);
        showSuccessToast("商品已删除");
      } catch (error) {
        console.error('删除商品失败:', error);
      }
    },
    editItem(item) {
      this.newItem = {
        id: item.id,
        name: item.name,
        defaultSalePrice: item.defaultSalePrice,
        categoryId: item.categoryId,
        category: { id: item.categoryId, name: this.categories.find(c => c.id === item.categoryId).name },
        batchManaged: item.batchManaged,
        costPrice: item.costPrice,
        expiryMonitoringThresholdDays: item.expiryMonitoringThresholdDays,
        barcode: item.barcode || "",
      };
      this.isEdit = true;
      this.showAddPopup = true;
    },
    cancelEdit() {
      this.showAddPopup = false;
      this.newItem = this.getEmptyNewItem();
      this.isEdit = false;
    },
    async saveNewItem() {
      if (!this.validateItem()) return;

      try {
        if (this.isEdit) {
          // 获取原始商品数据
          const originalProduct = this.goodsList.find(item => item.id === this.newItem.id);
          if (originalProduct) {
            // 确保在编辑模式下保留原批次管理状态
            this.newItem.batchManaged = originalProduct.batchManaged;
          }
          
          await api.product.batchUpdate([this.newItem]);
          await this.init()
        } else {
          await api.product.createProduct(this.newItem);
          await this.getProducts();
        }

        this.cancelEdit();
        showSuccessToast("操作成功");
      } catch (error) {
        console.error('保存失败:', error);
      }
    },
    validateItem() {
      if (!this.newItem.name ||
        this.newItem.categoryId == null ||
        this.newItem.defaultSalePrice <= 0 ||
        this.newItem.costPrice <= 0
      ) {
        showFailToast("请填写完整信息");
        return false;
      }
      return true;
    },
    async onDragEnd(evt) {
      document.body.classList.remove('dragging-active');
      document.removeEventListener('touchmove', this.preventDefaultTouchMove);
      
      if (this.draggedElement) {
        this.draggedElement.style.willChange = 'auto';
        this.draggedElement.style.transform = '';
        this.draggedElement = null;
      }
      
      this.isDragging = false;
      
      if (evt.oldIndex === evt.newIndex) return;
      
      try {
        await api.product.batchUpdate(
          this.currentFoods.map((item, index) => ({
            id: item.id,
            sort: index
          }))
        );
        showSuccessToast('排序已更新');
      } catch (error) {
        console.error('排序失败:', error);
      }
    },

    async init() {
      await this.getCategories();
      await this.getProducts();
    },
    viewOrderList(productId, productName) {
      this.$router.push({
        name: ROUTE_NAMES.PRODUCT_ORDER_LIST,
        query: { productId, productName }
      });
    },
    showConvertToBatchPopup(item) {
      this.convertItem = { ...item };
      this.convertProductionDate = new Date();
      this.formatProductionDate = this.formatDate(this.convertProductionDate);
      this.showConvertBatchPopup = true;
    },
    formatDate(date) {
      if (!date) return '';
      const d = new Date(date);
      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    },
    onDateConfirm(value) {
      this.convertProductionDate = value;
      this.formatProductionDate = this.formatDate(value);
      this.showDatePicker = false;
    },
    async confirmConvertToBatch() {
      try {
        await api.product.convertToBatchProduct({
          productId: this.convertItem.id,
          productionDate: this.formatProductionDate
        });
        
        showSuccessToast("商品已转换为批次管理");
        this.showConvertBatchPopup = false;
        
        await this.getProducts();
      } catch (error) {
        console.error('转换批次管理失败:', error);
        showFailToast("转换失败：" + (error.message || '未知错误'));
      }
    },
    cancelConvertToBatch() {
      this.showConvertBatchPopup = false;
    },
  },
  async mounted() {
    await this.init()
  },
  beforeUnmount() {
    document.removeEventListener('touchmove', this.preventDefaultTouchMove);
  }
};
</script>

<style scoped>
.app {
  padding: 0;
  max-width: 1200px;
  margin: 0 auto;
  background: #f7f8fa;
  min-height: 100vh;
  box-sizing: border-box;
  -webkit-tap-highlight-color: transparent;
  display: flex;
  flex-direction: column;
  position: relative;
}

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 99;
  height: 56px;
  padding: env(safe-area-inset-top) 16px 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.nav-title {
  font-size: 18px;
  font-weight: 600;
  color: #323233;
}

.main-container {
  display: flex;
  margin-top: calc(56px + env(safe-area-inset-top));
  height: calc(100vh - 56px - env(safe-area-inset-top));
  position: relative;
  overflow: hidden;
}

.sidebar-container {
  position: sticky;
  top: calc(56px + env(safe-area-inset-top));
  width: 88px;
  height: calc(100vh - 56px - env(safe-area-inset-top));
  background: #fff;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  box-shadow: 1px 0 3px rgba(0, 0, 0, 0.1);
  z-index: 2;
}

.sidebar-container :deep(.van-sidebar) {
  width: 100%;
  background: transparent;
}

.sidebar-container :deep(.van-sidebar-item) {
  padding: 12px 8px;
  font-size: 14px;
}

.sidebar-container :deep(.van-sidebar-item--select) {
  background: var(--van-primary-color);
  color: #fff;
}

.sidebar-container :deep(.van-sidebar-item--select)::before {
  display: none;
}

.goods-list {
  flex: 1;
  background: transparent;
  padding: 16px;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior-y: contain;
  position: relative;
  height: 100%;
}

.goods-list-content {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  min-height: calc(100% - 32px);
}

.goods-item {
  position: relative;
  background: #fff;
  border-radius: 12px;
  margin-bottom: 12px;
  display: flex;
  border: 1px solid #ebedf0;
  will-change: transform;
  transition: all 0.25s ease;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.goods-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.08);
  border-color: #dcdee0;
}

.drag-handle {
  width: 48px;
  background: #f8f9fa;
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
  touch-action: none;
  -webkit-user-select: none;
  user-select: none;
  cursor: grab;
  font-size: 20px;
  border-right: 1px solid #ebedf0;
  transition: background-color 0.2s;
}

.drag-handle:hover {
  background: #f2f3f5;
  color: #666;
}

.drag-handle:active {
  cursor: grabbing;
  background: #ebedf0;
}

.goods-content {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.goods-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.goods-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #323233;
  line-height: 1.4;
}

.goods-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #666;
}

.goods-detail-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  line-height: 1.4;
}

.goods-detail-item :deep(.van-icon) {
  font-size: 16px;
  color: #969799;
}

.goods-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.goods-actions :deep(.van-button) {
  flex-shrink: 0;
  min-width: 64px;
  font-size: 12px;
  padding: 0 8px;
  height: 30px;
  border-radius: 4px;
  line-height: 28px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.goods-actions :deep(.van-button .van-icon) {
  margin-right: 4px;
  font-size: 14px;
}

.goods-actions :deep(.van-button:hover) {
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.goods-actions :deep(.van-button--primary.van-button--plain) {
  color: var(--van-primary-color);
  border-color: var(--van-primary-color);
}

.goods-actions :deep(.van-button--danger.van-button--plain) {
  color: var(--van-danger-color);
  border-color: var(--van-danger-color);
}

.goods-actions :deep(.van-button--warning.van-button--plain) {
  color: var(--van-warning-color);
  border-color: var(--van-warning-color);
}

.goods-actions :deep(.van-button--info.van-button--plain) {
  color: var(--van-info-color);
  border-color: var(--van-info-color);
}

/* 响应式调整 */
@media (max-width: 480px) {
  .goods-actions {
    gap: 6px;
  }
  
  .goods-actions :deep(.van-button) {
    min-width: auto;
    padding: 0 6px;
    height: 28px;
    line-height: 26px;
    font-size: 11px;
  }
  
  .goods-actions :deep(.van-button .van-icon) {
    margin-right: 3px;
    font-size: 12px;
  }
}

/* 超小屏幕调整 */
@media (max-width: 360px) {
  .goods-actions :deep(.van-button) {
    height: 26px;
    line-height: 24px;
    font-size: 10px;
    padding: 0 4px;
  }
}

.popup-content {
  padding: 20px 16px;
  padding-bottom: max(16px, env(safe-area-inset-bottom));
  border-radius: 16px 16px 0 0;
  max-height: 80vh;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.popup-actions {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.popup-actions :deep(.van-button) {
  border-radius: 8px;
  font-size: 16px;
}

.popup-header {
  position: sticky;
  top: 0;
  z-index: 1;
  padding: 16px;
  background: #fff;
  border-bottom: 1px solid #ebedf0;
  text-align: center;
}

.popup-title {
  font-size: 18px;
  font-weight: 600;
  color: #323233;
}

.category-popup {
  height: calc(100% - 120px);
  display: flex;
  flex-direction: column;
  padding: 16px;
}

.category-list {
  flex: 1;
  overflow-y: auto;
  margin: 16px 0;
  -webkit-overflow-scrolling: touch;
}

.category-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 8px;
  background: #f7f8fa;
  border-radius: 8px;
}

.popup-bottom-actions {
  padding: 16px 0;
  background: #fff;
}

.empty {
  text-align: center;
  color: #969799;
  padding: 48px 0;
  font-size: 15px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.sortable-ghost {
  opacity: 0.5;
  background: #f2f3f5;
  transform: scale(0.98);
  box-shadow: none !important;
}

.sortable-chosen {
  background-color: #fff;
}

.sortable-drag {
  opacity: 0.9;
  background: #fff;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.08);
  transform: scale(1.02);
  z-index: 10;
}

:global(.dragging-active) {
  cursor: grabbing !important;
  user-select: none;
  pointer-events: auto !important;
  touch-action: none !important;
}

:global(.dragging-active *) {
  cursor: grabbing !important;
}

:deep(.van-field) {
  margin-bottom: 16px;
  border-radius: 8px;
  background-color: #f7f8fa;
  padding: 4px 8px;
}

:deep(.van-field__label) {
  font-weight: 500;
  color: #323233;
}

:deep(.van-button) {
  height: 44px;
  line-height: 42px;
  font-weight: 500;
}

:deep(.van-button--small) {
  height: 36px;
  line-height: 34px;
  padding: 0 16px;
  font-size: 14px;
}

@media screen and (max-width: 768px) {
  .app {
    padding: 0;
  }

  .nav-bar {
    padding: env(safe-area-inset-top) 12px 0;
  }

  .sidebar-container {
    width: 72px;
  }

  .goods-list {
    padding: 12px;
  }

  .goods-list-content {
    padding: 12px;
  }

  .goods-content {
    padding: 10px 12px;
  }

  .goods-title {
    font-size: 15px;
  }

  .goods-detail-item {
    font-size: 13px;
  }
}

@media screen and (max-width: 360px) {
  .nav-bar {
    padding: env(safe-area-inset-top) 8px 0;
    height: 48px;
  }

  .main-container {
    margin-top: calc(48px + env(safe-area-inset-top));
    height: calc(100vh - 48px - env(safe-area-inset-top));
  }

  .sidebar-container {
    width: 64px;
    top: calc(48px + env(safe-area-inset-top));
    height: calc(100vh - 48px - env(safe-area-inset-top));
  }

  .goods-list {
    padding: 8px;
  }

  .goods-list-content {
    padding: 8px;
  }

  .drag-handle {
    width: 40px;
  }

  .goods-content {
    padding: 8px 10px;
  }

  .goods-title {
    font-size: 14px;
  }

  .goods-details {
    gap: 8px;
  }

  .goods-detail-item {
    font-size: 12px;
  }

  .goods-actions :deep(.van-button) {
    padding: 0 8px;
    height: 24px;
    line-height: 22px;
    font-size: 12px;
  }
}

.goods-list::-webkit-scrollbar {
  width: 6px;
}

.goods-list::-webkit-scrollbar-track {
  background: transparent;
}

.goods-list::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}

@media (hover: none) {
  .goods-item:hover {
    transform: none;
    box-shadow: none;
  }

  .category-item-wrapper:active {
    opacity: 0.7;
    transition: opacity 0.2s;
  }

  .goods-item:active {
    background: rgba(0, 0, 0, 0.05);
  }
}

.sortable-drag, .sortable-ghost, .sortable-chosen {
  transform: translateZ(0);
  will-change: transform, opacity;
  backface-visibility: hidden;
  perspective: 1000px;
}

/* 批次转换弹窗样式 */
.warning-text {
  color: #ff976a;
  font-weight: bold;
  margin: 10px 0;
}

/* 字段提示样式 */
.field-tip {
  color: #ff976a;
  font-size: 12px;
  margin: -8px 0 12px 16px;
  line-height: 1.4;
}

/* 添加硬件加速 */
.sortable-drag, .sortable-ghost, .sortable-chosen {
  transform: translateZ(0);
  will-change: transform, opacity;
  backface-visibility: hidden;
  perspective: 1000px;
}
</style>