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
            <h3>{{ leaveFormConfig.title }}</h3>
          </div>
          <span class="muted-text">{{ leaveFormConfig.description }}</span>
        </div>

        <div class="management-meta">
          <span class="meta-pill">表单 Key {{ startFormKey || '-' }}</span>
          <span class="meta-pill">表单标签 {{ startFormLabel }}</span>
          <span class="meta-pill">申请人 {{ currentUser?.nickname || currentUser?.username || '-' }}</span>
          <span class="meta-pill">可选审批人 {{ approvers.length }}</span>
        </div>

        <form class="form-stack" @submit.prevent="submitLeave">
          <div class="form-grid">
            <label
              v-for="field in leaveFormFields"
              :key="field.key"
              :class="{ 'full-width-field': resolveFieldSpan(field) > 1 || field.type === 'textarea' }"
            >
              <span>{{ field.label }}</span>
              <input
                v-if="field.type === 'text' || field.type === 'number' || field.type === 'datetime-local'"
                v-model="form[field.key]"
                v-bind="resolveInputProps(field)"
                :type="field.type"
                :readonly="isReadonlyField(field)"
                :disabled="isReadonlyField(field)"
                :min="field.min"
                :max="field.max"
                :placeholder="field.placeholder || ''"
              />
              <select
                v-else-if="field.type === 'select'"
                v-model="form[field.key]"
                v-bind="resolveInputProps(field)"
                :disabled="isReadonlyField(field)"
              >
                <option value="">请选择</option>
                <option
                  v-for="option in resolveFieldOptions(field)"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </option>
              </select>
              <textarea
                v-else-if="field.type === 'textarea'"
                v-model.trim="form[field.key]"
                v-bind="resolveInputProps(field)"
                :rows="field.rows || 4"
                :placeholder="field.placeholder || ''"
                :readonly="isReadonlyField(field)"
              ></textarea>
              <small v-if="field.help" class="field-help">{{ field.help }}</small>
            </label>
          </div>

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
            <h3>提交流程前检查</h3>
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
          <div class="list-item rich-list-item">
            <div>
              <strong>审批链路</strong>
              <p>请确认部门经理和总经理都已设置，否则流程无法继续向下推进。</p>
            </div>
            <span class="status-pill">{{ form.deptManager && form.generalManager ? '完整' : '待补充' }}</span>
          </div>
          <div class="list-item rich-list-item">
            <div>
              <strong>时间范围</strong>
              <p>建议先填写完整的开始和结束时间，再核对请假天数是否一致。</p>
            </div>
            <span class="status-pill">{{ form.startDate && form.endDate ? '已填写' : '未完成' }}</span>
          </div>
          <div class="action-row">
            <button v-if="canOpenDesigner" type="button" class="secondary-btn" @click="router.push('/designer')">查看流程设计</button>
            <button v-if="canOpenTaskCenter" type="button" class="secondary-btn" @click="router.push('/tasks')">查看我的待办</button>
            <button v-if="canOpenMonitoring" type="button" class="ghost-btn" @click="openMonitoringWithApplicant">查看流程监控</button>
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
            <input v-model.trim="keyword" type="text" placeholder="搜索状态、流程实例、审批人或原因" />
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
            <th>实际走过节点</th>
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
            <td>{{ formatPassedNodeLabels(leave.passedNodeLabelsJson) }}</td>
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
            <td colspan="9" class="empty-row">
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { canAccessPath, getCurrentUser, http } from '../api/http'
import {
  ensureFormOptionSourcesLoaded,
  resolveFieldOptions as resolveSchemaFieldOptions,
  resolveFieldProps,
  resolveFieldReadonly,
  resolveFieldSpan,
  resolveFieldVisible,
  resolveFormConfig,
  ensureRemoteFormRegistryLoaded,
  validateFormBySchema
} from '../utils/businessForms'

const LEAVE_PROCESS_KEY = 'leave-process'
const router = useRouter()
const route = useRoute()

const loading = ref(false)
const leaves = ref([])
const approvers = ref([])
const statusMessage = ref('')
const errorMessage = ref('')
const keyword = ref('')
const statusFilter = ref('')
const currentUser = ref(getCurrentUser())
const startNodeConfig = ref(null)
const remoteOptionSources = ref({})

const form = reactive({
  applicant: currentUser.value?.username || '',
  deptManager: '',
  generalManager: '',
  days: 1,
  reason: '',
  startDate: '',
  endDate: ''
})

const leaveFormConfig = computed(() => resolveFormConfig(startNodeConfig.value?.formKey || 'leave-form'))
const leaveFormFields = computed(() =>
  (leaveFormConfig.value.fields || []).filter((field) =>
    resolveFieldVisible(field, form, {
      currentUser: currentUser.value
    })
  )
)
const startFormKey = computed(() => startNodeConfig.value?.formKey || 'leave-form')
const startPageLabel = computed(() => startNodeConfig.value?.pageLabel || '请假发起页')
const startFormLabel = computed(() => startNodeConfig.value?.formLabel || leaveFormConfig.value.title)
const canOpenMonitoring = canAccessPath('/monitoring', 'monitoring:view')
const canOpenTaskCenter = canAccessPath('/tasks', 'task:approve')
const canOpenDesigner = canAccessPath('/designer', 'process:design')

