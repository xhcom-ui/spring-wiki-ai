<template>
  <div class="page-grid">
    <section class="hero-workbench">
      <div class="hero-workbench-copy">
        <p class="eyebrow">工作台概览</p>
        <h3>常用流程入口和运行状态都集中在这里</h3>
        <p class="hero-text">
          这里展示当前账号最常用的流程信息，包括流程定义、待办审批、我的申请和监控统计，方便直接查看和进入下一步操作。
        </p>

        <div class="hero-action-row">
          <RouterLink v-if="canDesign" to="/designer" class="primary-btn hero-link">进入设计中心</RouterLink>
          <RouterLink v-if="canSubmitLeave" to="/leave" class="secondary-btn hero-link">发起流程</RouterLink>
          <RouterLink v-if="canApprove" to="/tasks" class="secondary-btn hero-link">处理待办</RouterLink>
        </div>

        <div class="hero-metrics">
          <div class="hero-metric">
            <span>激活流程</span>
            <strong>{{ definitions.length }}</strong>
          </div>
          <div class="hero-metric">
            <span>待办任务</span>
            <strong>{{ tasks.length }}</strong>
          </div>
          <div class="hero-metric">
            <span>我的申请</span>
            <strong>{{ leaves.length }}</strong>
          </div>
        </div>
      </div>

      <div class="hero-workbench-side">
        <div class="hero-side-card">
          <span class="hero-side-label">当前提醒</span>
          <strong>{{ nextTaskLabel }}</strong>
          <p>{{ nextTaskHint }}</p>
        </div>
      </div>
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
        <span class="muted-text">当前待办</span>
        <strong>{{ tasks.length }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">我的申请</span>
        <strong>{{ leaves.length }}</strong>
      </article>
    </section>

    <section class="content-grid two-column">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">流程定义</p>
            <h3>激活中的流程定义</h3>
          </div>
          <RouterLink v-if="canDesign" to="/designer" class="text-link">进入设计中心</RouterLink>
          <span v-else class="muted-text">无流程设计权限</span>
        </div>
        <div class="stack-list">
          <div v-for="definition in definitions" :key="definition.id" class="list-item rich-list-item">
            <div>
              <strong>{{ definition.processName }}</strong>
              <p>{{ definition.processKey }} · v{{ definition.version }}</p>
            </div>
            <span class="status-pill success-pill">{{ definition.status }}</span>
          </div>
          <div v-if="!definitions.length" class="empty-state">暂无激活流程</div>
        </div>
      </article>

      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">待办审批</p>
            <h3>今日审批重点</h3>
          </div>
          <RouterLink v-if="canApprove" to="/tasks" class="text-link">去审批</RouterLink>
          <span v-else class="muted-text">无审批权限</span>
        </div>
        <div class="stack-list">
          <div v-for="task in tasks.slice(0, 5)" :key="task.id" class="list-item rich-list-item">
            <div>
              <strong>{{ task.name }}</strong>
              <p>{{ task.businessKey || task.processInstanceId }}</p>
            </div>
            <span :class="['status-pill', task.assignee ? 'warning-pill' : 'muted-pill']">
              {{ task.assignee || '待签收' }}
            </span>
          </div>
          <div v-if="!tasks.length" class="empty-state">当前没有待办任务</div>
        </div>
      </article>
    </section>

    <section class="content-grid two-column">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">我的申请</p>
            <h3>最近发起的流程</h3>
          </div>
          <RouterLink v-if="canSubmitLeave" to="/leave" class="text-link">继续发起</RouterLink>
          <span v-else class="muted-text">无发起权限</span>
        </div>
        <table class="simple-table">
          <thead>
            <tr>
              <th>申请人</th>
              <th>天数</th>
              <th>状态</th>
              <th>创建时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="leave in leaves.slice(0, 6)" :key="leave.id">
              <td>{{ leave.applicant }}</td>
              <td>{{ leave.days }}</td>
              <td>{{ leave.status }}</td>
              <td>{{ formatDate(leave.createdAt) }}</td>
            </tr>
            <tr v-if="!leaves.length">
              <td colspan="4" class="empty-row">暂无请假申请</td>
            </tr>
          </tbody>
        </table>
      </article>

      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">权限治理</p>
            <h3>系统治理入口</h3>
          </div>
          <RouterLink v-if="canMonitor" to="/monitoring" class="text-link">查看监控</RouterLink>
          <span v-else class="muted-text">无监控权限</span>
        </div>
        <div class="stack-list">
          <div class="list-item rich-list-item">
            <div>
              <strong>运行统计</strong>
              <p>监控中心当前可查看实例、任务和历史轨迹。</p>
            </div>
            <span class="status-pill">{{ statistics.runningProcessCount || 0 }} 运行中</span>
          </div>
          <div
            v-for="entry in governanceEntries"
            :key="entry.path"
            class="list-item rich-list-item dashboard-governance-item"
          >
            <div>
              <strong>{{ entry.title }}</strong>
              <p>{{ entry.description }}</p>
            </div>
            <RouterLink :to="entry.path" class="text-link">{{ entry.action }}</RouterLink>
          </div>
          <div v-if="!governanceEntries.length" class="empty-state">当前账号没有权限治理模块入口</div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getCurrentUser, hasPermission, http } from '../api/http'

