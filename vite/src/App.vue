<template>
  <div class="app-container">

    <!-- 主视图区域 -->
    <div class="main-content" :class="{ 'has-tabbar': showTabbar }">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component"/>
        </transition>
      </router-view>
    </div>

    <!-- 底部导航条 -->
    <van-tabbar v-if="showTabbar" v-model="activeTab">
      <van-tabbar-item icon="home-o" name="index" :to="{ name: ROUTE_NAMES.HOME }">首页</van-tabbar-item>
      <van-tabbar-item icon="orders-o" name="order-list" :to="{ name: ROUTE_NAMES.ORDER_HOME }">全部订单</van-tabbar-item>
      <van-tabbar-item icon="list-switching" name="tools" :to="{ name: ROUTE_NAMES.TOOLS }">我的工具</van-tabbar-item>
      <van-tabbar-item icon="user-o" name="mine" :to="{ name: ROUTE_NAMES.MINE }">我的</van-tabbar-item>
    </van-tabbar>

    <!-- 使用封装的ActionBubble组件 -->
    <ActionBubble 
      :show="showFloatingButton"
      :items="actionItems"
      @navigate="handleNavigate"
    />
  </div>
</template>

<script setup>
import {ref, computed, watch, onMounted} from 'vue';
import {useRouter, useRoute} from 'vue-router';
import {App as CapApp} from '@capacitor/app';
import {showConfirmDialog} from "vant";
import {useMyStore} from '@/stores/defineStore.js';
import { ROUTE_NAMES } from '@/constants/routeNames';
import ActionBubble from '@/pages/components/ActionBubble.vue';

const router = useRouter();
const route = useRoute();
const myStore = useMyStore();

//测试代码区域
import { useAppUpdate } from '@/utils/update.js';

const { checkForUpdates } = useAppUpdate();



checkForUpdates()

// Tabbar 激活状态
const activeTab = ref('index');

// 计算是否显示Tabbar
const showTabbar = computed(() => {
  // 只在这些精确路由名称上显示Tabbar
  const tabbarRouteNames = [
    ROUTE_NAMES.HOME,
    ROUTE_NAMES.ORDER_HOME,
    ROUTE_NAMES.TOOLS,
    ROUTE_NAMES.MINE
  ];

  // 返回当前路由名称是否匹配
  return tabbarRouteNames.includes(route.name);
});

// 计算是否显示浮动按钮
const showFloatingButton = computed(() => {
  // 只在这些指定路由名称上显示浮动按钮
  const floatingButtonRouteNames = [
    ROUTE_NAMES.HOME,
    ROUTE_NAMES.ORDER_HOME,
    ROUTE_NAMES.TOOLS,
    ROUTE_NAMES.MINE
  ];

  // 检查是否为新建订单页面
  if (route.name === ROUTE_NAMES.ORDER_SALE_NEW || route.name === ROUTE_NAMES.ORDER_RETURN_NEW) {
    return false;
  }

  // 返回当前路由名称是否在显示列表中
  return floatingButtonRouteNames.includes(route.name);
});

// 操作项数据
const actionItems = [
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

// 处理ActionBubble的导航事件
const handleNavigate = (routeName) => {
  router.push({ name: routeName });
};

// 监听路由变化更新activeTab
watch(() => route.name, (newName) => {
  if (newName === ROUTE_NAMES.HOME) {
    activeTab.value = 'index';
  } else if (newName === ROUTE_NAMES.ORDER_HOME || newName?.startsWith('order-')) {
    activeTab.value = 'order-list';
  } else if (newName === ROUTE_NAMES.MINE) {
    activeTab.value = 'mine';
  } else if (newName === ROUTE_NAMES.TOOLS) {
    activeTab.value = 'tools';
  }
}, {immediate: true});

// 处理返回按钮
const handleBackButton = ({canGoBack}) => {
  const currentPath = router.currentRoute.value.path;

  if (currentPath === '/') {
    // 在首页时弹出确认对话框
    showConfirmDialog({
      title: '退出应用',
      message: '确定要退出吗？',
    })
        .then(() => {
          CapApp.exitApp(); // 确认退出
        })
        .catch(() => {
          // 取消操作，无动作
        });
  } else {
    // 不在首页时正常返回或退出
    if (canGoBack) {
      router.back(); // 返回上一页
    } else {
      CapApp.exitApp(); // 无历史记录时退出
    }
  }
};

// 初始化
onMounted(async () => {
  // 添加返回按钮监听
  CapApp.addListener('backButton', handleBackButton);

  // 同步store和tabbar的activeIndex (如果需要)
  watch(activeTab, (newVal) => {
    if (newVal === 'index') myStore.activeIndex = 0;
    else if (newVal === 'order-list') myStore.activeIndex = 1;
    else if (newVal === 'mine') myStore.activeIndex = 2;
  });
});
</script>

<style>
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  width: 100%;
  overflow: hidden;
  background: #f7f8fa;
}

.app-container {
  height: 100vh;
  width: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  background: #f7f8fa;
  overflow: hidden;
}

.main-content {
  flex: 1;
  position: relative;
  width: 100%;
  overflow: auto; /* 子页面负责控制各自内容的滚动 */
}

/* 只有当显示tabbar时才应用底部填充 */
.has-tabbar {
  padding-bottom: var(--van-tabbar-height) !important;
}

/* 移除content-with-tabbar类，由各页面自行处理底部间距 */

@supports (padding-bottom: env(safe-area-inset-bottom)) {
  .has-tabbar {
    padding-bottom: calc(var(--van-tabbar-height) + env(safe-area-inset-bottom, 0px)) !important;
  }
}

:root {
  --van-tabbar-item-active-color: #1989fa;
  --van-tabbar-item-font-size: 12px;
  --van-tabbar-item-icon-size: 22px;
  --van-tabbar-height: 50px;
  --van-floating-bubble-size: 56px;
}

.van-tabbar {
  box-shadow: 0 -1px 4px rgba(0, 0, 0, 0.05);
  border-top: none;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  z-index: 100;
}

.van-tabbar-item {
  padding: 8px 0;
}

.van-tabbar-item__icon {
  margin-bottom: 4px;
  transition: transform 0.2s;
}

.van-tabbar-item--active .van-tabbar-item__icon {
  transform: scale(1.1);
}

/* 修复Safari中的滚动问题 */
@supports (-webkit-touch-callout: none) {
  body, .app-container {
    height: 100%;
    -webkit-overflow-scrolling: touch;
  }
}

/* 修复iOS底部安全区适配 */
@supports (padding-bottom: env(safe-area-inset-bottom)) {
  .van-tabbar {
    padding-bottom: env(safe-area-inset-bottom);
    height: calc(var(--van-tabbar-height) + env(safe-area-inset-bottom));
  }
}

/* 添加页面过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
