function createId(prefix = 'node') {
  return `${prefix}_${Math.random().toString(36).slice(2, 10)}`
}

function clone(value) {
  return value ? JSON.parse(JSON.stringify(value)) : value
}

function safeText(value, fallback = '') {
  return typeof value === 'string' && value.trim() ? value.trim() : fallback
}

function escapeXml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
}

function defaultUserTask(name = '审批节点', assignee = '') {
  return {
    nodeId: createId('task'),
    nodeType: 'between',
    nodeName: name,
    value: '',
    properties: {
      assignmentMode: 'assignee',
      assignee,
      candidateUsers: '',
      candidateGroups: '',
      formKey: '',
      formLabel: '',
      pageLabel: '',
      listeners: []
    }
  }
}

export function createSerialConditions() {
  return [
    {
      nodeId: createId('condition'),
      nodeType: 'serial-node',
      nodeName: '条件 1',
      value: '',
      properties: {
        conditionSummary: '',
        conditionExpression: '',
        isDefault: false
      }
    },
    {
      nodeId: createId('condition'),
      nodeType: 'serial-node',
      nodeName: '默认条件',
      value: '默认分支',
      properties: {
        conditionSummary: '默认分支',
        conditionExpression: '',
        isDefault: true
      }
    }
  ]
}

export function summarizeUserTask(node) {
  const properties = node?.properties || {}
  const parts = []
  if (properties.assignmentMode === 'candidateUsers' && safeText(properties.candidateUsers)) {
    parts.push(`候选人 ${properties.candidateUsers}`)
  } else if (properties.assignmentMode === 'candidateGroups' && safeText(properties.candidateGroups)) {
    parts.push(`候选组 ${properties.candidateGroups}`)
  } else if (safeText(properties.assignee)) {
    parts.push(`办理人 ${properties.assignee}`)
  }
  if (safeText(properties.formLabel)) {
    parts.push(`表单 ${properties.formLabel}`)
  }
  if (safeText(properties.pageLabel)) {
    parts.push(`页面 ${properties.pageLabel}`)
  }
  return parts.join(' · ') || '待配置审批人'
}

export function getNodeTagLabels(node) {
  const properties = node?.properties || {}
  return [safeText(properties.pageLabel), safeText(properties.formLabel)].filter(Boolean)
}

export function updateDerivedNodeValue(node) {
  if (!node) {
    return
  }
  if (node.nodeType === 'between') {
    node.value = summarizeUserTask(node)
    return
  }
  if (node.nodeType === 'start') {
    const properties = node.properties || {}
    const parts = [safeText(properties.initiatorLabel, '发起人')]
    if (safeText(properties.formLabel)) {
      parts.push(properties.formLabel)
    }
    node.value = parts.join(' · ')
    return
  }
  if (node.nodeType === 'serial-node') {
    const properties = node.properties || {}
    node.value = properties.isDefault
      ? '默认分支'
      : safeText(properties.conditionSummary, safeText(properties.conditionExpression, '请设置条件'))
  }
  if (node.nodeType === 'end') {
    node.value = safeText(node.value, '流程结束')
  }
}

