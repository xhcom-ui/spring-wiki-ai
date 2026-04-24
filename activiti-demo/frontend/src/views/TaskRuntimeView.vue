<template>
  <div class="page-grid">
    <section class="panel-card">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Business Entry</p>
          <h3>{{ pageTitle }}</h3>
        </div>
        <div class="inline-actions">
          <button type="button" class="secondary-btn mini-btn" @click="goBackToTasks">返回待办中心</button>
          <button v-if="task && mode === 'todo'" type="button" class="ghost-btn mini-btn" @click="refreshTask">刷新</button>
        </div>
      </div>
      <p class="muted-text">{{ pageSummary }}</p>
    </section>

    <p v-if="statusMessage" class="feedback success">{{ statusMessage }}</p>
    <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>

    <div v-if="loading" class="panel-card empty-state">任务加载中...</div>

    <div v-else-if="mode === 'done' && completionSnapshot" class="page-grid">
      <component
        :is="activeComponent"
        :task="completionSnapshot"
        :mode="mode"
        :descriptor="descriptor"
        :initial-comment="comment"
        :initial-outcome="outcome"
      />
    </div>

    <div v-else-if="task" class="page-grid">
      <component
        :is="activeComponent"
        :task="task"
        :mode="mode"
        :descriptor="descriptor"
        :submitting="submitting"
        :claiming="claiming"
        :initial-comment="comment"
        :initial-outcome="outcome"
        @submit="handleSubmit"
        @claim="claimTask"
        @update:comment="comment = $event"
        @update:outcome="outcome = $event"
      />
    </div>

    <div v-else-if="isPreviewMode" class="panel-card">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Route Preview</p>
          <h3>{{ descriptor.title }}</h3>
        </div>
        <span class="meta-pill">{{ mode === 'done' ? '完成页预览' : '待办页预览' }}</span>
      </div>
      <p class="muted-text">{{ descriptor.description }}</p>
      <div class="detail-grid compact-detail-grid">
        <div class="detail-item">
          <span class="detail-key">路由页标识</span>
          <strong class="detail-value">{{ route.params.pageKey || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span class="detail-key">预览模式</span>
          <strong class="detail-value">{{ mode === 'done' ? 'done' : 'todo' }}</strong>
        </div>
        <div class="detail-item">
          <span class="detail-key">组件映射</span>
          <strong class="detail-value">{{ descriptor.kind === 'leave' ? 'LeaveRuntimePage' : 'GenericRuntimePage' }}</strong>
        </div>
        <div class="detail-item">
          <span class="detail-key">连通性</span>
          <strong class="detail-value">前端路由已接通</strong>
        </div>
      </div>
    </div>

    <div v-else class="panel-card empty-state">
      没有找到可打开的任务数据。请从待办中心重新进入该业务页。
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import GenericRuntimePage from '../components/task-runtime/GenericRuntimePage.vue'
import LeaveRuntimePage from '../components/task-runtime/LeaveRuntimePage.vue'
import { http } from '../api/http'
import { ensureTaskPageCatalogLoaded } from '../runtime/taskPageCatalog'
import { buildRuntimeRoute, resolveRuntimeDescriptor } from '../runtime/taskRuntimeRegistry'

const route = useRoute()
const router = useRouter()

const task = ref(null)
const completionSnapshot = ref(null)
const loading = ref(false)
const submitting = ref(false)
const claiming = ref(false)
const comment = ref('')
const outcome = ref('approved')
const statusMessage = ref('')
const errorMessage = ref('')
const activeComponent = shallowRef(GenericRuntimePage)

const mode = computed(() => (route.query.mode === 'done' ? 'done' : 'todo'))
const taskId = computed(() => String(route.query.taskId || ''))
const isPreviewMode = computed(() => route.query.preview === '1')
const descriptor = computed(() => resolveRuntimeDescriptor(task.value || completionSnapshot.value, mode.value))
const pageTitle = computed(() => descriptor.value.title || route.params.pageKey || '业务办理页')
const pageSummary = computed(() =>
  isPreviewMode.value
    ? '当前处于路由预览模式，用于验证业务页目录项是否能命中实际前端页面。'
    : mode.value === 'done'
    ? '当前通过流程节点的 donePage/formKey 打开完成回执页。'
    : '当前通过流程节点的 todoPage/formKey 打开业务办理页。'
)

watch(
  descriptor,
  (value) => {
    activeComponent.value = value.kind === 'leave' ? LeaveRuntimePage : GenericRuntimePage
  },
  { immediate: true }
)

function resetMessages() {
  statusMessage.value = ''
  errorMessage.value = ''
}

function goBackToTasks() {
  router.push('/tasks')
}

function resolveCompletionSnapshot(currentTask, submitOutcome, submitComment) {
  completionSnapshot.value = {
    ...currentTask,
    canClaim: false,
    claimed: true,
    leaveStatus: submitOutcome === 'approved' ? 'APPROVED' : 'REJECTED',
    variables: {
      ...(currentTask?.variables || {}),
      comment: submitComment,
      leaveStatus: submitOutcome === 'approved' ? 'APPROVED' : 'REJECTED'
    }
  }
}

function buildTaskPayload(currentTask, submitOutcome, submitComment) {
  const variables = {
    comment: submitComment,
    leaveStatus: submitOutcome === 'approved' ? 'APPROVED' : 'REJECTED'
  }
  let status = submitOutcome === 'approved' ? 'APPROVED' : 'REJECTED'

  if (currentTask.name?.includes('部门经理')) {
    variables.deptApproval = submitOutcome
  } else if (currentTask.name?.includes('总经理')) {
    variables.generalApproval = submitOutcome
  } else if (descriptor.value.kind !== 'leave') {
    status = submitOutcome === 'approved' ? 'COMPLETED' : 'REJECTED'
  }

  return {
    taskId: currentTask.id,
    processInstanceId: currentTask.processInstanceId,
    status,
    variables
  }
}

async function refreshTask() {
  if (!taskId.value || mode.value === 'done') {
    return
  }
  resetMessages()
  loading.value = true
  try {
    const { data } = await http.get(`/activiti/task/${taskId.value}`)
    task.value = data
  } catch (error) {
    task.value = null
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

async function claimTask() {
  if (!task.value?.id) {
    return
  }
  resetMessages()
  claiming.value = true
  try {
    await http.post('/activiti/task/claim', { taskId: task.value.id })
    statusMessage.value = '任务已签收，可以继续在当前业务页处理。'
    await refreshTask()
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    claiming.value = false
  }
}

async function handleSubmit(payload) {
  if (!task.value?.id) {
    return
  }
  resetMessages()
  submitting.value = true
  try {
    await http.post('/activiti/task/complete', buildTaskPayload(task.value, payload.outcome, payload.comment))
    resolveCompletionSnapshot(task.value, payload.outcome, payload.comment)
    statusMessage.value = '任务处理成功，已切换到完成页回执。'
    router.replace(buildRuntimeRoute(completionSnapshot.value, 'done'))
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    submitting.value = false
  }
}

async function syncRouteState() {
  comment.value = ''
  outcome.value = 'approved'
  task.value = null
  completionSnapshot.value = null
  if (mode.value === 'done') {
    completionSnapshot.value = {
      id: taskId.value,
      name: String(route.query.taskName || '任务已完成'),
      processInstanceId: String(route.query.processInstanceId || ''),
      processDefinitionKey: String(route.query.processDefinitionKey || ''),
      processDefinitionName: String(route.query.processDefinitionName || ''),
      taskDefinitionKey: String(route.query.taskDefinitionKey || ''),
      formKey: String(route.query.formKey || ''),
      todoPage: String(route.query.todoPage || ''),
      donePage: String(route.query.donePage || ''),
      leaveStatus: String(route.query.status || 'COMPLETED'),
      applicant: String(route.query.applicant || ''),
      variables: {
        comment: String(route.query.comment || ''),
        leaveStatus: String(route.query.status || 'COMPLETED'),
        days: String(route.query.days || ''),
        reason: String(route.query.reason || ''),
        startDate: String(route.query.startDate || ''),
        endDate: String(route.query.endDate || ''),
        deptManager: String(route.query.deptManager || ''),
        generalManager: String(route.query.generalManager || '')
      }
    }
    return
  }
  await refreshTask()
}

watch(
  () => route.fullPath,
  async () => {
    await syncRouteState()
  }
)

onMounted(async () => {
  await ensureTaskPageCatalogLoaded()
  await syncRouteState()
})
</script>
