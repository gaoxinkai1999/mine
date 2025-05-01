<route>
{
  name: "tools"
}
</route>

<template>
  <div class="my-tools">
    <h1 class="page-title">管理工具</h1>

    <div class="tool-groups">
      <ToolGroup title="商家" :items="shopItems" />
      <ToolGroup title="商品" :items="productItems" />
<!--      <ToolGroup title="订单" :items="orderItems" />-->
<!--      <ToolGroup title="采购" :items="purchaseItems" />-->
      <ToolGroup title="销售数据" :items="salesDataItems" />
      <ToolGroup title="设置" :items="settingsItems" />
    </div>

<!--    <div class="additional-tools">-->
<!--      <van-cell title="地图组件" is-link to="Home" class="tool-item" />-->
<!--    </div>-->
  </div>
</template>

<script>
import ToolGroup from './components/ToolGroup.vue'
import { ROUTE_NAMES } from '@/constants/routeNames';
// 假设 ToolGroup 组件可以处理 name 属性进行导航

export default {
  name: "MyTools",
  components: {
    ToolGroup
  },
  data() {
    return {
      shopItems: [
        { title: "商家总览", name: ROUTE_NAMES.SHOP_LIST },
        { title: "商家统计信息", name: ROUTE_NAMES.STATISTICS_SHOP },
        { title: "欠款详情", name: ROUTE_NAMES.SHOP_ARREARS },
        { title: "商家地图分布", name: ROUTE_NAMES.SHOP_MAP }
      ],
      productItems: [
        { title: "商品总览", name: ROUTE_NAMES.PRODUCT_LIST },
        { title: "库存管理", name: ROUTE_NAMES.PRODUCT_INVENTORY },
      ],

      salesDataItems: [
        { title: "月销售数据", name: ROUTE_NAMES.STATISTICS_MONTHLY },
        { title: "每日销售数据", name: ROUTE_NAMES.STATISTICS_DAILY }
      ],
      settingsItems: [
        { title: "打印设置", name: ROUTE_NAMES.SETTINGS_PRINT } // 使用路由名称常量导航
      ]
    }
  }
}
</script>

<style scoped>
.my-tools {
  padding: 16px;
  background-color: #f5f7fa;
  width: 100%;
  box-sizing: border-box;
  position: relative;
}

.page-title {
  font-size: 20px;
  color: #2c3e50;
  margin-bottom: 20px;
  text-align: center;
  font-weight: 600;
}

.tool-groups {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  margin-bottom: 20px; /* 确保底部有足够间距 */
}

.additional-tools {
  margin-top: 20px;
}

.tool-item {
  background-color: #ffffff;
  border-radius: 8px;
  margin-bottom: 20px; /* 增加底部间距 */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.tool-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

@media (max-width: 600px) {
  .tool-groups {
    grid-template-columns: 1fr;
  }
  
  .my-tools {
    padding: 12px;
  }
  
  .page-title {
    font-size: 18px;
  }
}
</style>
