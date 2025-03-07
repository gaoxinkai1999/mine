<template>
  <div>
    <van-nav-bar
        title="采购记录"
        right-text="商品采购"
        @click-right="goToStockIn()"
    />

    <!--      组件详细内容-->

    <van-cell-group inset v-for="(stockIn,index) in stockInData" :key="index"
                    style="margin-bottom: 30px; border: 5px solid rgba(41,138,210,0.4);">、
      <van-cell-group inset >
        <van-cell>
          <template #title>
            <div style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
              <span class="order-time" style="font-size: 17px;color: blue;">ID:{{ stockIn.id }}</span>
              <span class="delete-order" style="color: red;" @click="deleteStockIn(stockIn.id)">删除订单</span>
            </div>
          </template>
        </van-cell>
        <van-cell title="创建时间" :value="stockIn.createTime"></van-cell>
        <van-cell title="完成时间" :value="stockIn.inTime===null?'未完成':stockIn.inTime"></van-cell>
        <van-cell title="当前状态">
          <template #right-icon>
            <van-tag  :type="stockIn.type==='采购下单'?'warning':'primary'" size="large">{{ stockIn.type }}</van-tag>
          </template>

        </van-cell>


      </van-cell-group>
      <!--       订单信息-->
      <van-cell-group inset title="订单">

        <van-cell name="stepper" center
                  size="small"
                  v-for="stockInDetail in stockIn.stockInDetails" :key="stockInDetail.id"
                  :value="stockInDetail.totalAmount+'元'"
                  style="background-color: rgba(204,255,153,0.48);">

          <template #title>
            <!-- 使用flex布局 -->
            <div style="display: flex; align-items: center;">
              <!-- 设置固定宽度以保持位置稳定 -->
              <span style="flex-shrink: 0; margin-right:8px; min-width: 100px;">{{ stockInDetail.product.name }}</span>
              <span style="flex-shrink: 0; margin-right:8px;">x{{ stockInDetail.num }}</span>
            </div>
          </template>
        </van-cell>

        <van-cell style="background-color: rgba(143,218,222,0.44);margin-top: 1vh;margin-bottom: 1vh"
                  center
                  title="总价"
                  :value="stockIn.totalAmount+'元'"/>

        <div style="margin: 16px;" v-if="stockIn.type==='采购下单'">
          <van-button round block type="info" @click="start(stockIn.id)">入库</van-button>
        </div>

      </van-cell-group>


    </van-cell-group>


  </div>
</template>

<script>
import qs from "qs";
import {Dialog} from "vant";

export default {
  name: "StockInData",
  data() {
    return {
      stockInData: []
    }
  },
  created() {
    this.getStockInData()
  },
  methods: {
    goToStockIn() {
      this.$router.push('/stockIn')
    },
    getStockInData() {
      // 获取采购记录
      this.$http.post('/stockIn/findAll').then(res => {
        this.stockInData = res.data
      })
    },
    deleteStockIn(id){
      Dialog.confirm({
        title: '确认操作',
      })
          .then(() => {
            // on confirm
            this.$http.post('/stockIn/delete', qs.stringify({id: id})).then(() => {
              this.getStockInData()
            })
          })
          .catch(() => {
            // on cancel
          });

    },
    start(id) {

      Dialog.confirm({
        title: '确认操作',
      })
          .then(() => {
            // on confirm
            this.$http.post('/stockIn/in', qs.stringify({id: id})).then(() => {
                this.getStockInData()
            })
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