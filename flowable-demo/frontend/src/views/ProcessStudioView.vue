<template>
  <div class="page-grid">
    <section class="studio-content">
      <section class="panel-card studio-console-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Design Console</p>
            <h3>流程设计与版本编排</h3>
          </div>
          <div class="management-meta">
            <span class="meta-pill">流程定义 {{ processDefinitions.length }}</span>
            <span class="meta-pill">激活版本 {{ activeDefinitionCount }}</span>
            <span class="meta-pill">设计模式 {{ designerModeLabel }}</span>
            <span class="meta-pill">表单目录 {{ formCatalogs.length }}</span>
            <span class="meta-pill">{{ hasUnsavedChanges ? '未保存改动' : '画布已同步' }}</span>
          </div>
        </div>

        <div class="studio-mode-switch">
          <button
            type="button"
            :class="['segment-btn', { active: designerMode === DESIGNER_TYPE_BPMN }]"
            @click="switchDesignerMode(DESIGNER_TYPE_BPMN)"
          >
            BPMN 设计器
          </button>
          <button
            type="button"
            :class="['segment-btn', { active: designerMode === DESIGNER_TYPE_CUSTOM }]"
            @click="switchDesignerMode(DESIGNER_TYPE_CUSTOM)"
          >
            Flowlong 设计器
          </button>
        </div>

        <div class="form-grid studio-form-grid">
          <label>
            <span>流程 Key</span>
            <input
              v-model.trim="processForm.processKey"
              type="text"
              placeholder="leave-process"
              :disabled="Boolean(selectedProcessId)"
            />
          </label>
          <label>
            <span>流程名称</span>
            <input v-model.trim="processForm.processName" type="text" placeholder="请假审批流程" />
          </label>
        </div>

        <p class="hint">
          {{ designerMode === DESIGNER_TYPE_CUSTOM
            ? 'Flowlong 设计器负责可视化编排，保存时会自动转换为系统当前可部署的自定义流程 JSON 和 BPMN XML。'
            : 'BPMN 设计器继续直接编辑 XML，适合兼容现有流程定义与外部导入。' }}
        </p>

        <div class="action-row studio-action-row">
          <button type="button" class="primary-btn" @click="saveProcess" :disabled="loading">
            {{ loading ? '处理中...' : selectedProcessId ? '保存更新' : '保存新版本' }}
          </button>
          <button type="button" class="secondary-btn" @click="prepareLeaveProcessTemplate" :disabled="loading">
            请假流程模板
          </button>
          <button type="button" class="secondary-btn" @click="activateProcess" :disabled="loading || !selectedProcessId">
            激活版本
          </button>
          <button type="button" class="secondary-btn" @click="downloadXml" :disabled="loading">导出 XML</button>
          <button
            v-if="designerMode === DESIGNER_TYPE_CUSTOM"
            type="button"
            class="secondary-btn"
            @click="downloadCustomSchema"
            :disabled="loading"
          >
            导出设计 JSON
          </button>
          <label v-if="designerMode === DESIGNER_TYPE_BPMN" class="upload-btn">
            导入 XML
            <input ref="xmlInputRef" type="file" accept=".bpmn,.xml" @change="importLocalXml" />
          </label>
          <label v-else class="upload-btn">
            导入设计 JSON
            <input ref="schemaInputRef" type="file" accept=".json" @change="importLocalSchema" />
          </label>
          <button type="button" class="danger-btn" @click="deleteProcess" :disabled="loading || !selectedProcessId">
            删除
          </button>
        </div>

        <div v-if="statusMessage" class="feedback success">{{ statusMessage }}</div>
        <div v-if="errorMessage" class="feedback error">{{ errorMessage }}</div>
      </section>

      <section class="content-grid studio-summary-grid">
        <aside class="panel-card definition-panel summary-card">
          <div class="panel-head definition-panel-head">
            <div>
              <p class="eyebrow">Process Registry</p>
              <h3>流程定义</h3>
            </div>
            <button type="button" class="secondary-btn" @click="createProcess" :disabled="loading">新建</button>
          </div>

          <div class="management-meta">
            <span class="meta-pill">总数 {{ processDefinitions.length }}</span>
            <span class="meta-pill">激活 {{ activeDefinitionCount }}</span>
            <span class="meta-pill">筛选 {{ filteredDefinitions.length }}</span>
          </div>

          <div class="definition-filter-bar">
            <label class="filter-grow">
              <span>搜索流程</span>
              <input v-model.trim="keyword" type="text" placeholder="按流程名称、Key 或版本搜索" />
            </label>
            <label class="toggle-row definition-toggle">
              <span class="definition-toggle-copy">
                <strong>只看激活版本</strong>
                <small>和后端 ACTIVE 状态保持一致</small>
              </span>
              <input v-model="activeOnly" type="checkbox" />
            </label>
          </div>

          <div class="stack-list definition-list">
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
              <p class="definition-key">{{ definition.processKey }}</p>
              <div class="definition-meta-row">
                <small>版本 v{{ definition.version }}</small>
                <small>{{ formatDate(definition.updatedAt || definition.createdAt) }}</small>
              </div>
              <div class="definition-tags">
                <span class="meta-pill meta-pill-tight">
                  {{ definition.designerType === DESIGNER_TYPE_CUSTOM ? '自定义编排' : 'BPMN 设计器' }}
                </span>
              </div>
            </button>
            <div v-if="!filteredDefinitions.length" class="empty-state">
              {{ keyword ? '没有匹配的流程定义，试试调整关键词。' : '还没有流程定义，先新建一个。' }}
            </div>
          </div>
        </aside>

        <article class="panel-card summary-card">
          <div class="panel-head">
            <div>
              <p class="eyebrow">Version Snapshot</p>
              <h3>当前版本摘要</h3>
            </div>
            <span :class="['status-pill', selectedDefinition?.status === 'ACTIVE' ? 'success-pill' : 'muted-pill']">
              {{ selectedDefinition?.status || 'DRAFT' }}
            </span>
          </div>

          <div class="detail-grid studio-detail-grid">
            <article class="detail-item">
              <span class="detail-key">流程名称</span>
              <strong class="detail-value">{{ processForm.processName || '新建流程' }}</strong>
            </article>
            <article class="detail-item">
              <span class="detail-key">流程 Key</span>
              <strong class="detail-value">{{ processForm.processKey || normalizeProcessKey() }}</strong>
            </article>
            <article class="detail-item">
              <span class="detail-key">设计模式</span>
              <strong class="detail-value">{{ designerModeLabel }}</strong>
            </article>
            <article class="detail-item">
              <span class="detail-key">当前版本</span>
              <strong class="detail-value">{{ selectedDefinition?.version ? `v${selectedDefinition.version}` : '未保存' }}</strong>
            </article>
            <article class="detail-item">
              <span class="detail-key">最近保存</span>
              <strong class="detail-value">{{ lastSavedAt ? formatDate(lastSavedAt) : '尚未保存' }}</strong>
            </article>
            <article class="detail-item">
              <span class="detail-key">目录数量</span>
              <strong class="detail-value">{{ formCatalogs.length }}</strong>
            </article>
          </div>
        </article>

        <article class="panel-card summary-card">
          <div class="panel-head">
            <div>
              <p class="eyebrow">Recent Lane</p>
              <h3>最近版本</h3>
            </div>
          </div>

          <div class="stack-list">
            <div v-for="definition in recentDefinitions" :key="definition.id" class="list-item rich-list-item">
              <div>
                <strong>{{ definition.processName }}</strong>
                <p>{{ definition.processKey }} · v{{ definition.version }}</p>
              </div>
              <div class="recent-definition-side">
                <span class="status-pill">{{ definition.designerType === DESIGNER_TYPE_CUSTOM ? '自定义' : 'BPMN' }}</span>
                <span :class="['status-pill', definition.status === 'ACTIVE' ? 'success-pill' : 'muted-pill']">
                  {{ definition.status }}
                </span>
              </div>
            </div>
            <div v-if="!recentDefinitions.length" class="empty-state">保存流程后，这里会展示最近版本轨迹。</div>
          </div>
        </article>
      </section>

      <section class="designer-stage">
        <Suspense>
          <template #default>
            <ProcessDesigner
              v-show="designerMode === DESIGNER_TYPE_BPMN"
              ref="bpmnDesignerRef"
              class="designer-card"
              :form-catalogs="formCatalogs"
              @changed="handleDesignerChange"
              @error="handleDesignerError"
            />
          </template>
          <template #fallback>
            <section v-if="designerMode === DESIGNER_TYPE_BPMN" class="designer-loading-card">
              <p class="eyebrow">Designer Engine</p>
              <h3>流程设计器加载中</h3>
              <p class="muted-text">BPMN 画布和建模引擎会在进入当前页面后按需加载。</p>
            </section>
          </template>
        </Suspense>

        <FlowlongDesignerAdapter
          v-show="designerMode === DESIGNER_TYPE_CUSTOM"
          ref="customDesignerRef"
          class="designer-card"
          :form-catalogs="formCatalogs"
          @changed="handleDesignerChange"
          @error="handleDesignerError"
        />
      </section>
    </section>
  </div>
