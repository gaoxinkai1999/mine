<template>
  <div>
    <van-nav-bar
        title="规则详情"
        :right-text="readonly?'修改':'取消'"
        @click-right="readonly=!readonly"

    />
    <van-cell-group inset title="基本信息">
      <van-field label="id" input-align="center " disabled v-model="data.id"></van-field>
      <van-field label="规则名" input-align="center " :readonly="readonly" v-model="data.name"></van-field>
      <van-field label="标签显示颜色" input-align="center ">
        <!-- 使用 right-icon 插槽来自定义右侧图标 -->
        <template #input>
          <el-color-picker v-model="data.color" size="medium" :disabled="readonly"></el-color-picker>
        </template>
      </van-field>
    </van-cell-group>
    <van-cell-group inset title="价格">
      <van-field :label="item.product.name" input-align="center " :readonly="readonly"
                 v-for="item in data.priceRuleDetails" :key="item.id"
                 v-model="item.price"></van-field>
    </van-cell-group>

    <div style="margin: 16px;" v-if="!readonly">
      <van-button round block type="info" @click="save">提交</van-button>
    </div>
  </div>
</template>
<script>

export default {
  name: "PriceRuleInfo",
  data() {
    return {
      data: {},
      readonly: true
    }
  },
  created() {
    this.findById()
  },
  /**
   * 监听readonly变化，如果取消修改，则重新请求以恢复data
   */
  watch: {
    readonly: function (newVal) {
      if (newVal) {
        this.findById()
      }
    }
  },
  methods: {
    findById() {
      console.log("执行")
      this.$http.post("/priceRule/findById", this.$qs.stringify({id: this.$route.query.id})).then(res => {
        this.data = res.data
      })
    },
    save() {
      // eslint-disable-next-line no-unused-vars
      this.$http.post("/priceRule/setPriceRule", this.data).then(res => {
          this.$toast.success("保存成功")
        this.readonly = true
      })
    }
  }
}
</script>


<style scoped>

</style>