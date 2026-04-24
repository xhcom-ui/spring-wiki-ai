<template>
  <div class="page-grid">
    <section class="studio-content">
      <section class="panel-card studio-console-card">
          <div class="panel-head">
            <div>
              <p class="eyebrow">Design Console</p>
              <h3>流程设计与发布</h3>
            </div>
            <div class="management-meta">
              <span class="meta-pill">流程定义 {{ processDefinitions.length }}</span>
              <span class="meta-pill">激活版本 {{ activeDefinitionCount }}</span>
              <span class="meta-pill">运行时 {{ runtimeVersionLabel }}</span>
              <span class="meta-pill">当前模式 {{ designerModeLabel }}</span>
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
              FlowLong Designer
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
              ? 'FlowLong Designer 使用可视化节点编排保存设计 Schema，并自动生成兼容部署的 BPMN XML。'
              : 'BPMN 设计器继续直接编辑 XML，适合兼容历史版本和外部导入流程。' }}
          </p>

          <div class="action-row studio-action-row">
            <button type="button" class="primary-btn" @click="saveProcess" :disabled="loading">
              {{ loading ? '处理中...' : selectedProcessId ? '保存更新' : '保存新版本' }}
            </button>
            <button type="button" class="primary-btn" @click="publishProcess" :disabled="loading">
              发布流程
            </button>
            <button type="button" class="secondary-btn" @click="prepareLeaveProcessTemplate" :disabled="loading">
              请假流程模板
            </button>
            <button type="button" class="secondary-btn" @click="activateProcess" :disabled="loading || !selectedProcessId">
              激活版本
            </button>
            <button type="button" class="secondary-btn" @click="deployCurrentProcess" :disabled="loading || !selectedProcessId">
              部署当前版本
            </button>
            <button type="button" class="secondary-btn" @click="deployActiveProcess" :disabled="loading || !currentProcessKey">
              部署激活版本
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

          <div class="definition-filter-bar">
            <label class="toggle-row definition-toggle">
              <span class="definition-toggle-copy">
                <strong>只看激活版本</strong>
                <small>和后端 `ACTIVE` 状态保持一致</small>
              </span>
              <span class="definition-toggle-switch">
                <input v-model="activeOnly" type="checkbox" />
                <span class="definition-toggle-slider"></span>
              </span>
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
                  {{ formatDefinitionStatus(definition.status) }}
                </span>
              </div>
              <p class="definition-key">{{ definition.processKey }}</p>
              <div class="definition-meta-row">
                <small>版本 v{{ definition.version }}</small>
                <small>{{ formatDate(definition.updatedAt || definition.createdAt) }}</small>
              </div>
              <div class="definition-tags">
                <span class="meta-pill meta-pill-tight">
                  {{ formatDesignerType(definition.designerType) }}
                </span>
              </div>
            </button>
            <div v-if="!filteredDefinitions.length" class="empty-state">还没有流程定义，先新建一个。</div>
          </div>
        </aside>

        <article class="panel-card runtime-panel summary-card">
            <div class="panel-head">
              <div>
                <p class="eyebrow">Runtime Sync</p>
                <h3>FlowLong 引擎部署状态</h3>
              </div>
              <button type="button" class="ghost-btn" @click="refreshRuntimeStatus" :disabled="loading || !currentProcessKey">
                刷新状态
              </button>
            </div>
            <div class="stack-list runtime-status-list">
              <div class="list-item">
                <div>
                  <strong>流程 Key</strong>
                  <p>{{ runtimeStatus.processKey || currentProcessKey || '-' }}</p>
                </div>
                <span :class="['status-pill', runtimeStatus.deployed ? 'success-pill' : 'muted-pill']">
                  {{ runtimeStatus.deployed ? '已部署' : '未部署' }}
                </span>
              </div>
              <div class="list-item">
                <div>
                  <strong>激活定义版本</strong>
                  <p>定义中心当前激活版本</p>
                </div>
                <span class="status-pill">{{ runtimeStatus.activeDefinitionVersion || '-' }}</span>
              </div>
              <div class="list-item">
                <div>
                  <strong>引擎版本</strong>
                  <p>FlowLong 当前运行时版本</p>
                </div>
                <span class="status-pill">{{ runtimeStatus.version || runtimeStatus.deployedFlowLongVersion || '-' }}</span>
              </div>
              <div class="list-item">
                <div>
                  <strong>同步结果</strong>
                  <p>激活定义与引擎部署版本是否一致</p>
                </div>
                <span :class="['status-pill', runtimeStatus.definitionSynchronized ? 'success-pill' : 'warning-pill']">
                  {{ runtimeStatus.definitionSynchronized ? '已同步' : runtimeStatus.deployed ? '待同步' : '未同步' }}
                </span>
              </div>
            </div>
        </article>

        <article class="panel-card history-panel summary-card">
            <div class="panel-head">
              <div>
                <p class="eyebrow">Deployment History</p>
                <h3>最近部署记录</h3>
              </div>
            </div>
            <div class="history-table-wrap">
              <table class="simple-table">
                <thead>
                  <tr>
                    <th>时间</th>
                    <th>来源版本</th>
                    <th>运行时版本</th>
                    <th>部署人</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in deploymentHistory.slice(0, 5)" :key="item.id">
                    <td>{{ formatDate(item.deployedAt) }}</td>
                    <td>{{ item.sourceDefinitionVersion || '-' }}</td>
                    <td>{{ item.flowLongProcessVersion || '-' }}</td>
                    <td>{{ item.deployedByName || '-' }}</td>
                    <td>
                      <div class="inline-actions">
                        <button type="button" class="secondary-btn mini-btn" @click="openDeploymentDetail(item)">详情</button>
                        <button
                          type="button"
                          class="primary-btn mini-btn"
                          :disabled="loading || rollbacking"
                          @click="rollbackDeployment(item)"
                        >
                          回滚
                        </button>
                      </div>
                    </td>
                  </tr>
                  <tr v-if="!deploymentHistory.length">
                    <td colspan="5" class="empty-row">当前流程还没有部署记录</td>
                  </tr>
                </tbody>
              </table>
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
              :form-catalogs="availableFormCatalogs"
              @changed="handleDesignerChange"
              @error="handleDesignerError"
            />
          </template>
          <template #fallback>
            <section v-if="designerMode === DESIGNER_TYPE_BPMN" class="designer-loading-card">
              <p class="eyebrow">Designer Engine</p>
              <h3>流程设计器加载中</h3>
              <p class="muted-text">BPMN 画布和 FlowLong 设计引擎会在进入当前页面后按需加载。</p>
            </section>
          </template>
        </Suspense>

        <SeaFlowDesigner
          v-show="designerMode === DESIGNER_TYPE_CUSTOM"
          ref="customDesignerRef"
          class="designer-card"
          :form-catalogs="availableFormCatalogs"
          @changed="handleDesignerChange"
          @error="handleDesignerError"
        />
      </section>
    </section>

    <div v-if="deploymentDetailVisible" class="modal-mask" @click.self="closeDeploymentDetail">
      <div class="modal-panel detail-modal">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Deployment Record</p>
            <h3>部署记录详情</h3>
          </div>
          <div class="inline-actions">
            <button
              v-if="canCompareSourceAndActive"
              type="button"
              class="secondary-btn"
              :disabled="deploymentCompareLoading"
              @click="compareDeploymentVersions"
            >
              {{ deploymentCompareLoading ? '对比中...' : '对比当前激活版本' }}
            </button>
            <button
              v-if="deploymentDetail?.canRollback"
              type="button"
              class="primary-btn"
              :disabled="loading || rollbacking"
              @click="rollbackDeployment(deploymentDetail)"
            >
              {{ rollbacking ? '回滚中...' : '一键回滚到该版本' }}
            </button>
            <button type="button" class="ghost-btn" @click="closeDeploymentDetail">关闭</button>
          </div>
        </div>

        <p v-if="deploymentDetailError" class="feedback error">{{ deploymentDetailError }}</p>
        <p v-else-if="deploymentDetailLoading" class="muted-text">部署详情加载中...</p>

        <template v-else-if="deploymentDetail">
          <section class="detail-grid compact-detail-grid">
            <article class="detail-item">
              <span class="detail-key">流程 Key</span>
              <strong class="detail-value">{{ deploymentDetail.processKey || '-' }}</strong>
            </article>
            <article class="detail-item">
              <span class="detail-key">来源版本</span>
              <strong class="detail-value">v{{ deploymentDetail.sourceDefinitionVersion || '-' }}</strong>
            </article>
            <article class="detail-item">
              <span class="detail-key">运行时版本</span>
              <strong class="detail-value">{{ deploymentDetail.flowLongProcessVersion || '-' }}</strong>
            </article>
            <article class="detail-item">
              <span class="detail-key">当前状态</span>
              <strong class="detail-value">{{ deploymentDetail.isCurrentDeployment ? '当前部署' : '历史部署' }}</strong>
            </article>
          </section>

          <section class="content-grid two-column">
            <article class="panel-card">
              <div class="panel-head">
                <div>
                  <p class="eyebrow">Source Definition</p>
                  <h3>来源流程定义</h3>
                </div>
              </div>
              <div class="detail-grid">
                <article class="detail-item">
                  <span class="detail-key">定义名称</span>
                  <strong class="detail-value">{{ deploymentDetail.sourceDefinition?.processName || deploymentDetail.processName || '-' }}</strong>
                </article>
                <article class="detail-item">
                  <span class="detail-key">定义 ID</span>
                  <strong class="detail-value">{{ deploymentDetail.sourceDefinition?.id || deploymentDetail.sourceDefinitionId || '-' }}</strong>
                </article>
                <article class="detail-item">
                  <span class="detail-key">设计器类型</span>
                  <strong class="detail-value">{{ deploymentDetail.sourceDefinition?.designerType || '-' }}</strong>
                </article>
                <article class="detail-item">
                  <span class="detail-key">更新时间</span>
                  <strong class="detail-value">{{ formatDate(deploymentDetail.sourceDefinition?.updatedAt) }}</strong>
                </article>
              </div>
            </article>

            <article class="panel-card">
              <div class="panel-head">
                <div>
                  <p class="eyebrow">Deployment Snapshot</p>
                  <h3>发布快照</h3>
                </div>
              </div>
              <div class="detail-grid">
                <article class="detail-item">
                  <span class="detail-key">部署人</span>
                  <strong class="detail-value">{{ deploymentDetail.deployedByName || '-' }}</strong>
                </article>
                <article class="detail-item">
                  <span class="detail-key">部署时间</span>
                  <strong class="detail-value">{{ formatDate(deploymentDetail.deployedAt) }}</strong>
                </article>
                <article class="detail-item">
                  <span class="detail-key">来源类型</span>
                  <strong class="detail-value">{{ deploymentDetail.sourceType || '-' }}</strong>
                </article>
                <article class="detail-item">
                  <span class="detail-key">激活版本</span>
                  <strong class="detail-value">v{{ deploymentDetail.activeDefinition?.version || '-' }}</strong>
                </article>
              </div>
            </article>
          </section>

          <article class="panel-card">
            <div class="panel-head">
              <div>
                <p class="eyebrow">BPMN Preview</p>
                <h3>来源版本 BPMN 预览</h3>
              </div>
            </div>
            <BpmnTraceViewer :xml="deploymentDetail.sourceDefinition?.bpmnXml || ''" />
          </article>

          <article class="panel-card">
            <div class="panel-head">
              <div>
                <p class="eyebrow">Version Compare</p>
                <h3>当前激活版本 vs 该部署来源版本</h3>
              </div>
              <span v-if="deploymentCompareResult" class="status-pill">
                变化 {{ deploymentCompareResult.changedLineCount }}
              </span>
            </div>

            <p v-if="deploymentCompareError" class="feedback error">{{ deploymentCompareError }}</p>
            <p v-else-if="deploymentCompareLoading" class="muted-text">版本对比加载中...</p>
            <div v-else-if="!canCompareSourceAndActive" class="empty-state">
              该部署来源版本就是当前激活版本，暂不需要额外对比。
            </div>
            <template v-else-if="deploymentCompareResult">
              <section class="detail-grid compact-detail-grid">
                <article class="detail-item">
                  <span class="detail-key">来源版本</span>
                  <strong class="detail-value">v{{ deploymentCompareResult.left.version }}</strong>
                </article>
                <article class="detail-item">
                  <span class="detail-key">激活版本</span>
                  <strong class="detail-value">v{{ deploymentCompareResult.right.version }}</strong>
                </article>
                <article class="detail-item">
                  <span class="detail-key">设计器一致</span>
                  <strong class="detail-value">{{ deploymentCompareResult.sameDesignerType ? '是' : '否' }}</strong>
                </article>
                <article class="detail-item">
                  <span class="detail-key">变化行数</span>
                  <strong class="detail-value">{{ deploymentCompareResult.changedLineCount }}</strong>
                </article>
              </section>

              <table class="simple-table">
                <thead>
                  <tr>
                    <th>行号</th>
                    <th>来源版本</th>
                    <th>当前激活版本</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in deploymentCompareResult.diffPreview || []" :key="row.line">
                    <td>{{ row.line }}</td>
                    <td>{{ row.left || '-' }}</td>
                    <td>{{ row.right || '-' }}</td>
                  </tr>
                  <tr v-if="!(deploymentCompareResult.diffPreview || []).length">
                    <td colspan="3" class="empty-row">当前两版没有可展示的差异预览</td>
                  </tr>
                </tbody>
              </table>
            </template>
          </article>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Suspense, computed, defineAsyncComponent, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute } from 'vue-router'
