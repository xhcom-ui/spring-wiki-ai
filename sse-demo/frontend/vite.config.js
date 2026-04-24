import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3012,
    proxy: {
      '/api': {
        target: 'http://localhost:8089',
        changeOrigin: true
      },
      '/sse': {
        target: 'http://localhost:8089',
        changeOrigin: true
      }
    }
  }
})
