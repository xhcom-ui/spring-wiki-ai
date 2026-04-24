<template>
  <div class="page-grid">
    <section class="hero-workbench">
      <article class="hero-workbench-copy">
        <p class="eyebrow">Studio Guide</p>
        <h3>流程设计工作台</h3>
        <p class="muted-text">
          现在设计器右侧配置、节点表单 Key、待办页和完成页，都会直接联动到运行时待办页和业务回执页。
        </p>
        <div class="hero-action-row">
          <button type="button" class="primary-btn" @click="createProcess" :disabled="loading">新建空白流程</button>
          <RouterLink class="secondary-btn hero-link" to="/runtime-catalog">打开业务页目录</RouterLink>
          <button type="button" class="ghost-btn" :disabled="catalogLoading" @click="refreshCatalogSummary">
            {{ catalogLoading ? '刷新中...' : '刷新目录摘要' }}
          </button>
        </div>
        <div class="management-meta">
          <span class="meta-pill">流程定义 {{ processDefinitions.length }}</span>
          <span class="meta-pill">激活版本 {{ activeDefinitionCount }}</span>
          <span class="meta-pill">目录条目 {{ pageCatalog.length + formCatalog.length }}</span>
          <span class="meta-pill">{{ hasUnsavedChanges ? '未保存改动' : '画布已同步' }}</span>
        </div>
      </article>

      <aside class="hero-workbench-side">
        <div class="hero-side-card">
          <span class="hero-side-label">表单目录</span>
          <strong>{{ formCatalog.length }}</strong>
          <p class="muted-text">设计器里的表单 Key 下拉和推荐页面都从这里来。</p>
        </div>
        <div class="hero-side-card">
          <span class="hero-side-label">页面目录</span>
          <strong>{{ pageCatalog.length }}</strong>
          <p class="muted-text">节点 `todoPage / donePage` 会直接映射到运行时业务页。</p>
        </div>
        <div class="hero-side-card accent-card">
          <span class="hero-side-label">设计器引擎</span>
          <strong>{{ designerFamily === 'flowlong' ? 'Flowlong + BPMN' : 'BPMN + Flowlong' }}</strong>
          <p class="muted-text">旧 BPMN 与新 Flowlong 双向兼容，历史流程可继续编辑，新流程可直接回显设计 JSON。</p>
        </div>
      </aside>
    </section>

    <section class="content-grid studio-two-column">
      <aside class="panel-card definition-panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Process Registry</p>
            <h3>流程定义</h3>
          </div>
          <button type="button" class="secondary-btn" @click="createProcess" :disabled="loading">
            新建
          </button>
        </div>

        <div class="registry-controls">
          <label class="registry-switch">
            <input v-model="activeOnly" type="checkbox" />
            <span>只看激活版本</span>
          </label>

          <label class="filter-grow registry-search">
            <span>搜索流程</span>
            <input v-model.trim="keyword" type="text" placeholder="按流程名称、Key、版本或状态搜索" />
          </label>
        </div>

        <section class="stack-list catalog-preview-list">
          <div class="workspace-chip">
            <span class="workspace-chip-label">常用表单</span>
            <strong>{{ formCatalog[0]?.label || '暂无' }}</strong>
            <span class="muted-text">{{ formCatalog[0]?.key || '请先在业务页目录里配置' }}</span>
          </div>
          <div class="workspace-chip">
            <span class="workspace-chip-label">待办页样例</span>
            <strong>{{ todoPageCatalog[0]?.label || '暂无' }}</strong>
            <span class="muted-text">{{ todoPageCatalog[0]?.key || '请先配置待办页目录' }}</span>
          </div>
          <div class="workspace-chip">
            <span class="workspace-chip-label">完成页样例</span>
            <strong>{{ donePageCatalog[0]?.label || '暂无' }}</strong>
            <span class="muted-text">{{ donePageCatalog[0]?.key || '请先配置完成页目录' }}</span>
          </div>
        </section>

        <div class="stack-list">
          <button
            v-for="definition in filteredDefinitions"
            :key="definition.id"
            type="button"
            :class="['definition-card', { selected: definition.id === selectedProcessId }]"
            @click="loadDefinition(definition.id)"
          >
            <div class="definition-title">
              <strong>{{ definition.processName }}</strong>
              <span :class="['status-pill', definition.status === 'ACTIVE' ? 'success-pill' : 'muted-pill']">
                {{ definition.status }}
              </span>
            </div>
            <p>{{ definition.processKey }}</p>
            <small>v{{ definition.version }} · {{ formatDate(definition.updatedAt || definition.createdAt) }}</small>
          </button>
          <div v-if="!filteredDefinitions.length" class="empty-state">
            {{ keyword ? '没有匹配的流程定义，试试调整关键词。' : '还没有流程定义，先新建一个。' }}
          </div>
        </div>
      </aside>

      <div class="studio-content">
        <section class="panel-card">
          <div class="form-grid">
            <label>
              <span>流程 Key</span>
              <input
                v-model.trim="processForm.processKey"
                type="text"
                placeholder="leave_process"
                :disabled="Boolean(selectedProcessId)"
              />
            </label>
            <label>
              <span>流程名称</span>
              <input v-model.trim="processForm.processName" type="text" placeholder="请假审批流程" />
            </label>
          </div>

          <p class="hint">
            {{ designerFamily === 'flowlong'
              ? (selectedProcessId ? '当前使用 Flowlong 设计器，保存时会同时写入设计 JSON 和运行时 BPMN。' : '新建 Flowlong 流程时会同时保存设计 JSON 与可执行 BPMN XML。')
              : (selectedProcessId ? '当前使用 BPMN 设计器，兼容旧流程定义和经典节点配置。' : '新建 BPMN 流程时会保存经典 XML，并保留后续切换到 Flowlong 的能力。') }}
          </p>

          <div class="action-row">
            <button type="button" class="primary-btn" @click="saveProcess" :disabled="loading">
              {{ loading ? '处理中...' : selectedProcessId ? '保存更新' : '保存新版本' }}
            </button>
            <div class="designer-family-toggle">
              <button
                type="button"
                :class="['secondary-btn', { active: designerFamily === 'flowlong' }]"
                @click="switchDesignerFamily('flowlong')"
              >
                Flowlong
              </button>
              <button
                type="button"
                :class="['secondary-btn', { active: designerFamily === 'bpmn' }]"
                @click="switchDesignerFamily('bpmn')"
              >
                BPMN
              </button>
            </div>
            <button type="button" class="secondary-btn" @click="activateProcess" :disabled="loading || !selectedProcessId">
              激活版本
            </button>
            <button type="button" class="secondary-btn" @click="downloadXml" :disabled="loading">
              导出 XML
            </button>
            <label class="upload-btn">
              导入 XML
              <input ref="fileInputRef" type="file" accept=".bpmn,.xml" @change="importLocalXml" />
            </label>
            <button type="button" class="danger-btn" @click="deleteProcess" :disabled="loading || !selectedProcessId">
              删除
            </button>
          </div>

          <div v-if="statusMessage" class="feedback success">{{ statusMessage }}</div>
          <div v-if="errorMessage" class="feedback error">{{ errorMessage }}</div>
        </section>

        <Suspense>
          <template #default>
            <div class="designer-stack">
              <FlowlongDesigner
                v-if="designerFamily === 'flowlong'"
                ref="flowlongDesignerRef"
                class="designer-card flowlong-designer-card"
                @changed="handleDesignerChange"
                @error="handleDesignerError"
              />
              <ProcessDesigner
                v-else
                ref="classicDesignerRef"
                v-model:ui-mode="classicCanvasMode"
                :show-meta-badges="showMetaBadges"
                class="designer-card classic-designer-card"
                @changed="handleDesignerChange"
                @error="handleDesignerError"
              />
            </div>
          </template>
          <template #fallback>
            <section class="designer-loading-card">
              <p class="eyebrow">Designer Engine</p>
              <h3>流程设计器加载中</h3>
              <p class="muted-text">Flowlong 和经典 BPMN 设计器会在进入当前页面后按需加载。</p>
            </section>
          </template>
        </Suspense>
      </div>
    </section>
  </div>
