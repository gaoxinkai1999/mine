<template>
  <van-dialog
    :show="visible"
    @update:show="$emit('update:visible', $event)"
    title="修改库存"
    :show-confirm-button="false"
    close-on-click-overlay
    class="custom-dialog"
  >
    <van-form @submit="$emit('submit')">
      <van-cell-group inset>
        <van-field
          label="产品名称"
          :model-value="editForm.productName"
          readonly
          label-width="90px"
          class="form-field"
        />
        <van-field
          v-if="editForm.batchNumber !== undefined"
          label="批次号"
          :model-value="editForm.batchNumber"
          readonly
          label-width="90px"
          class="form-field"
        />
        <van-field
          name="quantity"
          label="库存数量"
          label-width="90px"
          required
          class="form-field"
        >
          <template #input>
            <van-stepper
              v-model="editForm.quantity"
              :min="0"
              integer
              input-width="80px"
              button-size="28px"
              theme="round"
            />
          </template>
        </van-field>
      </van-cell-group>
      <div class="dialog-footer">
        <van-button round block type="primary" native-type="submit" class="submit-btn">确认修改</van-button>
        <van-button round block plain @click="$emit('update:visible', false)" class="cancel-btn">取消</van-button>
      </div>
    </van-form>
  </van-dialog>
</template>

<script setup>
defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  editForm: {
    type: Object,
    required: true
  }
});

defineEmits(['update:visible', 'submit']);
</script>

<style scoped>
/* 弹窗样式 */
:deep(.custom-dialog .van-dialog__header) {
  padding: 16px;
  text-align: center;
  font-size: 18px;
  color: #333;
  font-weight: 600;
}

.form-field {
  margin-bottom: 8px;
}

.dialog-footer {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.submit-btn {
  margin-bottom: 4px;
  height: 44px;
  font-size: 16px;
  font-weight: 500;
}

.cancel-btn {
  height: 44px;
  font-size: 16px;
}

/* 适配小屏设备 */
@media screen and (max-width: 360px) {
  .dialog-footer {
    padding: 12px;
  }
  .submit-btn, .cancel-btn {
    height: 40px;
    font-size: 15px;
  }
}
</style> 