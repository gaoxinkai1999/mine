<template>
  <div>
    <van-nav-bar
        title="新建其他订单"
    />
    <!--    选择商家-->
    <van-field
        readonly
        clickable
        label="商家"
        placeholder="选择商家"
        @click="showPicker = true"
    >
      <template #input v-if="shop.id!==undefined">
        <span> ({{ shop.location }}){{ shop.name }}

           <van-tag type="danger" v-if="shop.isDel">弃用</van-tag>
        </span>
      </template>
    </van-field>
    <!--    商家选择器-->
    <van-popup v-model="showPicker" round position="bottom">
      <van-picker
          show-toolbar
          :columns="shops"
          @confirm="onConfirmShop"
          @cancel="showPicker=false"
          :visible-item-count="10"
          :swipe-duration="100"
      />
    </van-popup>
    <!--    选择商品-->


    <!--    输入退货金额-->


    <!--    提交-->
    <div style="margin: 16px;">
      <van-button round block type="info" @click="onSubmit">确认筛选条件</van-button>
    </div>
  </div>

</template>

<script>
export default {
  name: "SaveReturnOrder",
  data() {
    return {
      showPicker: false,
      shops: [],
      products: [],
      order:{

      }
    }
  },
  methods: {
    // 获取全部商家
    getShops() {
      const res = this.$http.get('/shop/all')
      this.shops = res.data
    },
    //获取全部商品
    getProducts() {
      this.$http.get('/goods/all').then(res => {
        this.goods = res.data
      })

    },
    onConfirmShop(value, index) {
      this.shop = this.shops[index[0]].children[index[1]].value
      this.form.shopId = this.shop.id
      this.showPicker = false
    },
    onSubmit() {
      // 保存数据到 localStorage
      this.$router.push({
        path: '/AllOrder',
        query: {
          shopId: this.form.shopId,
          localDate: this.form.localDate,
        }
      })
      localStorage.setItem('shopId', this.$qs.stringify(this.form.shopId));
      localStorage.setItem('shopName', this.$qs.stringify('(' + this.shop.location + ')' + this.shop.name));
      localStorage.setItem('localDate', this.$qs.stringify(this.form.localDate));

    },
  }
}
</script>


<style scoped>

</style>