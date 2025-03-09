// src/utils/locationService.js
import {registerPlugin} from '@capacitor/core';
import api from "@/api";

const MyLocation = registerPlugin('MyLocation');
const MyDistance = registerPlugin('MyDistance');

export default {
    // 仅获取当前位置
    async getCurrentLocation() {
        try {
            return await MyLocation.startLocationService();
        } catch (error) {
            console.error('获取当前位置失败:', error);
            throw error;
        }
    },

    // 获取附近商店
    async getNearbyShops() {
        try {
            let complexData = {
                currentLocation: null,
                shopLocations: null
            };

            // 获取当前位置
            complexData.currentLocation = await this.getCurrentLocation();

            // 获取所有商店位置
            complexData.shopLocations = await api.shop.getShopLocations();

            // 计算距离并排序
            let result = await MyDistance.getDistance(complexData);

            return {
                currentLocation: complexData.currentLocation,
                nearbyShops: result.shops.slice(0, 10)
            };
        } catch (error) {
            console.error('获取附近商店失败:', error);
            throw error;
        }
    },


};
