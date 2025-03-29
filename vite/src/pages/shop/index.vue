<route>
{
name: "shop-list"
}
</route>
<template>

  <div>
    <van-nav-bar
        left-text="返回"
        right-text="添加商家"
        left-arrow
        title="商家列表"
        @click-left="onClickLeft"
        @click-right="onClickRight"
    />
    <van-cell :value="this.totalShops" title='合计'/>
    <ShopListItem @total-change="handleTotalChange" @selectShop="goToInfo"></ShopListItem>


  </div>
</template>

<script>

import ShopListItem from "@/components/Shop/ShopListItem.vue";
import {ROUTE_NAMES as routeNames} from "@/constants/routeNames.js";

export default {
  name: "MyShop",
  components: {ShopListItem},
  data() {
    return {
      totalShops: 0
    }
  },
  methods: {
    // 接收子组件传递的总数
    handleTotalChange(total) {
      this.totalShops = total;
    },


    //到指定商家详细信息
    goToInfo(shop) {
      this.$router.push({
        name: routeNames.SHOP_DETAIL,
        query: {
          id: shop.id
        }
      });

    },
    onClickLeft(){
      this.$router.go(-1);
    },
    onClickRight(){
      this.$router.push({
        name: routeNames.SHOP_CREATE,
      })
    }

  },

}
</script>

<style>


</style>
