<template>
  <div class="parking-spots">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>车位管理</span>
          <el-button type="primary" @click="openCreateDialog">
            <el-icon><Plus /></el-icon>
            <span>新增车位</span>
          </el-button>
        </div>
      </template>
      <div class="search-bar">
        <el-input
          v-model="searchParkingId"
          placeholder="请输入停车场ID"
          style="width: 200px; margin-right: 10px"
        />
        <el-button type="primary" @click="loadParkingSpots">
          <el-icon><Search /></el-icon>
          <span>搜索</span>
        </el-button>
      </div>
      <el-table :data="parkingSpots" style="width: 100%" :loading="loading">
        <el-table-column prop="spotId" label="车位ID" width="180" />
        <el-table-column prop="parkingId" label="停车场ID" width="180" />
        <el-table-column prop="areaId" label="区域ID" width="180" />
        <el-table-column prop="floor" label="楼层" width="100" />
        <el-table-column prop="spotNumber" label="车位编号" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getTagType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sensorId" label="传感器ID" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="openEditDialog(scope.row)">
              <el-icon><Edit /></el-icon>
              <span>编辑</span>
            </el-button>
            <el-button size="small" type="danger" @click="deleteParkingSpot(scope.row.id)">
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
        <el-form-item label="车位ID">
          <el-input v-model="form.spotId" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="停车场ID" required>
          <el-input v-model="form.parkingId" />
        </el-form-item>
        <el-form-item label="区域ID">
          <el-input v-model="form.areaId" />
        </el-form-item>
        <el-form-item label="楼层">
          <el-input-number v-model="form.floor" :min="-3" :max="10" />
        </el-form-item>
        <el-form-item label="车位编号" required>
          <el-input v-model="form.spotNumber" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" placeholder="请选择状态">
            <el-option label="空闲" value="0" />
            <el-option label="占用" value="1" />
            <el-option label="故障" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="传感器ID">
          <el-input v-model="form.sensorId" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveParkingSpot">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Plus, Edit, Delete, Search } from '@element-plus/icons-vue'
import { parkingSpotApi } from '../api'

export default {
  name: 'ParkingSpots',
  components: {
    Plus,
    Edit,
    Delete,
    Search
  },
  data() {
    return {
      parkingSpots: [],
      loading: false,
      searchParkingId: '',
      dialogVisible: false,
      dialogTitle: '新增车位',
      isEdit: false,
      form: {
        id: null,
        spotId: '',
        parkingId: '',
        areaId: '',
        floor: 1,
        spotNumber: '',
        status: 0,
        sensorId: ''
      }
    }
  },
  mounted() {
    this.loadParkingSpots()
  },
  methods: {
    async loadParkingSpots() {
      this.loading = true
      try {
        let response
        if (this.searchParkingId) {
          response = await parkingSpotApi.getByParkingId(this.searchParkingId)
        } else {
          response = await parkingSpotApi.getAll()
        }
        this.parkingSpots = response.data
      } catch (error) {
        console.error('加载车位数据失败:', error)
        this.$message.error('加载车位数据失败')
      } finally {
        this.loading = false
      }
    },
    getTagType(status) {
      switch (status) {
        case 0: return 'success'
        case 1: return 'warning'
        case 2: return 'danger'
        default: return ''
      }
    },
    getStatusText(status) {
      switch (status) {
        case 0: return '空闲'
        case 1: return '占用'
        case 2: return '故障'
        default: return '未知'
      }
    },
    openCreateDialog() {
      this.isEdit = false
      this.dialogTitle = '新增车位'
      this.form = {
        id: null,
        spotId: '',
        parkingId: '',
        areaId: '',
        floor: 1,
        spotNumber: '',
        status: 0,
        sensorId: ''
      }
      this.dialogVisible = true
    },
    openEditDialog(row) {
      this.isEdit = true
      this.dialogTitle = '编辑车位'
      this.form = { ...row }
      this.dialogVisible = true
    },
    async saveParkingSpot() {
      try {
        if (this.isEdit) {
          await parkingSpotApi.update(this.form.id, this.form)
          this.$message.success('车位更新成功')
        } else {
          await parkingSpotApi.create(this.form)
          this.$message.success('车位创建成功')
        }
        this.dialogVisible = false
        this.loadParkingSpots()
      } catch (error) {
        console.error('保存车位数据失败:', error)
        this.$message.error('保存车位数据失败')
      }
    },
    async deleteParkingSpot(id) {
      try {
        await this.$confirm('确定要删除这个车位吗？', '警告', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await parkingSpotApi.delete(id)
        this.$message.success('车位删除成功')
        this.loadParkingSpots()
      } catch (error) {
        if (error.message !== 'cancel') {
          console.error('删除车位失败:', error)
          this.$message.error('删除车位失败')
        }
      }
    }
  }
}
</script>

<style scoped>
.parking-spots {
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
