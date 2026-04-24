<template>
  <div class="flowlong-adapter">
    <el-affix :offset="16" class="toolbar-affix">
      <div class="btn-container">
        <div class="designer-status">
          <span :class="['status-dot', statusClass]"></span>
          <span>{{ statusText }}</span>
        </div>
        <el-button type="primary" @click="drawer = true">查看 JSON</el-button>
        <el-button type="primary" @click="saveAsPng">保存图片</el-button>
        <div class="slider">
          <el-button type="primary" @click="adjustZoom(-0.1)">
            <el-icon><Minus /></el-icon>
          </el-button>
          <el-slider v-model="zoom" :marks="marks" :min="0.1" :max="5" :step="0.1" />
          <el-button type="primary" @click="adjustZoom(0.1)">
            <el-icon><Plus /></el-icon>
          </el-button>
        </div>
      </div>
    </el-affix>

    <div
      ref="canvasShellRef"
      class="affix-container"
      :style="{ transform: `scale(${zoom})`, transformOrigin: '0 0' }"
      @wheel.prevent="handleWheel"
    >
      <ScWorkflow
        v-if="workflow.nodeConfig"
        id="content-to-capture"
        class="workflow"
        v-model="workflow.nodeConfig"
      />
    </div>

    <el-drawer
      v-model="drawer"
      size="500px"
      class="drawer"
      append-to-body
      :modal="false"
      :with-header="false"
    >
      <div class="drawer-body">
        <div class="drawer-toolbar">
          <el-button type="primary" plain @click="copyPrettyJson">复制格式化后的 JSON</el-button>
          <el-button type="primary" plain @click="copyJson">复制压缩后的 JSON</el-button>
          <el-button type="primary" plain @click="drawer = false">关闭弹窗</el-button>
        </div>
        <JsonEditorView
          class="editor"
          language="zh-CN"
          mode="view"
          v-model="jsonPreview"
        />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, provide, reactive, ref, watch } from 'vue'
import html2canvas from 'html2canvas'
import JsonEditorView from '../common/JsonEditorView.vue'
import ScWorkflow from './scWorkflow/index.vue'
import {
  buildFlowlongBpmnXml,
  createFlowlongWorkflow,
  exportFlowlongSchemaJson,
  flowlongWorkflowToCustomSchema,
  normalizeFlowlongWorkflow,
  parseStoredFlowlongSchema
} from '../../utils/flowlongAdapter'

const emit = defineEmits(['ready', 'changed', 'error'])

const props = defineProps({
  readonly: {
    type: Boolean,
    default: false
  },
  formCatalogs: {
    type: Array,
    default: () => []
  }
})

const workflow = ref(createFlowlongWorkflow())
const jsonPreview = ref(createFlowlongWorkflow())
const canvasShellRef = ref(null)
const zoom = ref(1)
const marks = reactive({
  0.1: 'min',
  1: '1',
  2: '2',
  3: '3',
  4: '4',
  5: 'max'
})
const drawer = ref(false)
const meta = ref({
  processId: 'process',
  processName: '新建流程'
})
const isReady = ref(false)
const isDirty = ref(false)
let silentUpdate = false

provide('flowlongFormCatalogs', computed(() => props.formCatalogs))
provide('flowlongReadonly', computed(() => props.readonly))

const statusText = computed(() => {
  if (!isReady.value) {
    return 'Flowlong 设计器初始化中'
  }
  return isDirty.value ? '有未保存改动' : '已同步'
})
const statusClass = computed(() => (isDirty.value ? 'status-dirty' : isReady.value ? 'status-clean' : 'status-waiting'))

watch(
  () => workflow.value?.nodeConfig,
  () => {
    if (silentUpdate) {
      silentUpdate = false
      return
    }
    if (!isReady.value) {
      return
    }
    syncJsonPreview()
    isDirty.value = true
    emit('changed', workflow.value)
  },
  { deep: true }
)

function syncMeta(nextMeta = {}) {
  meta.value = {
    processId: nextMeta.processId || meta.value.processId || 'process',
    processName: nextMeta.processName || meta.value.processName || '新建流程'
  }
}

function normalizeCurrentWorkflow() {
  return normalizeFlowlongWorkflow({
    ...workflow.value,
    key: meta.value.processId,
    name: meta.value.processName
  }, meta.value)
}

function syncJsonPreview() {
  jsonPreview.value = JSON.parse(JSON.stringify(workflow.value))
}

function clampZoom(value) {
  if (value < 0.1) {
    return 0.1
  }
  if (value > 5) {
    return 5
  }
  return Number(value.toFixed(1))
}

function adjustZoom(delta) {
  zoom.value = clampZoom(zoom.value + delta)
}

