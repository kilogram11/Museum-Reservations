import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router' // 后续创建路由文件

const app = createApp(App)
app.use(ElementPlus)
app.use(router)
app.mount('#app')