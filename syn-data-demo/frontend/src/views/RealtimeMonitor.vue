<template>
  <div class="realtime-monitor">
    <h2>实时同步监控</h2>

    <!-- 实时状态卡片 -->
    <el-row :gutter="20" class="status-cards">
      <el-col :span="6">
        <el-card class="status-card">
          <div class="status-item">
            <div class="status-label">运行中的监听</div>
            <div class="status-value success">{{ runningListeners }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="status-card">
          <div class="status-item">
            <div class="status-label">待处理队列</div>
            <div class="status-value warning">{{ pendingQueue }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="status-card">
          <div class="status-item">
            <div class="status-label">实时同步速率</div>
            <div class="status-value">{{ syncRate }} 条/秒</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="status-card">
          <div class="status-item">
            <div class="status-label">累计同步数据</div>
            <div class="status-value">{{ totalSynced }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 实时同步任务列表 -->
    <el-card class="task-list">
      <template #header>
        <div class="card-header">
          <span>实时同步任务</span>
          <el-button type="primary" @click="refreshStatus">刷新状态</el-button>
        </div>
      </template>

      <el-table :data="realtimeTasks" style="width: 100%">
        <el-table-column prop="taskName" label="任务名称" />
        <el-table-column prop="dataSourceType" label="数据源类型">
          <template #default="scope">
            <el-tag :type="scope.row.dataSourceType === 'mysql' ? 'success' : 'primary'">
              {{ scope.row.dataSourceType === 'mysql' ? 'MySQL' : 'PostgreSQL' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetIndex" label="目标索引" />
        <el-table-column prop="status" label="监听状态">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'running' ? 'success' : 'danger'">
              {{ scope.row.status === 'running' ? '运行中' : '已停止' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="queueSize" label="队列大小" />
        <el-table-column prop="syncedCount" label="已同步数量" />
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button
              v-if="scope.row.status !== 'running'"
              type="success"
              size="small"
              @click="startListener(scope.row)"
            >
              启动
            </el-button>
            <el-button
              v-else
              type="danger"
              size="small"
              @click="stopListener(scope.row)"
            >
              停止
            </el-button>
            <el-button type="primary" size="small" @click="viewDetails(scope.row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!realtimeTasks.length" description="暂无 watcher 配置，请先在 Watcher 配置页保存" />
    </el-card>

    <!-- 实时性能图表 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>实时同步速率</span>
          </template>
          <div ref="rateChart" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>队列长度变化</span>
          </template>
          <div ref="queueChart" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 实时日志 -->
    <el-card class="log-card">
      <template #header>
        <div class="card-header">
          <span>实时日志</span>
          <el-button type="text" @click="clearLogs">清空</el-button>
        </div>
      </template>
      <div class="log-container">
        <div
          v-for="(log, index) in logs"
          :key="index"
          class="log-item"
          :class="log.level"
        >
          <span class="log-time">{{ log.time }}</span>
          <span class="log-level">[{{ log.level.toUpperCase() }}]</span>
          <span class="log-message">{{ log.message }}</span>
        </div>
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailsVisible"
      title="实时同步详情"
      width="60%"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务名称">{{ currentTask?.taskName }}</el-descriptions-item>
        <el-descriptions-item label="数据源类型">{{ currentTask?.dataSourceType }}</el-descriptions-item>
        <el-descriptions-item label="数据源ID">{{ currentTask?.dataSourceId }}</el-descriptions-item>
        <el-descriptions-item label="目标索引">{{ currentTask?.targetIndex }}</el-descriptions-item>
        <el-descriptions-item label="监听状态">{{ currentTask?.status }}</el-descriptions-item>
        <el-descriptions-item label="队列大小">{{ currentTask?.queueSize }}</el-descriptions-item>
        <el-descriptions-item label="已同步数量">{{ currentTask?.syncedCount }}</el-descriptions-item>
        <el-descriptions-item label="最后同步时间">{{ currentTask?.lastSyncTime }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-actions">
        <el-button type="primary" @click="refreshTaskStatus">刷新状态</el-button>
        <el-button @click="detailsVisible = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { echarts } from '../utils/echarts'
import axios from '../utils/request'

// 状态数据
const runningListeners = ref(0)
const pendingQueue = ref(0)
const syncRate = ref(0)
const totalSynced = ref(0)

// 实时任务列表
const realtimeTasks = ref([])

// 日志数据
const logs = ref([
  { time: '2024-01-15 14:30:25', level: 'info', message: 'MySQL订单实时同步: 成功同步 50 条数据' },
  { time: '2024-01-15 14:30:22', level: 'info', message: 'PostgreSQL用户实时同步: 成功同步 30 条数据' },
  { time: '2024-01-15 14:30:20', level: 'warning', message: 'MySQL商品实时同步: 队列积压，当前队列大小: 200' },
  { time: '2024-01-15 14:30:15', level: 'info', message: 'MySQL订单实时同步: Binlog监听正常' },
  { time: '2024-01-15 14:30:10', level: 'error', message: 'PostgreSQL用户实时同步: 连接断开，正在重试...' }
])

// 图表引用
const rateChart = ref(null)
const queueChart = ref(null)
let rateChartInstance = null
let queueChartInstance = null
let resizeHandler = null

// 详情对话框
const detailsVisible = ref(false)
const currentTask = ref(null)

// 定时器
let refreshTimer = null

// 初始化图表
const initCharts = () => {
  // 同步速率图表
  if (rateChartInstance) {
    rateChartInstance.dispose()
  }
  rateChartInstance = echarts.init(rateChart.value)
  const rateOption = {
    xAxis: {
      type: 'category',
      data: ['10:00', '10:05', '10:10', '10:15', '10:20', '10:25', '10:30']
    },
    yAxis: {
      type: 'value',
      name: '条/秒'
    },
    series: [{
      data: [120, 132, 101, 134, 90, 230, 210],
      type: 'line',
      smooth: true,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
          { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
        ])
      }
    }]
  }
  rateChartInstance.setOption(rateOption)

  // 队列长度图表
  if (queueChartInstance) {
    queueChartInstance.dispose()
  }
  queueChartInstance = echarts.init(queueChart.value)
  const queueOption = {
    xAxis: {
      type: 'category',
      data: ['10:00', '10:05', '10:10', '10:15', '10:20', '10:25', '10:30']
    },
    yAxis: {
      type: 'value',
      name: '队列长度'
    },
    series: [{
      data: [50, 80, 120, 90, 150, 180, 245],
      type: 'line',
      smooth: true,
      itemStyle: {
        color: '#E6A23C'
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(230, 162, 60, 0.3)' },
          { offset: 1, color: 'rgba(230, 162, 60, 0.05)' }
        ])
      }
    }]
  }
  queueChartInstance.setOption(queueOption)
}

const toTaskRow = (item) => ({
  id: item.id,
  taskName: item.description || `${item.table || 'unknown'} watcher`,
  dataSourceType: item.sourceType,
  targetIndex: item.targetIndex,
  status: item.status || 'stopped',
  queueSize: item.queueSize || 0,
  syncedCount: item.syncedCount || 0,
  dataSourceId: item.sourceId,
  hostName: item.hostName,
  database: item.database,
  table: item.table,
  incrementalField: item.incrementalField,
  eventTypes: item.eventTypes || [],
  lastSyncTime: item.lastSyncTime || ''
})

const fetchWatchers = async () => {
  try {
    const response = await axios.get('/api/watchers')
    const payload = response.data || response
    realtimeTasks.value = (payload || []).map(toTaskRow)
  } catch (error) {
    ElMessage.error('获取 watcher 配置失败: ' + error.message)
  }
}

// 刷新状态
const refreshStatus = async () => {
  try {
    await fetchWatchers()

    // 计算统计数据
    runningListeners.value = realtimeTasks.value.filter(t => t.status === 'running').length
    pendingQueue.value = realtimeTasks.value.reduce((sum, t) => sum + t.queueSize, 0)
    syncRate.value = Math.floor(Math.random() * 500) + 100
    totalSynced.value = realtimeTasks.value.reduce((sum, t) => sum + t.syncedCount, 0)

    // 获取每个任务的实时状态
    for (const task of realtimeTasks.value) {
      try {
        const response = await axios.get(`/api/watchers/${task.id}/status`)
        const payload = response.data || response
        if (payload) {
          task.status = payload.isRunning ? 'running' : 'stopped'
          task.queueSize = payload.queueSize || 0
          task.syncedCount = payload.syncedCount || task.syncedCount
          task.lastSyncTime = payload.lastSyncTime || task.lastSyncTime
        }
      } catch (error) {
        console.error(`获取任务 ${task.taskName} 状态失败:`, error)
      }
    }

    ElMessage.success('状态刷新成功')
  } catch (error) {
    ElMessage.error('刷新状态失败: ' + error.message)
  }
}

// 启动监听
const startListener = async (task) => {
  try {
    const response = await axios.post(`/api/watchers/${task.id}/start`)
    const payload = response.data || response
    if (payload.success) {
      task.status = 'running'
    }
    task.status = payload.status || 'running'
    task.lastSyncTime = payload.lastSyncTime || task.lastSyncTime
    ElMessage.success('监听启动成功')
    addLog('info', `${task.taskName}: 监听已启动`)
  } catch (error) {
    ElMessage.error('监听启动失败: ' + error.message)
  }
}

// 停止监听
const stopListener = async (task) => {
  try {
    const response = await axios.post(`/api/watchers/${task.id}/stop`)
    const payload = response.data || response
    task.status = payload.status || 'stopped'
    ElMessage.success('监听停止成功')
    addLog('info', `${task.taskName}: 监听已停止`)
  } catch (error) {
    ElMessage.error('监听停止失败: ' + error.message)
  }
}

// 查看详情
const viewDetails = (task) => {
  currentTask.value = task
  detailsVisible.value = true
}

// 刷新任务状态
const refreshTaskStatus = async () => {
  if (currentTask.value) {
    await refreshStatus()
    ElMessage.success('任务状态已刷新')
  }
}

// 添加日志
const addLog = (level, message) => {
  const now = new Date()
  const time = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
  logs.value.unshift({ time, level, message })
  
  // 限制日志数量
  if (logs.value.length > 100) {
    logs.value = logs.value.slice(0, 100)
  }
}

// 清空日志
const clearLogs = () => {
  logs.value = []
}

// 模拟实时数据更新
const simulateRealtimeData = () => {
  // 随机更新同步速率
  syncRate.value = Math.floor(Math.random() * 300) + 50
  
  // 随机更新队列大小
  realtimeTasks.value.forEach(task => {
    if (task.status === 'running') {
      const change = Math.floor(Math.random() * 20) - 10
      task.queueSize = Math.max(0, task.queueSize + change)
      task.syncedCount += Math.floor(Math.random() * 50)
    }
  })
  
  // 更新统计
  pendingQueue.value = realtimeTasks.value.reduce((sum, t) => sum + t.queueSize, 0)
  totalSynced.value = realtimeTasks.value.reduce((sum, t) => sum + t.syncedCount, 0)
}

onMounted(() => {
  initCharts()
  refreshStatus()
  
  // 定时刷新数据
  refreshTimer = setInterval(() => {
    simulateRealtimeData()
  }, 3000)
  
  // 窗口大小改变时重新渲染图表
  resizeHandler = () => {
    rateChartInstance?.resize()
    queueChartInstance?.resize()
  }
  window.addEventListener('resize', resizeHandler)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
  }
  rateChartInstance?.dispose()
  queueChartInstance?.dispose()
})
</script>

<style scoped>
.realtime-monitor {
  padding: 20px;
}

.status-cards {
  margin-bottom: 20px;
}

.status-card {
  text-align: center;
}

.status-item {
  padding: 10px;
}

.status-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 10px;
}

.status-value {
  font-size: 28px;
  font-weight: bold;
  color: #409EFF;
}

.status-value.success {
  color: #67C23A;
}

.status-value.warning {
  color: #E6A23C;
}

.task-list {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.charts-row {
  margin-bottom: 20px;
}

.chart {
  height: 300px;
}

.log-card {
  margin-bottom: 20px;
}

.log-container {
  max-height: 300px;
  overflow-y: auto;
  background-color: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
}

.log-item {
  padding: 5px 0;
  font-family: monospace;
  font-size: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.log-item:last-child {
  border-bottom: none;
}

.log-time {
  color: #909399;
  margin-right: 10px;
}

.log-level {
  font-weight: bold;
  margin-right: 10px;
}

.log-item.info .log-level {
  color: #409EFF;
}

.log-item.warning .log-level {
  color: #E6A23C;
}

.log-item.error .log-level {
  color: #F56C6C;
}

.log-message {
  color: #303133;
}

.detail-actions {
  margin-top: 20px;
  text-align: right;
}
</style>
