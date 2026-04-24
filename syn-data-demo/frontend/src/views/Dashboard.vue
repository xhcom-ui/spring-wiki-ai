<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon primary">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ summary.dataSourceCount }}</div>
              <div class="stat-label">数据源</div>
              <div class="stat-trend positive">
                <el-icon><ArrowUp /></el-icon>
                <span>{{ summary.watcherCount }} 个 watcher</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon success">
              <el-icon><Refresh /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ summary.syncTaskCount }}</div>
              <div class="stat-label">同步任务</div>
              <div class="stat-trend positive">
                <el-icon><ArrowUp /></el-icon>
                <span>队列 {{ queueLength }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon warning">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ summary.successCount }}</div>
              <div class="stat-label">成功次数</div>
              <div class="stat-trend positive">
                <el-icon><ArrowUp /></el-icon>
                <span>近 24h {{ summary.recentSuccessCount }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon danger">
              <el-icon><CircleClose /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ summary.failureCount }}</div>
              <div class="stat-label">失败次数</div>
              <div class="stat-trend negative">
                <el-icon><ArrowDown /></el-icon>
                <span>近 24h {{ summary.recentFailureCount }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>最近同步任务</span>
              <el-button type="primary" size="small" @click="refreshTasks">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>
          <el-table
            :data="recentTasks"
            style="width: 100%"
            :row-style="{ cursor: 'pointer' }"
            @row-click="viewTaskDetail"
            v-loading="loading"
          >
            <el-table-column prop="taskName" label="任务名称" min-width="160" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag
                  :type="scope.row.status === 'success' ? 'success' : scope.row.status === 'running' ? 'warning' : 'danger'"
                  effect="dark"
                >
                  {{ getStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="startTime" label="开始时间" min-width="180" />
            <el-table-column prop="duration" label="耗时" width="100">
              <template #default="scope">
                <span>{{ formatDuration(scope.row.duration) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="scope">
                <el-button
                  v-if="canManage"
                  type="primary"
                  size="small"
                  @click.stop="rerunTask(scope.row.taskId)"
                >
                  重跑
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && !recentTasks.length" description="暂无执行记录" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>系统状态</span>
              <el-button type="info" size="small" @click="refreshStatus">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>
          <div class="system-status">
            <div class="status-item" v-for="status in systemStatus" :key="status.name">
              <div class="status-info">
                <span class="status-label">{{ status.name }}</span>
                <span class="status-desc">{{ status.description }}</span>
              </div>
              <el-tag :type="status.level" effect="dark">
                {{ status.value }}
              </el-tag>
            </div>
            <div class="system-metrics">
              <div class="metric-item">
                <span class="metric-label">平均吞吐</span>
                <span class="metric-value">{{ speedStats.averageSpeed || 0 }} 条/秒</span>
              </div>
              <div class="metric-item">
                <span class="metric-label">CPU 使用率</span>
                <span class="metric-value">{{ resourceUsage.cpuUsage ?? 0 }}%</span>
              </div>
              <div class="metric-item">
                <span class="metric-label">内存使用率</span>
                <span class="metric-value">{{ resourceUsage.memoryUsage ?? 0 }}%</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :xs="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>同步趋势</span>
              <el-select v-model="trendPeriod" size="small" style="width: 120px;" @change="fetchHistory">
                <el-option label="今日" value="1d" />
                <el-option label="本周" value="7d" />
                <el-option label="本月" value="30d" />
              </el-select>
            </div>
          </template>
          <div class="chart-container">
            <el-empty v-if="!chartData.length" description="暂无数据" />
            <div v-else id="trendChart" class="chart"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="detailsVisible" title="任务执行详情" width="700px">
      <el-descriptions v-if="currentTask" :column="1" border>
        <el-descriptions-item label="任务名称">{{ currentTask.taskName }}</el-descriptions-item>
        <el-descriptions-item label="同步模式">{{ currentTask.syncMode === 'full' ? '全量' : '增量' }}</el-descriptions-item>
        <el-descriptions-item label="执行状态">{{ getStatusText(currentTask.status) }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ currentTask.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ currentTask.endTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ formatDuration(currentTask.duration) }}</el-descriptions-item>
        <el-descriptions-item label="源端数据量">{{ currentTask.sourceCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="目标成功量">{{ currentTask.targetCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="失败数量">{{ currentTask.errorCount ?? 0 }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import { computed, ref, onMounted, onUnmounted, nextTick } from 'vue'
import { DataAnalysis, ArrowUp, Refresh, CircleCheck, CircleClose, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { echarts } from '../utils/echarts'
import axios from '../utils/request'
import { isAdmin } from '../utils/auth'

export default {
  name: 'Dashboard',
  components: {
    DataAnalysis,
    ArrowUp,
    Refresh,
    CircleCheck,
    CircleClose,
    ArrowDown
  },
  setup() {
    const canManage = ref(isAdmin())
    const loading = ref(false)
    const summary = ref({
      dataSourceCount: 0,
      syncTaskCount: 0,
      watcherCount: 0,
      alertCount: 0,
      successCount: 0,
      failureCount: 0,
      recentSuccessCount: 0,
      recentFailureCount: 0
    })
    const queueLength = ref(0)
    const speedStats = ref({})
    const resourceUsage = ref({})
    const trendPeriod = ref('7d')
    const recentTasks = ref([])
    const chartData = ref([])
    const currentTask = ref(null)
    const detailsVisible = ref(false)

    const systemStatus = computed(() => [
      {
        name: '运行中任务',
        description: '当前执行中的同步任务数量',
        value: String(summary.value.syncTaskCount || 0),
        level: 'success'
      },
      {
        name: 'Watcher 队列',
        description: 'watcher 配置总数 / 当前积压队列',
        value: `${summary.value.watcherCount || 0} / ${queueLength.value || 0}`,
        level: queueLength.value > 500 ? 'danger' : 'info'
      },
      {
        name: '执行成功率',
        description: '基于当前时间范围内的执行日志',
        value: `${historySuccessRate.value}%`,
        level: historySuccessRate.value >= 80 ? 'success' : 'warning'
      },
      {
        name: '告警记录',
        description: '累计已持久化告警条数',
        value: String(summary.value.alertCount || 0),
        level: summary.value.alertCount > 0 ? 'warning' : 'success'
      }
    ])

    const historySuccessRate = computed(() => {
      return historyData.value.successRate?.rate || 0
    })

    const historyData = ref({
      trendData: [],
      performanceAnalysis: {},
      successRate: {},
      executionTimeDistribution: {}
    })

    let trendChart = null
    let resizeHandler = null

    const initChart = () => {
      nextTick(() => {
        const chartDom = document.getElementById('trendChart')
        if (!chartDom) {
          return
        }
        if (trendChart) {
          trendChart.dispose()
        }
        trendChart = echarts.init(chartDom)
        updateChart()
      })
    }

    const updateChart = () => {
      if (!trendChart || !chartData.value.length) {
        return
      }
      trendChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['成功', '失败'], top: 0 },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: chartData.value.map(item => item.date)
        },
        yAxis: { type: 'value' },
        series: [
          {
            name: '成功',
            type: 'line',
            smooth: true,
            data: chartData.value.map(item => item.successCount),
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(103, 194, 58, 0.5)' },
                { offset: 1, color: 'rgba(103, 194, 58, 0.1)' }
              ])
            }
          },
          {
            name: '失败',
            type: 'line',
            smooth: true,
            data: chartData.value.map(item => item.failedCount),
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(245, 108, 108, 0.45)' },
                { offset: 1, color: 'rgba(245, 108, 108, 0.08)' }
              ])
            }
          }
        ]
      })
    }

    const fetchRealtime = async () => {
      const response = await axios.get('/api/monitoring/realtime')
      const payload = response.data || response
      summary.value = payload.summary || summary.value
      recentTasks.value = payload.recentTasks || []
      speedStats.value = payload.speedStats || {}
      resourceUsage.value = payload.resourceUsage || {}
      queueLength.value = payload.queueLength || 0
    }

    const fetchHistory = async () => {
      const response = await axios.get('/api/monitoring/history', {
        params: { timeRange: trendPeriod.value }
      })
      const payload = response.data || response
      historyData.value = payload || historyData.value
      chartData.value = payload.trendData || []
      initChart()
    }

    const refreshTasks = async () => {
      loading.value = true
      try {
        await fetchRealtime()
        ElMessage.success('任务数据已刷新')
      } catch (error) {
        console.error('刷新任务失败:', error)
        ElMessage.error('刷新任务失败')
      } finally {
        loading.value = false
      }
    }

    const refreshStatus = async () => {
      loading.value = true
      try {
        await Promise.all([fetchRealtime(), fetchHistory()])
        ElMessage.success('系统状态已刷新')
      } catch (error) {
        console.error('刷新状态失败:', error)
        ElMessage.error('刷新状态失败')
      } finally {
        loading.value = false
      }
    }

    const viewTaskDetail = async (row) => {
      try {
        const response = await axios.get('/api/monitoring/task/detail', {
          params: {
            taskId: row.taskId,
            logId: row.id
          }
        })
        currentTask.value = response.data || response
        detailsVisible.value = true
      } catch (error) {
        console.error('获取任务详情失败:', error)
        ElMessage.error('获取任务详情失败')
      }
    }

    const rerunTask = async (taskId) => {
      try {
        await axios.post(`/api/task/${taskId}/execute`)
        ElMessage.success('已触发重跑')
        refreshTasks()
      } catch (error) {
        console.error('重跑任务失败:', error)
        ElMessage.error('重跑任务失败')
      }
    }

    const getStatusText = (status) => {
      switch (status) {
        case 'success':
          return '成功'
        case 'failed':
          return '失败'
        case 'running':
          return '运行中'
        default:
          return '未知'
      }
    }

    const formatDuration = (duration) => {
      if (!duration && duration !== 0) {
        return '-'
      }
      if (duration < 1000) {
        return `${duration}ms`
      }
      return `${(duration / 1000).toFixed(1)}s`
    }

    const handleResize = () => {
      if (trendChart) {
        trendChart.resize()
      }
    }

    onMounted(async () => {
      loading.value = true
      try {
        await Promise.all([fetchRealtime(), fetchHistory()])
      } finally {
        loading.value = false
      }
      resizeHandler = handleResize
      window.addEventListener('resize', resizeHandler)
    })

    onUnmounted(() => {
      if (resizeHandler) {
        window.removeEventListener('resize', resizeHandler)
      }
      if (trendChart) {
        trendChart.dispose()
        trendChart = null
      }
    })

    return {
      canManage,
      loading,
      summary,
      queueLength,
      speedStats,
      resourceUsage,
      trendPeriod,
      recentTasks,
      chartData,
      systemStatus,
      currentTask,
      detailsVisible,
      refreshTasks,
      refreshStatus,
      viewTaskDetail,
      rerunTask,
      getStatusText,
      formatDuration
    }
  }
}
</script>

