<route>
{
  name: "shop-arrears"
}
</route>

<template>
  <div>
    <van-nav-bar
        title="欠款详情"/>
    <el-table
        :data="TableData"
        border
        show-summary
        style="width: 100%">
      <el-table-column
          label="地址"
          prop="location">
      </el-table-column>
      <el-table-column
          label="商家名">
        <template #default="scope">
          <span style="color: #474cb6" @click="handleClick(scope.row.id)">{{ scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column
          label="欠款金额"
          prop="arrears">
      </el-table-column>
      <el-table-column label="Operations">
        <template #default="scope">
          <van-button size="small" @click="handleEdit(scope.$index, scope.row)">
            编辑
          </van-button>

        </template>
      </el-table-column>

    </el-table>

    <van-popup v-model:show="show" :style="{ padding: '64px' }">
      <div class="popup-content">
        <van-field
            v-model="newItem.name"
            disabled
            label="商家"
        />
        <van-field
            v-model="newItem.arrears"
            label="欠款"
            placeholder="请输入欠款"
            required
            type="number"
        />
        <div class="popup-actions">
          <van-button block type="primary" @click="saveNewItem">保存</van-button>
          <van-button block type="default" @click="show=false">取消</van-button>
        </div>
      </div>
    </van-popup>

  </div>
</template>

<script>
import api from "@/api/index.js";
import {ROUTE_NAMES} from "@/constants/routeNames.js";
// import {showConfirmDialog, showSuccessToast} from "vant";

export default {
  name: "ArrearsData",
  data() {
    return {
      TableData: [],
      show: false,
      newItem: null
    }
  },
  mounted() {
    this.getArrears()
  },
  methods: {
    handleClick(id) {
      this.$router.push({
        name: ROUTE_NAMES.SHOP_DETAIL,
        query: {
          id: id
        }
      })
    },
    async getArrears() {
      this.TableData = await api.shop.arrears();
    },
    handleDelete(index, row) {
      console.log(index, row)
    },
    handleEdit(index, row) {
      this.show = true;
      this.newItem = row;
      console.log(index, row)
    },
    async saveNewItem() {
      showConfirmDialog({
        title: '确认提交',
      })
          .then(async () => {
            // on confirm
            await api.shop.update([{id: this.newItem.id, arrears: this.newItem.arrears}]);
            this.show=false
            showSuccessToast('111')
          })
          .catch(() => {
            // on cancel
          });

    }
  }
}
</script>


<style scoped>
/* 弹出层样式 */
.popup-content {
  padding: 20px 16px;
}

.popup-actions {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>