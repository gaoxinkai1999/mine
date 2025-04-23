import {createApp} from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import piniaPersist from 'pinia-plugin-persistedstate'

import router from "./router/index.js";



const app = createApp(App)
const pinia = createPinia()
pinia.use(piniaPersist)
app.use(router)
app.use(pinia).mount('#app')



