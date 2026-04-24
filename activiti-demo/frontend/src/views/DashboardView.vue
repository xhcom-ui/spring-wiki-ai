<template>
  <div class="page-grid dashboard-page">
    <section class="hero-workbench dashboard-hero">
      <div class="hero-workbench-copy dashboard-hero-copy">
        <p class="eyebrow">工作台概览</p>
        <h3>常用流程入口和运行状态都集中在这里</h3>
        <p class="hero-text">
          这里展示当前账号最常用的流程信息，包括流程定义、待办审批、我的申请和监控统计，方便直接查看和进入下一步操作。
        </p>

        <div class="hero-action-row dashboard-hero-actions">
          <RouterLink v-if="canDesign" to="/designer" class="primary-btn hero-link">进入设计中心</RouterLink>
          <RouterLink v-if="canSubmitLeave" to="/leave" class="secondary-btn hero-link">发起流程</RouterLink>
          <RouterLink v-if="canApprove" to="/tasks" class="secondary-btn hero-link">处理待办</RouterLink>
        </div>

        <div class="hero-metrics dashboard-hero-metrics">
          <div class="hero-metric dashboard-hero-metric">
            <span>激活流程</span>
            <strong>{{ definitions.length }}</strong>
          </div>
          <div class="hero-metric dashboard-hero-metric">
            <span>待办任务</span>
            <strong>{{ tasks.length }}</strong>
          </div>
          <div class="hero-metric dashboard-hero-metric">
            <span>我的申请</span>
            <strong>{{ leaves.length }}</strong>
          </div>
        </div>
      </div>

      <div class="hero-workbench-side dashboard-hero-side">
        <div class="hero-side-card dashboard-spotlight-card">
          <span class="hero-side-label">当前提醒</span>
          <strong>{{ nextTaskLabel }}</strong>
          <p>{{ nextTaskHint }}</p>
        </div>
      </div>
    </section>

    <section class="stats-grid dashboard-stats-grid">
      <article class="stat-card dashboard-stat-card">
        <span class="muted-text">运行中流程</span>
        <strong>{{ statistics.runningProcessCount || 0 }}</strong>
      </article>
      <article class="stat-card dashboard-stat-card">
        <span class="muted-text">已完成流程</span>
        <strong>{{ statistics.completedProcessCount || 0 }}</strong>
      </article>
      <article class="stat-card dashboard-stat-card">
        <span class="muted-text">当前待办</span>
        <strong>{{ tasks.length }}</strong>
      </article>
      <article class="stat-card dashboard-stat-card">
        <span class="muted-text">我的申请</span>
        <strong>{{ leaves.length }}</strong>
      </article>
    </section>

    <section class="content-grid two-column dashboard-content-grid">
      <article class="panel-card dashboard-panel-card">
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

      <article class="panel-card dashboard-panel-card">
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

    <section class="content-grid two-column dashboard-content-grid">
      <article class="panel-card dashboard-panel-card">
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

      <article class="panel-card dashboard-panel-card">
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
const canOrchestrate = hasPermission('orchestration:manage')
const canManageRuntimeCatalog = hasPermission('runtime:catalog:manage')

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
  if (canOrchestrate) {
    entries.push({
      path: '/orchestration',
      title: '流程编排',
      description: '发起编排、回调和信号事件，验证流程与外部动作的协同。',
      action: '进入编排'
    })
  }
  if (canManageRuntimeCatalog) {
    entries.push({
      path: '/runtime-catalog',
      title: '业务页目录',
      description: '维护待办页、完成页和运行时映射，打通流程节点与业务页面。',
      action: '维护目录'
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
    requests.push(http.get(`/activiti/tasks/${username}`))
  } else {
    requests.push(Promise.resolve({ data: [] }))
  }
  if (username && canSubmitLeave) {
    requests.push(http.get(`/activiti/leaves/applicant/${username}`))
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
.dashboard-page {
  gap: 16px;
}

.dashboard-hero {
  align-items: stretch;
}

.dashboard-hero-copy,
.dashboard-hero-side,
.dashboard-stat-card,
.dashboard-panel-card {
  border-color: rgba(15, 23, 42, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 251, 255, 0.98));
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(10px);
}

.dashboard-hero-copy {
  position: relative;
  overflow: hidden;
  padding: 24px;
  gap: 16px;
  background:
    radial-gradient(circle at top right, rgba(47, 125, 225, 0.12), transparent 32%),
    linear-gradient(135deg, rgba(245, 249, 255, 0.98), rgba(255, 255, 255, 0.98));
}

.dashboard-hero-copy::after {
  content: '';
  position: absolute;
  inset: auto 22px 20px auto;
  width: 92px;
  height: 92px;
  border-radius: 20px;
  background:
    linear-gradient(135deg, rgba(47, 125, 225, 0.08), rgba(255, 255, 255, 0));
  pointer-events: none;
}

.dashboard-hero-copy h3 {
  max-width: 14ch;
  font-size: clamp(1.55rem, 2.6vw, 2.4rem);
  letter-spacing: -0.03em;
  color: #162235;
}

.dashboard-hero-copy .hero-text {
  max-width: 62ch;
  font-size: 14px;
  line-height: 1.7;
  color: #5f728b;
}

.dashboard-hero-actions {
  gap: 10px;
}

.dashboard-hero-metrics {
  gap: 10px;
}

.dashboard-hero-metric {
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(73, 97, 135, 0.1);
  background: rgba(255, 255, 255, 0.84);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.dashboard-hero-metric span {
  color: #70829a;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.dashboard-hero-metric strong {
  font-size: clamp(1.55rem, 2.5vw, 2.05rem);
  color: #17304f;
}

.dashboard-hero-side {
  padding: 18px;
  background:
    radial-gradient(circle at top left, rgba(47, 125, 225, 0.08), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(246, 250, 255, 0.98));
}

.dashboard-spotlight-card {
  height: 100%;
  padding: 18px;
  border-radius: 20px;
  border: 1px solid rgba(73, 97, 135, 0.1);
  background: rgba(255, 255, 255, 0.86);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.92);
}

.dashboard-spotlight-card strong {
  font-size: 1.28rem;
  color: #16263f;
}

.dashboard-spotlight-card p {
  margin: 0;
  color: #657891;
  line-height: 1.75;
}

.dashboard-stats-grid {
  gap: 12px;
}

.dashboard-stat-card {
  min-height: 96px;
  padding: 16px 18px;
  border-radius: 20px;
  justify-content: center;
  gap: 8px;
}

.dashboard-stat-card .muted-text {
  color: #7588a0;
  font-size: 13px;
}

.dashboard-stat-card strong {
  font-size: clamp(1.6rem, 2.3vw, 2.2rem);
  color: #172b45;
  letter-spacing: -0.03em;
}

.dashboard-content-grid {
  gap: 14px;
}

.dashboard-panel-card {
  min-height: 100%;
  border-radius: 22px;
  padding: 20px;
}

.dashboard-panel-card :deep(.simple-table thead th) {
  color: #70829a;
}

.dashboard-governance-item {
  align-items: center;
}

@media (max-width: 900px) {
  .dashboard-hero-copy {
    padding: 26px;
  }

  .dashboard-hero-copy h3 {
    max-width: none;
  }
}
</style>
