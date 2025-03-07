<template>
<div >
  <van-nav-bar
      title="订单信息"
      right-text="修改"
      @click-right="showSet=true"
  />
  <van-cell-group inset title="基本信息">
    <van-cell title="订单时间" :value="order.create_time"   size="large"/>
    <van-cell title="商家名" :value="order.shop_name"  size="large" />

  </van-cell-group>


  <van-field name="stepper" center :label=item.goods_name :value="'×'+item.num"   size="large" v-for="(item,index) in order.orderGoods"  :key="index">
    <template #input v-if="showSet">
      <van-stepper v-model="item.num" theme="round" button-size="22" disable-input min=0 />
    </template>

  </van-field>
  <van-button round block type="info" @click="submit" v-if="showSet">提交</van-button>


<!--  <van-cell-group inset title=" ">-->
<!--    <van-cell  center title="累计金额" :value="'￥'+order.all_sum_price" size="large" />-->
<!--  </van-cell-group>-->
  </div>

</template>

<script>
import {Dialog, Toast} from "vant";

export default {
  name: "OrderInfo",
  data(){
    return{
      order:'',
      showSet:false
    }
  },
  created() {
    this.$http.post("/getOrderById",{id:this.$route.query.id}).then(res=>{
      this.order=res.data
    })
  },
  methods:{
    submit(){
      Dialog.confirm({
        title: '是否确认提交',
        message: '',
      })
          .then(() => {
            this.$http.post('/setOrder',this.order)
            Toast.success('提交成功');
            this.showSet=false

          })
          .catch(() => {
            // on cancel
          });
    }
  }
}
</script>

<style scoped>

</style>
