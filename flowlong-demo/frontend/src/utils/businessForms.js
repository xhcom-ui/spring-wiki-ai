import { reactive } from 'vue'
import { http } from '../api/http'

const BASE_FORM_REGISTRY = {
  'leave-form': {
    key: 'leave-form',
    title: '请假申请单',
    description: '发起请假流程时填写完整的申请信息和审批链路。',
    fields: [
      {
        key: 'applicant',
        label: '申请人',
        type: 'text',
        readonlyWhen: 'always',
        componentProps: {
          autocomplete: 'off'
        },
        help: '当前登录用户会自动作为流程发起人。',
        validator: [{ type: 'required', message: '申请人不能为空' }]
      },
      {
        key: 'days',
        label: '请假天数',
        type: 'number',
        min: 1,
        max: 30,
        componentProps: {
          step: 1,
          inputmode: 'numeric'
        },
        placeholder: '请输入 1-30 的整数天数',
        help: '建议与开始/结束时间保持一致。',
        validator: [
          { type: 'required', message: '请假天数不能为空' },
          { type: 'positiveInteger', message: '请假天数必须为正整数' },
          { type: 'max', value: 30, message: '请假天数不能超过 30 天' }
        ]
      },
      {
        key: 'startDate',
        label: '开始时间',
        type: 'datetime-local',
        componentProps: {
          step: 60
        },
        help: '请选择实际请假开始时间。',
        validator: [{ type: 'required', message: '开始时间不能为空' }]
      },
      {
        key: 'endDate',
        label: '结束时间',
        type: 'datetime-local',
        componentProps: {
          step: 60
        },
        help: '结束时间应晚于开始时间。',
        validator: [
          { type: 'required', message: '结束时间不能为空' },
          { type: 'afterField', field: 'startDate', message: '结束时间必须晚于开始时间' }
        ]
      },
      {
        key: 'deptManager',
        label: '部门经理',
        type: 'select',
        optionsKey: 'approvers',
        optionsApi: '/users/lookup',
        help: '请选择第一审批节点办理人。',
        validator: [{ type: 'required', message: '部门经理不能为空' }]
      },
      {
        key: 'generalManager',
        label: '总经理',
        type: 'select',
        optionsKey: 'approvers',
        optionsApi: '/users/lookup',
        help: '请选择第二审批节点办理人。',
        validator: [{ type: 'required', message: '总经理不能为空' }]
      },
      {
        key: 'reason',
        label: '请假原因',
        type: 'textarea',
        rows: 5,
        span: 2,
        componentProps: {
          maxlength: 300
        },
        placeholder: '请输入请假原因',
        help: '原因越清晰，审批人越容易快速处理。',
        validator: [
          { type: 'required', message: '请假原因不能为空' },
          { type: 'minLength', value: 4, message: '请假原因至少填写 4 个字符' }
        ]
      }
    ]
  },
  'manager-approval': {
    key: 'manager-approval',
    title: '部门经理审批单',
    description: '请确认请假时间、原因与当前团队安排，再给出审批意见。',
    summaryFields: [
      { key: 'applicant', label: '申请人' },
      { key: 'days', label: '请假天数' },
      { key: 'startDate', label: '开始时间', formatter: 'datetime' },
      { key: 'endDate', label: '结束时间', formatter: 'datetime' },
      { key: 'reason', label: '请假原因', span: 2 },
      { key: 'deptManager', label: '部门经理' },
      { key: 'generalManager', label: '总经理' }
    ],
    decisionFields: [
      {
        key: 'approvalComment',
        label: '审批意见',
        type: 'textarea',
        rows: 5,
        span: 2,
        componentProps: {
          maxlength: 300
        },
        placeholder: '请输入同意审批的说明或补充意见',
        help: '同意时建议说明审批判断依据。',
        outcome: 'approved',
        validator: [{ type: 'required', message: '审批意见不能为空' }]
      },
      {
        key: 'rejectReason',
        label: '退回原因',
        type: 'textarea',
        rows: 5,
        span: 2,
        componentProps: {
          maxlength: 300
        },
        placeholder: '请输入驳回原因，便于申请人补充或修正',
        help: '驳回时建议明确说明退回原因。',
        outcome: 'rejected',
        validator: [{ type: 'required', message: '退回原因不能为空' }]
      },
      {
        key: 'systemRemark',
        label: '系统备注',
        type: 'textarea',
        rows: 3,
        span: 2,
        componentProps: {
          maxlength: 200
        },
        placeholder: '记录额外上下文、交接说明或系统备注',
        help: '这部分会和审批意见一起进入节点审计日志。'
      }
    ]
  },
  'general-approval': {
    key: 'general-approval',
    title: '总经理审批单',
    description: '请结合部门经理意见和业务影响，完成最终审批。',
    summaryFields: [
      { key: 'applicant', label: '申请人' },
      { key: 'days', label: '请假天数' },
      { key: 'startDate', label: '开始时间', formatter: 'datetime' },
      { key: 'endDate', label: '结束时间', formatter: 'datetime' },
      { key: 'reason', label: '请假原因', span: 2 },
      { key: 'deptManager', label: '部门经理' },
      { key: 'generalManager', label: '总经理' }
    ],
    decisionFields: [
      {
        key: 'approvalComment',
        label: '审批意见',
        type: 'textarea',
        rows: 5,
        span: 2,
        componentProps: {
          maxlength: 300
        },
        placeholder: '请输入最终审批意见',
        help: '同意时建议补充最终审批结论。',
        outcome: 'approved',
        validator: [{ type: 'required', message: '审批意见不能为空' }]
      },
      {
        key: 'rejectReason',
        label: '退回原因',
        type: 'textarea',
        rows: 5,
        span: 2,
        componentProps: {
          maxlength: 300
        },
        placeholder: '请输入驳回原因，便于申请人补充或修正',
        help: '驳回时建议明确说明退回原因。',
        outcome: 'rejected',
        validator: [{ type: 'required', message: '退回原因不能为空' }]
      },
      {
        key: 'systemRemark',
        label: '系统备注',
        type: 'textarea',
        rows: 3,
        span: 2,
        componentProps: {
          maxlength: 200
        },
        placeholder: '记录额外上下文、交接说明或系统备注',
        help: '这部分会和审批意见一起进入节点审计日志。'
      }
    ]
  },
  default: {
    key: 'default',
    title: '业务表单',
    description: '当前节点没有绑定专门的业务表单，展示标准审批摘要。',
    summaryFields: [
      { key: 'applicant', label: '申请人' },
      { key: 'businessKey', label: '业务键' },
      { key: 'processInstanceId', label: '流程实例' },
      { key: 'reason', label: '业务说明', span: 2 }
    ],
    decisionFields: [
      {
        key: 'approvalComment',
        label: '审批意见',
        type: 'textarea',
        rows: 5,
        span: 2,
        placeholder: '请输入审批说明',
        outcome: 'approved'
      },
      {
        key: 'rejectReason',
        label: '退回原因',
        type: 'textarea',
        rows: 5,
        span: 2,
        placeholder: '请输入退回原因',
        outcome: 'rejected'
      },
      {
        key: 'systemRemark',
        label: '系统备注',
        type: 'textarea',
        rows: 3,
        span: 2,
        placeholder: '记录系统备注'
      }
    ]
  }
}

