<template>
  <div class="page-grid">
    <section class="content-grid two-column">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Leave Request</p>
            <h3>发起请假流程</h3>
          </div>
        </div>

        <form class="form-stack" @submit.prevent="submitLeave">
          <div class="form-grid">
            <label>
              <span>流程定义</span>
              <select v-model="form.processKey">
                <option v-for="item in availableProcesses" :key="item.processKey" :value="item.processKey">
                  {{ item.processName }} · {{ item.processKey }}
                </option>
              </select>
            </label>
            <label>
              <span>申请人</span>
              <input v-model="form.applicant" type="text" disabled />
            </label>
            <label>
              <span>请假天数</span>
              <input v-model.number="form.days" type="number" min="1" max="30" />
            </label>
            <label>
              <span>开始时间</span>
              <input v-model="form.startDate" type="datetime-local" />
            </label>
            <label>
              <span>结束时间</span>
              <input v-model="form.endDate" type="datetime-local" />
            </label>
            <label>
              <span>部门经理</span>
              <select v-model="form.deptManager">
                <option value="">请选择</option>
                <option v-for="user in approvers" :key="user.id" :value="user.username">{{ user.nickname || user.username }}</option>
              </select>
            </label>
            <label>
              <span>总经理</span>
              <select v-model="form.generalManager">
                <option value="">请选择</option>
                <option v-for="user in approvers" :key="user.id" :value="user.username">{{ user.nickname || user.username }}</option>
              </select>
            </label>
          </div>

          <p class="hint">
            当前发起会直接使用所选流程定义里的用户任务配置，办理人、候选人和表单 Key 会同步影响后续待办工作台。
          </p>

          <label>
            <span>请假原因</span>
            <textarea v-model.trim="form.reason" rows="5" placeholder="请输入请假原因"></textarea>
          </label>

          <div class="action-row">
            <button type="submit" class="primary-btn" :disabled="loading">
              {{ loading ? '提交中...' : '发起流程' }}
            </button>
            <button type="button" class="secondary-btn" @click="resetForm">重置</button>
          </div>

          <p v-if="statusMessage" class="feedback success">{{ statusMessage }}</p>
          <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>
        </form>
      </article>

      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">My Requests</p>
            <h3>我的请假记录</h3>
          </div>
          <button type="button" class="ghost-btn" @click="fetchLeaves">刷新</button>
        </div>

        <table class="simple-table">
          <thead>
            <tr>
              <th>流程定义</th>
              <th>申请人</th>
              <th>天数</th>
              <th>状态</th>
              <th>流程实例</th>
              <th>创建时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="leave in leaves" :key="leave.id">
              <td>{{ leave.processKey || activeProcessLabel }}</td>
              <td>{{ leave.applicant }}</td>
              <td>{{ leave.days }}</td>
              <td>{{ leave.status }}</td>
              <td>{{ leave.processInstanceId }}</td>
              <td>{{ formatDate(leave.createdAt) }}</td>
            </tr>
            <tr v-if="!leaves.length">
              <td colspan="6" class="empty-row">暂无请假记录</td>
            </tr>
          </tbody>
        </table>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getCurrentUser, http } from '../api/http'

const loading = ref(false)
const leaves = ref([])
const approvers = ref([])
const processDefinitions = ref([])
const statusMessage = ref('')
const errorMessage = ref('')
const currentUser = ref(getCurrentUser())

const form = reactive({
  processKey: 'leaveProcess',
  applicant: currentUser.value?.username || '',
  deptManager: '',
  generalManager: '',
  days: 1,
  reason: '',
  startDate: '',
  endDate: ''
})

const availableProcesses = computed(() => {
  const activeItems = processDefinitions.value.filter((item) => item.status === 'ACTIVE')
  if (activeItems.length) {
    return activeItems
  }
  return [
    {
      processKey: 'leaveProcess',
      processName: '默认请假流程'
    }
  ]
})

const activeProcessLabel = computed(() => {
  const current = availableProcesses.value.find((item) => item.processKey === form.processKey)
  return current?.processName || form.processKey
})

function resetMessages() {
  statusMessage.value = ''
  errorMessage.value = ''
}

function resetForm() {
  form.applicant = currentUser.value?.username || ''
  form.deptManager = ''
  form.generalManager = ''
  form.days = 1
  form.reason = ''
  form.startDate = ''
  form.endDate = ''
  form.processKey = availableProcesses.value[0]?.processKey || 'leaveProcess'
}

function formatDate(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

async function fetchApprovers() {
  const { data } = await http.get('/users/lookup')
  approvers.value = Array.isArray(data)
    ? data.filter((item) => item.username !== currentUser.value?.username && item.status === 1)
    : []
}

async function fetchProcessDefinitions() {
  const { data } = await http.get('/process-definition/all')
  processDefinitions.value = Array.isArray(data) ? data : []
  form.processKey = availableProcesses.value[0]?.processKey || 'leaveProcess'
}

async function fetchLeaves() {
  if (!currentUser.value?.username) {
    leaves.value = []
    return
  }
  const { data } = await http.get(`/activiti/leaves/applicant/${currentUser.value.username}`)
  leaves.value = Array.isArray(data) ? data : []
}

async function submitLeave() {
  resetMessages()
  loading.value = true
  try {
    await http.post('/activiti/leave/start', {
      ...form
    })
    statusMessage.value = '请假流程已发起，所选流程定义里的用户任务配置已进入运行时待办。'
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
  await Promise.all([fetchApprovers(), fetchLeaves(), fetchProcessDefinitions()])
})
</script>