import BpmnTraceViewer from '../components/BpmnTraceViewer.vue'
import SeaFlowDesigner from '../components/seaflow/SeaFlowDesigner.vue'
import { http } from '../api/http'
import { ensureRemoteFormRegistryLoaded, getAvailableFormCatalogs } from '../utils/businessForms'

const ProcessDesigner = defineAsyncComponent(() => import('../components/ProcessDesigner.vue'))
const route = useRoute()

const DESIGNER_TYPE_BPMN = 'BPMN'
const DESIGNER_TYPE_CUSTOM = 'CUSTOM'

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
const runtimeStatus = ref({})
const deploymentHistory = ref([])
const deploymentDetailVisible = ref(false)
const deploymentDetailLoading = ref(false)
const deploymentDetailError = ref('')
const deploymentDetail = ref(null)
const rollbacking = ref(false)
const deploymentCompareLoading = ref(false)
const deploymentCompareError = ref('')
const deploymentCompareResult = ref(null)
const designerMode = ref(DESIGNER_TYPE_BPMN)
const hasUnsavedChanges = ref(false)

const processForm = reactive({
  processKey: '',
  processName: ''
})

const selectedDefinition = computed(() => processDefinitions.value.find((item) => item.id === selectedProcessId.value) || null)
const currentProcessKey = computed(() => processForm.processKey || normalizeProcessKey())
const activeDefinitionCount = computed(() => processDefinitions.value.filter((item) => item.status === 'ACTIVE').length)
const runtimeVersionLabel = computed(() => runtimeStatus.value.version || runtimeStatus.value.deployedFlowLongVersion || '未部署')
const designerModeLabel = computed(() => designerMode.value === DESIGNER_TYPE_CUSTOM ? 'FlowLong Designer' : 'BPMN 设计器')
const availableFormCatalogs = computed(() => getAvailableFormCatalogs())
const canCompareSourceAndActive = computed(() => {
  const sourceId = deploymentDetail.value?.sourceDefinition?.id
  const activeId = deploymentDetail.value?.activeDefinition?.id
  return Boolean(sourceId && activeId && sourceId !== activeId)
})

