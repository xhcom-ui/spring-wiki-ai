<template>
  <div class="page-grid">
    <section class="stats-grid">
      <article class="stat-card">
        <span class="muted-text">待办总数</span>
        <strong>{{ tasks.length }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">我的任务</span>
        <strong>{{ assignedTaskCount }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">候选待办</span>
        <strong>{{ claimableTaskCount }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">自定义编排</span>
        <strong>{{ customDesignerTaskCount }}</strong>
      </article>
    </section>

    <section class="content-grid task-workbench-grid">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Task Inbox</p>
            <h3>我的待办任务</h3>
          </div>
          <div class="management-meta">
            <span class="meta-pill">待办 {{ tasks.length }}</span>
            <span class="meta-pill">已分配 {{ assignedTaskCount }}</span>
            <span class="meta-pill">待签收 {{ claimableTaskCount }}</span>
          </div>
        </div>

        <div class="action-row">
          <label class="flex-field">
            <span>搜索任务</span>
            <input v-model.trim="keyword" type="text" placeholder="按任务名、申请人、业务键、表单标签过滤" />
          </label>
          <label class="compact-field">
            <span>任务来源</span>
            <select v-model="taskType">
              <option value="all">全部</option>
              <option value="assigned">我的任务</option>
              <option value="claimable">候选待办</option>
              <option value="custom">自定义编排</option>
              <option value="bpmn">BPMN</option>
            </select>
          </label>
        </div>

        <table class="simple-table">
          <thead>
            <tr>
              <th>任务名称</th>
              <th>申请人</th>
              <th>业务键</th>
              <th>表单/页面</th>
              <th>处理人</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="task in filteredTasks" :key="task.id">
              <td>
                <div class="task-primary-cell">
                  <strong>{{ task.name }}</strong>
                  <small>{{ task.taskDefinitionKey || '-' }} · {{ task.processName || task.processDefinitionKey || '-' }}</small>
                </div>
              </td>
              <td>{{ task.applicant || '-' }}</td>
              <td>{{ task.businessKey || task.processInstanceId }}</td>
              <td>{{ [task.pageLabel, task.formLabel].filter(Boolean).join(' / ') || '-' }}</td>
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
              <td colspan="7" class="empty-row">当前没有符合条件的待办任务</td>
            </tr>
          </tbody>
        </table>
      </article>

      <aside class="panel-card approval-panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Approval Console</p>
            <h3>{{ selectedTask ? selectedTask.name : '选择一个任务开始处理' }}</h3>
          </div>
        </div>

        <template v-if="selectedTask">
          <section class="approval-summary">
            <div class="detail-item">
              <span class="detail-key">节点标签</span>
              <strong class="detail-value">{{ selectedTask.nodeLabel || selectedTask.name }}</strong>
            </div>
            <div class="detail-item">
              <span class="detail-key">申请人</span>
              <strong class="detail-value">{{ selectedTask.applicant || '-' }}</strong>
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
            <span class="meta-pill">{{ selectedTask.formLabel || selectedTaskFormRegistration.label }}</span>
            <span class="meta-pill">{{ selectedTask.assignmentExpression || '静态分配' }}</span>
          </div>

          <div class="action-row task-link-row">
            <button v-if="canOpenMonitoring" type="button" class="secondary-btn" @click="openMonitoring(selectedTask)">查看流程监控</button>
          </div>

          <section class="panel-card task-form-card">
            <div class="panel-head compact-panel-head">
              <div>
                <p class="eyebrow">Business Form</p>
                <h3>{{ selectedTask.formLabel || selectedTask.pageLabel || '业务表单' }}</h3>
              </div>
              <span class="status-pill">{{ selectedTaskFormRegistration.label }}</span>
            </div>

            <div v-if="selectedTaskFormSummary.length" class="detail-grid compact-detail-grid">
              <article v-for="item in selectedTaskFormSummary" :key="item.label" class="detail-item">
                <span class="detail-key">{{ item.label }}</span>
                <strong class="detail-value">
                  {{ ['开始时间', '结束时间'].includes(item.label) ? formatDate(item.value) : item.value }}
                </strong>
              </article>
            </div>

            <ApprovalFormRenderer :task="selectedTask" :form-data="businessForm" :errors="schemaErrors" />
          </section>

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
            <label>
              <span>审批意见</span>
              <textarea v-model.trim="comment" rows="6" placeholder="请输入审批意见"></textarea>
            </label>
            <div v-if="selectedTask.comments?.length" class="stack-list compact-comment-list">
              <div v-for="item in selectedTask.comments" :key="item.id || `${item.time}-${item.message}`" class="list-item">
                <strong>{{ item.userId || '系统' }}</strong>
                <p>{{ item.message }}</p>
              </div>
            </div>
            <div class="action-row">
              <button type="button" class="primary-btn" :disabled="loading" @click="completeSelectedTask">
                {{ loading ? '提交中...' : `${selectedTask.claimable ? '自动签收并' : ''}确认${selectedOutcome === 'approved' ? '同意' : '驳回'}` }}
              </button>
              <button type="button" class="secondary-btn" @click="closeTask">取消</button>
            </div>
            <p v-if="statusMessage" class="feedback success">{{ statusMessage }}</p>
            <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>
          </div>
        </template>

        <div v-else class="empty-state task-empty-panel">
          从左侧任务列表中选择一条待办，右侧会显示审批摘要、表单标签和处理入口。
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { canAccessPath, getCurrentUser, http } from '../api/http'
import ApprovalFormRenderer from '../components/task-forms/ApprovalFormRenderer.vue'
import { resolveTaskFormRegistration, resolveTaskFormSummary } from '../components/task-forms/formRegistry'
import { applySchemaDefaults as fillSchemaDefaults, validateSchemaForm } from '../utils/schemaForm'

const router = useRouter()
const currentUser = ref(getCurrentUser())
const tasks = ref([])
const selectedTask = ref(null)
const selectedOutcome = ref('approved')
const comment = ref('')
const keyword = ref('')
const taskType = ref('all')
const loading = ref(false)
const statusMessage = ref('')
const errorMessage = ref('')
const schemaErrors = reactive({})
const canOpenMonitoring = canAccessPath('/monitoring', 'monitoring:view')
const businessForm = reactive({
  managerAdvice: '',
  priority: 'normal',
  finalDecisionNote: '',
  allowResubmit: false
})

const filteredTasks = computed(() => {
  const needle = keyword.value.trim().toLowerCase()
  return tasks.value.filter((task) => {
    const typeMatched =
      taskType.value === 'all'
        ? true
        : taskType.value === 'assigned'
          ? !task.claimable
          : taskType.value === 'claimable'
            ? Boolean(task.claimable)
            : taskType.value === 'custom'
              ? task.designerType === 'CUSTOM'
              : task.designerType !== 'CUSTOM'

    if (!typeMatched) {
      return false
    }
    if (!needle) {
      return true
    }
    return [
      task.name,
      task.applicant,
      task.businessKey,
      task.processInstanceId,
      task.assignee,
      task.formLabel,
      task.pageLabel,
      task.taskDefinitionKey
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(needle))
  })
})

const assignedTaskCount = computed(() => tasks.value.filter((task) => !task.claimable).length)
const claimableTaskCount = computed(() => tasks.value.filter((task) => task.claimable).length)
const customDesignerTaskCount = computed(() => tasks.value.filter((task) => task.designerType === 'CUSTOM').length)
const selectedTaskFormRegistration = computed(() => resolveTaskFormRegistration(selectedTask.value))
const selectedTaskFormSummary = computed(() => resolveTaskFormSummary(selectedTask.value))

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
  comment.value = ''
}

function closeTask() {
  selectedTask.value = null
  comment.value = ''
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

function resetBusinessForm() {
  Object.keys(schemaErrors).forEach((key) => delete schemaErrors[key])
  Object.keys(businessForm).forEach((key) => delete businessForm[key])
  businessForm.managerAdvice = ''
  businessForm.priority = 'normal'
  businessForm.finalDecisionNote = ''
  businessForm.allowResubmit = false
}

function applySchemaDefaults(task) {
  if (!Array.isArray(task?.fieldSchema) || !task.fieldSchema.length) {
    return
  }
  Object.keys(businessForm).forEach((key) => delete businessForm[key])
  fillSchemaDefaults(task.fieldSchema, businessForm, { readonlySource: task, preserveExisting: false })
}

function buildTaskVariables() {
  if (Array.isArray(selectedTask.value?.fieldSchema) && selectedTask.value.fieldSchema.length) {
    const variables = {}
    for (const field of selectedTask.value.fieldSchema) {
      if (!field?.field || field.readonly) {
        continue
      }
      variables[field.field] = businessForm[field.field]
    }
    return variables
  }
  return {
    managerAdvice: businessForm.managerAdvice,
    priority: businessForm.priority,
    finalDecisionNote: businessForm.finalDecisionNote,
    allowResubmit: businessForm.allowResubmit
  }
}

async function fetchTasks() {
  if (!currentUser.value?.username) {
    tasks.value = []
    return
  }
  const { data } = await http.get(`/flowable/tasks/${currentUser.value.username}`)
  tasks.value = data || []
  if (selectedTask.value) {
    selectedTask.value = tasks.value.find((task) => task.id === selectedTask.value.id) || null
  }
}

async function completeSelectedTask() {
  if (!selectedTask.value) {
    return
  }
  resetMessages()
  Object.keys(schemaErrors).forEach((key) => delete schemaErrors[key])
  if (Array.isArray(selectedTask.value?.fieldSchema) && selectedTask.value.fieldSchema.length) {
    const validation = validateSchemaForm(selectedTask.value.fieldSchema, businessForm)
    if (!validation.valid) {
      Object.assign(schemaErrors, validation.errors)
      errorMessage.value = Object.values(validation.errors)[0]
      return
    }
  }
  loading.value = true
  try {
    const { data } = await http.post('/flowable/task/complete', {
      taskId: selectedTask.value.id,
      processInstanceId: selectedTask.value.processInstanceId,
      outcome: selectedOutcome.value,
      comment: comment.value,
      variables: buildTaskVariables()
    })
    statusMessage.value = `任务处理成功${data?.status ? `，申请状态已更新为 ${data.status}` : ''}`
    await fetchTasks()
    closeTask()
    resetBusinessForm()
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  currentUser.value = currentUser.value || (await http.get('/auth/current')).data
  await fetchTasks()
})

watch(selectedTask, () => {
  resetBusinessForm()
  applySchemaDefaults(selectedTask.value)
})
</script>

<style scoped>
.task-primary-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.task-form-badge-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.task-link-row {
  margin-bottom: 14px;
}

.task-primary-cell small {
  color: #7a664c;
}

.task-form-card {
  padding: 18px;
}

.compact-comment-list {
  max-height: 180px;
  overflow: auto;
}
</style>