</template>

<script setup>
import { Suspense, computed, defineAsyncComponent, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { getCurrentUser, http } from '../api/http'
import {
  ensureTaskPageCatalogLoaded,
  getTaskFormCatalogSnapshot,
  getTaskPageCatalogSnapshot
} from '../runtime/taskPageCatalog'

const ProcessDesigner = defineAsyncComponent(() => import('../components/ProcessDesigner.vue'))
const FlowlongDesigner = defineAsyncComponent(() => import('../components/flowlong-designer/FlowlongDesigner.vue'))
const DESIGNER_FAMILY_KEY = 'activiti_process_designer_family'
const CLASSIC_MODE_KEY = 'activiti_process_classic_mode'
const DESIGNER_BADGE_KEY = 'activiti_process_designer_badges'

const route = useRoute()
const router = useRouter()
const flowlongDesignerRef = ref(null)
const classicDesignerRef = ref(null)
const fileInputRef = ref(null)
const loading = ref(false)
const activeOnly = ref(false)
const keyword = ref('')
const statusMessage = ref('')
const errorMessage = ref('')
const selectedProcessId = ref(null)
const processDefinitions = ref([])
const catalogLoading = ref(false)
const formCatalog = ref([])
const pageCatalog = ref([])
const hasUnsavedChanges = ref(false)
const designerFamily = ref(localStorage.getItem(DESIGNER_FAMILY_KEY) || 'flowlong')
const classicCanvasMode = ref(localStorage.getItem(CLASSIC_MODE_KEY) || 'studio')
const showMetaBadges = ref(localStorage.getItem(DESIGNER_BADGE_KEY) !== '0')
const sharedXmlCache = ref('')

const processForm = reactive({
  processKey: '',
  processName: ''
})

const filteredDefinitions = computed(() => {
  const needle = keyword.value.trim().toLowerCase()
  return processDefinitions.value.filter((item) => {
    if (activeOnly.value && item.status !== 'ACTIVE') {
      return false
    }
    if (!needle) {
      return true
    }
    return [
      item.processName,
      item.processKey,
      item.status,
      `v${item.version}`
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(needle))
  })
})
const activeDefinitionCount = computed(() => processDefinitions.value.filter((item) => item.status === 'ACTIVE').length)
const todoPageCatalog = computed(() => pageCatalog.value.filter((item) => item.mode === 'todo'))
const donePageCatalog = computed(() => pageCatalog.value.filter((item) => item.mode === 'done'))

function resetMessages() {
  statusMessage.value = ''
  errorMessage.value = ''
}

function formatDate(value) {
  if (!value) {
    return '刚刚'
  }
  return new Date(value).toLocaleString()
}

function normalizeProcessKey() {
  const source = processForm.processKey || processForm.processName || 'process'
  return source
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '') || 'process'
}

