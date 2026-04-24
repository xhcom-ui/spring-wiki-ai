<template>
  <div class="task-log">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>执行日志</span>
          <div class="toolbar">
            <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 140px" @change="handleFilterChange">
              <el-option label="成功" value="success" />
              <el-option label="失败" value="failed" />
              <el-option label="运行中" value="running" />
            </el-select>
            <el-input-number v-model="filters.taskId" :min="1" controls-position="right" placeholder="任务ID" @change="handleFilterChange" />
            <el-button type="primary" @click="fetchLogs">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="logList" style="width: 100%" v-loading="loading">
        <el-table-column prop="taskId" label="任务ID" width="90" />
        <el-table-column prop="taskName" label="任务名称" min-width="180" />
        <el-table-column prop="syncMode" label="同步模式" width="110">
          <template #default="scope">
            <el-tag :type="scope.row.syncMode === 'full' ? 'primary' : 'success'">
              {{ scope.row.syncMode === 'full' ? '全量' : '增量' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="执行状态" width="110">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" min-width="180" />
        <el-table-column prop="endTime" label="结束时间" min-width="180" />
        <el-table-column prop="duration" label="耗时(ms)" width="110" />
        <el-table-column prop="totalCount" label="源端数据量" width="120" />
        <el-table-column prop="successCount" label="目标成功量" width="120" />
        <el-table-column prop="failedCount" label="失败数量" width="100" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleView(scope.row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !logList.length" description="暂无执行日志" />

      <el-pagination
        class="pagination"
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" title="执行详情" width="700px">
      <el-descriptions :column="1" border v-if="currentLog">
        <el-descriptions-item label="任务ID">{{ currentLog.taskId }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ currentLog.taskName }}</el-descriptions-item>
        <el-descriptions-item label="同步模式">{{ currentLog.syncMode === 'full' ? '全量' : '增量' }}</el-descriptions-item>
        <el-descriptions-item label="执行状态">
          <el-tag :type="getStatusType(currentLog.status)">
            {{ getStatusText(currentLog.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ currentLog.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ currentLog.endTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ currentLog.duration || 0 }}ms</el-descriptions-item>
        <el-descriptions-item label="源端数据量">{{ currentLog.sourceCount ?? currentLog.totalCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="目标成功量">{{ currentLog.targetCount ?? currentLog.successCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="失败数量">{{ currentLog.errorCount ?? currentLog.failedCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="执行参数">{{ currentLog.executionParams || '-' }}</el-descriptions-item>
        <el-descriptions-item label="执行步骤">
          <div class="steps">
            <el-tag v-for="step in currentLog.executionSteps || []" :key="step" effect="plain">{{ step }}</el-tag>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" v-if="currentLog.errorMessages?.length || currentLog.errorMessage">
          <span class="error-text">{{ (currentLog.errorMessages || [currentLog.errorMessage]).filter(Boolean).join('；') }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from '../utils/request'
import { ElMessage } from 'element-plus'

export default {
  name: 'TaskLog',
  setup() {
    const logList = ref([])
    const total = ref(0)
    const pageSize = ref(10)
    const currentPage = ref(1)
    const dialogVisible = ref(false)
    const currentLog = ref(null)
    const loading = ref(false)
    const filters = ref({
      status: '',
      taskId: null
    })

    const fetchLogs = async () => {
      loading.value = true
      try {
        const response = await axios.get('/api/monitoring/task/logs', {
          params: {
            page: currentPage.value,
            size: pageSize.value,
            status: filters.value.status || undefined,
            taskId: filters.value.taskId || undefined
          }
        })
        const payload = response.data || response
        logList.value = payload.list || []
        total.value = payload.total || 0
      } catch (error) {
        console.error('获取日志列表失败:', error)
        ElMessage.error('获取日志列表失败')
      } finally {
        loading.value = false
      }
    }

    const getStatusType = (status) => {
      switch (status) {
        case 'success':
          return 'success'
        case 'failed':
          return 'danger'
        case 'running':
          return 'warning'
        default:
          return 'info'
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

    const handleView = async (row) => {
      try {
        const response = await axios.get('/api/monitoring/task/detail', {
          params: {
            taskId: row.taskId,
            logId: row.id
          }
        })
        const payload = response.data || response
        currentLog.value = {
          ...row,
          ...payload
        }
        dialogVisible.value = true
      } catch (error) {
        console.error('获取执行详情失败:', error)
        ElMessage.error('获取执行详情失败')
      }
    }

    const handlePageChange = (page) => {
      currentPage.value = page
      fetchLogs()
    }

    const handleFilterChange = () => {
      currentPage.value = 1
      fetchLogs()
    }

    onMounted(() => {
      fetchLogs()
    })

    return {
      logList,
      total,
      pageSize,
      currentPage,
      dialogVisible,
      currentLog,
      loading,
      filters,
      fetchLogs,
      getStatusType,
      getStatusText,
      handleView,
      handlePageChange,
      handleFilterChange
    }
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pagination {
  margin-top: 20px;
  text-align: center;
}

.steps {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.error-text {
  color: #f56c6c;
}
</style>
