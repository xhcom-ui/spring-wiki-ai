<template>
  <div class="designer-shell designer-layout">
    <section class="designer-main">
      <div class="designer-toolbar">
        <div class="designer-status">
          <span :class="['status-dot', statusClass]"></span>
          <span>{{ statusText }}</span>
        </div>
        <div class="designer-actions">
          <button type="button" class="ghost-btn" @click="zoomOut">缩小</button>
          <button type="button" class="ghost-btn" @click="resetZoom">适配画布</button>
          <button type="button" class="ghost-btn" @click="zoomIn">放大</button>
        </div>
      </div>
      <div ref="canvasRef" class="designer-canvas"></div>
    </section>

    <aside class="designer-inspector">
      <div class="inspector-head">
        <div>
          <p class="eyebrow">BPMN Inspector</p>
          <h3>{{ selectedElementName }}</h3>
        </div>
        <span class="status-pill">{{ selectedElementTypeLabel }}</span>
      </div>

      <div v-if="selectedElementId" class="form-stack">
        <article class="detail-item">
          <span class="detail-key">节点 ID</span>
          <strong class="detail-value">{{ selectedElementId }}</strong>
        </article>

        <template v-if="isUserTaskSelected || isStartEventSelected">
          <label>
            <span>表单 Key</span>
            <select :value="selectedFormKey" @change="applyFormCatalog(($event.target).value)">
              <option value="">{{ isStartEventSelected ? '请选择发起表单' : '请选择审批表单' }}</option>
              <option v-for="item in selectedCatalogOptions" :key="item.id || item.formKey" :value="item.formKey">
                {{ item.formName }} · {{ item.formKey }}
              </option>
            </select>
          </label>

          <article class="detail-item">
            <span class="detail-key">页面标签</span>
            <strong class="detail-value">{{ selectedFormCatalogMeta.pageLabel || '-' }}</strong>
          </article>
          <article class="detail-item">
            <span class="detail-key">表单标签</span>
            <strong class="detail-value">{{ selectedFormCatalogMeta.formName || '-' }}</strong>
          </article>
          <article class="detail-item">
            <span class="detail-key">组件 Key</span>
            <strong class="detail-value">{{ selectedFormCatalogMeta.componentKey || '-' }}</strong>
          </article>
          <article v-if="isUserTaskSelected" class="detail-item">
            <span class="detail-key">办理人</span>
            <strong class="detail-value">{{ selectedAssignee || '-' }}</strong>
          </article>
        </template>

        <div v-else class="empty-state compact-empty">
          请选择 `Start Event` 或 `User Task` 节点后，在这里绑定表单目录。当前 BPMN 设计器已支持发起节点和审批节点表单绑定。
        </div>
      </div>

      <div v-else class="empty-state compact-empty">
        点击左侧 BPMN 画布中的节点后，这里会展示节点属性和表单目录绑定入口。
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'

const emit = defineEmits(['ready', 'changed', 'error'])

const props = defineProps({
  formCatalogs: {
    type: Array,
    default: () => []
  }
})

const canvasRef = ref(null)
const isReady = ref(false)
const isDirty = ref(false)
const selectedElement = ref(null)
const selectedFormKey = ref('')

let modeler
let overlayIds = []

function createDefaultDiagram({ processId = 'Process_1', processName = '新建流程' } = {}) {
  return `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
  xmlns:flowable="http://flowable.org/bpmn"
  id="Definitions_1"
  targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="${processId}" name="${processName}" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="开始" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="${processId}">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="180" y="120" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNLabel id="StartEvent_1_label">
        <dc:Bounds x="184" y="162" width="28" height="14" />
      </bpmndi:BPMNLabel>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`
}

async function importXml(xml, markDirty = false) {
  if (!modeler) {
    return
  }
  try {
    await modeler.importXML(xml)
    modeler.get('canvas').zoom('fit-viewport', 'auto')
    isReady.value = true
    isDirty.value = markDirty
    syncSelectedElementState(modeler.get('selection').get()[0] || null)
    refreshNodeLabelOverlays()
    emit('ready')
  } catch (error) {
    emit('error', error)
    throw error
  }
}

async function createNewDiagram(metadata = {}) {
  await importXml(createDefaultDiagram(metadata), false)
}

async function getXml() {
  const { xml } = await modeler.saveXML({ format: true })
  return xml
}

