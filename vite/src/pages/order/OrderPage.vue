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
           <van-tag v-if="shop.isDel" type="danger">弃用</van-tag>
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

      <!-- 日期选择字段，增加选择类型 -->
      <van-field
          name="dateType"
          label="日期类型"
          is-link
          readonly
          :model-value="dateTypeLabel"
          @click="showDateTypePopup = true"
      />
      <van-popup 
          v-model:show="showDateTypePopup" 
          position="bottom"
          round
          teleport="body"
      >
        <van-picker
          title="选择日期类型"
          :columns="dateTypeColumns"
          @confirm="onConfirmDateType"
          @cancel="showDateTypePopup = false"
          show-toolbar
        />
      </van-popup>

      <!-- 日期选择器，单日或日期范围 -->
      <template v-if="dateType === 'single'">
        <van-field
            :model-value="form.startDate"
            is-link
            label="日期"
            name="calendar"
            placeholder="点击选择日期"
            readonly
            @click="showCalendar = true"
        />
        <van-calendar 
            v-model:show="showCalendar" 
            @confirm="onConfirmDate"
            round
            teleport="body"
        />
      </template>

      <template v-else-if="dateType === 'range'">
        <van-field
            :model-value="dateRangeText"
            is-link
            label="日期范围"
            name="calendarRange"
            placeholder="点击选择日期范围"
            readonly
            @click="showCalendarRange = true"
        />
        <van-calendar 
          v-model:show="showCalendarRange" 
          type="range" 
          @confirm="onConfirmDateRange"
          round
          teleport="body"
        />
      </template>

      <div style="margin: 16px;">
        <van-button block native-type="submit" round type="primary">确认筛选条件</van-button>
      </div>
    </van-form>
  </div>
</template>
<script>
import ShopListItem from "@/components/Shop/ShopListItem.vue";
import { showSuccessToast, showFailToast } from 'vant';

