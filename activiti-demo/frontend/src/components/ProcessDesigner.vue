<template>
  <div :class="['designer-shell', { 'studio-mode': currentMode === 'studio' }]">
    <div class="designer-topbar">
      <div class="designer-topbar-left">
        <div class="designer-status">
          <span :class="['status-dot', statusClass]"></span>
          <span>{{ statusText }}</span>
        </div>
        <div class="mode-switch">
          <button
            type="button"
            :class="['mode-btn', { active: currentMode === 'studio' }]"
            @click="switchMode('studio')"
          >
            工作台设计器
          </button>
          <button
            type="button"
            :class="['mode-btn', { active: currentMode === 'classic' }]"
            @click="switchMode('classic')"
          >
            经典画布
          </button>
        </div>
      </div>

      <div class="designer-actions">
        <button type="button" class="ghost-btn" @click="undo">撤销</button>
        <button type="button" class="ghost-btn" @click="redo">重做</button>
        <button type="button" class="ghost-btn" @click="zoomOut">缩小</button>
        <button type="button" class="ghost-btn" @click="resetZoom">适配画布</button>
        <button type="button" class="ghost-btn" @click="zoomIn">放大</button>
        <button type="button" class="ghost-btn danger-ghost" :disabled="selectedIsProcess" @click="deleteSelected">
          删除节点
        </button>
      </div>
    </div>

    <div class="designer-body">
      <aside v-if="currentMode === 'studio'" class="designer-palette">
        <div v-for="group in paletteGroups" :key="group.key" class="palette-group">
          <p class="palette-title">{{ group.label }}</p>
          <div class="palette-list">
            <button
              v-for="item in group.items"
              :key="item.type"
              type="button"
              class="palette-item"
              draggable="true"
              @click="insertElement(item)"
              @dragstart="handlePaletteDragStart(item)"
            >
              <strong>{{ item.label }}</strong>
              <span>{{ item.description }}</span>
            </button>
          </div>
        </div>
      </aside>

      <section ref="stageRef" class="designer-stage">
        <div v-if="currentMode === 'studio'" class="designer-stage-hint">
          <span>拖拽左侧节点到画布，或点击节点快速插入。</span>
          <span>当前选中：{{ selectedSummary }}</span>
        </div>
        <div
          ref="canvasRef"
          class="designer-canvas"
          @dragover.prevent
          @drop.prevent="handleCanvasDrop"
        ></div>
        <div
          v-if="showQuickToolbar"
          class="designer-quick-tools"
          :style="quickToolbarStyle"
        >
          <template v-if="selectedMeta.isSequenceFlow">
            <div class="quick-group">
              <span class="quick-group-title">流转</span>
              <button type="button" class="quick-tool-btn" @click="insertOnSequenceFlow('bpmn:UserTask')">
                <span class="quick-icon">UT</span>
                <span>插审批</span>
              </button>
              <button type="button" class="quick-tool-btn" @click="insertOnSequenceFlow('bpmn:ServiceTask')">
                <span class="quick-icon">ST</span>
                <span>插服务</span>
              </button>
              <button type="button" class="quick-tool-btn" @click="insertOnSequenceFlow('bpmn:ExclusiveGateway')">
                <span class="quick-icon">GW</span>
                <span>插网关</span>
              </button>
            </div>
          </template>
          <template v-else>
            <div class="quick-group">
              <span class="quick-group-title">节点</span>
              <button type="button" class="quick-tool-btn" @click="appendElement('bpmn:UserTask')">
                <span class="quick-icon">UT</span>
                <span>后接审批</span>
              </button>
              <button type="button" class="quick-tool-btn" @click="appendElement('bpmn:ServiceTask')">
                <span class="quick-icon">ST</span>
                <span>后接服务</span>
              </button>
              <button type="button" class="quick-tool-btn" @click="appendElement('bpmn:ExclusiveGateway')">
                <span class="quick-icon">GW</span>
                <span>后接网关</span>
              </button>
            </div>
            <div v-if="selectedSupportsWorkflowMeta" class="quick-group">
              <span class="quick-group-title">执行</span>
              <button
                type="button"
                class="quick-tool-btn"
                @click="cycleAsyncMode()"
              >
                <span class="quick-icon">EX</span>
                <span>{{ asyncModeLabel }}</span>
              </button>
            </div>
          </template>
          <div class="quick-group quick-group-danger">
            <span class="quick-group-title">操作</span>
            <button type="button" class="quick-tool-btn danger" @click="deleteSelected">
              <span class="quick-icon">DL</span>
              <span>删除</span>
            </button>
          </div>
        </div>
        <div
          v-if="contextMenu.visible"
          class="designer-context-menu"
          :style="contextMenuStyle"
        >
          <button
            v-for="action in contextMenu.actions"
            :key="action.key"
            type="button"
            :class="['context-menu-item', { danger: action.danger }]"
            @click="runContextMenuAction(action)"
          >
            {{ action.label }}
          </button>
        </div>
      </section>

      <aside v-if="currentMode === 'studio'" class="designer-inspector">
        <div class="inspector-head">
          <p class="inspector-kicker">Inspector</p>
          <h4>{{ selectedIsProcess ? '流程属性' : '节点属性' }}</h4>
        </div>

        <div class="inspector-form">
          <label class="field-block">
            <span>元素类型</span>
            <input :value="selectedMeta.typeLabel" type="text" disabled />
          </label>

          <label class="field-block">
            <span>{{ selectedIsProcess ? '流程 ID' : '元素 ID' }}</span>
            <input :value="selectedMeta.id" type="text" @input="updateSelectedId($event.target.value)" />
          </label>

          <label class="field-block">
            <span>{{ selectedIsProcess ? '流程名称' : '显示名称' }}</span>
            <input :value="selectedMeta.name" type="text" @input="updateSelectedName($event.target.value)" />
          </label>

          <label v-if="selectedIsProcess" class="field-inline">
            <input :checked="selectedMeta.executable" type="checkbox" @change="updateProcessExecutable($event.target.checked)" />
            <span>可执行流程</span>
          </label>

          <label v-if="!selectedIsProcess" class="field-block">
            <span>备注说明</span>
            <textarea
              :value="selectedMeta.documentation"
              rows="4"
              placeholder="节点说明、办理提示、业务备注"
              @input="updateDocumentation($event.target.value)"
            ></textarea>
          </label>

          <div v-if="selectedSupportsWorkflowMeta" class="inspector-section">
            <div class="inspector-section-head">
              <strong>业务配置</strong>
              <span>和后端兼容，直接保存在 BPMN XML 内</span>
            </div>

            <div v-if="selectedSupportsAssignment" class="field-cluster">
              <label class="field-block">
                <span>办理人</span>
                <input
                  :value="selectedMeta.assignee"
                  type="text"
                  placeholder="userId / ${starter}"
                  @input="updateDesignerMeta('assignee', $event.target.value)"
                />
              </label>

              <label class="field-block">
                <span>候选用户</span>
                <input
                  :value="selectedMeta.candidateUsers"
                  type="text"
                  placeholder="u001,u002"
                  @input="updateDesignerMeta('candidateUsers', $event.target.value)"
                />
              </label>

              <label class="field-block">
                <span>候选角色</span>
                <input
                  :value="selectedMeta.candidateGroups"
                  type="text"
                  placeholder="hr,leader"
                  @input="updateDesignerMeta('candidateGroups', $event.target.value)"
                />
              </label>
            </div>

            <div class="field-cluster two-column">
              <label class="field-block">
                <span>表单 Key</span>
                <select
                  :value="selectedMeta.formKey"
                  @change="updateDesignerMeta('formKey', $event.target.value)"
                >
                  <option value="">未配置</option>
                  <option v-for="option in designerFormOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
                <input
                  :value="selectedMeta.formKey"
                  type="text"
                  placeholder="也可手动输入自定义表单 Key"
                  @input="updateDesignerMeta('formKey', $event.target.value)"
                />
                <p class="field-help">
                  {{ selectedFormDefinition?.description || '优先从注册表里选，运行时会按 formKey 映射办理页。' }}
                </p>
              </label>

              <label class="field-block">
                <span>节点优先级</span>
                <input
                  :value="selectedMeta.priority"
                  type="text"
                  placeholder="50"
                  @input="updateDesignerMeta('priority', $event.target.value)"
                />
              </label>

              <label class="field-block">
                <span>时限表达式</span>
                <input
                  :value="selectedMeta.dueExpression"
                  type="text"
                  placeholder="P2D / ${deadline}"
                  @input="updateDesignerMeta('dueExpression', $event.target.value)"
                />
              </label>

              <label class="field-block">
                <span>展示排序</span>
                <input
                  :value="selectedMeta.sort"
                  type="text"
                  placeholder="10"
                  @input="updateDesignerMeta('sort', $event.target.value)"
                />
              </label>
            </div>

            <div v-if="selectedSupportsAssignment" class="field-cluster two-column">
              <label class="field-block">
                <span>待办页面</span>
                <select
                  :value="selectedMeta.todoPage"
                  @change="updateDesignerMeta('todoPage', $event.target.value)"
                >
                  <option value="">未配置</option>
                  <option v-for="option in designerTodoPageOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
                <input
                  :value="selectedMeta.todoPage"
                  type="text"
                  placeholder="也可手动输入自定义待办页标识"
                  @input="updateDesignerMeta('todoPage', $event.target.value)"
                />
                <p class="field-help">
                  {{ selectedTodoPageDefinition?.description || '运行时会根据 todoPage 打开对应业务办理页。' }}
                </p>
              </label>

              <label class="field-block">
                <span>已办页面</span>
                <select
                  :value="selectedMeta.donePage"
                  @change="updateDesignerMeta('donePage', $event.target.value)"
                >
                  <option value="">未配置</option>
                  <option v-for="option in designerDonePageOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
                <input
                  :value="selectedMeta.donePage"
                  type="text"
                  placeholder="也可手动输入自定义完成页标识"
                  @input="updateDesignerMeta('donePage', $event.target.value)"
                />
                <p class="field-help">
                  {{ selectedDonePageDefinition?.description || '任务完成后会优先根据 donePage 切到业务回执页。' }}
                </p>
              </label>
            </div>

            <div v-if="selectedSupportsAssignment" class="field-actions">
              <button
                type="button"
                class="ghost-btn mini-ghost-btn"
                :disabled="!selectedFormDefinition"
                @click="applyRecommendedBusinessPages"
              >
                套用该表单推荐页面
              </button>
              <p class="field-help">
                {{ selectedFormDefinition ? `当前表单推荐待办页 ${selectedFormDefinition.defaultTodoPage}，完成页 ${selectedFormDefinition.defaultDonePage}。` : '未命中注册表时，仍可手动输入自定义业务页标识。' }}
              </p>
            </div>
          </div>

          <div v-if="selectedIsServiceTask" class="inspector-section">
            <div class="inspector-section-head">
              <strong>服务任务配置</strong>
              <span>支持类名、表达式或代理表达式</span>
            </div>

            <div class="field-cluster">
              <label class="field-block">
                <span>实现类</span>
                <input
                  :value="selectedMeta.serviceClass"
                  type="text"
                  placeholder="com.example.workflow.LeaveApproveDelegate"
                  @input="updateDesignerMeta('serviceClass', $event.target.value)"
                />
              </label>

              <label class="field-block">
                <span>表达式</span>
                <input
                  :value="selectedMeta.serviceExpression"
                  type="text"
                  placeholder="${leaveService.approve(execution)}"
                  @input="updateDesignerMeta('serviceExpression', $event.target.value)"
                />
              </label>

              <label class="field-block">
                <span>代理表达式</span>
                <input
                  :value="selectedMeta.serviceDelegateExpression"
                  type="text"
                  placeholder="${leaveTaskDelegate}"
                  @input="updateDesignerMeta('serviceDelegateExpression', $event.target.value)"
                />
              </label>
            </div>
          </div>

          <div v-if="selectedSupportsWorkflowMeta && !selectedMeta.isSequenceFlow" class="inspector-section">
            <div class="inspector-section-head">
              <strong>监听与执行</strong>
              <span>适合补充业务监听器或异步执行意图</span>
            </div>

            <div class="field-cluster two-column">
              <label class="field-block">
                <span>执行监听 Bean</span>
                <input
                  :value="selectedMeta.listenerBean"
                  type="text"
                  placeholder="leaveTaskListener"
                  @input="updateDesignerMeta('listenerBean', $event.target.value)"
                />
              </label>

              <label class="field-block">
                <span>执行方法</span>
                <input
                  :value="selectedMeta.listenerMethod"
                  type="text"
                  placeholder="startMethod,endMethod"
                  @input="updateDesignerMeta('listenerMethod', $event.target.value)"
                />
              </label>
            </div>

            <div class="field-cluster two-column">
              <label class="field-block">
                <span>执行方式</span>
                <select :value="selectedMeta.asyncMode" @change="updateAsyncMode($event.target.value)">
                  <option value="sync">同步执行</option>
                  <option value="async">进入前异步</option>
                </select>
              </label>

              <label class="field-inline field-inline-card">
                <input
                  :checked="selectedMeta.exclusiveJob"
                  type="checkbox"
                  :disabled="selectedMeta.asyncMode === 'sync'"
                  @change="updateDesignerMeta('exclusiveJob', $event.target.checked)"
                />
                <span>独占 Job</span>
              </label>
            </div>

            <p class="field-help">
              当前 Activiti 引擎以 `activiti:async` 和 `activiti:exclusive` 为主；如需“离开后异步”，建议拆为独立服务任务建模。
            </p>
          </div>

          <div v-if="selectedIsUserTask" class="inspector-section">
            <div class="inspector-section-head">
              <strong>任务监听</strong>
              <span>用户任务支持创建和完成两个监听时机</span>
            </div>

            <div class="field-cluster two-column">
              <label class="field-block">
                <span>任务监听 Bean</span>
                <input
                  :value="selectedMeta.taskListenerBean"
                  type="text"
                  placeholder="leaveTaskLifecycleListener"
                  @input="updateDesignerMeta('taskListenerBean', $event.target.value)"
                />
              </label>

              <label class="field-block">
                <span>创建方法</span>
                <input
                  :value="selectedMeta.taskCreateMethod"
                  type="text"
                  placeholder="onCreate"
                  @input="updateDesignerMeta('taskCreateMethod', $event.target.value)"
                />
              </label>

              <label class="field-block">
                <span>完成方法</span>
                <input
                  :value="selectedMeta.taskCompleteMethod"
                  type="text"
                  placeholder="onComplete"
                  @input="updateDesignerMeta('taskCompleteMethod', $event.target.value)"
                />
              </label>
            </div>
          </div>

          <label v-if="selectedMeta.isSequenceFlow" class="field-block">
            <span>流转条件</span>
            <textarea
              :value="selectedMeta.conditionExpression"
              rows="4"
              placeholder="${approved == true}"
              @input="updateConditionExpression($event.target.value)"
            ></textarea>
          </label>

          <div v-if="!selectedIsProcess" class="inspector-actions">
            <button type="button" class="ghost-btn" @click="focusProcess">切回流程属性</button>
            <button type="button" class="ghost-btn danger-ghost" @click="deleteSelected">删除当前元素</button>
          </div>

          <div v-if="selectedCanAppend" class="inspector-section">
            <div class="inspector-section-head">
              <strong>快捷编排</strong>
              <span>基于当前节点快速向后追加常用流程节点</span>
            </div>

            <div class="append-actions">
              <button type="button" class="ghost-btn" @click="appendElement('bpmn:UserTask')">追加人工任务</button>
              <button type="button" class="ghost-btn" @click="appendElement('bpmn:ServiceTask')">追加服务任务</button>
              <button type="button" class="ghost-btn" @click="appendElement('bpmn:ExclusiveGateway')">追加排他网关</button>
              <button type="button" class="ghost-btn" @click="appendElement('bpmn:ParallelGateway')">追加并行网关</button>
              <button type="button" class="ghost-btn" @click="appendElement('bpmn:EndEvent')">追加结束事件</button>
            </div>
          </div>

          <div v-if="selectedCanInsertOnFlow" class="inspector-section">
            <div class="inspector-section-head">
              <strong>连线中插入</strong>
              <span>在当前流转连线中间插入节点并自动重新连线</span>
            </div>

            <div class="append-actions">
              <button type="button" class="ghost-btn" @click="insertOnSequenceFlow('bpmn:UserTask')">插入人工任务</button>
              <button type="button" class="ghost-btn" @click="insertOnSequenceFlow('bpmn:ServiceTask')">插入服务任务</button>
              <button type="button" class="ghost-btn" @click="insertOnSequenceFlow('bpmn:ExclusiveGateway')">插入排他网关</button>
              <button type="button" class="ghost-btn" @click="insertOnSequenceFlow('bpmn:ParallelGateway')">插入并行网关</button>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import {
  ensureTaskPageCatalogLoaded,
  findTaskFormDefinition,
  findTaskPageDefinition,
  getTaskFormOptions,
  getTaskPageOptions,
  resolveRecommendedPages
} from '../runtime/taskPageCatalog'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'

