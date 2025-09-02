import axios from 'axios';
import {showFailToast} from 'vant'; // 替换成你实际使用的 Toast 组件
// import { Loading } from 'element-ui'; // 如果需要 loading 功能，可以引入对应组件

// 根据环境变量选择 baseURL
const BASE_URL =  '/api';

// 创建 axios 实例
const instance = axios.create({
    baseURL: 'https://new.abocidee.com/api', // 使用环境变量中的 API URL
    timeout: 10000, // 请求超时时间
});


// 请求拦截器
instance.interceptors.request.use(
    (config) => {


        return config;
    },
    (error) => {
        // 请求错误处理
        showFailToast('请求发送失败');
        return Promise.reject(error);
    }
);

// 响应拦截器
instance.interceptors.response.use(
    (response) => {
        // 隐藏加载动画（如果需要）
        // if (loadingInstance) loadingInstance.close();

        // 对响应数据处理
        if (response.data.status === 200) {
            return response.data;
        } else {
            return response.data;
        }
    },
    (error) => {
        // 隐藏加载动画（如果需要）
        // if (loadingInstance) loadingInstance.close();

        // 全局错误处理
        const message = error.response?.data?.message || '请求失败';
        showFailToast(message);
        return Promise.reject(error);
    }
);

// 导出 axios 实例
export default instance;
