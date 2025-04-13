<template>
  <div>
    <van-dialog
      :show="visible"
      @update:show="$emit('update:visible', $event)"
      title="编辑批次信息"
      show-cancel-button
      @confirm="$emit('submit')"
      class="batch-info-dialog"
    >
      <van-form @submit.prevent>
        <van-cell-group inset class="dialog-form-group">
          <van-field
            label="产品名称"
            :model-value="form.productName"
            readonly
            label-width="70px"
          />
          <van-field
            v-model="form.batchNumber"
            label="批次号"
            label-width="70px"
            placeholder="请输入批次号"
          />
          <van-field
            v-model="form.productionDateFormatted"
            is-link
            readonly
            name="productionDatePicker"
            label="生产日期"
            label-width="70px"
            placeholder="点击选择生产日期"
            @click="$emit('update:show-production-date-picker', true)"
          />
          <van-field
            v-model="form.expirationDateFormatted"
            is-link
            readonly
            name="expirationDatePicker"
            label="过期日期"
            label-width="70px"
            placeholder="点击选择过期日期"
            @click="$emit('update:show-expiration-date-picker', true)"
          />
          <van-field name="switch" label="状态" label-width="70px">
            <template #input>
              <van-switch v-model="form.status" size="20px" />
            </template>
          </van-field>
          <van-field
            v-model="form.remark"
            rows="1"
            autosize
            label="备注"
            label-width="70px"
            type="textarea"
            maxlength="50"
            placeholder="请输入备注"
            show-word-limit
          />
        </van-cell-group>
      </van-form>
    </van-dialog>
    
    <!-- 生产日期选择器 -->
    <van-popup
      :show="showProductionDatePicker"
      @update:show="$emit('update:show-production-date-picker', $event)"
      position="bottom"
      destroy-on-close
    >
      <van-date-picker
        :model-value="productionDate"
        @confirm="$emit('production-date-confirm', $event)"
        @cancel="$emit('update:show-production-date-picker', false)"
      />
    </van-popup>
    
    <!-- 过期日期选择器 -->
    <van-popup
      :show="showExpirationDatePicker"
      @update:show="$emit('update:show-expiration-date-picker', $event)"
      position="bottom"
      destroy-on-close
    >
      <van-date-picker
        :model-value="expirationDate"
        @confirm="$emit('expiration-date-confirm', $event)"
        @cancel="$emit('update:show-expiration-date-picker', false)"
      />
    </van-popup>
  </div>
</template>

<script setup>
defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  form: {
    type: Object,
    required: true
  },
  productionDate: {
    type: Array,
    default: () => []
  },
  expirationDate: {
    type: Array,
    default: () => []
  },
  showProductionDatePicker: {
    type: Boolean,
    default: false
  },
  showExpirationDatePicker: {
    type: Boolean,
    default: false
  }
});

defineEmits([
  'update:visible',
  'update:show-production-date-picker',
  'update:show-expiration-date-picker',
  'production-date-confirm',
  'expiration-date-confirm',
  'submit'
]);
</script>

<style scoped>
/* 批次信息编辑弹窗样式 */
.batch-info-dialog {
  /* 可以根据需要调整样式 */
}

.dialog-form-group {
  margin-top: 15px;
  margin-bottom: 15px;
}
</style> 