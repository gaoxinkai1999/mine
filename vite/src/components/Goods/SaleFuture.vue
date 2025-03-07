<template>
  <div>
    <van-nav-bar
        title="销量预测(测试版)"
    />





    <van-field
        v-model="Days"
        type="digit"
        center
        clearable
        label="未来天数"

    >
      <template #button>
        <van-button size="small" type="primary" @click="getForecastResult">执行预测</van-button>
      </template>
    </van-field>
    <div v-if="ForecastResult!=null" style="text-align: center">
      <div>五香花生米:{{ ForecastResult['五香花生米'] }}</div>
      <div>麻辣花生米:{{ ForecastResult['麻辣花生米'] }}</div>
      <div>蚕豆:{{ ForecastResult['蚕豆'] }}</div>
      <div>辣椒:{{ ForecastResult['辣椒'] }}</div>
      <div></div>
      <div>总利润:{{ ForecastResult['总利润'] }}</div>
    </div>
    <van-cell title="当前库存可维持天数">
      <!-- 使用 right-icon 插槽来自定义右侧图标 -->
      <template right-icon>
        <van-button size="small" type="primary" @click="getPredictStockDays">执行预测</van-button>
      </template>
    </van-cell>
    <div v-if="PredictStockDays!=null">
      <div>五香花生米:{{ PredictStockDays['五香花生米'] }}</div>
      <div>麻辣花生米:{{ PredictStockDays['麻辣花生米'] }}</div>
      <div>蚕豆:{{ PredictStockDays['蚕豆'] }}</div>
      <div>辣椒:{{ PredictStockDays['辣椒'] }}</div>
    </div>


  </div>
</template>


<script>
export default {
  // eslint-disable-next-line vue/multi-word-component-names
  name: "SaleFuture",
  data() {
    return {
      Days: 4,
      ForecastResult: null,
      PredictStockDays: null
    };
  },
  methods: {
    getForecastResult() {
      this.$http.post('getForecastResult', {'Days': this.Days}).then(res => {
        this.ForecastResult = res.data
      })

    },
    getPredictStockDays() {
      this.$http.post('getPredictStockDays').then(res => {
        this.PredictStockDays = res.data
      })
    }

  },
}
</script>


<style scoped>

</style>