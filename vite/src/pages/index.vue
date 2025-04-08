<route>
{
  name: "home"
}
</route>

<template>
  <div class="delivery-system">
    <!-- 顶部区域 -->
    <header class="header">
      <div class="header-content">
        <h1 class="title">门店配送系统</h1>
        <div class="version-info">
          <h3 class="version">v{{ version }}</h3>
          <span class="changelog" v-if="changelog">更新日志:{{ changelog }}</span>
        </div>
      </div>
    </header>

    <!-- 主功能区域 -->
    <main class="main-content">
      <div class="menu-grid">
        <router-link
            v-for="(item, index) in menuItems"
            :key="index"
            :to="{ name: item.name }"
            class="menu-card"
        >
          <div class="menu-card-content">
            <van-icon :name="item.icon" class="menu-icon" />
            <span class="menu-text">{{ item.text }}</span>
          </div>
        </router-link>

      </div>
    </main>

    <!-- 底部备案信息 -->
    <footer class="footer">
      <a
          class="beian-link"
          href="https://beian.miit.gov.cn/"
          rel="noopener noreferrer"
          target="_blank"
      >
        晋ICP备2023014165号
      </a>
    </footer>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import APP_VERSION from '@/../version.json'
import { ROUTE_NAMES } from '@/constants/routeNames';

export default {
  name: 'HomePage',
  setup() {

    const {version,changelog} = APP_VERSION;

    
    // 功能菜单项
    const menuItems = ref([
      { text: '采购历史', icon: 'cart-o', name: ROUTE_NAMES.PURCHASE_LIST },
      { text: '库存管理', icon: 'label-o', name: ROUTE_NAMES.PRODUCT_INVENTORY },
      { text: '新建商家', icon: 'user-o', name: ROUTE_NAMES.SHOP_CREATE },
    ]);
    
    onMounted(() => {
      // 可以在这里获取版本信息和更新日志
      console.log('当前版本日志：', changelog)
    });
    
    return {
      version,
      changelog,
      menuItems
    };
  }
};
</script>

<style scoped>
.delivery-system {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f7f8fa;
  position: relative;
}

.header {
  background: linear-gradient(135deg, #1989fa, #3b9cff);
  color: white;
  padding: 24px 16px;
  text-align: center;
  width: 100%;
  box-sizing: border-box;
}

.header-content {
  max-width: 600px;
  margin: 0 auto;
}

.title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.version-info {
  margin-top: 8px;
  font-size: 14px;
  opacity: 0.9;
  white-space: pre-line;
}

.version {
  margin-right: 8px;
}

.main-content {
  flex: 1;
  padding: 20px 16px;
  width: 100%;
  box-sizing: border-box;
}

.menu-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  max-width: 600px;
  margin: 0 auto;
}

.menu-card {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(100, 100, 100, 0.08);
  overflow: hidden;
  text-decoration: none;
  color: #333;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.menu-card:active {
  transform: scale(0.98);
}

.menu-card-content {
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.menu-icon {
  font-size: 32px;
  color: #1989fa;
  margin-bottom: 12px;
}

.menu-text {
  font-size: 15px;
  font-weight: 500;
}

.footer {
  padding: 16px;
  text-align: center;
  color: #999;
  font-size: 12px;
  margin-top: auto;
  margin-bottom: 10px;
}

.beian-link {
  color: #999;
  text-decoration: none;
}
</style>