const props = defineProps({
  uiMode: {
    type: String,
    default: 'studio'
  },
  showMetaBadges: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['ready', 'changed', 'error', 'update:uiMode'])

const router = useRouter()
const canvasRef = ref(null)
const stageRef = ref(null)
const isReady = ref(false)
const isDirty = ref(false)
const currentMode = ref(props.uiMode)
const selectedElement = ref(null)
const draggedPaletteItem = ref(null)
const quickToolbar = reactive({
  left: 0,
  top: 0,
  visible: false
})
const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  actions: []
})

const selectedMeta = reactive({
  id: '',
  name: '',
  type: '',
  typeLabel: '流程',
  documentation: '',
  conditionExpression: '',
  executable: true,
  isSequenceFlow: false,
  assignee: '',
  candidateUsers: '',
  candidateGroups: '',
  formKey: '',
  todoPage: '',
  donePage: '',
  dueExpression: '',
  priority: '',
  sort: '',
  listenerBean: '',
  listenerMethod: '',
  taskListenerBean: '',
  taskCreateMethod: '',
  taskCompleteMethod: '',
  serviceClass: '',
  serviceExpression: '',
  serviceDelegateExpression: '',
  asyncMode: 'sync',
  exclusiveJob: true,
  asyncBefore: false,
  asyncAfter: false
})

