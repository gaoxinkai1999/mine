import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import Pages from 'vite-plugin-pages';
import path from 'path'; // 添加这行
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import {VantResolver} from '@vant/auto-import-resolver';
import {ElementPlusResolver} from 'unplugin-vue-components/resolvers'
import fs from 'fs';

export default defineConfig({
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
        {
            name: 'version-update',
            buildStart() {
                // 读取 package.json 中的版本号
                const pkg = JSON.parse(fs.readFileSync('./version.json', 'utf-8'));

                // 更新版本配置文件
                const versionConfig = `
          export const APP_VERSION = {
            version: '${pkg.version}',
            buildDate: '${new Date().toISOString()}',
            changelog:'${pkg.changelog}'
          };
        `;

                fs.writeFileSync('./src/config/version.js', versionConfig);
            }
        }

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
