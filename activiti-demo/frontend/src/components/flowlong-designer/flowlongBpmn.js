function escapeXml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&apos;')
}

function normalizeProcessId(value) {
  return (
    String(value || 'process')
      .trim()
      .toLowerCase()
      .replace(/[^a-z0-9_]+/g, '_')
      .replace(/^_+|_+$/g, '') || 'process'
  )
}

function createFlowIdFactory() {
  let index = 1
  return () => `Flow_${index++}`
}

const BPMN_SHAPES = {
  startEvent: { width: 36, height: 36 },
  endEvent: { width: 36, height: 36 },
  task: { width: 160, height: 90 },
  serviceTask: { width: 160, height: 90 },
  gateway: { width: 60, height: 60 },
}

const LAYOUT_CONFIG = {
  mainCenterX: 720,
  startTopY: 100,
  verticalGap: 132,
  branchGapX: 260,
  branchStartOffset: 126,
}

function createNodeKey(prefix = 'flk') {
  return `${prefix}_${Math.random().toString(36).slice(2, 10)}`
}

function createConditionExpression(conditionList) {
  if (!Array.isArray(conditionList) || !conditionList.length) {
    return ''
  }
  const groups = conditionList
    .map((group) => {
      const predicates = (group || []).map((condition) => buildSinglePredicate(condition)).filter(Boolean)
      if (!predicates.length) {
        return ''
      }
      return predicates.length === 1 ? predicates[0] : `(${predicates.join(' && ')})`
    })
    .filter(Boolean)

  if (!groups.length) {
    return ''
  }
  return groups.length === 1 ? groups[0] : groups.join(' || ')
}

function buildSinglePredicate(condition) {
  const field = String(condition?.field || '').trim()
  const operator = String(condition?.operator || '').trim()
  const value = normalizeLiteral(condition?.value)
  if (!field || !operator || value == null) {
    return ''
  }

  if (operator === 'include') {
    return `${field} != null && ${field}.toString().contains(${value})`
  }
  if (operator === 'notinclude') {
    return `${field} == null || !${field}.toString().contains(${value})`
  }
  return `${field} ${operator} ${value}`
}

function normalizeLiteral(value) {
  if (value == null) {
    return null
  }
  const text = String(value).trim()
  if (!text) {
    return null
  }
  if (/^-?\d+(\.\d+)?$/.test(text)) {
    return text
  }
  if (text === 'true' || text === 'false') {
    return text
  }
  return `'${text.replaceAll("'", "\\'")}'`
}

function resolveUserTaskAttributes(node) {
  const assignees = (node?.nodeAssigneeList || []).map((item) => item?.id).filter(Boolean)
  switch (Number(node?.setType || 1)) {
    case 1:
      if (assignees.length === 1) {
        return [`activiti:assignee="${escapeXml(assignees[0])}"`]
      }
      if (assignees.length > 1) {
        return [`activiti:candidateUsers="${escapeXml(assignees.join(','))}"`]
      }
      return []
    case 2:
      return [
        `activiti:assignee="${escapeXml(Number(node?.examineLevel || 1) > 1 ? '${generalManager}' : '${deptManager}')}"`,
      ]
    case 3:
      if (assignees.length > 0) {
        return [`activiti:candidateGroups="${escapeXml(assignees.join(','))}"`]
      }
      return []
    case 5:
      return ['activiti:assignee="${applicant}"']
    case 7:
      return [
        `activiti:assignee="${escapeXml(Number(node?.directorLevel || 1) > 1 ? '${generalManager}' : '${deptManager}')}"`,
      ]
    default:
      return []
  }
}

function buildTaskNodeXml(node) {
  const attrs = resolveUserTaskAttributes(node)
  return `<userTask id="${escapeXml(node.nodeKey)}" name="${escapeXml(node.nodeName || '审批节点')}"${attrs.length ? ` ${attrs.join(' ')}` : ''} />`
}

