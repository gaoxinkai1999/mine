<template>
  <div>
    <van-nav-bar
        right-text="删除商家"
        title="修改商家"
        @click-right="delShop()"
    />

    <van-cell-group inset title="基本信息">


      <van-field v-model="shop.location" label="地区" size="large"/>


      <van-field v-model="shop.name" label="商家名" size="large"/>



      <div style=" margin-top:3vh">
        <div style="display: flex;justify-content:space-around;">
          <van-button type="success" @click="saveChanges">保存</van-button>
          <van-button type="warning" @click="cancelChanges">取消</van-button>
        </div>

      </div>

    </van-cell-group>


  </div>


</template>
<script>
import {Dialog, Toast} from "vant";
import api from "@/api";
import {ROUTE_NAMES} from "@/constants/routeNames.js";

export default {
  name: "SetShop",
  data() {
    return {
      shop: {}
    }
  },
  mounted() {
    this.getShop()
  },
  methods: {
    async getShop() {
      this.shop = await api.shop.getShop({id: this.$route.query.id})


    },

    delShop() {

      Dialog.confirm({
        title: '是否确认删除',
        message: '',
      })
          .then(() => {
            // on confirm
            this.$http.post('shop/updateIsDelById', this.$qs.stringify({id: this.shop.id})).then(() => {
              Toast.success('删除成功');
              this.$router.push({
                name: ROUTE_NAMES.SHOP_LIST,
              })
            })


          })
          .catch(() => {
            // on cancel
          });

    },
    goToInfo() {
      this.$router.push({
        name: ROUTE_NAMES.SHOP_DETAIL,
        query: {
          id: this.shop.id
        }
      });
    },
    saveChanges() {
      this.$http.post("/shop/updateShopInfo", this.shop).then(() => {
        Toast.success("保存成功")
        this.goToInfo()
      })

    },
    cancelChanges() {
      this.goToInfo()
    }
  }
}
</script>


<style scoped>

</style>