function ensureNode(node) {
  if (!node || typeof node !== 'object') {
    return null
  }
  node.nodeId = safeText(node.nodeId, createId(node.nodeType || 'node'))
  node.nodeName = safeText(node.nodeName, '未命名节点')
  node.properties = node.properties && typeof node.properties === 'object' ? node.properties : {}

  if (node.childNode) {
    node.childNode = ensureNode(node.childNode)
  }
  if (Array.isArray(node.conditionNodes)) {
    node.conditionNodes = node.conditionNodes.map(ensureNode).filter(Boolean)
  }

  if (node.nodeType === 'start') {
    node.properties.initiatorLabel = safeText(node.properties.initiatorLabel, '发起人')
    node.properties.formKey = safeText(node.properties.formKey)
    node.properties.formLabel = safeText(node.properties.formLabel)
    node.properties.pageLabel = safeText(node.properties.pageLabel)
    updateDerivedNodeValue(node)
  } else if (node.nodeType === 'between') {
    node.properties.assignmentMode = safeText(node.properties.assignmentMode, 'assignee')
    node.properties.assignee = safeText(node.properties.assignee)
    node.properties.candidateUsers = safeText(node.properties.candidateUsers)
    node.properties.candidateGroups = safeText(node.properties.candidateGroups)
    node.properties.formKey = safeText(node.properties.formKey)
    node.properties.formLabel = safeText(node.properties.formLabel)
    node.properties.pageLabel = safeText(node.properties.pageLabel)
    node.properties.listeners = Array.isArray(node.properties.listeners) ? node.properties.listeners : []
    updateDerivedNodeValue(node)
  } else if (node.nodeType === 'serial') {
    if (!Array.isArray(node.conditionNodes) || node.conditionNodes.length < 2) {
      node.conditionNodes = createSerialConditions()
    }
    node.conditionNodes.forEach((item, index) => {
      item.nodeType = 'serial-node'
      item.properties = item.properties && typeof item.properties === 'object' ? item.properties : {}
      item.properties.conditionSummary = safeText(item.properties.conditionSummary, item.value || '')
      item.properties.conditionExpression = safeText(item.properties.conditionExpression)
      item.properties.isDefault = Boolean(item.properties.isDefault)
      item.nodeName = safeText(item.nodeName, `条件 ${index + 1}`)
      updateDerivedNodeValue(item)
    })
    if (!node.conditionNodes.some((item) => item.properties?.isDefault)) {
      const lastBranch = node.conditionNodes[node.conditionNodes.length - 1]
      lastBranch.properties.isDefault = true
      updateDerivedNodeValue(lastBranch)
    }
  } else if (node.nodeType === 'parallel') {
    if (!Array.isArray(node.conditionNodes) || node.conditionNodes.length < 2) {
      node.conditionNodes = [defaultUserTask('并行审批 1'), defaultUserTask('并行审批 2')]
    }
  } else if (node.nodeType === 'end') {
    updateDerivedNodeValue(node)
  }

  return node
}

export function createDefaultCustomSchema({ processName = '新建流程' } = {}) {
  return ensureNode({
    nodeId: createId('start'),
    nodeType: 'start',
    nodeName: '开始',
    value: '发起人',
    properties: {
      initiatorLabel: '发起人',
      formKey: '',
      formLabel: '',
      pageLabel: ''
    },
    childNode: {
      ...defaultUserTask(processName.includes('请假') ? '部门经理审批' : '审批节点', processName.includes('请假') ? '${deptManager}' : ''),
      childNode: {
        nodeId: createId('end'),
        nodeType: 'end',
        nodeName: '结束',
        value: '流程结束',
        properties: {}
      }
    }
  })
}

export function createLeaveCustomSchema() {
  return ensureNode({
    nodeId: createId('start'),
    nodeType: 'start',
    nodeName: '提交申请',
    properties: {
      initiatorLabel: '申请人',
      formKey: 'leave-form',
      formLabel: '请假申请单',
      pageLabel: '请假发起页'
    },
    childNode: {
      ...defaultUserTask('部门经理审批', '${deptManager}'),
      properties: {
        assignmentMode: 'assignee',
        assignee: '${deptManager}',
        candidateUsers: '',
        candidateGroups: '',
        formKey: 'manager-approval',
        formLabel: '部门经理审批单',
        pageLabel: '待办审批页',
        listeners: []
      },
      childNode: {
        ...defaultUserTask('总经理审批', '${generalManager}'),
        properties: {
          assignmentMode: 'assignee',
          assignee: '${generalManager}',
          candidateUsers: '',
          candidateGroups: '',
          formKey: 'general-approval',
          formLabel: '总经理审批单',
          pageLabel: '待办审批页',
          listeners: []
        },
        childNode: {
          nodeId: createId('end'),
          nodeType: 'end',
          nodeName: '审批完成',
          properties: {}
        }
      }
    }
  })
}

export function normalizeCustomSchema(schema, meta = {}) {
  const source = clone(schema) || createDefaultCustomSchema(meta)
  return ensureNode(source)
}

export function parseStoredCustomSchema(rawValue, meta = {}) {
  if (!rawValue) {
    return createDefaultCustomSchema(meta)
  }
  try {
    return normalizeCustomSchema(JSON.parse(rawValue), meta)
  } catch (error) {
    return createDefaultCustomSchema(meta)
  }
}

function dimensionsForType(type) {
  if (type === 'start' || type === 'end') {
    return { width: 40, height: 40 }
  }
  if (type === 'gateway') {
    return { width: 56, height: 56 }
  }
  return { width: 148, height: 84 }
}

function createDiagramShape(diagram, id, type, x, y) {
  const { width, height } = dimensionsForType(type)
  diagram.bounds[id] = { x, y, width, height, centerX: x + width / 2, centerY: y + height / 2 }
  return diagram.bounds[id]
}

