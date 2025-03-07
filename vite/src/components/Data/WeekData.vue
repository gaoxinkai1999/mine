<template>
  <div>
    <van-nav-bar
        title="周销售数据"/>
    <el-table
        :data="tableData"
        border
        style="width: 100%">
      <el-table-column
          prop="year"
          label="年">
      </el-table-column>
      <el-table-column
          prop="week"
          label="周">
      </el-table-column>
      <el-table-column
          prop="week_start"
          label="开始日期"
      width="100">
      </el-table-column>
      <el-table-column
          prop="total_profit"
          label="周利润">
      </el-table-column>
      <el-table-column
          prop="growth_rate_percentage"
          label="增长率">
      </el-table-column>
    </el-table>
  </div>

</template>


<script>
export default {
  name: "WeekData",
  data(){
    return{
      tableData:""
    }
  },
  methods:{
    getData(){
      this.$http.post('getWeekData').then(res=>{
        this.tableData=res.data
      })
    }
  },
  created() {
    this.getData()
  }
}
</script>



<style scoped>

</style>