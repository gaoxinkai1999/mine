<template>
  <div>

    <el-table
        :data="shopDataList"
        :row-class-name="tableRowClassName"
        border
        height="85vh"
        lazy
        size="small"

    >
      <el-table-column
          fixed
          type="index"
          width="45"
      >
      </el-table-column>
<!--      <el-table-column-->
<!--          align="center"-->
<!--          fixed-->
<!--          label="位置"-->
<!--          prop="location"-->
<!--      >-->

<!--      </el-table-column>-->

      <el-table-column
          align="center"
          fixed
          label="店铺"
      >
        <template #default="scope">
          <span style="color: #474cb6" @click="handleClick(scope.row.shopId)">{{ scope.row.shopName }}</span>
        </template>
      </el-table-column>


<!--      <el-table-column-->
<!--          align="center"-->
<!--          label="运营天数"-->
<!--          min-width="50" prop="days" sortable>-->
<!--      </el-table-column>-->
<!--      <el-table-column-->
<!--          align="center"-->
<!--          label="总订单数"-->
<!--          min-width="50" prop="totalOrderNum" sortable>-->
<!--      </el-table-column>-->

      <el-table-column
          align="center"
          label="总销售额"
          min-width="70" prop="totalSales" sortable>
      </el-table-column>
      <el-table-column
          align="center"
          label="总利润"
          min-width="70" prop="totalProfit" sortable>
      </el-table-column>
      <el-table-column
          align="center"
          label="月平均利润"
          min-width="70" prop="averageMonthlyProfit" sortable>
      </el-table-column>

    </el-table>


  </div>
</template>

<script>

import api from "@/api";

export default {
  name: "ShopData",
  data() {
    return {
      loading: true,
      currentPage: 1,
      shopDataList: [],
    }
  },
  mounted() {
    this.getShopDataList()

  },
  methods: {
    async getShopDataList() {
      this.shopDataList = await api.statistics.getShopStatistics();
    },

    // eslint-disable-next-line no-unused-vars
    tableRowClassName({row, rowIndex}) {
      if (row.last_order_time == null) {
        row.last_order_time = '暂无订单'
      }
      if (row.days_since_last_order >= 15) {
        // return 'warning-row';
      }

      return '';
    },
    handleClick(id) {
      this.$router.push({
        path: '/shop/info',
        query: {
          id: id
        }
      })
    },

  }
}
</script>

<style>
.el-table .warning-row {
  background: rgb(227, 123, 133);
}

.el-table .success-row {
  background: #f0f9eb;
}

.demo-table-expand {
  font-size: 0;
  width: 500px !important;

}

.demo-table-expand label {
  width: 150px;
  color: #99a9bf;

}

.demo-table-expand .el-form-item {
  margin-right: 0;
  margin-bottom: 0;
  width: 50%;
}

</style>
