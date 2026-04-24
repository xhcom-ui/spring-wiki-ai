<template>
  <div class="designer-shell sf-shell">
    <div class="designer-toolbar">
      <div class="designer-status">
        <span :class="['status-dot', statusClass]"></span>
        <span>{{ statusText }}</span>
      </div>
      <div class="designer-actions">
        <button type="button" class="ghost-btn" @click="zoomOut">缩小</button>
        <button type="button" class="ghost-btn" @click="resetZoom">适配视图</button>
        <button type="button" class="ghost-btn" @click="zoomIn">放大</button>
      </div>
    </div>

    <div class="sf-layout">
      <section class="sf-canvas-panel">
        <div class="sf-canvas-meta">
          <div>
            <p class="eyebrow">Custom Process Canvas</p>
            <h3>SeaFlow 风格流程设计器</h3>
          </div>
          <div class="management-meta">
            <span class="meta-pill">缩放 {{ Math.round(zoom * 100) }}%</span>
            <span class="meta-pill">节点 {{ nodeCount }}</span>
          </div>
        </div>

        <div class="sf-canvas-viewport">
          <div class="sf-canvas-scale" :style="{ transform: `scale(${zoom})` }">
            <SeaFlowNode
              v-if="schema"
              :node="schema"
              :readonly="readonly"
              :selected-node-id="selectedNodeId"
              @select-node="selectNode"
              @remove-node="removeNode"
              @insert-node="insertNode"
              @append-branch="appendBranch"
              @tag-click="selectNode"
            />
          </div>
        </div>
      </section>

      <aside class="sf-inspector-panel">
        <div class="sf-canvas-meta">
          <div>
            <p class="eyebrow">Node Inspector</p>
            <h3>{{ selectedNode?.nodeName || '节点配置' }}</h3>
          </div>
          <span class="status-pill">{{ selectedNodeTypeLabel }}</span>
        </div>

        <div v-if="selectedNode" class="sf-form-stack">
          <label>
            <span>节点名称</span>
            <input v-model.trim="selectedNode.nodeName" type="text" @input="handleInspectorChange" />
          </label>

          <template v-if="selectedNode.nodeType === 'start'">
            <label>
              <span>发起人说明</span>
              <input v-model.trim="selectedNode.properties.initiatorLabel" type="text" placeholder="发起人 / 申请人" @input="handleInspectorChange" />
            </label>
            <label>
              <span>表单 Key</span>
              <select v-model="selectedNode.properties.formKey" @change="handleFormCatalogSelection">
                <option value="">请选择发起表单</option>
                <option v-for="item in startFormCatalogOptions" :key="item.id || item.formKey" :value="item.formKey">
                  {{ item.formName }} · {{ item.formKey }}
                </option>
              </select>
            </label>
            <article class="detail-item sf-preview-item">
              <span class="detail-key">业务页标签</span>
              <strong class="detail-value">{{ selectedFormCatalogMeta.pageLabel || selectedNode.properties.pageLabel || '未配置' }}</strong>
            </article>
            <article class="detail-item sf-preview-item">
              <span class="detail-key">表单标签</span>
              <strong class="detail-value">{{ selectedFormCatalogMeta.formName || selectedNode.properties.formLabel || '未配置' }}</strong>
            </article>
          </template>

          <template v-else-if="selectedNode.nodeType === 'between'">
            <label>
              <span>审批模式</span>
              <select v-model="selectedNode.properties.assignmentMode" @change="handleInspectorChange">
                <option value="assignee">单一办理人</option>
                <option value="candidateUsers">候选用户</option>
                <option value="candidateGroups">候选角色/组</option>
              </select>
            </label>
            <label v-if="selectedNode.properties.assignmentMode === 'assignee'">
              <span>办理人表达式</span>
              <input v-model.trim="selectedNode.properties.assignee" type="text" placeholder="${deptManager}" @input="handleInspectorChange" />
            </label>
            <label v-if="selectedNode.properties.assignmentMode === 'candidateUsers'">
              <span>候选用户</span>
              <textarea v-model.trim="selectedNode.properties.candidateUsers" rows="3" placeholder="userA,userB 或 ${candidateUsers}" @input="handleInspectorChange"></textarea>
            </label>
            <label v-if="selectedNode.properties.assignmentMode === 'candidateGroups'">
              <span>候选组</span>
              <textarea v-model.trim="selectedNode.properties.candidateGroups" rows="3" placeholder="hr,finance 或 ${candidateGroups}" @input="handleInspectorChange"></textarea>
            </label>
            <label>
              <span>表单 Key</span>
              <select v-model="selectedNode.properties.formKey" @change="handleFormCatalogSelection">
                <option value="">请选择审批表单</option>
                <option v-for="item in taskFormCatalogOptions" :key="item.id || item.formKey" :value="item.formKey">
                  {{ item.formName }} · {{ item.formKey }}
                </option>
              </select>
            </label>
            <article class="detail-item sf-preview-item">
              <span class="detail-key">业务页标签</span>
              <strong class="detail-value">{{ selectedFormCatalogMeta.pageLabel || selectedNode.properties.pageLabel || '未配置' }}</strong>
            </article>
            <article class="detail-item sf-preview-item">
              <span class="detail-key">表单标签</span>
              <strong class="detail-value">{{ selectedFormCatalogMeta.formName || selectedNode.properties.formLabel || '未配置' }}</strong>
            </article>
            <label>
              <span>任务监听器</span>
              <textarea
                :value="listenersText"
                rows="4"
                placeholder="一行一个，格式：create=com.demo.TaskCreateListener"
                @input="updateListeners($event.target.value)"
              ></textarea>
            </label>
          </template>

          <template v-else-if="selectedNode.nodeType === 'serial-node'">
            <label class="toggle-row sf-checkbox-row">
              <input v-model="selectedNode.properties.isDefault" type="checkbox" @change="toggleDefaultBranch" />
              <span>设为默认分支</span>
            </label>
            <label>
              <span>条件描述</span>
              <input
                v-model.trim="selectedNode.properties.conditionSummary"
                type="text"
                :disabled="selectedNode.properties.isDefault"
                placeholder="例如：请假天数 > 3"
                @input="handleInspectorChange"
              />
            </label>
            <label>
              <span>条件表达式</span>
              <textarea
                v-model.trim="selectedNode.properties.conditionExpression"
                rows="4"
                :disabled="selectedNode.properties.isDefault"
                placeholder="${days > 3}"
                @input="handleInspectorChange"
              ></textarea>
            </label>
          </template>

          <template v-else-if="selectedNode.nodeType === 'serial' || selectedNode.nodeType === 'parallel'">
            <div class="empty-state compact-empty">
              {{ selectedNode.nodeType === 'serial' ? '当前节点控制条件分支，请在下方各分支节点里配置条件表达式。' : '当前节点控制并行审批，请在各并行节点里配置审批人。' }}
            </div>
          </template>

          <template v-else-if="selectedNode.nodeType === 'end'">
            <div class="empty-state compact-empty">
              结束节点无需额外配置。
            </div>
          </template>

          <article class="detail-item sf-preview-item">
            <span class="detail-key">节点摘要</span>
            <strong class="detail-value">{{ selectedNode.value || '未配置' }}</strong>
          </article>
        </div>

        <div v-else class="empty-state compact-empty">
          点击左侧节点后，在这里编辑其审批人、条件表达式和表单标签。
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import SeaFlowNode from './SeaFlowNode.vue'
import {
  buildCustomBpmnXml,
  createDefaultCustomSchema,
  createLeaveCustomSchema,
  createSerialConditions,
  exportSchemaJson,
  normalizeCustomSchema,
  parseStoredCustomSchema,
  updateDerivedNodeValue
} from '../../utils/seaflowDesigner'

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