function buildSendNodeXml(node) {
  return `<serviceTask id="${escapeXml(node.nodeKey)}" name="${escapeXml(node.nodeName || '抄送节点')}" activiti:delegateExpression="\${copySendService}" />`
}

function buildSequenceFlowXml(flowId, sourceRef, targetRef, conditionExpression) {
  if (!conditionExpression) {
    return `<sequenceFlow id="${flowId}" sourceRef="${escapeXml(sourceRef)}" targetRef="${escapeXml(targetRef)}" />`
  }
  const body = `<conditionExpression xsi:type="tFormalExpression"><![CDATA[\${${conditionExpression}}]]></conditionExpression>`
  return `<sequenceFlow id="${flowId}" sourceRef="${escapeXml(sourceRef)}" targetRef="${escapeXml(targetRef)}">${body}</sequenceFlow>`
}

function createPromoterNode() {
  return {
    nodeName: '发起人',
    nodeKey: 'start_promoter',
    type: 0,
    nodeAssigneeList: [],
    childNode: null,
  }
}

function createApproverNode(source = {}) {
  return {
    nodeName: source.nodeName || '审批节点',
    nodeKey: source.nodeKey || createNodeKey('approver'),
    type: 1,
    setType: source.setType || 1,
    nodeAssigneeList: Array.isArray(source.nodeAssigneeList) ? source.nodeAssigneeList : [],
    examineLevel: source.examineLevel || 1,
    directorLevel: source.directorLevel || 1,
    selectMode: source.selectMode || 1,
    termAuto: Boolean(source.termAuto),
    term: source.term || 0,
    termMode: source.termMode || 1,
    examineMode: source.examineMode || 1,
    directorMode: source.directorMode || 0,
    childNode: source.childNode || null,
  }
}

function createSendNode(source = {}) {
  return {
    nodeName: source.nodeName || '抄送节点',
    nodeKey: source.nodeKey || createNodeKey('send'),
    type: 2,
    userSelectFlag: source.userSelectFlag !== false,
    nodeAssigneeList: Array.isArray(source.nodeAssigneeList) ? source.nodeAssigneeList : [],
    childNode: source.childNode || null,
  }
}

function createBranchNode(source = {}) {
  const conditionNodes = Array.isArray(source.conditionNodes) && source.conditionNodes.length
    ? source.conditionNodes
    : [
        createConditionBranch(1),
        createConditionBranch(2),
      ]
  return {
    nodeName: source.nodeName || '条件路由',
    nodeKey: source.nodeKey || createNodeKey('branch'),
    type: 4,
    conditionNodes,
    childNode: source.childNode || null,
  }
}

function createConditionBranch(index, overrides = {}) {
  return {
    nodeName: overrides.nodeName || `条件${index}`,
    nodeKey: overrides.nodeKey || createNodeKey('condition'),
    type: 3,
    priorityLevel: overrides.priorityLevel || index,
    conditionMode: 1,
    conditionList: Array.isArray(overrides.conditionList) ? overrides.conditionList : [],
    childNode: overrides.childNode || null,
  }
}

export function createDefaultFlowDefinition(meta = {}) {
  return {
    id: Date.now(),
    key: normalizeProcessId(meta.processId),
    name: meta.processName || '新建流程',
    nodeConfig: createPromoterNode(),
  }
}

export function normalizeFlowDefinition(raw, meta = {}) {
  if (raw && raw.nodeConfig) {
    return {
      ...raw,
      key: normalizeProcessId(raw.key || meta.processId),
      name: raw.name || meta.processName || '新建流程',
      nodeConfig: normalizeNodeTree(raw.nodeConfig, true),
    }
  }
  return createDefaultFlowDefinition(meta)
}