function buildDiagramMeta(overrides = {}) {
  return {
    processId: overrides.processId || normalizeProcessKey(),
    processName: overrides.processName || processForm.processName || '新建流程'
  }
}

function getDesignerRefByFamily(family = designerFamily.value) {
  return family === 'flowlong' ? flowlongDesignerRef.value : classicDesignerRef.value
}

function getActiveDesignerRef() {
  return getDesignerRefByFamily(designerFamily.value)
}

async function switchDesignerFamily(nextFamily) {
  if (nextFamily === designerFamily.value) {
    return
  }
  resetMessages()
  try {
    const currentRef = getActiveDesignerRef()
    let xml = ''
    if (currentRef?.syncProcessMeta) {
      await currentRef.syncProcessMeta(buildDiagramMeta())
    }
    if (currentRef?.getXml) {
      xml = await currentRef.getXml()
    }
    sharedXmlCache.value = xml || sharedXmlCache.value
    designerFamily.value = nextFamily
    await nextTick()
    const nextRef = getActiveDesignerRef()
    if (sharedXmlCache.value && nextRef?.importXml) {
      if (nextFamily === 'flowlong') {
        await nextRef.importXml(sharedXmlCache.value, buildDiagramMeta(), hasUnsavedChanges.value)
      } else {
        await nextRef.importXml(sharedXmlCache.value, hasUnsavedChanges.value)
      }
    } else if (nextRef?.createNewDiagram && !selectedProcessId.value) {
      await nextRef.createNewDiagram(buildDiagramMeta())
    }
    if (hasUnsavedChanges.value) {
      statusMessage.value = `已切换到 ${nextFamily === 'flowlong' ? 'Flowlong' : 'BPMN'} 设计器，并同步当前画布`
    }
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '切换设计器失败'
  }
}

