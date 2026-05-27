import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src') // 配置@指向src目录
    }
  },
  server: {
    port: 3000, // 前端运行端口
    proxy: {
      // 配置后端接口代理（解决跨域）
      '/api': {
        target: 'http://localhost:8081', // 后端接口地址
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      // 新增：静态文件代理
      '/files': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})