function normalizeNodeTree(node, isRoot = false) {
  if (!node) {
    return isRoot ? createPromoterNode() : null
  }
  const type = Number(node.type || 0)
  if (type === 0) {
    return {
      ...createPromoterNode(),
      ...node,
      nodeKey: node.nodeKey || 'start_promoter',
      childNode: normalizeNodeTree(node.childNode),
    }
  }
  if (type === 1) {
    return createApproverNode({
      ...node,
      childNode: normalizeNodeTree(node.childNode),
    })
  }
  if (type === 2) {
    return createSendNode({
      ...node,
      childNode: normalizeNodeTree(node.childNode),
    })
  }
  if (type === 4) {
    return createBranchNode({
      ...node,
      conditionNodes: (node.conditionNodes || []).map((item, index) =>
        createConditionBranch(index + 1, {
          ...item,
          childNode: normalizeNodeTree(item.childNode),
        })
      ),
      childNode: normalizeNodeTree(node.childNode),
    })
  }
  return null
}

export function buildFlowlongBpmn(definition, meta = {}) {
  const processId = normalizeProcessId(meta.processId || definition?.key)
  const processName = definition?.name || meta.processName || '新建流程'
  const flowId = createFlowIdFactory()
  const elements = []
  const sequenceFlows = []
  const shapeBounds = new Map()

  function placeShape(id, kind, centerX, topY) {
    const size = BPMN_SHAPES[kind] || BPMN_SHAPES.task
    shapeBounds.set(id, {
      id,
      x: Math.round(centerX - size.width / 2),
      y: Math.round(topY),
      width: size.width,
      height: size.height,
    })
    return shapeBounds.get(id)
  }

  function addSequenceFlow(id, sourceRef, targetRef, conditionExpression = '') {
    sequenceFlows.push({
      id,
      sourceRef,
      targetRef,
      conditionExpression,
    })
  }

  function connectSources(sources, targetId) {
    sources.forEach((source) => {
      addSequenceFlow(source.flowId || flowId(), source.sourceRef, targetId, source.conditionExpression)
    })
  }

  function walk(node, sources, layout) {
    if (!node) {
      return {
        sources,
        nextY: layout.topY,
      }
    }

    if (node.type === 1) {
      elements.push(buildTaskNodeXml(node))
      const bounds = placeShape(node.nodeKey, 'task', layout.centerX, layout.topY)
      connectSources(sources, node.nodeKey)
      return walk(node.childNode, [{ sourceRef: node.nodeKey }], {
        centerX: layout.centerX,
        topY: bounds.y + bounds.height + LAYOUT_CONFIG.verticalGap,
      })
    }

    if (node.type === 2) {
      elements.push(buildSendNodeXml(node))
      const bounds = placeShape(node.nodeKey, 'serviceTask', layout.centerX, layout.topY)
      connectSources(sources, node.nodeKey)
      return walk(node.childNode, [{ sourceRef: node.nodeKey }], {
        centerX: layout.centerX,
        topY: bounds.y + bounds.height + LAYOUT_CONFIG.verticalGap,
      })
    }

    if (node.type === 4) {
      const gatewayId = node.nodeKey
      const mergeId = `${node.nodeKey}_merge`
      const defaultBranch = findDefaultBranch(node.conditionNodes)
      const defaultFlowId = defaultBranch ? flowId() : ''
      const defaultAttr = defaultFlowId ? ` default="${escapeXml(defaultFlowId)}"` : ''
      elements.push(`<exclusiveGateway id="${escapeXml(gatewayId)}" name="${escapeXml(node.nodeName || '条件路由')}"${defaultAttr} />`)
      elements.push(`<exclusiveGateway id="${escapeXml(mergeId)}" name="${escapeXml((node.nodeName || '条件路由') + '汇聚')}" />`)
      const gatewayBounds = placeShape(gatewayId, 'gateway', layout.centerX, layout.topY)
      connectSources(sources, gatewayId)

      const branchOutputs = []
      const branchNextY = []
      const branchCount = Math.max(1, (node.conditionNodes || []).length)
      const branchStartY = gatewayBounds.y + gatewayBounds.height + LAYOUT_CONFIG.branchStartOffset
      ;(node.conditionNodes || []).forEach((conditionNode) => {
        const conditionExpression = createConditionExpression(conditionNode.conditionList)
        const branchIndex = Number(conditionNode?.priorityLevel || 1) - 1
        const centerOffset = branchIndex - (branchCount - 1) / 2
        const branchCenterX = layout.centerX + centerOffset * LAYOUT_CONFIG.branchGapX
        if (conditionNode.childNode) {
          const outputs = walk(conditionNode.childNode, [
            {
              sourceRef: gatewayId,
              conditionExpression,
              flowId: defaultBranch?.nodeKey === conditionNode.nodeKey && !conditionExpression ? defaultFlowId : undefined,
            },
          ], {
            centerX: branchCenterX,
            topY: branchStartY,
          })
          branchOutputs.push(...outputs.sources)
          branchNextY.push(outputs.nextY)
        } else {
          const targetFlowId = defaultBranch?.nodeKey === conditionNode.nodeKey && !conditionExpression ? defaultFlowId : flowId()
          addSequenceFlow(targetFlowId, gatewayId, mergeId, conditionExpression)
          branchNextY.push(branchStartY + LAYOUT_CONFIG.verticalGap)
        }
      })

      const mergeTopY = Math.max(
        branchStartY + LAYOUT_CONFIG.verticalGap,
        ...branchNextY.map((value) => value - Math.round(LAYOUT_CONFIG.verticalGap * 0.38))
      )
      const mergeBounds = placeShape(mergeId, 'gateway', layout.centerX, mergeTopY)

      branchOutputs.forEach((output) => {
        addSequenceFlow(flowId(), output.sourceRef, mergeId)
      })
      return walk(node.childNode, [{ sourceRef: mergeId }], {
        centerX: layout.centerX,
        topY: mergeBounds.y + mergeBounds.height + LAYOUT_CONFIG.verticalGap,
      })
    }

    return walk(node.childNode, sources, layout)
  }

  elements.push('<startEvent id="StartEvent_1" name="开始" />')
  const startBounds = placeShape('StartEvent_1', 'startEvent', LAYOUT_CONFIG.mainCenterX, LAYOUT_CONFIG.startTopY)
  const tailSources = walk(definition?.nodeConfig?.childNode || null, [{ sourceRef: 'StartEvent_1' }], {
    centerX: LAYOUT_CONFIG.mainCenterX,
    topY: startBounds.y + startBounds.height + LAYOUT_CONFIG.verticalGap,
  })
  elements.push('<endEvent id="EndEvent_1" name="结束" />')
  const endBounds = placeShape('EndEvent_1', 'endEvent', LAYOUT_CONFIG.mainCenterX, tailSources.nextY)
  tailSources.sources.forEach((source) => {
    addSequenceFlow(flowId(), source.sourceRef, 'EndEvent_1')
  })

  const body = [
    ...elements,
    ...sequenceFlows.map((item) => buildSequenceFlowXml(item.id, item.sourceRef, item.targetRef, item.conditionExpression)),
  ].join('\n    ')
  const diagramXml = buildDiagramXml(processId, shapeBounds, sequenceFlows, startBounds, endBounds)

  return `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
  xmlns:activiti="http://activiti.org/bpmn"
  targetNamespace="http://www.activiti.org/processdef">
  <process id="${escapeXml(processId)}" name="${escapeXml(processName)}" isExecutable="true">
    ${body}
  </process>
  ${diagramXml}
</definitions>`
}

