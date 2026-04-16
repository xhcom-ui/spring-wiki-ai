<template>
  <div class="task-log">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>执行日志</span>
          <el-button type="primary" @click="fetchLogs">刷新</el-button>
        </div>
      </template>

      <el-table :data="logList" style="width: 100%">
        <el-table-column prop="taskId" label="任务ID" width="80" />
        <el-table-column prop="status" label="执行状态">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" />
        <el-table-column prop="endTime" label="结束时间" />
        <el-table-column prop="duration" label="耗时(ms)" />
        <el-table-column prop="sourceCount" label="源端数据量" />
        <el-table-column prop="targetCount" label="目标数据量" />
        <el-table-column prop="failedCount" label="失败数量" />
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleView(scope.row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        @current-change="handlePageChange"
      />
    </el-card>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="dialogVisible" title="执行详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="任务ID">{{ currentLog.taskId }}</el-descriptions-item>
        <el-descriptions-item label="执行状态">
          <el-tag :type="getStatusType(currentLog.status)">
            {{ getStatusText(currentLog.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ currentLog.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ currentLog.endTime }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ currentLog.duration }}ms</el-descriptions-item>
        <el-descriptions-item label="源端数据量">{{ currentLog.sourceCount }}</el-descriptions-item>
        <el-descriptions-item label="目标数据量">{{ currentLog.targetCount }}</el-descriptions-item>
        <el-descriptions-item label="失败数量">{{ currentLog.failedCount }}</el-descriptions-item>
        <el-descriptions-item label="错误信息" v-if="currentLog.errorMessage">
          <span style="color: #F56C6C;">{{ currentLog.errorMessage }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from '../utils/request'

export default {
  name: 'TaskLog',
  setup() {
    const logList = ref([])
    const total = ref(0)
    const pageSize = ref(10)
    const currentPage = ref(1)
    const dialogVisible = ref(false)
    const currentLog = ref({})

    const fetchLogs = async () => {
      try {
        // 这里应该调用后端API获取日志列表
        // 简化实现，使用模拟数据
        logList.value = [
          {
            id: 1,
            taskId: 1,
            status: 'success',
            startTime: '2024-01-15 10:30:00',
            endTime: '2024-01-15 10:30:15',
            duration: 15000,
            sourceCount: 10000,
            targetCount: 10000,
            failedCount: 0,
            errorMessage: ''
          },
          {
            id: 2,
            taskId: 2,
            status: 'success',
            startTime: '2024-01-15 09:15:00',
            endTime: '2024-01-15 09:15:30',
            duration: 30000,
            sourceCount: 5000,
            targetCount: 5000,
            failedCount: 0,
            errorMessage: ''
          },
          {
            id: 3,
            taskId: 3,
            status: 'failed',
            startTime: '2024-01-15 08:00:00',
            endTime: '2024-01-15 08:00:05',
            duration: 5000,
            sourceCount: 0,
            targetCount: 0,
            failedCount: 0,
            errorMessage: '数据库连接超时'
          }
        ]
        total.value = 3
      } catch (error) {
        console.error('获取日志列表失败:', error)
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

    const handleView = (row) => {
      currentLog.value = row
      dialogVisible.value = true
    }

    const handlePageChange = (page) => {
      currentPage.value = page
      fetchLogs()
    }

    onMounted(() => {
      fetchLogs()
    })

    return {
      logList,
      total,
      pageSize,
      dialogVisible,
      currentLog,
      fetchLogs,
      getStatusType,
      getStatusText,
      handleView,
      handlePageChange
    }
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  text-align: center;
}
</style>
