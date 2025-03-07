<template>
  <div>
    <van-nav-bar

        right-text="编辑商家"
        title="商家信息"
        @click-right="showEditDialog = true"
    />

    <van-cell-group v-if="shop!==''" inset title="基本信息">

      <van-cell :value="shop.location" size="large" title="地区"/>


      <van-cell :value="shop.name" size="large" title="商家名"/>


      <van-cell :value="shop.priceRule.name" size="large" title="价格规则"/>


      <van-field v-model="shop.arrears" input-align="right" label="欠款" size="large" type="digit"
                 @change="setArrears()"/>


    </van-cell-group>
    <van-cell-group inset title="位置信息">

      <div v-if="shop.longitude!==null" style=" margin-top:3vh">
        <van-button block color="#8BC96E" style=" margin-top:3vh" @click="navigateToLocation()">开启导航</van-button>
      </div>
      <div style=" margin-top:3vh">
        <van-button block color="#AF5F4E" style=" margin-top:3vh" @click="setLocation">修改坐标为当前位置</van-button>
      </div>
    </van-cell-group>
<!--    <van-cell-group inset title="订单">-->
<!--      <div style=" margin-top:3vh">-->
<!--        <van-button block style=" margin-top:3vh" type="success" @click="goToOrder">跳转到该商家订单</van-button>-->
<!--      </div>-->
<!--    </van-cell-group>-->

    <!-- 编辑商家弹窗 -->
    <van-dialog
      v-model:show="showEditDialog"
      title="编辑商家信息"
      show-cancel-button
      :before-close="handleBeforeClose"
    >
      <div class="edit-form">
        <van-form >
          <van-cell-group inset>
            <van-field
              v-model="editForm.name"
              label="商家名"
              placeholder="请输入商家名称"
              :rules="[{ required: true, message: '请输入商家名称' }]"
            />

            <van-field
              v-model="editForm.priceRule.name"
              is-link
              readonly
              label="价格规则"
              placeholder="请选择价格规则"
              @click="showPriceRulePicker = true"
            />
          </van-cell-group>
        </van-form>
      </div>
    </van-dialog>

    <!-- 价格规则选择器弹窗 -->
    <van-popup v-model:show="showPriceRulePicker" position="bottom" round>
      <van-picker
        :columns="priceRules"
        :columns-field-names=customFieldName
        @confirm="onPriceRuleConfirm"
        @cancel="showPriceRulePicker = false"
        show-toolbar
        title="选择价格规则"
      />
    </van-popup>
  </div>
</template>

<script>

import api from "@/api";
import { useGaodeNavigation } from '@/utils/useGaodeNavigation';
import { showToast, showConfirmDialog } from 'vant';

export default {
  name: "ShopInfo",
  props: {
    id: Number,

  },

  setup() {
    // 在 setup 中调用组合式函数
    const { navigateToLocation } = useGaodeNavigation();

    // 返回方法，使其在模板和其他选项中可用
    return {
      navigateToLocation
    };
  },
  data() {
    return {
      shop: '',
      orders: [],
      showEditDialog: false,
      showPriceRulePicker: false,
      priceRules: [],
      editForm: {
        name: '',
        priceRule: { name: '', id: null }
      },
      customFieldName : {
        text: 'name',
      }
    }
  },
  async mounted() {
    await this.findById()
    await this.loadPriceRules()
  },
  methods: {
    async loadPriceRules() {
      try {
        // 假设这是获取价格规则的API
        const response = await api.pricerule.getSimplePriceRules()
        this.priceRules = response
      } catch (error) {
        showToast('获取价格规则失败')
      }
    },

    handleBeforeClose(action) {
      if (action === 'confirm') {
        this.updateShop()
      }
      return true
    },

    onPriceRuleConfirm({selectedOptions}) {
      this.editForm.priceRule = selectedOptions
      this.showPriceRulePicker = false
    },

    async updateShop() {
      try {
        await api.shop.update([{
          id: this.shop.id,
          name: this.editForm.name,
          priceRuleId: this.editForm.priceRule.id
        }])
        
        showToast({
          type: 'success',
          message: '更新成功'
        })
        
        // 重新加载商家信息
        await this.findById()
      } catch (error) {
        showToast({
          type: 'fail',
          message: '更新失败'
        })
      }
    },

    // // 跳转到该商家订单
    // goToOrder() {
    //   this.$router.push({
    //     path: '/AllOrder',
    //     query: {
    //       shopId: this.shop.id,
    //     }
    //   })
    // },

    async findById() {
      this.shop = await api.shop.getShop({id: this.$route.query.id})
      // 初始化编辑表单
      this.editForm = {
        name: this.shop.name,
        priceRule: { ...this.shop.priceRule }
      }
    },

    setArrears() {
      showConfirmDialog({
        title: '确认修改',
      })
          .then(() => {
         api.shop.update([{id: this.shop.id, arrears: this.shop.arrears}])

          })
    },
    setLocation() {

    }


  }
}

</script>

<style scoped>
.edit-form {
  padding: 16px;
}

:deep(.van-dialog__content) {
  padding-top: 8px;
}

:deep(.van-cell-group--inset) {
  margin: 0;
}

:deep(.van-field__label) {
  width: 80px;
}

:deep(.van-dialog) {
  border-radius: 12px;
}

:deep(.van-picker__toolbar) {
  background-color: #f7f8fa;
}

:deep(.van-picker-column) {
  font-size: 16px;
}
</style>
