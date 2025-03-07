// src/services/amapService.js
import { load } from '@amap/amap-jsapi-loader';

export const loadAMap = () => {





    return new Promise((resolve, reject) => {
        load({
            key: "27f27344e89695fc415a483f46c9c8a9", // 替换为你自己的Key
            version: "2.0",
            plugins: ["AMap.Scale", "AMap.Geolocation", "AMap.LabelMarker", "AMap.Driving"]
        })
            .then(AMaps => resolve(AMaps))
            .catch(error => reject(error));
    });
};

// 获取当前位置
// export const getCurrentLocation = (AMaps) => {
//     return new Promise((resolve, reject) => {
//         const geolocation = new AMaps.Geolocation({
//             enableHighAccuracy: true,
//             timeout: 10000,
//         });
//
//         geolocation.getCurrentPosition((status, result) => {
//             if (status === 'complete' && result.info === 'SUCCESS') {
//                 console.log('***当前位置***'+result.position)
//                 resolve(result.position); // 返回定位结果
//             } else {
//                 reject(result); // 定位失败
//             }
//         });
//     });
// };
import { registerPlugin   } from '@capacitor/core';

const CameraPlugin = registerPlugin('MyLocation');
// 封装获取当前位置信息的函数
export const getCurrentLocation = () => {
    return new Promise((resolve, reject) => {
        CameraPlugin.startLocationService()
            .then(result => {
                console.log('Location:', result.longitude+','+result.latitude);
                resolve(result); // 操作成功，返回位置信息
            })
            .catch(error => {
                console.error('Error:', error);
                reject(error); // 操作失败，返回错误信息
            });
    });
};

// 逆地理编码
export const reverseGeocode = (AMaps, lnglat, callback) => {
    const geocoder = new AMaps.Geocoder();
    geocoder.getAddress(lnglat, (status, result) => {
        if (status === 'complete' && result.info === 'OK') {
            callback(result.regeocode.formattedAddress);
        } else {
            callback(null);
        }
    });
};
