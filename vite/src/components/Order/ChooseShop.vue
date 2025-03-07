<template>
  <div>
    <van-nav-bar title="选择商家" placeholder fixed />
    <van-tabs v-model="active" @rendered="rendered">
      <van-tab title="附近商家">
        <van-cell
            v-for="shop in shops"
            :key="shop.id"
            border
            center
            :label="shop.location"
            @click="goToNewOrder(shop.id)"
        >
          <template #title>
            <div style="display: flex; align-items: center;">
              <span style="flex-shrink: 0; min-width: 35vw;">{{ shop.name }}</span>
              <van-tag :color="shop.priceRule.color">{{ shop.priceRule.name }}</van-tag>
            </div>
          </template>
        </van-cell>
      </van-tab>
      <van-tab title="全部商家">
        <ShopList :parentMethod="goToNewOrder"></ShopList>
      </van-tab>
    </van-tabs>
  </div>
</template>

<script>
import ShopList from "@/components/Shop/ShopListItem.vue";
import { getCurrentLocation } from "@/components/new/amapService.js";

export default {
  name: "ChooseShop",
  components: { ShopList },
  data() {
    return {
      active: 0,
      shops: [],
    };
  },
  methods: {
    // 跳转路由到新建订单
    goToNewOrder(ShopId) {
      console.log("跳转到新建订单");
      this.$router.push({
        path: "/NewOrder",
        query: {
          ShopId: ShopId,
        },
      });
    },
    rendered(name, title) {
      console.log(name, title);
    },
    async fetchNearbyShops() {
      try {
        this.$loading("加载中");

        // 获取当前位置
        const position = await getCurrentLocation();



        // 调用后端 API 获取附近商家
        const response = await this.$http.post(
            "/shop/findNearShop",
            this.$qs.stringify({
              longitude: position.longitude,
              latitude: position.latitude,
            })
        );

        this.shops = response.data;
      } catch (error) {
        console.error("获取位置或商家失败", error);
        this.$toast.fail("加载失败，请检查定位服务");
      } finally {
        this.$loading().close();
      }
    },
  },
  mounted() {
    this.fetchNearbyShops();
  },
};
</script>

<style scoped></style>