export default {
  name: "ChooseOrder",
  components: {ShopListItem},
  data() {
    return {
      showCalendar: false,
      showCalendarRange: false,
      showPicker: false,
      showDateTypePopup: false,
      dateType: 'single', // 默认单日查询
      dateTypeOptions: {
        'single': '单日查询',
        'range': '日期范围',
        'none': '不限日期'
      },
      shops: [],
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
      }
      return '';
    },
    // 当前选中的日期类型标签
    dateTypeLabel() {
      return this.dateTypeOptions[this.dateType] || '请选择日期类型';
    },
    // 日期类型选择器的选项列表，使用正确的对象格式
    dateTypeColumns() {
      return Object.entries(this.dateTypeOptions).map(([value, text]) => ({
        text,
        value
      }));
    }
  },
  created() {
    // 从路由参数或localStorage还原筛选条件
    this.restoreFilters();
  },
  methods: {
    // 还原筛选条件
    restoreFilters() {
      try {
        // 首先尝试从路由参数获取
        const { shopId, shopName, startDate, endDate } = this.$route.query;
        
        // 处理商店信息
        if (shopId) {
          this.form.shopId = shopId;
          
          // 如果路由中有商店名称，暂时构造一个基本的商店对象
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
          
          // 判断日期类型
          if (endDate && endDate !== startDate) {
            this.form.endDate = endDate;
            this.dateType = 'range';
          } else {
            this.form.endDate = startDate; // 确保单日查询也设置endDate
            this.dateType = 'single';
          }
        } else {
          this.dateType = 'none';
          this.form.startDate = null;
          this.form.endDate = null;
        }

        // 如果路由参数中没有，则尝试从localStorage恢复
        if (!shopId && !startDate) {
          const savedFilters = localStorage.getItem('orderFilters');
          if (savedFilters) {
            try {
              const parsedFilters = JSON.parse(savedFilters);
              
              // 恢复商店信息
              if (parsedFilters.shopId) {
                this.form.shopId = parsedFilters.shopId;
                
                // 如果有商店名称，处理同上
                if (parsedFilters.shopName) {
                  const match = parsedFilters.shopName.match(/\(([^)]+)\)(.+)/);
                  if (match) {
                    this.shop = {
                      id: parsedFilters.shopId,
                      location: match[1],
                      name: match[2].trim()
                    };
                  } else {
                    this.shop = {
                      id: parsedFilters.shopId,
                      name: parsedFilters.shopName
                    };
                  }
                }
              }
              
              // 恢复日期信息
              if (parsedFilters.startDate) {
                this.form.startDate = parsedFilters.startDate;
                
                if (parsedFilters.endDate && parsedFilters.endDate !== parsedFilters.startDate) {
                  this.form.endDate = parsedFilters.endDate;
                  this.dateType = 'range';
                } else {
                  this.form.endDate = parsedFilters.startDate;
                  this.dateType = 'single';
                }
              } else {
                this.dateType = 'none';
                this.form.startDate = null;
                this.form.endDate = null;
              }
            } catch (e) {
              console.error('解析localStorage中的筛选条件出错:', e);
              // 清除可能损坏的数据
              localStorage.removeItem('orderFilters');
            }
          }
        }
        
        console.log('已还原筛选条件:', {
          shop: this.shop,
          dateType: this.dateType,
          startDate: this.form.startDate,
          endDate: this.form.endDate
        });
      } catch (error) {
        console.error('还原筛选条件出错:', error);
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
        if (this.dateType === 'single' && this.form.startDate) {
          query.startDate = this.form.startDate;
          query.endDate = this.form.startDate;
        } else if (this.dateType === 'range') {
          if (this.form.startDate) query.startDate = this.form.startDate;
          if (this.form.endDate) query.endDate = this.form.endDate;
        }
        
        // 保存到localStorage (即使没有筛选条件也保存，覆盖之前的设置)
        localStorage.setItem('orderFilters', JSON.stringify(query));
        
        // 跳转到订单列表页面
        this.$router.push({
          path: '/order/list',
          query
        });
        
        showSuccessToast('筛选条件已应用');
        console.log('提交筛选条件:', query);
      } catch (error) {
        console.error('提交筛选条件出错:', error);
        showFailToast('应用筛选条件失败');
      }
    },
    
    // 日期类型选择
    onConfirmDateType(value) {
      try {
        // value是一个对象，包含text和value属性
        if (!value || typeof value !== 'object') {
          console.error('无效的日期类型选择:', value);
          return;
        }
        
        this.dateType = value.value;
        this.showDateTypePopup = false;
        
        // 如果切换到不限日期，清空日期值
        if (this.dateType === 'none') {
          this.form.startDate = null;
          this.form.endDate = null;
        }
        
        console.log('已选择日期类型:', this.dateType, this.dateTypeOptions[this.dateType]);
      } catch (error) {
        console.error('选择日期类型出错:', error);
        showFailToast('选择日期类型失败');
      }
    },
    
    // 单日选择
    onConfirmDate(date) {
      try {
        if (!date || !(date instanceof Date)) {
          console.error('无效的日期:', date);
          return;
        }
        
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        this.form.startDate = `${year}-${month}-${day}`;
        this.form.endDate = this.form.startDate;
        this.showCalendar = false;
        
        console.log('已选择日期:', this.form.startDate);
      } catch (error) {
        console.error('选择日期出错:', error);
        showFailToast('选择日期失败');
      }
    },
    
    // 日期范围选择
    onConfirmDateRange(dateRange) {
      try {
        if (!Array.isArray(dateRange) || dateRange.length < 2 || 
            !(dateRange[0] instanceof Date) || !(dateRange[1] instanceof Date)) {
          console.error('无效的日期范围:', dateRange);
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
        
        console.log('已选择日期范围:', this.form.startDate, '至', this.form.endDate);
      } catch (error) {
        console.error('选择日期范围出错:', error);
        showFailToast('选择日期范围失败');
      }
    },
    
    // 商店选择
    receiveDataFromChild(data) {
      try {
        // 验证商店数据的有效性
        if (!data || !data.id) {
          console.error('接收到无效的商店数据:', data);
          return;
        }
        
        this.shop = data;
        this.form.shopId = data.id;
        this.showPicker = false;
        
        console.log('已选择商店:', data);
      } catch (error) {
        console.error('选择商店出错:', error);
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