function buildDiagramXml(processId, shapeBounds, sequenceFlows) {
  const shapes = Array.from(shapeBounds.values()).map((bounds) => {
    return `<bpmndi:BPMNShape id="${escapeXml(bounds.id)}_di" bpmnElement="${escapeXml(bounds.id)}">
      <dc:Bounds x="${bounds.x}" y="${bounds.y}" width="${bounds.width}" height="${bounds.height}" />
    </bpmndi:BPMNShape>`
  })

  const edges = sequenceFlows
    .map((flow) => {
      const waypoints = buildWaypoints(shapeBounds.get(flow.sourceRef), shapeBounds.get(flow.targetRef))
      if (!waypoints.length) {
        return ''
      }
      const points = waypoints
        .map((point) => `<di:waypoint x="${point.x}" y="${point.y}" />`)
        .join('\n      ')
      return `<bpmndi:BPMNEdge id="${escapeXml(flow.id)}_di" bpmnElement="${escapeXml(flow.id)}">
      ${points}
    </bpmndi:BPMNEdge>`
    })
    .filter(Boolean)

  return `<bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="${escapeXml(processId)}">
      ${[...shapes, ...edges].join('\n      ')}
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>`
}

function buildWaypoints(source, target) {
  if (!source || !target) {
    return []
  }
  const sourceX = source.x + source.width / 2
  const sourceY = source.y + source.height
  const targetX = target.x + target.width / 2
  const targetY = target.y

  if (Math.abs(sourceX - targetX) < 2) {
    return [
      { x: Math.round(sourceX), y: Math.round(sourceY) },
      { x: Math.round(targetX), y: Math.round(targetY) },
    ]
  }

  const middleY = Math.round(sourceY + Math.max(36, (targetY - sourceY) / 2))
  return [
    { x: Math.round(sourceX), y: Math.round(sourceY) },
    { x: Math.round(sourceX), y: middleY },
    { x: Math.round(targetX), y: middleY },
    { x: Math.round(targetX), y: Math.round(targetY) },
  ]
}

