import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import Pages from 'vite-plugin-pages';
import path from 'path'; // 添加这行
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import {VantResolver} from '@vant/auto-import-resolver';
import {ElementPlusResolver} from 'unplugin-vue-components/resolvers'
const versionConfig = require('../versions.config.js'); // 读取项目根目录的配置

export default defineConfig({
    define: {
      // 将版本号和日志定义为全局常量
      // JSON.stringify 是必须的，因为它会把字符串包装在引号中
      '__APP_VERSION__': JSON.stringify(versionConfig.webVersion),
      '__APP_RELEASE_NOTES__': JSON.stringify(versionConfig.webReleaseNotes || "") 
    },
    plugins: [vue(),
        Pages({
            // 配置项（可选）
            dirs: './src/pages', // 默认扫描的文件夹
            extensions: ['vue'], // 文件后缀
            importMode: 'async'//懒加载
        }),
        AutoImport({
            resolvers: [VantResolver(), ElementPlusResolver()],
        }),
        Components({
            resolvers: [VantResolver(), ElementPlusResolver()], // 自动引入 Vant 组件
        }),
    ],
    /**
     * idea不能识别
     * import api from '@/api';
     * 别名
     * 添加以下配置解决
     */
    server: {				// ← ← ← ← ← ←
        host: '0.0.0.0'	// ← 新增内容 ←
    },

    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'), // 定义 `@` 为 `src` 目录
        },
    },



})
