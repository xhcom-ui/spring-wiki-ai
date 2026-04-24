import {
  buildCustomBpmnXml,
  createDefaultCustomSchema,
  createLeaveCustomSchema
} from './seaflowDesigner'

function clone(value) {
  return value == null ? value : JSON.parse(JSON.stringify(value))
}

function text(value, fallback = '') {
  return typeof value === 'string' && value.trim() ? value.trim() : fallback
}

function createKey(prefix = 'flk') {
  return `${prefix}${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`
}

function createWorkflowData(nodeConfig, meta = {}) {
  return {
    id: Date.now(),
    name: text(meta.processName, '新建流程'),
    key: text(meta.processId, 'process'),
    nodeConfig
  }
}

function splitCsv(value) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function makeAssigneeList(values) {
  return values.map((value) => ({
    id: value,
    name: value
  }))
}

function conditionSummaryFromList(conditionList = []) {
  if (!Array.isArray(conditionList) || !conditionList.length) {
    return '默认分支'
  }
  if (
    conditionList.length === 1 &&
    conditionList[0].length === 1 &&
    conditionList[0][0]?.field === '__raw__' &&
    text(conditionList[0][0].rawExpression)
  ) {
    return text(conditionList[0][0].label, '条件分支')
  }
  return conditionList
    .map((group) => group.map((item) => `${text(item.label, text(item.field, '条件'))}${text(item.operator)}${text(item.value)}`).join(' 且 '))
    .join(' 或 ')
}

function buildConditionExpression(conditionList = [], isDefault = false) {
  if (isDefault || !Array.isArray(conditionList) || !conditionList.length) {
    return ''
  }
  if (
    conditionList.length === 1 &&
    conditionList[0].length === 1 &&
    conditionList[0][0]?.field === '__raw__' &&
    text(conditionList[0][0].rawExpression)
  ) {
    return text(conditionList[0][0].rawExpression)
  }

  const orGroups = conditionList
    .map((group) => group
      .map((item) => {
        const field = text(item.field)
        const value = text(item.value)
        const operator = text(item.operator, '==')
        if (!field) {
          return ''
        }
        if (operator === 'include') {
          return `${field} != null && ${field}.contains("${value}")`
        }
        if (operator === 'notinclude') {
          return `${field} == null || !${field}.contains("${value}")`
        }
        const normalizedValue = /^-?\d+(\.\d+)?$/.test(value) ? value : `"${value}"`
        return `${field} ${operator} ${normalizedValue}`
      })
      .filter(Boolean)
      .join(' && '))
    .filter(Boolean)

  if (!orGroups.length) {
    return ''
  }
  return `\${${orGroups.map((item) => `(${item})`).join(' || ')}}`
}

function legacyBranchToConditionList(node) {
  const summary = text(node?.properties?.conditionSummary, text(node?.nodeName, '条件分支'))
  const expression = text(node?.properties?.conditionExpression)
  if (!expression && node?.properties?.isDefault) {
    return []
  }
  return [[{
    label: summary,
    field: '__raw__',
    operator: '==',
    value: expression || summary,
    rawExpression: expression
  }]]
}