function anchor(bounds, side) {
  if (side === 'right') {
    return { x: bounds.x + bounds.width, y: bounds.centerY }
  }
  if (side === 'left') {
    return { x: bounds.x, y: bounds.centerY }
  }
  return { x: bounds.centerX, y: bounds.centerY }
}

function createSequenceId() {
  return createId('Flow')
}

function addSequence(diagram, sourceId, targetId, options = {}) {
  const sourceBounds = diagram.bounds[sourceId]
  const targetBounds = diagram.bounds[targetId]
  const from = anchor(sourceBounds, 'right')
  const to = anchor(targetBounds, 'left')
  const sameRow = Math.abs(from.y - to.y) < 2
  const waypoints = sameRow
    ? [from, to]
    : [from, { x: (from.x + to.x) / 2, y: from.y }, { x: (from.x + to.x) / 2, y: to.y }, to]
  const flowId = options.id || createSequenceId()
  diagram.sequenceFlows.push({
    id: flowId,
    sourceRef: sourceId,
    targetRef: targetId,
    name: options.name || '',
    conditionExpression: options.conditionExpression || '',
    isDefault: Boolean(options.isDefault),
    waypoints
  })
  return flowId
}

function createTaskExtensions(node) {
  const listeners = Array.isArray(node.properties?.listeners) ? node.properties.listeners : []
  const xml = listeners
    .filter((item) => safeText(item.listenerType) && safeText(item.listenerPath))
    .map((item) => `<flowable:taskListener event="${escapeXml(item.listenerType)}" class="${escapeXml(item.listenerPath)}" />`)
    .join('')
  return xml ? `<bpmn:extensionElements>${xml}</bpmn:extensionElements>` : ''
}

function createNodeElement(diagram, node, x, y) {
  if (node.nodeType === 'start') {
    const documentation = [safeText(node.properties?.pageLabel), safeText(node.properties?.formLabel)].filter(Boolean).join(' / ')
    const formKey = safeText(node.properties?.formKey)
    const attributes = [`id="${escapeXml(node.nodeId)}"`, `name="${escapeXml(node.nodeName)}"`]
    if (formKey) {
      attributes.push(`flowable:formKey="${escapeXml(formKey)}"`)
    }
    const content = documentation ? `<bpmn:documentation>${escapeXml(documentation)}</bpmn:documentation>` : ''
    diagram.elements.push(content
      ? `<bpmn:startEvent ${attributes.join(' ')}>${content}</bpmn:startEvent>`
      : `<bpmn:startEvent ${attributes.join(' ')} />`)
    createDiagramShape(diagram, node.nodeId, 'start', x, y)
    return node.nodeId
  }
  if (node.nodeType === 'end') {
    diagram.elements.push(`<bpmn:endEvent id="${escapeXml(node.nodeId)}" name="${escapeXml(node.nodeName)}" />`)
    createDiagramShape(diagram, node.nodeId, 'end', x, y)
    return node.nodeId
  }
  if (node.nodeType === 'between') {
    const properties = node.properties || {}
    const attributes = [`id="${escapeXml(node.nodeId)}"`, `name="${escapeXml(node.nodeName)}"`]
    if (properties.assignmentMode === 'candidateUsers' && safeText(properties.candidateUsers)) {
      attributes.push(`flowable:candidateUsers="${escapeXml(properties.candidateUsers)}"`)
    } else if (properties.assignmentMode === 'candidateGroups' && safeText(properties.candidateGroups)) {
      attributes.push(`flowable:candidateGroups="${escapeXml(properties.candidateGroups)}"`)
    } else if (safeText(properties.assignee)) {
      attributes.push(`flowable:assignee="${escapeXml(properties.assignee)}"`)
    }
    if (safeText(properties.formKey)) {
      attributes.push(`flowable:formKey="${escapeXml(properties.formKey)}"`)
    }
    const documentation = [safeText(properties.pageLabel), safeText(properties.formLabel)].filter(Boolean).join(' / ')
    const content = []
    if (documentation) {
      content.push(`<bpmn:documentation>${escapeXml(documentation)}</bpmn:documentation>`)
    }
    const extensions = createTaskExtensions(node)
    if (extensions) {
      content.push(extensions)
    }
    diagram.elements.push(content.length
      ? `<bpmn:userTask ${attributes.join(' ')}>${content.join('')}</bpmn:userTask>`
      : `<bpmn:userTask ${attributes.join(' ')} />`)
    createDiagramShape(diagram, node.nodeId, 'task', x, y)
    return node.nodeId
  }
  return ''
}