const DESIGNER_META_DOC_FORMAT = 'application/vnd.activiti-designer+json'
const ACTIVITI_NAMESPACE = 'http://activiti.org/bpmn'
const ACTIVITI_ATTRIBUTE_MAP = {
  assignee: 'activiti:assignee',
  candidateUsers: 'activiti:candidateUsers',
  candidateGroups: 'activiti:candidateGroups',
  formKey: 'activiti:formKey',
  dueExpression: 'activiti:dueDate',
  priority: 'activiti:priority',
  serviceClass: 'activiti:class',
  serviceExpression: 'activiti:expression',
  serviceDelegateExpression: 'activiti:delegateExpression'
}

const paletteGroups = [
  {
    key: 'basic',
    label: '基础节点',
    items: [
      { type: 'bpmn:StartEvent', label: '开始事件', description: '流程入口节点' },
      { type: 'bpmn:EndEvent', label: '结束事件', description: '流程结束节点' },
      { type: 'bpmn:UserTask', label: '人工任务', description: '需要人工审批或处理' },
      { type: 'bpmn:ServiceTask', label: '服务任务', description: '自动化服务调用' }
    ]
  },
  {
    key: 'decision',
    label: '流转控制',
    items: [
      { type: 'bpmn:ExclusiveGateway', label: '排他网关', description: '条件分支流转' },
      { type: 'bpmn:ParallelGateway', label: '并行网关', description: '并发拆分或汇聚' },
      { type: 'bpmn:SubProcess', label: '子流程', description: '折叠子流程节点', isExpanded: false }
    ]
  }
]

let modeler
let designerMetaOverlayIds = []