const schema = ref(null)
const selectedNodeId = ref('')
const zoom = ref(1)
const isReady = ref(false)
const isDirty = ref(false)
const meta = ref({
  processId: 'process',
  processName: '新建流程'
})

let silentUpdate = false

const selectedNode = computed(() => findNodeById(schema.value, selectedNodeId.value))
const nodeCount = computed(() => countNodes(schema.value))
const startFormCatalogOptions = computed(() => props.formCatalogs.filter((item) => String(item.scope || '').toUpperCase() === 'START'))
const taskFormCatalogOptions = computed(() => props.formCatalogs.filter((item) => String(item.scope || '').toUpperCase() !== 'START'))
const selectedFormCatalogMeta = computed(() => {
  const formKey = selectedNode.value?.properties?.formKey
  if (!formKey) {
    return {}
  }
  return props.formCatalogs.find((item) => item.formKey === formKey) || {}
})

const selectedNodeTypeLabel = computed(() => {
  if (!selectedNode.value) {
    return '未选中'
  }
  switch (selectedNode.value.nodeType) {
    case 'start':
      return '开始节点'
    case 'between':
      return '审批节点'
    case 'serial':
      return '条件网关'
    case 'serial-node':
      return '条件分支'
    case 'parallel':
      return '并行网关'
    case 'end':
      return '结束节点'
    default:
      return '流程节点'
  }
})