const filteredDefinitions = computed(() => {
  return processDefinitions.value.filter((item) => {
    if (activeOnly.value && item.status !== 'ACTIVE') {
      return false
    }
    return true
  })
})

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

function formatDefinitionStatus(status) {
  return status === 'ACTIVE' ? '已激活' : status === 'INACTIVE' ? '未激活' : status || '-'
}

function formatDesignerType(designerType) {
  return designerType === DESIGNER_TYPE_CUSTOM ? 'FlowLong Designer' : 'BPMN 设计器'
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

function extractFormKeysFromCustomSchema(node, bucket = new Set()) {
  if (!node || typeof node !== 'object') {
    return bucket
  }
  if (node.nodeConfig) {
    return extractFormKeysFromCustomSchema(node.nodeConfig, bucket)
  }
  const formKey = node.properties?.formKey || node.formKey
  if (typeof formKey === 'string' && formKey.trim()) {
    bucket.add(formKey.trim())
  }
  if (node.childNode) {
    extractFormKeysFromCustomSchema(node.childNode, bucket)
  }
  if (Array.isArray(node.conditionNodes)) {
    node.conditionNodes.forEach((item) => extractFormKeysFromCustomSchema(item, bucket))
  }
  return bucket
}

function extractFormKeysFromBpmnXml(xml = '') {
  const bucket = new Set()
  const pattern = /\b(?:flowable|activiti):formKey="([^"]+)"/g
  let matched = pattern.exec(xml)
  while (matched) {
    if (matched[1]?.trim()) {
      bucket.add(matched[1].trim())
    }
    matched = pattern.exec(xml)
  }
  return bucket
}

