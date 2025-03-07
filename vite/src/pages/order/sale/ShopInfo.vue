<template>
  <div class="shop-info">
    <div class="shop-info__content" @click="showPicker=!showPicker">
      <div class="shop-info__header">
        <h2 class="shop-name">{{ currentShop?.name || '加载中...' }}</h2>
        <van-icon name="arrow"/>
      </div>

      <div class="shop-info__details">
        <div class="info-item">
          <span>{{ currentShop?.address || '' }}</span>
        </div>

        <div class="info-row">
          <div class="info-item">
            <van-icon name="guide-o"/>
            <span>距离 {{ Math.round(currentShop?.distance || 0) }}米</span>
          </div>
          <div class="info-item">
            <van-icon name="clock-o"/>
            <span>营业时间 09:00-22:00</span>
          </div>
        </div>

        <div class="info-item">
          <van-icon name="info-o"/>
          <span>{{ currentShop?.notice || '欢迎光临，请尽情选购' }}</span>
        </div>
      </div>
    </div>
  </div>

  <van-popup 
    v-model:show="showPicker" 
    position="bottom" 
    :style="{ height: '80%' }" 
    :z-index="3000"
    :overlay-style="{ zIndex: 2900 }"
  >
    <van-nav-bar left-text="关闭" title="选择商家" @click-left="showPicker=false"/>
    <van-tabs v-model:active="active">
      <van-tab title="附近商家">
        <nearShopList @selectShop="receiveDataFromChild"></nearShopList>
      </van-tab>
      <van-tab title="全部商家">
        <ShopListItem @selectShop="receiveDataFromChild"></ShopListItem>
      </van-tab>
    </van-tabs>
  </van-popup>
</template>

<script setup>
import {useOrderStore} from '@/stores/order'
import {storeToRefs} from 'pinia'
import {ref} from 'vue'
import ShopListItem from '@/components/Shop/ShopListItem.vue'
import NearShopList from "@/components/Shop/nearShopList.vue";

const store = useOrderStore()
const {currentShop} = storeToRefs(store)
const showPicker = ref(false)
const active = ref(0);

const receiveDataFromChild = (data) => {
  currentShop.value = data
  showPicker.value = false
  // 处理从子组件传来的数据
  console.log(data)
}
</script>

<style scoped>
.shop-info {
  width: 100%;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #f5f5f5;
}

.shop-info__content {
  position: relative;
  cursor: pointer;
}

.shop-info__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.shop-name {
  font-size: 18px;
  font-weight: 600;
  color: #323233;
  margin: 0;
}

.shop-info__details {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-row {
  display: flex;
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 14px;
  flex-wrap: wrap;
}

.info-item .van-icon {
  color: #969799;
  font-size: 16px;
  flex-shrink: 0;
}

/* 响应式调整 */
@media (max-width: 375px) {
  .shop-info {
    padding: 8px 12px;
  }
  
  .shop-name {
    font-size: 16px;
  }
  
  .shop-info__details {
    gap: 4px;
  }
  
  .info-item {
    font-size: 12px;
  }
  
  .info-item .van-icon {
    font-size: 14px;
  }
}

/* 店铺选择弹出层样式 */
:deep(.van-popup) {
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

:deep(.van-tabs__content) {
  flex: 1;
  overflow-y: auto;
}

:deep(.van-tab) {
  font-size: 15px;
}
</style>