function renderNodeChain(node, x, y, diagram) {
  if (!node) {
    return { startId: '', endId: '', maxX: x, minY: y, maxY: y }
  }
  if (node.nodeType === 'serial') {
    return renderExclusiveBranch(node, x, y, diagram)
  }
  if (node.nodeType === 'parallel') {
    return renderParallelBranch(node, x, y, diagram)
  }

  const id = createNodeElement(diagram, node, x, y)
  let result = { startId: id, endId: id, maxX: x, minY: y, maxY: y }
  if (node.childNode) {
    const childResult = renderNodeChain(node.childNode, x + 220, y, diagram)
    addSequence(diagram, id, childResult.startId)
    result = {
      startId: id,
      endId: childResult.endId,
      maxX: Math.max(x, childResult.maxX),
      minY: Math.min(y, childResult.minY),
      maxY: Math.max(y, childResult.maxY)
    }
  }
  return result
}

function renderExclusiveBranch(node, x, y, diagram) {
  const splitId = `${node.nodeId}_split`
  diagram.elements.push(`<bpmn:exclusiveGateway id="${escapeXml(splitId)}" name="${escapeXml(node.nodeName || '条件分支')}" />`)
  createDiagramShape(diagram, splitId, 'gateway', x, y)

  const branches = Array.isArray(node.conditionNodes) && node.conditionNodes.length ? node.conditionNodes : createSerialConditions()
  const offset = (branches.length - 1) / 2
  const branchResults = []
  let minY = y
  let maxY = y
  let maxBranchX = x + 220

  branches.forEach((branch, index) => {
    const branchY = y + (index - offset) * 170
    const branchResult = branch.childNode
      ? renderNodeChain(branch.childNode, x + 220, branchY, diagram)
      : { startId: '', endId: '', maxX: x + 220, minY: branchY, maxY: branchY }
    branchResults.push({ branch, branchResult })
    minY = Math.min(minY, branchResult.minY)
    maxY = Math.max(maxY, branchResult.maxY)
    maxBranchX = Math.max(maxBranchX, branchResult.maxX)
  })

  const joinId = `${node.nodeId}_join`
  const joinX = maxBranchX + 220
  diagram.elements.push(`<bpmn:exclusiveGateway id="${escapeXml(joinId)}" name="${escapeXml(node.nodeName || '分支汇合')}" />`)
  createDiagramShape(diagram, joinId, 'gateway', joinX, y)

  let defaultFlowId = ''
  branchResults.forEach(({ branch, branchResult }) => {
    const flowId = addSequence(diagram, splitId, branchResult.startId || joinId, {
      name: branch.nodeName,
      conditionExpression: branch.properties?.isDefault ? '' : safeText(branch.properties?.conditionExpression),
      isDefault: Boolean(branch.properties?.isDefault)
    })
    if (branch.properties?.isDefault) {
      defaultFlowId = flowId
    }
    if (branchResult.endId) {
      addSequence(diagram, branchResult.endId, joinId)
    }
  })

  if (defaultFlowId) {
    const index = diagram.elements.findIndex((item) => item.includes(`id="${escapeXml(splitId)}"`))
    if (index >= 0) {
      diagram.elements[index] = `<bpmn:exclusiveGateway id="${escapeXml(splitId)}" name="${escapeXml(node.nodeName || '条件分支')}" default="${escapeXml(defaultFlowId)}" />`
    }
  }

  if (node.childNode) {
    const childResult = renderNodeChain(node.childNode, joinX + 220, y, diagram)
    addSequence(diagram, joinId, childResult.startId)
    return {
      startId: splitId,
      endId: childResult.endId,
      maxX: childResult.maxX,
      minY: Math.min(minY, childResult.minY),
      maxY: Math.max(maxY, childResult.maxY)
    }
  }

  return { startId: splitId, endId: joinId, maxX: joinX, minY, maxY }
}

