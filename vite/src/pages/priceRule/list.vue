<template>
  <div>
    <van-nav-bar
        right-text="新建规则"
        title="价格规则"
        @click-right="goToAddPriceRule"
    />
    <van-cell v-for="(item,index) in data" :key="index" :title=item.name is-link @click="goToInfo(item.id)"></van-cell>

  </div>

</template>

<script>
import api from "@/api/index.js";

export default {
  name: "PriceRule",
  data() {
    return {
      data: []
    }
  },
  created() {
    this.findAll()
  },
  methods: {
    async findAll() {
      this.data = await api.pricerule.getPriceRules()
    },

    goToInfo(id) {
      this.$router.push({
        path: '/PriceRuleInfo',
        query: {
          id: id
        }
      })
    },
    goToAddPriceRule() {
      this.$router.push({
        path: '/AddPriceRule'
      })
    }
  }

}
</script>


<style scoped>

</style>