<template>
  <div class="flowlong-shell">
    <div class="flowlong-toolbar">
      <div class="flowlong-toolbar-left">
        <span class="status-dot"></span>
        <div class="flowlong-title-block">
          <strong>Flowlong Designer</strong>
          <span class="flowlong-hint">审批 / 抄送 / 条件分支</span>
        </div>
        <div class="flowlong-meta-strip">
          <span class="flowlong-meta-pill">结构清晰</span>
          <span class="flowlong-meta-pill">支持人员选择</span>
        </div>
      </div>
      <div class="flowlong-toolbar-actions">
        <span class="flowlong-zoom-pill">{{ Math.round(canvasZoom * 100) }}%</span>
        <button type="button" class="flowlong-toolbar-btn" @click="zoomOut">缩小</button>
        <button type="button" class="flowlong-toolbar-btn active" @click="resetZoom">重置</button>
        <button type="button" class="flowlong-toolbar-btn" @click="zoomIn">放大</button>
        <button type="button" class="flowlong-toolbar-btn accent" @click="showJson = true">查看 JSON</button>
      </div>
    </div>

    <div
      ref="canvasWrapRef"
      class="flowlong-canvas-wrap"
      :class="{ 'is-panning': isPanning }"
      @wheel.prevent="handleWheelZoom"
      @mousedown="startPan"
      @mousemove="handlePan"
      @mouseup="stopPan"
      @mouseleave="stopPan">
      <div class="flowlong-stage-panel">
        <div class="flowlong-stage-head">
          <small>拖动画布空白区域，点击中间加号继续扩展审批链</small>
        </div>
        <div class="flowlong-canvas-viewport">
          <div class="flowlong-canvas-scale" :style="canvasScaleStyle">
            <ScWorkflow v-model="definition.nodeConfig" class="flowlong-canvas" />
          </div>
        </div>
      </div>
    </div>

    <el-drawer v-model="showJson" title="流程 JSON" size="560px" append-to-body>
      <pre class="flowlong-json-preview">{{ formattedJson }}</pre>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import ScWorkflow from './scWorkflow/index.vue'
import {
  buildFlowlongBpmn,
  createDefaultFlowDefinition,
  normalizeFlowDefinition,
  parseBpmnToFlowDefinition
} from './flowlongBpmn'

const emit = defineEmits(['changed', 'error'])

const definition = ref(createDefaultFlowDefinition())
const zoom = ref(1)
const showJson = ref(false)
const lastSnapshot = ref(JSON.stringify(definition.value))
const canvasWrapRef = ref(null)
const isPanning = ref(false)
const panState = ref({
  startX: 0,
  startY: 0,
  scrollLeft: 0,
  scrollTop: 0,
})

const formattedJson = computed(() => JSON.stringify(definition.value, null, 2))
const canvasZoom = computed(() => (Number.isFinite(zoom.value) ? clampZoom(zoom.value) : 1))
const canvasScaleStyle = computed(() => ({
  transform: `scale(${canvasZoom.value})`
}))

watch(
  definition,
  (value) => {
    const snapshot = JSON.stringify(value)
    if (snapshot !== lastSnapshot.value) {
      emit('changed', value)
    }
  },
  { deep: true }
)

function clampZoom(value) {
  return Math.min(2, Math.max(0.4, Number(value || 1)))
}

function zoomIn() {
  zoom.value = clampZoom(canvasZoom.value + 0.1)
}

function zoomOut() {
  zoom.value = clampZoom(canvasZoom.value - 0.1)
}

function resetZoom() {
  zoom.value = 1
}

function handleWheelZoom(event) {
  const delta = Number(event?.deltaY || 0)
  if (!Number.isFinite(delta) || delta === 0) {
    return
  }
  const step = delta > 0 ? -0.08 : 0.08
  zoom.value = clampZoom(canvasZoom.value + step)
}

function isInteractiveTarget(target) {
  return Boolean(
    target?.closest?.(
      '.node-wrap-box, .auto-judge, .add-node-btn, .add-node-popover-body, .el-button, .el-drawer, .el-input, .el-select'
    )
  )
}

function startPan(event) {
  if (event.button !== 0 || isInteractiveTarget(event.target)) {
    return
  }
  const wrap = canvasWrapRef.value
  if (!wrap) {
    return
  }
  isPanning.value = true
  panState.value = {
    startX: event.clientX,
    startY: event.clientY,
    scrollLeft: wrap.scrollLeft,
    scrollTop: wrap.scrollTop,
  }
}

function handlePan(event) {
  if (!isPanning.value) {
    return
  }
  const wrap = canvasWrapRef.value
  if (!wrap) {
    return
  }
  const deltaX = event.clientX - panState.value.startX
  const deltaY = event.clientY - panState.value.startY
  wrap.scrollLeft = panState.value.scrollLeft - deltaX
  wrap.scrollTop = panState.value.scrollTop - deltaY
}

function stopPan() {
  isPanning.value = false
}

function markClean() {
  lastSnapshot.value = JSON.stringify(definition.value)
}

function buildMeta(meta = {}) {
  return {
    processId: meta.processId || definition.value.key || 'process',
    processName: meta.processName || definition.value.name || '新建流程'
  }
}

