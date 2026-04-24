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
          if (id.includes('/node_modules/element-plus/') || id.includes('\\node_modules\\element-plus\\')) {
            return 'element-plus'
          }
          if (id.includes('/node_modules/@element-plus/') || id.includes('\\node_modules\\@element-plus\\')) {
            return 'element-plus'
          }
          if (id.includes('/node_modules/jsoneditor/') || id.includes('\\node_modules\\jsoneditor\\')) {
            return 'json-editor'
          }
          if (id.includes('/node_modules/html2canvas/') || id.includes('\\node_modules\\html2canvas\\')) {
            return 'capture-tools'
          }
          if (
            id.includes('/node_modules/vue/') ||
            id.includes('\\node_modules\\vue\\') ||
            id.includes('/node_modules/@vue/') ||
            id.includes('\\node_modules\\@vue\\') ||
            id.includes('/node_modules/vue-router/') ||
            id.includes('\\node_modules\\vue-router\\')
          ) {
            return 'vue-core'
          }
          if (id.includes('/node_modules/axios/') || id.includes('\\node_modules\\axios\\')) {
            return 'http-client'
          }
        }
      }
    }
  },
  server: {
    port: 3006,
    proxy: {
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true
      }
    }
  }
})
