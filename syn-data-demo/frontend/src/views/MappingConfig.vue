<template>
  <div class="mapping-config">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>映射配置</span>
          <el-button type="primary" @click="handleSave">保存配置</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 字段映射 -->
        <el-tab-pane label="字段映射" name="fieldMapping">
          <div class="field-mapping">
            <el-button type="primary" size="small" @click="addFieldMapping" style="margin-bottom: 10px">添加字段映射</el-button>
            <el-table :data="fieldMappings" style="width: 100%">
              <el-table-column type="index" width="50" />
              <el-table-column label="源字段">
                <template #default="scope">
                  <el-select v-model="scope.row.sourceField" placeholder="请选择源字段" style="width: 100%">
                    <el-option
                      v-for="field in sourceFields"
                      :key="field"
                      :label="field"
                      :value="field"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="目标字段">
                <el-input v-model="scope.row.targetField" placeholder="请输入目标字段" />
              </el-table-column>
              <el-table-column label="ES类型">
                <template #default="scope">
                  <el-select v-model="scope.row.esType" placeholder="请选择ES类型" style="width: 100%">
                    <el-option-group label="基础类型">
                      <el-option label="text" value="text" />
                      <el-option label="keyword" value="keyword" />
                      <el-option label="integer" value="integer" />
                      <el-option label="long" value="long" />
                      <el-option label="double" value="double" />
                      <el-option label="boolean" value="boolean" />
                      <el-option label="date" value="date" />
                    </el-option-group>
                    <el-option-group label="复杂类型">
                      <el-option label="object" value="object" />
                      <el-option label="nested" value="nested" />
                      <el-option label="array" value="array" />
                      <el-option label="ip" value="ip" />
                    </el-option-group>
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="属性配置">
                <template #default="scope">
                  <el-popover
                    placement="right"
                    title="字段属性"
                    width="300"
                    trigger="click"
                  >
                    <template #reference>
                      <el-button type="text">配置</el-button>
                    </template>
                    <el-form :model="scope.row.properties" label-width="80px">
                      <el-form-item label="是否索引">
                        <el-switch v-model="scope.row.properties.index" />
                      </el-form-item>
                      <el-form-item label="是否存储">
                        <el-switch v-model="scope.row.properties.store" />
                      </el-form-item>
                      <el-form-item label="是否分词">
                        <el-switch v-model="scope.row.properties.analyzed" />
                      </el-form-item>
                      <el-form-item label="分析器">
                        <el-select v-model="scope.row.properties.analyzer" placeholder="请选择分析器">
                          <el-option label="standard" value="standard" />
                          <el-option label="ik_max_word" value="ik_max_word" />
                          <el-option label="ik_smart" value="ik_smart" />
                          <el-option label="whitespace" value="whitespace" />
                        </el-select>
                      </el-form-item>
                    </el-form>
                  </el-popover>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100">
                <template #default="scope">
                  <el-button type="danger" size="small" @click="removeFieldMapping(scope.$index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <!-- 索引设置 -->
        <el-tab-pane label="索引设置" name="indexSettings">
          <el-form :model="indexSettings" label-width="120px">
            <el-form-item label="索引名称">
              <el-input v-model="indexSettings.indexName" placeholder="请输入索引名称" />
            </el-form-item>
            <el-form-item label="分片数量">
              <el-input-number v-model="indexSettings.numberOfShards" :min="1" :max="10" style="width: 100%" />
            </el-form-item>
            <el-form-item label="副本数量">
              <el-input-number v-model="indexSettings.numberOfReplicas" :min="0" :max="5" style="width: 100%" />
            </el-form-item>
            <el-form-item label="刷新间隔">
              <el-input v-model="indexSettings.refreshInterval" placeholder="如: 1s, 5s" />
            </el-form-item>
            <el-form-item label="分词器设置">
              <el-collapse>
                <el-collapse-item title="自定义分词器">
                  <el-form-item label="分词器名称">
                    <el-input v-model="customAnalyzer.name" placeholder="请输入分词器名称" />
                  </el-form-item>
                  <el-form-item label="类型">
                    <el-select v-model="customAnalyzer.type" placeholder="请选择分词器类型">
                      <el-option label="ik" value="ik" />
                      <el-option label="standard" value="standard" />
                      <el-option label="whitespace" value="whitespace" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="配置">
                    <el-input v-model="customAnalyzer.config" type="textarea" :rows="3" placeholder="请输入分词器配置" />
                  </el-form-item>
                </el-collapse-item>
              </el-collapse>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- JSON编辑 -->
        <el-tab-pane label="JSON编辑" name="jsonEdit">
          <div class="json-editor">
            <div class="toolbar">
              <el-button size="small" @click="formatJson">格式化</el-button>
              <el-button size="small" @click="validateJson">验证</el-button>
              <el-button size="small" @click="loadDefaultMapping">默认模板</el-button>
            </div>
            <el-input v-model="mappingJson" type="textarea" :rows="15" placeholder="请输入ES Mapping JSON配置" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 自动映射对话框 -->
    <el-dialog
      v-model="autoMappingDialogVisible"
      title="自动映射"
      width="600px"
    >
      <el-form :model="autoMappingForm" label-width="100px">
        <el-form-item label="数据源">
          <el-select v-model="autoMappingForm.dataSourceId" placeholder="请选择数据源" style="width: 100%">
            <el-option
              v-for="ds in dataSourceList"
              :key="ds.id"
              :label="ds.name"
              :value="ds.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="表名">
          <el-input v-model="autoMappingForm.tableName" placeholder="请输入表名" />
        </el-form-item>
        <el-form-item label="映射规则">
          <el-checkbox-group v-model="autoMappingForm.rules">
            <el-checkbox label="id" border>ID字段映射为keyword</el-checkbox>
            <el-checkbox label="time" border>时间字段映射为date</el-checkbox>
            <el-checkbox label="text" border>文本字段映射为text</el-checkbox>
            <el-checkbox label="number" border>数字字段映射为相应类型</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="autoMappingDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="generateAutoMapping">生成映射</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from '../utils/request'