const REMOTE_FORM_REGISTRY = reactive({})
const OPTION_SOURCE_CACHE = reactive({})
let remoteRegistryLoaded = false
let remoteRegistryPromise = null
const optionRequestCache = new Map()

function normalizeFormConfig(formKey, config = {}) {
  return {
    ...config,
    key: config.key || formKey || 'default',
    title: config.title || BASE_FORM_REGISTRY[formKey]?.title || '业务表单',
    description: config.description || BASE_FORM_REGISTRY[formKey]?.description || ''
  }
}

function resetRemoteRegistry() {
  Object.keys(REMOTE_FORM_REGISTRY).forEach((key) => {
    delete REMOTE_FORM_REGISTRY[key]
  })
}

function parseRemoteSchema(catalog) {
  if (catalog?.schema && typeof catalog.schema === 'object') {
    return catalog.schema
  }
  if (catalog?.schemaJson) {
    try {
      return JSON.parse(catalog.schemaJson)
    } catch (error) {
      return {}
    }
  }
  return {}
}

export function syncRemoteFormRegistry(catalogs = []) {
  resetRemoteRegistry()
  for (const catalog of catalogs || []) {
    const formKey = catalog?.formKey || catalog?.key
    if (!formKey) {
      continue
    }
    REMOTE_FORM_REGISTRY[formKey] = normalizeFormConfig(formKey, parseRemoteSchema(catalog))
  }
  remoteRegistryLoaded = true
}

