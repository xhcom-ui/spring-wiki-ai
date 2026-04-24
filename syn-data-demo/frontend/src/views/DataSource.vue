<template>
  <div class="datasource">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据源管理</span>
          <el-button v-if="canManage" type="primary" @click="handleAdd">新增数据源</el-button>
        </div>
      </template>

      <el-table :data="dataSourceList" style="width: 100%">
        <el-table-column prop="name" label="数据源名称" />
        <el-table-column prop="type" label="类型">
          <template #default="scope">
            <el-tag>{{ scope.row.type === 'mysql' ? 'MySQL' : 'PostgreSQL' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="host" label="主机地址" />
        <el-table-column prop="port" label="端口" />
        <el-table-column prop="databaseName" label="数据库" />
        <el-table-column prop="status" label="状态">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleTest(scope.row)">测试连接</el-button>
            <el-button v-if="canManage" type="success" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button v-if="canManage" type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增数据源' : '编辑数据源'"
      width="700px"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="数据源名称">
          <el-input v-model="form.name" placeholder="请输入数据源名称" />
        </el-form-item>
        <el-form-item label="数据源类型">
          <el-select v-model="form.type" placeholder="请选择数据源类型" style="width: 100%" @change="handleTypeChange">
            <el-option
              v-for="type in dataSourceTypes"
              :key="type.value"
              :label="type.label"
              :value="type.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="主机地址">
          <el-input v-model="form.host" placeholder="请输入主机地址" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
        <el-form-item label="数据库">
          <el-input v-model="form.databaseName" placeholder="请输入数据库名称" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" placeholder="请输入描述" />
        </el-form-item>
        
        <el-collapse>
          <el-collapse-item title="连接池配置">
            <el-form-item label="最大连接数">
              <el-input-number v-model="form.maxConnections" :min="1" :max="100" style="width: 100%" />
            </el-form-item>
            <el-form-item label="最小连接数">
              <el-input-number v-model="form.minConnections" :min="0" :max="100" style="width: 100%" />
            </el-form-item>
            <el-form-item label="连接超时(毫秒)">
              <el-input-number v-model="form.connectionTimeout" :min="1000" :max="60000" style="width: 100%" />
            </el-form-item>
          </el-collapse-item>
        </el-collapse>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 连接测试结果对话框 -->
    <el-dialog
      v-model="testDialogVisible"
      title="连接测试结果"
      width="500px"
    >
      <div v-if="testResult">
        <el-result
          :icon="testResult.success ? 'success' : 'error'"
          :title="testResult.success ? '连接测试成功' : '连接测试失败'"
          :sub-title="testResult.message"
        >
          <template #extra>
            <div v-if="testResult.success">
              <el-descriptions border :column="1">
                <el-descriptions-item label="数据库版本">{{ testResult.version }}</el-descriptions-item>
                <el-descriptions-item label="字符集">{{ testResult.charset }}</el-descriptions-item>
                <el-descriptions-item label="连接耗时">{{ testResult.connectTime }} ms</el-descriptions-item>
              </el-descriptions>
              <el-button type="primary" @click="handlePerformanceTest">性能测试</el-button>
            </div>
          </template>
        </el-result>
      </div>
    </el-dialog>

    <!-- 性能测试结果对话框 -->
    <el-dialog
      v-model="performanceDialogVisible"
      title="性能测试结果"
      width="500px"
    >
      <div v-if="performanceResult">
        <el-result
          :icon="performanceResult.success ? 'success' : 'error'"
          :title="performanceResult.success ? '性能测试成功' : '性能测试失败'"
          :sub-title="performanceResult.message"
        >
          <template #extra>
            <div v-if="performanceResult.success">
              <el-descriptions border :column="1">
                <el-descriptions-item label="测试次数">{{ performanceResult.testCount }}</el-descriptions-item>
                <el-descriptions-item label="平均连接耗时">{{ performanceResult.avgConnectTime }} ms</el-descriptions-item>
                <el-descriptions-item label="总耗时">{{ performanceResult.totalTime }} ms</el-descriptions-item>
              </el-descriptions>
            </div>
          </template>
        </el-result>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { computed, ref, onMounted } from 'vue'
import axios from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { isAdmin } from '../utils/auth'

export default {
  name: 'DataSource',
  setup() {
    const canManage = computed(() => isAdmin())
    const dataSourceList = ref([])
    const dataSourceTypes = ref([])
    const dialogVisible = ref(false)
    const testDialogVisible = ref(false)
    const performanceDialogVisible = ref(false)
    const dialogType = ref('add')
    const testResult = ref(null)
    const performanceResult = ref(null)
    const currentDataSource = ref(null)
    const form = ref({
      name: '',
      type: 'mysql',
      host: '',
      port: 3306,
      databaseName: '',
      username: '',
      password: '',
      description: '',
      maxConnections: 10,
      minConnections: 5,
      connectionTimeout: 30000
    })

    const fetchData = async () => {
      try {
        const response = await axios.get('/api/datasource')
        dataSourceList.value = response.data || response
      } catch (error) {
        console.error('获取数据源列表失败:', error)
        ElMessage.error('获取数据源列表失败')
      }
    }

    const fetchDataSourceTypes = async () => {
      try {
        const response = await axios.get('/api/datasource/types')
        dataSourceTypes.value = response.data || response
      } catch (error) {
        console.error('获取数据源类型失败:', error)
      }
    }

    const handleAdd = () => {
      dialogType.value = 'add'
      form.value = {
        name: '',
        type: 'mysql',
        host: '',
        port: 3306,
        databaseName: '',
        username: '',
        password: '',
        description: '',
        maxConnections: 10,
        minConnections: 5,
        connectionTimeout: 30000
      }
      dialogVisible.value = true
    }

    const handleEdit = (row) => {
      dialogType.value = 'edit'
      form.value = {
        ...row,
        databaseName: row.databaseName ?? row.database ?? ''
      }
      dialogVisible.value = true
    }

    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(`确认删除数据源“${row.name}”吗？`, '删除确认', {
          type: 'warning'
        })
        await axios.delete(`/api/datasource/${row.id}`)
        ElMessage.success('删除成功')
        fetchData()
      } catch (error) {
        console.error('删除数据源失败:', error)
        ElMessage.error('删除数据源失败')
      }
    }

    const handleSubmit = async () => {
      try {
        if (dialogType.value === 'add') {
          await axios.post('/api/datasource', form.value)
          ElMessage.success('新增数据源成功')
        } else {
          await axios.put('/api/datasource', form.value)
          ElMessage.success('更新数据源成功')
        }
        dialogVisible.value = false
        fetchData()
      } catch (error) {
        console.error('保存数据源失败:', error)
        ElMessage.error('保存数据源失败')
      }
    }

    const handleTest = async (row) => {
      try {
        currentDataSource.value = row
        ElMessage.info('正在测试连接...')
        const response = await axios.post(`/api/datasource/${row.id}/test`)
        testResult.value = response.data || response
        testDialogVisible.value = true
      } catch (error) {
        console.error('测试连接失败:', error)
        ElMessage.error('测试连接失败')
      }
    }

    const handlePerformanceTest = async () => {
      try {
        if (!currentDataSource.value) return
        ElMessage.info('正在进行性能测试...')
        const response = await axios.post(`/api/datasource/${currentDataSource.value.id}/performance`)
        performanceResult.value = response.data || response
        performanceDialogVisible.value = true
      } catch (error) {
        console.error('性能测试失败:', error)
        ElMessage.error('性能测试失败')
      }
    }

    const handleTypeChange = (type) => {
      // 根据数据源类型设置默认端口
      const typeInfo = dataSourceTypes.value.find(t => t.value === type)
      if (typeInfo) {
        form.value.port = typeInfo.defaultPort
      }
    }

    onMounted(() => {
      fetchData()
      fetchDataSourceTypes()
    })

    return {
      canManage,
      dataSourceList,
      dataSourceTypes,
      dialogVisible,
      testDialogVisible,
      performanceDialogVisible,
      dialogType,
      form,
      testResult,
      performanceResult,
      handleAdd,
      handleEdit,
      handleDelete,
      handleSubmit,
      handleTest,
      handlePerformanceTest,
      handleTypeChange
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
</style>
