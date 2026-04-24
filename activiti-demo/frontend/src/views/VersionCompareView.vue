<template>
  <div class="page-grid">
    <section class="panel-card">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Version Inspector</p>
          <h3>流程版本历史对比</h3>
        </div>
      </div>

      <div class="form-grid">
        <label>
          <span>流程 Key</span>
          <select v-model="selectedProcessKey" @change="fetchVersions">
            <option value="">请选择</option>
            <option v-for="key in processKeys" :key="key" :value="key">{{ key }}</option>
          </select>
        </label>
        <label>
          <span>左侧版本</span>
          <select v-model.number="leftId">
            <option :value="0">请选择</option>
            <option v-for="version in versions" :key="version.id" :value="version.id">
              v{{ version.version }} · {{ version.processName }}
            </option>
          </select>
        </label>
        <label>
          <span>右侧版本</span>
          <select v-model.number="rightId">
            <option :value="0">请选择</option>
            <option v-for="version in versions" :key="version.id" :value="version.id">
              v{{ version.version }} · {{ version.processName }}
            </option>
          </select>
        </label>
      </div>

      <div class="action-row">
        <button type="button" class="primary-btn" :disabled="!leftId || !rightId || loading" @click="compareVersions">
          执行对比
        </button>
      </div>
      <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>
    </section>

    <section v-if="compareResult" class="content-grid two-column">
      <article class="panel-card">
        <h3>对比摘要</h3>
        <div class="stack-list">
          <div class="list-item"><strong>流程 Key 一致</strong><span>{{ compareResult.sameProcessKey ? '是' : '否' }}</span></div>
          <div class="list-item"><strong>版本差值</strong><span>{{ compareResult.versionGap }}</span></div>
          <div class="list-item"><strong>变化行数</strong><span>{{ compareResult.changedLineCount }}</span></div>
          <div class="list-item"><strong>左侧行数</strong><span>{{ compareResult.leftLineCount }}</span></div>
          <div class="list-item"><strong>右侧行数</strong><span>{{ compareResult.rightLineCount }}</span></div>
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
              <td>{{ row.left }}</td>
              <td>{{ row.right }}</td>
            </tr>
          </tbody>
        </table>
      </article>

      <article class="panel-card">
        <h3>版本元数据</h3>
        <div class="stack-list">
          <div class="list-item"><strong>左侧版本</strong><span>v{{ compareResult.left.version }} / {{ compareResult.left.status }}</span></div>
          <div class="list-item"><strong>右侧版本</strong><span>v{{ compareResult.right.version }} / {{ compareResult.right.status }}</span></div>
          <div class="list-item"><strong>左侧部署</strong><span>{{ compareResult.left.deploymentId }}</span></div>
          <div class="list-item"><strong>右侧部署</strong><span>{{ compareResult.right.deploymentId }}</span></div>
        </div>
      </article>
    </section>

    <section v-if="compareResult" class="content-grid two-column">
      <article class="panel-card">
        <h3>左侧 XML</h3>
        <pre class="code-block">{{ compareResult.leftXml }}</pre>
      </article>
      <article class="panel-card">
        <h3>右侧 XML</h3>
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
const selectedProcessKey = ref('')
const leftId = ref(0)
const rightId = ref(0)
const compareResult = ref(null)
const errorMessage = ref('')

const processKeys = computed(() => [...new Set((definitions.value || []).map((item) => item.processKey))])

async function fetchDefinitions() {
  const { data } = await http.get('/process-definition/all')
  definitions.value = data || []
}

async function fetchVersions() {
  compareResult.value = null
  leftId.value = 0
  rightId.value = 0
  if (!selectedProcessKey.value) {
    versions.value = []
    return
  }
  const { data } = await http.get(`/process-definition/versions/${selectedProcessKey.value}`)
  versions.value = data || []
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

onMounted(fetchDefinitions)
</script>