function createDefaultDiagram({ processId = 'Process_1', processName = '新建流程' } = {}) {
  return `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
  xmlns:activiti="${ACTIVITI_NAMESPACE}"
  id="Definitions_1"
  targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="${escapeXml(processId)}" name="${escapeXml(processName)}" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="开始" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="${escapeXml(processId)}">
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

function escapeXml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function getCanvas() {
  return modeler?.get('canvas')
}

function getOverlays() {
  return modeler?.get('overlays')
}

function getElementRegistry() {
  return modeler?.get('elementRegistry')
}

function getModeling() {
  return modeler?.get('modeling')
}

function getSelection() {
  return modeler?.get('selection')
}

function getElementFactory() {
  return modeler?.get('elementFactory')
}

function getModdle() {
  return modeler?.get('moddle')
}

function getRootElement() {
  return getCanvas()?.getRootElement() || null
}

function getProcessElement() {
  const root = getRootElement()
  return root || null
}

function switchMode(mode) {
  currentMode.value = mode
  emit('update:uiMode', mode)
}

async function importXml(xml, markDirty = false) {
  if (!modeler) {
    return
  }
  try {
    await modeler.importXML(xml)
    resetZoom()
    isReady.value = true
    isDirty.value = markDirty
    syncSelectedMeta(getProcessElement())
    renderDesignerMetaOverlays()
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
  return enrichActivitiXml(xml)
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

async function syncProcessMeta({ processId, processName } = {}) {
  const processElement = getProcessElement()
  const updates = {}
  if (processId) {
    updates.id = processId
  }
  if (typeof processName === 'string') {
    updates.name = processName
  }
  if (processElement && Object.keys(updates).length) {
    getModeling()?.updateProperties(processElement, updates)
    syncSelectedMeta(selectedElement.value || processElement)
  }
}

function syncSelectedMeta(element = null) {
  const target = element || getProcessElement()
  if (!target) {
    return
  }

  selectedElement.value = target
  const businessObject = target.businessObject || {}
  const docs = Array.isArray(businessObject.documentation) ? businessObject.documentation : []
  const designerMeta = readDesignerMeta(businessObject)
  const conditionExpression = businessObject.conditionExpression?.body || ''

  selectedMeta.id = businessObject.id || target.id || ''
  selectedMeta.name = businessObject.name || ''
  selectedMeta.type = businessObject.$type || target.type || 'bpmn:Process'
  selectedMeta.typeLabel = readableType(selectedMeta.type)
  selectedMeta.documentation = readPlainDocumentation(docs)
  selectedMeta.conditionExpression = conditionExpression
  selectedMeta.executable = businessObject.isExecutable !== false
  selectedMeta.isSequenceFlow = selectedMeta.type === 'bpmn:SequenceFlow'
  selectedMeta.assignee = designerMeta.assignee || ''
  selectedMeta.candidateUsers = designerMeta.candidateUsers || ''
  selectedMeta.candidateGroups = designerMeta.candidateGroups || ''
  selectedMeta.formKey = designerMeta.formKey || ''
  selectedMeta.todoPage = designerMeta.todoPage || ''
  selectedMeta.donePage = designerMeta.donePage || ''
  selectedMeta.dueExpression = designerMeta.dueExpression || ''
  selectedMeta.priority = designerMeta.priority || ''
  selectedMeta.sort = designerMeta.sort || ''
  selectedMeta.listenerBean = designerMeta.listenerBean || ''
  selectedMeta.listenerMethod = designerMeta.listenerMethod || ''
  selectedMeta.taskListenerBean = designerMeta.taskListenerBean || ''
  selectedMeta.taskCreateMethod = designerMeta.taskCreateMethod || ''
  selectedMeta.taskCompleteMethod = designerMeta.taskCompleteMethod || ''
  selectedMeta.serviceClass = designerMeta.serviceClass || ''
  selectedMeta.serviceExpression = designerMeta.serviceExpression || ''
  selectedMeta.serviceDelegateExpression = designerMeta.serviceDelegateExpression || ''
  selectedMeta.asyncMode = resolveAsyncMode(designerMeta)
  selectedMeta.exclusiveJob = designerMeta.exclusiveJob !== false
  selectedMeta.asyncBefore = Boolean(designerMeta.asyncBefore)
  selectedMeta.asyncAfter = Boolean(designerMeta.asyncAfter)
  renderDesignerMetaOverlays()
  updateQuickToolbarPosition()
  hideContextMenu()
}

function readableType(type) {
  const map = {
    'bpmn:Process': '流程',
    'bpmn:StartEvent': '开始事件',
    'bpmn:EndEvent': '结束事件',
    'bpmn:UserTask': '人工任务',
    'bpmn:ServiceTask': '服务任务',
    'bpmn:ExclusiveGateway': '排他网关',
    'bpmn:ParallelGateway': '并行网关',
    'bpmn:SubProcess': '子流程',
    'bpmn:SequenceFlow': '连线'
  }
  return map[type] || type
}

function supportsDesignerMetaOverlay(element) {
  const type = element?.businessObject?.$type || element?.type
  return ['bpmn:UserTask', 'bpmn:ServiceTask', 'bpmn:SubProcess', 'bpmn:StartEvent', 'bpmn:EndEvent'].includes(type)
}

function clearDesignerMetaOverlays() {
  const overlays = getOverlays()
  if (!overlays || !designerMetaOverlayIds.length) {
    designerMetaOverlayIds = []
    return
  }
  designerMetaOverlayIds.forEach((overlayId) => overlays.remove(overlayId))
  designerMetaOverlayIds = []
}

function buildDesignerMetaBadges(element) {
  if (!supportsDesignerMetaOverlay(element) || element.labelTarget) {
    return []
  }
  const metadata = readDesignerMeta(element.businessObject || {})
  const badges = []
  const formDefinition = findTaskFormDefinition(metadata.formKey)
  const todoPageDefinition = findTaskPageDefinition(metadata.todoPage, 'todo')
  const donePageDefinition = findTaskPageDefinition(metadata.donePage, 'done')

  if (metadata.formKey) {
    badges.push({
      itemKey: metadata.formKey,
      itemType: 'FORM',
      tone: 'form',
      prefix: '表单',
      text: formDefinition?.label || metadata.formKey
    })
  }
  if (metadata.todoPage) {
    badges.push({
      itemKey: metadata.todoPage,
      itemType: 'PAGE',
      tone: 'todo',
      prefix: '待办',
      text: todoPageDefinition?.label || metadata.todoPage
    })
  }
  if (metadata.donePage) {
    badges.push({
      itemKey: metadata.donePage,
      itemType: 'PAGE',
      tone: 'done',
      prefix: '完成',
      text: donePageDefinition?.label || metadata.donePage
    })
  }
  return badges
}

function renderDesignerMetaOverlays() {
  const overlays = getOverlays()
  const registry = getElementRegistry()
  if (!overlays || !registry) {
    return
  }
  clearDesignerMetaOverlays()
  if (!props.showMetaBadges) {
    return
  }
  registry.getAll().forEach((element) => {
    const badges = buildDesignerMetaBadges(element)
    if (!badges.length) {
      return
    }
    const container = document.createElement('div')
    container.className = 'designer-meta-overlay'
    if (selectedElement.value?.id === element.id) {
      container.classList.add('is-selected')
    }
    badges.forEach((badge) => {
      const badgeNode = document.createElement('span')
      badgeNode.className = `designer-meta-badge ${badge.tone}`
      badgeNode.textContent = `${badge.prefix} · ${badge.text}`
      badgeNode.title = '点击跳转到业务页目录详情'
      badgeNode.addEventListener('click', (event) => {
        event.preventDefault()
        event.stopPropagation()
        router.push({
          name: 'runtime-catalog',
          query: {
            itemKey: badge.itemKey,
            itemType: badge.itemType,
            source: 'designer'
          }
        })
      })
      container.appendChild(badgeNode)
    })
    const overlayId = overlays.add(element, {
      position: {
        top: -18,
        left: Math.max(0, Math.round((element.width || 80) / 2) - 36)
      },
      html: container
    })
    designerMetaOverlayIds.push(overlayId)
  })
}

function insertElement(item, dropPoint = null) {
  if (!modeler) {
    return
  }

  const root = getRootElement()
  const elementFactory = getElementFactory()
  const modeling = getModeling()
  const canvas = getCanvas()
  if (!root || !elementFactory || !modeling || !canvas) {
    return
  }

  const shape = elementFactory.createShape({
    type: item.type,
    isExpanded: item.isExpanded
  })

  const viewbox = canvas.viewbox()
  const position = dropPoint || {
    x: viewbox.x + viewbox.width / 2 - (shape.width || 80) / 2,
    y: viewbox.y + viewbox.height / 2 - (shape.height || 80) / 2
  }

  const created = modeling.createShape(shape, position, root)
  const current = selectedElement.value
  if (current && current !== root && current.type !== 'bpmn:SequenceFlow') {
    try {
      modeling.connect(current, created)
    } catch (error) {
      // ignore invalid auto-connect
    }
  }
  getSelection()?.select(created)
  syncSelectedMeta(created)
}

function handlePaletteDragStart(item) {
  draggedPaletteItem.value = item
}

function handleCanvasDrop(event) {
  if (!draggedPaletteItem.value || !modeler) {
    return
  }
  const rect = canvasRef.value.getBoundingClientRect()
  const viewbox = getCanvas()?.viewbox()
  if (!viewbox) {
    return
  }
  const x = viewbox.x + (event.clientX - rect.left) / viewbox.scale
  const y = viewbox.y + (event.clientY - rect.top) / viewbox.scale
  insertElement(draggedPaletteItem.value, { x, y })
  draggedPaletteItem.value = null
}

function updateSelectedId(value) {
  if (!selectedElement.value || !value.trim()) {
    return
  }
  getModeling()?.updateProperties(selectedElement.value, { id: value.trim() })
  syncSelectedMeta(selectedElement.value)
}

function updateSelectedName(value) {
  if (!selectedElement.value) {
    return
  }
  const cleanValue = value ?? ''
  if (selectedIsProcess.value) {
    getModeling()?.updateProperties(selectedElement.value, { name: cleanValue })
  } else {
    getModeling()?.updateLabel(selectedElement.value, cleanValue)
  }
  syncSelectedMeta(selectedElement.value)
}

function updateProcessExecutable(checked) {
  if (!selectedIsProcess.value || !selectedElement.value) {
    return
  }
  getModeling()?.updateProperties(selectedElement.value, { isExecutable: checked })
  syncSelectedMeta(selectedElement.value)
}

function updateDocumentation(text) {
  if (!selectedElement.value || selectedIsProcess.value) {
    return
  }
  updateDocumentationBundle({
    documentation: text
  })
}

function updateDesignerMeta(field, value) {
  if (!selectedElement.value || selectedIsProcess.value) {
    return
  }
  const metadataPatch = field === 'exclusiveJob' && !value && selectedMeta.asyncMode === 'sync'
    ? { exclusiveJob: false, asyncMode: 'sync', asyncBefore: false, asyncAfter: false }
    : { [field]: value }
  updateDocumentationBundle({
    metadataPatch
  })
}

function applyRecommendedBusinessPages() {
  if (!selectedElement.value || selectedIsProcess.value) {
    return
  }
  const recommended = resolveRecommendedPages(selectedMeta.formKey)
  updateDocumentationBundle({
    metadataPatch: {
      todoPage: recommended.todoPage,
      donePage: recommended.donePage
    }
  })
}

function updateAsyncMode(mode) {
  if (!selectedElement.value || selectedIsProcess.value) {
    return
  }
  const normalizedMode = mode === 'async' ? 'async' : 'sync'
  updateDocumentationBundle({
    metadataPatch: {
      asyncMode: normalizedMode,
      asyncBefore: normalizedMode === 'async',
      asyncAfter: false,
      exclusiveJob: normalizedMode === 'async' ? selectedMeta.exclusiveJob !== false : true
    }
  })
}

function cycleAsyncMode() {
  updateAsyncMode(selectedMeta.asyncMode === 'async' ? 'sync' : 'async')
}

function updateDocumentationBundle({ documentation, metadataPatch } = {}) {
  if (!selectedElement.value || selectedIsProcess.value) {
    return
  }
  const moddle = getModdle()
  if (!moddle) {
    return
  }
  const businessObject = selectedElement.value.businessObject || {}
  const existingDocs = Array.isArray(businessObject.documentation) ? businessObject.documentation : []
  const currentDocumentation = documentation === undefined ? readPlainDocumentation(existingDocs) : documentation
  const cleanText = currentDocumentation?.trim() || ''
  const existingMeta = readDesignerMeta(businessObject)
  const nextMeta = sanitizeDesignerMeta({
    ...existingMeta,
    ...(metadataPatch || {})
  })
  const docs = []
  if (cleanText) {
    docs.push(
      moddle.create('bpmn:Documentation', {
        text: cleanText,
        textFormat: 'text/plain'
      })
    )
  }
  if (Object.keys(nextMeta).length) {
    docs.push(
      moddle.create('bpmn:Documentation', {
        text: JSON.stringify(nextMeta, null, 2),
        textFormat: DESIGNER_META_DOC_FORMAT
      })
    )
  }
  getModeling()?.updateProperties(selectedElement.value, { documentation: docs })
  syncSelectedMeta(selectedElement.value)
}

function updateConditionExpression(text) {
  if (!selectedElement.value || !selectedMeta.isSequenceFlow) {
    return
  }
  const cleanText = text?.trim() || ''
  const moddle = getModdle()
  if (!moddle) {
    return
  }
  const conditionExpression = cleanText
    ? moddle.create('bpmn:FormalExpression', { body: cleanText })
    : undefined
  getModeling()?.updateProperties(selectedElement.value, { conditionExpression })
  syncSelectedMeta(selectedElement.value)
}

function deleteSelected() {
  if (!selectedElement.value || selectedIsProcess.value) {
    return
  }
  getModeling()?.removeElements([selectedElement.value])
  focusProcess()
}

function appendElement(type) {
  if (!selectedCanAppend.value || !selectedElement.value) {
    return
  }
  const root = getRootElement()
  const elementFactory = getElementFactory()
  const modeling = getModeling()
  if (!root || !elementFactory || !modeling) {
    return
  }

  const current = selectedElement.value
  const shape = elementFactory.createShape({ type })
  const position = {
    x: current.x + current.width + 140,
    y: current.y + Math.max((current.height - (shape.height || 80)) / 2, -20)
  }

  const created = modeling.createShape(shape, position, root)
  try {
    modeling.connect(current, created)
  } catch (error) {
    // ignore invalid connect cases
  }
  getSelection()?.select(created)
  syncSelectedMeta(created)
}

function insertOnSequenceFlow(type) {
  if (!selectedCanInsertOnFlow.value || !selectedElement.value) {
    return
  }
  const root = getRootElement()
  const elementFactory = getElementFactory()
  const modeling = getModeling()
  const connection = selectedElement.value
  const source = connection.source
  const target = connection.target
  if (!root || !elementFactory || !modeling || !source || !target) {
    return
  }

  const shape = elementFactory.createShape({ type })
  const midpoint = resolveConnectionMidpoint(connection)
  modeling.removeElements([connection])

  const created = modeling.createShape(
    shape,
    {
      x: midpoint.x - (shape.width || 80) / 2,
      y: midpoint.y - (shape.height || 80) / 2
    },
    root
  )

  try {
    modeling.connect(source, created)
    modeling.connect(created, target)
  } catch (error) {
    // ignore invalid reconnect cases
  }

  getSelection()?.select(created)
  syncSelectedMeta(created)
}

function updateQuickToolbarPosition() {
  if (!stageRef.value || !canvasRef.value || !selectedElement.value || selectedIsProcess.value || currentMode.value !== 'studio') {
    quickToolbar.visible = false
    return
  }

  const stageRect = stageRef.value.getBoundingClientRect()
  const canvasRect = canvasRef.value.getBoundingClientRect()
  const viewbox = getCanvas()?.viewbox()
  if (!viewbox) {
    quickToolbar.visible = false
    return
  }

  const anchor = selectedMeta.isSequenceFlow
    ? resolveConnectionMidpoint(selectedElement.value)
    : {
        x: selectedElement.value.x + selectedElement.value.width / 2,
        y: selectedElement.value.y
      }

  quickToolbar.left = (anchor.x - viewbox.x) * viewbox.scale + (canvasRect.left - stageRect.left)
  quickToolbar.top = (anchor.y - viewbox.y) * viewbox.scale + (canvasRect.top - stageRect.top) - 16
  quickToolbar.visible = true
}

function hideContextMenu() {
  contextMenu.visible = false
  contextMenu.actions = []
}

function openContextMenu(element, nativeEvent) {
  if (!stageRef.value || !nativeEvent || currentMode.value !== 'studio') {
    return
  }
  const stageRect = stageRef.value.getBoundingClientRect()
  contextMenu.x = nativeEvent.clientX - stageRect.left
  contextMenu.y = nativeEvent.clientY - stageRect.top
  contextMenu.actions = buildContextMenuActions(element)
  contextMenu.visible = contextMenu.actions.length > 0
}

function buildContextMenuActions(element) {
  if (!element || element.type === 'bpmn:Process') {
    return []
  }
  if (element.type === 'bpmn:SequenceFlow') {
    return [
      {
        key: 'insert-user-task',
        label: '插入人工任务',
        run: () => insertOnSequenceFlow('bpmn:UserTask')
      },
      {
        key: 'insert-service-task',
        label: '插入服务任务',
        run: () => insertOnSequenceFlow('bpmn:ServiceTask')
      },
      {
        key: 'insert-gateway',
        label: '插入排他网关',
        run: () => insertOnSequenceFlow('bpmn:ExclusiveGateway')
      },
      {
        key: 'delete-flow',
        label: '删除连线',
        danger: true,
        run: () => deleteSelected()
      }
    ]
  }
  const actions = [
    {
      key: 'append-user-task',
      label: '后接人工任务',
      run: () => appendElement('bpmn:UserTask')
    },
    {
      key: 'append-service-task',
      label: '后接服务任务',
      run: () => appendElement('bpmn:ServiceTask')
    },
    {
      key: 'append-gateway',
      label: '后接排他网关',
      run: () => appendElement('bpmn:ExclusiveGateway')
    }
  ]
  if (selectedSupportsWorkflowMeta.value) {
    actions.push({
      key: 'toggle-async',
      label: selectedMeta.asyncMode === 'async' ? '切回同步执行' : '切为异步执行',
      run: () => cycleAsyncMode()
    })
  }
  actions.push({
    key: 'delete-node',
    label: '删除节点',
    danger: true,
    run: () => deleteSelected()
  })
  return actions
}

function runContextMenuAction(action) {
  hideContextMenu()
  action?.run?.()
}

function focusProcess() {
  const processElement = getProcessElement()
  if (processElement) {
    getSelection()?.select(processElement)
    syncSelectedMeta(processElement)
  }
}

function focusElementById(elementId) {
  if (!elementId) {
    return false
  }
  const registry = getElementRegistry()
  const canvas = getCanvas()
  const target = registry?.get(elementId)
  if (!target || target.labelTarget) {
    return false
  }
  getSelection()?.select(target)
  canvas?.scrollToElement(target, {
    top: 120,
    left: 120
  })
  syncSelectedMeta(target)
  return true
}

function undo() {
  modeler?.get('commandStack')?.undo()
}

function redo() {
  modeler?.get('commandStack')?.redo()
}

function zoom(step) {
  const canvas = getCanvas()
  if (!canvas) {
    return
  }
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
  scheduleFitViewport()
}

function scheduleFitViewport(attempt = 0) {
  const canvas = getCanvas()
  const container = canvasRef.value
  if (!canvas || !container) {
    return
  }
  const width = container.clientWidth
  const height = container.clientHeight
  if (!Number.isFinite(width) || !Number.isFinite(height) || width <= 0 || height <= 0) {
    if (attempt < 10) {
      requestAnimationFrame(() => scheduleFitViewport(attempt + 1))
    }
    return
  }
  try {
    canvas.resized?.()
    canvas.zoom('fit-viewport', 'auto')
  } catch (error) {
    if (attempt < 10) {
      requestAnimationFrame(() => scheduleFitViewport(attempt + 1))
    }
  }
}

function readPlainDocumentation(docs) {
  const plainDoc = docs.find((item) => item?.textFormat !== DESIGNER_META_DOC_FORMAT)
  return plainDoc?.text || ''
}

function readDesignerMeta(businessObject) {
  const attrsMeta = {
    ...readActivitiAttrs(businessObject),
    ...readActivitiListeners(businessObject)
  }
  const docs = Array.isArray(businessObject?.documentation) ? businessObject.documentation : []
  const metaDoc = docs.find((item) => item?.textFormat === DESIGNER_META_DOC_FORMAT)
  if (!metaDoc?.text) {
    return attrsMeta
  }
  try {
    return sanitizeDesignerMeta({
      ...attrsMeta,
      ...JSON.parse(metaDoc.text)
    })
  } catch (error) {
    return attrsMeta
  }
}

function readActivitiAttrs(businessObject) {
  const attrs = businessObject?.$attrs || {}
  return sanitizeDesignerMeta({
    assignee: attrs['activiti:assignee'],
    candidateUsers: attrs['activiti:candidateUsers'],
    candidateGroups: attrs['activiti:candidateGroups'],
    formKey: attrs['activiti:formKey'],
    dueExpression: attrs['activiti:dueDate'],
    priority: attrs['activiti:priority'],
    serviceClass: attrs['activiti:class'],
    serviceExpression: attrs['activiti:expression'],
    serviceDelegateExpression: attrs['activiti:delegateExpression'],
    exclusiveJob: attrs['activiti:exclusive'] !== 'false',
    asyncMode: attrs['activiti:async'] === 'true' ? 'async' : 'sync',
    asyncBefore: attrs['activiti:async'] === 'true',
    asyncAfter: false
  })
}

function readActivitiListeners(businessObject) {
  const values = Array.isArray(businessObject?.extensionElements?.values)
    ? businessObject.extensionElements.values
    : []
  const metadata = {
    listenerBean: '',
    listenerMethod: '',
    taskListenerBean: '',
    taskCreateMethod: '',
    taskCompleteMethod: ''
  }

  values.forEach((item) => {
    const typeName = item?.$type || item?.$descriptor?.name || ''
    const expression = item?.expression || item?.$attrs?.expression || ''
    const event = item?.event || item?.$attrs?.event || ''
    const { bean, method } = parseListenerExpression(expression)
    if (!bean || !method) {
      return
    }

    if (String(typeName).includes('ExecutionListener')) {
      metadata.listenerBean = metadata.listenerBean || bean
      if (event === 'start') {
        metadata.listenerMethod = joinListenerMethods(method, metadata.listenerMethod.split(',')[1] || '')
      } else if (event === 'end') {
        metadata.listenerMethod = joinListenerMethods(metadata.listenerMethod.split(',')[0] || '', method)
      }
    }

    if (String(typeName).includes('TaskListener')) {
      metadata.taskListenerBean = metadata.taskListenerBean || bean
      if (event === 'create') {
        metadata.taskCreateMethod = method
      } else if (event === 'complete') {
        metadata.taskCompleteMethod = method
      }
    }
  })

  return sanitizeDesignerMeta(metadata)
}

function sanitizeDesignerMeta(meta = {}) {
  return Object.entries(meta).reduce((accumulator, [key, value]) => {
    if (typeof value === 'string') {
      const trimmed = value.trim()
      if (trimmed) {
        accumulator[key] = trimmed
      }
      return accumulator
    }
    if (typeof value === 'boolean') {
      accumulator[key] = value
      return accumulator
    }
    if (value !== undefined && value !== null && value !== '') {
      accumulator[key] = value
    }
    return accumulator
  }, {})
}

function resolveAsyncMode(metadata = {}) {
  if (metadata.asyncMode === 'async') {
    return 'async'
  }
  if (metadata.asyncBefore || metadata.asyncAfter) {
    return 'async'
  }
  return 'sync'
}

function resolveConnectionMidpoint(connection) {
  const waypoints = Array.isArray(connection?.waypoints) ? connection.waypoints : []
  if (!waypoints.length) {
    return {
      x: (connection.source?.x || 0) + 160,
      y: connection.source?.y || 120
    }
  }
  const middleIndex = Math.floor(waypoints.length / 2)
  const current = waypoints[middleIndex]
  const previous = waypoints[Math.max(0, middleIndex - 1)]
  return {
    x: (current.x + previous.x) / 2,
    y: (current.y + previous.y) / 2
  }
}

function enrichActivitiXml(xml) {
  if (!xml) {
    return xml
  }
  const parser = new DOMParser()
  const doc = parser.parseFromString(xml, 'application/xml')
  if (doc.querySelector('parsererror')) {
    return xml
  }

  const definitions = doc.documentElement
  if (definitions && !definitions.getAttribute('xmlns:activiti')) {
    definitions.setAttribute('xmlns:activiti', ACTIVITI_NAMESPACE)
  }

  doc.querySelectorAll('bpmn\\:userTask, userTask, bpmn\\:serviceTask, serviceTask').forEach((node) => {
    const metadata = readDesignerMetaFromNode(node)
    applyActivitiAttributes(node, metadata)
    applyExecutionListenerExtensions(doc, node, metadata)
    applyTaskListenerExtensions(doc, node, metadata)
  })

  const serialized = new XMLSerializer().serializeToString(doc)
  if (serialized.startsWith('<?xml')) {
    return serialized
  }
  return `<?xml version="1.0" encoding="UTF-8"?>\n${serialized}`
}

function readDesignerMetaFromNode(node) {
  const metadata = {
    assignee: node.getAttribute('activiti:assignee') || '',
    candidateUsers: node.getAttribute('activiti:candidateUsers') || '',
    candidateGroups: node.getAttribute('activiti:candidateGroups') || '',
    formKey: node.getAttribute('activiti:formKey') || '',
    dueExpression: node.getAttribute('activiti:dueDate') || '',
    priority: node.getAttribute('activiti:priority') || '',
    serviceClass: node.getAttribute('activiti:class') || '',
    serviceExpression: node.getAttribute('activiti:expression') || '',
    serviceDelegateExpression: node.getAttribute('activiti:delegateExpression') || '',
    listenerBean: '',
    listenerMethod: '',
    taskListenerBean: '',
    taskCreateMethod: '',
    taskCompleteMethod: ''
  }
  Array.from(node.children)
    .filter((child) => child.localName === 'documentation')
    .forEach((docNode) => {
    const format = docNode.getAttribute('textFormat') || ''
    if (format === DESIGNER_META_DOC_FORMAT) {
      try {
        Object.assign(metadata, JSON.parse(docNode.textContent || '{}'))
      } catch (error) {
        // ignore malformed metadata blocks
      }
      }
    })
  const extensionElements = Array.from(node.children).find((child) => child.localName === 'extensionElements')
  if (extensionElements) {
    Array.from(extensionElements.children).forEach((child) => {
      const expression = child.getAttribute('expression') || ''
      const event = child.getAttribute('event') || ''
      const { bean, method } = parseListenerExpression(expression)
      if (!bean || !method) {
        return
      }
      if (child.localName === 'executionListener') {
        metadata.listenerBean = metadata.listenerBean || bean
        if (event === 'start') {
          metadata.listenerMethod = joinListenerMethods(method, metadata.listenerMethod.split(',')[1] || '')
        } else if (event === 'end') {
          metadata.listenerMethod = joinListenerMethods(metadata.listenerMethod.split(',')[0] || '', method)
        }
      }
      if (child.localName === 'taskListener') {
        metadata.taskListenerBean = metadata.taskListenerBean || bean
        if (event === 'create') {
          metadata.taskCreateMethod = method
        } else if (event === 'complete') {
          metadata.taskCompleteMethod = method
        }
      }
    })
  }
  return sanitizeDesignerMeta(metadata)
}

function applyActivitiAttributes(node, metadata) {
  Object.values(ACTIVITI_ATTRIBUTE_MAP).forEach((attributeName) => {
    node.removeAttribute(attributeName)
  })
  node.removeAttribute('activiti:async')
  node.removeAttribute('activiti:exclusive')

  const taskName = node.localName
  if (taskName === 'userTask') {
    setXmlAttribute(node, 'assignee', metadata.assignee)
    setXmlAttribute(node, 'candidateUsers', metadata.candidateUsers)
    setXmlAttribute(node, 'candidateGroups', metadata.candidateGroups)
    setXmlAttribute(node, 'formKey', metadata.formKey)
    setXmlAttribute(node, 'dueExpression', metadata.dueExpression)
    setXmlAttribute(node, 'priority', metadata.priority)
  } else if (taskName === 'serviceTask') {
    setXmlAttribute(node, 'serviceClass', metadata.serviceClass)
    setXmlAttribute(node, 'serviceExpression', metadata.serviceExpression)
    setXmlAttribute(node, 'serviceDelegateExpression', metadata.serviceDelegateExpression)
  }

  const asyncMode = resolveAsyncMode(metadata)
  if (asyncMode === 'async') {
    node.setAttribute('activiti:async', 'true')
    node.setAttribute('activiti:exclusive', metadata.exclusiveJob === false ? 'false' : 'true')
  }
}

function setXmlAttribute(node, field, value) {
  const attributeName = ACTIVITI_ATTRIBUTE_MAP[field]
  const cleanValue = typeof value === 'string' ? value.trim() : value
  if (!attributeName || !cleanValue) {
    return
  }
  node.setAttribute(attributeName, cleanValue)
}

function applyExecutionListenerExtensions(doc, node, metadata) {
  const listeners = buildExecutionListeners(metadata)
  const extensionElements = ensureExtensionElements(doc, node)

  Array.from(extensionElements.children)
    .filter((child) => child.localName === 'executionListener')
    .forEach((child) => extensionElements.removeChild(child))

  listeners.forEach((listener) => {
    const listenerNode = doc.createElementNS(ACTIVITI_NAMESPACE, 'activiti:executionListener')
    listenerNode.setAttribute('event', listener.event)
    listenerNode.setAttribute('expression', listener.expression)
    extensionElements.appendChild(listenerNode)
  })
  cleanupEmptyExtensionElements(node, extensionElements)
}

function buildExecutionListeners(metadata) {
  const bean = metadata.listenerBean?.trim()
  const methods = String(metadata.listenerMethod || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)

  if (!bean || !methods.length) {
    return []
  }

  if (methods.length === 1) {
    return [
      {
        event: 'start',
        expression: buildListenerExpression(bean, methods[0])
      }
    ]
  }

  return methods.slice(0, 2).map((method, index) => ({
    event: index === 0 ? 'start' : 'end',
    expression: buildListenerExpression(bean, method)
  }))
}

function buildListenerExpression(bean, method) {
  return `\${${bean}.${method}(execution)}`
}

function applyTaskListenerExtensions(doc, node, metadata) {
  if (node.localName !== 'userTask') {
    return
  }
  const listeners = buildTaskListeners(metadata)
  const extensionElements = ensureExtensionElements(doc, node)

  Array.from(extensionElements.children)
    .filter((child) => child.localName === 'taskListener')
    .forEach((child) => extensionElements.removeChild(child))

  listeners.forEach((listener) => {
    const listenerNode = doc.createElementNS(ACTIVITI_NAMESPACE, 'activiti:taskListener')
    listenerNode.setAttribute('event', listener.event)
    listenerNode.setAttribute('expression', listener.expression)
    extensionElements.appendChild(listenerNode)
  })
  cleanupEmptyExtensionElements(node, extensionElements)
}

function buildTaskListeners(metadata) {
  const bean = metadata.taskListenerBean?.trim()
  if (!bean) {
    return []
  }
  const listeners = []
  const createMethod = metadata.taskCreateMethod?.trim()
  const completeMethod = metadata.taskCompleteMethod?.trim()
  if (createMethod) {
    listeners.push({
      event: 'create',
      expression: buildTaskListenerExpression(bean, createMethod)
    })
  }
  if (completeMethod) {
    listeners.push({
      event: 'complete',
      expression: buildTaskListenerExpression(bean, completeMethod)
    })
  }
  return listeners
}

function buildTaskListenerExpression(bean, method) {
  return `\${${bean}.${method}(task)}`
}

function ensureExtensionElements(doc, node) {
  const existing = Array.from(node.children).find((child) => child.localName === 'extensionElements')
  if (existing) {
    return existing
  }
  const extensionElements = doc.createElementNS('http://www.omg.org/spec/BPMN/20100524/MODEL', 'bpmn:extensionElements')
  const firstDocumentation = Array.from(node.children).find((child) => child.localName === 'documentation')
  if (firstDocumentation) {
    node.insertBefore(extensionElements, firstDocumentation.nextSibling)
  } else {
    node.insertBefore(extensionElements, node.firstChild)
  }
  return extensionElements
}

function cleanupEmptyExtensionElements(node, extensionElements) {
  if (extensionElements && !extensionElements.children.length) {
    node.removeChild(extensionElements)
  }
}

function parseListenerExpression(expression) {
  const matched = String(expression || '').match(/^\$\{([^.\s]+)\.([^(]+)\((execution|task)\)\}$/)
  if (!matched) {
    return { bean: '', method: '' }
  }
  return {
    bean: matched[1] || '',
    method: matched[2] || ''
  }
}

function joinListenerMethods(startMethod, endMethod) {
  return [startMethod, endMethod].filter(Boolean).join(',')
}

const selectedIsProcess = computed(() => selectedMeta.type === 'bpmn:Process')
const selectedIsUserTask = computed(() => selectedMeta.type === 'bpmn:UserTask')
const selectedIsServiceTask = computed(() => selectedMeta.type === 'bpmn:ServiceTask')
const selectedSupportsWorkflowMeta = computed(() => {
  return ['bpmn:UserTask', 'bpmn:ServiceTask', 'bpmn:SubProcess', 'bpmn:StartEvent', 'bpmn:EndEvent'].includes(selectedMeta.type)
})
const selectedSupportsAssignment = computed(() => ['bpmn:UserTask', 'bpmn:SubProcess'].includes(selectedMeta.type))
const selectedCanAppend = computed(() => {
  return Boolean(selectedElement.value) && !selectedIsProcess.value && !selectedMeta.isSequenceFlow
})
const selectedCanInsertOnFlow = computed(() => {
  return selectedMeta.isSequenceFlow && Boolean(selectedElement.value?.source) && Boolean(selectedElement.value?.target)
})
const showQuickToolbar = computed(() => quickToolbar.visible && currentMode.value === 'studio')
const quickToolbarStyle = computed(() => ({
  left: `${quickToolbar.left}px`,
  top: `${quickToolbar.top}px`
}))
const contextMenuStyle = computed(() => ({
  left: `${contextMenu.x}px`,
  top: `${contextMenu.y}px`
}))
const asyncModeLabel = computed(() => (selectedMeta.asyncMode === 'async' ? '异步执行' : '同步执行'))
const designerFormOptions = computed(() => getTaskFormOptions(selectedMeta.formKey))
const designerTodoPageOptions = computed(() => getTaskPageOptions('todo', selectedMeta.todoPage))
const designerDonePageOptions = computed(() => getTaskPageOptions('done', selectedMeta.donePage))
const selectedFormDefinition = computed(() => findTaskFormDefinition(selectedMeta.formKey))
const selectedTodoPageDefinition = computed(() => findTaskPageDefinition(selectedMeta.todoPage, 'todo'))
const selectedDonePageDefinition = computed(() => findTaskPageDefinition(selectedMeta.donePage, 'done'))

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

const selectedSummary = computed(() => `${selectedMeta.typeLabel} · ${selectedMeta.name || selectedMeta.id || '未命名'}`)

watch(
  () => props.uiMode,
  (value) => {
    if (value && value !== currentMode.value) {
      currentMode.value = value
      updateQuickToolbarPosition()
      hideContextMenu()
    }
  }
)

watch(
  () => [selectedMeta.asyncMode, selectedMeta.isSequenceFlow, currentMode.value],
  () => {
    updateQuickToolbarPosition()
  }
)

watch(
  () => props.showMetaBadges,
  () => {
    renderDesignerMetaOverlays()
  }
)

onMounted(async () => {
  await ensureTaskPageCatalogLoaded()
  modeler = new BpmnModeler({
    container: canvasRef.value,
    keyboard: {
      bindTo: window
    }
  })

  modeler.on('selection.changed', ({ newSelection }) => {
    syncSelectedMeta(newSelection?.[0] || getProcessElement())
  })

  modeler.on('element.contextmenu', ({ element, originalEvent }) => {
    originalEvent?.preventDefault?.()
    if (element) {
      getSelection()?.select(element)
      syncSelectedMeta(element)
      openContextMenu(element, originalEvent)
    }
  })

  modeler.on('canvas.viewbox.changed', () => {
    updateQuickToolbarPosition()
    hideContextMenu()
  })

  modeler.on('commandStack.changed', async () => {
    if (!isReady.value) {
      return
    }
    isDirty.value = true
    syncSelectedMeta(selectedElement.value || getProcessElement())
    renderDesignerMetaOverlays()
    emit('changed', await getXml())
  })

  window.addEventListener('resize', updateQuickToolbarPosition)
  window.addEventListener('click', hideContextMenu)

  await createNewDiagram()
})

onUnmounted(() => {
  clearDesignerMetaOverlays()
  window.removeEventListener('resize', updateQuickToolbarPosition)
  window.removeEventListener('click', hideContextMenu)
  modeler?.destroy()
})

defineExpose({
  createNewDiagram,
  importXml,
  getXml,
  downloadXml,
  syncProcessMeta,
  focusElementById,
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
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #f7f3ea 0%, #f3eee4 100%);
  border: 1px solid rgba(73, 58, 37, 0.14);
  border-radius: 24px;
  overflow: hidden;
}

.designer-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  border-bottom: 1px solid rgba(73, 58, 37, 0.1);
  background: rgba(255, 252, 246, 0.94);
}

.designer-topbar-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.designer-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #5a4b34;
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

.mode-switch {
  display: inline-flex;
  padding: 4px;
  border-radius: 999px;
  background: rgba(125, 97, 49, 0.08);
}

.mode-btn {
  border: 0;
  background: transparent;
  color: #6d593c;
  border-radius: 999px;
  padding: 8px 14px;
  font-size: 13px;
  cursor: pointer;
}

.mode-btn.active {
  background: #fffaf0;
  color: #8f4d12;
  box-shadow: 0 4px 12px rgba(128, 90, 36, 0.12);
}

.designer-actions {
  display: inline-flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
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

.ghost-btn:disabled {
  opacity: 0.48;
  cursor: not-allowed;
}

.danger-ghost {
  color: #ab3f34;
  border-color: rgba(171, 63, 52, 0.22);
}

.designer-body {
  flex: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  min-height: 0;
}

.studio-mode .designer-body {
  grid-template-columns: 260px minmax(0, 1fr) 320px;
}

.designer-palette {
  border-right: 1px solid rgba(73, 58, 37, 0.1);
  background: linear-gradient(180deg, rgba(255, 250, 241, 0.96), rgba(248, 241, 228, 0.96));
  padding: 18px 16px;
  overflow: auto;
}

.palette-group + .palette-group {
  margin-top: 18px;
}

.palette-title {
  margin: 0 0 10px;
  color: #7c6541;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.palette-list {
  display: grid;
  gap: 10px;
}

.palette-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 5px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(73, 58, 37, 0.12);
  background: rgba(255, 255, 255, 0.85);
  color: #5b4a33;
  cursor: grab;
  text-align: left;
}

.palette-item span {
  font-size: 12px;
  color: #7c6c56;
}

.designer-stage {
  min-width: 0;
  display: flex;
  flex-direction: column;
  position: relative;
  background: linear-gradient(180deg, #f6f0e3 0%, #f3ebe0 100%);
}

.designer-stage-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 16px;
  border-bottom: 1px solid rgba(73, 58, 37, 0.08);
  color: #6b5a43;
  font-size: 12px;
  background: rgba(255, 252, 246, 0.68);
}

.designer-canvas {
  flex: 1;
  min-height: 680px;
}

.designer-stage :deep(.designer-meta-overlay) {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 72px;
  pointer-events: none;
}

.designer-stage :deep(.designer-meta-overlay.is-selected) {
  transform: translateY(-1px);
}

.designer-stage :deep(.designer-meta-badge) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 8px;
  border-radius: 999px;
  border: 1px solid rgba(77, 98, 127, 0.14);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 8px 18px rgba(20, 35, 57, 0.12);
  color: #33475f;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
  pointer-events: auto;
  cursor: pointer;
}

.designer-stage :deep(.designer-meta-badge.form) {
  background: rgba(255, 246, 231, 0.98);
  color: #8d5520;
}

.designer-stage :deep(.designer-meta-badge.todo) {
  background: rgba(235, 244, 231, 0.98);
  color: #2f7d32;
}

.designer-stage :deep(.designer-meta-badge.done) {
  background: rgba(236, 241, 247, 0.98);
  color: #2f5f8f;
}

.designer-quick-tools {
  position: absolute;
  z-index: 12;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px;
  transform: translate(-50%, -100%);
  border: 1px solid rgba(77, 98, 127, 0.14);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 28px rgba(20, 35, 57, 0.14);
  backdrop-filter: blur(12px);
}

.quick-group {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-right: 8px;
  border-right: 1px solid rgba(77, 98, 127, 0.1);
}

.quick-group:last-child {
  padding-right: 0;
  border-right: 0;
}

.quick-group-title {
  color: #6f8299;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.quick-tool-btn {
  border: 1px solid rgba(77, 98, 127, 0.14);
  border-radius: 999px;
  background: #fff;
  color: #41546b;
  padding: 6px 10px;
  font-size: 12px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.quick-tool-btn.danger {
  color: #ab3f34;
  border-color: rgba(171, 63, 52, 0.22);
}

.quick-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(41, 79, 129, 0.12);
  color: #2d5f98;
  font-size: 10px;
  font-weight: 700;
}

.designer-context-menu {
  position: absolute;
  z-index: 13;
  min-width: 170px;
  display: grid;
  gap: 4px;
  padding: 8px;
  border-radius: 16px;
  border: 1px solid rgba(77, 98, 127, 0.14);
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 16px 36px rgba(20, 35, 57, 0.18);
}

.context-menu-item {
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: #33475f;
  padding: 9px 10px;
  text-align: left;
  font-size: 13px;
  cursor: pointer;
}

.context-menu-item:hover {
  background: rgba(41, 79, 129, 0.08);
}

.context-menu-item.danger {
  color: #ab3f34;
}

.designer-inspector {
  border-left: 1px solid rgba(77, 98, 127, 0.1);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(244, 248, 253, 0.96));
  padding: 18px 16px;
  overflow: auto;
}

.inspector-head {
  margin-bottom: 16px;
}

.inspector-head h4 {
  margin: 4px 0 0;
  font-size: 18px;
  color: #33475f;
}

.inspector-kicker {
  margin: 0;
  color: #6f8299;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.inspector-form {
  display: grid;
  gap: 14px;
}

.inspector-section {
  display: grid;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(93, 72, 38, 0.1);
  background: rgba(255, 255, 255, 0.5);
}

.inspector-section-head {
  display: grid;
  gap: 4px;
}

.inspector-section-head strong {
  color: #4f3d24;
  font-size: 14px;
}

.inspector-section-head span {
  color: #866f49;
  font-size: 12px;
  line-height: 1.5;
}

.field-cluster {
  display: grid;
  gap: 12px;
}

.two-column {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field-block {
  display: grid;
  gap: 8px;
}

.field-block span,
.field-inline span {
  color: #6d593c;
  font-size: 13px;
}

.field-block input,
.field-block textarea,
.field-block select {
  width: 100%;
  box-sizing: border-box;
  border-radius: 14px;
  border: 1px solid rgba(86, 69, 43, 0.16);
  background: rgba(255, 255, 255, 0.92);
  padding: 10px 12px;
  color: #3d301d;
  font-size: 14px;
}

.field-block input:disabled {
  color: #877660;
  background: rgba(244, 239, 229, 0.92);
}

.field-block textarea {
  resize: vertical;
  min-height: 96px;
}

.field-inline {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.field-inline-card {
  min-height: 46px;
  padding: 0 12px;
  border-radius: 14px;
  border: 1px solid rgba(86, 69, 43, 0.16);
  background: rgba(255, 255, 255, 0.92);
}

.field-help {
  margin: 0;
  color: #7c6b53;
  font-size: 12px;
  line-height: 1.5;
}

.field-actions {
  display: grid;
  gap: 10px;
  align-items: start;
}

.mini-ghost-btn {
  justify-self: start;
  padding: 8px 14px;
  border-radius: 999px;
}

.checkbox-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.append-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.inspector-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

:deep(.djs-container) {
  background-image:
    radial-gradient(circle at 1px 1px, rgba(125, 102, 70, 0.08) 1px, transparent 0);
  background-size: 18px 18px;
}

:deep(.bjs-powered-by) {
  display: none;
}

@media (max-width: 1260px) {
  .studio-mode .designer-body {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .designer-inspector {
    grid-column: 1 / -1;
    border-left: 0;
    border-top: 1px solid rgba(73, 58, 37, 0.1);
  }
}

@media (max-width: 880px) {
  .designer-topbar,
  .designer-stage-hint {
    flex-direction: column;
    align-items: flex-start;
  }

  .studio-mode .designer-body {
    grid-template-columns: minmax(0, 1fr);
  }

  .designer-palette,
  .designer-inspector {
    border: 0;
    border-top: 1px solid rgba(73, 58, 37, 0.1);
  }

  .two-column {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