const listenersText = computed(() => {
  if (!selectedNode.value || selectedNode.value.nodeType !== 'between') {
    return ''
  }
  return (selectedNode.value.properties.listeners || [])
    .map((item) => `${item.listenerType}=${item.listenerPath}`)
    .join('\n')
})

const statusText = computed(() => {
  if (!isReady.value) {
    return '自定义设计器初始化中'
  }
  return isDirty.value ? '有未保存改动' : '已同步'
})

const statusClass = computed(() => (isDirty.value ? 'status-dirty' : isReady.value ? 'status-clean' : 'status-waiting'))

watch(
  schema,
  () => {
    if (silentUpdate) {
      silentUpdate = false
      return
    }
    if (!isReady.value) {
      return
    }
    isDirty.value = true
    emit('changed', schema.value)
  },
  { deep: true }
)

function countNodes(node) {
  if (!node) {
    return 0
  }
  let count = 1
  if (node.childNode) {
    count += countNodes(node.childNode)
  }
  if (Array.isArray(node.conditionNodes)) {
    count += node.conditionNodes.reduce((total, item) => total + countNodes(item), 0)
  }
  return count
}

function findNodeById(node, id) {
  if (!node || !id) {
    return null
  }
  if (node.nodeId === id) {
    return node
  }
  if (node.childNode) {
    const childHit = findNodeById(node.childNode, id)
    if (childHit) {
      return childHit
    }
  }
  if (Array.isArray(node.conditionNodes)) {
    for (const item of node.conditionNodes) {
      const hit = findNodeById(item, id)
      if (hit) {
        return hit
      }
    }
  }
  return null
}

function findParentContext(node, targetId, parent = null, containerKey = '', containerIndex = -1) {
  if (!node) {
    return null
  }
  if (node.nodeId === targetId) {
    return { node, parent, containerKey, containerIndex }
  }
  if (node.childNode) {
    const childHit = findParentContext(node.childNode, targetId, node, 'childNode', -1)
    if (childHit) {
      return childHit
    }
  }
  if (Array.isArray(node.conditionNodes)) {
    for (let index = 0; index < node.conditionNodes.length; index += 1) {
      const hit = findParentContext(node.conditionNodes[index], targetId, node, 'conditionNodes', index)
      if (hit) {
        return hit
      }
    }
  }
  return null
}

function refreshSchemaStructure(preferredNodeId = '') {
  if (!schema.value) {
    return
  }
  const normalized = normalizeCustomSchema(schema.value, meta.value)
  schema.value = normalized
  const fallbackId = preferredNodeId || selectedNodeId.value
  const resolved = fallbackId ? findNodeById(normalized, fallbackId) : null
  selectedNodeId.value = resolved?.nodeId || normalized.nodeId
}

function createNodeByType(nodeType) {
  if (nodeType === 'between') {
    return normalizeCustomSchema({
      nodeId: `task_${Date.now().toString(36)}`,
      nodeType: 'between',
      nodeName: '审批节点',
      value: '',
      properties: {
        assignmentMode: 'assignee',
        assignee: '',
        candidateUsers: '',
        candidateGroups: '',
        formKey: '',
        formLabel: '',
        pageLabel: '',
        listeners: []
      }
    })
  }
  if (nodeType === 'serial') {
    return normalizeCustomSchema({
      nodeId: `gateway_${Date.now().toString(36)}`,
      nodeType: 'serial',
      nodeName: '条件分支',
      properties: {},
      conditionNodes: createSerialConditions()
    })
  }
  if (nodeType === 'parallel') {
    return normalizeCustomSchema({
      nodeId: `parallel_${Date.now().toString(36)}`,
      nodeType: 'parallel',
      nodeName: '并行分支',
      properties: {},
      conditionNodes: [
        {
          nodeId: `task_${Math.random().toString(36).slice(2, 8)}`,
          nodeType: 'between',
          nodeName: '并行审批 1',
          value: '',
          properties: {
            assignmentMode: 'assignee',
            assignee: '',
            candidateUsers: '',
            candidateGroups: '',
            formKey: '',
            formLabel: '',
            pageLabel: '',
            listeners: []
          }
        },
        {
          nodeId: `task_${Math.random().toString(36).slice(2, 8)}`,
          nodeType: 'between',
          nodeName: '并行审批 2',
          value: '',
          properties: {
            assignmentMode: 'assignee',
            assignee: '',
            candidateUsers: '',
            candidateGroups: '',
            formKey: '',
            formLabel: '',
            pageLabel: '',
            listeners: []
          }
        }
      ]
    })
  }
  return createDefaultCustomSchema(meta.value).childNode
}

