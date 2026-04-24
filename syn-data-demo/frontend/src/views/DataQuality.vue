<template>
  <div class="data-quality">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据质量管理</span>
          <el-button type="primary" @click="handleValidate">执行数据校验</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 数据校验 -->
        <el-tab-pane label="数据校验" name="validation">
          <el-form :model="validationForm" label-width="120px">
            <el-form-item label="数据源">
              <el-select v-model="validationForm.dataSourceId" placeholder="请选择数据源" style="width: 100%">
                <el-option
                  v-for="ds in dataSourceList"
                  :key="ds.id"
                  :label="ds.name"
                  :value="ds.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="目标索引">
              <el-input v-model="validationForm.targetIndex" placeholder="请输入目标ES索引名称" />
            </el-form-item>
            <el-form-item label="校验类型">
              <el-checkbox-group v-model="validationForm.validationTypes">
                <el-checkbox label="count" border>数量校验</el-checkbox>
                <el-checkbox label="quality" border>质量校验</el-checkbox>
                <el-checkbox label="format" border>格式校验</el-checkbox>
                <el-checkbox label="business" border>业务规则校验</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="校验范围">
              <el-radio-group v-model="validationForm.rangeType">
                <el-radio label="full">全量校验</el-radio>
                <el-radio label="sample">抽样校验</el-radio>
                <el-radio label="range">范围校验</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="抽样比例" v-if="validationForm.rangeType === 'sample'">
              <el-input-number v-model="validationForm.sampleRatio" :min="1" :max="100" style="width: 100%" />
            </el-form-item>
            <el-form-item label="时间范围" v-if="validationForm.rangeType === 'range'">
              <el-date-picker
                v-model="validationForm.timeRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="executeValidation">开始校验</el-button>
              <el-button @click="resetValidationForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 问题管理 -->
        <el-tab-pane label="问题管理" name="issues">
          <div class="issues-filter">
            <el-select v-model="issueFilter.status" placeholder="状态" style="width: 150px; margin-right: 10px">
              <el-option label="全部" value="" />
              <el-option label="待处理" value="pending" />
              <el-option label="处理中" value="processing" />
              <el-option label="已修复" value="fixed" />
              <el-option label="已忽略" value="ignored" />
            </el-select>
            <el-select v-model="issueFilter.type" placeholder="问题类型" style="width: 150px; margin-right: 10px">
              <el-option label="全部" value="" />
              <el-option label="数量差异" value="count" />
              <el-option label="质量问题" value="quality" />
              <el-option label="格式错误" value="format" />
              <el-option label="业务规则" value="business" />
            </el-select>
            <el-input v-model="issueFilter.keyword" placeholder="搜索关键词" style="width: 200px; margin-right: 10px" />
            <el-button type="primary" @click="searchIssues">搜索</el-button>
          </div>

          <el-table :data="issues" style="width: 100%">
            <el-table-column prop="id" label="问题ID" width="120" />
            <el-table-column prop="type" label="问题类型">
              <template #default="scope">
                <el-tag :type="getIssueTypeTag(scope.row.type)">
                  {{ getIssueTypeLabel(scope.row.type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="问题描述" />
            <el-table-column prop="status" label="状态">
              <template #default="scope">
                <el-tag :type="getIssueStatusTag(scope.row.status)">
                  {{ getIssueStatusLabel(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column label="操作" width="200">
              <template #default="scope">
                <el-button size="small" @click="viewIssueDetails(scope.row)">查看</el-button>
                <el-button type="primary" size="small" @click="fixIssue(scope.row)">修复</el-button>
                <el-button type="danger" size="small" @click="ignoreIssue(scope.row)">忽略</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 质量统计 -->
        <el-tab-pane label="质量统计" name="statistics">
          <el-form :model="statisticsForm" inline>
            <el-form-item label="时间范围">
              <el-select v-model="statisticsForm.timeRange" style="width: 150px">
                <el-option label="最近7天" value="7d" />
                <el-option label="最近30天" value="30d" />
                <el-option label="最近90天" value="90d" />
                <el-option label="自定义" value="custom" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="statisticsForm.timeRange === 'custom'">
              <el-date-picker
                v-model="statisticsForm.customTimeRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                style="width: 300px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="generateStatistics">生成统计</el-button>
            </el-form-item>
          </el-form>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-card>
                <template #header>
                  <span>问题类型分布</span>
                </template>
                <div ref="typeChart" class="chart"></div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card>
                <template #header>
                  <span>问题状态分布</span>
                </template>
                <div ref="statusChart" class="chart"></div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card>
                <template #header>
                  <span>每日问题数量</span>
                </template>
                <div ref="trendChart" class="chart"></div>
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 校验结果对话框 -->
    <el-dialog
      v-model="validationResultVisible"
      title="校验结果"
      width="800px"
    >
      <div v-if="validationResult">
        <el-result
          :icon="validationResult.success ? 'success' : 'error'"
          :title="validationResult.success ? '校验成功' : '校验失败'"
          :sub-title="validationResult.message"
        >
          <template #extra>
            <div v-if="validationResult.success">
              <el-descriptions border :column="1">
                <el-descriptions-item label="源端数据量">{{ validationResult.sourceCount }} 条</el-descriptions-item>
                <el-descriptions-item label="目标端数据量">{{ validationResult.targetCount }} 条</el-descriptions-item>
                <el-descriptions-item label="差异数量">{{ validationResult.diffCount }} 条</el-descriptions-item>
                <el-descriptions-item label="执行耗时">{{ validationResult.duration }} ms</el-descriptions-item>
                <el-descriptions-item label="校验时间">{{ validationResult.validationTime }}</el-descriptions-item>
              </el-descriptions>
            </div>
            <div v-else>
              <el-descriptions border :column="1">
                <el-descriptions-item label="错误信息">{{ validationResult.message }}</el-descriptions-item>
                <el-descriptions-item label="执行耗时">{{ validationResult.duration }} ms</el-descriptions-item>
              </el-descriptions>
            </div>
          </template>
        </el-result>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="validationResultVisible = false">关闭</el-button>
          <el-button type="primary" v-if="!validationResult.success" @click="exportValidationResult">导出结果</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 问题详情对话框 -->
    <el-dialog
      v-model="issueDetailsVisible"
      title="问题详情"
      width="600px"
    >
      <div v-if="currentIssue">
        <el-descriptions border :column="2">
          <el-descriptions-item label="问题ID">{{ currentIssue.id }}</el-descriptions-item>
          <el-descriptions-item label="问题类型">{{ getIssueTypeLabel(currentIssue.type) }}</el-descriptions-item>
          <el-descriptions-item label="问题描述">{{ currentIssue.description }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ getIssueStatusLabel(currentIssue.status) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentIssue.createTime }}</el-descriptions-item>
          <el-descriptions-item label="关联任务">{{ currentIssue.taskName }}</el-descriptions-item>
        </el-descriptions>
        <div class="issue-details" v-if="currentIssue.details">
          <h4>详细信息</h4>
          <pre>{{ currentIssue.details }}</pre>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="issueDetailsVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 修复问题对话框 -->
    <el-dialog
      v-model="fixIssueVisible"
      title="修复问题"
      width="500px"
    >
      <el-form :model="fixForm" label-width="100px">
        <el-form-item label="问题ID">{{ currentIssue?.id }}</el-form-item>
        <el-form-item label="修复策略">
          <el-select v-model="fixForm.strategy" placeholder="请选择修复策略" style="width: 100%">
            <el-option label="增量修复" value="incremental" />
            <el-option label="全量修复" value="full" />
            <el-option label="手动修复" value="manual" />
          </el-select>
        </el-form-item>
        <el-form-item label="修复范围">
          <el-radio-group v-model="fixForm.range">
            <el-radio label="仅当前问题">仅当前问题</el-radio>
            <el-radio label="同类问题">同类问题</el-radio>
            <el-radio label="所有问题">所有问题</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="fixIssueVisible = false">取消</el-button>
          <el-button type="primary" @click="executeFix">执行修复</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { nextTick, ref, onMounted, onUnmounted } from 'vue'
import axios from '../utils/request'
import { ElMessage } from 'element-plus'
import { echarts } from '../utils/echarts'

export default {
  name: 'DataQuality',
  setup() {
    const activeTab = ref('validation')
    const dataSourceList = ref([])
    const issues = ref([])
    const validationResult = ref(null)
    const validationResultVisible = ref(false)
    const issueDetailsVisible = ref(false)
    const fixIssueVisible = ref(false)
    const currentIssue = ref(null)
    
    // 图表引用
    const typeChart = ref(null)
    const statusChart = ref(null)
    const trendChart = ref(null)
    let typeChartInstance = null
    let statusChartInstance = null
    let trendChartInstance = null
    let resizeHandler = null
    
    // 表单数据
    const validationForm = ref({
      dataSourceId: null,
      targetIndex: '',
      validationTypes: ['count', 'quality'],
      rangeType: 'full',
      sampleRatio: 10,
      timeRange: []
    })
    
    const issueFilter = ref({
      status: '',
      type: '',
      keyword: ''
    })
    
    const statisticsForm = ref({
      timeRange: '7d',
      customTimeRange: []
    })
    
    const fixForm = ref({
      strategy: 'incremental',
      range: '仅当前问题'
    })
    
    // 获取数据源列表
    const fetchDataSources = async () => {
      try {
        const response = await axios.get('/api/datasource')
        dataSourceList.value = response.data || response
      } catch (error) {
        console.error('获取数据源列表失败:', error)
      }
    }
    
    // 执行数据校验
    const executeValidation = async () => {
      try {
        ElMessage.info('正在执行数据校验...')
        const response = await axios.post('/api/data-quality/validate/full', {
          sourceCount: 1000,
          targetCount: 998,
          sourceData: { id: 1, name: 'test' },
          targetData: { id: 1, name: 'test' }
        })
        validationResult.value = response.data || response
        validationResultVisible.value = true
        
        if (validationResult.value.success) {
          ElMessage.success('数据校验成功')
        } else {
          ElMessage.error('数据校验失败: ' + validationResult.value.message)
        }
      } catch (error) {
        console.error('执行数据校验失败:', error)
        ElMessage.error('执行数据校验失败: ' + error.message)
      }
    }
    
    // 重置校验表单
    const resetValidationForm = () => {
      validationForm.value = {
        dataSourceId: null,
        targetIndex: '',
        validationTypes: ['count', 'quality'],
        rangeType: 'full',
        sampleRatio: 10,
        timeRange: []
      }
    }
    
    // 搜索问题
    const searchIssues = async () => {
      try {
        const response = await axios.get('/api/data-quality/issues', {
          params: {
            status: issueFilter.value.status,
            type: issueFilter.value.type,
            keyword: issueFilter.value.keyword
          }
        })
        issues.value = response.data || response
      } catch (error) {
        console.error('获取问题列表失败:', error)
        ElMessage.error('获取问题列表失败')
      }
    }
    
    // 查看问题详情
    const viewIssueDetails = (issue) => {
      currentIssue.value = issue
      issueDetailsVisible.value = true
    }
    
    // 修复问题
    const fixIssue = (issue) => {
      currentIssue.value = issue
      fixIssueVisible.value = true
    }
    
    // 执行修复
    const executeFix = async () => {
      try {
        ElMessage.info('正在执行修复...')
        const response = await axios.post(`/api/data-quality/issues/${currentIssue.value.id}/fix`, {
          fixStrategy: fixForm.value.strategy
        })
        const payload = response.data || response
        if (payload.success) {
          ElMessage.success('问题修复成功')
          fixIssueVisible.value = false
          searchIssues()
        } else {
          ElMessage.error('问题修复失败: ' + payload.message)
        }
      } catch (error) {
        console.error('执行修复失败:', error)
        ElMessage.error('执行修复失败: ' + error.message)
      }
    }
    
    // 忽略问题
    const ignoreIssue = async (issue) => {
      try {
        const response = await axios.post(`/api/data-quality/issues/${issue.id}/ignore`)
        if (response.data || response) {
          ElMessage.success('问题已忽略')
          searchIssues()
        } else {
          ElMessage.error('忽略问题失败')
        }
      } catch (error) {
        console.error('忽略问题失败:', error)
        ElMessage.error('忽略问题失败: ' + error.message)
      }
    }
    
    // 生成统计
    const generateStatistics = async () => {
      try {
        const response = await axios.get('/api/data-quality/issues/stats', {
          params: {
            timeRange: statisticsForm.value.timeRange
          }
        })
        initCharts(response.data || response)
        ElMessage.success('统计数据生成成功')
      } catch (error) {
        console.error('生成统计失败:', error)
        ElMessage.error('生成统计失败: ' + error.message)
      }
    }
    
    // 导出校验结果
    const exportValidationResult = async () => {
      try {
        const response = await axios.post('/api/data-quality/export', {
          timeRange: '7d',
          format: 'excel'
        })
        ElMessage.success('结果导出成功')
      } catch (error) {
        console.error('导出结果失败:', error)
        ElMessage.error('导出结果失败: ' + error.message)
      }
    }
    
    // 初始化图表
    const initCharts = (stats = {}) => {
      const typeSeriesData = stats.typeDistribution || [
        { value: 30, name: '数量差异' },
        { value: 15, name: '质量问题' },
        { value: 20, name: '格式错误' },
        { value: 10, name: '业务规则' }
      ]
      const statusSeriesData = stats.statusDistribution || [
        { value: 40, name: '待处理' },
        { value: 10, name: '处理中' },
        { value: 20, name: '已修复' },
        { value: 5, name: '已忽略' }
      ]
      const trendXAxis = stats.trend?.map(item => item.date || item.name) || ['1月10日', '1月11日', '1月12日', '1月13日', '1月14日', '1月15日']
      const trendSeries = stats.trend?.map(item => item.count ?? item.value) || [12, 19, 15, 25, 18, 10]

      // 问题类型分布
      if (typeChartInstance) {
        typeChartInstance.dispose()
      }
      typeChartInstance = echarts.init(typeChart.value)
      const typeOption = {
        tooltip: {
          trigger: 'item'
        },
        legend: {
          orient: 'vertical',
          left: 'left'
        },
        series: [
          {
            name: '问题类型',
            type: 'pie',
            radius: '50%',
            data: typeSeriesData,
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      }
      typeChartInstance.setOption(typeOption)
      
      // 问题状态分布
      if (statusChartInstance) {
        statusChartInstance.dispose()
      }
      statusChartInstance = echarts.init(statusChart.value)
      const statusOption = {
        tooltip: {
          trigger: 'item'
        },
        legend: {
          orient: 'vertical',
          left: 'left'
        },
        series: [
          {
            name: '问题状态',
            type: 'pie',
            radius: '50%',
            data: statusSeriesData,
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      }
      statusChartInstance.setOption(statusOption)
      
      // 每日问题数量
      if (trendChartInstance) {
        trendChartInstance.dispose()
      }
      trendChartInstance = echarts.init(trendChart.value)
      const trendOption = {
        tooltip: {
          trigger: 'axis'
        },
        xAxis: {
          type: 'category',
          data: trendXAxis
        },
        yAxis: {
          type: 'value'
        },
        series: [
          {
            data: trendSeries,
            type: 'line',
            smooth: true
          }
        ]
      }
      trendChartInstance.setOption(trendOption)
    }
    
    // 获取问题类型标签
    const getIssueTypeTag = (type) => {
      const tags = {
        count: 'warning',
        quality: 'danger',
        format: 'info',
        business: 'primary'
      }
      return tags[type] || 'default'
    }
    
    // 获取问题类型标签
    const getIssueTypeLabel = (type) => {
      const labels = {
        count: '数量差异',
        quality: '质量问题',
        format: '格式错误',
        business: '业务规则'
      }
      return labels[type] || type
    }
    
    // 获取问题状态标签
    const getIssueStatusTag = (status) => {
      const tags = {
        pending: 'warning',
        processing: 'info',
        fixed: 'success',
        ignored: 'default'
      }
      return tags[status] || 'default'
    }
    
    // 获取问题状态标签
    const getIssueStatusLabel = (status) => {
      const labels = {
        pending: '待处理',
        processing: '处理中',
        fixed: '已修复',
        ignored: '已忽略'
      }
      return labels[status] || status
    }
    
    onMounted(() => {
      fetchDataSources()
      searchIssues()

      nextTick(() => {
        initCharts({})
      })

      resizeHandler = () => {
        typeChartInstance?.resize()
        statusChartInstance?.resize()
        trendChartInstance?.resize()
      }
      window.addEventListener('resize', resizeHandler)
    })
    
    onUnmounted(() => {
      if (resizeHandler) {
        window.removeEventListener('resize', resizeHandler)
      }
      typeChartInstance?.dispose()
      statusChartInstance?.dispose()
      trendChartInstance?.dispose()
    })
    
    return {
      activeTab,
      dataSourceList,
      issues,
      validationResult,
      validationResultVisible,
      issueDetailsVisible,
      fixIssueVisible,
      currentIssue,
      validationForm,
      issueFilter,
      statisticsForm,
      fixForm,
      typeChart,
      statusChart,
      trendChart,
      executeValidation,
      resetValidationForm,
      searchIssues,
      viewIssueDetails,
      fixIssue,
      executeFix,
      ignoreIssue,
      generateStatistics,
      exportValidationResult,
      getIssueTypeTag,
      getIssueTypeLabel,
      getIssueStatusTag,
      getIssueStatusLabel
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

.issues-filter {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.chart {
  height: 250px;
}

.issue-details {
  margin-top: 20px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.issue-details h4 {
  margin-bottom: 10px;
  color: #606266;
}

.issue-details pre {
  white-space: pre-wrap;
  font-family: monospace;
  font-size: 12px;
  line-height: 1.5;
}
</style>
