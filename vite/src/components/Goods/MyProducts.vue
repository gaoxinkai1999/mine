<template>
  <div>
    <van-nav-bar
        title="商品列表"
        right-text="新建商品"
        @click-right="onClickRight"
    />


    <van-cell-group inset>
      <!-- 允许输入数字，调起带符号的纯数字键盘 -->
      <van-cell v-for="product in products" :key="product.id">
        <template #title>
          <span>{{ product.name }}</span>
        </template>
        <template #right-icon>
          <div style="display: flex;gap: 8px;">
            <van-button icon="arrow-up" @click="up(product)"></van-button>
            <van-button icon="arrow-down" mini @click="down(product)"></van-button>
            <van-button icon="arrow" mini @click="goToInfo(product)"></van-button>
          </div>

        </template>

      </van-cell>
    </van-cell-group>


  </div>
</template>

<script>
import {Dialog, Toast} from "vant";

export default {
  name: "MyProducts",
  data() {
    return {
      products: [],

    }
  },

  created() {
    this.getAllGoods()
  },
  methods: {
    getAllGoods() {
      this.$http.post('/product/findByIsDelFalse').then(res => {
        this.products = res.data
      })
    },
    up(product) {

      Dialog.confirm({
        title: '确认将"' + product.name + '"上移',
        message: '',
      })
          .then(() => {
            // on confirm
            const a = this.products.indexOf(product)
            if (a === 0) {
              Toast.fail('已经排在第一个');
            } else {
              const last = this.products[a - 1]
              this.swap(product, last)
              this.sort()
              this.updateIndex(product, last)
            }

          })
          .catch(() => {
            // on cancel
          });

    },
    //更新index
    updateIndex(a, b) {
      this.$http.post('/product/update', a).then(() => {

      })
      this.$http.post('/product/update', b).then(() => {
      })
    },

    // 交换位置
    swap(a, b) {
      const temp = a.index
      a.index = b.index
      b.index = temp
    },
    down(product) {

      Dialog.confirm({
        title: '确认将"' + product.name + '"下移',
        message: '',
      })
          .then(() => {
            // on confirm
            const a = this.products.indexOf(product)
            if (a === this.products.length - 1) {
              Toast.fail('已经排在最后一个');
            } else {
              const next = this.products[a + 1]
              this.swap(product, next)
              this.sort()
              this.updateIndex(product, next)
            }

          })
          .catch(() => {
            // on cancel
          });

    },
    // 排序
    sort() {
      this.products.sort((a, b) => {
        return a.index - b.index
      })
    },

    onClickRight() {
      this.$router.push({
        path: '/AddGoods'
      })
    },
    goToInfo(product) {
      this.$router.push({
        path: '/ProductInfo',
        query: {
          id: product.id
        }
      });
    },
  }


}
</script>

<style scoped>


</style>
