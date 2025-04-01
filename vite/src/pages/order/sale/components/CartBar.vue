<template>
  <div class="cart-bar">
    <div class="cart-icon" :class="{'has-items': totalCount > 0}" >
      <van-icon name="shopping-cart-o" />
      <div v-if="totalCount > 0" class="cart-badge">{{totalCount}}</div>
    </div>
    
    <div class="cart-info">
      <div v-if="totalCount > 0" class="total-price">¥ {{store.totalPrice.toFixed(2)}}</div>
      <div v-else class="cart-tip">未选购商品</div>
    </div>
    
    <van-button 
      class="checkout-btn" 
      type="primary" 
      :disabled="totalCount === 0"
      @click="store.submitOrder()"
    >
      去结算
    </van-button>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useOrderStore } from '@/stores/order';
import {storeToRefs} from 'pinia';

const store = useOrderStore();


const { totalCount } = storeToRefs(store)

</script>

<style scoped>
.cart-bar {
  display: flex;
  align-items: center;
  background: #fff;
  height: 50px;
  width: 100%;
  box-shadow: 0 -1px 8px rgba(0, 0, 0, 0.05);
  z-index: 500; /* 确保高于其他组件但低于弹窗 */
  padding-bottom: env(safe-area-inset-bottom, 0);
  box-sizing: content-box;
  position: relative; /* 添加相对定位 */
}

.cart-icon {
  width: 56px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cart-icon .van-icon {
  font-size: 24px;
  color: #969799;
}

.cart-icon.has-items .van-icon {
  color: #1989fa;
}

.cart-badge {
  position: absolute;
  top: 0;
  right: 9px;
  background: #ee0a24;
  color: #fff;
  font-size: 12px;
  border-radius: 10px;
  padding: 0 5px;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  text-align: center;
  transform: translate(50%, -30%);
}

.cart-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-left: 8px;
}

.total-price {
  font-size: 18px;
  font-weight: 500;
  color: #ee0a24;
  line-height: 1.2;
}

.cart-tip {
  font-size: 12px;
  color: #969799;
  line-height: 1.2;
}

.checkout-btn {
  margin-left: 5px;
  margin-right: 12px;
  height: 35px;
  font-weight: 500;
  border-radius: 18px;
}

@media (max-width: 375px) {
  .cart-bar {
    height: 48px;
  }
  
  .cart-icon {
    width: 48px;
  }
  
  .cart-icon .van-icon {
    font-size: 22px;
  }
  
  .total-price {
    font-size: 16px;
  }
  
  .checkout-btn {
    font-size: 14px;
    height: 32px;
    margin-right: 10px;
  }
}

@media (min-width: 768px) {
  .cart-bar {
    height: 56px;
    padding: 0 8px;
  }
  
  .cart-icon {
    width: 64px;
  }
  
  .cart-icon .van-icon {
    font-size: 28px;
  }
  
  .total-price {
    font-size: 20px;
  }
  
  .checkout-btn {
    height: 40px;
    min-width: 120px;
    margin-right: 16px;
  }
}
</style>