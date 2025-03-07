<template>
  <div>
    <van-nav-bar
        title="新建规则"
    />
    <van-field
        v-model="PriceRule.name"
        label="规则名"
        placeholder="请输入规则名">

    </van-field>

    <van-cell title="选择标签显示显色" icon="shop-o">
      <!-- 使用 right-icon 插槽来自定义右侧图标 -->
      <template #right-icon>
        <el-color-picker v-model="PriceRule.color"></el-color-picker>
      </template>
    </van-cell>
    <van-cell-group title="价格" >
      <van-field
          v-for="(item,index) in products"
          :key="index"
          v-model="item.defaultSalePrice"
          :label="item.name"
          type="number"
          input-align="center "
          >

      </van-field >

    </van-cell-group>
    <div style="margin: 16px;">
      <van-button round block type="info" @click="addPriceRule">提交</van-button>
    </div>
  </div>
</template>
<script>
export default {
  name: "AddPriceRule",
  data() {
    return {
      PriceRule: {
        name: '',
        priceRuleDetails: [],
        color:null
      },
      products: []

    }
  },
  created() {
    this.getProducts()
  },
  methods: {
    getProducts() {
      this.$http.post('/product/findByIsDelFalse').then(res => {
        this.products = res.data
      })
    },
    addPriceRule() {
      this.PriceRule.priceRuleDetails = this.products.map(item => {
        return {
          product: item,
          price: item.defaultSalePrice,
        }
      })
      this.$http.post('/priceRule/add', this.PriceRule).then(res => {
        console.log(res)
        this.$router.push({
          path: '/Lsit'
        })
      })
    }
  }

}
</script>


<style scoped>

</style>