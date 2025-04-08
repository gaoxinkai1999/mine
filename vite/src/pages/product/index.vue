<route>
  {
    name: 'product-list'
  }
</route>

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
                      <van-button size="mini" type="primary" plain @click="editItem(element)">编辑</van-button>
                      <van-button size="mini" type="danger" plain @click="deleteItem(element.id)">删除</van-button>
                      <van-button size="mini" type="info" plain @click="viewOrderList(element.id,element.name)">销售记录</van-button>
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
            <van-radio-group v-model="newItem.isBatchManaged" direction="horizontal">
              <van-radio :name="true">是</van-radio>
              <van-radio :name="false">否</van-radio>
            </van-radio-group>
          </template>
        </van-field>
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
        isBatchManaged: false,
        costPrice: 0,
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
      
      // 开始拖拽时，让元素在硬件加速层上运行，提高性能
      if (this.draggedElement) {
        this.draggedElement.style.willChange = 'transform';
        this.draggedElement.style.transform = 'translateZ(0)';
      }
      
      // 添加CSS类避免过多重新计算布局
      document.body.classList.add('dragging-active');
      
      // 禁用其他触摸事件，防止滚动冲突
      document.addEventListener('touchmove', this.preventDefaultTouchMove, { passive: false });
    },
    
    preventDefaultTouchMove(e) {
      if (this.isDragging) {
        e.preventDefault();
      }
    },
    
    handleTouchStart() {
      this.touchStartTime = Date.now();
    },
    handleTouchEnd(categoryId) {
      const touchDuration = Date.now() - this.touchStartTime;
      if (touchDuration < 200 && !this.isDragging) {
        this.setActiveCategory(categoryId);
      }
      this.isDragging = false;
    },
    setActiveCategory(id) {
      if (this.activeCategory !== id) {
        this.activeCategory = id;
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
        isBatchManaged: item.batchManaged,
        costPrice: item.costPrice,
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
      // 移除拖拽时添加的CSS类
      document.body.classList.remove('dragging-active');
      document.removeEventListener('touchmove', this.preventDefaultTouchMove);
      
      // 恢复拖拽元素的默认样式
      if (this.draggedElement) {
        this.draggedElement.style.willChange = 'auto';
        this.draggedElement.style.transform = '';
        this.draggedElement = null;
      }
      
      this.isDragging = false;
      
      // 如果没有实际移动位置，不进行更新操作
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
    async onCategoryDragEnd() {
      try {
        await api.category.update(
          this.categories.map((item, index) => ({
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
    viewOrderList(productId,productName) {
      this.$router.push({
        name: ROUTE_NAMES.PRODUCT_ORDER_LIST,
        query: { productId,productName }
      });
    },
  },
  async mounted() {
    await this.init()
  },
  beforeUnmount() {
    // 清理事件监听器
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
  transition: box-shadow 0.2s ease;
}

.drag-handle {
  width: 48px;
  background: #f7f8fa;
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
  touch-action: none;
  -webkit-user-select: none;
  user-select: none;
  cursor: grab;
  font-size: 20px;
}

.drag-handle:active {
  cursor: grabbing;
}

.goods-content {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.goods-header {
  display: flex;
  align-items: flex-start;
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
}

.goods-detail-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 14px;
}

.goods-actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

/* 弹出层样式优化 */
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

/* 品类管理样式优化 */
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

/* 拖拽效果优化 */
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

/* 表单字段样式优化 */
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

/* 响应式布局优化 */
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

/* 超小屏幕适配 */
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

/* 暗色模式优化 */
@media (prefers-color-scheme: dark) {
  .app {
    background: #1c1c1e;
  }

  .nav-bar {
    background: rgba(44, 44, 46, 0.95);
  }

  .nav-title {
    color: #fff;
  }

  .sidebar-container {
    background: #2c2c2e;
    border-right: 1px solid #3a3a3c;
  }

  .goods-list-content {
    background: #2c2c2e;
  }

  .goods-item,
  .goods-list {
    background: #2c2c2e;
    border-color: #3a3a3c;
  }

  .drag-handle {
    background: #3a3a3c;
  }

  .goods-title {
    color: #fff;
  }

  .goods-detail-item {
    color: #8e8e93;
  }

  .goods-detail-item .van-icon {
    color: #666;
  }

  .category-item span {
    color: #fff;
  }

  .empty {
    color: #8e8e93;
  }

  :deep(.van-field) {
    background-color: #2c2c2e;
  }

  :deep(.van-field__label) {
    color: #fff;
  }

  .sortable-ghost {
    background: #3a3a3c;
    border-color: #4a4a4c;
  }

  .sortable-drag {
    background: #2c2c2e;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  }

  .goods-item:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  }

  .popup-header {
    background: #2c2c2e;
    border-bottom-color: #3a3a3c;
  }

  .popup-title {
    color: #fff;
  }

  .category-item {
    background: #3a3a3c;
  }

  .category-popup {
    background: #2c2c2e;
  }

  .popup-bottom-actions {
    background: #2c2c2e;
  }
}

/* 滚动条美化 */
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

/* 添加硬件加速 */
.sortable-drag, .sortable-ghost, .sortable-chosen {
  transform: translateZ(0);
  will-change: transform, opacity;
  backface-visibility: hidden;
  perspective: 1000px;
}
</style>