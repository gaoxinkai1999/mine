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
    //跳转路由到添加商家
    goToAdd() {
      this.$router.push({
        path: '/Create',
      })
    },

    //到指定商家详细信息
    goToInfo(shop) {
      this.$router.push({
        path: '/shop/info',
        query: {
          id: shop.id
        }
      });

    },
    onClickLeft(){
      this.$router.go(-1);
    },

  },

}
</script>

<style>


</style>
