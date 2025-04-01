<template>
  <!-- 浮动气泡按钮 -->
  <van-floating-bubble
    v-if="show"
    v-model:offset="bubbleOffset"
    axis="xy"
    icon="plus"
    magnetic="x"
    @click="toggleActionPanel"
  />

  <!-- 点击后展开的操作面板 -->
  <div 
    class="action-panel-mask" 
    v-show="showActionPanel"
    @click="toggleActionPanel"
  ></div>

  <div 
    class="action-panel" 
    :class="{ 'show': showActionPanel }" 
    v-show="showActionPanel"
  >
    <a 
      v-for="(item, index) in actionItems" 
      :key="item.routeName"
      class="action-item-wrapper"
      href="javascript:;"
      @click.prevent="onItemClick(item)"
    >
      <div 
        class="action-item"
        :class="item.class"
        :style="{ transitionDelay: `${index * 0.05}s` }"
      >
        <van-icon :name="item.icon" />
        <span>{{ item.text }}</span>
      </div>
    </a>
  </div>
</template>

<script setup>
import { ref, onMounted, defineProps, defineEmits } from 'vue';
import { useRouter } from 'vue-router';
import { ROUTE_NAMES } from '@/constants/routeNames.js';

const props = defineProps({
  // 是否显示气泡
  show: {
    type: Boolean,
    default: true
  },
  // 操作项列表
  items: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['navigate']);

const router = useRouter();
const bubbleOffset = ref({ y: 0 });
const showActionPanel = ref(false);

// 计算浮动气泡的位置
const calculateBubblePosition = () => {
  const screenHeight = window.innerHeight;
  bubbleOffset.value = {
    y: Math.round(screenHeight * 0.75)
  };
};

// 切换操作面板显示状态
const toggleActionPanel = () => {
  showActionPanel.value = !showActionPanel.value;
};

// 处理按钮点击
const onItemClick = (item) => {
  console.log("按钮被点击", item.text, item.routeName);
  
  // 手动导航到指定路由
  try {
    emit('navigate', item.routeName);
    showActionPanel.value = false;
    setTimeout(() => {
      router.push({ name: item.routeName }).catch(err => {
        console.error("路由错误:", err);
      });
    }, 100);
  } catch (error) {
    console.error("点击处理错误:", error);
  }
};

// 操作项数据
const actionItems = props.items.length > 0 ? props.items : [
  { 
    text: '销售订单', 
    icon: 'cart-o', 
    routeName: ROUTE_NAMES.ORDER_SALE_NEW,
    class: 'action-item-sales'
  },
  { 
    text: '退货订单', 
    icon: 'revoke', 
    routeName: ROUTE_NAMES.ORDER_RETURN_NEW,
    class: 'action-item-return'
  },
  { 
    text: '采购订单', 
    icon: 'shopping-cart-o', 
    routeName: ROUTE_NAMES.PURCHASE_NEW,
    class: 'action-item-purchase'
  }
];

onMounted(() => {
  // 计算初始气泡位置
  calculateBubblePosition();
  
  // 监听屏幕尺寸变化，重新计算气泡位置
  window.addEventListener('resize', calculateBubblePosition);
});
</script>

<style scoped>
/* 自定义浮动按钮样式 */
:deep(.van-floating-bubble) {
  --van-floating-bubble-background: #1989fa;
  --van-floating-bubble-color: #fff;
  --van-floating-bubble-shadow: 0 0 10px rgba(25, 137, 250, 0.3);
  right: 16px;
  bottom: calc(var(--van-tabbar-height) + 36px);
  z-index: 101;
}

/* 操作面板蒙层 */
.action-panel-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0);
  z-index: 102;
  backdrop-filter: blur(0px);
  -webkit-backdrop-filter: blur(0px);
  transition: all 0.3s ease;
}

.action-panel-mask[v-show="true"] {
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
}

/* 操作面板容器 */
.action-panel {
  position: fixed;
  right: 16px;
  bottom: calc(var(--van-tabbar-height) + 36px);
  z-index: 103;
  width: 300px; /* 增加可点击区域 */
  height: 300px; /* 增加可点击区域 */
}

/* 操作按钮包装器 */
.action-item-wrapper {
  display: block;
  position: absolute;
  width: 60px;
  height: 60px;
  right: 0;
  bottom: 0;
  z-index: 104;
  text-decoration: none;
}

/* 销售订单包装器 - 小屏幕 */
.action-panel.show .action-item-wrapper:nth-child(1) {
  transform: translate(-80px, -80px);
}

/* 退货订单包装器 - 小屏幕 */
.action-panel.show .action-item-wrapper:nth-child(2) {
  transform: translate(-120px, -30px);
}

/* 采购订单包装器 - 小屏幕 */
.action-panel.show .action-item-wrapper:nth-child(3) {
  transform: translate(-30px, -120px);
}

/* 中等屏幕 */
@media screen and (min-width: 375px) {
  .action-panel.show .action-item-wrapper:nth-child(1) {
    transform: translate(-100px, -100px);
  }
  
  .action-panel.show .action-item-wrapper:nth-child(2) {
    transform: translate(-140px, -40px);
  }
  
  .action-panel.show .action-item-wrapper:nth-child(3) {
    transform: translate(-40px, -140px);
  }
}

/* 大屏幕 */
@media screen and (min-width: 414px) {
  .action-panel.show .action-item-wrapper:nth-child(1) {
    transform: translate(-120px, -120px);
  }
  
  .action-panel.show .action-item-wrapper:nth-child(2) {
    transform: translate(-160px, -50px);
  }
  
  .action-panel.show .action-item-wrapper:nth-child(3) {
    transform: translate(-50px, -160px);
  }
}

/* 操作按钮通用样式 */
.action-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  opacity: 0;
  transform: scale(0.5);
  overflow: hidden;
  cursor: pointer;
}

.action-panel.show .action-item {
  opacity: 1;
  transform: scale(1);
}

.action-item:active {
  transform: scale(0.9) !important;
}

.action-item .van-icon {
  font-size: 20px;
  margin-bottom: 2px;
}

.action-item span {
  font-size: 10px;
  white-space: nowrap;
  font-weight: 500;
}

/* 销售订单按钮 */
.action-item-sales {
  background: linear-gradient(145deg, #36d1dc, #5b86e5);
  color: white;
}

/* 退货订单按钮 */
.action-item-return {
  background: linear-gradient(145deg, #ff9966, #ff5e62);
  color: white; 
}

/* 采购订单按钮 */
.action-item-purchase {
  background: linear-gradient(145deg, #56ab2f, #a8e063);
  color: white;
}

/* 修复iOS底部安全区适配 */
@supports (padding-bottom: env(safe-area-inset-bottom)) {
  :deep(.van-floating-bubble) {
    bottom: calc(var(--van-tabbar-height) + 36px + env(safe-area-inset-bottom));
  }
  
  .action-panel {
    bottom: calc(var(--van-tabbar-height) + 36px + env(safe-area-inset-bottom));
  }
}
</style> 