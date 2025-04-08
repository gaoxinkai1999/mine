<route>
{
  name: "shop-arrears"
}
</route>

<template>
  <div class="arrears-page">
    <van-nav-bar
      title="欠款详情"
      left-arrow
      @click-left="$router.back()"
      fixed
      placeholder
    >
      <template #right>
        <van-icon name="replay" size="18" @click="getArrears" />
      </template>
    </van-nav-bar>
    
    <div class="page-container">
      <!-- 固定区域：总欠款卡片 -->
      <div class="fixed-section summary-section">
        <div class="summary-card">
          <div class="summary-content">
            <div class="summary-header">
              <span class="summary-label">总欠款金额</span>
              <van-icon name="balance-o" class="summary-icon" />
            </div>
            <div class="summary-amount">¥{{ totalArrears }}</div>
            <div class="summary-footer">
              来自{{ TableData.length }}个商家
            </div>
          </div>
        </div>
      </div>

      <!-- 固定区域：搜索框 -->
      <div class="fixed-section search-section">
        <van-search 
          v-model="searchText" 
          placeholder="搜索商家名称或地址" 
          shape="round"
        />
      </div>

      <!-- 固定区域：列表标题 -->
      <div class="fixed-section list-header-section">
        <div class="list-header">
          <span>商家欠款明细</span>
        </div>
      </div>

      <!-- 可滚动区域：明细列表 -->
      <div class="scroll-section">
        <van-empty v-if="filteredData.length === 0" description="暂无欠款数据" />
        
        <div v-else class="shop-list">
          <div 
            v-for="item in filteredData" 
            :key="item.id" 
            class="shop-item"
          >
            <div class="shop-content" >
              <div class="shop-icon-wrapper">
                <van-icon name="shop-o" class="shop-icon" @click="handleClick(item.id)" />
              </div>
              
              <div class="shop-details">
                <div class="shop-name" >{{ item.name }}</div>
                <div class="shop-address">
                  <van-icon name="location-o" size="12" />
                  <span>{{ item.location }}</span>
                </div>
              </div>
              
              <div class="shop-amount">
                ¥{{ item.arrears }}
              </div>
              
              <div class="shop-actions">
                <button class="edit-button" @click.stop="handleEdit(item)">
                  <van-icon name="edit" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <van-popup
      v-model:show="show"
      position="bottom"
      round
      closeable
      :style="{ height: '40%' }"
    >
      <div class="popup-header">
        编辑欠款
      </div>
      <div class="popup-content">
        <van-cell-group inset>
          <van-field
            v-model="newItem.name"
            disabled
            label="商家名称"
            label-width="80px"
          >
            <template #left-icon>
              <van-icon name="shop-o" class="field-icon" />
            </template>
          </van-field>
          <van-field
            v-model="newItem.arrears"
            label="欠款金额"
            placeholder="请输入欠款金额"
            required
            type="number"
            label-width="80px"
          >
            <template #left-icon>
              <van-icon name="balance-o" class="field-icon" />
            </template>
            <template #right-icon>
              <span class="amount-unit">元</span>
            </template>
          </van-field>
        </van-cell-group>
        
        <div class="popup-actions">
          <van-button round block type="primary" @click="saveNewItem">确认保存</van-button>
          <van-button round block type="default" @click="show=false">取消</van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script>
import api from "@/api/index.js";
import {ROUTE_NAMES} from "@/constants/routeNames.js";
import { showConfirmDialog, showSuccessToast, showLoadingToast } from "vant";

export default {
  name: "ArrearsData",
  data() {
    return {
      TableData: [],
      show: false,
      newItem: null,
      searchText: ''
    }
  },
  computed: {
    totalArrears() {
      return this.TableData.reduce((sum, item) => sum + Number(item.arrears), 0).toFixed(2);
    },
    filteredData() {
      if (!this.searchText) return this.TableData;
      const searchLower = this.searchText.toLowerCase();
      return this.TableData.filter(item => 
        item.name.toLowerCase().includes(searchLower) || 
        item.location.toLowerCase().includes(searchLower)
      );
    }
  },
  mounted() {
    this.getArrears();
    this.setupPageHeight();
    window.addEventListener('resize', this.setupPageHeight);
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.setupPageHeight);
  },
  methods: {
    setupPageHeight() {
      // 动态计算可滚动区域的高度
      const vh = window.innerHeight;
      document.documentElement.style.setProperty('--vh', `${vh}px`);
    },
    handleClick(id) {
      this.$router.push({
        name: ROUTE_NAMES.SHOP_DETAIL,
        query: {
          id: id
        }
      })
    },
    async getArrears() {
      const loading = showLoadingToast({
        message: '加载中...',
        forbidClick: true,
        duration: 0
      });
      try {
        this.TableData = await api.shop.arrears();
      } catch (error) {
        console.error('获取欠款数据失败', error);
      } finally {
        loading.close();
      }
    },
    handleEdit(item) {
      this.show = true;
      this.newItem = {...item};
    },
    async saveNewItem() {
      showConfirmDialog({
        title: '确认修改',
        message: `确定将 ${this.newItem.name} 的欠款修改为 ${this.newItem.arrears} 元吗？`
      })
      .then(async () => {
        const loading = showLoadingToast({
          message: '保存中...',
          forbidClick: true,
          duration: 0
        });
        
        try {
          await api.shop.update([{id: this.newItem.id, arrears: this.newItem.arrears}]);
          this.show = false;
          await this.getArrears();
          showSuccessToast('保存成功');
        } catch (error) {
          console.error('保存失败', error);
        } finally {
          loading.close();
        }
      })
      .catch(() => {
        // 用户取消，无需操作
      });
    }
  }
}
</script>