async function downloadXml(fileName = 'process.bpmn') {
  const xml = await getXml()
  const blob = new Blob([xml], { type: 'application/xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  URL.revokeObjectURL(url)
}

function zoom(step) {
  if (!modeler) {
    return
  }
  const canvas = modeler.get('canvas')
  const current = canvas.zoom()
  canvas.zoom(Math.max(0.2, Math.min(4, current + step)))
}

function zoomIn() {
  zoom(0.1)
}

function zoomOut() {
  zoom(-0.1)
}

function resetZoom() {
  if (modeler) {
    modeler.get('canvas').zoom('fit-viewport', 'auto')
  }
}

function readFlowableAttr(element, key) {
  const attrs = element?.businessObject?.$attrs || {}
  return attrs[`flowable:${key}`] || attrs[`activiti:${key}`] || ''
}

function syncSelectedElementState(element) {
  selectedElement.value = element || null
  selectedFormKey.value = readFlowableAttr(element, 'formKey')
}

function readDocumentationMeta(element) {
  const text = element?.businessObject?.documentation?.[0]?.text || ''
  if (!text) {
    return { pageLabel: '', formLabel: '' }
  }
  const [pageLabel = '', formLabel = ''] = text.split('/').map((item) => item.trim())
  return { pageLabel, formLabel }
}

function resolveElementFormMeta(element) {
  const formKey = readFlowableAttr(element, 'formKey')
  const catalog = props.formCatalogs.find((item) => item.formKey === formKey)
  const documentationMeta = readDocumentationMeta(element)
  return {
    formKey,
    pageLabel: catalog?.pageLabel || documentationMeta.pageLabel || '',
    formLabel: catalog?.formName || documentationMeta.formLabel || ''
  }
}

function clearNodeLabelOverlays() {
  if (!modeler || !overlayIds.length) {
    overlayIds = []
    return
  }
  const overlays = modeler.get('overlays')
  overlayIds.forEach((id) => overlays.remove(id))
  overlayIds = []
}

function refreshNodeLabelOverlays() {
  clearNodeLabelOverlays()
  if (!modeler || !isReady.value) {
    return
  }
  const overlays = modeler.get('overlays')
  const elementRegistry = modeler.get('elementRegistry')
  const selectedId = selectedElement.value?.id || ''
  elementRegistry.forEach((element) => {
    const type = element?.businessObject?.$type
    if (!['bpmn:UserTask', 'bpmn:StartEvent'].includes(type)) {
      return
    }
    const meta = resolveElementFormMeta(element)
    const tags = [meta.pageLabel, meta.formLabel].filter(Boolean)
    if (!tags.length && meta.formKey) {
      tags.push(meta.formKey)
    }
    if (!tags.length) {
      return
    }
    const html = document.createElement('div')
    html.className = `canvas-tag-overlay${selectedId === element.id ? ' is-selected' : ''}`
    tags.slice(0, 2).forEach((label) => {
      const node = document.createElement('span')
      node.className = 'canvas-tag-pill'
      node.textContent = label
      html.appendChild(node)
    })
    overlayIds.push(overlays.add(element, {
      position: {
        top: -14,
        right: -10
      },
      html
    }))
  })
}

function applyFormCatalog(formKey) {
  if (!modeler || !selectedElement.value || (!isUserTaskSelected.value && !isStartEventSelected.value)) {
    return
  }
  const modeling = modeler.get('modeling')
  const moddle = modeler.get('moddle')
  const meta = selectedCatalogOptions.value.find((item) => item.formKey === formKey) || {}
  const documentationText = formKey && (meta.pageLabel || meta.formName)
    ? [meta.pageLabel || '', meta.formName || ''].filter(Boolean).join(' / ')
    : ''
  modeling.updateProperties(selectedElement.value, {
    'flowable:formKey': formKey || undefined,
    documentation: documentationText
      ? [moddle.create('bpmn:Documentation', { text: documentationText })]
      : []
  })
  selectedFormKey.value = formKey || ''
  refreshNodeLabelOverlays()
}

const statusText = computed(() => {
  if (!isReady.value) {
    return '画布初始化中'
  }
  return isDirty.value ? '有未保存改动' : '已同步'
})

const statusClass = computed(() => {
  if (!isReady.value) {
    return 'status-waiting'
  }
  return isDirty.value ? 'status-dirty' : 'status-clean'
})

const selectedElementId = computed(() => selectedElement.value?.id || '')
const selectedElementName = computed(() => selectedElement.value?.businessObject?.name || selectedElementId.value || '未选择节点')
const selectedElementType = computed(() => selectedElement.value?.businessObject?.$type || '')
const isUserTaskSelected = computed(() => selectedElementType.value === 'bpmn:UserTask')
const isStartEventSelected = computed(() => selectedElementType.value === 'bpmn:StartEvent')
const selectedAssignee = computed(() => readFlowableAttr(selectedElement.value, 'assignee'))
const startFormCatalogOptions = computed(() =>
  props.formCatalogs.filter((item) => String(item.scope || '').toUpperCase() === 'START')
)
const taskFormCatalogOptions = computed(() =>
  props.formCatalogs.filter((item) => String(item.scope || '').toUpperCase() !== 'START')
)
const selectedCatalogOptions = computed(() =>
  isStartEventSelected.value ? startFormCatalogOptions.value : taskFormCatalogOptions.value
)
const selectedFormCatalogMeta = computed(() =>
  selectedCatalogOptions.value.find((item) => item.formKey === selectedFormKey.value) || {}
)
const selectedElementTypeLabel = computed(() => {
  if (!selectedElementType.value) {
    return '未选中'
  }
  if (selectedElementType.value === 'bpmn:UserTask') {
    return '用户任务'
  }
  if (selectedElementType.value === 'bpmn:StartEvent') {
    return '开始事件'
  }
  if (selectedElementType.value === 'bpmn:ExclusiveGateway') {
    return '排他网关'
  }
  return selectedElementType.value.replace('bpmn:', '')
})

onMounted(async () => {
  modeler = new BpmnModeler({
    container: canvasRef.value,
    keyboard: {
      bindTo: window
    }
  })

  modeler.on('commandStack.changed', async () => {
    if (!isReady.value) {
      return
    }
    isDirty.value = true
    syncSelectedElementState(modeler.get('selection').get()[0] || null)
    refreshNodeLabelOverlays()
    emit('changed', await getXml())
  })

  modeler.on('selection.changed', (event) => {
    syncSelectedElementState(event?.newSelection?.[0] || null)
    refreshNodeLabelOverlays()
  })

  await createNewDiagram()
})

onUnmounted(() => {
  clearNodeLabelOverlays()
  modeler?.destroy()
})

watch(
  () => props.formCatalogs,
  () => {
    refreshNodeLabelOverlays()
  },
  { deep: true }
)

defineExpose({
  createNewDiagram,
  importXml,
  getXml,
  downloadXml,
  zoomIn,
  zoomOut,
  resetZoom,
  markClean() {
    isDirty.value = false
  }
})
</script>

<style scoped>
.designer-shell {
  height: 100%;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(244, 248, 252, 0.92));
  border: 1px solid rgba(72, 95, 124, 0.12);
  border-radius: 28px;
  box-shadow: 0 24px 60px rgba(20, 35, 57, 0.08);
  overflow: hidden;
}

.designer-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
}

