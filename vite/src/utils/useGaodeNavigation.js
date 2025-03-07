// useGaodeNavigation.js
export function useGaodeNavigation() {
    // 高德地图导航方法
    const navigateToLocation = (longitude, latitude, name) => {
        const url = 'amapuri://route/plan/?did=&dlon=' + longitude + '&dlat=' + latitude + '&dname=' + name + '&dev=0&t=0'
        // 打开链接，尝试启动高德地图 App 进行导航
        window.location.href = url;
    };

    return {
        navigateToLocation
    };
}