</template>

<script setup>
import { Suspense, computed, defineAsyncComponent, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { getCurrentUser, http } from '../api/http'
import FlowlongDesignerAdapter from '../components/flowlong/FlowlongDesignerAdapter.vue'
import { createLeaveCustomSchema } from '../utils/seaflowDesigner'

const ProcessDesigner = defineAsyncComponent(() => import('../components/ProcessDesigner.vue'))

const DESIGNER_TYPE_BPMN = 'BPMN'
const DESIGNER_TYPE_CUSTOM = 'CUSTOM'
const route = useRoute()
const router = useRouter()

const bpmnDesignerRef = ref(null)
const customDesignerRef = ref(null)
const xmlInputRef = ref(null)
const schemaInputRef = ref(null)
const loading = ref(false)
const activeOnly = ref(false)
const statusMessage = ref('')
const errorMessage = ref('')
const selectedProcessId = ref(null)
const processDefinitions = ref([])
const keyword = ref('')
const hasUnsavedChanges = ref(false)
const lastSavedAt = ref('')
const designerMode = ref(DESIGNER_TYPE_BPMN)
const formCatalogs = ref([])

const processForm = reactive({
  processKey: '',
  processName: ''
})

const designerModeLabel = computed(() => designerMode.value === DESIGNER_TYPE_CUSTOM ? 'Flowlong 自定义编排' : 'BPMN 设计器')

function toTimestamp(value) {
  if (!value) {
    return 0
  }
  const timestamp = new Date(value).getTime()
  return Number.isNaN(timestamp) ? 0 : timestamp
}

const filteredDefinitions = computed(() => {
  const needle = keyword.value.trim().toLowerCase()
  return processDefinitions.value.filter((item) => {
    if (activeOnly.value && item.status !== 'ACTIVE') {
      return false
    }
    if (!needle) {
      return true
    }
    return [item.processName, item.processKey, `v${item.version}`, item.designerType]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(needle))
  })
})
const activeDefinitionCount = computed(() => processDefinitions.value.filter((item) => item.status === 'ACTIVE').length)
const selectedDefinition = computed(() => processDefinitions.value.find((item) => item.id === selectedProcessId.value) || null)
const recentDefinitions = computed(() =>
  [...processDefinitions.value]
    .sort((left, right) => {
      const timeDiff = toTimestamp(right.updatedAt || right.createdAt) - toTimestamp(left.updatedAt || left.createdAt)
      if (timeDiff !== 0) {
        return timeDiff
      }
      return (right.version || 0) - (left.version || 0)
    })
    .slice(0, 4)
)

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

