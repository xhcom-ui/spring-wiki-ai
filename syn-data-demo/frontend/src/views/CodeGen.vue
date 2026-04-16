<template>
  <div class="codegen-container">
    <h1>代码生成</h1>
    
    <el-tabs v-model="activeTab">
      <el-tab-pane label="PostgreSQL监听器" name="pg">
        <el-form :model="pgConfig" label-width="100px">
          <el-form-item label="主机名">
            <el-input v-model="pgConfig.hostName" placeholder="请输入主机名"></el-input>
          </el-form-item>
          <el-form-item label="数据库名">
            <el-input v-model="pgConfig.database" placeholder="请输入数据库名"></el-input>
          </el-form-item>
          <el-form-item label="表名">
            <el-input v-model="pgConfig.table" placeholder="请输入表名"></el-input>
          </el-form-item>
          <el-form-item label="实体类名">
            <el-input v-model="pgConfig.entityName" placeholder="请输入实体类名（可选）"></el-input>
          </el-form-item>
          <el-form-item label="监听器类名">
            <el-input v-model="pgConfig.listenerName" placeholder="请输入监听器类名（可选）"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="generatePgListener">生成代码</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <el-tab-pane label="MySQL监听器" name="mysql">
        <el-form :model="mysqlConfig" label-width="100px">
          <el-form-item label="主机名">
            <el-input v-model="mysqlConfig.hostName" placeholder="请输入主机名"></el-input>
          </el-form-item>
          <el-form-item label="数据库名">
            <el-input v-model="mysqlConfig.database" placeholder="请输入数据库名"></el-input>
          </el-form-item>
          <el-form-item label="表名">
            <el-input v-model="mysqlConfig.table" placeholder="请输入表名"></el-input>
          </el-form-item>
          <el-form-item label="实体类名">
            <el-input v-model="mysqlConfig.entityName" placeholder="请输入实体类名（可选）"></el-input>
          </el-form-item>
          <el-form-item label="监听器类名">
            <el-input v-model="mysqlConfig.listenerName" placeholder="请输入监听器类名（可选）"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="generateMysqlListener">生成代码</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
    
    <div v-if="generatedCode" class="code-result">
      <h2>生成的代码</h2>
      <pre class="code-block">{{ generatedCode }}</pre>
    </div>
  </div>
</template>

<script>
import request from '../utils/request'

export default {
  name: 'CodeGen',
  data() {
    return {
      activeTab: 'pg',
      pgConfig: {
        hostName: '',
        database: '',
        table: '',
        entityName: '',
        listenerName: ''
      },
      mysqlConfig: {
        hostName: '',
        database: '',
        table: '',
        entityName: '',
        listenerName: ''
      },
      generatedCode: ''
    }
  },
  methods: {
    async generatePgListener() {
      try {
        const response = await request.post('/api/codegen/pg-listener', this.pgConfig)
        this.generatedCode = response.data
      } catch (error) {
        this.$message.error('生成代码失败')
        console.error(error)
      }
    },
    async generateMysqlListener() {
      try {
        const response = await request.post('/api/codegen/mysql-listener', this.mysqlConfig)
        this.generatedCode = response.data
      } catch (error) {
        this.$message.error('生成代码失败')
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

.code-result {
  margin-top: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 10px;
}

.code-block {
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  overflow-x: auto;
  font-family: 'Courier New', Courier, monospace;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