<style scoped>
.stat-card {
  margin-bottom: 20px;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 28px;
  margin-right: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-icon.primary {
  background: linear-gradient(135deg, #409eff, #66b1ff);
}

.stat-icon.success {
  background: linear-gradient(135deg, #67c23a, #85ce61);
}

.stat-icon.warning {
  background: linear-gradient(135deg, #e6a23c, #ebb563);
}

.stat-icon.danger {
  background: linear-gradient(135deg, #f56c6c, #f78989);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}

.stat-trend {
  display: flex;
  align-items: center;
  margin-top: 8px;
  font-size: 12px;
}

.stat-trend.positive {
  color: #67c23a;
}

.stat-trend.negative {
  color: #f56c6c;
}

.stat-trend span {
  margin-left: 4px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.system-status {
  padding: 10px 0;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
  transition: all 0.3s ease;
}

.status-item:hover {
  background-color: rgba(64, 158, 255, 0.05);
  padding-left: 10px;
  border-radius: 4px;
}

.status-item:last-child {
  border-bottom: none;
}

.status-info {
  flex: 1;
}

.status-label {
  display: block;
  color: #303133;
  font-weight: 500;
  margin-bottom: 4px;
}

.status-desc {
  display: block;
  color: #909399;
  font-size: 12px;
}

.system-metrics {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.metric-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.metric-label {
  color: #606266;
}

.metric-value {
  color: #303133;
  font-weight: 600;
}

.chart-container {
  min-height: 320px;
}

.chart {
  width: 100%;
  height: 320px;
}
</style>