function findDefaultBranch(conditionNodes) {
  const items = conditionNodes || []
  for (let index = items.length - 1; index >= 0; index -= 1) {
    const item = items[index]
    if (!Array.isArray(item?.conditionList) || !item.conditionList.length) {
      return item
    }
  }
  return null
}

function localName(element) {
  return element?.localName || element?.nodeName?.split(':').pop() || ''
}

function parseXml(xml) {
  const parser = new DOMParser()
  const doc = parser.parseFromString(xml, 'application/xml')
  const errorNode = doc.querySelector('parsererror')
  if (errorNode) {
    throw new Error('XML 解析失败，文件格式不正确')
  }
  return doc
}

function getChildTextByLocalName(element, name) {
  const child = Array.from(element?.children || []).find((item) => localName(item) === name)
  return child?.textContent?.trim() || ''
}

function collectElements(processElement) {
  const nodes = new Map()
  const outgoing = new Map()
  const incoming = new Map()
  const flows = new Map()

  Array.from(processElement.children || []).forEach((element) => {
    const type = localName(element)
    const id = element.getAttribute('id')
    if (!id) {
      return
    }
    if (type === 'sequenceFlow') {
      const flow = {
        id,
        sourceRef: element.getAttribute('sourceRef') || '',
        targetRef: element.getAttribute('targetRef') || '',
        conditionExpression: getChildTextByLocalName(element, 'conditionExpression'),
      }
      flows.set(id, flow)
      if (!outgoing.has(flow.sourceRef)) {
        outgoing.set(flow.sourceRef, [])
      }
      if (!incoming.has(flow.targetRef)) {
        incoming.set(flow.targetRef, [])
      }
      outgoing.get(flow.sourceRef).push(flow)
      incoming.get(flow.targetRef).push(flow)
      return
    }
    nodes.set(id, {
      id,
      type,
      element,
      name: element.getAttribute('name') || '',
      defaultFlow: element.getAttribute('default') || '',
    })
  })

  return { nodes, outgoing, incoming, flows }
}

