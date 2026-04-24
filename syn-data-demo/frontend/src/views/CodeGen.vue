<template>
  <div class="codegen-container">
    <h1>Watcher 配置</h1>
    <p class="page-tip">当前已取消动态生成 Java watcher 文件，改为前端直接提交 watcher 配置。</p>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="PostgreSQL Watcher" name="pg">
        <el-form :model="pgConfig" label-width="110px">
          <el-form-item label="数据源ID">
            <el-input v-model.number="pgConfig.sourceId" placeholder="请输入数据源ID" />
          </el-form-item>
          <el-form-item label="主机名">
            <el-input v-model="pgConfig.hostName" placeholder="请输入主机名" />
          </el-form-item>
          <el-form-item label="数据库名">
            <el-input v-model="pgConfig.database" placeholder="请输入数据库名" />
          </el-form-item>
          <el-form-item label="表名">
            <el-input v-model="pgConfig.table" placeholder="请输入表名" />
          </el-form-item>
          <el-form-item label="目标索引">
            <el-input v-model="pgConfig.targetIndex" placeholder="请输入目标索引" />
          </el-form-item>
          <el-form-item label="增量字段">
            <el-input v-model="pgConfig.incrementalField" placeholder="如 update_time / id" />
          </el-form-item>
          <el-form-item label="事件类型">
            <el-checkbox-group v-model="pgConfig.eventTypes">
              <el-checkbox label="insert">insert</el-checkbox>
              <el-checkbox label="update">update</el-checkbox>
              <el-checkbox label="delete">delete</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="pgConfig.description" type="textarea" :rows="3" placeholder="请输入 watcher 描述" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="buildWatcher('pg')">生成前端配置</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="MySQL Watcher" name="mysql">
        <el-form :model="mysqlConfig" label-width="110px">
          <el-form-item label="数据源ID">
            <el-input v-model.number="mysqlConfig.sourceId" placeholder="请输入数据源ID" />
          </el-form-item>
          <el-form-item label="主机名">
            <el-input v-model="mysqlConfig.hostName" placeholder="请输入主机名" />
          </el-form-item>
          <el-form-item label="数据库名">
            <el-input v-model="mysqlConfig.database" placeholder="请输入数据库名" />
          </el-form-item>
          <el-form-item label="表名">
            <el-input v-model="mysqlConfig.table" placeholder="请输入表名" />
          </el-form-item>
          <el-form-item label="目标索引">
            <el-input v-model="mysqlConfig.targetIndex" placeholder="请输入目标索引" />
          </el-form-item>
          <el-form-item label="增量字段">
            <el-input v-model="mysqlConfig.incrementalField" placeholder="如 update_time / id" />
          </el-form-item>
          <el-form-item label="事件类型">
            <el-checkbox-group v-model="mysqlConfig.eventTypes">
              <el-checkbox label="insert">insert</el-checkbox>
              <el-checkbox label="update">update</el-checkbox>
              <el-checkbox label="delete">delete</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="mysqlConfig.description" type="textarea" :rows="3" placeholder="请输入 watcher 描述" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="buildWatcher('mysql')">生成前端配置</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <div v-if="generatedResult" class="result-card">
      <h2>前端提交配置</h2>
      <p><strong>保存接口：</strong>{{ generatedResult.saveEndpoint }}</p>
      <p><strong>任务绑定接口：</strong>{{ generatedResult.bindTaskEndpoint }}</p>
      <p><strong>启动接口：</strong>{{ generatedResult.startEndpoint }}</p>
      <p v-if="savedWatcherId"><strong>已保存 watcher ID：</strong>{{ savedWatcherId }}</p>
      <div class="result-actions">
        <el-button type="success" @click="saveWatcher">保存 watcher 配置</el-button>
      </div>
      <pre class="code-block">{{ formattedWatcher }}</pre>
    </div>
  </div>
</template>

<script>
import request from '../utils/request'
import { ElMessage } from 'element-plus'

export default {
  name: 'CodeGen',
  data() {
    return {
      activeTab: 'pg',
      pgConfig: this.createDefaultConfig(),
      mysqlConfig: this.createDefaultConfig(),
      generatedResult: null,
      savedWatcherId: null
    }
  },
  computed: {
    formattedWatcher() {
      return this.generatedResult ? JSON.stringify(this.generatedResult.watcher, null, 2) : ''
    }
  },
  methods: {
    createDefaultConfig() {
      return {
        sourceId: null,
        hostName: '',
        database: '',
        table: '',
        targetIndex: '',
        incrementalField: '',
        eventTypes: ['insert', 'update', 'delete'],
        description: ''
      }
    },
    async buildWatcher(type) {
      try {
        const payload = type === 'pg' ? this.pgConfig : this.mysqlConfig
        const endpoint = type === 'pg' ? '/api/codegen/pg-listener' : '/api/codegen/mysql-listener'
        const response = await request.post(endpoint, payload)
        this.generatedResult = response.data || response
        this.savedWatcherId = null
        ElMessage.success('已生成前端 watcher 配置')
      } catch (error) {
        ElMessage.error('生成 watcher 配置失败')
        console.error(error)
      }
    },
    async saveWatcher() {
      try {
        if (!this.generatedResult?.saveEndpoint || !this.generatedResult?.watcher) {
          ElMessage.warning('请先生成 watcher 配置')
          return
        }
        const response = await request.post(this.generatedResult.saveEndpoint, this.generatedResult.watcher)
        const saved = response.data || response
        this.savedWatcherId = saved.id || null
        ElMessage.success('watcher 配置已保存')
      } catch (error) {
        ElMessage.error('保存 watcher 配置失败')
        console.error(error)
      }
    }
  }
}
</script>

<style scoped>
.codegen-container {
  padding: 20px;
}

.page-tip {
  margin: 8px 0 20px;
  color: #606266;
}

.result-card {
  margin-top: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 16px;
  background: #fff;
}

.result-actions {
  margin: 12px 0;
}

.code-block {
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  overflow-x: auto;
  font-family: Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