const statistics = ref({})
const definitions = ref([])
const tasks = ref([])
const leaves = ref([])
const canMonitor = hasPermission('monitoring:view')
const canDesign = hasPermission('process:design')
const canApprove = hasPermission('task:approve')
const canSubmitLeave = hasPermission('leave:submit')
const canManageUsers = hasPermission('user:manage')
const canManageRoles = hasPermission('role:manage')
const canManageMenus = hasPermission('menu:manage')

const nextTaskLabel = computed(() => {
  if (tasks.value.length) {
    return tasks.value[0]?.name || '有待处理审批'
  }
  if (definitions.value.length) {
    return `${definitions.value[0]?.processName || '流程定义'} 可继续维护`
  }
  return '当前没有待办任务'
})

const nextTaskHint = computed(() => {
  if (tasks.value.length) {
    return `当前共有 ${tasks.value.length} 条待办，建议优先处理最上方任务。`
  }
  if (leaves.value.length) {
    return `你最近发起了 ${leaves.value.length} 条流程，可以继续查看处理进度。`
  }
  return '可以先到流程设计中维护流程，再发起和验证审批链路。'
})

const governanceEntries = computed(() => {
  const entries = []
  if (canManageUsers) {
    entries.push({
      path: '/users',
      title: '用户管理',
      description: '维护账号信息、人员状态和基础权限范围。',
      action: '管理用户'
    })
  }
  if (canManageRoles) {
    entries.push({
      path: '/roles',
      title: '角色管理',
      description: '配置角色与权限组合，统一治理审批职责边界。',
      action: '管理角色'
    })
  }
  if (canManageMenus) {
    entries.push({
      path: '/menu-permissions',
      title: '菜单权限',
      description: '控制菜单入口和页面访问范围的展示策略。',
      action: '配置菜单'
    })
  }
  return entries
})

function formatDate(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

async function fetchDashboard() {
  const currentUser = getCurrentUser()
  const username = currentUser?.username
  const requests = [
    canMonitor ? http.get('/monitoring/statistics') : Promise.resolve({ data: {} }),
    canDesign ? http.get('/process-definition/active') : Promise.resolve({ data: [] })
  ]
  if (username && canApprove) {
    requests.push(http.get(`/flowable/tasks/${username}`))
  } else {
    requests.push(Promise.resolve({ data: [] }))
  }
  if (username && canSubmitLeave) {
    requests.push(http.get(`/flowable/leaves/applicant/${username}`))
  } else {
    requests.push(Promise.resolve({ data: [] }))
  }

  const responses = await Promise.all(requests)
  statistics.value = responses[0].data || {}
  definitions.value = responses[1].data || []
  tasks.value = responses[2]?.data || []
  leaves.value = responses[3]?.data || []
}

onMounted(() => {
  fetchDashboard().catch(() => {
    statistics.value = {}
    definitions.value = []
    tasks.value = []
    leaves.value = []
  })
})
</script>

<style scoped>
.dashboard-governance-item {
  align-items: center;
}
</style>
