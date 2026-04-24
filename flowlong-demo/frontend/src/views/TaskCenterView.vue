<template>
  <div class="page-grid">
    <section class="stats-grid">
      <article class="stat-card">
        <span class="muted-text">待办总数</span>
        <strong>{{ tasks.length }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">部门审批</span>
        <strong>{{ deptTaskCount }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">总经理审批</span>
        <strong>{{ generalTaskCount }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">待签收</span>
        <strong>{{ unassignedTaskCount }}</strong>
      </article>
    </section>

    <section class="content-grid task-workbench-grid">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Task Inbox</p>
            <h3>我的待办任务</h3>
          </div>
          <button type="button" class="ghost-btn" @click="fetchTasks">刷新</button>
        </div>

        <div class="action-row">
          <label class="flex-field">
            <span>搜索任务</span>
            <input v-model.trim="keyword" type="text" placeholder="按任务名、申请人、业务键过滤" />
          </label>
          <label class="compact-field">
            <span>任务类型</span>
            <select v-model="taskType">
              <option value="all">全部</option>
              <option value="dept">部门审批</option>
              <option value="general">总经理审批</option>
              <option value="other">其他节点</option>
            </select>
          </label>
        </div>

        <table class="simple-table">
          <thead>
            <tr>
              <th>任务名称</th>
              <th>业务页</th>
              <th>表单</th>
              <th>申请人</th>
              <th>业务键</th>
              <th>处理人</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="task in filteredTasks" :key="task.id">
              <td>{{ task.nodeLabel || task.name }}</td>
              <td>{{ task.pageLabel || '-' }}</td>
              <td>{{ task.formLabel || task.formKey || '-' }}</td>
              <td>{{ task.applicant || '-' }}</td>
              <td>{{ task.businessKey || task.processInstanceId }}</td>
              <td>{{ task.assignee || '待签收' }}</td>
              <td>{{ formatDate(task.createTime) }}</td>
              <td>
                <div class="inline-actions">
                  <button type="button" class="secondary-btn mini-btn" @click="previewTask(task)">查看</button>
                  <button
                    v-if="canOpenMonitoring && task.processInstanceId"
                    type="button"
                    class="secondary-btn mini-btn"
                    @click="openMonitoring(task)"
                  >
                    监控
                  </button>
                  <button type="button" class="primary-btn mini-btn" @click="openTask(task, 'approved')">同意</button>
                  <button type="button" class="danger-btn mini-btn" @click="openTask(task, 'rejected')">驳回</button>
                </div>
              </td>
            </tr>
            <tr v-if="!filteredTasks.length">
              <td colspan="8" class="empty-row">当前没有符合条件的待办任务</td>
            </tr>
          </tbody>
        </table>
      </article>

      <aside class="panel-card approval-panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">{{ selectedTask?.pageLabel || 'Approval Console' }}</p>
            <h3>{{ selectedTaskFormConfig.title }}</h3>
          </div>
        </div>

        <template v-if="selectedTask">
          <section class="approval-summary">
            <div class="detail-item">
              <span class="detail-key">节点标签</span>
              <strong class="detail-value">{{ selectedTask.nodeLabel || selectedTask.name }}</strong>
            </div>
            <div class="detail-item">
              <span class="detail-key">表单 Key</span>
              <strong class="detail-value">{{ selectedTask.formKey || '-' }}</strong>
            </div>
            <div class="detail-item">
              <span class="detail-key">流程实例</span>
              <strong class="detail-value">{{ selectedTask.processInstanceId }}</strong>
            </div>
            <div class="detail-item">
              <span class="detail-key">业务键</span>
              <strong class="detail-value">{{ selectedTask.businessKey || '-' }}</strong>
            </div>
          </section>

          <div class="task-form-badge-row">
            <span class="meta-pill">{{ selectedTask.pageLabel || '待办审批页' }}</span>
            <span class="meta-pill">{{ selectedTask.formLabel || selectedTaskFormConfig.title }}</span>
          </div>

          <div class="action-row task-link-row">
            <button v-if="canOpenMonitoring" type="button" class="secondary-btn" @click="openMonitoring(selectedTask)">查看流程监控</button>
          </div>

          <ReadonlyFormSnapshot
            class="task-readonly-form"
            :form-key="selectedTask.formKey"
            :source="selectedTask"
            empty-text="当前任务没有可展示的业务摘要"
          />

          <div class="segmented">
            <button
              type="button"
              :class="['segment-btn', { active: selectedOutcome === 'approved' }]"
              @click="selectedOutcome = 'approved'"
            >
              同意
            </button>
            <button
              type="button"
              :class="['segment-btn', { active: selectedOutcome === 'rejected' }]"
              @click="selectedOutcome = 'rejected'"
            >
              驳回
            </button>
          </div>

          <div class="form-stack">
            <div class="form-grid schema-form-grid">
              <label
                v-for="field in decisionFields"
                :key="field.key"
                :class="{ 'full-width-field': resolveFieldSpan(field) > 1 || field.type === 'textarea' }"
              >
                <span>{{ field.label }}</span>
                <textarea
                  v-model.trim="decisionForm[field.key]"
                  v-bind="resolveDecisionProps(field)"
                  :rows="field.rows || 4"
                  :placeholder="field.placeholder || ''"
                ></textarea>
                <small v-if="field.help" class="field-help">{{ field.help }}</small>
              </label>
            </div>
            <div class="action-row">
              <button type="button" class="primary-btn" :disabled="loading" @click="completeSelectedTask">
                {{ loading ? '提交中...' : `确认${selectedOutcome === 'approved' ? '同意' : '驳回'}` }}
              </button>
              <button type="button" class="secondary-btn" @click="closeTask">取消</button>
            </div>
            <p v-if="statusMessage" class="feedback success">{{ statusMessage }}</p>
            <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>
          </div>
        </template>

        <div v-else class="empty-state task-empty-panel">
          从左侧任务列表中选择一条待办，右侧会按当前节点的表单配置展示业务单和审批入口。
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ReadonlyFormSnapshot from '../components/ReadonlyFormSnapshot.vue'
import { canAccessPath, getCurrentUser, http } from '../api/http'
import { ensureRemoteFormRegistryLoaded, resolveDecisionFields, resolveFieldProps, resolveFieldSpan, resolveFormConfig, validateDecisionBySchema } from '../utils/businessForms'

const currentUser = ref(getCurrentUser())
const router = useRouter()
const route = useRoute()
const tasks = ref([])
const selectedTask = ref(null)
const selectedOutcome = ref('approved')
const decisionForm = ref({
  approvalComment: '',
  rejectReason: '',
  systemRemark: ''
})
const keyword = ref('')
const taskType = ref('all')
const loading = ref(false)
const statusMessage = ref('')
const errorMessage = ref('')
const canOpenMonitoring = canAccessPath('/monitoring', 'monitoring:view')

const filteredTasks = computed(() => {
  const needle = keyword.value.trim().toLowerCase()
  return tasks.value.filter((task) => {
    const typeMatched =
      taskType.value === 'all'
        ? true
        : taskType.value === 'dept'
          ? (task.nodeLabel || task.name || '').includes('部门')
          : taskType.value === 'general'
            ? (task.nodeLabel || task.name || '').includes('总经理')
            : !(task.nodeLabel || task.name || '').includes('部门') && !(task.nodeLabel || task.name || '').includes('总经理')

    if (!typeMatched) {
      return false
    }
    if (!needle) {
      return true
    }
    return [task.name, task.nodeLabel, task.applicant, task.businessKey, task.processInstanceId, task.assignee, task.pageLabel, task.formLabel]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(needle))
  })
})

const selectedTaskFormConfig = computed(() => resolveFormConfig(selectedTask.value?.formKey))
const decisionFields = computed(() => resolveDecisionFields(selectedTask.value?.formKey, selectedOutcome.value))
const deptTaskCount = computed(() => tasks.value.filter((task) => (task.nodeLabel || task.name || '').includes('部门')).length)
const generalTaskCount = computed(() => tasks.value.filter((task) => (task.nodeLabel || task.name || '').includes('总经理')).length)
const unassignedTaskCount = computed(() => tasks.value.filter((task) => !task.assignee).length)

function formatDate(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

function resetMessages() {
  statusMessage.value = ''
  errorMessage.value = ''
}

function previewTask(task) {
  resetMessages()
  selectedTask.value = task
}

function openTask(task, outcome) {
  resetMessages()
  selectedTask.value = task
  selectedOutcome.value = outcome
  decisionForm.value = {
    approvalComment: '',
    rejectReason: '',
    systemRemark: ''
  }
}

function closeTask() {
  selectedTask.value = null
  decisionForm.value = {
    approvalComment: '',
    rejectReason: '',
    systemRemark: ''
  }
}

function resolveDecisionProps(field) {
  return resolveFieldProps(field, decisionForm.value, {
    task: selectedTask.value,
    outcome: selectedOutcome.value
  })
}

function buildTaskPayload(task, outcome) {
  return {
    taskId: task.id,
    processInstanceId: task.processInstanceId,
    status: outcome === 'approved' ? 'APPROVED' : 'REJECTED',
    variables: {
      approved: outcome === 'approved',
      comment: outcome === 'approved' ? decisionForm.value.approvalComment : decisionForm.value.rejectReason,
      approvalComment: decisionForm.value.approvalComment,
      rejectReason: decisionForm.value.rejectReason,
      systemRemark: decisionForm.value.systemRemark,
      formKey: task.formKey,
      pageLabel: task.pageLabel,
      formLabel: task.formLabel
    }
  }
}

function openMonitoring(task) {
  if (!task?.processInstanceId) {
    return
  }
  router.push({
    path: '/monitoring',
    query: {
      processInstanceId: task.processInstanceId,
      openDetail: '1',
      source: 'tasks'
    }
  })
}

async function fetchTasks() {
  try {
    if (!currentUser.value?.username) {
      tasks.value = []
      return
    }
    const { data } = await http.get(`/flowlong/tasks/${currentUser.value.username}`)
    tasks.value = data || []
    const targetTaskId = String(route.query.taskId || '')
    const targetInstanceId = String(route.query.processInstanceId || '')
    if (targetTaskId) {
      selectedTask.value = tasks.value.find((task) => String(task.id) === targetTaskId) || selectedTask.value
    } else if (targetInstanceId) {
      selectedTask.value = tasks.value.find((task) => String(task.processInstanceId) === targetInstanceId) || selectedTask.value
    }
    if (selectedTask.value) {
      selectedTask.value = tasks.value.find((task) => task.id === selectedTask.value.id) || null
    }
  } catch (error) {
    tasks.value = []
    errorMessage.value = error.normalizedMessage || error.message || '待办任务加载失败'
  }
}

async function completeSelectedTask() {
  if (!selectedTask.value) {
    return
  }
  resetMessages()
  const validationErrors = validateDecisionBySchema(selectedTask.value.formKey, selectedOutcome.value, decisionForm.value)
  if (validationErrors.length) {
    errorMessage.value = validationErrors[0]
    return
  }
  loading.value = true
  try {
    await http.post('/flowlong/task/complete', buildTaskPayload(selectedTask.value, selectedOutcome.value))
    statusMessage.value = `任务处理成功，已按“${selectedTask.value.formLabel || selectedTaskFormConfig.value.title}”提交流程结果`
    const handledInstanceId = selectedTask.value.processInstanceId
    await fetchTasks()
    if (canOpenMonitoring && handledInstanceId) {
      router.push({
        path: '/monitoring',
        query: {
          processInstanceId: handledInstanceId,
          openDetail: '1',
          tab: 'history',
          source: 'tasks'
        }
      })
      return
    }
    closeTask()
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    currentUser.value = currentUser.value || (await http.get('/auth/current')).data
    await ensureRemoteFormRegistryLoaded().catch(() => {})
    await fetchTasks()
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '任务中心初始化失败'
  }
})
</script>

<style scoped>
.task-form-badge-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.task-readonly-form {
  margin-bottom: 18px;
}

.task-link-row {
  margin-bottom: 14px;
}

.schema-form-grid {
  margin-bottom: 4px;
}

.full-width-field {
  grid-column: 1 / -1;
}

.field-help {
  color: #8a7152;
  font-size: 12px;
  line-height: 1.5;
}
</style>
