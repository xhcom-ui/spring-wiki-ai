<template>
  <div class="areas">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>区域管理</span>
          <el-button type="primary" @click="openCreateDialog">
            <el-icon><Plus /></el-icon>
            <span>新增区域</span>
          </el-button>
        </div>
      </template>
      <div class="search-bar">
        <el-input
          v-model="searchParkingId"
          placeholder="请输入停车场ID"
          style="width: 200px; margin-right: 10px"
        />
        <el-button type="primary" @click="loadAreas">
          <el-icon><Search /></el-icon>
          <span>搜索</span>
        </el-button>
      </div>
      <el-table :data="areas" style="width: 100%" :loading="loading">
        <el-table-column prop="areaId" label="区域ID" width="180" />
        <el-table-column prop="parkingId" label="停车场ID" width="180" />
        <el-table-column prop="name" label="区域名称" />
        <el-table-column prop="totalSpots" label="总车位数" width="120" />
        <el-table-column prop="floor" label="楼层" width="100" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="openEditDialog(scope.row)">
              <el-icon><Edit /></el-icon>
              <span>编辑</span>
            </el-button>
            <el-button size="small" type="danger" @click="deleteArea(scope.row.id)">
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
        <el-form-item label="区域ID">
          <el-input v-model="form.areaId" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="停车场ID" required>
          <el-input v-model="form.parkingId" />
        </el-form-item>
        <el-form-item label="区域名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="总车位数" required>
          <el-input-number v-model="form.totalSpots" :min="0" />
        </el-form-item>
        <el-form-item label="楼层">
          <el-input-number v-model="form.floor" :min="-3" :max="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveArea">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Plus, Edit, Delete, Search } from '@element-plus/icons-vue'
import { areaApi } from '../api'

export default {
  name: 'Areas',
  components: {
    Plus,
    Edit,
    Delete,
    Search
  },
  data() {
    return {
      areas: [],
      loading: false,
      searchParkingId: '',
      dialogVisible: false,
      dialogTitle: '新增区域',
      isEdit: false,
      form: {
        id: null,
        areaId: '',
        parkingId: '',
        name: '',
        totalSpots: 0,
        floor: 1
      }
    }
  },
  mounted() {
    this.loadAreas()
  },
  methods: {
    async loadAreas() {
      this.loading = true
      try {
        let response
        if (this.searchParkingId) {
          response = await areaApi.getByParkingId(this.searchParkingId)
        } else {
          response = await areaApi.getAll()
        }
        this.areas = response.data
      } catch (error) {
        console.error('加载区域数据失败:', error)
        this.$message.error('加载区域数据失败')
      } finally {
        this.loading = false
      }
    },
    openCreateDialog() {
      this.isEdit = false
      this.dialogTitle = '新增区域'
      this.form = {
        id: null,
        areaId: '',
        parkingId: '',
        name: '',
        totalSpots: 0,
        floor: 1
      }
      this.dialogVisible = true
    },
    openEditDialog(row) {
      this.isEdit = true
      this.dialogTitle = '编辑区域'
      this.form = { ...row }
      this.dialogVisible = true
    },
    async saveArea() {
      try {
        if (this.isEdit) {
          await areaApi.update(this.form.id, this.form)
          this.$message.success('区域更新成功')
        } else {
          await areaApi.create(this.form)
          this.$message.success('区域创建成功')
        }
        this.dialogVisible = false
        this.loadAreas()
      } catch (error) {
        console.error('保存区域数据失败:', error)
        this.$message.error('保存区域数据失败')
      }
    },
    async deleteArea(id) {
      try {
        await this.$confirm('确定要删除这个区域吗？', '警告', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await areaApi.delete(id)
        this.$message.success('区域删除成功')
        this.loadAreas()
      } catch (error) {
        if (error.message !== 'cancel') {
          console.error('删除区域失败:', error)
          this.$message.error('删除区域失败')
        }
      }
    }
  }
}
</script>

<style scoped>
.areas {
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