function confirmDiscardChanges(actionLabel = '离开当前页面') {
  if (!hasUnsavedChanges.value) {
    return true
  }
  return window.confirm(`当前流程画布还有未保存改动，确认继续${actionLabel}吗？`)
}

function handleBeforeUnload(event) {
  if (!hasUnsavedChanges.value) {
    return
  }
  event.preventDefault()
  event.returnValue = ''
}

async function fetchProcessDefinitions() {
  const { data } = await http.get('/process-definition/all')
  processDefinitions.value = data || []
}

async function refreshCatalogSummary() {
  catalogLoading.value = true
  try {
    await ensureTaskPageCatalogLoaded(true)
    formCatalog.value = getTaskFormCatalogSnapshot()
    pageCatalog.value = getTaskPageCatalogSnapshot()
  } finally {
    catalogLoading.value = false
  }
}

async function createProcess() {
  if (!confirmDiscardChanges('新建流程')) {
    return
  }
  resetMessages()
  selectedProcessId.value = null
  processForm.processKey = ''
  processForm.processName = ''
  sharedXmlCache.value = ''
  await nextTick()
  await getActiveDesignerRef()?.createNewDiagram({ processId: 'process', processName: '新建流程' })
  sharedXmlCache.value = await getActiveDesignerRef()?.getXml?.() || ''
  getActiveDesignerRef()?.markClean()
  hasUnsavedChanges.value = false
  statusMessage.value = '已创建空白画布'
}

async function loadDefinition(id) {
  if (selectedProcessId.value !== id && !confirmDiscardChanges('切换流程')) {
    return false
  }
  resetMessages()
  loading.value = true
  try {
    const { data } = await http.get(`/process-definition/${id}`)
    selectedProcessId.value = data.id
    processForm.processKey = data.processKey
    processForm.processName = data.processName
    designerFamily.value = data.designerType === 'FLOWLONG' ? 'flowlong' : 'bpmn'
    await nextTick()
    if (data.designerType === 'FLOWLONG' && data.designerJson) {
      await flowlongDesignerRef.value?.loadDefinition(data.designerJson, buildDiagramMeta({
        processId: data.processKey,
        processName: data.processName
      }))
      sharedXmlCache.value = await flowlongDesignerRef.value?.getXml(buildDiagramMeta({
        processId: data.processKey,
        processName: data.processName
      }))
      statusMessage.value = `已加载 ${data.processName}`
    } else if (data.bpmnXml) {
      await classicDesignerRef.value?.importXml(data.bpmnXml, false)
      sharedXmlCache.value = data.bpmnXml
      statusMessage.value = `已加载 ${data.processName}，兼容旧 BPMN，可随时切换到 Flowlong`
    } else {
      await getActiveDesignerRef()?.createNewDiagram(buildDiagramMeta({
        processId: data.processKey,
        processName: data.processName
      }))
      sharedXmlCache.value = await getActiveDesignerRef()?.getXml?.() || ''
      statusMessage.value = `已加载 ${data.processName}`
    }
    getActiveDesignerRef()?.markClean()
    hasUnsavedChanges.value = false
    return true
  } catch (error) {
    errorMessage.value = error.normalizedMessage
    return false
  } finally {
    loading.value = false
  }
}

