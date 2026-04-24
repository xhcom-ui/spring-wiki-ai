<template>
  <div class="designer-shell fl-shell">
    <div class="designer-toolbar">
      <div class="designer-status">
        <span :class="['status-dot', statusClass]"></span>
        <span>{{ statusText }}</span>
      </div>
      <div class="designer-actions">
        <button type="button" class="ghost-btn" @click="zoomOut">缩小</button>
        <button type="button" class="ghost-btn" @click="resetZoom">100%</button>
        <button type="button" class="ghost-btn" @click="zoomIn">放大</button>
      </div>
    </div>

    <section class="fl-canvas-card">
      <div class="fl-canvas-head">
        <div>
          <p class="eyebrow">FlowLong Designer</p>
          <h3>流程编排画布</h3>
        </div>
        <div class="management-meta">
          <span class="meta-pill">缩放 {{ Math.round(zoom * 100) }}%</span>
          <span class="meta-pill">节点 {{ nodeCount }}</span>
          <span class="meta-pill">模型 {{ workflow.key || meta.processId }}</span>
        </div>
      </div>

      <div class="fl-stage" :class="{ 'fl-stage-readonly': readonly }" @wheel.prevent="handleWheelZoom">
        <div class="fl-stage-scale" :style="{ transform: `scale(${zoom})` }">
          <VendorWorkflow v-model="workflow.nodeConfig" />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import VendorWorkflow from '../flowlongDesignerVendor/scWorkflow/index.vue'
import {
  buildVendorWorkflowBpmnXml,
  createDefaultVendorWorkflow,
  createLeaveVendorWorkflow,
  parseStoredVendorWorkflow
} from '../../utils/flowlongDesignerAdapter'

const emit = defineEmits(['ready', 'changed', 'error'])

const props = defineProps({
  readonly: {
    type: Boolean,
    default: false
  }
})

const meta = ref({
  processId: 'process',
  processName: '新建流程'
})
const workflow = ref(createDefaultVendorWorkflow(meta.value))
const zoom = ref(1)
const isReady = ref(false)
const isDirty = ref(false)

let silentUpdate = false

const statusText = computed(() => {
  if (!isReady.value) {
    return 'FlowLong Designer 初始化中'
  }
  return isDirty.value ? '有未保存改动' : '画布已同步'
})

const statusClass = computed(() => {
  if (!isReady.value) {
    return 'status-waiting'
  }
  return isDirty.value ? 'status-dirty' : 'status-clean'
})

const nodeCount = computed(() => countNodes(workflow.value?.nodeConfig))

watch(
  workflow,
  () => {
    if (silentUpdate) {
      silentUpdate = false
      return
    }
    if (!isReady.value) {
      return
    }
    isDirty.value = true
    emit('changed', workflow.value)
  },
  { deep: true }
)

function countNodes(node) {
  if (!node || typeof node !== 'object') {
    return 0
  }
  let total = 1
  if (node.childNode) {
    total += countNodes(node.childNode)
  }
  if (Array.isArray(node.conditionNodes)) {
    total += node.conditionNodes.reduce((sum, item) => sum + countNodes(item), 0)
  }
  return total
}

function normalizeMeta(metadata = {}) {
  meta.value = {
    processId: metadata.processId || meta.value.processId || 'process',
    processName: metadata.processName || meta.value.processName || '新建流程'
  }
}

async function importSchema(schemaInput, markDirty = false, metadata = {}) {
  try {
    normalizeMeta(metadata)
    silentUpdate = true
    workflow.value = parseStoredVendorWorkflow(schemaInput, meta.value)
    workflow.value.key = meta.value.processId
    workflow.value.name = meta.value.processName
    isReady.value = true
    isDirty.value = Boolean(markDirty)
    emit('ready', workflow.value)
  } catch (error) {
    emit('error', error)
    throw error
  }
}

async function createNewDiagram(metadata = {}) {
  normalizeMeta(metadata)
  silentUpdate = true
  workflow.value = createDefaultVendorWorkflow(meta.value)
  isReady.value = true
  isDirty.value = false
  emit('ready', workflow.value)
}

async function createLeaveTemplate() {
  silentUpdate = true
  workflow.value = createLeaveVendorWorkflow(meta.value)
  isReady.value = true
  isDirty.value = true
  emit('ready', workflow.value)
}

async function getSchema() {
  return JSON.parse(JSON.stringify(workflow.value))
}

async function getXml(metadata = {}) {
  return buildVendorWorkflowBpmnXml(await getSchema(), {
    ...meta.value,
    ...metadata
  })
}

function triggerDownload(blob, fileName) {
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = fileName
  link.click()
  URL.revokeObjectURL(link.href)
}

async function downloadXml(fileName = 'process.bpmn') {
  triggerDownload(new Blob([await getXml()], { type: 'application/xml;charset=utf-8' }), fileName)
}

async function downloadSchema(fileName = 'process-design.json') {
  triggerDownload(new Blob([JSON.stringify(await getSchema(), null, 2)], { type: 'application/json;charset=utf-8' }), fileName)
}

function zoomIn() {
  zoom.value = Math.min(2, Number((zoom.value + 0.1).toFixed(2)))
}

function zoomOut() {
  zoom.value = Math.max(0.5, Number((zoom.value - 0.1).toFixed(2)))
}

function resetZoom() {
  zoom.value = 1
}

function handleWheelZoom(event) {
  const delta = Number(event?.deltaY || 0)
  if (!Number.isFinite(delta) || delta === 0) {
    return
  }
  if (delta > 0) {
    zoomOut()
    return
  }
  zoomIn()
}

defineExpose({
  createNewDiagram,
  createLeaveTemplate,
  importSchema,
  getSchema,
  getXml,
  downloadXml,
  downloadSchema,
  markClean() {
    isDirty.value = false
  }
})
</script>

<style scoped>
.fl-shell {
  display: grid;
  gap: 12px;
}

.fl-canvas-card {
  border: 1px solid #d8e1ec;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.fl-canvas-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px 12px;
  border-bottom: 1px solid #e4ebf3;
  background: #fff;
}

.fl-stage {
  overflow: auto;
  min-height: clamp(540px, 60vh, 760px);
  padding: 16px 18px 24px;
  background-color: #f7f9fc;
  background-image:
    linear-gradient(rgba(221, 229, 240, 0.82) 1px, transparent 1px),
    linear-gradient(90deg, rgba(221, 229, 240, 0.82) 1px, transparent 1px);
  background-size: 20px 20px, 20px 20px;
}

.fl-stage-scale {
  transform-origin: top center;
  width: max-content;
  min-width: 100%;
}

.fl-stage-readonly {
  pointer-events: none;
}

@media (max-width: 900px) {
  .fl-canvas-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .fl-stage {
    min-height: 460px;
    padding-inline: 10px;
  }
}
</style>
