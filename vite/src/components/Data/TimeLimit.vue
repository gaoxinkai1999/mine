<template>
  <div>
    <van-nav-bar
        title="范围销售数据"/>

    <van-cell title="选择日期区间" :value="date" @click="show = true"/>
    <van-calendar v-model="show" type="range" @confirm="onConfirm" :min-date="minDate"/>
    <div  style="text-align: center">
      <van-button type="primary" @click="getSalesSummary" v-if="this.date!=''">查询</van-button>
    </div>

    <van-cell-group inset v-if="result!=null">
      <van-cell :title="key" :value="value" v-for=" (value, key, index) in result" :key="index"/>
    </van-cell-group>
  </div>

</template>
<script>


import {Dialog, Toast} from "vant";

export default {
  // eslint-disable-next-line vue/multi-word-component-names
  name: "TimeLimit",
  data() {
    return {
      date: '',
      show: false,
      result:null,
      minDate: new Date(2020, 0, 1), // 允许用户选择的最早日期，例如 2020 年 1 月 1 日

    };
  },
  methods: {
    formatDate(date) {
      const year = date.getFullYear();
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const day = date.getDate().toString().padStart(2, '0');
      return `${year}-${month}-${day}`;
    },

    onConfirm(date) {
      const [start, end] = date;
      this.show = false;
      this.date = `${this.formatDate(start)}至${this.formatDate(end)}`;
    },
    getSalesSummary() {
      Dialog.confirm({
        title: '确认查询',
        message: '',
      })
          .then(() => {
            this.$http.post('/getSalesSummary', {'value': this.date}).then(res => {
              Toast('查询成功');
              this.result=res.data
            })

          })
          .catch(() => {
            // on cancel
          });


    }
  },
}
</script>


<style scoped>

</style>