function normalizeProcessKey(sourceValue) {
  const source = sourceValue || processForm.processKey || processForm.processName || 'process'
  return source
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9_-]+/g, '_')
    .replace(/^[-_]+|[-_]+$/g, '') || 'process'
}

function buildDiagramMeta() {
  return {
    processId: normalizeProcessKey(),
    processName: processForm.processName || '新建流程'
  }
}

function confirmDiscardChanges(actionLabel) {
  if (!hasUnsavedChanges.value) {
    return true
  }
  return window.confirm(`当前画布有未保存改动，确认继续${actionLabel}吗？`)
}

function handleBeforeUnload(event) {
  if (!hasUnsavedChanges.value) {
    return
  }
  event.preventDefault()
  event.returnValue = ''
}

function buildLeaveProcessTemplate() {
  return `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
  xmlns:flowable="http://flowable.org/bpmn"
  id="Definitions_leave_process"
  targetNamespace="http://flowable.org/processdef">
  <bpmn:process id="leave-process" name="请假审批流程" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="提交申请" flowable:formKey="leave-form">
      <bpmn:documentation>请假发起页 / 请假申请单</bpmn:documentation>
    </bpmn:startEvent>
    <bpmn:userTask id="Task_DeptApprove" name="部门经理审批" flowable:assignee="\${deptManager}" flowable:formKey="manager-approval" />
    <bpmn:exclusiveGateway id="Gateway_DeptDecision" name="部门经理是否同意" />
    <bpmn:userTask id="Task_GeneralApprove" name="总经理审批" flowable:assignee="\${generalManager}" flowable:formKey="general-approval" />
    <bpmn:exclusiveGateway id="Gateway_GeneralDecision" name="总经理是否同意" />
    <bpmn:endEvent id="EndEvent_Approved" name="审批通过" />
    <bpmn:endEvent id="EndEvent_Rejected" name="审批驳回" />
    <bpmn:sequenceFlow id="Flow_Start_To_Dept" sourceRef="StartEvent_1" targetRef="Task_DeptApprove" />
    <bpmn:sequenceFlow id="Flow_Dept_To_Gateway" sourceRef="Task_DeptApprove" targetRef="Gateway_DeptDecision" />
    <bpmn:sequenceFlow id="Flow_Dept_Approved" name="同意" sourceRef="Gateway_DeptDecision" targetRef="Task_GeneralApprove">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression"><![CDATA[\${approved}]]></bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_Dept_Rejected" name="驳回" sourceRef="Gateway_DeptDecision" targetRef="EndEvent_Rejected" />
    <bpmn:sequenceFlow id="Flow_General_To_Gateway" sourceRef="Task_GeneralApprove" targetRef="Gateway_GeneralDecision" />
    <bpmn:sequenceFlow id="Flow_General_Approved" name="同意" sourceRef="Gateway_GeneralDecision" targetRef="EndEvent_Approved">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression"><![CDATA[\${approved}]]></bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_General_Rejected" name="驳回" sourceRef="Gateway_GeneralDecision" targetRef="EndEvent_Rejected" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_leave_process">
    <bpmndi:BPMNPlane id="BPMNPlane_leave_process" bpmnElement="leave-process">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="120" y="182" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Task_DeptApprove_di" bpmnElement="Task_DeptApprove">
        <dc:Bounds x="220" y="160" width="120" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Gateway_DeptDecision_di" bpmnElement="Gateway_DeptDecision" isMarkerVisible="true">
        <dc:Bounds x="395" y="175" width="50" height="50" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Task_GeneralApprove_di" bpmnElement="Task_GeneralApprove">
        <dc:Bounds x="510" y="160" width="120" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Gateway_GeneralDecision_di" bpmnElement="Gateway_GeneralDecision" isMarkerVisible="true">
        <dc:Bounds x="685" y="175" width="50" height="50" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="EndEvent_Approved_di" bpmnElement="EndEvent_Approved">
        <dc:Bounds x="800" y="182" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="EndEvent_Rejected_di" bpmnElement="EndEvent_Rejected">
        <dc:Bounds x="555" y="320" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_Start_To_Dept_di" bpmnElement="Flow_Start_To_Dept">
        <di:waypoint x="156" y="200" />
        <di:waypoint x="220" y="200" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_Dept_To_Gateway_di" bpmnElement="Flow_Dept_To_Gateway">
        <di:waypoint x="340" y="200" />
        <di:waypoint x="395" y="200" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_Dept_Approved_di" bpmnElement="Flow_Dept_Approved">
        <di:waypoint x="445" y="200" />
        <di:waypoint x="510" y="200" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_Dept_Rejected_di" bpmnElement="Flow_Dept_Rejected">
        <di:waypoint x="420" y="225" />
        <di:waypoint x="420" y="338" />
        <di:waypoint x="555" y="338" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_General_To_Gateway_di" bpmnElement="Flow_General_To_Gateway">
        <di:waypoint x="630" y="200" />
        <di:waypoint x="685" y="200" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_General_Approved_di" bpmnElement="Flow_General_Approved">
        <di:waypoint x="735" y="200" />
        <di:waypoint x="800" y="200" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_General_Rejected_di" bpmnElement="Flow_General_Rejected">
        <di:waypoint x="710" y="225" />
        <di:waypoint x="710" y="338" />
        <di:waypoint x="591" y="338" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`
}

