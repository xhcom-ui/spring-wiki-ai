<template>
  <div class="designer-shell">
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

    <div class="designer-stage">
      <div ref="canvasRef" class="designer-canvas"></div>

      <aside class="designer-inspector">
        <div class="inspector-head">
          <div>
            <p class="eyebrow">Legacy BPMN Inspector</p>
            <h3>节点业务配置</h3>
          </div>
          <span class="meta-pill meta-pill-tight">{{ selectionSummary }}</span>
        </div>

        <template v-if="selectedElement">
          <section v-if="isSupportedSelection" class="inspector-section">
            <label>
              <span>节点名称</span>
              <input v-model.trim="inspectorForm.nodeName" type="text" placeholder="请输入节点名称" @input="applyInspectorChanges" />
            </label>

            <label>
              <span>目录表单</span>
              <select :value="catalogSelectionValue" @change="handleCatalogSelection($event.target.value)">
                <option value="">未绑定目录</option>
                <option v-for="item in formCatalogs" :key="item.key" :value="item.key">
                  {{ item.title }} ({{ item.key }})
                </option>
              </select>
            </label>

            <label>
              <span>业务页标签</span>
              <input v-model.trim="inspectorForm.pageLabel" type="text" placeholder="请假发起页 / 待办审批页" @input="applyInspectorChanges" />
            </label>

            <label>
              <span>表单标签</span>
              <input v-model.trim="inspectorForm.formLabel" type="text" placeholder="请假申请单 / 审批单" @input="applyInspectorChanges" />
            </label>

            <label>
              <span>表单 Key</span>
              <input v-model.trim="inspectorForm.formKey" type="text" placeholder="leave-form" @input="applyInspectorChanges" />
            </label>

            <template v-if="selectedElementType === 'bpmn:UserTask'">
              <label>
                <span>办理策略</span>
                <select v-model="inspectorForm.assignmentMode" @change="applyInspectorChanges">
                  <option value="assignee">单一办理人</option>
                  <option value="candidateUsers">候选用户</option>
                  <option value="candidateGroups">候选角色/组</option>
                </select>
              </label>

              <label v-if="inspectorForm.assignmentMode === 'assignee'">
                <span>办理人</span>
                <input v-model.trim="inspectorForm.assignee" type="text" placeholder="${deptManager}" @input="applyInspectorChanges" />
              </label>

              <label v-if="inspectorForm.assignmentMode === 'candidateUsers'">
                <span>候选用户</span>
                <textarea
                  v-model.trim="inspectorForm.candidateUsers"
                  rows="3"
                  placeholder="userA,userB 或 ${candidateUsers}"
                  @input="applyInspectorChanges"
                ></textarea>
              </label>

              <label v-if="inspectorForm.assignmentMode === 'candidateGroups'">
                <span>候选角色/组</span>
                <textarea
                  v-model.trim="inspectorForm.candidateGroups"
                  rows="3"
                  placeholder="hr,finance 或 ${candidateGroups}"
                  @input="applyInspectorChanges"
                ></textarea>
              </label>
            </template>
          </section>

          <div v-else class="empty-state inspector-empty">
            当前选中的是 <strong>{{ selectedElementLabel }}</strong>，暂时只支持开始节点和用户任务的业务配置。
          </div>

          <section class="inspector-section info-section">
            <div class="info-row">
              <span>元素 ID</span>
              <strong>{{ selectedElement?.businessObject?.id || '-' }}</strong>
            </div>
            <div class="info-row">
              <span>BPMN 类型</span>
              <strong>{{ selectedElementType }}</strong>
            </div>
            <div class="info-row">
              <span>引擎命名空间</span>
              <strong>{{ activeNamespacePrefix }}</strong>
            </div>
          </section>
        </template>

        <div v-else class="empty-state inspector-empty">
          选择开始节点或用户任务后，这里会显示目录表单、业务标签和办理人配置。
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'