function selectNode(node) {
  selectedNodeId.value = node?.nodeId || ''
}

function handleInspectorChange() {
  if (!selectedNode.value) {
    return
  }
  updateDerivedNodeValue(selectedNode.value)
}

function handleFormCatalogSelection() {
  if (!selectedNode.value) {
    return
  }
  const current = selectedFormCatalogMeta.value
  if (current?.formName) {
    selectedNode.value.properties.formLabel = current.formName
  }
  if (current?.pageLabel) {
    selectedNode.value.properties.pageLabel = current.pageLabel
  }
  handleInspectorChange()
}

function toggleDefaultBranch() {
  if (!selectedNode.value || selectedNode.value.nodeType !== 'serial-node') {
    return
  }
  const context = findParentContext(schema.value, selectedNode.value.nodeId)
  if (!context?.parent || !Array.isArray(context.parent.conditionNodes)) {
    handleInspectorChange()
    return
  }
  if (selectedNode.value.properties.isDefault) {
    context.parent.conditionNodes.forEach((item) => {
      item.properties = item.properties || {}
      item.properties.isDefault = item.nodeId === selectedNode.value.nodeId
      updateDerivedNodeValue(item)
    })
    refreshSchemaStructure(selectedNode.value.nodeId)
    return
  }
  if (!context.parent.conditionNodes.some((item) => item.properties?.isDefault)) {
    selectedNode.value.properties.isDefault = true
    updateDerivedNodeValue(selectedNode.value)
    refreshSchemaStructure(selectedNode.value.nodeId)
    return
  }
  handleInspectorChange()
}

function updateListeners(rawText) {
  if (!selectedNode.value || selectedNode.value.nodeType !== 'between') {
    return
  }
  selectedNode.value.properties.listeners = rawText
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [listenerType, ...rest] = line.split('=')
      return {
        listenerType: listenerType?.trim() || '',
        listenerPath: rest.join('=').trim()
      }
    })
    .filter((item) => item.listenerType && item.listenerPath)
  handleInspectorChange()
}

function insertNode({ targetNode, nodeType }) {
  if (props.readonly || !targetNode?.nodeId) {
    return
  }
  const context = findParentContext(schema.value, targetNode.nodeId)
  if (!context?.node || context.node.nodeType === 'end') {
    return
  }
  const newNode = createNodeByType(nodeType)
  newNode.childNode = context.node.childNode || null
  context.node.childNode = newNode
  refreshSchemaStructure(newNode.nodeId)
}

function appendBranch({ node, branchType }) {
  if (props.readonly || !node?.nodeId) {
    return
  }
  const gateway = findNodeById(schema.value, node.nodeId)
  if (!gateway) {
    return
  }
  gateway.conditionNodes = Array.isArray(gateway.conditionNodes) ? gateway.conditionNodes : []
  if (branchType === 'serial') {
    const branchNumber = gateway.conditionNodes.filter((item) => item.nodeType === 'serial-node' && !item.properties?.isDefault).length + 1
    const defaultIndex = gateway.conditionNodes.findIndex((item) => item.properties?.isDefault)
    const nextBranch = normalizeCustomSchema({
      nodeId: `condition_${Date.now().toString(36)}`,
      nodeType: 'serial-node',
      nodeName: `条件 ${branchNumber}`,
      value: '',
      properties: {
        conditionSummary: '',
        conditionExpression: '',
        isDefault: false
      }
    })
    if (defaultIndex >= 0) {
      gateway.conditionNodes.splice(defaultIndex, 0, nextBranch)
    } else {
      gateway.conditionNodes.push(nextBranch)
    }
    refreshSchemaStructure(nextBranch.nodeId)
    return
  }
  const nextParallel = normalizeCustomSchema({
    nodeId: `task_${Date.now().toString(36)}`,
    nodeType: 'between',
    nodeName: `并行审批 ${gateway.conditionNodes.length + 1}`,
    value: '',
    properties: {
      assignmentMode: 'assignee',
      assignee: '',
      candidateUsers: '',
      candidateGroups: '',
      formKey: '',
      formLabel: '',
      pageLabel: '',
      listeners: []
    }
  })
  gateway.conditionNodes.push(nextParallel)
  refreshSchemaStructure(nextParallel.nodeId)
}

