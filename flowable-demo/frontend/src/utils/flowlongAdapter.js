import {
  buildCustomBpmnXml,
  createDefaultCustomSchema,
  normalizeCustomSchema,
  parseStoredCustomSchema,
  summarizeUserTask
} from './seaflowDesigner.js'

function clone(value) {
  return value ? JSON.parse(JSON.stringify(value)) : value
}

function safeText(...values) {
  for (const value of values) {
    if (typeof value === 'string' && value.trim()) {
      return value.trim()
    }
  }
  return ''
}

function createId(prefix = 'flw') {
  return `${prefix}_${Math.random().toString(36).slice(2, 10)}`
}

function createEndNode() {
  return {
    nodeId: createId('end'),
    nodeType: 'end',
    nodeName: '结束',
    value: '流程结束',
    properties: {}
  }
}

function createFlowlongBranch(index = 1, gatewayMode = 'serial') {
  return {
    nodeKey: createId('branch'),
    nodeId: '',
    type: 3,
    nodeName: gatewayMode === 'parallel' ? `并行分支 ${index}` : `条件 ${index}`,
    properties: {
      conditionSummary: gatewayMode === 'parallel' ? '' : '',
      conditionExpression: '',
      isDefault: gatewayMode === 'serial' && index === 2
    },
    childNode: null
  }
}

function createFlowlongStart(meta = {}) {
  const custom = createDefaultCustomSchema(meta)
  return customToFlowlongNode(custom)
}

function defaultNodeName(type) {
  switch (type) {
    case 0:
      return '发起人'
    case 1:
      return '审批节点'
    case 2:
      return '抄送节点'
    case 3:
      return '条件分支'
    case 4:
      return '条件路由'
    default:
      return '流程节点'
  }
}

function ensureArray(value) {
  return Array.isArray(value) ? value : []
}

function ensureStartProperties(properties) {
  return {
    initiatorLabel: safeText(properties?.initiatorLabel, '发起人'),
    initiatorUsers: safeText(properties?.initiatorUsers),
    initiatorUserList: ensureArray(properties?.initiatorUserList)
      .map((item) => ({
        id: safeText(item?.id),
        name: safeText(item?.name, item?.id)
      }))
      .filter((item) => item.id || item.name),
    formKey: safeText(properties?.formKey),
    formLabel: safeText(properties?.formLabel),
    pageLabel: safeText(properties?.pageLabel)
  }
}

function ensureTaskProperties(properties) {
  return {
    assignmentMode: safeText(properties?.assignmentMode, 'assignee') || 'assignee',
    assignee: safeText(properties?.assignee),
    candidateUsers: safeText(properties?.candidateUsers),
    candidateGroups: safeText(properties?.candidateGroups),
    assigneeList: ensureArray(properties?.assigneeList)
      .map((item) => ({
        id: safeText(item?.id),
        name: safeText(item?.name, item?.id)
      }))
      .filter((item) => item.id || item.name),
    candidateUserList: ensureArray(properties?.candidateUserList)
      .map((item) => ({
        id: safeText(item?.id),
        name: safeText(item?.name, item?.id)
      }))
      .filter((item) => item.id || item.name),
    candidateGroupList: ensureArray(properties?.candidateGroupList)
      .map((item) => ({
        id: safeText(item?.id),
        name: safeText(item?.name, item?.id)
      }))
      .filter((item) => item.id || item.name),
    formKey: safeText(properties?.formKey),
    formLabel: safeText(properties?.formLabel),
    pageLabel: safeText(properties?.pageLabel),
    listeners: ensureArray(properties?.listeners)
      .map((item) => ({
        listenerType: safeText(item?.listenerType),
        listenerPath: safeText(item?.listenerPath)
      }))
      .filter((item) => item.listenerType && item.listenerPath)
  }
}

function ensureBranchProperties(properties, fallbackIndex = 1, gatewayMode = 'serial') {
  return {
    conditionSummary: safeText(properties?.conditionSummary, gatewayMode === 'parallel' ? '' : `条件 ${fallbackIndex}`),
    conditionExpression: safeText(properties?.conditionExpression),
    isDefault: gatewayMode === 'serial' ? Boolean(properties?.isDefault) : false
  }
}

