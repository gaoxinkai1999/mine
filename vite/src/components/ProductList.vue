<template>
  <div class="product-list">
    <van-empty v-if="products.length === 0" description="没有找到商品"/>
    <div
        v-for="product in products"
        v-else
        :key="product.id"
        :class="['product-card', { selected: isSelected(product.id) }]"
    >
      <div class="product-info">
        <div class="tag-group">

          <!--          <van-tag type="warning">需要补货</van-tag>-->
          <!--          <van-tag type="success">库存充足</van-tag>-->
        </div>
        <div class="product-name">{{ product.name }}
          <van-tag v-if="product.warningQuantity>=product.currentStock" type="danger">已紧急断货</van-tag>
        </div>
        <div class="product-price">¥{{ formatAmount(product.costPrice) }}</div>
        <div class="product-spec">当前库存: {{ product.currentStock }}</div>
        <div v-if="product.isForecastNormal===true" class="product-spec warning">预警库存: {{
            product.warningQuantity
          }}
        </div>
        <div v-if="product.isForecastNormal===true" class="product-spec success">推荐采购数量:
          {{ product.recommendPurchaseQuantity }}
        </div>
      </div>
      <div class="product-action">
        <div :class="{ 'selected': isSelected(product.id) }" class="product-quantity">
          <template v-if="isSelected(product.id)">
            <van-icon name="shopping-cart-o"/>
            <span>已选 {{ getSelectedQuantity(product.id) }}</span>
          </template>
        </div>

        <van-button
            :icon="isSelected(product.id) ? 'add' : 'shopping-cart-o'"
            round
            size="small"
            type="primary"
            @click="handleSelect(product)"
        >
          {{ isSelected(product.id) ? '增加' : '添加' }}
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import {defineProps, defineEmits} from 'vue';

const props = defineProps({
  products: {
    type: Array,
    required: true
  },
  selectedProducts: {
    type: Array,
    required: true
  }
});

const emit = defineEmits(['product-selected']);

const isSelected = (productId) => {
  return props.selectedProducts.some(item => item.productId === productId);
};

const getSelectedQuantity = (productId) => {
  const item = props.selectedProducts.find(item => item.productId === productId);
  return item ? item.quantity : 0;
};

const handleSelect = (product) => {
  emit('product-selected', product);
};

const formatAmount = (amount) => {
  return Number(amount || 0).toFixed(2);
};
</script>

<style scoped>
.product-list {
  padding: 16px;
}

.product-card {
  margin-bottom: 12px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: space-between;
  transition: all 0.3s;
}

.product-card.selected {
  border: 1px solid var(--van-primary-color);
  background-color: rgba(var(--van-primary-color-rgb), 0.05);
}

.product-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tag-group {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  flex-wrap: wrap;
  width: fit-content;
}

:deep(.van-tag) {
  flex: none;
}

.product-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.product-price {
  font-size: 18px;
  font-weight: 600;
  color: #ee0a24;
}

.product-spec {
  font-size: 13px;
  color: #666;
}

.product-spec.warning {
  color: #d30c0c;
}

.product-spec.success {
  color: #7bc845;
}

.product-action {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-end;
  min-width: 80px;
  padding: 8px 0;
  gap: 8px;
}

.product-quantity {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  color: var(--van-primary-color);
  font-size: 13px;
  height: 20px;
  opacity: 0;
  transition: opacity 0.3s;
}

.product-quantity.selected {
  opacity: 1;
}

.product-quantity :deep(.van-icon) {
  margin-right: 4px;
  font-size: 16px;
}

:deep(.van-button--small) {
  padding: 0 12px;
  height: 28px;
}
</style> 