async function createNewDiagram(meta = {}) {
  definition.value = createDefaultFlowDefinition(buildMeta(meta))
  markClean()
}

async function loadDefinition(designerJson, meta = {}) {
  try {
    const parsed = typeof designerJson === 'string' ? JSON.parse(designerJson) : designerJson
    definition.value = normalizeFlowDefinition(parsed, buildMeta(meta))
    markClean()
  } catch (error) {
    emit('error', error)
    throw error
  }
}

async function syncProcessMeta(meta = {}) {
  const resolved = buildMeta(meta)
  definition.value = {
    ...definition.value,
    key: resolved.processId,
    name: resolved.processName
  }
}

async function getXml(meta = {}) {
  return buildFlowlongBpmn(definition.value, buildMeta(meta))
}

async function getDesignerJson() {
  return JSON.stringify(definition.value)
}

async function importXml(xml, meta = {}, markDirty = false) {
  try {
    definition.value = parseBpmnToFlowDefinition(xml, buildMeta(meta))
    if (markDirty) {
      lastSnapshot.value = ''
    } else {
      markClean()
    }
  } catch (error) {
    emit('error', error)
    throw error
  }
}

async function downloadXml(fileName = 'process.bpmn') {
  const xml = await getXml()
  downloadBlob(xml, fileName, 'application/xml;charset=utf-8')
}

function downloadBlob(content, fileName, mimeType) {
  const blob = new Blob([content], { type: mimeType })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  URL.revokeObjectURL(url)
}

function focusElementById() {
  return false
}

defineExpose({
  createNewDiagram,
  loadDefinition,
  syncProcessMeta,
  getXml,
  getDesignerJson,
  importXml,
  downloadXml,
  markClean,
  focusElementById
})
</script>

<style scoped>
.flowlong-shell {
  --flowlong-border: rgba(0, 0, 0, 0.08);
  --flowlong-text: #1f2329;
  --flowlong-muted: #6b7280;
  --flowlong-accent: #3296fa;
  --flowlong-surface: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 10px;
  background: #f5f5f7;
  overflow: hidden;
  box-shadow: none;
}

.flowlong-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: #fff;
}

.flowlong-toolbar-left {
  display: flex;
  align-items: center;
  gap: 14px;
  color: var(--flowlong-text);
  flex-wrap: wrap;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #3296fa;
  box-shadow: none;
}

.flowlong-title-block {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.flowlong-title-block strong {
  font-size: 15px;
  letter-spacing: 0;
}

.flowlong-hint {
  color: var(--flowlong-muted);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0;
}

.flowlong-meta-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.flowlong-meta-pill,
.flowlong-zoom-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid #dcdfe6;
  background: #f5f7fa;
  color: #606266;
  font-size: 11px;
  font-weight: 700;
}

.flowlong-zoom-pill {
  min-width: 62px;
  background: #f1f5fb;
}

.flowlong-toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.flowlong-toolbar-btn {
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
  min-height: 36px;
  padding: 0 16px;
  border-radius: 18px;
  font-weight: 700;
  transition: transform 0.16s ease, box-shadow 0.16s ease, border-color 0.16s ease, background 0.16s ease;
}

.flowlong-toolbar-btn:hover {
  border-color: #c0c4cc;
  box-shadow: none;
}

.flowlong-toolbar-btn.active {
  background: #ecf5ff;
  color: #409eff;
}

.flowlong-toolbar-btn.accent {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
}

.flowlong-canvas-wrap {
  overflow: auto;
  min-height: 340px;
  padding: 16px 24px 20px;
  cursor: grab;
  background:
    linear-gradient(rgba(0, 0, 0, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 0, 0, 0.06) 1px, transparent 1px),
    linear-gradient(180deg, #eef1f6 0%, #eef1f6 100%);
  background-size: 32px 32px, 32px 32px, auto;
}

.flowlong-canvas-wrap.is-panning {
  cursor: grabbing;
  user-select: none;
}

.flowlong-stage-panel {
  width: max-content;
  min-width: max(100%, 1480px);
  min-height: 0;
  padding: 16px;
  border-radius: 0;
  border: 1px solid var(--flowlong-border);
  background: #f5f5f7;
  box-shadow: none;
}

.flowlong-stage-head {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  padding: 0 4px 10px;
  color: var(--flowlong-text);
}

.flowlong-stage-head small {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #909399;
  font-size: 11px;
  font-weight: 700;
}

.flowlong-canvas-viewport {
  min-width: max(100%, 1420px);
  min-height: 0;
}

.flowlong-canvas-scale {
  display: inline-block;
  transform-origin: top center;
  transition: transform 0.18s ease;
  min-width: 100%;
}

.flowlong-canvas {
  min-width: 1360px;
  min-height: 180px;
  color: #122033;
  padding: 2px 10px 4px;
}

.flowlong-json-preview {
  margin: 0;
  padding: 16px;
  border-radius: 16px;
  background: #0f172a;
  color: #dbeafe;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 900px) {
  .flowlong-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .flowlong-stage-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .flowlong-stage-panel {
    min-width: max(100%, 1120px);
  }

  .flowlong-canvas-viewport {
    min-width: max(100%, 1060px);
  }

  .flowlong-canvas {
    min-width: 1020px;
  }
}
</style>
