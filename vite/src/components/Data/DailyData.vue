<template>
  <div>
    <van-nav-bar
        title="每日数据"/>
    <el-table
        :data="filteredSalesData"
        border
        style="width: 100%;" height="70vh">
      <el-table-column type="expand" inline class="demo-table-expand">
        <template slot-scope="props">
          <el-form label-position="left"  class="demo-table-expand">
          <el-form-item v-for="(count, key) in props.row.salesCountByGoodsName" :key="key" :label="key" >
            <span>{{count }}</span>
          </el-form-item>
          </el-form>
        </template>
      </el-table-column>
      <el-table-column
          prop="date"
          label="日期"
          min-width="30%">
      </el-table-column>
      <el-table-column
          prop="shopCount"
          label="商家数量"
          min-width="30%">
      </el-table-column>

      <el-table-column
          prop="totalSalesAmount"
          label="销售额" min-width="22%">
      </el-table-column>
      <el-table-column
          prop="totalProfit"
          label="利润" min-width="22%">
      </el-table-column>

    </el-table>



  </div>


</template>

<script>


export default {
  name: "DailyData",
  data() {
    return {
      tableData: [],
      products: []
    }
  },
  created() {
    this.getData()
    this.findAllProducts()
  },
  /**
   * 计算属性用于过滤数据中商品销量为0的
   */
  computed: {
    filteredSalesData() {
      return this.tableData.map(entry => {
        const filteredSalesCount = Object.entries(entry.salesCountByGoodsName)
            // eslint-disable-next-line no-unused-vars
            .filter(([_, count]) => count > 0)
            .reduce((acc, [key, count]) => {
              acc[key] = count;
              return acc;
            }, {});

        return {
          ...entry,
          salesCountByGoodsName: filteredSalesCount
        };
      });
    }
  },


  methods: {
    getData() {
      // eslint-disable-next-line no-undef
      getDaySalesList().then(res => {
        console.log('获取到数据')
        this.tableData = res.data

        Object.entries(   res.data[1].salesCountByGoodsName)
            // eslint-disable-next-line no-unused-vars
            .filter(([name,count]) => count > 0)
            .forEach(([name, count]) => {
              console.log(`${name}: ${count}`);
            });

      })
      // this.$http.post('/data/getDaySalesList').then(res => {
      //   console.log('获取到数据')
      //   this.tableData = res.data
      // })
    },
    findAllProducts() {
      this.$http.post('/product/findAll').then(res => {
        this.products = res.data
      })
    },

  }
}
</script>


<style scoped>
.demo-table-expand {
  font-size: 0;
}
.demo-table-expand label {
  width: 90px;
  color: #99a9bf;
}
.demo-table-expand .el-form-item {
  margin-right: 0;
  margin-bottom: 0;
  width: 50%;
}
</style>