function legacyNodeToVendor(node) {
  if (!node || node.nodeType === 'end') {
    return null
  }

  if (node.nodeType === 'start') {
    return {
      nodeName: text(node.nodeName, '开始'),
      nodeKey: text(node.nodeId, createKey()),
      type: 0,
      nodeAssigneeList: clone(node.properties?.nodeAssigneeList) || [],
      initiatorLabel: text(node.properties?.initiatorLabel, '发起人'),
      formKey: text(node.properties?.formKey),
      formLabel: text(node.properties?.formLabel),
      pageLabel: text(node.properties?.pageLabel),
      childNode: legacyNodeToVendor(node.childNode)
    }
  }

  if (node.nodeType === 'between') {
    const assignmentMode = text(node.properties?.assignmentMode, 'assignee')
    const assignee = text(node.properties?.assignee)
    let setType = 1
    let nodeAssigneeList = []
    let examineLevel = 1

    if (assignmentMode === 'candidateGroups') {
      setType = 3
      nodeAssigneeList = makeAssigneeList(splitCsv(node.properties?.candidateGroups))
    } else if (assignmentMode === 'candidateUsers') {
      setType = 1
      nodeAssigneeList = makeAssigneeList(splitCsv(node.properties?.candidateUsers))
    } else if (assignee === '${deptManager}') {
      setType = 2
      examineLevel = 1
    } else if (assignee === '${generalManager}') {
      setType = 2
      examineLevel = 2
    } else if (assignee === '${initiator}') {
      setType = 5
    } else if (assignee === '${initiatorSelected}') {
      setType = 4
    } else if (assignee === '${multiLevelManager}') {
      setType = 7
    } else if (assignee) {
      nodeAssigneeList = makeAssigneeList([assignee])
    }

    return {
      nodeName: text(node.nodeName, '审批节点'),
      nodeKey: text(node.nodeId, createKey()),
      type: 1,
      setType,
      nodeAssigneeList,
      examineLevel,
      directorLevel: 1,
      selectMode: 1,
      termAuto: false,
      term: 0,
      termMode: 1,
      examineMode: 1,
      directorMode: 0,
      formKey: text(node.properties?.formKey),
      formLabel: text(node.properties?.formLabel),
      pageLabel: text(node.properties?.pageLabel),
      listeners: clone(node.properties?.listeners) || [],
      childNode: legacyNodeToVendor(node.childNode)
    }
  }

  if (node.nodeType === 'copy') {
    const assignees = splitCsv(node.properties?.candidateUsers || node.properties?.assignee)
    return {
      nodeName: text(node.nodeName, '抄送节点'),
      nodeKey: text(node.nodeId, createKey()),
      type: 2,
      userSelectFlag: !assignees.length,
      nodeAssigneeList: makeAssigneeList(assignees),
      childNode: legacyNodeToVendor(node.childNode)
    }
  }

  if (node.nodeType === 'serial') {
    const conditionNodes = Array.isArray(node.conditionNodes) ? node.conditionNodes : []
    return {
      nodeName: text(node.nodeName, '条件分支'),
      nodeKey: text(node.nodeId, createKey()),
      type: 4,
      conditionNodes: conditionNodes.map((branch, index) => ({
        nodeName: text(branch.nodeName, index === conditionNodes.length - 1 ? '默认条件' : `条件 ${index + 1}`),
        nodeKey: text(branch.nodeId, createKey()),
        type: 3,
        priorityLevel: index + 1,
        conditionMode: 1,
        conditionList: legacyBranchToConditionList(branch),
        childNode: legacyNodeToVendor(branch.childNode)
      })),
      childNode: legacyNodeToVendor(node.childNode)
    }
  }

  return legacyNodeToVendor(node.childNode)
}

function approverToAssignment(node) {
  const setType = Number(node?.setType || 1)
  const assignees = Array.isArray(node?.nodeAssigneeList) ? node.nodeAssigneeList : []
  if (setType === 3) {
    return {
      assignmentMode: 'candidateGroups',
      assignee: '',
      candidateUsers: '',
      candidateGroups: assignees.map((item) => item.id || item.name).filter(Boolean).join(',')
    }
  }
  if (setType === 2) {
    const level = Number(node?.examineLevel || 1)
    return {
      assignmentMode: 'assignee',
      assignee: level >= 2 ? '${generalManager}' : '${deptManager}',
      candidateUsers: '',
      candidateGroups: ''
    }
  }
  if (setType === 4) {
    return {
      assignmentMode: 'assignee',
      assignee: '${initiatorSelected}',
      candidateUsers: '',
      candidateGroups: ''
    }
  }
  if (setType === 5) {
    return {
      assignmentMode: 'assignee',
      assignee: '${initiator}',
      candidateUsers: '',
      candidateGroups: ''
    }
  }
  if (setType === 7) {
    return {
      assignmentMode: 'assignee',
      assignee: '${multiLevelManager}',
      candidateUsers: '',
      candidateGroups: ''
    }
  }
  if (assignees.length > 1) {
    return {
      assignmentMode: 'candidateUsers',
      assignee: '',
      candidateUsers: assignees.map((item) => item.id || item.name).filter(Boolean).join(','),
      candidateGroups: ''
    }
  }
  return {
    assignmentMode: 'assignee',
    assignee: text(assignees[0]?.id || assignees[0]?.name),
    candidateUsers: '',
    candidateGroups: ''
  }
}