function summarizeFlowlongNode(node) {
  if (!node) {
    return ''
  }
  if (node.type === 0) {
    const selectedUsers = ensureArray(node.properties?.initiatorUserList)
      .map((item) => safeText(item?.name, item?.id))
      .filter(Boolean)
    const parts = [safeText(node.properties?.initiatorLabel, '发起人')]
    if (selectedUsers.length || safeText(node.properties?.initiatorUsers)) {
      parts.push(`发起范围 ${selectedUsers.join('、') || node.properties.initiatorUsers}`)
    }
    if (safeText(node.properties?.formLabel)) {
      parts.push(`表单 ${node.properties.formLabel}`)
    }
    if (safeText(node.properties?.pageLabel)) {
      parts.push(`页面 ${node.properties.pageLabel}`)
    }
    return parts.filter(Boolean).join(' · ')
  }
  if (node.type === 1) {
    return summarizeUserTask({ properties: node.properties || {} })
  }
  if (node.type === 2) {
    const selectedUsers = ensureArray(node.properties?.copyUserList)
      .map((item) => safeText(item?.name, item?.id))
      .filter(Boolean)
    const parts = []
    if (selectedUsers.length || safeText(node.properties?.copyUsers)) {
      parts.push(`抄送 ${selectedUsers.join('、') || node.properties.copyUsers}`)
    }
    if (node.properties?.allowInitiatorSelect) {
      parts.push('发起人可自选')
    }
    return parts.join(' · ') || '待配置抄送人'
  }
  if (node.type === 3) {
    if (node.properties?.isDefault) {
      return '默认分支'
    }
    return safeText(node.properties?.conditionSummary, node.properties?.conditionExpression, '请设置条件')
  }
  return ''
}

function normalizeFlowlongNode(node, fallbackType = 0, branchIndex = 1, gatewayMode = 'serial') {
  if (!node || typeof node !== 'object') {
    return null
  }
  const type = Number.isFinite(Number(node.type)) ? Number(node.type) : fallbackType
  const normalized = {
    ...clone(node),
    type,
    nodeKey: safeText(node.nodeKey, node.nodeId, createId(type === 0 ? 'start' : type === 4 ? 'gateway' : type === 3 ? 'branch' : 'task')),
    nodeName: safeText(node.nodeName, defaultNodeName(type))
  }
  normalized.nodeId = normalized.nodeKey

  if (type === 0) {
    normalized.properties = ensureStartProperties(node.properties)
  } else if (type === 1) {
    normalized.properties = ensureTaskProperties(node.properties)
  } else if (type === 2) {
    normalized.properties = {
      copyUsers: safeText(node.properties?.copyUsers),
      copyUserList: ensureArray(node.properties?.copyUserList)
        .map((item) => ({
          id: safeText(item?.id),
          name: safeText(item?.name, item?.id)
        }))
        .filter((item) => item.id || item.name),
      allowInitiatorSelect: Boolean(node.properties?.allowInitiatorSelect)
    }
  } else if (type === 4) {
    const mode = safeText(node.properties?.gatewayMode, gatewayMode || 'serial') || 'serial'
    normalized.properties = { gatewayMode: mode === 'parallel' ? 'parallel' : 'serial' }
    const rawBranches = ensureArray(node.conditionNodes)
    const minimumCount = mode === 'parallel' ? 2 : 2
    const branches = rawBranches.length >= minimumCount
      ? rawBranches
      : Array.from({ length: minimumCount }, (_, index) => createFlowlongBranch(index + 1, mode))
    normalized.conditionNodes = branches
      .map((item, index) => normalizeFlowlongNode(item, 3, index + 1, mode))
      .filter(Boolean)
    if (mode !== 'parallel') {
      if (!normalized.conditionNodes.some((item) => item.properties?.isDefault)) {
        const last = normalized.conditionNodes[normalized.conditionNodes.length - 1]
        if (last) {
          last.properties.isDefault = true
        }
      }
      normalized.conditionNodes.forEach((item, index) => {
        if (item.properties?.isDefault) {
          item.nodeName = safeText(item.nodeName, '默认条件')
        } else {
          item.nodeName = safeText(item.nodeName, `条件 ${index + 1}`)
        }
      })
    }
  } else if (type === 3) {
    normalized.properties = ensureBranchProperties(node.properties, branchIndex, gatewayMode)
  } else {
    normalized.type = 1
    normalized.properties = ensureTaskProperties(node.properties)
  }

  if (type !== 4) {
    normalized.childNode = normalizeFlowlongNode(node.childNode, 1, 1, gatewayMode)
  } else {
    normalized.childNode = normalizeFlowlongNode(node.childNode, 1, 1, gatewayMode)
  }

  normalized.value = summarizeFlowlongNode(normalized)
  return normalized
}