function removeNode(node) {
  if (props.readonly || !node?.nodeId) {
    return
  }
  const context = findParentContext(schema.value, node.nodeId)
  if (!context?.parent) {
    return
  }
  if (context.containerKey === 'childNode') {
    context.parent.childNode = context.node.childNode || null
  } else if (context.containerKey === 'conditionNodes') {
    if ((context.parent.conditionNodes || []).length <= 2) {
      return
    }
    context.parent.conditionNodes.splice(context.containerIndex, 1)
    const hasDefault = context.parent.conditionNodes.some((item) => item.properties?.isDefault)
    if (!hasDefault && context.parent.nodeType === 'serial') {
      const fallback = context.parent.conditionNodes[context.parent.conditionNodes.length - 1]
      fallback.properties = fallback.properties || {}
      fallback.properties.isDefault = true
      updateDerivedNodeValue(fallback)
    }
  }
  if (selectedNodeId.value === node.nodeId) {
    selectedNodeId.value = context.parent.nodeId
  }
  refreshSchemaStructure(selectedNodeId.value || context.parent.nodeId)
}

function zoomIn() {
  zoom.value = Math.min(1.6, zoom.value + 0.1)
}

function zoomOut() {
  zoom.value = Math.max(0.5, zoom.value - 0.1)
}

function resetZoom() {
  zoom.value = 1
}

async function importSchema(schemaInput, markDirty = false, metadata = {}) {
  try {
    silentUpdate = true
    meta.value = {
      processId: metadata.processId || meta.value.processId,
      processName: metadata.processName || meta.value.processName
    }
    const normalized = typeof schemaInput === 'string'
      ? parseStoredCustomSchema(schemaInput, meta.value)
      : normalizeCustomSchema(schemaInput, meta.value)
    schema.value = normalized
    selectedNodeId.value = normalized.nodeId
    isReady.value = true
    isDirty.value = Boolean(markDirty)
    emit('ready')
  } catch (error) {
    emit('error', error)
    throw error
  }
}

async function createNewDiagram(metadata = {}) {
  meta.value = {
    processId: metadata.processId || 'process',
    processName: metadata.processName || '新建流程'
  }
  await importSchema(createDefaultCustomSchema(meta.value), false, meta.value)
}

async function createLeaveTemplate() {
  await importSchema(createLeaveCustomSchema(), true, meta.value)
}

async function getSchema() {
  return normalizeCustomSchema(schema.value, meta.value)
}

async function getXml(metadata = {}) {
  const payload = {
    processId: metadata.processId || meta.value.processId,
    processName: metadata.processName || meta.value.processName
  }
  return buildCustomBpmnXml(await getSchema(), payload)
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

async function downloadSchema(fileName = 'process-design.json') {
  const blob = new Blob([exportSchemaJson(await getSchema())], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  URL.revokeObjectURL(url)
}

defineExpose({
  createNewDiagram,
  createLeaveTemplate,
  importSchema,
  getSchema,
  getXml,
  downloadXml,
  downloadSchema,
  zoomIn,
  zoomOut,
  resetZoom,
  markClean() {
    isDirty.value = false
  }
})
</script>

<style scoped>
.sf-shell {
  min-height: 720px;
}

.sf-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  min-height: 680px;
}

.sf-canvas-panel,
.sf-inspector-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.sf-canvas-panel {
  border-right: 1px solid rgba(61, 79, 107, 0.08);
}

.sf-canvas-meta {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 24px 26px 16px;
}

.sf-canvas-meta h3 {
  margin: 0;
  color: #16263d;
}

.sf-canvas-viewport {
  flex: 1;
  overflow: auto;
  padding: 18px 28px 38px;
  background:
    radial-gradient(circle at 1px 1px, rgba(126, 146, 175, 0.16) 1px, transparent 0),
    linear-gradient(180deg, rgba(250, 252, 255, 0.96), rgba(243, 247, 252, 0.92));
  background-size: 22px 22px, auto;
}

.sf-canvas-scale {
  transform-origin: top center;
  min-width: max-content;
  padding: 24px 48px 56px;
  display: flex;
  justify-content: center;
}

.sf-inspector-panel {
  padding-bottom: 24px;
  background:
    linear-gradient(180deg, rgba(252, 254, 255, 0.96), rgba(246, 249, 253, 0.9));
}

.sf-form-stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 0 24px;
}

.sf-form-stack label {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sf-form-stack span {
  color: #52667f;
  font-size: 13px;
  font-weight: 600;
}

.sf-checkbox-row {
  justify-content: flex-start;
}

.sf-preview-item {
  margin-top: 6px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(65, 85, 113, 0.08);
}

.compact-empty {
  margin: 0 24px;
}

@media (max-width: 1120px) {
  .sf-layout {
    grid-template-columns: 1fr;
  }

  .sf-canvas-panel {
    border-right: 0;
    border-bottom: 1px solid rgba(87, 69, 44, 0.1);
  }
}
</style>
