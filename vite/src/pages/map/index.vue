<route>
{
  name: "shop-map"
}
</route>

<template>
  <div class="map-page-container">
    <van-nav-bar
      title="商家地图分布"
      left-arrow
      @click-left="goBack"
      fixed
      placeholder
      safe-area-inset-top
    />
    <div v-if="loading" class="loading-indicator">
      <van-loading type="spinner" size="24px">地图加载中...</van-loading>
    </div>
    <div id="map-container" ref="mapContainerRef"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import AMapLoader from '@amap/amap-jsapi-loader';
import api from '@/api';
import locationService from '@/utils/locationService';
import { showLoadingToast, showFailToast, closeToast, showToast } from 'vant';

const router = useRouter();
const mapInstance = ref(null);
const shops = ref([]);
const loading = ref(true);
const mapContainerRef = ref(null);
const centerPoint = ref(null);

const initializeMap = async () => {
  loading.value = true;
  let loadingToast = null;
  try {
    loadingToast = showLoadingToast({ message: '加载地图...', forbidClick: true, duration: 0 });

    // 1. 设置高德安全密钥
    window._AMapSecurityConfig = {
      securityJsCode: '49013a8c134717569a059ffcb25c0161', // 替换成你申请的安全密钥
    };

    // 2. 加载高德地图 JS API
    const AMap = await AMapLoader.load({
      key: "27f27344e89695fc415a483f46c9c8a9", // 替换成你的高德Key
      version: "2.0",
      plugins: ['AMap.Geocoder', 'AMap.Marker', 'AMap.InfoWindow', 'AMap.Bounds'],
    });

    // 3. 获取中心点 (优先用户当前位置)
    try {
      const location = await locationService.getCurrentLocation();
      centerPoint.value = [location.longitude, location.latitude];
    } catch (locationError) {
      console.warn("获取用户位置失败:", locationError);
      showFailToast('获取定位失败，将使用默认中心');
      // 设置默认中心点
      centerPoint.value = [116.397428, 39.90923]; // 北京市中心示例
    }

    // 4. 获取商家位置数据
    shops.value = await api.shop.getActiveShops();
    if (shops.value.length === 0) {
      showToast('没有获取到商家位置信息');
    }

    // 5. (已移除) 获取商家数据后设置备用中心点

    // 6. 计算限制边界 (100km 半径)
    const mapBounds = calculateBounds(centerPoint.value[0], centerPoint.value[1], 100, AMap);
    if (!mapBounds) {
      console.error("无法计算地图边界");
      showFailToast("无法设置地图范围");
    }

    // 7. 创建地图实例
    mapInstance.value = new AMap.Map(mapContainerRef.value, {
      viewMode: '2D',
      zoom: 13,
      center: centerPoint.value,
      pitchEnable: false,
      rotateEnable: false,
    });

    // 8. 设置地图的可交互边界
    if (mapBounds) {
      mapInstance.value.setLimitBounds(mapBounds);
      console.log("地图边界已设置:", mapBounds);
    }

    // 9. 添加商家标记点和信息窗体
    addShopMarkers(AMap);

    closeToast();
    loading.value = false;

  } catch (error) {
    console.error("地图初始化失败:", error);
    if(loadingToast) closeToast();
    showFailToast('地图加载失败，请稍后重试');
    loading.value = false;
  }
};

const calculateBounds = (centerLng, centerLat, radiusKm, AMap) => {
  if (!AMap || !AMap.Bounds) {
    console.error("AMap 或 AMap.Bounds 未加载");
    return null;
  }
  // 纬度一度大约111km
  const deltaLat = radiusKm / 111.0;
  // 经度一度大约111km * cos(纬度)
  const deltaLng = radiusKm / (111.0 * Math.cos(centerLat * Math.PI / 180.0));

  const swLng = centerLng - deltaLng;
  const swLat = centerLat - deltaLat;
  const neLng = centerLng + deltaLng;
  const neLat = centerLat + deltaLat;

  // 验证坐标范围
  if (swLng < -180 || neLng > 180 || swLat < -90 || neLat > 90) {
    console.warn("计算出的边界超出有效范围，可能中心点或半径不合理");
    return null; // 这里选择不设限
  }

  // 使用西南和东北角创建 AMap.Bounds 对象
  return new AMap.Bounds(new AMap.LngLat(swLng, swLat), new AMap.LngLat(neLng, neLat));
};

const addShopMarkers = (AMap) => {
  if (!mapInstance.value || shops.value.length === 0) return;

  const infoWindow = new AMap.InfoWindow({ offset: new AMap.Pixel(0, -30) });

  shops.value.forEach(shop => {
    if (shop.longitude != null && shop.latitude != null) {
      // 创建标记点
      const marker = new AMap.Marker({
        position: new AMap.LngLat(shop.longitude, shop.latitude),
        title: shop.name, // 鼠标悬停提示
        map: mapInstance.value,
        // 使用自定义样式，移除默认图标
        content: `<div class="custom-marker">
                    <div class="marker-bg"></div>
                    <div class="marker-label">${shop.name}</div>
                  </div>`
      });

      marker.on('click', () => {
        // 设置信息窗体内容
        const content = `
          <div style="padding: 7px 10px; font-size: 14px;">
            <h4 style="margin: 0 0 5px 0; padding: 0;">${shop.name}</h4>
            <p style="margin: 0; font-size: 13px; color: #666;">${shop.location || '地址未提供'}</p>
          </div>`;
        infoWindow.setContent(content);
        infoWindow.open(mapInstance.value, marker.getPosition());
      });
    }
  });
};

const goBack = () => {
  router.back();
};

onMounted(() => {
  nextTick(() => {
    initializeMap();
  });
});

onUnmounted(() => {
  if (mapInstance.value) {
    mapInstance.value.destroy();
    mapInstance.value = null;
    console.log("地图已销毁");
  }
});
</script>

<style scoped>
.map-page-container {
  height: 100vh;
  width: 100%;
  display: flex;
  flex-direction: column;
}
#map-container {
  flex: 1;
  width: 100%;
}
.loading-indicator {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10;
  background-color: rgba(255, 255, 255, 0.8);
  padding: 15px;
  border-radius: 8px;
}
/* 自定义标记点样式 */
:deep(.custom-marker) {
  position: relative;
  width: auto;
  min-width: 80px;
  text-align: center;
}
:deep(.marker-bg) {
  position: absolute;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background-color: #1989fa;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  z-index: 1;
}
:deep(.marker-label) {
  position: relative;
  padding: 2px 8px;
  background-color: #1989fa;
  color: white;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
  white-space: nowrap;
  z-index: 2;
}
:deep(.amap-info-contentContainer) {
  padding: 0 !important;
}
:deep(.amap-info-close) {
  top: 5px !important;
  right: 5px !important;
}
</style> 