function customToFlowlongNode(node) {
  if (!node || node.nodeType === 'end') {
    return null
  }
  if (node.nodeType === 'start') {
    return normalizeFlowlongNode({
      type: 0,
      nodeKey: safeText(node.nodeId, createId('start')),
      nodeName: safeText(node.nodeName, '发起人'),
      properties: ensureStartProperties(node.properties),
      childNode: customToFlowlongNode(node.childNode)
    }, 0)
  }
  if (node.nodeType === 'between') {
    return normalizeFlowlongNode({
      type: 1,
      nodeKey: safeText(node.nodeId, createId('task')),
      nodeName: safeText(node.nodeName, '审批节点'),
      properties: ensureTaskProperties(node.properties),
      childNode: customToFlowlongNode(node.childNode)
    }, 1)
  }
  if (node.nodeType === 'copy') {
    return normalizeFlowlongNode({
      type: 2,
      nodeKey: safeText(node.nodeId, createId('copy')),
      nodeName: safeText(node.nodeName, '抄送节点'),
      properties: {
        copyUsers: safeText(node.properties?.copyUsers),
        copyUserList: ensureArray(node.properties?.copyUserList)
          .map((item) => ({
            id: safeText(item?.id),
            name: safeText(item?.name, item?.id)
          }))
          .filter((item) => item.id || item.name),
        allowInitiatorSelect: Boolean(node.properties?.allowInitiatorSelect)
      },
      childNode: customToFlowlongNode(node.childNode)
    }, 2)
  }
  if (node.nodeType === 'serial' || node.nodeType === 'parallel') {
    const gatewayMode = node.nodeType === 'parallel' ? 'parallel' : 'serial'
    return normalizeFlowlongNode({
      type: 4,
      nodeKey: safeText(node.nodeId, createId('gateway')),
      nodeName: safeText(node.nodeName, gatewayMode === 'parallel' ? '并行分支' : '条件分支'),
      properties: {
        gatewayMode
      },
      conditionNodes: ensureArray(node.conditionNodes).map((item, index) => {
        if (gatewayMode === 'parallel') {
          return {
            type: 3,
            nodeKey: safeText(item.nodeId, createId('branch')),
            nodeName: safeText(item.nodeName, `并行分支 ${index + 1}`),
            properties: {
              conditionSummary: '',
              conditionExpression: '',
              isDefault: false
            },
            childNode: customToFlowlongNode(item)
          }
        }
        return {
          type: 3,
          nodeKey: safeText(item.nodeId, createId('branch')),
          nodeName: safeText(item.nodeName, `条件 ${index + 1}`),
          properties: ensureBranchProperties(item.properties, index + 1, 'serial'),
          childNode: customToFlowlongNode(item.childNode)
        }
      }),
      childNode: customToFlowlongNode(node.childNode)
    }, 4, 1, gatewayMode)
  }
  return null
}

function flowlongBranchToCustom(node, gatewayMode = 'serial') {
  if (!node) {
    return null
  }
  if (gatewayMode === 'parallel') {
    const branchNode = flowlongToCustomNode(node.childNode)
    if (branchNode) {
      return branchNode
    }
    return {
      nodeId: safeText(node.nodeKey, createId('task')),
      nodeType: 'between',
      nodeName: safeText(node.nodeName, '并行审批'),
      value: '',
      properties: ensureTaskProperties({})
    }
  }
  return {
    nodeId: safeText(node.nodeKey, createId('branch')),
    nodeType: 'serial-node',
    nodeName: safeText(node.nodeName, '条件分支'),
    value: '',
    properties: ensureBranchProperties(node.properties, 1, 'serial'),
    childNode: flowlongToCustomNode(node.childNode)
  }
}

function flowlongToCustomNode(node) {
  if (!node) {
    return null
  }
  const normalized = normalizeFlowlongNode(node, Number(node.type) || 1)
  if (normalized.type === 0) {
    return {
      nodeId: safeText(normalized.nodeKey, createId('start')),
      nodeType: 'start',
      nodeName: safeText(normalized.nodeName, '开始'),
      value: '',
      properties: ensureStartProperties(normalized.properties),
      childNode: flowlongToCustomNode(normalized.childNode)
    }
  }
  if (normalized.type === 1) {
    return {
      nodeId: safeText(normalized.nodeKey, createId('task')),
      nodeType: 'between',
      nodeName: safeText(normalized.nodeName, '审批节点'),
      value: '',
      properties: ensureTaskProperties(normalized.properties),
      childNode: flowlongToCustomNode(normalized.childNode)
    }
  }
  if (normalized.type === 2) {
    return {
      nodeId: safeText(normalized.nodeKey, createId('copy')),
      nodeType: 'copy',
      nodeName: safeText(normalized.nodeName, '抄送节点'),
      value: '',
      properties: {
        copyUsers: safeText(normalized.properties?.copyUsers),
        copyUserList: ensureArray(normalized.properties?.copyUserList)
          .map((item) => ({
            id: safeText(item?.id),
            name: safeText(item?.name, item?.id)
          }))
          .filter((item) => item.id || item.name),
        allowInitiatorSelect: Boolean(normalized.properties?.allowInitiatorSelect)
      },
      childNode: flowlongToCustomNode(normalized.childNode)
    }
  }
  if (normalized.type === 4) {
    const gatewayMode = safeText(normalized.properties?.gatewayMode, 'serial')
    return {
      nodeId: safeText(normalized.nodeKey, createId('gateway')),
      nodeType: gatewayMode === 'parallel' ? 'parallel' : 'serial',
      nodeName: safeText(normalized.nodeName, gatewayMode === 'parallel' ? '并行分支' : '条件分支'),
      value: '',
      properties: {},
      conditionNodes: ensureArray(normalized.conditionNodes)
        .map((item) => flowlongBranchToCustom(item, gatewayMode))
        .filter(Boolean),
      childNode: flowlongToCustomNode(normalized.childNode)
    }
  }
  return null
}