function unwrapExpression(expression) {
  const text = String(expression || '').trim()
  const match = text.match(/^\$\{([\s\S]+)\}$/)
  return (match ? match[1] : text).trim()
}

function stripOuterParens(expression) {
  let text = String(expression || '').trim()
  while (text.startsWith('(') && text.endsWith(')')) {
    let depth = 0
    let balanced = true
    for (let index = 0; index < text.length; index += 1) {
      const char = text[index]
      if (char === '(') {
        depth += 1
      } else if (char === ')') {
        depth -= 1
        if (depth === 0 && index !== text.length - 1) {
          balanced = false
          break
        }
      }
    }
    if (!balanced) {
      break
    }
    text = text.slice(1, -1).trim()
  }
  return text
}

function protectCompositePredicates(expression) {
  const tokens = []
  const patterns = [
    /([A-Za-z_][\w.]*)\s*!=\s*null\s*&&\s*\1\.toString\(\)\.contains\(([^()]+)\)/g,
    /([A-Za-z_][\w.]*)\s*==\s*null\s*\|\|\s*!\1\.toString\(\)\.contains\(([^()]+)\)/g,
  ]
  let result = expression
  patterns.forEach((pattern) => {
    result = result.replace(pattern, (match) => {
      const token = `__PRED_${tokens.length}__`
      tokens.push(match)
      return token
    })
  })
  return { expression: result, tokens }
}

function restoreCompositePredicate(expression, tokens) {
  let result = expression
  tokens.forEach((tokenValue, index) => {
    result = result.replaceAll(`__PRED_${index}__`, tokenValue)
  })
  return result
}

function splitTopLevel(expression, operator) {
  const result = []
  let current = ''
  let depth = 0
  let quote = ''
  for (let index = 0; index < expression.length; index += 1) {
    const char = expression[index]
    const next = expression[index + 1]
    if (quote) {
      current += char
      if (char === quote && expression[index - 1] !== '\\') {
        quote = ''
      }
      continue
    }
    if (char === "'" || char === '"') {
      quote = char
      current += char
      continue
    }
    if (char === '(') {
      depth += 1
      current += char
      continue
    }
    if (char === ')') {
      depth = Math.max(0, depth - 1)
      current += char
      continue
    }
    if (depth === 0 && char === operator[0] && next === operator[1]) {
      result.push(current.trim())
      current = ''
      index += 1
      continue
    }
    current += char
  }
  if (current.trim()) {
    result.push(current.trim())
  }
  return result
}

function unquoteLiteral(value) {
  const text = String(value || '').trim()
  if ((text.startsWith("'") && text.endsWith("'")) || (text.startsWith('"') && text.endsWith('"'))) {
    return text.slice(1, -1).replaceAll("\\'", "'").replaceAll('\\"', '"')
  }
  return text
}

function parsePredicate(text) {
  const expression = stripOuterParens(String(text || '').trim())
  let match = expression.match(/^([A-Za-z_][\w.]*)\s*!=\s*null\s*&&\s*\1\.toString\(\)\.contains\((.+)\)$/)
  if (match) {
    return {
      label: match[1],
      field: match[1],
      operator: 'include',
      value: unquoteLiteral(match[2]),
    }
  }
  match = expression.match(/^([A-Za-z_][\w.]*)\s*==\s*null\s*\|\|\s*!\1\.toString\(\)\.contains\((.+)\)$/)
  if (match) {
    return {
      label: match[1],
      field: match[1],
      operator: 'notinclude',
      value: unquoteLiteral(match[2]),
    }
  }
  match = expression.match(/^([A-Za-z_][\w.]*)\s*(==|!=|>=|<=|>|<)\s*(.+)$/)
  if (match) {
    return {
      label: match[1],
      field: match[1],
      operator: match[2],
      value: unquoteLiteral(match[3]),
    }
  }
  return null
}

