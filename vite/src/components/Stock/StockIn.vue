<template>
  <div>


    <van-nav-bar
        title="商品采购"
    />

    <van-cell-group title="订单" inset>


      <van-cell name="stepper" v-for="product in products" :key="product.id">
        <template #title>
          <div style="display: flex; align-items: center;">
            <span style="flex-shrink: 0; min-width: 30vw;">{{ product.name }}</span>
            <van-stepper v-model="product.num" theme="round" button-size="22" min=0 default-value="0"/>
            <span style="margin-left: 10vw">{{ amount(product) }}元</span>
          </div>
        </template>

      </van-cell>
    </van-cell-group>
    <van-cell-group title="合计" inset>
      <van-cell title="总金额">
        <template #right-icon>
          <span>{{ totalAmount }}元</span>
        </template>
      </van-cell>
      <div style="margin: 16px;">
        <van-button round block type="info" @click="onSubmit">提交</van-button>
      </div>
    </van-cell-group>
  </div>
</template>

<script>


import {Dialog} from "vant";

export default {
  name: "StockIn",
  data() {
    return {
      products: [],
      stockIn: {
        totalAmount: 0,
        type: '采购下单',
        stockInDetails: []
      },
      totalAmount: 0
    }
  },
  created() {
    this.getProducts()
  },
  computed: {
    amount: function () {
      return  function (product){
        // 计算 product.amount
        product.amount = Math.round(product.num * product.costPrice * 100) / 100; // 保留两位小数
        // 计算 totalAmount
        this.totalAmount = this.products.reduce((total, product) => {
          // 将每个产品的金额转换为整数，并累加到总金额上
          return total + Math.round(product.amount * 100);
        }, 0) / 100; // 将累加的整数总金额转换回浮点数，并保留两位小数

        return product.amount;
      };
    },

  },
  methods: {
    //获取全部商品
    getProducts() {
      this.$http.post('/product/findByIsDelFalse').then(res => {
        this.products = res.data
      })
    },
    onSubmit() {
      //判断商品数量是否全部为0
      const allNumsAreZero = this.products.every(product => product.num === 0);
      if (allNumsAreZero) {
        this.$toast.fail('请至少选择一个商品');
        return;
      }
      //加工数据并提交
      this.stockIn.stockInDetails=[]
      this.stockIn.totalAmount = this.totalAmount
      this.products.map(product => {
        if (product.num > 0) {
          this.stockIn.stockInDetails.push({
            product: product,
            num: product.num,
            totalAmount: product.amount
          })
        }
      })
      this.save()


    },
    save() {
      Dialog.confirm({
        title: '确认操作',
      })
          .then(() => {
            // on confirm
            //提交订单
            this.$http.post('/stockIn/create', this.stockIn).then(() => {
              this.$toast.success('操作成功');
              this.$router.go(-1);
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

