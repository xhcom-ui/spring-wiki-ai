<!-- SseClient.vue -->
<template>
  <!-- 这是一个无UI的组件，只提供逻辑 -->
</template>

<script>
export default {
  name: 'SseClient',

  props: {
    // 必填：当前用户ID
    userId: {
      type: String,
      required: true
    },
    // SSE服务地址
    sseUrl: {
      type: String,
      default: '/sse/connect'
    },
    // 自动连接
    autoConnect: {
      type: Boolean,
      default: true
    }
  },

  data() {
    return {
      eventSource: null,
      isConnected: false,
      retryCount: 0,
      maxRetries: 3
    }
  },

  created() {
    if (this.autoConnect) {
      this.connect()
    }
  },

  beforeUnmount() {
    this.disconnect()
  },

  methods: {
    /**
     * 建立SSE连接
     */
    connect() {
      if (this.eventSource) {
        this.disconnect()
      }

      const url = `${this.sseUrl}?userId=${this.userId}`
      this.eventSource = new EventSource(url)

      // 连接成功
      this.eventSource.onopen = () => {
        this.isConnected = true
        this.retryCount = 0
        console.log('SSE连接已建立')
        this.$emit('connected')
      }

      // 接收消息
      this.eventSource.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data)
          console.log('收到SSE消息:', message)

          // 根据消息类型分发事件
          this.handleMessage(message)

        } catch (error) {
          console.error('消息解析失败:', error)
        }
      }

      // 自定义事件处理
      this.eventSource.addEventListener('order_update', (event) => {
        this.$emit('order-update', JSON.parse(event.data))
      })

      this.eventSource.addEventListener('alert', (event) => {
        this.$emit('alert', JSON.parse(event.data))
      })

      // 错误处理
      this.eventSource.onerror = (error) => {
        console.error('SSE连接错误:', error)
        this.isConnected = false
        this.$emit('error', error)

        // 自动重连
        if (this.retryCount < this.maxRetries) {
          this.retryCount++
          setTimeout(() => {
            console.log(`第${this.retryCount}次重连...`)
            this.connect()
          }, 3000 * this.retryCount) // 递增延迟
        }
      }
    },

    /**
     * 处理不同类型消息
     */
    handleMessage(message) {
      switch (message.type) {
        case 'ORDER_UPDATE':
          this.handleOrderUpdate(message)
          break
        case 'APM_ALERT':
          this.handleAlert(message)
          break
        case 'APPROVAL_NOTIFY':
          this.handleApproval(message)
          break
        default:
          this.$emit('message', message)
      }
    },

    /**
     * 处理订单更新
     */
    handleOrderUpdate(message) {
      const data = JSON.parse(message.content)
      this.$emit('order-update', {
        orderId: data.orderId,
        status: data.status,
        timestamp: data.timestamp
      })

      // 可触发通知
      this.showNotification(`订单${data.orderId}状态更新为${data.status}`)
    },

    /**
     * 处理监控告警
     */
    handleAlert(message) {
      const data = JSON.parse(message.content)
      this.$emit('alert', {
        level: data.level,
        content: data.message,
        timestamp: data.timestamp
      })

      // 根据不同级别显示不同样式通知
      if (data.level === 'ERROR') {
        this.showErrorNotification(data.message)
      } else {
        this.showWarningNotification(data.message)
      }
    },

    /**
     * 显示通知
     */
    showNotification(text) {
      // 这里可以集成Element UI、Ant Design Vue等通知组件
      this.$notify({
        title: '新消息',
        message: text,
        type: 'info'
      })
    },

    showErrorNotification(text) {
      this.$notify.error({
        title: '系统告警',
        message: text,
        duration: 0 // 不自动关闭
      })
    },

    /**
     * 断开连接
     */
    disconnect() {
      if (this.eventSource) {
        this.eventSource.close()
        this.eventSource = null
        this.isConnected = false
        this.$emit('disconnected')
      }
    },

    /**
     * 手动发送心跳（如果需要）
     */
    sendHeartbeat() {
      if (this.eventSource && this.isConnected) {
        this.eventSource.send('ping')
      }
    }
  },

  // 提供Vue插件形式
  install(Vue) {
    Vue.component('SseClient', this)
  }
}
</script>