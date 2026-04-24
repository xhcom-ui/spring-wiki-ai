<template>
  <div class="page-grid">
    <section class="panel-card">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Version Inspector</p>
          <h3>流程版本历史对比</h3>
        </div>
        <div class="management-meta">
          <span class="meta-pill">流程 Key {{ processKeys.length }}</span>
          <span class="meta-pill">当前版本 {{ versions.length }}</span>
          <span class="meta-pill">部署记录 {{ deploymentHistory.length }}</span>
        </div>
      </div>

      <div class="management-toolbar">
        <div class="management-filters">
          <label class="compact-field">
            <span>流程 Key</span>
            <select v-model="selectedProcessKey" @change="fetchVersions">
              <option value="">请选择</option>
              <option v-for="key in processKeys" :key="key" :value="key">{{ key }}</option>
            </select>
          </label>
          <label class="filter-grow">
            <span>左侧版本</span>
            <select v-model.number="leftId">
              <option :value="0">请选择</option>
              <option v-for="version in versions" :key="version.id" :value="version.id">
                v{{ version.version }} · {{ version.processName }}
              </option>
            </select>
          </label>
          <label class="filter-grow">
            <span>右侧版本</span>
            <select v-model.number="rightId">
              <option :value="0">请选择</option>
              <option v-for="version in versions" :key="version.id" :value="version.id">
                v{{ version.version }} · {{ version.processName }}
              </option>
            </select>
          </label>
        </div>

        <div class="management-actions">
          <button type="button" class="secondary-btn" @click="fillLatestPair" :disabled="versions.length < 2">自动选择最新两版</button>
          <button type="button" class="secondary-btn" @click="fetchDeploymentHistory" :disabled="!selectedProcessKey || loading">查看部署历史</button>
          <button type="button" class="primary-btn" :disabled="!leftId || !rightId || loading" @click="compareVersions">
            {{ loading ? '对比中...' : '执行对比' }}
          </button>
        </div>
      </div>

      <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>
    </section>

    <section v-if="deploymentHistory.length" class="panel-card">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Deployment History</p>
          <h3>FlowLong 部署记录</h3>
        </div>
      </div>
      <table class="simple-table">
        <thead>
          <tr>
            <th>部署时间</th>
            <th>流程名称</th>
            <th>运行时版本</th>
            <th>来源定义版本</th>
            <th>部署人</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in deploymentHistory" :key="item.id">
            <td>{{ formatDate(item.deployedAt) }}</td>
            <td>{{ item.processName || '-' }}</td>
            <td>{{ item.flowLongProcessVersion || '-' }}</td>
            <td>{{ item.sourceDefinitionVersion || '-' }}</td>
            <td>{{ item.deployedByName || '-' }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section v-if="compareResult" class="stats-grid">
      <article class="stat-card">
        <span class="muted-text">变化行数</span>
        <strong>{{ compareResult.changedLineCount }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">版本差值</span>
        <strong>{{ compareResult.versionGap }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">左侧行数</span>
        <strong>{{ compareResult.leftLineCount }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">右侧行数</span>
        <strong>{{ compareResult.rightLineCount }}</strong>
      </article>
    </section>

    <section v-if="compareResult" class="content-grid two-column">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Compare Summary</p>
            <h3>对比摘要</h3>
          </div>
          <span :class="['status-pill', compareResult.sameProcessKey ? 'success-pill' : 'warning-pill']">
            {{ compareResult.sameProcessKey ? '同一流程' : '跨流程版本' }}
          </span>
        </div>

        <div class="stack-list">
          <div class="list-item rich-list-item">
            <div>
              <strong>左侧版本</strong>
              <p>{{ compareResult.left.processKey }} · v{{ compareResult.left.version }}</p>
            </div>
            <span class="status-pill">{{ compareResult.left.status }}</span>
          </div>
          <div class="list-item rich-list-item">
            <div>
              <strong>右侧版本</strong>
              <p>{{ compareResult.right.processKey }} · v{{ compareResult.right.version }}</p>
            </div>
            <span class="status-pill">{{ compareResult.right.status }}</span>
          </div>
        </div>

        <table class="simple-table">
          <thead>
            <tr>
              <th>行号</th>
              <th>左侧</th>
              <th>右侧</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in compareResult.diffPreview" :key="row.line">
              <td>{{ row.line }}</td>
              <td>{{ row.left || '-' }}</td>
              <td>{{ row.right || '-' }}</td>
            </tr>
            <tr v-if="!(compareResult.diffPreview || []).length">
              <td colspan="3" class="empty-row">暂无差异预览</td>
            </tr>
          </tbody>
        </table>
      </article>

      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Version Metadata</p>
            <h3>版本元数据</h3>
          </div>
        </div>
        <div class="detail-grid compact-detail-grid">
          <article class="detail-item">
            <span class="detail-key">左侧版本</span>
            <strong class="detail-value">v{{ compareResult.left.version }}</strong>
          </article>
          <article class="detail-item">
            <span class="detail-key">右侧版本</span>
            <strong class="detail-value">v{{ compareResult.right.version }}</strong>
          </article>
          <article class="detail-item">
            <span class="detail-key">左侧状态</span>
            <strong class="detail-value">{{ compareResult.left.status }}</strong>
          </article>
          <article class="detail-item">
            <span class="detail-key">右侧状态</span>
            <strong class="detail-value">{{ compareResult.right.status }}</strong>
          </article>
          <article class="detail-item">
            <span class="detail-key">左侧部署</span>
            <strong class="detail-value">{{ compareResult.left.deploymentId || '-' }}</strong>
          </article>
          <article class="detail-item">
            <span class="detail-key">右侧部署</span>
            <strong class="detail-value">{{ compareResult.right.deploymentId || '-' }}</strong>
          </article>
          <article class="detail-item">
            <span class="detail-key">左侧名称</span>
            <strong class="detail-value">{{ compareResult.left.processName || '-' }}</strong>
          </article>
          <article class="detail-item">
            <span class="detail-key">右侧名称</span>
            <strong class="detail-value">{{ compareResult.right.processName || '-' }}</strong>
          </article>
        </div>
      </article>
    </section>

    <section v-if="compareResult" class="content-grid two-column">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Left XML</p>
            <h3>左侧 XML</h3>
          </div>
        </div>
        <pre class="code-block">{{ compareResult.leftXml }}</pre>
      </article>
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Right XML</p>
            <h3>右侧 XML</h3>
          </div>
        </div>
        <pre class="code-block">{{ compareResult.rightXml }}</pre>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { http } from '../api/http'

const loading = ref(false)
const definitions = ref([])
const versions = ref([])
const deploymentHistory = ref([])
const selectedProcessKey = ref('')
const leftId = ref(0)
const rightId = ref(0)
const compareResult = ref(null)
const errorMessage = ref('')

const processKeys = computed(() => [...new Set((definitions.value || []).map((item) => item.processKey))])

function formatDate(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

async function fetchDefinitions() {
  try {
    const { data } = await http.get('/process-definition/all')
    definitions.value = data || []
  } catch (error) {
    definitions.value = []
    errorMessage.value = error.normalizedMessage || error.message || '流程定义加载失败'
  }
}

async function fetchVersions() {
  compareResult.value = null
  deploymentHistory.value = []
  leftId.value = 0
  rightId.value = 0
  if (!selectedProcessKey.value) {
    versions.value = []
    return
  }
  try {
    const { data } = await http.get(`/process-definition/versions/${selectedProcessKey.value}`)
    versions.value = data || []
  } catch (error) {
    versions.value = []
    errorMessage.value = error.normalizedMessage || error.message || '流程版本加载失败'
  }
}

function fillLatestPair() {
  if (versions.value.length < 2) {
    return
  }
  leftId.value = versions.value[0].id
  rightId.value = versions.value[1].id
}

async function fetchDeploymentHistory() {
  if (!selectedProcessKey.value) {
    deploymentHistory.value = []
    return
  }
  try {
    const { data } = await http.get(`/flowlong/deployments/${selectedProcessKey.value}`)
    deploymentHistory.value = data || []
  } catch (error) {
    deploymentHistory.value = []
    errorMessage.value = error.normalizedMessage || error.message || '部署记录加载失败'
  }
}

async function compareVersions() {
  errorMessage.value = ''
  loading.value = true
  try {
    const { data } = await http.get('/process-definition/compare', {
      params: { leftId: leftId.value, rightId: rightId.value }
    })
    compareResult.value = data
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDefinitions().catch(() => {})
})
</script>