export async function ensureRemoteFormRegistryLoaded(force = false) {
  if (remoteRegistryLoaded && !force) {
    return REMOTE_FORM_REGISTRY
  }
  if (remoteRegistryPromise && !force) {
    return remoteRegistryPromise
  }
  remoteRegistryPromise = http.get('/form-catalogs/runtime')
    .then(({ data }) => {
      syncRemoteFormRegistry(Array.isArray(data) ? data : [])
      return REMOTE_FORM_REGISTRY
    })
    .finally(() => {
      remoteRegistryPromise = null
    })
  return remoteRegistryPromise
}

export function getAvailableFormCatalogs() {
  const merged = new Map()
  Object.entries(BASE_FORM_REGISTRY).forEach(([key, value]) => {
    merged.set(key, normalizeFormConfig(key, value))
  })
  Object.entries(REMOTE_FORM_REGISTRY).forEach(([key, value]) => {
    merged.set(key, normalizeFormConfig(key, value))
  })
  return [...merged.values()]
    .filter((item) => item.key !== 'default')
    .sort((left, right) => left.title.localeCompare(right.title, 'zh-CN'))
}

export function getCachedOptionSource(sourceKey) {
  return Array.isArray(OPTION_SOURCE_CACHE[sourceKey]) ? OPTION_SOURCE_CACHE[sourceKey] : []
}

export async function ensureFieldOptionsLoaded(field, force = false) {
  const sourceKey = field?.optionsApi
  if (!sourceKey) {
    return []
  }
  if (!force && Array.isArray(OPTION_SOURCE_CACHE[sourceKey])) {
    return OPTION_SOURCE_CACHE[sourceKey]
  }
  if (!force && optionRequestCache.has(sourceKey)) {
    return optionRequestCache.get(sourceKey)
  }
  const request = http.get(sourceKey)
    .then(({ data }) => {
      const source = Array.isArray(data) ? data : Array.isArray(data?.items) ? data.items : []
      OPTION_SOURCE_CACHE[sourceKey] = source
      return source
    })
    .finally(() => {
      optionRequestCache.delete(sourceKey)
    })
  optionRequestCache.set(sourceKey, request)
  return request
}

export async function ensureFormOptionSourcesLoaded(formKey, force = false) {
  const config = resolveFormConfig(formKey)
  const fields = [...(config.fields || []), ...(config.decisionFields || [])]
  const optionFields = fields.filter((field) => field?.optionsApi)
  if (!optionFields.length) {
    return {}
  }
  await Promise.all(optionFields.map((field) => ensureFieldOptionsLoaded(field, force)))
  return optionFields.reduce((result, field) => {
    if (field.optionsApi) {
      result[field.optionsApi] = getCachedOptionSource(field.optionsApi)
    }
    return result
  }, {})
}

function safeNumber(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number : NaN
}

function isBlank(value) {
  return value == null || String(value).trim() === ''
}

function matchesConditionalRule(rule, model = {}, context = {}) {
  if (typeof rule === 'boolean') {
    return rule
  }
  if (typeof rule === 'function') {
    return Boolean(rule(model, context))
  }
  if (typeof rule === 'string') {
    if (rule === 'always') {
      return true
    }
    if (rule === 'never') {
      return false
    }
    if (rule === 'systemManaged') {
      return true
    }
  }
  if (rule && typeof rule === 'object') {
    const currentValue = model?.[rule.field]
    if (Object.prototype.hasOwnProperty.call(rule, 'equals')) {
      return currentValue === rule.equals
    }
    if (Object.prototype.hasOwnProperty.call(rule, 'notEquals')) {
      return currentValue !== rule.notEquals
    }
    if (Array.isArray(rule.in)) {
      return rule.in.includes(currentValue)
    }
    if (Array.isArray(rule.notIn)) {
      return !rule.notIn.includes(currentValue)
    }
    if (rule.truthy) {
      return Boolean(currentValue)
    }
    if (rule.falsy) {
      return !currentValue
    }
  }
  return false
}

export function resolveFormConfig(formKey) {
  if (!formKey) {
    return normalizeFormConfig('default', BASE_FORM_REGISTRY.default)
  }
  return normalizeFormConfig(
    formKey,
    REMOTE_FORM_REGISTRY[formKey] || BASE_FORM_REGISTRY[formKey] || BASE_FORM_REGISTRY.default
  )
}

export function resolveReadonlyFields(formKey) {
  const config = resolveFormConfig(formKey)
  return config.summaryFields || config.fields || []
}

export function resolveDecisionFields(formKey, outcome = '') {
  const config = resolveFormConfig(formKey)
  const fields = config.decisionFields || []
  return fields.filter((field) => !field.outcome || field.outcome === outcome)
}