function parseConditionExpression(expression) {
  const raw = unwrapExpression(expression)
  if (!raw) {
    return []
  }
  const protectedExpression = protectCompositePredicates(raw)
  const groups = splitTopLevel(protectedExpression.expression, '||')
    .map((group) => restoreCompositePredicate(stripOuterParens(group), protectedExpression.tokens))
    .map((group) => {
      const protectedGroup = protectCompositePredicates(group)
      return splitTopLevel(protectedGroup.expression, '&&')
        .map((item) => restoreCompositePredicate(stripOuterParens(item), protectedGroup.tokens))
        .map((item) => parsePredicate(item))
        .filter(Boolean)
    })
    .filter((group) => group.length)
  return groups
}

function resolveAssigneeList(value) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => ({ id: item, name: item }))
}

function getActivitiAttr(element, name) {
  return (
    element.getAttribute(`activiti:${name}`) ||
    element.getAttributeNS('http://activiti.org/bpmn', name) ||
    ''
  )
}

function parseUserTaskNode(node) {
  const assignee = getActivitiAttr(node.element, 'assignee')
  const candidateUsers = getActivitiAttr(node.element, 'candidateUsers')
  const candidateGroups = getActivitiAttr(node.element, 'candidateGroups')

  if (assignee === '${applicant}') {
    return createApproverNode({
      nodeName: node.name || '发起人自己',
      nodeKey: node.id,
      setType: 5,
    })
  }
  if (assignee === '${deptManager}') {
    return createApproverNode({
      nodeName: node.name || '主管审批',
      nodeKey: node.id,
      setType: 2,
      examineLevel: 1,
    })
  }
  if (assignee === '${generalManager}') {
    return createApproverNode({
      nodeName: node.name || '主管审批',
      nodeKey: node.id,
      setType: 2,
      examineLevel: 2,
    })
  }
  if (candidateGroups) {
    return createApproverNode({
      nodeName: node.name || '角色审批',
      nodeKey: node.id,
      setType: 3,
      nodeAssigneeList: resolveAssigneeList(candidateGroups),
    })
  }
  if (candidateUsers) {
    return createApproverNode({
      nodeName: node.name || '审批节点',
      nodeKey: node.id,
      setType: 1,
      nodeAssigneeList: resolveAssigneeList(candidateUsers),
    })
  }
  return createApproverNode({
    nodeName: node.name || '审批节点',
    nodeKey: node.id,
    setType: 1,
    nodeAssigneeList: resolveAssigneeList(assignee),
  })
}

function isCopySendTask(node) {
  return getActivitiAttr(node.element, 'class') === 'com.example.activiti.service.CopySendService'
}

function parseServiceTaskNode(node) {
  if (!isCopySendTask(node)) {
    return null
  }
  return createSendNode({
    nodeName: node.name || '抄送节点',
    nodeKey: node.id,
  })
}

function collectReachableIds(startId, outgoing, seen = new Set()) {
  if (!startId || seen.has(startId)) {
    return seen
  }
  seen.add(startId)
  const flows = outgoing.get(startId) || []
  flows.forEach((flow) => {
    collectReachableIds(flow.targetRef, outgoing, seen)
  })
  return seen
}

function findMergeNodeId(flows, outgoing, nodes) {
  if (!flows.length) {
    return ''
  }
  const reachableSets = flows.map((flow) => collectReachableIds(flow.targetRef, outgoing, new Set()))
  const [firstSet, ...restSets] = reachableSets
  const candidates = Array.from(firstSet).filter((id) => restSets.every((set) => set.has(id)))
  if (!candidates.length) {
    return ''
  }
  const preferred = candidates.find((id) => {
    const node = nodes.get(id)
    return node?.type === 'exclusiveGateway' && (outgoing.get(id) || []).length <= 1
  })
  return preferred || candidates[0]
}