.designer-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.designer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(72, 95, 124, 0.08);
  background: rgba(251, 253, 255, 0.92);
}

.designer-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #556b84;
  font-size: 13px;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
}

.status-clean {
  background: #2f7d32;
}

.status-dirty {
  background: #cc7a00;
}

.status-waiting {
  background: #8f8f8f;
}

.designer-actions {
  display: inline-flex;
  gap: 10px;
}

.ghost-btn {
  border: 1px solid rgba(73, 58, 37, 0.18);
  border-radius: 999px;
  background: #fffdf8;
  color: #5a4b34;
  padding: 8px 14px;
  font-size: 13px;
  cursor: pointer;
}

.designer-canvas {
  flex: 1;
  min-height: 620px;
}

.designer-inspector {
  border-left: 1px solid rgba(72, 95, 124, 0.08);
  background: linear-gradient(180deg, rgba(252, 254, 255, 0.96), rgba(245, 249, 253, 0.92));
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.inspector-head h3 {
  margin: 0;
}

.compact-empty {
  margin: 0;
}

:deep(.djs-container) {
  background-image:
    radial-gradient(circle at 1px 1px, rgba(126, 146, 175, 0.16) 1px, transparent 0);
  background-size: 20px 20px;
}

:deep(.canvas-tag-overlay) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  pointer-events: none;
}

:deep(.canvas-tag-overlay.is-selected .canvas-tag-pill) {
  background: #244c82;
  color: #f7fbff;
}

:deep(.canvas-tag-pill) {
  display: inline-flex;
  align-items: center;
  max-width: 140px;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(66, 92, 126, 0.18);
  color: #46617e;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  box-shadow: 0 10px 22px rgba(21, 39, 63, 0.12);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 1120px) {
  .designer-layout {
    grid-template-columns: 1fr;
  }

  .designer-inspector {
    border-left: 0;
    border-top: 1px solid rgba(73, 58, 37, 0.1);
  }
}
</style>
