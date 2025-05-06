
<template>
  <div >
    <van-nav-bar
        title="标题"
        left-text="返回"
        right-text="按钮"
        left-arrow
        @click-left="onClickLeft"
        @click-right="onClickRight"
    />
    <h3 style="text-align: center">商品销售额</h3>
    <ve-line-chart :data="this.productData" :grid="this.grid" :settings="chartSettings" title.text="title.text"/>
    <!--    <h3 style="text-align: center">总利润</h3>-->
    <!--    <ve-line-chart :data="this.totalProfitData"  :grid="this.grid"/>-->
    <!--    <h3 style="text-align: center">商品销售比例</h3>-->
    <!--    <ve-line-chart :data="this.salesProportionData" :grid="this.grid"/>-->
  </div>
</template>

<script>
import api from "@/api";

export default {
  name: "ProductChart",
  data() {
    return {
      movingAverageLineRequest: {
        productIds: [
          87,88, 99,107,108
        ],
        extractorType: "Quantity",
        period: 30
      },


      chartSettings: {
        label: {
          show: true,
          fontSize: '12',
          fontWeight: 'bold',
          formatter: function(value) {
            // 这里的 params.value 是数据点的值
            // 使用 toFixed() 方法来控制小数点后的位数
            return value.value[value.seriesIndex+1]; // 显示两位小数
          }  // 显示两位小数
        },
        smooth: true,
      },
      title: {
        text: '商品销售额',
        textStyle: {
          align: 'center',


          color: '#333',
        }
      },
      grid: { // 设置图表的边距
        top: '10%',    // 上边距
        right: '10%',  // 右边距
        bottom: '10%', // 下边距
        left: '10%',   // 左边距
        containLabel: true // 是否包含标签在内
      },

      show: false,
      productData: {
        dimensions: {
          name: '',
          data: [1123]
        },
        measures: [
          {
            name: '',
            data: [0.1]
          }
        ],

        grid: { // 设置图表的边距
          top: '20%',    // 上边距
          right: '20%',  // 右边距
          bottom: '20%', // 下边距
          left: '20%',   // 左边距
          containLabel: true // 是否包含标签在内
        },
      },
      totalProfitData: {
        dimensions: {
          data: []
        },
        measures: []
      },
      // 销售比例
      salesProportionData: {
        dimensions: {
          name: '',
          data: []
        },
        measures: []
      }
    }
  },

  created() {
    console.log('xxx')
    this.getData()

  },
  methods: {
   async getData() {
     const overallTrend =await api.statistics.getOverallTrend({period:30,startDate:"2024-03-05",endDate:"2025-01-05"});

     this.productData.measures = overallTrend.movingAverageInfoDTOS
     this.productData.dimensions.data = overallTrend.dates


    },
    onClickLeft(){

    },
    onClickRight(){
      this.$router.go(-1);
    }
  }

}
</script>


<style scoped>

</style>