function parseGatewayNode(node, context, trail = new Set()) {
  const branchFlows = context.outgoing.get(node.id) || []
  if (!branchFlows.length) {
    return null
  }
  const mergeId = findMergeNodeId(branchFlows, context.outgoing, context.nodes)
  const branchNode = createBranchNode({
    nodeName: node.name || '条件路由',
    nodeKey: node.id,
    conditionNodes: branchFlows.map((flow, index) =>
      createConditionBranch(index + 1, {
        nodeName: `条件${index + 1}`,
        nodeKey: `${flow.id || node.id}_${index + 1}`,
        priorityLevel: index + 1,
        conditionList: parseConditionExpression(flow.conditionExpression),
        childNode: buildNodeFromId(flow.targetRef, context, new Set([...trail, node.id]), new Set([mergeId])),
      })
    ),
    childNode: mergeId ? buildNextFromSource(mergeId, context, new Set([...trail, node.id, mergeId]), new Set()) : null,
  })

  const defaultBranch = branchNode.conditionNodes.find((item) => !item.conditionList.length)
  if (!defaultBranch && branchNode.conditionNodes.length) {
    branchNode.conditionNodes[branchNode.conditionNodes.length - 1].conditionList = []
  }
  return branchNode
}

function buildNextFromSource(sourceId, context, trail, stopIds) {
  const flows = context.outgoing.get(sourceId) || []
  const nextFlow = flows.find((flow) => !stopIds.has(flow.targetRef))
  if (!nextFlow) {
    return null
  }
  return buildNodeFromId(nextFlow.targetRef, context, trail, stopIds)
}

function buildNodeFromId(nodeId, context, trail = new Set(), stopIds = new Set()) {
  if (!nodeId || stopIds.has(nodeId) || trail.has(nodeId)) {
    return null
  }
  const node = context.nodes.get(nodeId)
  if (!node) {
    return null
  }

  const nextTrail = new Set(trail)
  nextTrail.add(nodeId)

  if (node.type === 'endEvent') {
    return null
  }
  if (node.type === 'startEvent') {
    return buildNextFromSource(nodeId, context, nextTrail, stopIds)
  }
  if (node.type === 'userTask') {
    const approver = parseUserTaskNode(node)
    approver.childNode = buildNextFromSource(nodeId, context, nextTrail, stopIds)
    return approver
  }
  if (node.type === 'serviceTask') {
    const sendNode = parseServiceTaskNode(node)
    if (!sendNode) {
      return buildNextFromSource(nodeId, context, nextTrail, stopIds)
    }
    sendNode.childNode = buildNextFromSource(nodeId, context, nextTrail, stopIds)
    return sendNode
  }
  if (node.type === 'exclusiveGateway') {
    const outgoingFlows = context.outgoing.get(nodeId) || []
    if (outgoingFlows.length <= 1) {
      return buildNextFromSource(nodeId, context, nextTrail, stopIds)
    }
    return parseGatewayNode(node, context, nextTrail)
  }
  return buildNextFromSource(nodeId, context, nextTrail, stopIds)
}

export function parseBpmnToFlowDefinition(xml, meta = {}) {
  const doc = parseXml(xml)
  const processElement = Array.from(doc.documentElement.children || []).find((item) => localName(item) === 'process')
  if (!processElement) {
    throw new Error('未找到 BPMN process 定义')
  }

  const context = collectElements(processElement)
  const startNode = Array.from(context.nodes.values()).find((item) => item.type === 'startEvent')
  if (!startNode) {
    throw new Error('流程缺少开始节点，无法转换')
  }

  const definition = createDefaultFlowDefinition({
    processId: processElement.getAttribute('id') || meta.processId,
    processName: processElement.getAttribute('name') || meta.processName,
  })
  const promoter = createPromoterNode()
  promoter.childNode = buildNextFromSource(startNode.id, context, new Set([startNode.id]), new Set())
  definition.nodeConfig = promoter
  return normalizeFlowDefinition(definition, meta)
}
