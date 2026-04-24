<template>
  <div class="page-grid">
    <section class="panel-card">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Monitor Center</p>
          <h3>流程监控中心</h3>
        </div>
        <div class="management-meta">
          <span class="meta-pill">运行中 {{ statistics.runningProcessCount || 0 }}</span>
          <span class="meta-pill">已完成 {{ statistics.completedProcessCount || 0 }}</span>
          <span class="meta-pill">当前任务 {{ statistics.totalTaskCount || 0 }}</span>
        </div>
      </div>

      <div class="management-toolbar">
        <div class="management-filters">
          <label class="filter-grow">
            <span>搜索流程</span>
            <input
              v-model.trim="keyword"
              type="text"
              placeholder="搜索实例 ID、定义、业务键、申请人或任务名"
              @keyup.enter="searchAll"
            />
          </label>
          <label class="compact-field">
            <span>任务分配</span>
            <select v-model="assigneeFilter">
              <option value="all">全部</option>
              <option value="assigned">已分配</option>
              <option value="unassigned">未分配</option>
            </select>
          </label>
          <label class="compact-field">
            <span>历史面板</span>
            <select v-model="historyScope">
              <option value="running">运行中</option>
              <option value="completed">已完成</option>
            </select>
          </label>
        </div>

        <div class="management-actions">
          <button type="button" class="secondary-btn" @click="resetFilters">重置</button>
          <button type="button" class="ghost-btn" :disabled="pageLoading" @click="refreshAll">
            {{ pageLoading ? '刷新中...' : '刷新' }}
          </button>
        </div>
      </div>

      <p v-if="pageError" class="feedback error">{{ pageError }}</p>
      <p v-if="historyError" class="feedback error">{{ historyError }}</p>
    </section>

    <section class="stats-grid">
      <article class="stat-card">
        <span class="muted-text">运行中流程</span>
        <strong>{{ statistics.runningProcessCount || 0 }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">已完成流程</span>
        <strong>{{ statistics.completedProcessCount || 0 }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">已分配任务</span>
        <strong>{{ statistics.assignedTaskCount || 0 }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">未分配任务</span>
        <strong>{{ statistics.unassignedTaskCount || 0 }}</strong>
      </article>
    </section>

    <section class="content-grid two-column">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Runtime Instances</p>
            <h3>运行中流程</h3>
          </div>
          <span class="muted-text">{{ runningLoading ? '加载中...' : `共 ${runningTotal} 条` }}</span>
        </div>
        <table class="simple-table">
          <thead>
            <tr>
              <th>实例 ID</th>
              <th>定义 Key</th>
              <th>名称</th>
              <th>申请人</th>
              <th>开始时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="instance in runningProcesses" :key="instance.id">
              <td>{{ instance.id }}</td>
              <td>{{ instance.processDefinitionKey }}</td>
              <td>{{ instance.processDefinitionName }}</td>
              <td>{{ instance.applicant || '-' }}</td>
              <td>{{ formatDate(instance.startTime) }}</td>
              <td>
                <div class="inline-actions">
                  <button type="button" class="secondary-btn mini-btn" @click="loadHistory(instance.id)">历史</button>
                  <button type="button" class="primary-btn mini-btn" @click="openDetail(instance.id)">详情</button>
                </div>
              </td>
            </tr>
            <tr v-if="runningLoading">
              <td colspan="6" class="empty-row">运行中流程加载中...</td>
            </tr>
            <tr v-else-if="!runningProcesses.length">
              <td colspan="6" class="empty-row">
                <div class="table-empty-state">
                  <strong>没有匹配的运行中实例</strong>
                  <p>可以调整关键词或切换筛选条件。</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <AppPagination
          :page="runningPage"
          :size="runningSize"
          :total="runningTotal"
          :disabled="runningLoading"
          @update:page="changeRunningPage"
          @update:size="changeRunningSize"
        />
      </article>

      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Task Pool</p>
            <h3>当前任务</h3>
          </div>
          <span class="muted-text">{{ tasksLoading ? '加载中...' : `共 ${tasksTotal} 条` }}</span>
        </div>
        <table class="simple-table">
          <thead>
            <tr>
              <th>任务</th>
              <th>处理人</th>
              <th>流程实例</th>
              <th>申请人</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="task in tasks" :key="task.id">
              <td>{{ task.name }}</td>
              <td>{{ task.assignee || '未分配' }}</td>
              <td>{{ task.processInstanceId }}</td>
              <td>{{ task.applicant || '-' }}</td>
              <td>{{ formatDate(task.createTime) }}</td>
              <td>
                <button
                  v-if="task.processInstanceId"
                  type="button"
                  class="secondary-btn mini-btn"
                  @click="openDetail(task.processInstanceId)"
                >
                  查看实例
                </button>
              </td>
            </tr>
            <tr v-if="tasksLoading">
              <td colspan="6" class="empty-row">任务列表加载中...</td>
            </tr>
            <tr v-else-if="!tasks.length">
              <td colspan="6" class="empty-row">
                <div class="table-empty-state">
                  <strong>没有匹配的任务数据</strong>
                  <p>可以切换任务分配状态或重新搜索。</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <AppPagination
          :page="tasksPage"
          :size="tasksSize"
          :total="tasksTotal"
          :disabled="tasksLoading"
          @update:page="changeTasksPage"
          @update:size="changeTasksSize"
        />
      </article>
    </section>

    <section class="content-grid two-column">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Completed</p>
            <h3>已完成流程</h3>
          </div>
          <span class="muted-text">{{ completedLoading ? '加载中...' : `共 ${completedTotal} 条` }}</span>
        </div>
        <table class="simple-table">
          <thead>
            <tr>
              <th>实例 ID</th>
              <th>定义名称</th>
              <th>申请人</th>
              <th>耗时</th>
              <th>结束时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="instance in completedProcesses" :key="instance.id">
              <td>{{ instance.id }}</td>
              <td>{{ instance.processDefinitionName }}</td>
              <td>{{ instance.applicant || '-' }}</td>
              <td>{{ instance.duration || 0 }} ms</td>
              <td>{{ formatDate(instance.endTime) }}</td>
              <td>
                <div class="inline-actions">
                  <button type="button" class="secondary-btn mini-btn" @click="loadHistory(instance.id)">历史</button>
                  <button type="button" class="primary-btn mini-btn" @click="openDetail(instance.id)">详情</button>
                </div>
              </td>
            </tr>
            <tr v-if="completedLoading">
              <td colspan="6" class="empty-row">已完成流程加载中...</td>
            </tr>
            <tr v-else-if="!completedProcesses.length">
              <td colspan="6" class="empty-row">
                <div class="table-empty-state">
                  <strong>没有匹配的已完成实例</strong>
                  <p>可以调整搜索条件，或者等待新的流程归档。</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <AppPagination
          :page="completedPage"
          :size="completedSize"
          :total="completedTotal"
          :disabled="completedLoading"
          @update:page="changeCompletedPage"
          @update:size="changeCompletedSize"
        />
      </article>

      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Instance Timeline</p>
            <h3>流程历史</h3>
          </div>
          <span class="muted-text">{{ selectedHistoryLabel }}</span>
        </div>
        <p v-if="historyLoading" class="muted-text">流程历史加载中...</p>
        <table v-else class="simple-table">
          <thead>
            <tr>
              <th>任务</th>
              <th>处理人</th>
              <th>开始</th>
              <th>结束</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in history" :key="item.id">
              <td>{{ item.name }}</td>
              <td>{{ item.assignee || '-' }}</td>
              <td>{{ formatDate(item.createTime) }}</td>
              <td>{{ formatDate(item.endTime) }}</td>
            </tr>
            <tr v-if="!history.length">
              <td colspan="4" class="empty-row">
                <div class="table-empty-state">
                  <strong>尚未选择实例历史</strong>
                  <p>点击运行中或已完成列表里的“历史”即可查看。</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </article>
    </section>

    <div v-if="detailVisible" class="modal-mask" @click.self="closeDetail">
      <div class="modal-panel detail-modal compact-detail-modal">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Process Inspector</p>
            <h3>流程实例详情</h3>
          </div>
          <div class="inline-actions">
            <span v-if="detail?.status" :class="['status-pill', detail.active ? 'success-pill' : 'muted-pill']">
              {{ detail.active ? '运行中' : '已完成' }}
            </span>
            <button type="button" class="ghost-btn" @click="closeDetail">关闭</button>
          </div>
        </div>

        <p v-if="detailError" class="feedback error">{{ detailError }}</p>
        <p v-else-if="detailLoading" class="muted-text">实例详情加载中...</p>

        <template v-else-if="detail">
          <section class="detail-grid compact-detail-grid">
            <article class="detail-item">
              <span class="detail-key">实例 ID</span>
              <strong class="detail-value">{{ detail.processInstanceId }}</strong>
            </article>
            <article class="detail-item">
              <span class="detail-key">流程定义</span>
              <strong class="detail-value">{{ detail.processDefinitionName || '-' }}</strong>
            </article>
            <article class="detail-item">
              <span class="detail-key">业务键</span>
              <strong class="detail-value">{{ detail.businessKey || '-' }}</strong>
            </article>
            <article class="detail-item">
              <span class="detail-key">状态</span>
              <strong class="detail-value">{{ detail.status || '-' }}</strong>
            </article>
          </section>

          <div class="detail-tabs">
            <button
              v-for="tab in detailTabs"
              :key="tab.value"
              type="button"
              :class="['tab-btn', { active: detailTab === tab.value }]"
              @click="detailTab = tab.value"
            >
              {{ tab.label }}
            </button>
          </div>

          <section v-if="detailTab === 'overview'" class="page-grid">
            <section class="detail-grid">
              <article class="detail-item">
                <span class="detail-key">定义 Key</span>
                <strong class="detail-value">{{ detail.processDefinitionKey || '-' }}</strong>
              </article>
              <article class="detail-item">
                <span class="detail-key">部署 ID</span>
                <strong class="detail-value">{{ detail.deploymentId || '-' }}</strong>
              </article>
              <article class="detail-item">
                <span class="detail-key">开始时间</span>
                <strong class="detail-value">{{ formatDate(detail.startTime) }}</strong>
              </article>
              <article class="detail-item">
                <span class="detail-key">结束时间</span>
                <strong class="detail-value">{{ formatDate(detail.endTime) }}</strong>
              </article>
            </section>

            <section class="content-grid two-column">
              <article class="panel-card">
                <div class="panel-head">
                  <div>
                    <p class="eyebrow">Business Snapshot</p>
                    <h3>业务快照</h3>
                  </div>
                </div>
                <table class="simple-table">
                  <tbody>
                    <tr>
                      <th>申请人</th>
                      <td>{{ detail.variables?.applicant || '-' }}</td>
                    </tr>
                    <tr>
                      <th>部门审批人</th>
                      <td>{{ detail.variables?.deptManager || '-' }}</td>
                    </tr>
                    <tr>
                      <th>总经理审批人</th>
                      <td>{{ detail.variables?.generalManager || '-' }}</td>
                    </tr>
                    <tr>
                      <th>请假天数</th>
                      <td>{{ detail.variables?.days || '-' }}</td>
                    </tr>
                  </tbody>
                </table>
              </article>

              <article class="panel-card">
                <div class="panel-head">
                  <div>
                    <p class="eyebrow">Current Tasks</p>
                    <h3>当前任务</h3>
                  </div>
                </div>
                <table class="simple-table">
                  <thead>
                    <tr>
                      <th>任务名</th>
                      <th>处理人</th>
                      <th>创建时间</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="task in detail.currentTasks || []" :key="task.id">
                      <td>{{ task.name }}</td>
                      <td>{{ task.assignee || '未分配' }}</td>
                      <td>{{ formatDate(task.createTime) }}</td>
                    </tr>
                    <tr v-if="!(detail.currentTasks || []).length">
                      <td colspan="3" class="empty-row">当前没有活动任务</td>
                    </tr>
                  </tbody>
                </table>
              </article>
            </section>
          </section>

          <article v-else-if="detailTab === 'variables'" class="panel-card">
            <div class="panel-head">
              <div>
                <p class="eyebrow">Variables</p>
                <h3>流程变量</h3>
              </div>
            </div>
            <table class="simple-table">
              <thead>
                <tr>
                  <th>变量名</th>
                  <th>变量值</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in detailVariables" :key="item.key">
                  <td>{{ item.key }}</td>
                  <td>{{ item.value }}</td>
                </tr>
                <tr v-if="!detailVariables.length">
                  <td colspan="2" class="empty-row">没有可展示的流程变量</td>
                </tr>
              </tbody>
            </table>
          </article>

          <article v-else-if="detailTab === 'trajectory'" class="panel-card">
            <div class="panel-head">
              <div>
                <p class="eyebrow">Trajectory</p>
                <h3>任务轨迹图</h3>
              </div>
            </div>
            <div class="trajectory-canvas">
              <svg :viewBox="`0 0 ${graph.width} ${graph.height}`" class="trajectory-svg" preserveAspectRatio="xMidYMid meet">
                <line
                  v-for="edge in graph.edges"
                  :key="`${edge.from}-${edge.to}`"
                  class="trajectory-line"
                  :x1="edge.fromNode.x"
                  :y1="edge.fromNode.y"
                  :x2="edge.toNode.x"
                  :y2="edge.toNode.y"
                />
                <g v-for="node in graph.nodes" :key="node.id">
                  <circle
                    class="trajectory-node"
                    :class="{ active: node.status === 'ACTIVE' }"
                    :cx="node.x"
                    :cy="node.y"
                    r="20"
                  />
                  <text :x="node.x" :y="node.y - 34" class="trajectory-caption">{{ node.activityType }}</text>
                  <text :x="node.x" :y="node.y + 48" class="trajectory-label">{{ shorten(node.activityName || node.activityId) }}</text>
                  <text :x="node.x" :y="node.y + 66" class="trajectory-meta">{{ formatDate(node.startTime, true) }}</text>
                </g>
              </svg>
              <div v-if="!graph.nodes.length" class="empty-state">暂无可绘制的轨迹节点</div>
            </div>
          </article>

          <article v-else class="panel-card">
            <div class="panel-head">
              <div>
                <p class="eyebrow">Execution History</p>
                <h3>执行历史</h3>
              </div>
            </div>
            <table class="simple-table">
              <thead>
                <tr>
                  <th>任务</th>
                  <th>处理人</th>
                  <th>开始</th>
                  <th>结束</th>
                  <th>耗时</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in detail.history || []" :key="item.id">
                  <td>{{ item.name }}</td>
                  <td>{{ item.assignee || '-' }}</td>
                  <td>{{ formatDate(item.createTime) }}</td>
                  <td>{{ formatDate(item.endTime) }}</td>
                  <td>{{ item.duration || 0 }} ms</td>
                </tr>
                <tr v-if="!(detail.history || []).length">
                  <td colspan="5" class="empty-row">暂无执行历史</td>
                </tr>
              </tbody>
            </table>
          </article>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import AppPagination from '../components/common/AppPagination.vue'
import { http } from '../api/http'
import { usePagedQuery } from '../composables/usePagedQuery'

const statistics = ref({})
const keyword = ref('')
const assigneeFilter = ref('all')
const historyScope = ref('running')
const history = ref([])
const historyLoading = ref(false)
const historyError = ref('')
const selectedHistoryId = ref(null)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detail = ref(null)
const detailTab = ref('overview')
const pageError = ref('')
const statisticsLoading = ref(false)

const runningQuery = usePagedQuery(async ({ page, size }) => {
  const { data } = await http.get('/monitoring/processes/running/query', {
    params: {
      page,
      size,
      keyword: keyword.value || undefined
    }
  })
  return data || {}
}, { size: 6 })

const completedQuery = usePagedQuery(async ({ page, size }) => {
  const { data } = await http.get('/monitoring/processes/completed/query', {
    params: {
      page,
      size,
      keyword: keyword.value || undefined
    }
  })
  return data || {}
}, { size: 6 })

const tasksQuery = usePagedQuery(async ({ page, size }) => {
  const { data } = await http.get('/monitoring/tasks/query', {
    params: {
      page,
      size,
      keyword: keyword.value || undefined,
      assigneeFilter: assigneeFilter.value || undefined
    }
  })
  return data || {}
}, { size: 6 })

const runningProcesses = runningQuery.items
const runningLoading = runningQuery.loading
const runningTotal = runningQuery.total
const runningPage = runningQuery.page
const runningSize = runningQuery.size

const completedProcesses = completedQuery.items
const completedLoading = completedQuery.loading
const completedTotal = completedQuery.total
const completedPage = completedQuery.page
const completedSize = completedQuery.size

const tasks = tasksQuery.items
const tasksLoading = tasksQuery.loading
const tasksTotal = tasksQuery.total
const tasksPage = tasksQuery.page
const tasksSize = tasksQuery.size

const detailTabs = [
  { value: 'overview', label: '概览' },
  { value: 'variables', label: '变量' },
  { value: 'trajectory', label: '轨迹' },
  { value: 'history', label: '历史' }
]

const pageLoading = computed(() =>
  Boolean(statisticsLoading.value || runningLoading.value || completedLoading.value || tasksLoading.value)
)

const selectedHistoryLabel = computed(() => {
  if (!selectedHistoryId.value) {
    return '点击实例历史后展示'
  }
  return `实例 ${selectedHistoryId.value} 的执行轨迹`
})

const detailVariables = computed(() =>
  Object.entries(detail.value?.variables || {}).map(([key, value]) => ({
    key,
    value: stringifyValue(value)
  }))
)

const graph = computed(() => {
  const nodes = (detail.value?.trajectoryNodes || []).map((item, index) => ({
    ...item,
    x: 120 + index * 180,
    y: 120
  }))
  const positionMap = new Map(nodes.map((item) => [item.id, item]))
  const edges = (detail.value?.trajectoryEdges || [])
    .map((item) => ({
      ...item,
      fromNode: positionMap.get(item.from),
      toNode: positionMap.get(item.to)
    }))
    .filter((item) => item.fromNode && item.toNode)

  return {
    nodes,
    edges,
    width: Math.max(760, nodes.length * 180 + 120),
    height: 240
  }
})

function formatDate(value, short = false) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  return short ? date.toLocaleString().slice(0, 16) : date.toLocaleString()
}

function shorten(value) {
  if (!value) {
    return '-'
  }
  return value.length > 14 ? `${value.slice(0, 14)}...` : value
}

function stringifyValue(value) {
  if (value == null) {
    return '-'
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

function currentHistorySource() {
  return historyScope.value === 'completed'
    ? completedProcesses.value[0]
    : runningProcesses.value[0]
}

async function loadStatistics() {
  statisticsLoading.value = true
  try {
    const { data } = await http.get('/monitoring/statistics')
    statistics.value = data || {}
  } catch (error) {
    pageError.value = error.normalizedMessage || error.message || '统计数据加载失败'
  } finally {
    statisticsLoading.value = false
  }
}

async function loadHistory(processInstanceId) {
  selectedHistoryId.value = processInstanceId
  historyLoading.value = true
  historyError.value = ''
  try {
    const { data } = await http.get(`/monitoring/processes/${processInstanceId}/history`)
    history.value = data || []
  } catch (error) {
    history.value = []
    historyError.value = error.normalizedMessage || error.message || '流程历史加载失败'
  } finally {
    historyLoading.value = false
  }
}

async function syncHistorySelection() {
  const source = currentHistorySource()
  if (source?.id) {
    await loadHistory(source.id)
    return
  }
  selectedHistoryId.value = null
  history.value = []
}

async function refreshLists(resetPage = false) {
  pageError.value = ''
  const loaders = resetPage
    ? [runningQuery.search(), completedQuery.search(), tasksQuery.search()]
    : [runningQuery.load(), completedQuery.load(), tasksQuery.load()]
  const results = await Promise.allSettled(loaders)
  const firstRejected = results.find((item) => item.status === 'rejected')
  if (firstRejected) {
    pageError.value = firstRejected.reason?.normalizedMessage || firstRejected.reason?.message || '监控列表加载失败'
  }
  await syncHistorySelection()
}

async function refreshAll() {
  await Promise.all([loadStatistics(), refreshLists(false)])
}

function resetFilters() {
  keyword.value = ''
  assigneeFilter.value = 'all'
  historyScope.value = 'running'
  searchAll()
}

function searchAll() {
  refreshLists(true)
}

async function changeRunningPage(nextPage) {
  await runningQuery.changePage(nextPage).catch((error) => {
    pageError.value = error.normalizedMessage || error.message
  })
  if (historyScope.value === 'running') {
    await syncHistorySelection()
  }
}

async function changeRunningSize(nextSize) {
  await runningQuery.changeSize(nextSize).catch((error) => {
    pageError.value = error.normalizedMessage || error.message
  })
  if (historyScope.value === 'running') {
    await syncHistorySelection()
  }
}

async function changeCompletedPage(nextPage) {
  await completedQuery.changePage(nextPage).catch((error) => {
    pageError.value = error.normalizedMessage || error.message
  })
  if (historyScope.value === 'completed') {
    await syncHistorySelection()
  }
}

async function changeCompletedSize(nextSize) {
  await completedQuery.changeSize(nextSize).catch((error) => {
    pageError.value = error.normalizedMessage || error.message
  })
  if (historyScope.value === 'completed') {
    await syncHistorySelection()
  }
}

async function changeTasksPage(nextPage) {
  await tasksQuery.changePage(nextPage).catch((error) => {
    pageError.value = error.normalizedMessage || error.message
  })
}

async function changeTasksSize(nextSize) {
  await tasksQuery.changeSize(nextSize).catch((error) => {
    pageError.value = error.normalizedMessage || error.message
  })
}

function closeDetail() {
  detailVisible.value = false
  detailLoading.value = false
  detailError.value = ''
  detail.value = null
  detailTab.value = 'overview'
}

async function openDetail(processInstanceId) {
  detailVisible.value = true
  detailLoading.value = true
  detailError.value = ''
  detail.value = null
  detailTab.value = 'overview'
  try {
    const { data } = await http.get(`/monitoring/processes/${processInstanceId}/detail`)
    detail.value = data || null
  } catch (error) {
    detailError.value = error.normalizedMessage || error.message || '实例详情加载失败'
  } finally {
    detailLoading.value = false
  }
}

onMounted(async () => {
  await refreshAll()
})

watch(historyScope, async () => {
  historyError.value = ''
  await syncHistorySelection()
})
</script>
