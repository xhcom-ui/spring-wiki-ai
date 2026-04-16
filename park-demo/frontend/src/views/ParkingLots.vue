<template>
  <div class="parking-lots">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>停车场管理</span>
          <el-button type="primary" @click="openCreateDialog">
            <el-icon><Plus /></el-icon>
            <span>新增停车场</span>
          </el-button>
        </div>
      </template>
      <el-table :data="parkingLots" style="width: 100%">
        <el-table-column prop="parkingId" label="停车场ID" width="180" />
        <el-table-column prop="name" label="停车场名称" />
        <el-table-column prop="totalSpots" label="总车位数" width="120" />
        <el-table-column prop="address" label="地址" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '开放' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="openEditDialog(scope.row)">
              <el-icon><Edit /></el-icon>
              <span>编辑</span>
            </el-button>
            <el-button size="small" type="danger" @click="deleteParkingLot(scope.row.id)">
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
        <el-form-item label="停车场ID">
          <el-input v-model="form.parkingId" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="停车场名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="总车位数" required>
          <el-input-number v-model="form.totalSpots" :min="0" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" active-value="1" inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveParkingLot">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { parkingLotApi } from '../api'

export default {
  name: 'ParkingLots',
  components: {
    Plus,
    Edit,
    Delete
  },
  data() {
    return {
      parkingLots: [],
      dialogVisible: false,
      dialogTitle: '新增停车场',
      isEdit: false,
      form: {
        id: null,
        parkingId: '',
        name: '',
        totalSpots: 0,
        address: '',
        status: 1
      }
    }
  },
  mounted() {
    this.loadParkingLots()
  },
  methods: {
    async loadParkingLots() {
      try {
        const response = await parkingLotApi.getAll()
        this.parkingLots = response.data
      } catch (error) {
        console.error('加载停车场数据失败:', error)
        this.$message.error('加载停车场数据失败')
      }
    },
    openCreateDialog() {
      this.isEdit = false
      this.dialogTitle = '新增停车场'
      this.form = {
        id: null,
        parkingId: '',
        name: '',
        totalSpots: 0,
        address: '',
        status: 1
      }
      this.dialogVisible = true
    },
    openEditDialog(row) {
      this.isEdit = true
      this.dialogTitle = '编辑停车场'
      this.form = { ...row }
      this.dialogVisible = true
    },
    async saveParkingLot() {
      try {
        if (this.isEdit) {
          await parkingLotApi.update(this.form.id, this.form)
          this.$message.success('停车场更新成功')
        } else {
          await parkingLotApi.create(this.form)
          this.$message.success('停车场创建成功')
        }
        this.dialogVisible = false
        this.loadParkingLots()
      } catch (error) {
        console.error('保存停车场数据失败:', error)
        this.$message.error('保存停车场数据失败')
      }
    },
    async deleteParkingLot(id) {
      try {
        await this.$confirm('确定要删除这个停车场吗？', '警告', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await parkingLotApi.delete(id)
        this.$message.success('停车场删除成功')
        this.loadParkingLots()
      } catch (error) {
        if (error.message !== 'cancel') {
          console.error('删除停车场失败:', error)
          this.$message.error('删除停车场失败')
        }
      }
    }
  }
}
</script>

<style scoped>
.parking-lots {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
