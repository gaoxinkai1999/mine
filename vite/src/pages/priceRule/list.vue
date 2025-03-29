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
import {ROUTE_NAMES} from "@/constants/routeNames.js";

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
        name: ROUTE_NAMES.PRICE_RULE_DETAIL,
        query: {
          id: id
        }
      })
    },
    goToAddPriceRule() {
      this.$router.push({
        name: ROUTE_NAMES.PRICE_RULE_CREATE
      })
    }
  }

}
</script>


<style scoped>

</style>