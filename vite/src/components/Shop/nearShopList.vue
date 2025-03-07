<template>

  <van-cell v-for="shop in nearbyShops" :title="shop.name" :value="shop.id" border center
            @click="handleClick(shop)">
    <template #label><span class="shop-id">{{ shop.address }}</span></template>
    <template #value><span class="shop-id">{{ Math.round(shop.distance) }}米</span></template>
  </van-cell>

</template>
<script setup>
import {useReturnOrderStore} from "@/stores/returnOrder.js";
import {storeToRefs} from "pinia";

const returnOrderStore = useReturnOrderStore();
const {nearbyShops} = storeToRefs(returnOrderStore);

// 定义可触发的事件
const emit = defineEmits(['selectShop']);


const handleClick = (shop) => {
  emit('selectShop', shop);
}

</script>


<style scoped>

</style>