function syncModeFromDefinition(definition) {
  designerMode.value = definition?.designerType === DESIGNER_TYPE_CUSTOM ? DESIGNER_TYPE_CUSTOM : DESIGNER_TYPE_BPMN
}

async function ensureCustomDesignerSeeded() {
  if (!customDesignerRef.value) {
    return
  }
  await customDesignerRef.value.createNewDiagram(buildDiagramMeta())
  customDesignerRef.value.markClean()
}

async function fetchProcessDefinitions() {
  const { data } = await http.get('/process-definition/all')
  processDefinitions.value = data || []
}

async function fetchFormCatalogs() {
  const { data } = await http.get('/form-catalog/active')
  formCatalogs.value = data || []
}

async function switchDesignerMode(nextMode) {
  if (nextMode === designerMode.value) {
    return
  }
  resetMessages()
  if (!confirmDiscardChanges(`切换到${nextMode === DESIGNER_TYPE_CUSTOM ? '新设计流程' : '老流程 BPMN'}模式`)) {
    return
  }
  designerMode.value = nextMode
  await nextTick()
  if (nextMode === DESIGNER_TYPE_CUSTOM) {
    if (selectedDefinition.value?.designerType === DESIGNER_TYPE_CUSTOM && selectedDefinition.value?.designSchemaJson) {
      await customDesignerRef.value?.importSchema(selectedDefinition.value.designSchemaJson, false, buildDiagramMeta())
      customDesignerRef.value?.markClean()
      hasUnsavedChanges.value = false
    } else {
      await ensureCustomDesignerSeeded()
      hasUnsavedChanges.value = true
      statusMessage.value = '已切换到 Flowlong 设计器，当前流程尚未生成自定义编排版本'
    }
  } else if (selectedDefinition.value?.designerType === DESIGNER_TYPE_CUSTOM) {
    const xml = await customDesignerRef.value?.getXml(buildDiagramMeta())
    if (xml) {
      await bpmnDesignerRef.value?.importXml(xml)
      bpmnDesignerRef.value?.markClean()
    }
    hasUnsavedChanges.value = false
  } else {
    hasUnsavedChanges.value = false
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
  lastSavedAt.value = ''
  if (bpmnDesignerRef.value) {
    await bpmnDesignerRef.value.createNewDiagram({ processId: 'process', processName: '新建流程' })
    bpmnDesignerRef.value.markClean()
  }
  if (customDesignerRef.value) {
    await customDesignerRef.value.createNewDiagram({ processId: 'process', processName: '新建流程' })
    customDesignerRef.value.markClean()
  }
  hasUnsavedChanges.value = false
  statusMessage.value = '已创建空白画布'
  if (route.query.definitionId) {
    router.replace({ query: { ...route.query, definitionId: undefined } })
  }
}

async function prepareLeaveProcessTemplate() {
  if (!confirmDiscardChanges('套用请假模板')) {
    return
  }
  resetMessages()
  selectedProcessId.value = null
  lastSavedAt.value = ''
  processForm.processKey = 'leave-process'
  processForm.processName = '请假审批流程'
  if (designerMode.value === DESIGNER_TYPE_CUSTOM) {
    await customDesignerRef.value?.importSchema(createLeaveCustomSchema(), true, buildDiagramMeta())
    statusMessage.value = '已切换到 Flowlong 请假流程模板，请继续完善审批人和条件表达式'
    hasUnsavedChanges.value = true
    return
  }
  await bpmnDesignerRef.value?.importXml(buildLeaveProcessTemplate(), true)
  hasUnsavedChanges.value = true
  statusMessage.value = '已切换到 BPMN 请假流程模板，请继续完善 BPMN 节点与审批流'
}

async function loadDefinition(id) {
  if (id === selectedProcessId.value) {
    return
  }
  if (!confirmDiscardChanges('切换流程定义')) {
    return
  }
  resetMessages()
  loading.value = true
  try {
    const { data } = await http.get(`/process-definition/${id}`)
    selectedProcessId.value = data.id
    processForm.processKey = data.processKey
    processForm.processName = data.processName
    syncModeFromDefinition(data)
    await nextTick()
    if (data.bpmnXml && bpmnDesignerRef.value) {
      await bpmnDesignerRef.value.importXml(data.bpmnXml)
      bpmnDesignerRef.value.markClean()
    }
    if (designerMode.value === DESIGNER_TYPE_CUSTOM) {
      await customDesignerRef.value?.importSchema(data.designSchemaJson, false, buildDiagramMeta())
      customDesignerRef.value?.markClean()
    }
    hasUnsavedChanges.value = false
    lastSavedAt.value = data.updatedAt || data.createdAt || ''
    statusMessage.value = `已加载 ${data.processName}`
    if (route.query.definitionId !== String(id)) {
      router.replace({ query: { ...route.query, definitionId: String(id) } })
    }
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

async function buildSavePayload() {
  const currentUser = getCurrentUser()
  const payload = {
    processKey: normalizeProcessKey(),
    processName: processForm.processName.trim(),
    userId: currentUser?.id ?? 1,
    designerType: designerMode.value,
    designSchemaJson: ''
  }
  if (designerMode.value === DESIGNER_TYPE_CUSTOM) {
    const schema = await customDesignerRef.value?.getSchema()
    payload.designSchemaJson = JSON.stringify(schema)
    payload.bpmnXml = await customDesignerRef.value?.getXml(buildDiagramMeta())
  } else {
    payload.bpmnXml = await bpmnDesignerRef.value?.getXml()
  }
  return payload
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
    const wasEditing = Boolean(selectedProcessId.value)
    const payload = await buildSavePayload()
    const request = selectedProcessId.value
      ? http.put(`/process-definition/update/${selectedProcessId.value}`, payload)
      : http.post('/process-definition/save', payload)
    const { data } = await request
    selectedProcessId.value = data.id
    processForm.processKey = data.processKey
    processForm.processName = data.processName
    bpmnDesignerRef.value?.markClean()
    customDesignerRef.value?.markClean()
    hasUnsavedChanges.value = false
    lastSavedAt.value = data.updatedAt || data.createdAt || new Date().toISOString()
    await fetchProcessDefinitions()
    statusMessage.value = wasEditing ? '流程已保存' : '流程已创建'
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
    await http.put(`/process-definition/activate/${selectedProcessId.value}`, { userId: currentUser?.id ?? 1 })
    await fetchProcessDefinitions()
    hasUnsavedChanges.value = false
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
  if (designerMode.value === DESIGNER_TYPE_CUSTOM) {
    await customDesignerRef.value?.downloadXml(fileName)
    return
  }
  await bpmnDesignerRef.value?.downloadXml(fileName)
}

async function downloadCustomSchema() {
  await customDesignerRef.value?.downloadSchema(`${normalizeProcessKey() || 'process'}-design.json`)
}

async function importLocalXml(event) {
  const [file] = event.target.files || []
  if (!file) {
    return
  }
  resetMessages()
  try {
    const xml = await file.text()
    const derivedName = file.name.replace(/\.(bpmn|xml)$/i, '')
    if (!selectedProcessId.value) {
      processForm.processName = processForm.processName || derivedName
      processForm.processKey = processForm.processKey || normalizeProcessKey(derivedName)
    }
    await bpmnDesignerRef.value?.importXml(xml, true)
    hasUnsavedChanges.value = true
    statusMessage.value = `已导入 ${file.name}`
  } catch (error) {
    errorMessage.value = error.message || '导入 XML 失败'
  } finally {
    event.target.value = ''
  }
}

async function importLocalSchema(event) {
  const [file] = event.target.files || []
  if (!file) {
    return
  }
  resetMessages()
  try {
    const json = await file.text()
    const derivedName = file.name.replace(/\.json$/i, '')
    if (!selectedProcessId.value) {
      processForm.processName = processForm.processName || derivedName
      processForm.processKey = processForm.processKey || normalizeProcessKey(derivedName)
    }
    await customDesignerRef.value?.importSchema(json, true, buildDiagramMeta())
    hasUnsavedChanges.value = true
    statusMessage.value = `已导入 ${file.name}`
  } catch (error) {
    errorMessage.value = error.message || '导入设计 JSON 失败'
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
  await Promise.all([fetchProcessDefinitions(), fetchFormCatalogs()])
  await nextTick()
  if (customDesignerRef.value) {
    await customDesignerRef.value.createNewDiagram(buildDiagramMeta())
    customDesignerRef.value.markClean()
  }
  const queryDefinitionId = Number(route.query.definitionId)
  if (Number.isFinite(queryDefinitionId) && queryDefinitionId > 0) {
    await loadDefinition(queryDefinitionId)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

onBeforeRouteLeave(() => {
  if (!hasUnsavedChanges.value) {
    return true
  }
  return window.confirm('当前流程设计页还有未保存改动，确认离开吗？')
})
</script>

<style scoped>
.studio-console-card {
  margin-bottom: 20px;
}

.studio-summary-grid {
  grid-template-columns: minmax(320px, 1.2fr) minmax(260px, 0.8fr) minmax(260px, 0.8fr);
  align-items: start;
  margin-bottom: 20px;
}

.summary-card {
  height: 100%;
}

.definition-panel-head {
  align-items: flex-start;
}

.definition-filter-bar {
  display: grid;
  gap: 14px;
  margin-top: 16px;
}

.definition-toggle {
  justify-content: space-between;
  align-items: center;
}

.definition-toggle-copy {
  display: grid;
  gap: 4px;
}

.definition-toggle-copy strong,
.definition-toggle-copy small {
  margin: 0;
}

.definition-list {
  margin-top: 18px;
  max-height: 420px;
  overflow: auto;
}

.definition-key {
  margin: 6px 0 0;
}

.definition-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
}

.definition-tags {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.meta-pill-tight {
  padding: 4px 10px;
}

.studio-detail-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.recent-definition-side {
  display: flex;
  align-items: center;
  gap: 8px;
}

.studio-mode-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 20px;
  padding: 6px;
  border-radius: 22px;
  background: rgba(27, 53, 89, 0.06);
  border: 1px solid rgba(71, 94, 126, 0.08);
}

.studio-form-grid {
  margin-bottom: 16px;
}

.studio-action-row {
  margin-top: 18px;
}

.designer-stage {
  min-height: 720px;
}

.designer-stage :deep(.designer-card) {
  min-height: 720px;
}

.designer-loading-card {
  min-height: 680px;
  border: 1px solid rgba(75, 98, 127, 0.1);
  border-radius: 28px;
  background:
    radial-gradient(circle at top left, rgba(45, 86, 138, 0.12), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(243, 247, 252, 0.92));
  box-shadow: 0 24px 60px rgba(20, 35, 57, 0.08);
  backdrop-filter: blur(18px);
  padding: 30px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 12px;
}

.designer-loading-card h3,
.designer-loading-card p {
  margin: 0;
}

@media (max-width: 1180px) {
  .studio-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
