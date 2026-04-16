<template>
  <div class="sensors">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>传感器管理</span>
          <el-button type="primary" @click="openCreateDialog">
            <el-icon><Plus /></el-icon>
            <span>新增传感器</span>
          </el-button>
        </div>
      </template>
      <div class="search-bar">
        <el-input
          v-model="searchParkingId"
          placeholder="请输入停车场ID"
          style="width: 200px; margin-right: 10px"
        />
        <el-button type="primary" @click="loadSensors">
          <el-icon><Search /></el-icon>
          <span>搜索</span>
        </el-button>
      </div>
      <el-table :data="sensors" style="width: 100%" :loading="loading">
        <el-table-column prop="sensorId" label="传感器ID" width="180" />
        <el-table-column prop="parkingId" label="停车场ID" width="180" />
        <el-table-column prop="spotId" label="车位ID" width="180" />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="scope">
            <el-tag :type="getTagType(scope.row.type)">
              {{ getTypeText(scope.row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="signalStrength" label="信号强度" width="120" />
        <el-table-column prop="batteryLevel" label="电池电量" width="120" />
        <el-table-column prop="lastReportedAt" label="最后上报时间" width="200" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="openEditDialog(scope.row)">
              <el-icon><Edit /></el-icon>
              <span>编辑</span>
            </el-button>
            <el-button size="small" type="danger" @click="deleteSensor(scope.row.id)">
              <el-icon><Delete /></el-icon>
              <span>删除</span>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="传感器ID">
          <el-input v-model="form.sensorId" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="停车场ID" required>
          <el-input v-model="form.parkingId" />
        </el-form-item>
        <el-form-item label="车位ID">
          <el-input v-model="form.spotId" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" placeholder="请选择类型">
            <el-option label="地磁" value="0" />
            <el-option label="视频" value="1" />
            <el-option label="地锁" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" active-value="1" inactive-value="0" />
        </el-form-item>
        <el-form-item label="信号强度">
          <el-input-number v-model="form.signalStrength" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="电池电量">
          <el-input-number v-model="form.batteryLevel" :min="0" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveSensor">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Plus, Edit, Delete, Search } from '@element-plus/icons-vue'
import { sensorApi } from '../api'

export default {
  name: 'Sensors',
  components: {
    Plus,
    Edit,
    Delete,
    Search
  },
  data() {
    return {
      sensors: [],
      loading: false,
      searchParkingId: '',
      dialogVisible: false,
      dialogTitle: '新增传感器',
      isEdit: false,
      form: {
        id: null,
        sensorId: '',
        parkingId: '',
        spotId: '',
        type: 0,
        status: 1,
        signalStrength: 100,
        batteryLevel: 100,
        lastReportedAt: null
      }
    }
  },
  mounted() {
    this.loadSensors()
  },
  methods: {
    async loadSensors() {
      this.loading = true
      try {
        let response
        if (this.searchParkingId) {
          response = await sensorApi.getByParkingId(this.searchParkingId)
        } else {
          response = await sensorApi.getAll()
        }
        this.sensors = response.data
      } catch (error) {
        console.error('加载传感器数据失败:', error)
        this.$message.error('加载传感器数据失败')
      } finally {
        this.loading = false
      }
    },
    getTagType(type) {
      switch (type) {
        case 0: return 'info'
        case 1: return 'warning'
        case 2: return 'success'
        default: return ''
      }
    },
    getTypeText(type) {
      switch (type) {
        case 0: return '地磁'
        case 1: return '视频'
        case 2: return '地锁'
        default: return '未知'
      }
    },
    openCreateDialog() {
      this.isEdit = false
      this.dialogTitle = '新增传感器'
      this.form = {
        id: null,
        sensorId: '',
        parkingId: '',
        spotId: '',
        type: 0,
        status: 1,
        signalStrength: 100,
        batteryLevel: 100,
        lastReportedAt: null
      }
      this.dialogVisible = true
    },
    openEditDialog(row) {
      this.isEdit = true
      this.dialogTitle = '编辑传感器'
      this.form = { ...row }
      this.dialogVisible = true
    },
    async saveSensor() {
      try {
        if (this.isEdit) {
          await sensorApi.update(this.form.id, this.form)
          this.$message.success('传感器更新成功')
        } else {
          await sensorApi.create(this.form)
          this.$message.success('传感器创建成功')
        }
        this.dialogVisible = false
        this.loadSensors()
      } catch (error) {
        console.error('保存传感器数据失败:', error)
        this.$message.error('保存传感器数据失败')
      }
    },
    async deleteSensor(id) {
      try {
        await this.$confirm('确定要删除这个传感器吗？', '警告', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await sensorApi.delete(id)
        this.$message.success('传感器删除成功')
        this.loadSensors()
      } catch (error) {
        if (error.message !== 'cancel') {
          console.error('删除传感器失败:', error)
          this.$message.error('删除传感器失败')
        }
      }
    }
  }
}
</script>

<style scoped>
.sensors {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.search-bar {
  margin-bottom: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