function validateFlowLongSequenceCompatibility(xml = '') {
  if (!xml.trim()) {
    return
  }
  const document = new DOMParser().parseFromString(xml, 'application/xml')
  if (document.querySelector('parsererror')) {
    throw new Error('流程 XML 解析失败，无法校验发布兼容性')
  }

  const outgoingMap = new Map()
  const sequenceFlows = document.getElementsByTagNameNS('*', 'sequenceFlow')
  for (const flow of sequenceFlows) {
    const sourceRef = flow.getAttribute('sourceRef')
    const targetRef = flow.getAttribute('targetRef')
    if (!sourceRef || !targetRef) {
      continue
    }
    const targets = outgoingMap.get(sourceRef) || []
    targets.push(targetRef)
    outgoingMap.set(sourceRef, targets)
  }

  for (const [sourceRef, targets] of outgoingMap.entries()) {
    if (targets.length > 1) {
      throw new Error(`当前后端仅支持单出口顺序流，节点 ${sourceRef} 存在多条出口。请移除条件/并行分支后再发布。`)
    }
  }
}

function validateReferencedFormKeys(formKeys) {
  const availableKeys = new Set(getAvailableFormCatalogs().map((item) => item.key))
  availableKeys.add('default')
  const missingKeys = [...formKeys].filter((item) => !availableKeys.has(item))
  if (missingKeys.length) {
    throw new Error(`以下 formKey 未在表单目录中维护：${missingKeys.join('、')}`)
  }
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
    <bpmn:userTask id="Task_DeptApprove" name="部门经理审批" flowable:assignee="\${deptManager}" flowable:formKey="manager-approval">
      <bpmn:documentation>待办审批页 / 部门经理审批单</bpmn:documentation>
    </bpmn:userTask>
    <bpmn:exclusiveGateway id="Gateway_DeptDecision" name="部门经理是否同意" />
    <bpmn:userTask id="Task_GeneralApprove" name="总经理审批" flowable:assignee="\${generalManager}" flowable:formKey="general-approval">
      <bpmn:documentation>待办审批页 / 总经理审批单</bpmn:documentation>
    </bpmn:userTask>
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

function getActiveDesigner() {
  return designerMode.value === DESIGNER_TYPE_CUSTOM ? customDesignerRef.value : bpmnDesignerRef.value
}

function confirmDiscardChanges(actionLabel) {
  if (!hasUnsavedChanges.value) {
    return true
  }
  return window.confirm(`当前画布有未保存改动，确认继续${actionLabel}吗？`)
}

function syncModeFromDefinition(definition) {
  designerMode.value = definition?.designerType === DESIGNER_TYPE_CUSTOM ? DESIGNER_TYPE_CUSTOM : DESIGNER_TYPE_BPMN
}

async function initDesigners(metadata = { processId: 'process', processName: '新建流程' }) {
  await nextTick()
  if (bpmnDesignerRef.value) {
    await bpmnDesignerRef.value.createNewDiagram(metadata)
    bpmnDesignerRef.value.markClean()
  }
  if (customDesignerRef.value) {
    await customDesignerRef.value.createNewDiagram(metadata)
    customDesignerRef.value.markClean()
  }
}

async function fetchProcessDefinitions() {
  const { data } = await http.get('/process-definition/all')
  processDefinitions.value = data || []
}

async function fetchRuntimeStatus(processKey = currentProcessKey.value) {
  if (!processKey) {
    runtimeStatus.value = {}
    return
  }
  const { data } = await http.get(`/flowlong/process/${processKey}/status`)
  runtimeStatus.value = data || {}
}

async function fetchDeploymentHistory(processKey = currentProcessKey.value) {
  if (!processKey) {
    deploymentHistory.value = []
    return
  }
  try {
    const { data } = await http.get(`/flowlong/deployments/${processKey}`)
    deploymentHistory.value = data || []
  } catch (error) {
    deploymentHistory.value = []
  }
}

async function refreshRuntimeStatus() {
  await Promise.all([fetchRuntimeStatus(), fetchDeploymentHistory()])
}

async function switchDesignerMode(nextMode) {
  if (nextMode === designerMode.value) {
    return
  }
  resetMessages()
  if (!confirmDiscardChanges(`切换到${nextMode === DESIGNER_TYPE_CUSTOM ? 'FlowLong Designer' : 'BPMN 设计器'}模式`)) {
    return
  }

  if (nextMode === DESIGNER_TYPE_BPMN && customDesignerRef.value) {
    const xml = await customDesignerRef.value.getXml(buildDiagramMeta())
    designerMode.value = nextMode
    await nextTick()
    await bpmnDesignerRef.value?.importXml(xml)
    bpmnDesignerRef.value?.markClean()
    hasUnsavedChanges.value = false
    return
  }

  designerMode.value = nextMode
  await nextTick()
  if (selectedDefinition.value?.designerType === DESIGNER_TYPE_CUSTOM && selectedDefinition.value?.designSchemaJson) {
    await customDesignerRef.value?.importSchema(selectedDefinition.value.designSchemaJson, false, buildDiagramMeta())
    customDesignerRef.value?.markClean()
    hasUnsavedChanges.value = false
    return
  }
  await customDesignerRef.value?.createNewDiagram(buildDiagramMeta())
  hasUnsavedChanges.value = true
  statusMessage.value = '已切换到 FlowLong Designer，当前流程还没有对应的可视化编排版本'
}

function closeDeploymentDetail() {
  deploymentDetailVisible.value = false
  deploymentDetailLoading.value = false
  deploymentDetailError.value = ''
  deploymentDetail.value = null
  deploymentCompareLoading.value = false
  deploymentCompareError.value = ''
  deploymentCompareResult.value = null
}

async function openDeploymentDetail(item) {
  deploymentDetailVisible.value = true
  deploymentDetailLoading.value = true
  deploymentDetailError.value = ''
  deploymentCompareLoading.value = false
  deploymentCompareError.value = ''
  deploymentCompareResult.value = null
  try {
    const { data } = await http.get(`/flowlong/deployment-records/${item.id}`)
    deploymentDetail.value = data || null
    await compareDeploymentVersions()
  } catch (error) {
    deploymentDetailError.value = error.normalizedMessage
  } finally {
    deploymentDetailLoading.value = false
  }
}

async function compareDeploymentVersions() {
  deploymentCompareLoading.value = false
  deploymentCompareError.value = ''
  deploymentCompareResult.value = null
  const sourceId = deploymentDetail.value?.sourceDefinition?.id
  const activeId = deploymentDetail.value?.activeDefinition?.id
  if (!sourceId || !activeId || sourceId === activeId) {
    return
  }
  deploymentCompareLoading.value = true
  try {
    const { data } = await http.get('/process-definition/compare', {
      params: {
        leftId: sourceId,
        rightId: activeId
      }
    })
    deploymentCompareResult.value = data || null
  } catch (error) {
    deploymentCompareError.value = error.normalizedMessage
  } finally {
    deploymentCompareLoading.value = false
  }
}

async function createProcess(nextMode = designerMode.value) {
  if (!confirmDiscardChanges('新建流程')) {
    return
  }
  resetMessages()
  selectedProcessId.value = null
  processForm.processKey = ''
  processForm.processName = ''
  runtimeStatus.value = {}
  deploymentHistory.value = []
  designerMode.value = nextMode
  await initDesigners({ processId: 'process', processName: '新建流程' })
  hasUnsavedChanges.value = false
  statusMessage.value = '已创建空白画布'
}

async function prepareLeaveProcessTemplate() {
  const targetMode = designerMode.value
  await createProcess(targetMode)
  processForm.processKey = 'leave-process'
  processForm.processName = '请假审批流程'
  if (targetMode === DESIGNER_TYPE_CUSTOM) {
    await customDesignerRef.value?.createLeaveTemplate()
  } else {
    await bpmnDesignerRef.value?.importXml(buildLeaveProcessTemplate(), true)
  }
  hasUnsavedChanges.value = true
  await refreshRuntimeStatus()
  statusMessage.value = '已切换到请假流程模板，请继续完善节点与审批流'
}

async function loadDefinition(id) {
  if (!confirmDiscardChanges('加载其他流程')) {
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
    if (data.bpmnXml) {
      await bpmnDesignerRef.value?.importXml(data.bpmnXml)
      bpmnDesignerRef.value?.markClean()
    }
    if (data.designerType === DESIGNER_TYPE_CUSTOM && data.designSchemaJson) {
      await customDesignerRef.value?.importSchema(data.designSchemaJson, false, buildDiagramMeta())
      customDesignerRef.value?.markClean()
    } else {
      await customDesignerRef.value?.createNewDiagram(buildDiagramMeta())
      customDesignerRef.value?.markClean()
    }
    hasUnsavedChanges.value = false
    await refreshRuntimeStatus()
    statusMessage.value = `已加载 ${data.processName}`
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

async function buildSavePayload() {
  processForm.processKey = normalizeProcessKey()
  if (!processForm.processName.trim()) {
    throw new Error('请输入流程名称')
  }
  if (designerMode.value === DESIGNER_TYPE_CUSTOM) {
    const schema = await customDesignerRef.value.getSchema()
    const bpmnXml = await customDesignerRef.value.getXml(buildDiagramMeta())
    validateReferencedFormKeys(extractFormKeysFromCustomSchema(schema))
    return {
      processKey: processForm.processKey,
      processName: processForm.processName.trim(),
      designerType: DESIGNER_TYPE_CUSTOM,
      designSchemaJson: JSON.stringify(schema),
      bpmnXml
    }
  }
  const bpmnXml = await bpmnDesignerRef.value.getXml()
  validateReferencedFormKeys(extractFormKeysFromBpmnXml(bpmnXml))
  return {
    processKey: processForm.processKey,
    processName: processForm.processName.trim(),
    designerType: DESIGNER_TYPE_BPMN,
    designSchemaJson: null,
    bpmnXml
  }
}

async function persistProcessDefinition() {
  const payload = await buildSavePayload()
  const wasEditing = Boolean(selectedProcessId.value)
  const request = selectedProcessId.value
    ? http.put(`/process-definition/update/${selectedProcessId.value}`, payload)
    : http.post('/process-definition/save', payload)
  const { data } = await request
  selectedProcessId.value = data.id
  processForm.processKey = data.processKey
  processForm.processName = data.processName
  if (payload.bpmnXml) {
    await bpmnDesignerRef.value?.importXml(payload.bpmnXml, designerMode.value === DESIGNER_TYPE_BPMN)
    bpmnDesignerRef.value?.markClean()
  }
  getActiveDesigner()?.markClean?.()
  hasUnsavedChanges.value = false
  await fetchProcessDefinitions()
  return { data, wasEditing, payload }
}

async function saveProcess() {
  resetMessages()
  loading.value = true
  try {
    const { wasEditing } = await persistProcessDefinition()
    await refreshRuntimeStatus()
    statusMessage.value = wasEditing ? '流程已保存' : '流程已创建'
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '保存失败'
  } finally {
    loading.value = false
  }
}

async function publishProcess() {
  resetMessages()
  loading.value = true
  try {
    const { payload } = await persistProcessDefinition()
    if (!selectedProcessId.value) {
      throw new Error('流程尚未保存，无法发布')
    }
    validateFlowLongSequenceCompatibility(payload?.bpmnXml || '')
    await http.put(`/process-definition/activate/${selectedProcessId.value}`, {})
    await http.post(`/flowlong/deploy/definition/${selectedProcessId.value}`)
    await fetchProcessDefinitions()
    await refreshRuntimeStatus()
    statusMessage.value = '流程已发布到 FlowLong 引擎'
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '发布失败'
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
    await http.put(`/process-definition/activate/${selectedProcessId.value}`, {})
    await fetchProcessDefinitions()
    await refreshRuntimeStatus()
    statusMessage.value = '当前版本已激活'
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

async function deployCurrentProcess() {
  if (!selectedProcessId.value) {
    return
  }
  resetMessages()
  loading.value = true
  try {
    validateFlowLongSequenceCompatibility(selectedDefinition.value?.bpmnXml || '')
    await http.post(`/flowlong/deploy/definition/${selectedProcessId.value}`)
    await refreshRuntimeStatus()
    statusMessage.value = '当前版本已部署到 FlowLong 引擎'
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

async function deployActiveProcess() {
  if (!currentProcessKey.value) {
    return
  }
  resetMessages()
  loading.value = true
  try {
    const activeDefinition = processDefinitions.value.find((item) => item.processKey === currentProcessKey.value && item.status === 'ACTIVE')
    validateFlowLongSequenceCompatibility(activeDefinition?.bpmnXml || '')
    await http.post(`/flowlong/deploy/active/${currentProcessKey.value}`)
    await refreshRuntimeStatus()
    statusMessage.value = '激活版本已部署到 FlowLong 引擎'
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

async function rollbackDeployment(item) {
  if (!item?.id) {
    return
  }
  resetMessages()
  rollbacking.value = true
  try {
    const { data } = await http.post(`/flowlong/deployment-records/${item.id}/rollback`)
    await fetchProcessDefinitions()
    await refreshRuntimeStatus()
    if (data?.rolledBackToDefinitionId) {
      await loadDefinition(data.rolledBackToDefinitionId)
    }
    if (deploymentDetailVisible.value) {
      const latestRecord = deploymentHistory.value[0]
      if (latestRecord?.id) {
        await openDeploymentDetail(latestRecord)
      }
    }
    statusMessage.value = `已回滚并重新部署到来源版本 v${data?.rolledBackToVersion || '-'}`
  } catch (error) {
    errorMessage.value = error.normalizedMessage
    deploymentDetailError.value = error.normalizedMessage
  } finally {
    rollbacking.value = false
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
  await getActiveDesigner()?.downloadXml?.(fileName)
}

async function downloadCustomSchema() {
  const fileName = `${normalizeProcessKey() || 'process'}-design.json`
  await customDesignerRef.value?.downloadSchema(fileName)
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
    designerMode.value = DESIGNER_TYPE_BPMN
    await nextTick()
    await bpmnDesignerRef.value?.importXml(xml, true)
    hasUnsavedChanges.value = true
    await refreshRuntimeStatus()
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
    const schemaJson = await file.text()
    const derivedName = file.name.replace(/\.json$/i, '')
    if (!selectedProcessId.value) {
      processForm.processName = processForm.processName || derivedName
      processForm.processKey = processForm.processKey || normalizeProcessKey(derivedName)
    }
    designerMode.value = DESIGNER_TYPE_CUSTOM
    await nextTick()
    await customDesignerRef.value?.importSchema(schemaJson, true, buildDiagramMeta())
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

async function syncStudioRoute() {
  const targetId = Number(route.query.id)
  if (!Number.isFinite(targetId) || targetId <= 0 || targetId === selectedProcessId.value) {
    return
  }
  await loadDefinition(targetId)
}

onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  try {
    await ensureRemoteFormRegistryLoaded().catch(() => {})
    await fetchProcessDefinitions()
    await initDesigners(buildDiagramMeta())
    await refreshRuntimeStatus()
    await syncStudioRoute()
  } catch (error) {
    errorMessage.value = error?.normalizedMessage || error?.message || '流程设计页初始化失败'
    processDefinitions.value = []
  }
})

watch(
  () => route.query.id,
  async () => {
    try {
      await syncStudioRoute()
    } catch (error) {
      errorMessage.value = error?.normalizedMessage || error?.message || '流程加载失败'
    }
  }
)

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

onBeforeRouteLeave(() => {
  if (!hasUnsavedChanges.value) {
    return true
  }
  return confirmDiscardChanges('离开流程设计页')
})
</script>

<style scoped>
.page-grid {
  align-content: start;
}

.studio-console-card {
  padding: 12px 14px 10px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #dfe6ef;
  box-shadow: none;
}

.studio-form-grid {
  gap: 8px 12px;
  margin-bottom: 2px;
}

.studio-action-row {
  gap: 6px;
  padding-top: 0;
  flex-wrap: wrap;
}

.studio-action-row > * {
  flex: 0 0 auto;
}

.definition-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  border-radius: 8px;
  min-height: 0;
}

.definition-panel-head {
  padding-bottom: 0;
}

.definition-filter-bar {
  padding: 6px 8px;
  border: 1px solid #e3eaf4;
  border-radius: 6px;
  background: #fafbfd;
}

.definition-toggle {
  justify-content: space-between;
  gap: 14px;
  width: 100%;
}

.definition-toggle-copy {
  display: grid;
  gap: 2px;
}

.definition-toggle-copy strong {
  color: #243445;
  font-size: 12px;
  font-weight: 700;
}

.definition-toggle-copy small {
  color: #7b8a9b;
  font-size: 11px;
}

.definition-toggle-switch {
  position: relative;
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
}

.definition-toggle-switch input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.definition-toggle-slider {
  width: 38px;
  height: 22px;
  border-radius: 999px;
  background: #d8e1ed;
  border: 1px solid #cad5e3;
  position: relative;
  transition: background-color 0.16s ease, border-color 0.16s ease;
}

.definition-toggle-slider::after {
  content: '';
  position: absolute;
  top: 1px;
  left: 1px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 2px 6px rgba(28, 42, 60, 0.16);
  transition: transform 0.16s ease;
}

.definition-toggle-switch input:checked + .definition-toggle-slider {
  background: linear-gradient(180deg, #5f9bff 0%, #3d7eff 100%);
  border-color: #4a83ef;
}

.definition-toggle-switch input:checked + .definition-toggle-slider::after {
  transform: translateX(16px);
}

.definition-list {
  gap: 8px;
  flex: 1 1 auto;
  min-height: 0;
  max-height: 268px;
  overflow: auto;
  padding-right: 2px;
}

.definition-card {
  padding: 10px;
  border-radius: 6px;
  border-color: #dfe7f2;
  box-shadow: none;
  background: #fff;
}

.definition-card:hover,
.definition-card.selected {
  border-color: #9dbbf1;
  box-shadow: none;
  background: #f7fbff;
}

.definition-title {
  align-items: flex-start;
  gap: 8px;
}

.definition-title strong {
  flex: 1;
  min-width: 0;
  color: #203246;
  font-size: 14px;
  line-height: 1.3;
}

.definition-key {
  margin-top: 4px;
  color: #5f7390;
  font-size: 11px;
  font-weight: 600;
  word-break: break-all;
}

.definition-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}

.definition-meta-row small {
  color: #7b8ca0;
  font-size: 11px;
}

.studio-content {
  display: grid;
  gap: 6px;
  align-content: start;
}

.studio-summary-grid {
  grid-template-columns: minmax(280px, 0.9fr) minmax(300px, 0.95fr) minmax(380px, 1.15fr);
  align-items: start;
  gap: 6px;
}

.summary-card {
  display: flex;
  flex-direction: column;
  min-height: 250px;
  height: auto;
  padding: 10px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #dfe6ef;
  box-shadow: none;
  align-self: start;
}

.runtime-panel,
.history-panel {
  min-height: 0;
}

.runtime-status-list {
  flex: 0 0 auto;
  min-height: 0;
}

.runtime-status-list .list-item {
  min-height: 48px;
  border-radius: 6px;
}

.history-table-wrap {
  flex: 0 0 auto;
  min-height: 0;
  max-height: 192px;
  overflow: auto;
  margin-top: 2px;
}

.history-panel :deep(.simple-table) {
  min-width: 100%;
}

.history-panel :deep(th) {
  white-space: nowrap;
}

.history-panel :deep(td) {
  white-space: nowrap;
}

.studio-mode-switch {
  display: inline-flex;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.definition-tags {
  margin-top: 6px;
}

.meta-pill-tight {
  padding-inline: 8px;
}

.studio-console-card :deep(.panel-head h3),
.summary-card :deep(.panel-head h3) {
  font-size: 16px;
  line-height: 1.2;
}

.studio-console-card :deep(.eyebrow),
.summary-card :deep(.eyebrow) {
  margin-bottom: 6px;
  font-size: 11px;
}

.studio-console-card :deep(.hint) {
  margin: 0;
  font-size: 12px;
  line-height: 1.45;
}

.designer-stage {
  min-height: clamp(560px, 62vh, 780px);
  border-radius: 8px;
  border: 1px solid #dfe6ef;
  background: #fff;
  overflow: hidden;
}

.designer-card {
  min-height: 0;
  margin: 0;
}

@media (max-width: 1280px) {
  .studio-summary-grid {
    grid-template-columns: 1fr;
  }

  .summary-card {
    min-height: auto;
  }

  .definition-list,
  .history-table-wrap {
    max-height: none;
  }
}

@media (max-width: 820px) {
  .studio-content {
    gap: 12px;
  }
}
</style>
