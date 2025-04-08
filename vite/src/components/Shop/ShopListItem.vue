<template>
  <div>
    <form action="/">
      <van-search
          v-model="value"
          placeholder="请输入搜索关键词"
          show-action
          @cancel="onCancel"
          @search="onSearch"
      />
    </form>


    <div>

      <van-index-bar v-if="!UseSearch" :index-list="indexList">
        <!-- 遍历分组数据 -->
        <div v-for="group  in groupedShops" :key="group.index">
          <van-index-anchor :index="group.index"/>
          <!-- 遍历门店列表 -->
          <van-cell v-for="shop in group.items" :key="shop.id" :label="shop.location" border center
                    @click="handleClick(shop)">
            <template #title>
              <div style="display: flex; align-items: center;">
                <span style="flex-shrink: 0; min-width:35vw;">{{ shop.name }}</span>

              </div>
            </template>
          </van-cell>


        </div>

      </van-index-bar>
    </div>
    <!--    搜索结果-->
    <div v-if="UseSearch">

      <van-cell v-for="shop in SearchResult" :key="shop.id" :label="shop.location" border center
                @click="handleClick(shop)">
        <template #title>
          <div style="display: flex; align-items: center;">
            <span style="flex-shrink: 0; min-width:35vw;">{{ shop.name }}</span>

          </div>
        </template>
      </van-cell>

    </div>

  </div>

</template>

<script>

import api from "@/api";

export default {
  name: "ShopList",
  data() {
    return {

      shop: {
        name: '',
        location: ''
      },
      value: '',

      groupedShops: [],
      indexList: [],
      totalShops: 0,
      UseSearch: false,
      SearchResult: []
    }
  },
  props: {
    parentMethod: {
      type: Function,
    },
    isDie: {
      type: Boolean,
    }
  },
  mounted() {
    this.fetchShopData();
  },
  methods: {
    handleClick(shop) {
      this.$emit('selectShop', shop);
      console.log(shop)
    },
    async fetchShopData() {

      const response = await api.shop.groupShopsByPinyin();
      // 转换数据结构
      this.groupedShops = this.transformData(response);
      // 计算总数
      this.totalShops = this.calculateTotal(this.groupedShops);
      // 向父组件传递总数
      this.$emit('total-change', this.totalShops);
      this.indexList = this.generateIndexList(this.groupedShops); // 生成索引列表
    },
    // 新增：计算总数的方法
    calculateTotal(groups) {
      return groups.reduce((sum, group) => sum + group.items.length, 0);
    },
    generateIndexList(groups) {
      // 提取分组索引并转换为大写字母（兼容性处理）
      return groups.map(group => group.index.toUpperCase());
    },


    transformData(data) {
      // 将 { a: [...], b: [...] } 转换为 [{ index: 'A', items: [...] }, ...]
      return Object.keys(data)
          .sort() // 按字母排序
          .map(key => ({
            index: key.toUpperCase(), // 索引显示为大写
            items: data[key].map(shop => ({
              id: shop.id,
              name: shop.name,
              location: shop.location,

              // 其他需要展示的字段...
            }))
          }))
          .filter(group => group.items.length > 0); // 过滤空分组
    },
    // 商家模糊搜索
    async findByLike() {
      this.SearchResult = await api.shop.searchShops({name: this.value});

    },
    onSearch() {
      this.findByLike()
      this.UseSearch = true
    },
    onCancel() {
      this.fetchShopData()
      this.UseSearch = false
    },


  },

}

</script>

<style scoped>
/* 使用深度选择器正确覆盖 Vant 组件样式 */
:deep(.van-search) {
  border-radius: 8px;
  margin-bottom: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

:deep(.van-cell) {
  padding: 12px 16px;
  border-radius: 4px;
  margin: 4px 0;
  transition: background-color 0.2s;
}

:deep(.van-cell:active) {
  background-color: #f8f8f8;
}

:deep(.van-cell__title) {
  font-weight: 500;
}

:deep(.van-cell__label) {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}

/* 索引栏样式优化 */
:deep(.van-index-bar__sidebar) {
  width: 25px;
  font-size: 12px;
  right: 5px;
  max-height: 70vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: fixed;
  top: 50%;
  transform: translateY(-50%);
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  padding: 8px 0;
  z-index: 100;
}

:deep(.van-index-bar__index) {
  padding: 4px 0;
  font-size: 12px;
  line-height: 1.2;
  font-weight: 500;
  color: #666;
  transition: all 0.2s;
}

:deep(.van-index-bar__index--active) {
  color: #1989fa;
  font-weight: 700;
  transform: scale(1.2);
}

:deep(.van-index-anchor) {
  font-size: 15px;
  font-weight: 600;
  padding: 10px 16px;
  background-color: #f5f7fa;
  color: #323233;
  border-left: 3px solid #1989fa;
  margin: 5px 0;
}

/* 移动端适配 */
@media (max-width: 768px) {
  :deep(.van-index-bar__sidebar) {
    width: 20px;
    right: 2px;
    max-height: 60vh;
  }
  
  :deep(.van-index-bar__index) {
    font-size: 10px;
    padding: 3px 0;
  }
  
  :deep(.van-cell__title span) {
    font-size: 14px;
  }
  
  :deep(.van-cell__label) {
    font-size: 11px;
  }
}
</style>