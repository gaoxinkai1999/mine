import {createApp} from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import piniaPersist from 'pinia-plugin-persistedstate'

import router from "./router/index.js";
import { initializeUpdateService } from './services/updateService'; // 导入服务初始化函数


const app = createApp(App)
const pinia = createPinia()
pinia.use(piniaPersist)
app.use(router)
app.use(pinia)

// 在设置核心插件后初始化我们的自定义服务
initializeUpdateService();

app.mount('#app')