<style scoped>
/* 基本页面样式 */
.arrears-page {
  min-height: 100vh;
  height: 100%;
  background-color: #f7f8fa;
  overflow: hidden;
  position: relative;
}

/* 页面容器 - 固定布局 */
.page-container {
  display: flex;
  flex-direction: column;
  height: calc(var(--vh, 100vh) - 46px); /* 减去导航栏高度 */
  padding: 16px;
  box-sizing: border-box;
  overflow: hidden;
}

/* 固定部分 */
.fixed-section {
  flex-shrink: 0;
  width: 100%;
}

/* 可滚动部分 */
.scroll-section {
  flex: 1;
  overflow-y: auto;
  margin-top: 12px;
  padding: 2px; /* 防止边缘阴影被裁切 */
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

/* 间距控制 */
.summary-section {
  margin-bottom: 16px;
}

.search-section {
  margin-bottom: 16px;
}

.list-header-section {
  margin-bottom: 0;
}

/* 总欠款卡片样式 */
.summary-card {
  background: linear-gradient(135deg, #4481eb, #04befe);
  border-radius: 12px;
  color: white;
  padding: 18px;
  box-shadow: 0 8px 16px rgba(4, 190, 254, 0.15);
}

.summary-content {
  position: relative;
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.summary-label {
  font-size: 14px;
  font-weight: 500;
}

.summary-icon {
  font-size: 24px;
  opacity: 0.8;
}

.summary-amount {
  font-size: 32px;
  font-weight: bold;
  margin: 10px 0;
}

.summary-footer {
  font-size: 12px;
  opacity: 0.8;
}

/* 搜索框样式 */
:deep(.van-search) {
  padding: 0;
  background: transparent;
}

:deep(.van-search__content) {
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

/* 列表标题样式 */
.list-header {
  padding: 14px 16px;
  font-size: 16px;
  font-weight: 500;
  background: white;
  border-radius: 12px 12px 0 0;
  border-bottom: 1px solid #f5f5f5;
}

/* 商家列表样式 */
.shop-list {
  padding: 8px 12px;
}

.shop-item {
  margin-bottom: 8px;
}

.shop-item:last-child {
  margin-bottom: 0;
}

.shop-content {
  display: flex;
  align-items: center;
  padding: 12px;
  background-color: white;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  transition: all 0.2s;
}

.shop-content:active {
  transform: scale(0.98);
  background-color: #f9f9f9;
}

.shop-icon-wrapper {
  margin-right: 12px;
}

.shop-icon {
  font-size: 20px;
  color: #4481eb;
  background: rgba(68, 129, 235, 0.1);
  padding: 8px;
  border-radius: 50%;
}

.shop-details {
  flex: 1;
  min-width: 0;
}

.shop-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.shop-address {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.shop-amount {
  font-size: 18px;
  font-weight: bold;
  color: #ff6b6b;
  margin: 0 12px;
}

.shop-actions {
  display: flex;
  align-items: center;
}

.edit-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: #eef2fd;
  color: #4481eb;
  cursor: pointer;
  transition: all 0.2s;
}

.edit-button:active {
  background: #4481eb;
  color: white;
}

/* 弹窗样式 */
.popup-header {
  text-align: center;
  font-size: 18px;
  font-weight: 500;
  padding: 20px 0 10px;
  border-bottom: 1px solid #f5f5f5;
}

.popup-content {
  padding: 20px;
}

.field-icon {
  color: #4481eb;
}

.amount-unit {
  color: #969799;
}

.popup-actions {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

:deep(.van-field__label) {
  color: #323233;
}

:deep(.van-empty) {
  padding: 32px 0;
}

:deep(.van-nav-bar__title) {
  font-weight: 500;
}

/* 响应式调整 */
@media (min-width: 768px) {
  .page-container {
    max-width: 900px;
    margin: 0 auto;
  }
  
  /* 在较大屏幕上可以两列显示商家项 */
  .shop-list {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
  }
  
  .shop-item {
    width: calc(50% - 6px);
    margin-bottom: 12px;
  }
}

/* 针对较短屏幕的优化 */
@media (max-height: 667px) {
  .summary-card {
    padding: 14px;
  }
  
  .summary-amount {
    font-size: 28px;
    margin: 8px 0;
  }
  
  .search-section {
    margin-bottom: 12px;
  }
  
  .summary-section {
    margin-bottom: 12px;
  }
}

/* 针对较高屏幕的优化 */
@media (min-height: 812px) {
  .summary-card {
    padding: 22px;
  }
  
  .summary-amount {
    font-size: 36px;
    margin: 12px 0;
  }
  
  .shop-content {
    padding: 16px;
  }
}
</style>