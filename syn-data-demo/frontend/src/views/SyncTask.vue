<template>
  <div class="sync-task">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>同步任务</span>
          <el-button type="primary" @click="handleAdd">新增任务</el-button>
        </div>
      </template>

      <el-table :data="taskList" style="width: 100%">
        <el-table-column prop="name" label="任务名称" />
        <el-table-column prop="syncMode" label="同步模式">
          <template #default="scope">
            <el-tag :type="scope.row.syncMode === 'full' ? 'primary' : 'success'">
              {{ scope.row.syncMode === 'full' ? '全量' : '增量' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetIndex" label="目标索引" />
        <el-table-column prop="scheduleType" label="调度类型">
          <template #default="scope">
            <el-tag>{{ scope.row.scheduleType === 'cron' ? '定时' : '手动' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastSyncTime" label="最后同步时间" />
        <el-table-column label="操作" width="350">
          <template #default="scope">
            <el-button type="success" size="small" @click="handleStart(scope.row)">启动</el-button>
            <el-button type="warning" size="small" @click="handleStop(scope.row)">停止</el-button>
            <el-button type="primary" size="small" @click="executeSync(scope.row)">执行</el-button>
            <el-button type="info" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增任务' : '编辑任务'"
      width="900px"
    >
      <el-form :model="form" label-width="120px">
        <!-- 任务基本信息 -->
        <el-collapse>
          <el-collapse-item title="任务基本信息">
            <el-form-item label="任务名称">
              <el-input v-model="form.name" placeholder="请输入任务名称" />
            </el-form-item>
            <el-form-item label="任务描述">
              <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入任务描述" />
            </el-form-item>
            <el-form-item label="所属项目">
              <el-select v-model="form.project" placeholder="请选择项目" style="width: 100%">
                <el-option label="默认项目" value="default" />
                <el-option label="项目A" value="projectA" />
                <el-option label="项目B" value="projectB" />
              </el-select>
            </el-form-item>
            <el-form-item label="负责人">
              <el-input v-model="form.owner" placeholder="请输入负责人" />
            </el-form-item>
            <el-form-item label="优先级">
              <el-select v-model="form.priority" placeholder="请选择优先级" style="width: 100%">
                <el-option label="低" value="low" />
                <el-option label="中" value="medium" />
                <el-option label="高" value="high" />
              </el-select>
            </el-form-item>
            <el-form-item label="标签">
              <el-tag v-for="tag in form.tags" :key="tag" closable @close="removeTag(tag)">
                {{ tag }}
              </el-tag>
              <el-input
                v-model="newTag"
                @keyup.enter="addTag"
                placeholder="输入标签后按回车添加"
                style="width: 150px; margin-left: 10px"
              />
            </el-form-item>
          </el-collapse-item>

          <!-- 执行配置 -->
          <el-collapse-item title="执行配置">
            <el-form-item label="数据源">
              <el-select v-model="form.sourceId" placeholder="请选择数据源" style="width: 100%" @change="handleDataSourceChange">
                <el-option
                  v-for="ds in dataSourceList"
                  :key="ds.id"
                  :label="ds.name"
                  :value="ds.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="目标ES集群">
              <el-select v-model="form.esCluster" placeholder="请选择ES集群" style="width: 100%">
                <el-option label="默认集群" value="default" />
              </el-select>
            </el-form-item>
            <el-form-item label="目标索引">
              <el-input v-model="form.targetIndex" placeholder="请输入目标ES索引名称" />
            </el-form-item>
            <el-form-item label="同步模式">
              <el-radio-group v-model="form.syncMode">
                <el-radio label="full">全量同步</el-radio>
                <el-radio label="incremental">增量同步</el-radio>
                <el-radio label="full+incremental">全量+增量</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="执行策略">
              <el-radio-group v-model="form.executionStrategy">
                <el-radio label="immediate">立即执行</el-radio>
                <el-radio label="scheduled">定时执行</el-radio>
                <el-radio label="triggered">触发执行</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-collapse-item>

          <!-- 增量同步配置 -->
          <el-collapse-item title="增量同步配置" v-if="form.syncMode !== 'full'">
            <el-form-item label="增量字段">
              <el-select v-model="form.incrementalField" placeholder="请选择增量字段" style="width: 100%">
                <el-option
                  v-for="field in tableFields"
                  :key="field"
                  :label="field"
                  :value="field"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="增量策略">
              <el-select v-model="form.incrementalStrategy" placeholder="请选择增量策略" style="width: 100%">
                <el-option label="基于时间戳" value="timestamp" />
                <el-option label="基于自增ID" value="autoIncrement" />
                <el-option label="基于时间窗口" value="timeWindow" />
                <el-option label="基于业务逻辑" value="businessLogic" />
              </el-select>
            </el-form-item>
            <el-form-item label="断点续传">
              <el-switch v-model="form.enableCheckpoint" />
            </el-form-item>
          </el-collapse-item>

          <!-- 调度配置 -->
          <el-collapse-item title="调度配置" v-if="form.executionStrategy === 'scheduled'">
            <el-form-item label="调度类型">
              <el-radio-group v-model="form.scheduleType">
                <el-radio label="manual">手动执行</el-radio>
                <el-radio label="cron">定时执行</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="CRON表达式" v-if="form.scheduleType === 'cron'">
              <el-input v-model="form.cronExpression" placeholder="请输入CRON表达式，如：0 0 2 * * ?" />
              <el-button type="text" @click="showCronHelp" style="margin-top: 5px">CRON帮助</el-button>
            </el-form-item>
          </el-collapse-item>

          <!-- 执行参数 -->
          <el-collapse-item title="执行参数">
            <el-form-item label="批次大小">
              <el-input-number v-model="form.batchSize" :min="1" :max="10000" style="width: 100%" />
            </el-form-item>
            <el-form-item label="并发线程数">
              <el-input-number v-model="form.concurrentThreads" :min="1" :max="10" style="width: 100%" />
            </el-form-item>
            <el-form-item label="连接超时(秒)">
              <el-input-number v-model="form.connectionTimeout" :min="1" :max="300" style="width: 100%" />
            </el-form-item>
            <el-form-item label="查询超时(秒)">
              <el-input-number v-model="form.queryTimeout" :min="1" :max="300" style="width: 100%" />
            </el-form-item>
            <el-form-item label="写入超时(秒)">
              <el-input-number v-model="form.writeTimeout" :min="1" :max="300" style="width: 100%" />
            </el-form-item>
          </el-collapse-item>

          <!-- 容错参数 -->
          <el-collapse-item title="容错参数">
            <el-form-item label="失败重试次数">
              <el-input-number v-model="form.retryCount" :min="0" :max="10" style="width: 100%" />
            </el-form-item>
            <el-form-item label="重试间隔(秒)">
              <el-input-number v-model="form.retryInterval" :min="1" :max="300" style="width: 100%" />
            </el-form-item>
            <el-form-item label="跳过错误记录">
              <el-switch v-model="form.skipErrorRecords" />
            </el-form-item>
            <el-form-item label="错误处理策略">
              <el-select v-model="form.errorHandlingStrategy" placeholder="请选择错误处理策略" style="width: 100%">
                <el-option label="继续执行" value="continue" />
                <el-option label="暂停执行" value="pause" />
                <el-option label="终止任务" value="terminate" />
              </el-select>
            </el-form-item>
          </el-collapse-item>

          <!-- SQL配置 -->
          <el-collapse-item title="SQL配置">
            <el-form-item label="SQL语句">
              <div class="sql-editor">
                <div class="toolbar">
                  <el-button size="small" @click="formatSql">格式化</el-button>
                  <el-button size="small" @click="testSql">测试SQL</el-button>
                  <el-button size="small" @click="showSqlTemplates">模板</el-button>
                  <el-button size="small" @click="viewExecutionPlan">执行计划</el-button>
                </div>
                <el-input v-model="form.sql_" type="textarea" :rows="8" placeholder="请输入SQL语句" />
                <div class="variables-help">
                  <el-button type="text" @click="showVariables">变量帮助</el-button>
                </div>
              </div>
            </el-form-item>
          </el-collapse-item>

          <!-- ES Mapping配置 -->
          <el-collapse-item title="ES Mapping配置">
            <el-form-item label="ES Mapping">
              <div class="mapping-editor">
                <div class="toolbar">
                  <el-button size="small" @click="formatMapping">格式化</el-button>
                  <el-button size="small" @click="testMapping">测试Mapping</el-button>
                  <el-button size="small" @click="showMappingTemplates">模板</el-button>
                </div>
                <el-input v-model="form.fieldMapping" type="textarea" :rows="6" placeholder="请输入ES Mapping配置（JSON格式）" />
              </div>
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

    <!-- 执行结果对话框 -->
    <el-dialog
      v-model="resultDialogVisible"
      title="同步执行结果"
      width="500px"
    >
      <div v-if="syncResult">
        <el-result
          :icon="syncResult.success ? 'success' : 'error'"
          :title="syncResult.success ? '同步成功' : '同步失败'"
          :sub-title="syncResult.message"
        >
          <template #extra>
            <div v-if="syncResult.success">
              <p>处理数据量: {{ syncResult.totalCount }} 条</p>
              <p>执行耗时: {{ syncResult.duration }} ms</p>
            </div>
          </template>
        </el-result>
      </div>
    </el-dialog>

    <!-- SQL测试结果对话框 -->
    <el-dialog
      v-model="sqlTestDialogVisible"
      title="SQL测试结果"
      width="700px"
    >
      <div v-if="sqlTestResult">
        <el-result
          :icon="sqlTestResult.success ? 'success' : 'error'"
          :title="sqlTestResult.success ? 'SQL测试成功' : 'SQL测试失败'"
          :sub-title="sqlTestResult.message"
        >
          <template #extra>
            <div v-if="sqlTestResult.success">
              <el-tabs>
                <el-tab-pane label="数据预览">
                  <el-table :data="sqlTestResult.previewData" style="width: 100%">
                    <el-table-column
                      v-for="(value, key) in sqlTestResult.previewData[0]"
                      :key="key"
                      :prop="key"
                      :label="key"
                    />
                  </el-table>
                </el-tab-pane>
                <el-tab-pane label="执行计划">
                  <pre>{{ sqlTestResult.executionPlan }}</pre>
                </el-tab-pane>
                <el-tab-pane label="执行信息">
                  <el-descriptions border :column="1">
                    <el-descriptions-item label="执行耗时">{{ sqlTestResult.executionTime }} ms</el-descriptions-item>
                    <el-descriptions-item label="预估数据量">{{ sqlTestResult.estimatedCount }} 条</el-descriptions-item>
                  </el-descriptions>
                </el-tab-pane>
              </el-tabs>
            </div>
          </template>
        </el-result>
      </div>
    </el-dialog>

    <!-- 变量帮助对话框 -->
    <el-dialog
      v-model="variablesDialogVisible"
      title="变量帮助"
      width="600px"
    >
      <el-tabs>
        <el-tab-pane label="系统变量">
          <el-table :data="systemVariables" style="width: 100%">
            <el-table-column prop="name" label="变量名" />
            <el-table-column prop="description" label="描述" />
            <el-table-column prop="example" label="示例值" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="自定义变量">
          <el-table :data="customVariables" style="width: 100%">
            <el-table-column prop="name" label="变量名" />
            <el-table-column prop="description" label="描述" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="上下文变量">
          <el-table :data="contextVariables" style="width: 100%">
            <el-table-column prop="name" label="变量名" />
            <el-table-column prop="description" label="描述" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- SQL模板对话框 -->
    <el-dialog
      v-model="sqlTemplatesDialogVisible"
      title="SQL模板"
      width="700px"
    >
      <el-table :data="sqlTemplates" style="width: 100%">
        <el-table-column prop="name" label="模板名称" />
        <el-table-column prop="description" label="描述" />
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button size="small" @click="applySqlTemplate(scope.row)">应用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from '../utils/request'
import { ElMessage } from 'element-plus'

export default {
  name: 'SyncTask',
  setup() {
    const taskList = ref([])
    const dataSourceList = ref([])
    const dialogVisible = ref(false)
    const resultDialogVisible = ref(false)
    const sqlTestDialogVisible = ref(false)
    const variablesDialogVisible = ref(false)
    const sqlTemplatesDialogVisible = ref(false)
    const dialogType = ref('add')
    const syncResult = ref(null)
    const sqlTestResult = ref(null)
    const tableFields = ref(['id', 'name', 'create_time', 'update_time', 'status'])
    const newTag = ref('')
    const form = ref({
      name: '',
      description: '',
      project: 'default',
      owner: '',
      priority: 'medium',
      tags: [],
      sourceId: null,
      esCluster: 'default',
      sql_: '',
      fieldMapping: '',
      targetIndex: '',
      syncMode: 'full',
      executionStrategy: 'immediate',
      incrementalField: '',
      incrementalStrategy: 'timestamp',
      enableCheckpoint: true,
      scheduleType: 'manual',
      cronExpression: '',
      batchSize: 1000,
      concurrentThreads: 1,
      connectionTimeout: 30,
      queryTimeout: 60,
      writeTimeout: 60,
      retryCount: 3,
      retryInterval: 5,
      skipErrorRecords: false,
      errorHandlingStrategy: 'continue',
      status: 1
    })

    // 变量定义
    const systemVariables = ref([
      { name: '${now}', description: '当前时间', example: '2024-04-09 12:00:00' },
      { name: '${yesterday}', description: '昨天', example: '2024-04-08' },
      { name: '${today}', description: '今天', example: '2024-04-09' },
      { name: '${start_time}', description: '任务开始时间', example: '2024-04-09 10:00:00' },
      { name: '${end_time}', description: '任务结束时间', example: '2024-04-09 10:01:00' },
      { name: '${last_sync_time}', description: '上次同步时间', example: '2024-04-08 10:00:00' },
      { name: '${batch_date}', description: '批次日期', example: '20240409' }
    ])

    const customVariables = ref([
      { name: '${user_id}', description: '用户可定义' },
      { name: '${business_date}', description: '业务日期' }
    ])

    const contextVariables = ref([
      { name: '${last_max_id}', description: '上次同步最大ID' },
      { name: '${last_update_time}', description: '上次更新时间' }
    ])

    // SQL模板
    const sqlTemplates = ref([
      { name: '查询所有数据', description: 'SELECT * FROM table', sql: 'SELECT * FROM {table_name}' },
      { name: '按时间查询', description: '按时间字段查询数据', sql: 'SELECT * FROM {table_name} WHERE update_time > \'{last_sync_time}\' ' },
      { name: '分页查询', description: '分页查询数据', sql: 'SELECT * FROM {table_name} LIMIT {limit} OFFSET {offset}' }
    ])

    const fetchTasks = async () => {
      try {
        const response = await axios.get('/api/task')
        taskList.value = response.data
      } catch (error) {
        console.error('获取任务列表失败:', error)
      }
    }

    const fetchDataSources = async () => {
      try {
        const response = await axios.get('/api/datasource')
        dataSourceList.value = response.data
      } catch (error) {
        console.error('获取数据源列表失败:', error)
      }
    }

    const handleDataSourceChange = async (sourceId) => {
      if (!sourceId) return
      // 这里可以获取数据源的表结构信息，用于字段自动补全
      tableFields.value = ['id', 'name', 'create_time', 'update_time', 'status', 'description']
    }

    const handleAdd = () => {
      dialogType.value = 'add'
      form.value = {
        name: '',
        description: '',
        project: 'default',
        owner: '',
        priority: 'medium',
        tags: [],
        sourceId: null,
        esCluster: 'default',
        sql_: '',
        fieldMapping: '',
        targetIndex: '',
        syncMode: 'full',
        executionStrategy: 'immediate',
        incrementalField: '',
        incrementalStrategy: 'timestamp',
        enableCheckpoint: true,
        scheduleType: 'manual',
        cronExpression: '',
        batchSize: 1000,
        concurrentThreads: 1,
        connectionTimeout: 30,
        queryTimeout: 60,
        writeTimeout: 60,
        retryCount: 3,
        retryInterval: 5,
        skipErrorRecords: false,
        errorHandlingStrategy: 'continue',
        status: 1
      }
      dialogVisible.value = true
    }

    const addTag = () => {
      if (newTag.value && !form.value.tags.includes(newTag.value)) {
        form.value.tags.push(newTag.value)
        newTag.value = ''
      }
    }

    const removeTag = (tag) => {
      form.value.tags = form.value.tags.filter(t => t !== tag)
    }

    const formatSql = () => {
      ElMessage.success('SQL格式化成功')
    }

    const testSql = async () => {
      try {
        if (!form.value.id) {
          ElMessage.warning('请先保存任务后再测试SQL')
          return
        }
        ElMessage.info('正在测试SQL...')
        const response = await axios.post(`/api/task/${form.value.id}/test-sql`)
        sqlTestResult.value = response.data
        sqlTestDialogVisible.value = true
      } catch (error) {
        console.error('SQL测试失败:', error)
        ElMessage.error('SQL测试失败:' + error.message)
      }
    }

    const viewExecutionPlan = async () => {
      try {
        if (!form.value.id) {
          ElMessage.warning('请先保存任务后再查看执行计划')
          return
        }
        ElMessage.info('正在获取执行计划...')
        const response = await axios.post(`/api/task/${form.value.id}/execution-plan`)
        sqlTestResult.value = response.data
        sqlTestDialogVisible.value = true
      } catch (error) {
        console.error('获取执行计划失败:', error)
        ElMessage.error('获取执行计划失败:' + error.message)
      }
    }

    const formatMapping = () => {
      ElMessage.success('Mapping格式化成功')
    }

    const testMapping = async () => {
      try {
        if (!form.value.id) {
          ElMessage.warning('请先保存任务后再测试Mapping')
          return
        }
        const response = await axios.post(`/api/task/${form.value.id}/test-mapping`)
        if (response.data.success) {
          ElMessage.success(response.data.message)
        } else {
          ElMessage.error(response.data.message)
        }
      } catch (error) {
        console.error('ES Mapping测试失败:', error)
        ElMessage.error('ES Mapping测试失败:' + error.message)
      }
    }

    const executeSync = async (row) => {
      try {
        ElMessage.info('开始执行同步任务...')
        const response = await axios.post(`/api/task/${row.id}/execute`)
        syncResult.value = response.data
        resultDialogVisible.value = true
        
        if (response.data.success) {
          ElMessage.success('同步执行成功！')
        } else {
          ElMessage.error('同步执行失败：' + response.data.message)
        }
        fetchTasks()
      } catch (error) {
        console.error('执行同步失败:', error)
        ElMessage.error('执行同步失败:' + error.message)
      }
    }

    const handleEdit = (row) => {
      dialogType.value = 'edit'
      form.value = { ...row }
      dialogVisible.value = true
    }

    const handleDelete = async (row) => {
      try {
        await axios.delete(`/api/task/${row.id}`)
        ElMessage.success('删除成功')
        fetchTasks()
      } catch (error) {
        console.error('删除任务失败:', error)
        ElMessage.error('删除任务失败')
      }
    }

    const handleStart = async (row) => {
      try {
        await axios.post(`/api/task/${row.id}/start`)
        ElMessage.success('任务启动成功')
        fetchTasks()
      } catch (error) {
        console.error('启动任务失败:', error)
        ElMessage.error('启动任务失败')
      }
    }

    const handleStop = async (row) => {
      try {
        await axios.post(`/api/task/${row.id}/stop`)
        ElMessage.success('任务停止成功')
        fetchTasks()
      } catch (error) {
        console.error('停止任务失败:', error)
        ElMessage.error('停止任务失败')
      }
    }

    const handleSubmit = async () => {
      try {
        if (dialogType.value === 'add') {
          await axios.post('/api/task', form.value)
          ElMessage.success('新增任务成功')
        } else {
          await axios.put('/api/task', form.value)
          ElMessage.success('更新任务成功')
        }
        dialogVisible.value = false
        fetchTasks()
      } catch (error) {
        console.error('保存任务失败:', error)
        ElMessage.error('保存任务失败')
      }
    }

    const showVariables = () => {
      variablesDialogVisible.value = true
    }

    const showSqlTemplates = () => {
      sqlTemplatesDialogVisible.value = true
    }

    const applySqlTemplate = (template) => {
      form.value.sql_ = template.sql
      sqlTemplatesDialogVisible.value = false
      ElMessage.success('模板应用成功')
    }

    const showCronHelp = () => {
      ElMessage.info('CRON表达式格式: 秒 分 时 日 月 周 年(可选)')
    }

    const showMappingTemplates = () => {
      ElMessage.info('Mapping模板功能开发中')
    }

    onMounted(() => {
      fetchTasks()
      fetchDataSources()
    })

    return {
      taskList,
      dataSourceList,
      dialogVisible,
      resultDialogVisible,
      sqlTestDialogVisible,
      variablesDialogVisible,
      sqlTemplatesDialogVisible,
      dialogType,
      form,
      syncResult,
      sqlTestResult,
      tableFields,
      newTag,
      systemVariables,
      customVariables,
      contextVariables,
      sqlTemplates,
      handleAdd,
      handleEdit,
      handleDelete,
      handleStart,
      handleStop,
      handleSubmit,
      handleDataSourceChange,
      addTag,
      removeTag,
      formatSql,
      testSql,
      viewExecutionPlan,
      formatMapping,
      testMapping,
      executeSync,
      showVariables,
      showSqlTemplates,
      applySqlTemplate,
      showCronHelp,
      showMappingTemplates
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

.sql-editor,
.mapping-editor {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.toolbar {
  background-color: #f5f7fa;
  padding: 8px 12px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  gap: 8px;
}

.variables-help {
  padding: 8px 12px;
  background-color: #f5f7fa;
  border-top: 1px solid #e4e7ed;
  text-align: right;
}
</style>
