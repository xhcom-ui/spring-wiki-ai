<template>
  <div class="page-grid">
    <section class="stats-grid">
      <article class="stat-card">
        <span class="muted-text">我的申请</span>
        <strong>{{ leaves.length }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">审批中</span>
        <strong>{{ runningLeaveCount }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">已通过</span>
        <strong>{{ approvedLeaveCount }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">已驳回</span>
        <strong>{{ rejectedLeaveCount }}</strong>
      </article>
    </section>

    <section class="content-grid two-column">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">{{ startPageLabel }}</p>
            <h3>{{ startFormLabel }}</h3>
          </div>
          <span class="muted-text">{{ launchComponentLabel }}</span>
        </div>

        <div class="management-meta">
          <span class="meta-pill">表单 Key {{ startFormKey || '-' }}</span>
          <span class="meta-pill">表单标签 {{ startFormLabel }}</span>
          <span class="meta-pill">申请人 {{ currentUser?.nickname || currentUser?.username || '-' }}</span>
          <span class="meta-pill">设计模式 {{ runtimeConfig?.designerType === 'CUSTOM' ? '自定义编排' : 'BPMN' }}</span>
          <span class="meta-pill">审批节点 {{ approvalNodes.length }}</span>
        </div>

        <form class="form-stack" @submit.prevent="submitLeave">
          <section class="launch-form-section">
            <div class="panel-head compact-panel-head">
              <div>
                <p class="eyebrow">Launch Form</p>
                <h3>{{ runtimeConfig?.launchForm?.formLabel || '发起表单' }}</h3>
              </div>
              <span class="status-pill">{{ runtimeConfig?.launchForm?.componentKey || '目录驱动' }}</span>
            </div>
            <SchemaDrivenTaskForm :schema="launchFormSchema" :form-data="form" :errors="schemaErrors" />
          </section>

          <section v-if="inputApprovalNodes.length" class="launch-form-section">
            <div class="panel-head compact-panel-head">
              <div>
                <p class="eyebrow">Runtime Assignment</p>
                <h3>运行时审批人</h3>
              </div>
            </div>

            <div class="form-grid">
              <template v-for="slot in inputApprovalNodes" :key="slot.nodeId">
                <label>
                  <span>{{ slot.nodeName }}</span>
                  <select
                    v-if="!slot.multiple"
                    v-model="runtimeAssignments[slot.variableKey]"
                  >
                    <option value="">请选择</option>
                    <option v-for="user in approvers" :key="user.id" :value="user.username">
                      {{ user.nickname || user.username }}
                    </option>
                  </select>
                  <select
                    v-else
                    v-model="runtimeAssignments[slot.variableKey]"
                    multiple
                  >
                    <option v-for="user in approvers" :key="user.id" :value="user.username">
                      {{ user.nickname || user.username }}
                    </option>
                  </select>
                  <small class="field-hint">
                    {{ slot.pageLabel || slot.formLabel || slot.assignmentExpression || '运行时将注入到流程变量中' }}
                  </small>
                </label>
              </template>
            </div>
          </section>

          <div class="action-row">
            <button type="submit" class="primary-btn" :disabled="loading">{{ loading ? '提交中...' : '发起流程' }}</button>
            <button type="button" class="secondary-btn" @click="resetForm">重置</button>
          </div>

          <p v-if="statusMessage" class="feedback success">{{ statusMessage }}</p>
          <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>
        </form>
      </article>

      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Submission Guide</p>
            <h3>发起页与审批链路</h3>
          </div>
        </div>

        <div class="stack-list">
          <div class="list-item rich-list-item">
            <div>
              <strong>设计器入口标签</strong>
              <p>当前流程开始节点展示为“{{ startPageLabel }} / {{ startFormLabel }}”，已经和流程设计器配置保持一致。</p>
            </div>
            <span class="status-pill success-pill">已联动</span>
          </div>
          <div
            v-for="slot in approvalNodes"
            :key="slot.nodeId"
            class="list-item rich-list-item"
          >
            <div>
              <strong>{{ slot.nodeName }}</strong>
              <p>{{ slot.summary || slot.assignmentExpression || '审批节点' }}</p>
            </div>
            <span class="status-pill">
              {{ slot.pageLabel || slot.formLabel || (slot.requiresInput ? '待选择审批人' : '静态配置') }}
            </span>
          </div>
          <div class="list-item rich-list-item">
            <div>
              <strong>时间范围</strong>
              <p>建议先填写完整的开始和结束时间，再核对请假天数和审批人配置是否一致。</p>
            </div>
            <span class="status-pill">{{ form.startDate && form.endDate ? '已填写' : '未完成' }}</span>
          </div>
          <div class="action-row">
            <button v-if="canOpenDesigner" type="button" class="secondary-btn" @click="router.push('/designer')">查看流程设计</button>
            <button v-if="canOpenTaskCenter" type="button" class="secondary-btn" @click="router.push('/tasks')">查看我的待办</button>
            <button v-if="canOpenMonitoring" type="button" class="ghost-btn" @click="openMonitoringCenter">查看流程监控</button>
          </div>
        </div>
      </article>
    </section>

    <section class="panel-card">
      <div class="panel-head">
        <div>
          <p class="eyebrow">My Requests</p>
          <h3>我的请假记录</h3>
        </div>
        <div class="management-meta">
          <span class="meta-pill">筛选后 {{ filteredLeaves.length }} 条</span>
        </div>
      </div>

      <div class="management-toolbar">
        <div class="management-filters">
          <label class="filter-grow">
            <span>搜索记录</span>
            <input
              v-model.trim="keyword"
              type="text"
              placeholder="搜索状态、流程实例、审批人或原因"
            />
          </label>
          <label class="compact-field">
            <span>状态</span>
            <select v-model="statusFilter">
              <option value="">全部状态</option>
              <option value="APPROVED">已通过</option>
              <option value="REJECTED">已驳回</option>
              <option value="RUNNING">审批中</option>
            </select>
          </label>
        </div>

        <div class="management-actions">
          <button type="button" class="secondary-btn" @click="resetFilters">重置</button>
          <button type="button" class="ghost-btn" @click="fetchLeaves">刷新</button>
        </div>
      </div>

      <table class="simple-table">
        <thead>
          <tr>
            <th>申请人</th>
            <th>天数</th>
            <th>状态</th>
            <th>已走过节点</th>
            <th>当前节点</th>
            <th>部门经理</th>
            <th>总经理</th>
            <th>流程实例</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="leave in filteredLeaves" :key="leave.id">
            <td>{{ leave.applicant }}</td>
            <td>{{ leave.days }}</td>
            <td>
              <span :class="['status-pill', resolveLeaveStatusClass(leave.status)]">
                {{ leave.status }}
              </span>
            </td>
            <td>
              <div class="leave-tag-stack">
                <span v-for="label in leave.visitedNodeLabels || []" :key="label" class="status-pill">
                  {{ label }}
                </span>
                <span v-if="!(leave.visitedNodeLabels || []).length">-</span>
              </div>
            </td>
            <td>
              <div class="leave-tag-stack">
                <span v-for="label in leave.currentNodeLabels || []" :key="label" class="status-pill warning-pill">
                  {{ label }}
                </span>
                <span v-if="!(leave.currentNodeLabels || []).length">-</span>
              </div>
            </td>
            <td>{{ leave.deptManager || '-' }}</td>
            <td>{{ leave.generalManager || '-' }}</td>
            <td>{{ leave.processInstanceId }}</td>
            <td>{{ formatDate(leave.createdAt) }}</td>
            <td>
              <div class="inline-actions">
                <button
                  v-if="canOpenMonitoring && leave.processInstanceId"
                  type="button"
                  class="secondary-btn mini-btn"
                  @click="openMonitoringByInstance(leave.processInstanceId)"
                >
                  监控详情
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!filteredLeaves.length">
            <td colspan="10" class="empty-row">
              <div class="table-empty-state">
                <strong>没有匹配的请假记录</strong>
                <p>可以重置筛选条件，或者直接发起新的请假申请。</p>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { canAccessPath, getCurrentUser, http } from '../api/http'
import SchemaDrivenTaskForm from '../components/task-forms/SchemaDrivenTaskForm.vue'
import { applySchemaDefaults, validateSchemaForm } from '../utils/schemaForm'

const router = useRouter()
const loading = ref(false)
const leaves = ref([])
const approvers = ref([])
const runtimeConfig = ref(null)
const statusMessage = ref('')
const errorMessage = ref('')
const schemaErrors = reactive({})
const keyword = ref('')
const statusFilter = ref('')
const currentUser = ref(getCurrentUser())
const runtimeAssignments = reactive({})

const form = reactive({
  applicant: currentUser.value?.username || '',
  deptManager: '',
  generalManager: '',
  days: 1,
  reason: '',
  startDate: '',
  endDate: ''
})

const approvalNodes = computed(() => Array.isArray(runtimeConfig.value?.approvalNodes) ? runtimeConfig.value.approvalNodes : [])
const inputApprovalNodes = computed(() => approvalNodes.value.filter((item) => item.requiresInput))
const launchFormSchema = computed(() => Array.isArray(runtimeConfig.value?.launchForm?.fieldSchema) ? runtimeConfig.value.launchForm.fieldSchema : [])
const startFormKey = computed(() => runtimeConfig.value?.launchForm?.formKey || 'leave-form')
const startPageLabel = computed(() => runtimeConfig.value?.launchForm?.pageLabel || '请假发起页')
const startFormLabel = computed(() => runtimeConfig.value?.launchForm?.formLabel || '请假申请单')
const launchComponentLabel = computed(() => runtimeConfig.value?.launchForm?.componentKey || '提交后会自动进入工作流审批')
const coreLeaveFields = new Set(['applicant', 'deptManager', 'generalManager', 'days', 'reason', 'startDate', 'endDate'])
const canOpenMonitoring = canAccessPath('/monitoring', 'monitoring:view')
const canOpenTaskCenter = canAccessPath('/tasks', 'task:approve')
const canOpenDesigner = canAccessPath('/designer', 'process:design')

const filteredLeaves = computed(() => {
  const needle = keyword.value.trim().toLowerCase()
  return leaves.value.filter((leave) => {
    const statusMatched =
      !statusFilter.value
      || leave.status === statusFilter.value
    if (!statusMatched) {
      return false
    }
    if (!needle) {
      return true
    }
    return [
      leave.status,
      leave.processInstanceId,
      leave.reason,
      leave.deptManager,
      leave.generalManager
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(needle))
  })
})

const runningLeaveCount = computed(() => leaves.value.filter((item) => item.status === 'RUNNING').length)
const approvedLeaveCount = computed(() => leaves.value.filter((item) => item.status === 'APPROVED').length)
const rejectedLeaveCount = computed(() => leaves.value.filter((item) => item.status === 'REJECTED').length)

function resetMessages() {
  statusMessage.value = ''
  errorMessage.value = ''
  Object.keys(schemaErrors).forEach((key) => delete schemaErrors[key])
}

function initializeRuntimeAssignments() {
  Object.keys(runtimeAssignments).forEach((key) => delete runtimeAssignments[key])
  for (const slot of inputApprovalNodes.value) {
    runtimeAssignments[slot.variableKey] = slot.multiple ? [] : ''
  }
}

function resetForm() {
  form.applicant = currentUser.value?.username || ''
  form.deptManager = ''
  form.generalManager = ''
  form.days = 1
  form.reason = ''
  form.startDate = ''
  form.endDate = ''
  initializeRuntimeAssignments()
  applyLaunchSchemaDefaults()
}

function resetFilters() {
  keyword.value = ''
  statusFilter.value = ''
}

function formatDate(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

function resolveLeaveStatusClass(status) {
  if (status === 'APPROVED') {
    return 'success-pill'
  }
  if (status === 'REJECTED') {
    return 'muted-pill'
  }
  return 'warning-pill'
}

function openMonitoringCenter() {
  router.push('/monitoring')
}

function openMonitoringByInstance(processInstanceId) {
  if (!processInstanceId) {
    return
  }
  router.push({
    path: '/monitoring',
    query: {
      processInstanceId,
      openDetail: '1',
      source: 'leave'
    }
  })
}

async function fetchApprovers() {
  const { data } = await http.get('/users/lookup')
  approvers.value = Array.isArray(data)
    ? data.filter((item) => item.username !== currentUser.value?.username && item.status === 1)
    : []
}

async function fetchRuntimeConfig() {
  const { data } = await http.get('/flowable/runtime-config/leave-process')
  runtimeConfig.value = data || null
  applyLaunchSchemaDefaults()
  initializeRuntimeAssignments()
}

async function fetchLeaves() {
  if (!currentUser.value?.username) {
    leaves.value = []
    return
  }
  const { data } = await http.get(`/flowable/leaves/applicant/${currentUser.value.username}`)
  leaves.value = data || []
}

function buildProcessVariables() {
  const variables = {}
  for (const field of launchFormSchema.value) {
    if (!field?.field || coreLeaveFields.has(field.field)) {
      continue
    }
    variables[field.field] = form[field.field]
  }
  for (const slot of inputApprovalNodes.value) {
    const value = runtimeAssignments[slot.variableKey]
    if (slot.multiple) {
      variables[slot.variableKey] = Array.isArray(value) ? value.join(',') : ''
    } else {
      variables[slot.variableKey] = value || ''
    }
  }
  form.deptManager = typeof variables.deptManager === 'string' ? variables.deptManager : form.deptManager
  form.generalManager = typeof variables.generalManager === 'string' ? variables.generalManager : form.generalManager
  return variables
}

function applyLaunchSchemaDefaults() {
  if (!launchFormSchema.value.length) {
    return
  }
  applySchemaDefaults(launchFormSchema.value, form, {
    readonlySource: { applicant: currentUser.value?.username || form.applicant || '' }
  })
}

function validateLaunchForm() {
  const validation = validateSchemaForm(launchFormSchema.value, form)
  if (!validation.valid) {
    Object.assign(schemaErrors, validation.errors)
    errorMessage.value = Object.values(validation.errors)[0]
    return false
  }
  return true
}

async function submitLeave() {
  resetMessages()
  if (!validateLaunchForm()) {
    return
  }
  const processVariables = buildProcessVariables()
  for (const slot of inputApprovalNodes.value) {
    const value = processVariables[slot.variableKey]
    if (!value || (Array.isArray(value) && !value.length)) {
      errorMessage.value = `请先选择 ${slot.nodeName}`
      return
    }
  }
  loading.value = true
  try {
    await http.post('/flowable/leave/start', {
      ...form,
      processVariables
    })
    statusMessage.value = '请假流程已发起，申请已进入工作流审批。'
    resetForm()
    await fetchLeaves()
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  currentUser.value = currentUser.value || (await http.get('/auth/current')).data
  form.applicant = currentUser.value?.username || ''
  await Promise.all([fetchApprovers(), fetchRuntimeConfig(), fetchLeaves()])
})
</script>

<style scoped>
.field-hint {
  color: #7a664c;
  font-size: 12px;
}

.launch-form-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.leave-tag-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