export function resolveFieldReadonly(field, model = {}, context = {}) {
  if (field?.readonly === true) {
    return true
  }
  const rule = field?.readonlyWhen
  if (!rule) {
    return false
  }
  return matchesConditionalRule(rule, model, context)
}

export function resolveFieldVisible(field, model = {}, context = {}) {
  if (field?.visible === false) {
    return false
  }
  if (!field?.visibleWhen) {
    return true
  }
  return matchesConditionalRule(field.visibleWhen, model, context)
}

export function resolveFieldSpan(field) {
  const span = Number(field?.span || field?.layout?.span || 1)
  if (!Number.isFinite(span) || span <= 1) {
    return 1
  }
  return Math.min(2, Math.round(span))
}

export function resolveFieldProps(field, model = {}, context = {}) {
  const props = field?.componentProps
  if (!props) {
    return {}
  }
  if (typeof props === 'function') {
    return props(model, context) || {}
  }
  return { ...props }
}

export function resolveFieldOptions(field, optionSources = {}) {
  const sourceKey = field?.optionsKey || field?.optionsApi
  if (!sourceKey) {
    return []
  }
  const source = optionSources[sourceKey] || optionSources[field.optionsKey] || []
  if (!Array.isArray(source)) {
    return []
  }
  return source.map((item) => {
    if (item && typeof item === 'object') {
      if ('value' in item && 'label' in item) {
        return item
      }
      return {
        value: item.username || item.id || item.value || '',
        label: item.nickname || item.name || item.label || item.username || String(item.id || '')
      }
    }
    return {
      value: item,
      label: String(item)
    }
  })
}

export function formatFieldValue(field, value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  if (Array.isArray(value)) {
    return value.length ? value.join(', ') : '-'
  }
  if (field?.formatter === 'datetime' || field?.type === 'datetime-local') {
    const date = new Date(value)
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString()
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

export function buildReadonlyEntries(formKey, source, options = {}) {
  const fields = resolveReadonlyFields(formKey).filter((field) => resolveFieldVisible(field, source || {}, options.context || {}))
  const entries = fields.map((field) => ({
    key: field.key,
    label: field.label,
    help: field.help || '',
    span: resolveFieldSpan(field),
    value: formatFieldValue(field, source?.[field.key])
  }))
  if (!options.includeRest || !source || typeof source !== 'object') {
    return entries
  }
  const knownKeys = new Set(fields.map((field) => field.key))
  for (const [key, value] of Object.entries(source)) {
    if (knownKeys.has(key)) {
      continue
    }
    entries.push({
      key,
      label: options.extraLabels?.[key] || key,
      help: '',
      span: 1,
      value: formatFieldValue(null, value)
    })
  }
  return entries
}

export function validateFields(fields, model = {}) {
  const errors = []
  for (const field of fields || []) {
    for (const rule of field.validator || []) {
      if (rule.type === 'required' && isBlank(model[field.key])) {
        errors.push(rule.message || `${field.label}不能为空`)
      }
      if (rule.type === 'positiveInteger') {
        const number = safeNumber(model[field.key])
        if (!Number.isInteger(number) || number <= 0) {
          errors.push(rule.message || `${field.label}必须为正整数`)
        }
      }
      if (rule.type === 'max') {
        const number = safeNumber(model[field.key])
        if (!Number.isNaN(number) && number > Number(rule.value)) {
          errors.push(rule.message || `${field.label}不能大于 ${rule.value}`)
        }
      }
      if (rule.type === 'minLength') {
        const value = String(model[field.key] || '')
        if (value.trim().length < Number(rule.value || 0)) {
          errors.push(rule.message || `${field.label}长度不足`)
        }
      }
      if (rule.type === 'afterField') {
        const current = model[field.key] ? new Date(model[field.key]).getTime() : NaN
        const compare = model[rule.field] ? new Date(model[rule.field]).getTime() : NaN
        if (!Number.isNaN(current) && !Number.isNaN(compare) && current <= compare) {
          errors.push(rule.message || `${field.label}必须晚于${rule.field}`)
        }
      }
    }
  }
  return errors
}

export function validateFormBySchema(formKey, model = {}) {
  const config = resolveFormConfig(formKey)
  return validateFields(config.fields || [], model)
}

export function validateDecisionBySchema(formKey, outcome, model = {}) {
  return validateFields(resolveDecisionFields(formKey, outcome), model)
}
