import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom', // 模拟浏览器环境（关键）
    globals: true,        // 支持像 describe, it, expect 这样的全局变量
  },
})
