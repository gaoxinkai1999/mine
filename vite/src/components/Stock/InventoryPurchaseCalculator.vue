<template>
  <div>
    <van-nav-bar
        title="库存采购计算器"/>
    <!-- 允许输入数字，调起带符号的纯数字键盘 -->
    <van-field v-model="money" type="number" label="数字">
      <template #button>
        <van-button size="small" type="primary" @click="demo">提交</van-button>
      </template>
    </van-field>

    <el-table
        :data="result"
        border
        show-summary
    >

      <el-table-column label="商品名" align="center" prop="name"/>
      <el-table-column label="现有库存" align="center" prop="现有库存"/>
      <el-table-column label="进货数量" align="center" prop="进货数量"/>
      <el-table-column label="进货金额" align="center" prop="进货金额"/>
      <el-table-column label="进货后数量" align="center" prop="进货后数量"/>


    </el-table>
  </div>

</template>


<script>
export default {
  name: "InventoryPurchaseCalculator",
  data() {
    return {
      money: 10000,
      result: []
    }
  },
  methods: {
    demo() {
      this.$http.post('/stockIn/getPurchaseQuantity', this.$qs.stringify({money: this.money})).then(res => {
        console.log(res.data)
        this.result = res.data
      })
    }

  }
}
</script>


<style scoped>

</style>