function handleWheel(event) {
  if (!canvasShellRef.value?.contains(event.target)) {
    return
  }
  event.preventDefault()
  adjustZoom(event.deltaY > 0 ? -0.1 : 0.1)
}

async function copyPrettyJson() {
  await navigator.clipboard.writeText(JSON.stringify(workflow.value, null, 2))
}

async function copyJson() {
  await navigator.clipboard.writeText(JSON.stringify(workflow.value))
}

async function saveAsPng() {
  const element = document.getElementById('content-to-capture')
  const container = canvasShellRef.value
  if (!element || !container) {
    return
  }
  const previousTransform = container.style.transform
  container.style.transform = 'scale(1)'
  try {
    const canvas = await html2canvas(element, {
      backgroundColor: '#efefef'
    })
    const image = canvas.toDataURL('image/png').replace('image/png', 'image/octet-stream')
    const link = document.createElement('a')
    link.download = `flowlong-${Date.now()}.png`
    link.href = image
    link.click()
  } finally {
    container.style.transform = previousTransform
  }
}

async function createNewDiagram(nextMeta = {}) {
  syncMeta(nextMeta)
  silentUpdate = true
  workflow.value = createFlowlongWorkflow(meta.value)
  syncJsonPreview()
  isReady.value = true
  isDirty.value = false
  emit('ready', workflow.value)
}

async function importSchema(rawValue, markDirty = false, nextMeta = {}) {
  syncMeta(nextMeta)
  try {
    silentUpdate = true
    workflow.value = parseStoredFlowlongSchema(rawValue, meta.value)
    workflow.value.key = meta.value.processId
    workflow.value.name = meta.value.processName
    syncJsonPreview()
    isReady.value = true
    isDirty.value = Boolean(markDirty)
    emit('ready', workflow.value)
  } catch (error) {
    emit('error', error)
    throw error
  }
}

async function getSchema() {
  workflow.value = normalizeCurrentWorkflow()
  syncJsonPreview()
  return flowlongWorkflowToCustomSchema(workflow.value, meta.value)
}

async function getXml(nextMeta = {}) {
  syncMeta(nextMeta)
  workflow.value = normalizeCurrentWorkflow()
  syncJsonPreview()
  return buildFlowlongBpmnXml(workflow.value, meta.value)
}

function triggerDownload(content, filename, contentType) {
  const blob = new Blob([content], { type: contentType })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

async function downloadSchema(filename = 'process-design.json') {
  workflow.value = normalizeCurrentWorkflow()
  syncJsonPreview()
  triggerDownload(exportFlowlongSchemaJson(workflow.value, meta.value), filename, 'application/json;charset=utf-8')
}

async function downloadXml(filename = 'process.bpmn') {
  triggerDownload(await getXml(meta.value), filename, 'application/xml;charset=utf-8')
}

function markClean() {
  isDirty.value = false
}

defineExpose({
  createNewDiagram,
  importSchema,
  getSchema,
  getXml,
  downloadSchema,
  downloadXml,
  markClean
})
</script>

<style scoped>
:deep(:root) {
  --el-drawer-padding-primary: 0;
}

.flowlong-adapter {
  min-height: 780px;
}

.toolbar-affix {
  width: 100%;
  height: 74px;
  z-index: 15;
}

.btn-container {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 56px;
  padding: 10px 16px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(10px);
}

.designer-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-right: 4px;
  padding: 0 10px;
  color: #334155;
  font-size: 13px;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #94a3b8;
}

.status-clean {
  background: #10b981;
}

.status-dirty {
  background: #f59e0b;
}

.status-waiting {
  background: #94a3b8;
}

.slider {
  margin-left: auto;
  width: 320px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.affix-container {
  display: flex;
  justify-content: center;
  min-height: 720px;
  padding: 28px 18px 18px;
  background: #efefef;
  border-radius: 24px;
  overflow: auto;
}

.workflow {
  padding: 10px;
}

.drawer-body {
  height: 100vh;
}

.drawer-toolbar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding: 10px;
  background-color: #3883fa;
}

.editor {
  width: 500px;
  height: calc(100vh - 56px);
}

:deep(.editor .jsoneditor-poweredBy),
:deep(.editor .jsoneditor-transform),
:deep(.editor .jsoneditor-repair),
:deep(.editor .full-screen) {
  display: none !important;
}

:deep(.jsoneditor-menu > button.jsoneditor-copy) {
  background-position: -48px 0;
}

:deep(.el-drawer__body) {
  padding: 0 !important;
}

@media (max-width: 1024px) {
  .btn-container {
    flex-wrap: wrap;
  }

  .slider {
    margin-left: 0;
    width: 100%;
  }

  .editor {
    width: 100%;
  }
}
</style>
