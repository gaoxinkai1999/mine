<template>
  <div class="app-container">

    <!-- 主视图区域 -->
    <div class="main-content" :class="{ 'has-tabbar': showTabbar }">
      <router-view v-slot="{ Component }">
        <component :is="Component"/>
      </router-view>
    </div>

    <!-- 底部导航条 -->
    <van-tabbar v-if="showTabbar" v-model="activeTab" route>
      <van-tabbar-item icon="home-o" name="index" to="/">首页</van-tabbar-item>
      <van-tabbar-item icon="orders-o" name="order-list" to="/order/list">全部订单</van-tabbar-item>
      <van-tabbar-item icon="list-switching" name="tools" to="/mytools">我的工具</van-tabbar-item>
      <van-tabbar-item icon="user-o" name="mine" to="/mymine">我的</van-tabbar-item>
    </van-tabbar>

    <!-- 浮动气泡按钮 - 新建订单 -->
    <van-floating-bubble
        v-if="showFloatingButton"
        v-model:offset="bubbleOffset"
        axis="xy"
        icon="plus"
        magnetic="x"
        @click="navigateToNewOrder"
    />
  </div>
</template>

<script setup>
import {ref, computed, watch, onMounted} from 'vue';
import {useRouter, useRoute} from 'vue-router';
import {App as CapApp} from '@capacitor/app';
import {showConfirmDialog} from "vant";
import {useMyStore} from '@/stores/defineStore.js';

const router = useRouter();
const route = useRoute();
const myStore = useMyStore();

//测试代码区域
import { useAppUpdate } from '@/utils/update.js';

const { checkForUpdates } = useAppUpdate();

const apkUrl = 'http://update.abocidee.com/updates/app-release.apk'; // 真实的 APK 下载地址

checkForUpdates()





// Tabbar 激活状态
const activeTab = ref('index');

// 浮动气泡位置设置 - 定义初始位置
const bubbleOffset = ref({ y: 1400 }); // 先初始化，稍后动态计算

// 计算是否显示Tabbar
const showTabbar = computed(() => {
  // 只在这些精确路径上显示Tabbar
  const tabbarPaths = [
    '/',
    '/index',
    '/order/list',
    '/mytools',
    '/mymine'
  ];

  // 返回当前路径是否精确匹配tabbar路径
  return tabbarPaths.includes(route.path);
});

// 计算是否显示浮动按钮
const showFloatingButton = computed(() => {
  // 只在这些指定路径上显示浮动按钮
  const floatingButtonPaths = ['/', '/index', '/order/list', '/mytools', '/mymine'];
  
  // 检查当前路径或父路径是否包含 new 订单路径
  if (route.path.includes('/order') && route.path.includes('/new')) {
    return false;
  }
  
  // 返回当前路径是否在显示列表中
  return floatingButtonPaths.includes(route.path);
});

// 计算浮动气泡的位置
const calculateBubblePosition = () => {
  const screenHeight = window.innerHeight;
  // 将气泡定位在屏幕高度的40%处，这样会更靠上
  bubbleOffset.value = { 
    y: Math.round(screenHeight * 0.7)
  };
};

// 导航到新建订单页面
const navigateToNewOrder = () => {
  router.push('/order/sale/new');
};

// 监听路由变化更新activeTab
watch(() => route.path, (newPath) => {
  if (newPath === '/' || newPath === '/index') {
    activeTab.value = 'index';
  } else if (newPath.includes('/order/list')) {
    activeTab.value = 'order-list';
  } else if (newPath.includes('/mymine')) {
    activeTab.value = 'mine';
  } else if (newPath.includes('/mytools')) {
    activeTab.value = 'mine';
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

  // 计算初始气泡位置
  calculateBubblePosition();
  
  // 监听屏幕尺寸变化，重新计算气泡位置
  window.addEventListener('resize', calculateBubblePosition);

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
  overflow: visible;
  background: #f7f8fa;
}

.app-container {
  min-height: 100vh;
  width: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  background: #f7f8fa;
}

.main-content {
  flex: 1;
  position: relative;
  width: 100%;
  /* 允许内容溢出并滚动，但默认行为交给子页面控制 */
  overflow: visible;
}

/* 只有当显示tabbar时才应用底部填充 */
.has-tabbar {
  padding-bottom: var(--van-tabbar-height) !important;
}

/* 添加用于有tabbar页面的内容底部填充类 */
.content-with-tabbar {
  margin-bottom: var(--van-tabbar-height) !important;
}

@supports (padding-bottom: env(safe-area-inset-bottom)) {
  .has-tabbar {
    padding-bottom: calc(var(--van-tabbar-height) + env(safe-area-inset-bottom, 0px)) !important;
  }
  
  .content-with-tabbar {
    margin-bottom: calc(var(--van-tabbar-height) + env(safe-area-inset-bottom, 0px)) !important;
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

/* 自定义浮动按钮样式 */
.van-floating-bubble {
  --van-floating-bubble-background: #1989fa;
  --van-floating-bubble-color: #fff;
  --van-floating-bubble-shadow: 0 0 10px rgba(25, 137, 250, 0.3);
  right: 16px;
  bottom: calc(var(--van-tabbar-height) + 36px);
  z-index: 101;
}

/* 修复Safari中的滚动问题 */
@supports (-webkit-touch-callout: none) {
  body, .app-container, .main-content {
    -webkit-overflow-scrolling: touch;
  }
}

/* 修复iOS底部安全区适配 */
@supports (padding-bottom: env(safe-area-inset-bottom)) {
  .van-tabbar {
    padding-bottom: env(safe-area-inset-bottom);
    height: calc(var(--van-tabbar-height) + env(safe-area-inset-bottom));
  }
  
  .van-floating-bubble {
    bottom: calc(var(--van-tabbar-height) + 36px + env(safe-area-inset-bottom));
  }
}
</style>
