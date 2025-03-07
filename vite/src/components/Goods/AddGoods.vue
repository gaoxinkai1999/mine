<template>
<div>
  <van-nav-bar
      title="添加商品"
  />
  <van-form @submit="onSubmit">
    <van-field
        v-model="goods.name"
        name="商品名"
        label="商品名"
        placeholder="商品名"
        size="large"
        :rules="[{ required: true, message: '请填写商品名' }]"
    />
    <van-field v-model="goods.costPrice" type="number" label="成本价" />

    <van-field v-model="goods.defaultSalePrice" type="number" label="售价" />




    <div style="margin: 16px;">
      <van-button round block type="info" native-type="submit">提交</van-button>
    </div>
  </van-form>
</div>
</template>

<script>
import {Dialog, Toast} from "vant";

export default {
  name: "AddGoods",
  data(){
    return{
      goods:{
        name:'',
        costPrice:'',
        defaultSalePrice:'',

      }
    }
  },
  methods:{
    onSubmit(){
      Dialog.confirm({
        title: '确认提交',
        message: '',
      })
          .then(() => {
            // on confirm
            this.$http.post('/product/add',this.goods).then(res=>{
              console.log(res)
              Toast('提交成功');
              this.$router.push('/MyProducts')
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