const props = defineProps({
  formCatalogs: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['ready', 'changed', 'error'])

const FLOWABLE_NAMESPACE = 'http://flowable.org/bpmn'
const ACTIVITI_NAMESPACE = 'http://activiti.org/bpmn'

const canvasRef = ref(null)
const isReady = ref(false)
const isDirty = ref(false)
const selectedElement = ref(null)

const inspectorForm = reactive(createInspectorForm())

let modeler
let changeEmitTimer = null
let fitViewportTimer = null

function createInspectorForm() {
  return {
    nodeName: '',
    formKey: '',
    formLabel: '',
    pageLabel: '',
    assignmentMode: 'assignee',
    assignee: '',
    candidateUsers: '',
    candidateGroups: ''
  }
}

function resetInspectorForm() {
  Object.assign(inspectorForm, createInspectorForm())
}

function createDefaultDiagram({ processId = 'Process_1', processName = '新建流程' } = {}) {
  return `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
  xmlns:flowable="${FLOWABLE_NAMESPACE}"
  xmlns:activiti="${ACTIVITI_NAMESPACE}"
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

function normalizeSelectionElement(element) {
  if (!element) {
    return null
  }
  return element.labelTarget || element
}

function getElementType(element) {
  return element?.businessObject?.$type || element?.type || ''
}

function parseDocumentationText(businessObject) {
  const source = Array.isArray(businessObject?.documentation)
    ? businessObject.documentation.map((item) => item?.text || '').filter(Boolean).join('\n').trim()
    : ''
  if (!source) {
    return {
      pageLabel: '',
      formLabel: '',
      rawText: ''
    }
  }
  const segments = source.split(/\s*\/\s*/).map((item) => item.trim()).filter(Boolean)
  return {
    pageLabel: segments[0] || source,
    formLabel: segments[1] || '',
    rawText: source
  }
}

function resolveNamespacePrefix(businessObject) {
  const attrs = businessObject?.$attrs || {}
  if (Object.keys(attrs).some((key) => key.startsWith('activiti:'))) {
    return 'activiti'
  }
  if (Object.keys(attrs).some((key) => key.startsWith('flowable:'))) {
    return 'flowable'
  }
  return 'flowable'
}

function readNamespacedValue(businessObject, name) {
  const attrs = businessObject?.$attrs || {}
  return attrs[`flowable:${name}`] || attrs[`activiti:${name}`] || ''
}

function syncSelection(element) {
  selectedElement.value = normalizeSelectionElement(element)
  const businessObject = selectedElement.value?.businessObject
  if (!businessObject) {
    resetInspectorForm()
    return
  }
  const documentation = parseDocumentationText(businessObject)
  const candidateUsers = readNamespacedValue(businessObject, 'candidateUsers')
  const candidateGroups = readNamespacedValue(businessObject, 'candidateGroups')
  Object.assign(inspectorForm, {
    nodeName: businessObject.name || '',
    formKey: readNamespacedValue(businessObject, 'formKey'),
    formLabel: documentation.formLabel,
    pageLabel: documentation.pageLabel,
    assignmentMode: candidateUsers
      ? 'candidateUsers'
      : candidateGroups
        ? 'candidateGroups'
        : 'assignee',
    assignee: readNamespacedValue(businessObject, 'assignee'),
    candidateUsers,
    candidateGroups
  })
}

function ensureNamespace(prefix) {
  const definitions = modeler?.getDefinitions?.()
  if (!definitions) {
    return
  }
  definitions.$attrs = definitions.$attrs || {}
  definitions.$attrs['xmlns:flowable'] = FLOWABLE_NAMESPACE
  definitions.$attrs['xmlns:activiti'] = ACTIVITI_NAMESPACE
  if (prefix === 'activiti') {
    definitions.$attrs['xmlns:activiti'] = ACTIVITI_NAMESPACE
  } else {
    definitions.$attrs['xmlns:flowable'] = FLOWABLE_NAMESPACE
  }
}

function setEngineAttribute(attributes, prefix, name, value) {
  delete attributes[`flowable:${name}`]
  delete attributes[`activiti:${name}`]
  if (value) {
    attributes[`${prefix}:${name}`] = value
  }
}

function buildDocumentation(pageLabel, formLabel) {
  return [pageLabel, formLabel].map((item) => item.trim()).filter(Boolean).join(' / ')
}

function scheduleChangedEmit() {
  if (changeEmitTimer) {
    window.clearTimeout(changeEmitTimer)
  }
  changeEmitTimer = window.setTimeout(async () => {
    if (!isReady.value) {
      return
    }
    emit('changed', await getXml())
  }, 120)
}

function applyInspectorChanges() {
  if (!selectedElement.value || !isSupportedSelection.value || !modeler) {
    return
  }
  const businessObject = selectedElement.value.businessObject
  const namespacePrefix = resolveNamespacePrefix(businessObject)
  ensureNamespace(namespacePrefix)

  const attrs = {
    ...(businessObject.$attrs || {})
  }

  const trimmedFormKey = inspectorForm.formKey.trim()
  setEngineAttribute(attrs, namespacePrefix, 'formKey', trimmedFormKey)

  if (selectedElementType.value === 'bpmn:UserTask') {
    setEngineAttribute(
      attrs,
      namespacePrefix,
      'assignee',
      inspectorForm.assignmentMode === 'assignee' ? inspectorForm.assignee.trim() : ''
    )
    setEngineAttribute(
      attrs,
      namespacePrefix,
      'candidateUsers',
      inspectorForm.assignmentMode === 'candidateUsers' ? inspectorForm.candidateUsers.trim() : ''
    )
    setEngineAttribute(
      attrs,
      namespacePrefix,
      'candidateGroups',
      inspectorForm.assignmentMode === 'candidateGroups' ? inspectorForm.candidateGroups.trim() : ''
    )
  } else {
    setEngineAttribute(attrs, namespacePrefix, 'assignee', '')
    setEngineAttribute(attrs, namespacePrefix, 'candidateUsers', '')
    setEngineAttribute(attrs, namespacePrefix, 'candidateGroups', '')
  }

  businessObject.$attrs = attrs

  const documentationText = buildDocumentation(inspectorForm.pageLabel, inspectorForm.formLabel)
  businessObject.documentation = documentationText
    ? [modeler.get('moddle').create('bpmn:Documentation', { text: documentationText })]
    : []

  modeler.get('modeling').updateProperties(selectedElement.value, {
    name: inspectorForm.nodeName.trim()
  })
  isDirty.value = true
  scheduleChangedEmit()
}

function handleCatalogSelection(formKey) {
  const matched = formCatalogs.value.find((item) => item.key === formKey)
  inspectorForm.formKey = formKey
  if (matched?.title && !inspectorForm.formLabel.trim()) {
    inspectorForm.formLabel = matched.title
  } else if (matched?.title && formKey) {
    inspectorForm.formLabel = matched.title
  }
  applyInspectorChanges()
}

async function importXml(xml, markDirty = false) {
  if (!modeler) {
    return
  }
  try {
    await modeler.importXML(xml)
    scheduleFitViewportSafely()
    isReady.value = true
    isDirty.value = markDirty
    syncSelection(null)
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
  const current = Number(canvas.zoom())
  const baseScale = Number.isFinite(current) && current > 0 ? current : 1
  canvas.zoom(Math.max(0.2, Math.min(4, baseScale + step)))
}

function zoomIn() {
  zoom(0.1)
}

function zoomOut() {
  zoom(-0.1)
}

function resetZoom() {
  if (modeler) {
    scheduleFitViewportSafely()
  }
}

function fitViewportSafely() {
  if (!modeler || !canvasRef.value) {
    return
  }
  const { clientWidth, clientHeight } = canvasRef.value
  const canvas = modeler.get('canvas')
  if (clientWidth <= 0 || clientHeight <= 0) {
    return
  }
  try {
    canvas.zoom('fit-viewport', 'auto')
  } catch (error) {
    canvas.zoom(1)
  }
}

function scheduleFitViewportSafely() {
  if (fitViewportTimer) {
    window.clearTimeout(fitViewportTimer)
  }
  fitViewportTimer = window.setTimeout(() => {
    fitViewportSafely()
    fitViewportTimer = null
  }, 32)
}

const selectedElementType = computed(() => getElementType(selectedElement.value))
const isSupportedSelection = computed(() => ['bpmn:StartEvent', 'bpmn:UserTask'].includes(selectedElementType.value))
const activeNamespacePrefix = computed(() => resolveNamespacePrefix(selectedElement.value?.businessObject))
const formCatalogs = computed(() => props.formCatalogs || [])
const catalogSelectionValue = computed(() => inspectorForm.formKey || '')
const selectedElementLabel = computed(() => {
  if (!selectedElement.value) {
    return '未选择节点'
  }
  return selectedElement.value.businessObject?.name || selectedElementType.value
})
const selectionSummary = computed(() => {
  if (!selectedElement.value) {
    return '未选中节点'
  }
  return selectedElementType.value === 'bpmn:UserTask' ? '用户任务' : selectedElementType.value === 'bpmn:StartEvent' ? '开始节点' : selectedElementType.value
})

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

onMounted(async () => {
  try {
    modeler = new BpmnModeler({
      container: canvasRef.value,
      keyboard: {
        bindTo: window
      }
    })

    const eventBus = modeler.get('eventBus')
    eventBus.on('selection.changed', (event) => {
      syncSelection(event.newSelection?.[0] || null)
    })
    eventBus.on('element.click', (event) => {
      syncSelection(event.element)
    })

    modeler.on('commandStack.changed', async () => {
      if (!isReady.value) {
        return
      }
      if (selectedElement.value) {
        syncSelection(selectedElement.value)
      }
      try {
        isDirty.value = true
        emit('changed', await getXml())
      } catch (error) {
        emit('error', error)
      }
    })

    await createNewDiagram()
  } catch (error) {
    emit('error', error)
  }
})

onUnmounted(() => {
  if (changeEmitTimer) {
    window.clearTimeout(changeEmitTimer)
  }
  if (fitViewportTimer) {
    window.clearTimeout(fitViewportTimer)
  }
  modeler?.destroy()
})

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
  height: auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #dfe6ef;
  border-radius: 8px;
  overflow: hidden;
}

.designer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border-bottom: 1px solid #e6ebf2;
  background: #fafbfd;
}

.designer-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #4a5665;
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
  border: 1px solid #d7dde7;
  border-radius: 4px;
  background: #fff;
  color: #445163;
  padding: 6px 12px;
  font-size: 13px;
  cursor: pointer;
}

.designer-stage {
  flex: 0 0 auto;
  min-height: clamp(540px, 60vh, 760px);
  display: grid;
  grid-template-columns: minmax(0, 1fr) 250px;
  align-items: stretch;
}

.designer-canvas {
  min-height: clamp(540px, 60vh, 760px);
  height: clamp(540px, 60vh, 760px);
}

.designer-inspector {
  border-left: 1px solid #e6ebf2;
  background: #fff;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
}

.inspector-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.inspector-head h3 {
  margin: 4px 0 0;
}

.inspector-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.inspector-section label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.inspector-section span {
  font-size: 13px;
  color: #6b7785;
}

.inspector-section input,
.inspector-section select,
.inspector-section textarea {
  width: 100%;
  border: 1px solid #d7dde7;
  border-radius: 4px;
  padding: 8px 10px;
  background: #fff;
  color: #334155;
  font: inherit;
}

.info-section {
  padding-top: 8px;
  border-top: 1px solid #eef2f7;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
  color: #4a5665;
}

.inspector-empty {
  min-height: 120px;
  display: grid;
  place-items: center;
  text-align: center;
}

.meta-pill-tight {
  padding-inline: 10px;
}

:deep(.djs-container) {
  background-image:
    radial-gradient(circle at 1px 1px, rgba(148, 163, 184, 0.12) 1px, transparent 0);
  background-size: 18px 18px;
}

@media (max-width: 1180px) {
  .designer-stage {
    grid-template-columns: minmax(0, 1fr);
    min-height: 0;
  }

  .designer-inspector {
    border-left: none;
    border-top: 1px solid rgba(73, 58, 37, 0.12);
  }

  .designer-canvas {
    height: 460px;
    min-height: 460px;
  }
}
</style>