function vendorNodeToLegacy(node, meta = {}) {
  if (!node || typeof node !== 'object') {
    return null
  }

  if (Number(node.type) === 0) {
    return {
      nodeId: text(node.nodeKey, createKey('start_')),
      nodeType: 'start',
      nodeName: text(node.nodeName, '开始'),
      properties: {
        initiatorLabel: text(node.initiatorLabel, '发起人'),
        formKey: text(node.formKey),
        formLabel: text(node.formLabel),
        pageLabel: text(node.pageLabel)
      },
      childNode: vendorNodeToLegacy(node.childNode, meta)
    }
  }

  if (Number(node.type) === 1) {
    const assignment = approverToAssignment(node)
    return {
      nodeId: text(node.nodeKey, createKey('task_')),
      nodeType: 'between',
      nodeName: text(node.nodeName, '审批节点'),
      properties: {
        ...assignment,
        formKey: text(node.formKey),
        formLabel: text(node.formLabel),
        pageLabel: text(node.pageLabel),
        listeners: clone(node.listeners) || []
      },
      childNode: vendorNodeToLegacy(node.childNode, meta)
    }
  }

  if (Number(node.type) === 2) {
    const assignees = Array.isArray(node?.nodeAssigneeList) ? node.nodeAssigneeList : []
    return {
      nodeId: text(node.nodeKey, createKey('copy_')),
      nodeType: 'between',
      nodeName: text(node.nodeName, '抄送节点'),
      properties: {
        assignmentMode: assignees.length ? 'candidateUsers' : 'assignee',
        assignee: assignees.length ? '' : (node.userSelectFlag ? '${initiatorSelected}' : ''),
        candidateUsers: assignees.map((item) => item.id || item.name).filter(Boolean).join(','),
        candidateGroups: '',
        formKey: '',
        formLabel: '',
        pageLabel: '抄送节点',
        listeners: []
      },
      childNode: vendorNodeToLegacy(node.childNode, meta)
    }
  }

  if (Number(node.type) === 4) {
    const branches = Array.isArray(node.conditionNodes) ? node.conditionNodes : []
    return {
      nodeId: text(node.nodeKey, createKey('gateway_')),
      nodeType: 'serial',
      nodeName: text(node.nodeName, '条件分支'),
      properties: {},
      conditionNodes: branches.map((branch, index) => {
        const isDefault = (!Array.isArray(branch.conditionList) || !branch.conditionList.length) && index === branches.length - 1
        return {
          nodeId: text(branch.nodeKey, createKey('condition_')),
          nodeType: 'serial-node',
          nodeName: text(branch.nodeName, isDefault ? '默认条件' : `条件 ${index + 1}`),
          properties: {
            conditionSummary: conditionSummaryFromList(branch.conditionList),
            conditionExpression: buildConditionExpression(branch.conditionList, isDefault),
            isDefault
          },
          childNode: vendorNodeToLegacy(branch.childNode, meta)
        }
      }),
      childNode: vendorNodeToLegacy(node.childNode, meta)
    }
  }

  return vendorNodeToLegacy(node.childNode, meta)
}

function ensureTerminalEnd(node) {
  if (!node) {
    return {
      nodeId: createKey('end_'),
      nodeType: 'end',
      nodeName: '结束',
      properties: {}
    }
  }
  if (node.childNode) {
    node.childNode = ensureTerminalEnd(node.childNode)
    return node
  }
  node.childNode = {
    nodeId: createKey('end_'),
    nodeType: 'end',
    nodeName: '结束',
    properties: {}
  }
  return node
}

export function isVendorWorkflowSchema(value) {
  return Boolean(value?.nodeConfig && typeof value.nodeConfig === 'object' && Number.isInteger(Number(value.nodeConfig.type)))
}

export function normalizeVendorWorkflowSchema(value, meta = {}) {
  if (isVendorWorkflowSchema(value)) {
    const source = clone(value)
    source.name = text(source.name, text(meta.processName, '新建流程'))
    source.key = text(source.key, text(meta.processId, 'process'))
    return source
  }
  return createDefaultVendorWorkflow(meta)
}

export function legacyCustomSchemaToVendorWorkflow(schema, meta = {}) {
  const vendorNode = legacyNodeToVendor(clone(schema) || createDefaultCustomSchema(meta))
  return createWorkflowData(vendorNode || legacyNodeToVendor(createDefaultCustomSchema(meta)), meta)
}

export function createDefaultVendorWorkflow(meta = {}) {
  return legacyCustomSchemaToVendorWorkflow(createDefaultCustomSchema(meta), meta)
}

export function createLeaveVendorWorkflow(meta = {}) {
  return legacyCustomSchemaToVendorWorkflow(createLeaveCustomSchema(), {
    processName: text(meta.processName, '请假审批流程'),
    processId: text(meta.processId, 'leave-process')
  })
}

export function parseStoredVendorWorkflow(rawValue, meta = {}) {
  if (!rawValue) {
    return createDefaultVendorWorkflow(meta)
  }
  try {
    const parsed = typeof rawValue === 'string' ? JSON.parse(rawValue) : rawValue
    if (isVendorWorkflowSchema(parsed)) {
      return normalizeVendorWorkflowSchema(parsed, meta)
    }
    return legacyCustomSchemaToVendorWorkflow(parsed, meta)
  } catch (error) {
    return createDefaultVendorWorkflow(meta)
  }
}

export function vendorWorkflowToLegacyCustomSchema(workflow, meta = {}) {
  const normalized = normalizeVendorWorkflowSchema(workflow, meta)
  return ensureTerminalEnd(vendorNodeToLegacy(normalized.nodeConfig, meta))
}

export function buildVendorWorkflowBpmnXml(workflow, meta = {}) {
  const processId = text(meta.processId || workflow?.key, 'process')
  const processName = text(meta.processName || workflow?.name, '新建流程')
  return buildCustomBpmnXml(vendorWorkflowToLegacyCustomSchema(workflow, { processId, processName }), {
    processId,
    processName
  })
}
