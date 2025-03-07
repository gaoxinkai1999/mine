<template>
  <div>
    <van-nav-bar
        title="月销售数据"/>
    <el-table
        :data="tableData"
        border
        style="width: 100%">
      <el-table-column
          prop="yearMonth"
          label="年月">
      </el-table-column>
      <el-table-column
          prop="totalSalesAmount"
          label="月销售额">
      </el-table-column>
      <el-table-column
          prop="totalProfit"
          label="月利润">
      </el-table-column>
      <el-table-column
          prop="growthRate"
          label="利润增长率"
      :formatter="formatPercentage">
      </el-table-column>
    </el-table>
  </div>

</template>


<script>
export default {
  name: "MonthData",
  data(){
    return{
      tableData:[]
    }
  },
  methods:{
    getData(){
      this.$http.post('/data/getMonthData').then(res=>{
        this.tableData=res.data
      })
    },
    // 自定义格式化函数，将小数转换为百分比
    formatPercentage(row, column) {
      if (row[column.property] != null) {
        return (row[column.property] * 100).toFixed(2) + '%';
      }
      return '';
    }
  },
  created() {
    this.getData()
  }
}
</script>



<style scoped>

</style>