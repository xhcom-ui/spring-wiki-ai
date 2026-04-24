import { computed, ref } from 'vue'
import { http } from '../api/http'

const DEFAULT_FORM_CATALOG = [
  {
    key: 'leave-apply-form',
    label: '请假申请单',
    kind: 'leave',
    description: '适用于请假发起、部门经理审批、总经理审批等节点。',
    defaultTodoPage: 'leave-approval',
    defaultDonePage: 'leave-complete'
  }
]

const DEFAULT_PAGE_CATALOG = [
  {
    key: 'leave-approval',
    label: '请假审批页',
    mode: 'todo',
    kind: 'leave',
    description: '打开请假业务办理页，支持签收、审批和意见录入。'
  },
  {
    key: 'leave-complete',
    label: '请假完成页',
    mode: 'done',
    kind: 'leave',
    description: '展示请假节点处理完成后的业务回执。'
  },
  {
    key: 'generic-task',
    label: '通用任务页',
    mode: 'todo',
    kind: 'generic',
    description: '没有专用业务页时，使用统一的通用办理页。'
  },
  {
    key: 'generic-task-done',
    label: '通用完成页',
    mode: 'done',
    kind: 'generic',
    description: '没有专用完成页时，使用统一的通用回执页。'
  }
]

const formsRef = ref([...DEFAULT_FORM_CATALOG])
const pagesRef = ref([...DEFAULT_PAGE_CATALOG])
const catalogLoaded = ref(false)
const catalogLoading = ref(false)
let loadPromise = null

export const taskFormCatalog = computed(() => formsRef.value)
export const taskPageCatalog = computed(() => pagesRef.value)

export function getTaskFormCatalogSnapshot() {
  return [...formsRef.value]
}

export function getTaskPageCatalogSnapshot() {
  return [...pagesRef.value]
}

export function normalizeCatalogKey(value) {
  return String(value || '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
}

function normalizeForm(item) {
  return {
    key: normalizeCatalogKey(item?.key),
    label: String(item?.label || item?.key || ''),
    kind: normalizeCatalogKey(item?.kind || 'generic') || 'generic',
    description: String(item?.description || ''),
    defaultTodoPage: normalizeCatalogKey(item?.defaultTodoPage),
    defaultDonePage: normalizeCatalogKey(item?.defaultDonePage)
  }
}

function normalizePage(item) {
  return {
    key: normalizeCatalogKey(item?.key),
    label: String(item?.label || item?.key || ''),
    mode: item?.mode === 'done' ? 'done' : 'todo',
    kind: normalizeCatalogKey(item?.kind || 'generic') || 'generic',
    description: String(item?.description || '')
  }
}

export function hydrateTaskPageCatalog(payload = {}) {
  const nextForms = Array.isArray(payload.forms) ? payload.forms.map(normalizeForm).filter((item) => item.key) : []
  const nextPages = Array.isArray(payload.pages) ? payload.pages.map(normalizePage).filter((item) => item.key) : []
  if (nextForms.length) {
    formsRef.value = nextForms
  }
  if (nextPages.length) {
    pagesRef.value = nextPages
  }
  catalogLoaded.value = true
}

export async function ensureTaskPageCatalogLoaded(force = false) {
  if (catalogLoaded.value && !force) {
    return {
      forms: formsRef.value,
      pages: pagesRef.value
    }
  }
  if (catalogLoading.value && loadPromise && !force) {
    return loadPromise
  }

  catalogLoading.value = true
  loadPromise = http
    .get('/runtime-catalog/catalog')
    .then(({ data }) => {
      hydrateTaskPageCatalog(data || {})
      return {
        forms: formsRef.value,
        pages: pagesRef.value
      }
    })
    .catch(() => {
      catalogLoaded.value = true
      return {
        forms: formsRef.value,
        pages: pagesRef.value
      }
    })
    .finally(() => {
      catalogLoading.value = false
    })

  return loadPromise
}

export function findTaskFormDefinition(formKey) {
  const normalizedKey = normalizeCatalogKey(formKey)
  return formsRef.value.find((item) => item.key === normalizedKey) || null
}

export function findTaskPageDefinition(pageKey, mode) {
  const normalizedKey = normalizeCatalogKey(pageKey)
  return (
    pagesRef.value.find((item) => item.key === normalizedKey && (!mode || item.mode === mode)) || null
  )
}

export function getTaskFormOptions(currentValue = '') {
  const normalizedCurrent = normalizeCatalogKey(currentValue)
  const options = formsRef.value.map((item) => ({
    value: item.key,
    label: item.label,
    description: item.description
  }))
  if (normalizedCurrent && !options.some((item) => item.value === normalizedCurrent)) {
    options.unshift({
      value: normalizedCurrent,
      label: `自定义表单 · ${normalizedCurrent}`,
      description: '当前流程节点使用了未注册的自定义表单 Key。'
    })
  }
  return options
}

export function getTaskPageOptions(mode, currentValue = '') {
  const normalizedCurrent = normalizeCatalogKey(currentValue)
  const options = pagesRef.value
    .filter((item) => item.mode === mode)
    .map((item) => ({
      value: item.key,
      label: item.label,
      description: item.description
    }))
  if (normalizedCurrent && !options.some((item) => item.value === normalizedCurrent)) {
    options.unshift({
      value: normalizedCurrent,
      label: `自定义页面 · ${normalizedCurrent}`,
      description: '当前流程节点使用了未注册的自定义页面标识。'
    })
  }
  return options
}

export function resolveRecommendedPages(formKey) {
  const definition = findTaskFormDefinition(formKey)
  if (!definition) {
    return {
      todoPage: '',
      donePage: ''
    }
  }
  return {
    todoPage: definition.defaultTodoPage || '',
    donePage: definition.defaultDonePage || ''
  }
}