const filteredLeaves = computed(() => {
  const needle = keyword.value.trim().toLowerCase()
  return leaves.value.filter((leave) => {
    const statusMatched =
      !statusFilter.value
      || (statusFilter.value === 'RUNNING'
        ? !['APPROVED', 'REJECTED'].includes(leave.status)
        : leave.status === statusFilter.value)
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

const runningLeaveCount = computed(() => leaves.value.filter((item) => !['APPROVED', 'REJECTED'].includes(item.status)).length)
const approvedLeaveCount = computed(() => leaves.value.filter((item) => item.status === 'APPROVED').length)
const rejectedLeaveCount = computed(() => leaves.value.filter((item) => item.status === 'REJECTED').length)

function resetMessages() {
  statusMessage.value = ''
  errorMessage.value = ''
}

function resolveFieldOptions(field) {
  return resolveSchemaFieldOptions(field, {
    approvers: approvers.value,
    '/users/lookup': approvers.value,
    ...remoteOptionSources.value
  })
}

function isReadonlyField(field) {
  return resolveFieldReadonly(field, form, {
    currentUser: currentUser.value
  })
}

function resolveInputProps(field) {
  return resolveFieldProps(field, form, {
    currentUser: currentUser.value
  })
}

function resetForm() {
  form.applicant = currentUser.value?.username || ''
  form.deptManager = ''
  form.generalManager = ''
  form.days = 1
  form.reason = ''
  form.startDate = ''
  form.endDate = ''
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

function formatPassedNodeLabels(rawValue) {
  if (!rawValue) {
    return '-'
  }
  try {
    const labels = JSON.parse(rawValue)
    return Array.isArray(labels) && labels.length ? labels.join(' -> ') : '-'
  } catch (error) {
    return '-'
  }
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

async function fetchApprovers() {
  try {
    const { data } = await http.get('/users/lookup')
    approvers.value = Array.isArray(data)
      ? data.filter((item) => item.username !== currentUser.value?.username && item.status === 1)
      : []
  } catch (error) {
    approvers.value = []
    errorMessage.value = error.normalizedMessage || error.message || '审批人列表加载失败'
  }
}

async function fetchLeaves() {
  try {
    if (!currentUser.value?.username) {
      leaves.value = []
      return
    }
    const { data } = await http.get(`/flowlong/leaves/applicant/${currentUser.value.username}`)
    leaves.value = data || []
  } catch (error) {
    leaves.value = []
    errorMessage.value = error.normalizedMessage || error.message || '请假记录加载失败'
  }
}

async function fetchBusinessConfig() {
  try {
    const { data } = await http.get(`/flowlong/process/${LEAVE_PROCESS_KEY}/business-config`)
    startNodeConfig.value = data?.startNode || null
  } catch (error) {
    startNodeConfig.value = null
  }
}

async function refreshFormOptionSources() {
  remoteOptionSources.value = await ensureFormOptionSourcesLoaded(startFormKey.value).catch(() => ({}))
}

async function submitLeave() {
  resetMessages()
  const validationErrors = validateFormBySchema(startFormKey.value, form)
  if (validationErrors.length) {
    errorMessage.value = validationErrors[0]
    return
  }
  loading.value = true
  try {
    const { data } = await http.post('/flowlong/leave/start', { ...form })
    statusMessage.value = `请假流程已发起，入口表单“${startFormLabel.value}”已提交到工作流。`
    resetForm()
    await fetchLeaves()
    if (canOpenMonitoring && data?.processInstanceId) {
      statusMessage.value = `请假流程已发起，实例 ${data.processInstanceId} 已进入工作流。`
    }
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

function openMonitoringByInstance(processInstanceId) {
  router.push({
    path: '/monitoring',
    query: {
      processInstanceId,
      openDetail: '1',
      source: 'leave'
    }
  })
}

function openMonitoringWithApplicant() {
  router.push({
    path: '/monitoring',
    query: {
      applicant: currentUser.value?.username || '',
      source: 'leave'
    }
  })
}

onMounted(async () => {
  try {
    currentUser.value = currentUser.value || (await http.get('/auth/current')).data
    form.applicant = currentUser.value?.username || ''
    await ensureRemoteFormRegistryLoaded().catch(() => {})
    await Promise.all([fetchApprovers(), fetchLeaves(), fetchBusinessConfig()])
    await refreshFormOptionSources()
    keyword.value = String(route.query.processInstanceId || route.query.applicant || '')
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '发起流程页初始化失败'
  }
})

watch(startFormKey, async () => {
  try {
    await refreshFormOptionSources()
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '表单选项刷新失败'
  }
})
</script>

<style scoped>
.full-width-field {
  grid-column: 1 / -1;
}

.field-help {
  color: #8a7152;
  font-size: 12px;
  line-height: 1.5;
}
</style>
