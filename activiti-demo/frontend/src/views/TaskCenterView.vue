<template>
  <div class="page-grid">
    <section class="stats-grid">
      <article class="stat-card">
        <span class="muted-text">待办总数</span>
        <strong>{{ tasks.length }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">待签收</span>
        <strong>{{ claimableTaskCount }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">已签收到我</span>
        <strong>{{ assignedToMeCount }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">表单化任务</span>
        <strong>{{ formTaskCount }}</strong>
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
            <span class="meta-pill">待签收 {{ claimableTaskCount }}</span>
            <span class="meta-pill">已签收 {{ assignedToMeCount }}</span>
            <button type="button" class="ghost-btn mini-btn" @click="fetchTasks">刷新</button>
          </div>
        </div>

        <div class="action-row">
          <label class="flex-field">
            <span>搜索任务</span>
            <input v-model.trim="keyword" type="text" placeholder="按任务名、申请人、表单 Key、业务键过滤" />
          </label>
          <label class="compact-field">
            <span>领取状态</span>
            <select v-model="assignmentFilter">
              <option value="all">全部</option>
              <option value="claimable">待签收</option>
              <option value="assigned">已签收</option>
            </select>
          </label>
          <label class="compact-field">
            <span>表单类型</span>
            <select v-model="formFilter">
              <option value="all">全部</option>
              <option value="leave">请假表单</option>
              <option value="configured">已配置表单</option>
              <option value="generic">通用任务</option>
            </select>
          </label>
        </div>

        <p v-if="statusMessage" class="feedback success">{{ statusMessage }}</p>
        <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>

        <table class="simple-table">
          <thead>
            <tr>
              <th>任务名称</th>
              <th>申请人</th>
              <th>表单 Key</th>
              <th>分配方式</th>
              <th>处理人</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="task in filteredTasks" :key="task.id">
              <td>
                <strong>{{ task.name }}</strong>
                <p class="table-subline">{{ task.businessKey || task.processInstanceId }}</p>
              </td>
              <td>{{ task.applicant || '-' }}</td>
              <td>{{ task.formKey || '未配置' }}</td>
              <td>{{ renderAssignmentLabel(task) }}</td>
              <td>{{ task.assignee || '待签收' }}</td>
              <td>{{ formatDate(task.createTime) }}</td>
              <td>
                <div class="inline-actions">
                  <button type="button" class="secondary-btn mini-btn" @click="previewTask(task)">查看</button>
                  <button
                    v-if="canOpenRuntimePage(task)"
                    type="button"
                    class="ghost-btn mini-btn"
                    @click="openRuntimePage(task)"
                  >
                    进入待办页
                  </button>
                  <button
                    v-if="task.canClaim"
                    type="button"
                    class="primary-btn mini-btn"
                    :disabled="claiming"
                    @click="claimTask(task)"
                  >
                    {{ claiming && selectedTask?.id === task.id ? '签收中...' : '签收' }}
                  </button>
                  <template v-else>
                    <button type="button" class="primary-btn mini-btn" @click="openTask(task, 'approved')">同意</button>
                    <button type="button" class="danger-btn mini-btn" @click="openTask(task, 'rejected')">驳回</button>
                  </template>
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
              <span class="detail-key">流程定义</span>
              <strong class="detail-value">{{ selectedTask.processDefinitionName || selectedTask.processDefinitionKey || '-' }}</strong>
            </div>
            <div class="detail-item">
              <span class="detail-key">表单 Key</span>
              <strong class="detail-value">{{ selectedTask.formKey || '未配置' }}</strong>
            </div>
            <div class="detail-item">
              <span class="detail-key">待办页</span>
              <strong class="detail-value">{{ selectedTask.todoPage || '-' }}</strong>
            </div>
            <div class="detail-item">
              <span class="detail-key">候选范围</span>
              <strong class="detail-value">{{ renderCandidateSummary(selectedTask) }}</strong>
            </div>
          </section>

          <section class="approval-summary">
            <div class="detail-item">
              <span class="detail-key">申请人</span>
              <strong class="detail-value">{{ selectedTask.applicant || selectedTask.variables?.applicant || '-' }}</strong>
            </div>
            <div class="detail-item">
              <span class="detail-key">流程实例</span>
              <strong class="detail-value">{{ selectedTask.processInstanceId }}</strong>
            </div>
            <div class="detail-item">
              <span class="detail-key">业务键</span>
              <strong class="detail-value">{{ selectedTask.businessKey || '-' }}</strong>
            </div>
            <div class="detail-item">
              <span class="detail-key">当前状态</span>
              <strong class="detail-value">{{ selectedTask.leaveStatus || selectedTask.assignee || '-' }}</strong>
            </div>
          </section>

          <component :is="selectedTaskRenderer" :task="selectedTask" />

          <div class="inline-actions">
            <button
              v-if="canOpenRuntimePage(selectedTask)"
              type="button"
              class="ghost-btn mini-btn"
              @click="openRuntimePage(selectedTask)"
            >
              打开业务待办页
            </button>
            <button
              v-if="canOpenRuntimePage(selectedTask, 'done')"
              type="button"
              class="secondary-btn mini-btn"
              @click="openRuntimePage(selectedTask, 'done')"
            >
              预览完成页
            </button>
          </div>

          <section v-if="selectedTask.canClaim" class="task-stage-card">
            <div>
              <p class="eyebrow">Claim First</p>
              <h3>当前任务还未签收</h3>
            </div>
            <p class="muted-text">
              该任务命中了办理人/候选人配置，但仍处于待签收状态。先完成签收，再进入审批动作，避免把领取和处理混在一起。
            </p>
            <div class="inline-actions">
              <button type="button" class="primary-btn" :disabled="claiming" @click="claimTask(selectedTask)">
                {{ claiming ? '签收中...' : '签收当前任务' }}
              </button>
              <button type="button" class="secondary-btn" @click="fetchTasks">刷新状态</button>
            </div>
          </section>

          <template v-else>
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
              <div class="action-row">
                <button type="button" class="primary-btn" :disabled="loading" @click="completeSelectedTask">
                  {{ loading ? '提交中...' : `确认${selectedOutcome === 'approved' ? '同意' : '驳回'}` }}
                </button>
                <button type="button" class="secondary-btn" @click="closeTask">取消</button>
              </div>
            </div>
          </template>
        </template>

        <div v-else class="empty-state task-empty-panel">
          从左侧任务列表中选择一条待办，右侧会显示表单 Key、候选信息和处理入口。
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import GenericTaskForm from '../components/task-forms/GenericTaskForm.vue'
import LeaveTaskForm from '../components/task-forms/LeaveTaskForm.vue'
import { getCurrentUser, http } from '../api/http'
import { ensureTaskPageCatalogLoaded } from '../runtime/taskPageCatalog'
import { buildRuntimeRoute, canOpenRuntimePage } from '../runtime/taskRuntimeRegistry'

const FORM_RENDERERS = [
  {
    match: (task) =>
      Boolean(
        task?.formKey?.toLowerCase().includes('leave') ||
          task?.processDefinitionKey?.toLowerCase().includes('leave') ||
          task?.applicant
      ),
    component: LeaveTaskForm
  }
]

const router = useRouter()
const currentUser = ref(getCurrentUser())
const tasks = ref([])
const selectedTask = ref(null)
const selectedOutcome = ref('approved')
const comment = ref('')
const keyword = ref('')
const assignmentFilter = ref('all')
const formFilter = ref('all')
const loading = ref(false)
const claiming = ref(false)
const statusMessage = ref('')
const errorMessage = ref('')

const filteredTasks = computed(() => {
  const needle = keyword.value.trim().toLowerCase()
  return tasks.value.filter((task) => {
    if (assignmentFilter.value === 'claimable' && !task.canClaim) {
      return false
    }
    if (assignmentFilter.value === 'assigned' && task.canClaim) {
      return false
    }
    if (formFilter.value === 'leave' && !isLeaveTask(task)) {
      return false
    }
    if (formFilter.value === 'configured' && !task.formKey) {
      return false
    }
    if (formFilter.value === 'generic' && task.formKey) {
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
      task.formKey,
      task.todoPage
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(needle))
  })
})

const claimableTaskCount = computed(() => tasks.value.filter((task) => task.canClaim).length)
const assignedToMeCount = computed(() => tasks.value.filter((task) => task.assignmentMode === 'ASSIGNED_TO_ME').length)
const formTaskCount = computed(() => tasks.value.filter((task) => task.formKey).length)
const selectedTaskRenderer = computed(() => resolveFormRenderer(selectedTask.value))

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

function isLeaveTask(task) {
  return resolveFormRenderer(task) === LeaveTaskForm
}

function resolveFormRenderer(task) {
  if (!task) {
    return GenericTaskForm
  }
  const matched = FORM_RENDERERS.find((item) => item.match(task))
  return matched?.component || GenericTaskForm
}

function renderAssignmentLabel(task) {
  const map = {
    ASSIGNED_TO_ME: '已签收到我',
    ASSIGNED: '已分配',
    CANDIDATE_USER: '候选用户',
    CANDIDATE_GROUP: '候选角色',
    UNASSIGNED: '未分配'
  }
  return map[task.assignmentMode] || '未分配'
}

function renderCandidateSummary(task) {
  const users = Array.isArray(task.candidateUsers) ? task.candidateUsers.join(', ') : ''
  const groups = Array.isArray(task.candidateGroups) ? task.candidateGroups.join(', ') : ''
  return [users && `用户: ${users}`, groups && `角色: ${groups}`].filter(Boolean).join(' / ') || '无'
}

function openRuntimePage(task, mode = 'todo') {
  if (!canOpenRuntimePage(task, mode)) {
    return
  }
  router.push(buildRuntimeRoute(task, mode))
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
  resetMessages()
}

function buildTaskPayload(task, outcome) {
  const variables = {
    comment: comment.value,
    leaveStatus: outcome === 'approved' ? 'APPROVED' : 'REJECTED'
  }
  let status = outcome === 'approved' ? 'APPROVED' : 'REJECTED'

  if (task.name?.includes('部门经理')) {
    variables.deptApproval = outcome
  } else if (task.name?.includes('总经理')) {
    variables.generalApproval = outcome
  } else if (!isLeaveTask(task)) {
    status = outcome === 'approved' ? 'COMPLETED' : 'REJECTED'
  }

  return {
    taskId: task.id,
    processInstanceId: task.processInstanceId,
    status,
    variables
  }
}

function buildCompletionSnapshot(task, outcome) {
  return {
    ...task,
    canClaim: false,
    claimed: true,
    leaveStatus: outcome === 'approved' ? 'APPROVED' : 'REJECTED',
    variables: {
      ...(task?.variables || {}),
      comment: comment.value,
      leaveStatus: outcome === 'approved' ? 'APPROVED' : 'REJECTED'
    }
  }
}

async function fetchTasks() {
  if (!currentUser.value?.username) {
    tasks.value = []
    selectedTask.value = null
    return
  }
  try {
    const { data } = await http.get('/activiti/tasks/inbox')
    tasks.value = Array.isArray(data) ? data : []
    if (selectedTask.value) {
      selectedTask.value = tasks.value.find((task) => task.id === selectedTask.value.id) || null
    }
  } catch (error) {
    tasks.value = []
    errorMessage.value = error.normalizedMessage
  }
}

async function claimTask(task) {
  if (!task?.id) {
    return
  }
  resetMessages()
  claiming.value = true
  try {
    await http.post('/activiti/task/claim', { taskId: task.id })
    statusMessage.value = '任务已签收，可以继续审批处理。'
    selectedTask.value = task
    await fetchTasks()
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    claiming.value = false
  }
}

async function completeSelectedTask() {
  if (!selectedTask.value) {
    return
  }
  resetMessages()
  loading.value = true
  try {
    const snapshot = buildCompletionSnapshot(selectedTask.value, selectedOutcome.value)
    await http.post('/activiti/task/complete', buildTaskPayload(selectedTask.value, selectedOutcome.value))
    statusMessage.value = '任务处理成功'
    await fetchTasks()
    if (canOpenRuntimePage(snapshot, 'done')) {
      await router.push(buildRuntimeRoute(snapshot, 'done'))
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
  currentUser.value = currentUser.value || (await http.get('/auth/current')).data
  await ensureTaskPageCatalogLoaded()
  await fetchTasks()
})
</script>
