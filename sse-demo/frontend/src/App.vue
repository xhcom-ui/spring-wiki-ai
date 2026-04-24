<template>
  <div class="page-shell">
    <div class="page-noise"></div>
    <main class="page">
      <section class="hero">
        <div>
          <p class="eyebrow">Vue 3 + SSE + Redis Pub/Sub</p>
          <h1>SSE 消息接收控制台</h1>
          <p class="hero-copy">
            这个前端直接连接后端 <code>/sse/connect</code>，实时展示订单通知、审批提醒、系统公告等事件，并内置测试发送表单。
          </p>
        </div>
        <div class="hero-metrics">
          <article class="metric-card">
            <span class="metric-label">连接状态</span>
            <strong :class="['metric-value', connected ? 'online' : 'offline']">
              {{ connected ? 'ONLINE' : 'OFFLINE' }}
            </strong>
          </article>
          <article class="metric-card">
            <span class="metric-label">已收消息</span>
            <strong class="metric-value">{{ messages.length }}</strong>
          </article>
          <article class="metric-card">
            <span class="metric-label">最近事件</span>
            <strong class="metric-value event-name">{{ lastEventName }}</strong>
          </article>
        </div>
      </section>

      <section class="workspace">
        <article class="panel control-panel">
          <div class="panel-heading">
            <h2>连接控制</h2>
            <p>输入用户 ID，建立或关闭 SSE 连接。</p>
          </div>

          <label class="field">
            <span>用户 ID</span>
            <input v-model.trim="userId" type="text" placeholder="例如 u1001" />
          </label>

          <div class="button-row">
            <button class="primary-btn" :disabled="!canConnect" @click="connect">
              {{ connected ? '重新连接' : '建立连接' }}
            </button>
            <button class="secondary-btn" :disabled="!eventSource" @click="disconnect">
              断开连接
            </button>
          </div>

          <div class="status-box">
            <span class="status-dot" :class="{ active: connected }"></span>
            <div>
              <strong>{{ statusText }}</strong>
              <p>{{ statusDetail }}</p>
            </div>
          </div>
        </article>

        <article class="panel send-panel">
          <div class="panel-heading">
            <h2>测试发消息</h2>
            <p>通过后端 <code>/api/sse/send</code> 验证完整消息链路。</p>
          </div>

          <div class="grid">
            <label class="field">
              <span>目标用户</span>
              <input v-model.trim="sendForm.userId" type="text" placeholder="默认跟随当前连接用户" />
            </label>
            <label class="field">
              <span>消息类型</span>
              <select v-model="sendForm.type">
                <option v-for="item in messageTypes" :key="item" :value="item">
                  {{ item }}
                </option>
              </select>
            </label>
          </div>

          <label class="field">
            <span>消息内容</span>
            <textarea
              v-model="sendForm.content"
              rows="6"
              placeholder='例如 {"orderId":"ORD-1001","status":"PAID"}'
            ></textarea>
          </label>

          <div class="button-row">
            <button class="primary-btn" :disabled="sending || !canSend" @click="sendMessage">
              {{ sending ? '发送中...' : '发送测试消息' }}
            </button>
            <button class="ghost-btn" @click="fillDemoPayload">
              填充示例
            </button>
          </div>

          <p class="inline-message" :class="{ error: sendError }">
            {{ sendFeedback }}
          </p>
        </article>
      </section>

      <section class="panel stream-panel">
        <div class="stream-header">
          <div class="panel-heading compact">
            <h2>实时消息流</h2>
            <p>按接收时间倒序展示，点击可查看原始内容。</p>
          </div>
          <button class="ghost-btn small" :disabled="!messages.length" @click="clearMessages">
            清空列表
          </button>
        </div>

        <div v-if="!messages.length" class="empty-state">
          <strong>还没有收到消息</strong>
          <p>先建立 SSE 连接，再从右侧发送测试消息，或者由其他业务服务调用后端接口推送。</p>
        </div>

        <div v-else class="message-list">
          <article
            v-for="item in messages"
            :key="item.localId"
            class="message-card"
            @click="selectMessage(item)"
          >
            <div class="message-topline">
              <span class="pill">{{ item.type || item.eventName }}</span>
              <time>{{ item.receivedAt }}</time>
            </div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.preview }}</p>
            <footer>
              <span>userId: {{ item.userId || '-' }}</span>
              <span>messageId: {{ item.messageId || '-' }}</span>
            </footer>
          </article>
        </div>
      </section>

      <section v-if="selectedMessage" class="panel detail-panel">
        <div class="stream-header">
          <div class="panel-heading compact">
            <h2>消息详情</h2>
            <p>查看服务端推送过来的完整事件。</p>
          </div>
          <button class="ghost-btn small" @click="closeSelectedMessage">关闭</button>
        </div>
        <pre>{{ JSON.stringify(selectedMessage.raw, null, 2) }}</pre>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'

