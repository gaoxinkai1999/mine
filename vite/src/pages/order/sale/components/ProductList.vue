<template>
  <div class="product-list">
    <van-empty v-if="store.currentFoods.length === 0" description="没有找到商品"/>
    <van-card
        v-for="(item, index) in store.currentFoods"
        :key="index"
        :price="`${item.defaultSalePrice}`"
        :title="item.name"
    >
      <template #tags>
        <van-tag v-if="item.productStockDTO.totalInventory < 10" plain type="danger">剩{{ item.productStockDTO.totalInventory }}件</van-tag>
        <van-tag v-else plain type="success">库存充足剩{{ item.productStockDTO.totalInventory }}件</van-tag>
      </template>

      <template #footer>
        <van-stepper
            v-model="item.count"
            :max="item.productStockDTO.totalInventory"
            button-size="22"
            min="0"
            theme="round"
            @change="() => handleChange(item)"
        />
      </template>
    </van-card>
  </div>
</template>

<script setup>
import {useOrderStore} from '@/stores/order'

const store = useOrderStore()

const handleChange = (item) => {
  store.updateCart(item)
}
</script>
<style scoped>
.product-list {
  flex: 1;
  min-width: 0; /* 允许内容收缩 */
  padding: 12px;
  overflow-y: auto;
  overflow-x: hidden; /* 明确禁止水平滚动 */
  -webkit-overflow-scrolling: touch;
  width: calc(100% - 80px); /* 减去侧边栏宽度 */
  box-sizing: border-box;
  height: 100%; /* 确保高度为100% */
  position: relative; /* 添加相对定位 */
}

/* 卡片样式优化 */
.product-list :deep(.van-card) {
  margin-bottom: 12px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  position: relative;
  width: 100%;
  box-sizing: border-box;
  padding: 12px;
  min-height: 100px; /* 确保卡片有最小高度 */
  background-color: #fff; /* 确保背景是白色 */
}

/* 内容区域布局优化 */
.product-list :deep(.van-card__content) {
  padding-right: 70px; /* 为步进器留出空间 */
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-sizing: border-box;
}

/* 标题样式优化 */
.product-list :deep(.van-card__title) {
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  margin-bottom: 4px;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2; /* 最多显示两行 */
  -webkit-box-orient: vertical;
  word-break: break-word;
  box-sizing: border-box;
}

/* 标签容器样式 */
.product-list :deep(.van-card__tags) {
  width: 100%;
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  min-height: 20px; /* 确保有足够空间显示标签 */
  box-sizing: border-box;
}

/* 标签样式 */
.product-list :deep(.van-tag) {
  margin: 0;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  box-sizing: border-box;
}

/* 价格样式 */
.product-list :deep(.van-card__price) {
  color: #ff4444;
  font-size: 16px;
  font-weight: bold;
  margin-top: 4px;
  box-sizing: border-box;
}

/* 步进器位置优化 */
.product-list :deep(.van-card__footer) {
  position: absolute;
  right: 12px;
  bottom: 12px;
  width: auto; /* 调整为自动宽度 */
  max-width: 110px;
  z-index: 1; /* 确保步进器在最上层 */
  box-sizing: border-box;
}

/* 步进器样式优化 */
.product-list :deep(.van-stepper) {
  width: auto; /* 调整为自动宽度 */
  max-width: 110px;
  transform: scale(0.9); /* 稍微缩小步进器 */
  transform-origin: right bottom;
  box-sizing: border-box;
}

/* 滚动条样式 */
.product-list::-webkit-scrollbar {
  width: 4px;
}

.product-list::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 2px;
}

.product-list::-webkit-scrollbar-track {
  background: transparent;
}

/* 小屏幕适配 */
@media (max-width: 375px) {
  .product-list {
    width: calc(100% - 70px); /* 小屏幕调整侧边栏宽度 */
    padding: 8px;
  }
  
  .product-list :deep(.van-card) {
    padding: 8px;
  }
  
  .product-list :deep(.van-card__title) {
    font-size: 14px;
  }
  
  .product-list :deep(.van-card__footer) {
    right: 8px;
    bottom: 8px;
  }
}
</style>