async function syncDefinitionFromRoute() {
  const processId = Number(route.query.processId || 0)
  const focusElementId = String(route.query.focusElementId || '')
  if (!processId) {
    return
  }
  if (selectedProcessId.value !== processId) {
    const loaded = await loadDefinition(processId)
    if (!loaded) {
      await router.replace({
        path: route.path,
        query: {
          ...route.query,
          processId: selectedProcessId.value || undefined,
          focusElementId: undefined
        }
      })
      return
    }
  }
  if (focusElementId) {
    await nextTick()
    const focused = getActiveDesignerRef()?.focusElementById?.(focusElementId)
    if (focused) {
      statusMessage.value = `已定位到节点 ${focusElementId}`
    }
  }
}

async function saveProcess() {
  resetMessages()
  processForm.processKey = normalizeProcessKey()
  if (!processForm.processName.trim()) {
    errorMessage.value = '请输入流程名称'
    return
  }

  loading.value = true
  try {
    await getActiveDesignerRef()?.syncProcessMeta(buildDiagramMeta())
    const bpmnXml = await getActiveDesignerRef().getXml()
    sharedXmlCache.value = bpmnXml
    const currentUser = getCurrentUser()
    const payload = {
      processKey: processForm.processKey,
      processName: processForm.processName.trim(),
      bpmnXml,
      designerType: designerFamily.value === 'flowlong' ? 'FLOWLONG' : 'CLASSIC',
      designerJson: designerFamily.value === 'flowlong' ? await flowlongDesignerRef.value.getDesignerJson() : null,
      userId: currentUser?.id ?? 1
    }
    const request = selectedProcessId.value
      ? http.put(`/process-definition/update/${selectedProcessId.value}`, payload)
      : http.post('/process-definition/save', payload)
    const { data } = await request
    selectedProcessId.value = data.id
    processForm.processKey = data.processKey
    processForm.processName = data.processName
    getActiveDesignerRef()?.markClean()
    hasUnsavedChanges.value = false
    await fetchProcessDefinitions()
    statusMessage.value = '流程已保存'
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

async function activateProcess() {
  if (!selectedProcessId.value) {
    return
  }
  resetMessages()
  loading.value = true
  try {
    const currentUser = getCurrentUser()
    await http.put(`/process-definition/activate/${selectedProcessId.value}`, {
      userId: currentUser?.id ?? 1
    })
    await fetchProcessDefinitions()
    statusMessage.value = '当前版本已激活'
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

async function deleteProcess() {
  if (!selectedProcessId.value) {
    return
  }
  resetMessages()
  loading.value = true
  try {
    await http.delete(`/process-definition/delete/${selectedProcessId.value}`)
    await fetchProcessDefinitions()
    await createProcess()
    statusMessage.value = '流程已删除'
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

async function downloadXml() {
  const fileName = `${normalizeProcessKey() || 'process'}.bpmn`
  await getActiveDesignerRef()?.downloadXml(fileName)
}

async function importLocalXml(event) {
  const [file] = event.target.files || []
  if (!file) {
    return
  }
  if (!confirmDiscardChanges('导入新 XML')) {
    event.target.value = ''
    return
  }

  resetMessages()
  try {
    const xml = await file.text()
    const derivedName = file.name.replace(/\.(bpmn|xml)$/i, '')
    if (!selectedProcessId.value) {
      processForm.processName = processForm.processName || derivedName
      processForm.processKey = processForm.processKey || normalizeProcessKey()
    }
    if (designerFamily.value === 'flowlong') {
      await flowlongDesignerRef.value?.importXml(xml, buildDiagramMeta(), true)
      sharedXmlCache.value = await flowlongDesignerRef.value?.getXml(buildDiagramMeta())
    } else {
      await classicDesignerRef.value?.importXml(xml, true)
      sharedXmlCache.value = xml
    }
    hasUnsavedChanges.value = true
    statusMessage.value = `已导入 ${file.name}，请检查节点配置后保存`
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '导入 XML 失败'
  } finally {
    event.target.value = ''
  }
}

function handleDesignerChange() {
  resetMessages()
  hasUnsavedChanges.value = true
}

function handleDesignerError(error) {
  errorMessage.value = error.message || '画布加载失败'
}

onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  await Promise.all([fetchProcessDefinitions(), refreshCatalogSummary()])
  await nextTick()
  if (!selectedProcessId.value) {
    await getActiveDesignerRef()?.createNewDiagram(buildDiagramMeta())
  }
  await syncDefinitionFromRoute()
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

onBeforeRouteLeave(() => confirmDiscardChanges('离开流程设计页'))

watch(
  () => route.fullPath,
  async () => {
    await syncDefinitionFromRoute()
  }
)

watch(designerFamily, (value) => {
  localStorage.setItem(DESIGNER_FAMILY_KEY, value)
})

watch(classicCanvasMode, (value) => {
  localStorage.setItem(CLASSIC_MODE_KEY, value)
})

watch(showMetaBadges, (value) => {
  localStorage.setItem(DESIGNER_BADGE_KEY, value ? '1' : '0')
})
</script>

<style scoped>
.definition-panel {
  gap: 18px;
}

.registry-controls {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 14px;
  border-radius: 20px;
  border: 1px solid rgba(82, 103, 132, 0.1);
  background: rgba(255, 255, 255, 0.78);
}

.registry-switch {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  min-height: 22px;
  color: #304761;
  font-size: 14px;
  font-weight: 600;
}

.registry-switch input {
  width: 16px;
  height: 16px;
  margin: 0;
  accent-color: #2f7de1;
}

.registry-search {
  gap: 10px;
}

.registry-search span {
  font-size: 13px;
  font-weight: 700;
  color: #5c738f;
}

.registry-search input {
  min-height: 46px;
  padding: 0 16px;
  border-radius: 16px;
  border: 1px solid rgba(82, 103, 132, 0.14);
  background: rgba(255, 255, 255, 0.96);
  color: #243754;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.registry-search input::placeholder {
  color: #92a3b8;
}

.designer-family-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px;
  border-radius: 999px;
  background: rgba(28, 53, 88, 0.08);
}

.designer-family-toggle .secondary-btn {
  margin: 0;
}

.designer-family-toggle .secondary-btn.active {
  border-color: rgba(41, 106, 225, 0.36);
  background: linear-gradient(135deg, #1b3a67, #2f7de1);
  color: #fff;
}

.designer-stack {
  display: grid;
}

.designer-stack :deep(.flowlong-designer-card) {
  min-height: 0;
}

.designer-stack :deep(.classic-designer-card) {
  min-height: 840px;
}

.designer-loading-card {
  min-height: 680px;
  border: 1px solid rgba(77, 98, 127, 0.1);
  border-radius: 28px;
  background:
    radial-gradient(circle at top left, rgba(41, 79, 129, 0.12), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(244, 248, 253, 0.96));
  box-shadow: 0 18px 50px rgba(20, 35, 57, 0.08);
  padding: 28px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 12px;
}

.designer-loading-card h3,
.designer-loading-card p {
  margin: 0;
}

.catalog-preview-list {
  gap: 10px;
}

@media (max-width: 900px) {
  .registry-switch {
    justify-content: flex-start;
  }
}
</style>