const messageTypes = ['ORDER_UPDATE', 'APM_ALERT', 'APPROVAL_NOTIFY', 'SYSTEM_NOTICE', 'CUSTOM']

const userId = ref('u1001')
const connected = ref(false)
const eventSource = ref(null)
const statusText = ref('未连接')
const statusDetail = ref('输入用户 ID 后建立 SSE 连接。')
const lastEventName = ref('none')
const messages = ref([])
const selectedMessage = ref(null)
const sending = ref(false)
const sendFeedback = ref('发送表单已就绪。')
const sendError = ref(false)

const sendForm = ref({
  userId: 'u1001',
  type: 'ORDER_UPDATE',
  content: '{"orderId":"ORD-1001","status":"PAID","timestamp":"2026-04-20 10:00:00"}'
})

const canConnect = computed(() => userId.value.length > 0)
const canSend = computed(() => {
  return (sendForm.value.userId || userId.value) && sendForm.value.content.trim()
})

function connect() {
  if (!userId.value) {
    return
  }
  disconnect()

  statusText.value = '连接中'
  statusDetail.value = '正在等待服务器建立 SSE 通道...'

  const source = new EventSource(`/sse/connect?userId=${encodeURIComponent(userId.value)}`)
  eventSource.value = source

  source.onopen = () => {
    connected.value = true
    statusText.value = '已连接'
    statusDetail.value = `当前用户 ${userId.value} 已连接到 SSE 通道。`
  }

  source.onerror = () => {
    connected.value = false
    statusText.value = '连接异常'
    statusDetail.value = '连接中断或服务不可用，请检查后端和 Redis。'
  }

  bindEvent(source, 'connected')
  bindEvent(source, 'message')
  bindEvent(source, 'order_update')
  bindEvent(source, 'apm_alert')
  bindEvent(source, 'approval_notify')
  bindEvent(source, 'system_notice')
  bindEvent(source, 'custom')
}

function bindEvent(source, eventName) {
  source.addEventListener(eventName, (event) => {
    try {
      const parsed = JSON.parse(event.data)
      handleIncomingEvent(eventName, parsed)
    } catch (error) {
      handleIncomingEvent(eventName, {
        type: 'RAW',
        content: event.data
      })
    }
  })
}

function handleIncomingEvent(eventName, payload) {
  lastEventName.value = eventName
  const content = payload?.content ?? ''
  const displayContent = typeof content === 'string' ? content : JSON.stringify(content)
  const item = {
    localId: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    eventName,
    type: payload?.type ?? eventName,
    userId: payload?.userId,
    messageId: payload?.messageId,
    title: resolveTitle(payload?.type ?? eventName),
    preview: displayContent.length > 120 ? `${displayContent.slice(0, 120)}...` : displayContent,
    receivedAt: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
    raw: payload
  }
  messages.value.unshift(item)
  selectedMessage.value = item
}

function resolveTitle(type) {
  switch (type) {
    case 'ORDER_UPDATE':
    case 'order_update':
      return '订单状态更新'
    case 'APM_ALERT':
    case 'apm_alert':
      return '监控告警'
    case 'APPROVAL_NOTIFY':
    case 'approval_notify':
      return '审批通知'
    case 'SYSTEM_NOTICE':
    case 'system_notice':
      return '系统公告'
    case 'connected':
      return '连接初始化'
    default:
      return '自定义消息'
  }
}

function disconnect() {
  if (eventSource.value) {
    eventSource.value.close()
    eventSource.value = null
  }
  connected.value = false
  statusText.value = '已断开'
  statusDetail.value = '当前没有活动中的 SSE 连接。'
}

async function sendMessage() {
  sending.value = true
  sendError.value = false
  sendFeedback.value = '正在调用后端发送消息...'

  try {
    const payload = {
      userId: sendForm.value.userId || userId.value,
      type: sendForm.value.type,
      content: sendForm.value.content
    }

    const response = await fetch('/api/sse/send', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    })
    const result = await response.json()
    if (!response.ok || result.code !== 200) {
      throw new Error(result.msg || '发送失败')
    }
    sendFeedback.value = `发送成功，目标用户 ${payload.userId} 的消息已经提交到 Redis Pub/Sub。`
  } catch (error) {
    sendError.value = true
    sendFeedback.value = error.message || '发送失败'
  } finally {
    sending.value = false
  }
}

function fillDemoPayload() {
  sendForm.value = {
    userId: userId.value || 'u1001',
    type: 'ORDER_UPDATE',
    content: JSON.stringify(
      {
        orderId: `ORD-${Date.now().toString().slice(-6)}`,
        status: 'PAID',
        amount: 188.6,
        timestamp: new Date().toISOString()
      },
      null,
      2
    )
  }
}

function selectMessage(item) {
  selectedMessage.value = item
}

function closeSelectedMessage() {
  selectedMessage.value = null
}

function clearMessages() {
  messages.value = []
  selectedMessage.value = null
}

onBeforeUnmount(() => {
  disconnect()
})
</script>
