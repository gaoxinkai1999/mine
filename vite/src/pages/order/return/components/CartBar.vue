<template>
  <div class="cart-bar">
    <div class="cart-icon" :class="{'has-items': totalCount > 0}" @click="showCartPopup">
      <van-icon name="shopping-cart-o" />
      <div v-if="totalCount > 0" class="cart-badge">{{totalCount}}</div>
    </div>
    
    <div class="cart-info">
      <div v-if="totalCount > 0" class="total-price">¥ {{totalPrice.toFixed(2)}}</div>
      <div v-else class="cart-tip">未选择退货商品</div>
    </div>
    
    <van-button 
      class="checkout-btn" 
      type="primary" 
      :disabled="totalCount === 0"
      @click="$emit('submit')"
    >
      去退货
    </van-button>
  </div>
</template>

<script setup>

import {useReturnOrderStore} from "@/stores/returnOrder.js";
import {computed} from "vue";
import {storeToRefs} from "pinia";

const store = useReturnOrderStore();
const totalPrice = computed(() => store.totalPrice);
const totalCount = computed(() => store.totalCount);


const {showCart}= storeToRefs(store)

const showCartPopup = () => {
  showCart.value = !showCart.value;
};
</script>

<style scoped>
.cart-bar {
  display: flex;
  align-items: center;
  background: linear-gradient(to right, #ff9500, #ff8800); /* 退货系统使用橙色渐变背景 */
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
  color: #fff; /* 退货系统购物车图标使用白色 */
}

.cart-icon.has-items .van-icon {
  color: #fff; /* 退货系统购物车图标使用白色 */
}

.cart-badge {
  position: absolute;
  top: 0;
  right: 9px;
  background: #fff; /* 退货系统徽章背景使用白色 */
  color: #ff8800; /* 退货系统徽章文字使用橙色 */
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
  color: #fff; /* 退货系统价格文字使用白色 */
  line-height: 1.2;
}

.cart-tip {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8); /* 退货系统提示文字使用半透明白色 */
  line-height: 1.2;
}

.checkout-btn {
  margin-left: 5px;
  margin-right: 12px;
  height: 35px;
  font-weight: 500;
  border-radius: 18px;
  background: #fff !important; /* 退货系统按钮背景使用白色 */
  color: #ff8800 !important; /* 退货系统按钮文字使用橙色 */
  border-color: #fff !important; /* 退货系统按钮边框使用白色 */
}

.checkout-btn.van-button--disabled {
  background: rgba(255, 255, 255, 0.6) !important; /* 退货系统禁用按钮使用半透明白色 */
  color: rgba(255, 136, 0, 0.6) !important; /* 退货系统禁用按钮文字使用半透明橙色 */
  border-color: rgba(255, 255, 255, 0.6) !important;
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