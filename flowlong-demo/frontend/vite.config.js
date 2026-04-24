import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

function isBpmnDependency(id) {
  return [
    'bpmn-js',
    'diagram-js',
    'bpmn-moddle',
    'moddle',
    'moddle-xml',
    'min-dash',
    'min-dom',
    'tiny-svg',
    'tiny-warning',
    'tiny-case',
    'ids',
    'inherits-browser',
    'path-intersection',
    'object-refs'
  ].some((dep) => id.includes(`/node_modules/${dep}/`) || id.includes(`\\node_modules\\${dep}\\`))
}

export default defineConfig({
  plugins: [vue()],
  build: {
    chunkSizeWarningLimit: 650,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return
          }
          if (isBpmnDependency(id)) {
            return 'bpmn-engine'
          }
          if (id.includes('vue') || id.includes('vue-router')) {
            return 'vue-core'
          }
          if (id.includes('axios')) {
            return 'http-client'
          }
        }
      }
    }
  },
  server: {
    port: 3005,
    proxy: {
      '/api': {
        target: 'http://localhost:8087',
        changeOrigin: true
      }
    }
  }
})