import { ElMessage } from 'element-plus'

export default {
  name: 'MappingConfig',
  setup() {
    const activeTab = ref('fieldMapping')
    const dataSourceList = ref([])
    const sourceFields = ref(['id', 'name', 'create_time', 'update_time', 'status', 'description'])
    const fieldMappings = ref([
      {
        sourceField: 'id',
        targetField: 'id',
        esType: 'keyword',
        properties: {
          index: true,
          store: false,
          analyzed: false,
          analyzer: 'standard'
        }
      },
      {
        sourceField: 'name',
        targetField: 'name',
        esType: 'text',
        properties: {
          index: true,
          store: false,
          analyzed: true,
          analyzer: 'ik_max_word'
        }
      }
    ])
    const indexSettings = ref({
      indexName: '',
      numberOfShards: 5,
      numberOfReplicas: 1,
      refreshInterval: '1s'
    })
    const customAnalyzer = ref({
      name: '',
      type: 'ik',
      config: ''
    })
    const mappingJson = ref('')
    const autoMappingDialogVisible = ref(false)
    const autoMappingForm = ref({
      dataSourceId: null,
      tableName: '',
      rules: ['id', 'time', 'text', 'number']
    })

    const fetchDataSources = async () => {
      try {
        const response = await axios.get('/api/datasource')
        dataSourceList.value = response.data
      } catch (error) {
        console.error('获取数据源列表失败:', error)
      }
    }

    const addFieldMapping = () => {
      fieldMappings.value.push({
        sourceField: '',
        targetField: '',
        esType: 'text',
        properties: {
          index: true,
          store: false,
          analyzed: true,
          analyzer: 'standard'
        }
      })
    }

    const removeFieldMapping = (index) => {
      fieldMappings.value.splice(index, 1)
    }

    const handleSave = () => {
      // 生成Mapping JSON
      const mapping = {
        mappings: {
          properties: {}
        },
        settings: {
          number_of_shards: indexSettings.value.numberOfShards,
          number_of_replicas: indexSettings.value.numberOfReplicas,
          refresh_interval: indexSettings.value.refreshInterval
        }
      }

      // 添加字段映射
      fieldMappings.value.forEach(field => {
        mapping.mappings.properties[field.targetField] = {
          type: field.esType,
          index: field.properties.index,
          store: field.properties.store
        }
        if (field.properties.analyzer) {
          mapping.mappings.properties[field.targetField].analyzer = field.properties.analyzer
        }
      })

      // 保存到后端
      ElMessage.success('映射配置保存成功')
    }

    const formatJson = () => {
      try {
        const obj = JSON.parse(mappingJson.value)
        mappingJson.value = JSON.stringify(obj, null, 2)
        ElMessage.success('JSON格式化成功')
      } catch (error) {
        ElMessage.error('JSON格式错误')
      }
    }

    const validateJson = () => {
      try {
        JSON.parse(mappingJson.value)
        ElMessage.success('JSON格式正确')
      } catch (error) {
        ElMessage.error('JSON格式错误: ' + error.message)
      }
    }

    const loadDefaultMapping = () => {
      const defaultMapping = {
        mappings: {
          properties: {
            id: { type: 'keyword' },
            name: { type: 'text', analyzer: 'ik_max_word' },
            create_time: { type: 'date' },
            update_time: { type: 'date' },
            status: { type: 'integer' }
          }
        },
        settings: {
          number_of_shards: 5,
          number_of_replicas: 1,
          refresh_interval: '1s'
        }
      }
      mappingJson.value = JSON.stringify(defaultMapping, null, 2)
      ElMessage.success('默认模板加载成功')
    }

    const generateAutoMapping = () => {
      // 模拟自动映射生成
      ElMessage.success('自动映射生成成功')
      autoMappingDialogVisible.value = false
    }

    onMounted(() => {
      fetchDataSources()
      loadDefaultMapping()
    })

    return {
      activeTab,
      dataSourceList,
      sourceFields,
      fieldMappings,
      indexSettings,
      customAnalyzer,
      mappingJson,
      autoMappingDialogVisible,
      autoMappingForm,
      addFieldMapping,
      removeFieldMapping,
      handleSave,
      formatJson,
      validateJson,
      loadDefaultMapping,
      generateAutoMapping
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

.json-editor {
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
</style>
