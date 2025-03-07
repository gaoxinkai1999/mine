<template>
  <div>
    <van-nav-bar
        title="商品详情"
        left-text="编辑商品"
        right-text="删除商品"
        @click-left="openEdit()"
        @click-right="delProduct()"
    />
    <van-field label="ID:" input-align="right" v-model="product.id" readonly></van-field>
    <van-field label="商品名:" input-align="right" v-model="product.name" :readonly="!isEdit"></van-field>
    <van-field label="成本价:" input-align="right" v-model="product.costPrice" readonly></van-field>
    <van-field label="售价:" input-align="right" v-model="product.defaultSalePrice" readonly></van-field>
  </div>
</template>

<script>
import {Dialog} from "vant";

export default {
  name: "ProductInfo",
  data() {
    return {
      product: {},
      // 是否编辑
      isEdit: false
    }
  },
  created() {
    this.getProduct()
  },
  methods: {
    getProduct() {
      this.$http.post('/product/findById', this.$qs.stringify({id: this.$route.query.id})).then(res => {
        this.product = res.data
      })
    },
    delProduct() {
      Dialog.confirm({
        title: '是否确认删除',
        message: '',
      })
          .then(() => {
            // on confirm
            this.$http.post('delGoods', this.$qs.stringify({id: this.product.id})).then(res => {
              console.log(res)
              this.getAllGoods()
            })
          })
          .catch(() => {
            // on cancel
          });
    },
    // 打开编辑模式
    openEdit() {
      this.isEdit = true
    },
    // 保存编辑
    saveEdit() {
      this.$http.post('/product/update', this.$qs.stringify(this.product)).then(res => {
        console.log(res)
        this.isEdit = false
      })
    }
  }


}
</script>


<style scoped>

</style>