function appendTerminalEnd(node) {
  if (!node) {
    return null
  }
  if (node.nodeType === 'start' || node.nodeType === 'between') {
    node.childNode = appendTerminalEnd(node.childNode)
    if (!node.childNode) {
      node.childNode = createEndNode()
    }
    return node
  }
  if (node.nodeType === 'copy') {
    node.childNode = appendTerminalEnd(node.childNode)
    if (!node.childNode) {
      node.childNode = createEndNode()
    }
    return node
  }
  if (node.nodeType === 'serial') {
    node.conditionNodes = ensureArray(node.conditionNodes).map((item) => {
      if (item?.childNode) {
        item.childNode = appendTerminalEnd(item.childNode)
      }
      return item
    })
    node.childNode = appendTerminalEnd(node.childNode)
    if (!node.childNode) {
      node.childNode = createEndNode()
    }
    return node
  }
  if (node.nodeType === 'parallel') {
    node.conditionNodes = ensureArray(node.conditionNodes).map((item) => appendTerminalEnd(item)).filter(Boolean)
    node.childNode = appendTerminalEnd(node.childNode)
    if (!node.childNode) {
      node.childNode = createEndNode()
    }
    return node
  }
  if (node.nodeType === 'serial-node') {
    node.childNode = appendTerminalEnd(node.childNode)
    return node
  }
  return node
}

export function createFlowlongWorkflow(meta = {}) {
  const processId = safeText(meta.processId, 'process')
  const processName = safeText(meta.processName, '新建流程')
  return {
    id: createId('workflow'),
    key: processId,
    name: processName,
    nodeConfig: createFlowlongStart({ processName })
  }
}

export function normalizeFlowlongWorkflow(workflow, meta = {}) {
  const base = workflow && typeof workflow === 'object' ? clone(workflow) : {}
  const normalized = {
    id: safeText(base.id, createId('workflow')),
    key: safeText(base.key, meta.processId, 'process'),
    name: safeText(base.name, meta.processName, '新建流程'),
    nodeConfig: normalizeFlowlongNode(base.nodeConfig || createFlowlongStart(meta), 0)
  }
  return normalized
}

export function parseStoredFlowlongSchema(rawValue, meta = {}) {
  if (!rawValue) {
    return createFlowlongWorkflow(meta)
  }
  try {
    const parsed = typeof rawValue === 'string' ? JSON.parse(rawValue) : rawValue
    if (parsed?.nodeConfig) {
      return normalizeFlowlongWorkflow(parsed, meta)
    }
    if ([0, 1, 2, 3, 4].includes(Number(parsed?.type))) {
      return normalizeFlowlongWorkflow({
        id: createId('workflow'),
        key: safeText(meta.processId, 'process'),
        name: safeText(meta.processName, '新建流程'),
        nodeConfig: parsed
      }, meta)
    }
    const customSchema = normalizeCustomSchema(parsed, meta)
    return customSchemaToFlowlongWorkflow(customSchema, meta)
  } catch (error) {
    return customSchemaToFlowlongWorkflow(parseStoredCustomSchema(rawValue, meta), meta)
  }
}

export function customSchemaToFlowlongWorkflow(customSchema, meta = {}) {
  const normalizedCustom = normalizeCustomSchema(customSchema, meta)
  return normalizeFlowlongWorkflow({
    id: createId('workflow'),
    key: safeText(meta.processId, 'process'),
    name: safeText(meta.processName, '新建流程'),
    nodeConfig: customToFlowlongNode(normalizedCustom)
  }, meta)
}

export function flowlongWorkflowToCustomSchema(workflow, meta = {}) {
  const normalizedWorkflow = normalizeFlowlongWorkflow(workflow, meta)
  const customRoot = flowlongToCustomNode(normalizedWorkflow.nodeConfig) || createDefaultCustomSchema(meta)
  return normalizeCustomSchema(appendTerminalEnd(customRoot), meta)
}

export async function buildFlowlongBpmnXml(workflow, meta = {}) {
  return buildCustomBpmnXml(flowlongWorkflowToCustomSchema(workflow, meta), meta)
}

export function exportFlowlongSchemaJson(workflow, meta = {}) {
  return JSON.stringify(flowlongWorkflowToCustomSchema(workflow, meta), null, 2)
}

export { summarizeFlowlongNode }
