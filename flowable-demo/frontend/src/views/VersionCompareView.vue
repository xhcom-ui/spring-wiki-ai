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
          <span class="meta-pill">{{ selectedProcessKey || '未选择流程' }}</span>
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
                v{{ version.version }} · {{ version.processName }} · {{ resolveDesignerType(version.designerType) }}
              </option>
            </select>
          </label>
          <label class="filter-grow">
            <span>右侧版本</span>
            <select v-model.number="rightId">
              <option :value="0">请选择</option>
              <option v-for="version in versions" :key="version.id" :value="version.id">
                v{{ version.version }} · {{ version.processName }} · {{ resolveDesignerType(version.designerType) }}
              </option>
            </select>
          </label>
        </div>

        <div class="management-actions">
          <button type="button" class="secondary-btn" @click="fillLatestPair" :disabled="versions.length < 2">自动选择最新两版</button>
          <button type="button" class="primary-btn" :disabled="!leftId || !rightId || loading" @click="compareVersions">
            {{ loading ? '对比中...' : '执行对比' }}
          </button>
        </div>
      </div>

      <div class="compare-filter-summary">
        <span class="meta-pill">左侧 {{ leftVersionLabel }}</span>
        <span class="meta-pill">右侧 {{ rightVersionLabel }}</span>
        <span class="meta-pill">模式 {{ compareModeLabel }}</span>
      </div>

      <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>
    </section>

    <section v-if="compareResult" class="stats-grid">
      <article class="stat-card">
        <span class="muted-text">XML 变化行数</span>
        <strong>{{ compareResult.changedLineCount }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">版本差值</span>
        <strong>{{ compareResult.versionGap }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">设计节点变化</span>
        <strong>{{ compareResult.schemaComparison?.changedNodeCount || 0 }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">设计模式</span>
        <strong>{{ compareResult.sameDesignerType ? '一致' : '不同' }}</strong>
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
            <div class="compare-side-meta">
              <span class="status-pill">{{ resolveDesignerType(compareResult.left.designerType) }}</span>
              <span class="status-pill">{{ compareResult.left.status }}</span>
            </div>
          </div>
          <div class="list-item rich-list-item">
            <div>
              <strong>右侧版本</strong>
              <p>{{ compareResult.right.processKey }} · v{{ compareResult.right.version }}</p>
            </div>
            <div class="compare-side-meta">
              <span class="status-pill">{{ resolveDesignerType(compareResult.right.designerType) }}</span>
              <span class="status-pill">{{ compareResult.right.status }}</span>
            </div>
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
              <td colspan="3" class="empty-row">暂无 XML 差异预览</td>
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
            <span class="detail-key">左侧名称</span>
            <strong class="detail-value">{{ compareResult.left.processName || '-' }}</strong>
          </article>
          <article class="detail-item">
            <span class="detail-key">右侧名称</span>
            <strong class="detail-value">{{ compareResult.right.processName || '-' }}</strong>
          </article>
          <article class="detail-item">
            <span class="detail-key">左侧设计器</span>
            <strong class="detail-value">{{ resolveDesignerType(compareResult.left.designerType) }}</strong>
          </article>
          <article class="detail-item">
            <span class="detail-key">右侧设计器</span>
            <strong class="detail-value">{{ resolveDesignerType(compareResult.right.designerType) }}</strong>
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
            <span class="detail-key">设计模式变化</span>
            <strong class="detail-value">{{ compareResult.sameDesignerType ? '无' : '有' }}</strong>
          </article>
          <article class="detail-item">
            <span class="detail-key">设计 JSON</span>
            <strong class="detail-value">{{ hasSchemaDiff ? '存在编排差异' : '无编排差异' }}</strong>
          </article>
        </div>
      </article>
    </section>

    <section v-if="compareResult" class="content-grid two-column">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Designer Diff</p>
            <h3>设计器模式与编排差异</h3>
          </div>
          <span :class="['status-pill', compareResult.sameDesignerType ? 'success-pill' : 'warning-pill']">
            {{ compareResult.sameDesignerType ? '同模式' : 'BPMN / 自定义混合对比' }}
          </span>
        </div>

        <div class="detail-grid compact-detail-grid">
          <article class="detail-item">
            <span class="detail-key">左侧节点数</span>
            <strong class="detail-value">{{ compareResult.schemaComparison?.leftNodeCount || 0 }}</strong>
          </article>
          <article class="detail-item">
            <span class="detail-key">右侧节点数</span>
            <strong class="detail-value">{{ compareResult.schemaComparison?.rightNodeCount || 0 }}</strong>
          </article>
          <article class="detail-item">
            <span class="detail-key">变化节点数</span>
            <strong class="detail-value">{{ compareResult.schemaComparison?.changedNodeCount || 0 }}</strong>
          </article>
          <article class="detail-item">
            <span class="detail-key">设计器切换</span>
            <strong class="detail-value">{{ compareResult.sameDesignerType ? '未切换' : '已切换' }}</strong>
          </article>
        </div>

        <table class="simple-table">
          <thead>
            <tr>
              <th>节点序号</th>
              <th>左侧编排</th>
              <th>右侧编排</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in compareResult.schemaComparison?.diffPreview || []" :key="row.index">
              <td>{{ row.index }}</td>
              <td>{{ row.left || '-' }}</td>
              <td>{{ row.right || '-' }}</td>
            </tr>
            <tr v-if="!(compareResult.schemaComparison?.diffPreview || []).length">
              <td colspan="3" class="empty-row">暂无设计节点差异预览</td>
            </tr>
          </tbody>
        </table>
      </article>

      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Designer Snapshot</p>
            <h3>设计器结构快照</h3>
          </div>
        </div>
        <div class="stack-list compare-node-list">
          <div v-for="node in compareResult.schemaComparison?.leftNodes || []" :key="`left-${node.nodeId}`" class="list-item">
            <strong>左侧</strong>
            <p>{{ node.summary }}</p>
          </div>
          <div v-for="node in compareResult.schemaComparison?.rightNodes || []" :key="`right-${node.nodeId}`" class="list-item">
            <strong>右侧</strong>
            <p>{{ node.summary }}</p>
          </div>
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

    <section v-if="compareResult" class="content-grid two-column">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Left Design</p>
            <h3>左侧设计 JSON</h3>
          </div>
        </div>
        <pre class="code-block">{{ compareResult.schemaComparison?.leftSchemaJson || '无' }}</pre>
      </article>
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Right Design</p>
            <h3>右侧设计 JSON</h3>
          </div>
        </div>
        <pre class="code-block">{{ compareResult.schemaComparison?.rightSchemaJson || '无' }}</pre>
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
const selectedProcessKey = ref('')
const leftId = ref(0)
const rightId = ref(0)
const compareResult = ref(null)
const errorMessage = ref('')

const processKeys = computed(() => [...new Set((definitions.value || []).map((item) => item.processKey))])
const leftVersion = computed(() => versions.value.find((item) => item.id === leftId.value) || null)
const rightVersion = computed(() => versions.value.find((item) => item.id === rightId.value) || null)
const leftVersionLabel = computed(() => leftVersion.value ? `v${leftVersion.value.version}` : '未选择')
const rightVersionLabel = computed(() => rightVersion.value ? `v${rightVersion.value.version}` : '未选择')
const compareModeLabel = computed(() => {
  if (!leftVersion.value || !rightVersion.value) {
    return '待选择版本'
  }
  return leftVersion.value.designerType === rightVersion.value.designerType ? '同设计器模式' : '混合设计器模式'
})
const hasSchemaDiff = computed(() => (compareResult.value?.schemaComparison?.changedNodeCount || 0) > 0)

function resolveDesignerType(type) {
  return type === 'CUSTOM' ? '自定义编排' : 'BPMN'
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
  leftId.value = 0
  rightId.value = 0
  errorMessage.value = ''
  if (!selectedProcessKey.value) {
    versions.value = []
    return
  }
  try {
    const { data } = await http.get(`/process-definition/versions/${selectedProcessKey.value}`)
    versions.value = data || []
    if (versions.value.length >= 2) {
      fillLatestPair()
    }
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

async function compareVersions() {
  errorMessage.value = ''
  loading.value = true
  try {
    const { data } = await http.get('/process-definition/compare', {
      params: { leftId: leftId.value, rightId: rightId.value }
    })
    compareResult.value = data
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '版本对比失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDefinitions().catch(() => {})
})
</script>

<style scoped>
.compare-filter-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.compare-side-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.compare-node-list {
  max-height: 480px;
  overflow: auto;
}
</style>
