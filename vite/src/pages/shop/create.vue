<template>
  <div>
    <van-nav-bar
        left-arrow
        title="添加商家"
        @click-left="onClickLeft"
    />
    <van-form @submit="onSubmit">
      <van-field
          v-model="shop.name"
          :rules="[{ required: true, message: '请输入商家名称' }]"
          label="商家名"
          name="商家名"
          placeholder="请输入商家名称"
          required
      />
      <van-field
          v-model="shop.location"
          :rules="[{ required: true, message: '请选择位置' }]"
          disabled
          label="位置"
          name="位置"
          placeholder="点击地图选择位置"
      />
      <van-field
          :rules="[{ required: true, message: '请选择价格规则' }]"
          v-model="shop.priceRule.name"
          clickable
          label="价格规则"
          name="picker"
          readonly
          required
          @click="showPicker = true"
      />
      <van-popup v-model:show="showPicker" position="bottom">
        <van-picker
            :columns="priceRules"
            show-toolbar
            @cancel="showPicker = false"
            @confirm="onConfirm"
            :columns-field-names="customFieldName"
        />
      </van-popup>

      <!-- 地图容器 -->
      <div class="map-container">
        <div class="map-tip">点击地图选择具体位置</div>
        <div id="container"></div>
        <div class="map-controls">
          <van-button size="small" type="primary" @click="relocateToCurrentPosition">
            <template #icon>
              <van-icon name="aim"/>
            </template>
            定位到当前位置
          </van-button>
        </div>
      </div>

      <div style="margin: 16px;">
        <van-button :loading="submitting" block native-type="submit" round type="primary">
          {{ submitting ? '提交中...' : '提交' }}
        </van-button>
      </div>
    </van-form>

    <van-overlay :show="loading" class="loading-overlay">
      <van-loading type="spinner" vertical>加载中...</van-loading>
    </van-overlay>
  </div>
</template>

<script setup>
import {ref, onMounted, onUnmounted} from 'vue'
import {showDialog, showToast, showLoadingToast} from 'vant'
import {useRouter} from 'vue-router'
import AMapLoader from '@amap/amap-jsapi-loader'
import locationService from "@/utils/locationService"
import api from "@/api/index.js";

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const showPicker = ref(false)
let map = null
let marker = null
let geocoder = null

const shop = ref({
  name: '',
  location: '',
  priceRule: {name: ''},
  longitude: '',
  latitude: ''
})
const customFieldName = {
  text: 'name',
  value: 'id',
};

const priceRules = ref([])

// 初始化地图
const initMap = async () => {
  window._AMapSecurityConfig = {
    securityJsCode: "49013a8c134717569a059ffcb25c0161",
  };
  try {
    loading.value = true
    const position = await locationService.getCurrentLocation()
        .catch(() => ({
          longitude: 112.348196,
          latitude: 37.603501
        }))

    const AMap = await AMapLoader.load({
      key: '27f27344e89695fc415a483f46c9c8a9',
      version: '2.0',
      plugins: ['AMap.Geocoder']
    })

    map = new AMap.Map('container', {
      viewMode: '3D',
      zoom: 17,
      center: [position.longitude, position.latitude]
    })

    marker = new AMap.Marker({
      position: [position.longitude, position.latitude],
      map: map,
      draggable: true,
      animation: 'AMAP_ANIMATION_DROP'
    })

    geocoder = new AMap.Geocoder({
      city: '全国',
      radius: 1000
    })

    map.on('click', handleMapClick)
    marker.on('dragend', handleMarkerDragend)

    await updateLocationInfo([position.longitude, position.latitude])
  } catch (error) {
    console.error('地图初始化失败:', error)
    showToast('地图加载失败，请刷新重试')
  } finally {
    loading.value = false
  }
}

// 处理地图点击
const handleMapClick = (e) => {
  const lnglat = e.lnglat
  marker.setPosition(lnglat)
  updateLocationInfo([lnglat.getLng(), lnglat.getLat()])
}

// 处理标记拖动结束
const handleMarkerDragend = () => {
  const position = marker.getPosition()
  updateLocationInfo([position.getLng(), position.getLat()])
}

// 更新位置信息
const updateLocationInfo = async (position) => {
  try {
    shop.value.longitude = position[0]
    shop.value.latitude = position[1]
    console.log('当前位置:', JSON.stringify(position))
    const result = await new Promise((resolve, reject) => {
      geocoder.getAddress(position, (status, result) => {
        console.log('获取地址信息状态:', JSON.stringify(status))
        console.log('获取地址信息结果:', JSON.stringify(result))
        if (status === 'complete' && result.info === 'OK') {
          resolve(result)
        } else {
          reject(new Error(`获取地址失败: ${result.info}`))
        }
      })
    })

    shop.value.location = result.regeocode.formattedAddress
  } catch (error) {
    console.error('获取地址信息失败:', error)
    showToast('获取地址信息失败')
  }
}

// 重新定位到当前位置
const relocateToCurrentPosition = async () => {
  try {
    showLoadingToast({
      message: '定位中...',
      forbidClick: true
    })

    const position = await locationService.getCurrentLocation()
    if (position) {
      map.setCenter([position.longitude, position.latitude])
      marker.setPosition([position.longitude, position.latitude])
      await updateLocationInfo([position.longitude, position.latitude])
    }
  } catch (error) {
    console.error('重新定位失败:', error)
    showToast('定位失败，请检查定位权限')
  }
}

// 提交表单
const onSubmit = async () => {
  try {
    submitting.value = true
    await showDialog({
      title: '确认提交',
      message: '是否确认添加该商家？',
      showCancelButton: true
    })
    await api.shop.create(shop.value)

    showToast({
      type: 'success',
      message: '添加成功'
    })
    router.back()
  } catch (error) {
    console.error('提交失败:', error)
    showToast('提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

// 选择价格规则
const onConfirm = ({ selectedOptions }) => {
  showPicker.value = false
  shop.value.priceRule = selectedOptions[0]
}

// 获取价格规则列表
const findAllPriceRules = async () => {
  priceRules.value = await api.pricerule.getSimplePriceRules()
}

const onClickLeft = () => router.back()

onMounted(() => {
  initMap()
  findAllPriceRules()
})

onUnmounted(() => {
  if (map) {
    map.destroy()
  }
})
</script>

<style scoped>
.map-container {
  position: relative;
  margin: 16px 0;
}

.map-tip {
  position: absolute;
  top: 8px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  z-index: 1;
}

.map-controls {
  position: absolute;
  bottom: 16px;
  right: 16px;
  z-index: 1;
}

#container {
  width: 100%;
  height: 40vh;
  border-radius: 8px;
  overflow: hidden;
}

.loading-overlay {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
