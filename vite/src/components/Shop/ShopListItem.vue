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
                <van-tag :color="shop.priceRule.color">{{ shop.priceRule.name }}</van-tag>

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
            <van-tag :color="shop.priceRule.color">{{ shop.priceRule.name }}</van-tag>
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
              priceRule: {
                color: shop.priceRule.color,
                name: shop.priceRule.name
              }

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
      this.getShops()
      this.UseSearch = false
    },


  },

}

</script>

<style> .van-index-bar__sidebar {
  width: 40px !important;
  font-size: 16px !important;
  font-weight: 500 !important;
  right: 0 !important; /* 减小最大高度 */
  max-height: 400px !important;
  overflow-y: auto !important;
  display: flex !important;
  flex-direction: column !important;
  align-items: center !important;
  justify-content: center !important;
  position: fixed !important; /* 确保垂直居中 */
  top: 50% !important;
  transform: translateY(-50%) !important;
  background-color: rgba(255, 255, 255, 0.95) !important;
  border-radius: 20px !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1) !important;
  padding: 4px 0 !important;
  z-index: 100 !important; /* 压缩字母间距 */
  letter-spacing: 0 !important;
}

.van-index-bar__sidebar::-webkit-scrollbar {
  width: 0 !important;
  display: none !important;
}

.van-index-bar__index {
  font-size: 15px !important;
  padding: 2px 0 !important;
  line-height: 1.1 !important;
  touch-action: manipulation !important;
  cursor: pointer !important;
  transition: all 0.2s ease !important;
  font-weight: 600 !important;
}

.van-index-bar__index:active {
  background-color: #f5f5f5 !important;
  transform: scale(1.1) !important;
}

.van-index-anchor {
  font-size: 16px !important;
  font-weight: 600 !important;
  padding: 8px 16px !important;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .van-index-bar__sidebar {
    width: 35px !important;
    font-size: 14px !important; /* 移动端进一步减小高度 */
    max-height: 350px !important;
  }

  .van-index-bar__index {
    padding: 2px 0 !important;
    font-size: 14px !important;
  }
}</style>