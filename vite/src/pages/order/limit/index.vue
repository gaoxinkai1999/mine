<template>
  <div>
    <van-nav-bar
        title="筛选订单"
    />
    <van-form @submit="onSubmit">
      <van-field
          is-link
          label="商家"
          placeholder="选择商家"
          readonly
          @click="showPicker = true"
      >
        <template v-if="shop.id!==undefined" #input>
        <span> {{ shop.location ? `(${shop.location})` : '' }}{{ shop.name }}
           <van-tag v-if="shop.del" type="danger">弃用</van-tag>
        </span>
        </template>
      </van-field>

      <!-- 全屏商家选择弹窗 -->
      <van-popup
          v-model:show="showPicker"
          position="bottom"
          :style="{ height: '80%' }"
          teleport="body"
          round
      >
        <van-nav-bar
            title="选择商家"
        >
          <template #right>
            <van-icon name="cross" @click="showPicker = false" />
          </template>
        </van-nav-bar>
        <div class="shop-list-container">
          <ShopListItem @selectShop="receiveDataFromChild"></ShopListItem>
        </div>
      </van-popup>

      <!-- 日期范围选择 -->
      <van-field
          :model-value="dateRangeText"
          is-link
          label="日期范围"
          name="calendarRange"
          placeholder="点击选择日期范围"
          readonly
          @click="showCalendarRange = true"
      >
        <template #extra>
          <van-icon v-if="form.startDate || form.endDate" name="cross" @click.stop="clearDateRange" />
        </template>
      </van-field>
      <van-calendar 
        v-model:show="showCalendarRange" 
        type="range" 
        @confirm="onConfirmDateRange"
        round
        teleport="body"
        :min-date="new Date(2023, 0, 1)"
      />

      <div style="margin: 16px;">
        <van-button block native-type="submit" round type="primary">确认筛选条件</van-button>
      </div>
    </van-form>
  </div>
</template>
<script>
import ShopListItem from "@/components/Shop/ShopListItem.vue";
import { showSuccessToast, showFailToast } from 'vant';
import { useOrderListStore } from "@/stores/orderList.js";
import {ROUTE_NAMES} from "@/constants/routeNames.js";

export default {
  name: "ChooseOrder",
  components: {ShopListItem},
  setup() {
    const orderListStore = useOrderListStore();
    
    return {
      orderListStore
    };
  },
  data() {
    return {
      showCalendarRange: false,
      showPicker: false,
      shop: {}, // 当前选中的商店
      form: {
        shopId: null,
        startDate: null,
        endDate: null
      }
    }
  },
  computed: {
    dateRangeText() {
      if (this.form.startDate && this.form.endDate) {
        return `${this.form.startDate} 至 ${this.form.endDate}`;
      } else if (this.form.startDate) {
        return `从 ${this.form.startDate} 开始`;
      } else if (this.form.endDate) {
        return `至 ${this.form.endDate}`;
      }
      return '';
    }
  },
  created() {
    // 从store和路由参数还原筛选条件
    this.restoreFilters();
  },
  methods: {
    // 清除日期范围
    clearDateRange() {
      this.form.startDate = null;
      this.form.endDate = null;
    },
    
    // 还原筛选条件
    restoreFilters() {
      try {
        // 首先尝试从路由参数获取
        const { shopId, shopName, startDate, endDate } = this.$route.query;
        
        // 如果路由有参数，优先使用路由参数
        if (shopId || startDate || endDate) {
          // 处理商店信息
          if (shopId) {
            this.form.shopId = shopId;
            
            // 如果路由中有商店名称，构造商店对象
            if (shopName) {
              // 从shopName中提取location和name (格式通常是: "(location)name")
              const match = shopName.match(/\(([^)]+)\)(.+)/);
              if (match) {
                this.shop = {
                  id: shopId,
                  location: match[1],
                  name: match[2].trim()
                };
              } else {
                this.shop = {
                  id: shopId,
                  name: shopName
                };
              }
            }
          }
          
          // 处理日期信息
          if (startDate) {
            this.form.startDate = startDate;
          }
          
          if (endDate) {
            this.form.endDate = endDate;
          }
        } else {
          // 如果路由没有参数，从store获取
          this.form.shopId = this.orderListStore.filterParams.shopId;
          this.form.startDate = this.orderListStore.filterParams.startDate;
          this.form.endDate = this.orderListStore.filterParams.endDate;
          
          // 如果store有商店信息，构造商店对象
          if (this.orderListStore.filterParams.shopId && this.orderListStore.filterParams.shopName) {
            const shopName = this.orderListStore.filterParams.shopName;
            const match = shopName.match(/\(([^)]+)\)(.+)/);
            
            if (match) {
              this.shop = {
                id: this.orderListStore.filterParams.shopId,
                location: match[1],
                name: match[2].trim()
              };
            } else {
              this.shop = {
                id: this.orderListStore.filterParams.shopId,
                name: shopName
              };
            }
          }
        }
        
      } catch (error) {
        showFailToast('还原筛选条件失败');
      }
    },
    
    // 提交筛选条件
    onSubmit() {
      try {
        const query = {};
        
        // 添加商店信息
        if (this.form.shopId) {
          query.shopId = this.form.shopId;
          
          // 只有当商店对象完整时才添加商店名称
          if (this.shop && this.shop.name) {
            const location = this.shop.location ? `(${this.shop.location})` : '';
            query.shopName = `${location}${this.shop.name}`;
          }
        }
        
        // 添加日期信息
        if (this.form.startDate) query.startDate = this.form.startDate;
        if (this.form.endDate) query.endDate = this.form.endDate;
        
        // 更新到store
        this.orderListStore.setFilterParams({
          shopId: this.form.shopId,
          shopName: query.shopName || '',
          startDate: this.form.startDate,
          endDate: this.form.endDate
        });
        
        // 跳转到订单列表页面并带上查询参数
        this.$router.push({
          name: ROUTE_NAMES.ORDER_HOME,
          query
        });
        
        showSuccessToast('筛选条件已应用');
      } catch (error) {
        showFailToast('应用筛选条件失败');
      }
    },
    
    // 日期范围选择
    onConfirmDateRange(dateRange) {
      try {
        if (!Array.isArray(dateRange) || dateRange.length < 2 ||
            !(dateRange[0] instanceof Date) || !(dateRange[1] instanceof Date)) {
          return;
        }
        
        const formatDate = (date) => {
          const year = date.getFullYear();
          const month = String(date.getMonth() + 1).padStart(2, '0');
          const day = String(date.getDate()).padStart(2, '0');
          return `${year}-${month}-${day}`;
        };
        
        this.form.startDate = formatDate(dateRange[0]);
        this.form.endDate = formatDate(dateRange[1]);
        this.showCalendarRange = false;
      } catch (error) {
        showFailToast('选择日期范围失败');
      }
    },
    
    // 商店选择
    receiveDataFromChild(data) {
      try {
        // 验证商店数据的有效性
        if (!data || !data.id) {
          return;
        }
        
        this.shop = data;
        this.form.shopId = data.id;
        this.showPicker = false;
      } catch (error) {
        showFailToast('选择商店失败');
      }
    }
  }
}
</script>

<style scoped>
.van-form {
  padding: 16px;
}
.shop-list-container {
  height: calc(100% - 46px);
  overflow-y: auto;
}
</style>