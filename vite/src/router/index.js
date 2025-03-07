import { createRouter, createWebHistory } from 'vue-router';
import routes from '~pages'; // 这是 vite-plugin-pages 自动生成的路由文件

const router = createRouter({
    history: createWebHistory(),
    routes, // 使用自动生成的路由配置
});

export default router;