function renderParallelBranch(node, x, y, diagram) {
  const splitId = `${node.nodeId}_split`
  diagram.elements.push(`<bpmn:parallelGateway id="${escapeXml(splitId)}" name="${escapeXml(node.nodeName || '并行分支')}" />`)
  createDiagramShape(diagram, splitId, 'gateway', x, y)

  const branches = Array.isArray(node.conditionNodes) && node.conditionNodes.length
    ? node.conditionNodes
    : [defaultUserTask('并行审批 1'), defaultUserTask('并行审批 2')]
  const offset = (branches.length - 1) / 2
  const branchResults = []
  let minY = y
  let maxY = y
  let maxBranchX = x + 220

  branches.forEach((branch, index) => {
    const branchY = y + (index - offset) * 170
    const branchResult = renderNodeChain(branch, x + 220, branchY, diagram)
    branchResults.push(branchResult)
    minY = Math.min(minY, branchResult.minY)
    maxY = Math.max(maxY, branchResult.maxY)
    maxBranchX = Math.max(maxBranchX, branchResult.maxX)
  })

  const joinId = `${node.nodeId}_join`
  const joinX = maxBranchX + 220
  diagram.elements.push(`<bpmn:parallelGateway id="${escapeXml(joinId)}" name="${escapeXml(node.nodeName || '并行汇合')}" />`)
  createDiagramShape(diagram, joinId, 'gateway', joinX, y)

  branchResults.forEach((branchResult) => {
    addSequence(diagram, splitId, branchResult.startId)
    addSequence(diagram, branchResult.endId, joinId)
  })

  if (node.childNode) {
    const childResult = renderNodeChain(node.childNode, joinX + 220, y, diagram)
    addSequence(diagram, joinId, childResult.startId)
    return {
      startId: splitId,
      endId: childResult.endId,
      maxX: childResult.maxX,
      minY: Math.min(minY, childResult.minY),
      maxY: Math.max(maxY, childResult.maxY)
    }
  }

  return { startId: splitId, endId: joinId, maxX: joinX, minY, maxY }
}

function createDiagramXml(diagram, processId, processName) {
  const sequenceLines = diagram.sequenceFlows.map((item) => {
    if (item.conditionExpression) {
      return `<bpmn:sequenceFlow id="${escapeXml(item.id)}" sourceRef="${escapeXml(item.sourceRef)}" targetRef="${escapeXml(item.targetRef)}" name="${escapeXml(item.name || '')}"><bpmn:conditionExpression xsi:type="bpmn:tFormalExpression"><![CDATA[${item.conditionExpression}]]></bpmn:conditionExpression></bpmn:sequenceFlow>`
    }
    if (item.name) {
      return `<bpmn:sequenceFlow id="${escapeXml(item.id)}" sourceRef="${escapeXml(item.sourceRef)}" targetRef="${escapeXml(item.targetRef)}" name="${escapeXml(item.name)}" />`
    }
    return `<bpmn:sequenceFlow id="${escapeXml(item.id)}" sourceRef="${escapeXml(item.sourceRef)}" targetRef="${escapeXml(item.targetRef)}" />`
  })

  const shapeLines = Object.entries(diagram.bounds).map(([id, bounds]) =>
    `<bpmndi:BPMNShape id="${escapeXml(id)}_di" bpmnElement="${escapeXml(id)}"><dc:Bounds x="${bounds.x}" y="${bounds.y}" width="${bounds.width}" height="${bounds.height}" /></bpmndi:BPMNShape>`
  )

  const edgeLines = diagram.sequenceFlows.map((item) =>
    `<bpmndi:BPMNEdge id="${escapeXml(item.id)}_di" bpmnElement="${escapeXml(item.id)}">${item.waypoints.map((point) => `<di:waypoint x="${Math.round(point.x)}" y="${Math.round(point.y)}" />`).join('')}</bpmndi:BPMNEdge>`
  )

  return `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
  xmlns:flowable="http://flowable.org/bpmn"
  id="Definitions_${escapeXml(processId)}"
  targetNamespace="http://flowable.org/processdef">
  <bpmn:process id="${escapeXml(processId)}" name="${escapeXml(processName)}" isExecutable="true">
    ${diagram.elements.join('\n    ')}
    ${sequenceLines.join('\n    ')}
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_${escapeXml(processId)}">
    <bpmndi:BPMNPlane id="BPMNPlane_${escapeXml(processId)}" bpmnElement="${escapeXml(processId)}">
      ${shapeLines.join('\n      ')}
      ${edgeLines.join('\n      ')}
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`
}

export function buildCustomBpmnXml(schema, { processId = 'process', processName = '新建流程' } = {}) {
  const normalized = normalizeCustomSchema(schema, { processName })
  const diagram = {
    elements: [],
    sequenceFlows: [],
    bounds: {}
  }
  renderNodeChain(normalized, 80, 220, diagram)
  return createDiagramXml(diagram, processId, processName)
}

export function exportSchemaJson(schema) {
  return JSON.stringify(normalizeCustomSchema(schema), null, 2)
}
