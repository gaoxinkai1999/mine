<route lang="json">
{
  "name": "print-settings"
}
</route>

<template>
  <div class="print-settings">
    <van-nav-bar title="打印设置" />
    <van-cell-group inset>
      <van-cell center title="订单打印商品条码">
        <template #right-icon>
          <van-switch v-model="printBarcodeEnabled" @change="onSwitchChange" size="24px" />
        </template>
      </van-cell>
    </van-cell-group>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';

// 定义开关状态的 ref
const printBarcodeEnabled = ref(false);

// localStorage 的 key
const storageKey = 'printBarcodeEnabled';

// 组件挂载时从 localStorage 加载状态
onMounted(() => {
  const storedValue = localStorage.getItem(storageKey);
  if (storedValue !== null) {
    // 将字符串 'true'/'false' 转换为布尔值
    printBarcodeEnabled.value = storedValue === 'true';
  } else {
    // 如果 localStorage 中没有值，默认为 false
    printBarcodeEnabled.value = false;
  }
});

// 开关状态改变时的处理函数
const onSwitchChange = (newValue) => {
  // 将新的状态保存到 localStorage
  localStorage.setItem(storageKey, newValue.toString());
};
</script>

<style scoped>
.print-settings {
  /* 如果你的应用有固定的顶部导航栏，可能需要添加 padding-top */
  /* padding-top: 46px; */
}
/* 可以根据需要添加更多样式 */
</style>