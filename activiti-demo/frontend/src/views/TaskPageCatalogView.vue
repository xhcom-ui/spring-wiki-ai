<template>
  <div class="page-grid">
    <section class="stats-grid">
      <article class="stat-card">
        <span class="muted-text">目录总数</span>
        <strong>{{ allItems.length }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">表单目录</span>
        <strong>{{ formCount }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">页面目录</span>
        <strong>{{ pageCount }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">系统内置</span>
        <strong>{{ systemCount }}</strong>
      </article>
    </section>

    <p v-if="focusMessage" class="feedback success">{{ focusMessage }}</p>

    <section class="panel-card">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Runtime Catalog</p>
          <h3>业务页目录管理</h3>
        </div>
        <div class="management-actions">
          <button type="button" class="ghost-btn" @click="searchItems">搜索</button>
          <button type="button" class="secondary-btn" @click="resetFilters">重置</button>
          <button type="button" class="primary-btn" @click="openCreate">新增目录项</button>
        </div>
      </div>

      <div class="management-toolbar">
        <div class="management-filters">
          <label class="filter-grow">
            <span>搜索目录</span>
            <input
              v-model.trim="keyword"
              type="text"
              placeholder="按 Key、名称、业务类型、描述搜索"
              @keyup.enter="searchItems"
            />
          </label>
          <label class="compact-field">
            <span>目录类型</span>
            <select v-model="itemTypeFilter">
              <option value="">全部</option>
              <option value="FORM">表单</option>
              <option value="PAGE">页面</option>
            </select>
          </label>
          <label class="compact-field">
            <span>页面模式</span>
            <select v-model="pageModeFilter">
              <option value="">全部</option>
              <option value="todo">待办页</option>
              <option value="done">完成页</option>
            </select>
          </label>
          <label class="compact-field">
            <span>状态</span>
            <select v-model="statusFilter">
              <option value="">全部</option>
              <option value="1">启用</option>
              <option value="0">禁用</option>
            </select>
          </label>
        </div>
      </div>

      <p v-if="statusMessage" class="feedback success">{{ statusMessage }}</p>
      <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>

      <table class="simple-table">
        <thead>
          <tr>
            <th>Key</th>
            <th>名称</th>
            <th>类型</th>
            <th>业务类型</th>
            <th>默认映射</th>
            <th>连通性</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="item in items"
            :key="item.id"
            :class="{ 'table-row-selected': item.id === selectedItem?.id }"
            @click="selectItem(item)"
          >
            <td>
              <strong>{{ item.itemKey }}</strong>
              <p class="table-subline">{{ item.description || '未填写描述' }}</p>
            </td>
            <td>{{ item.label }}</td>
            <td>
              <span class="status-pill">{{ renderType(item) }}</span>
            </td>
            <td>{{ item.businessKind || '-' }}</td>
            <td>
              <template v-if="item.itemType === 'FORM'">
                <div class="stack-list compact-stack">
                  <span>待办: {{ item.defaultTodoPage || '-' }}</span>
                  <span>完成: {{ item.defaultDonePage || '-' }}</span>
                </div>
              </template>
              <template v-else>
                {{ item.pageMode === 'done' ? '完成页' : '待办页' }}
              </template>
            </td>
            <td>
              <span :class="['status-pill', connectivityOf(item).ok ? 'success-pill' : 'warning-pill']">
                {{ connectivityOf(item).ok ? '已连通' : '待修复' }}
              </span>
            </td>
            <td>
              <div class="stack-list compact-stack">
                <span :class="['status-pill', item.status === 1 ? 'success-pill' : 'muted-pill']">
                  {{ item.status === 1 ? '启用' : '禁用' }}
                </span>
                <span v-if="item.systemFlag === 1" class="meta-pill">系统内置</span>
              </div>
            </td>
            <td>
              <div class="inline-actions">
                <button type="button" class="secondary-btn mini-btn" @click="editItem(item)">编辑</button>
                <button
                  type="button"
                  class="danger-btn mini-btn"
                  :disabled="item.systemFlag === 1"
                  @click="removeItem(item)"
                >
                  删除
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!items.length">
            <td colspan="8" class="empty-row">
              <div class="table-empty-state">
                <strong>没有匹配的业务目录</strong>
                <p>新增后会同步影响流程设计器和运行时办理页映射。</p>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pagination-bar">
        <div class="pagination-meta">
          <span>第 {{ page }} 页</span>
          <span>共 {{ total }} 条</span>
        </div>
        <div class="inline-actions">
          <label class="page-size-field">
            <span>每页</span>
            <select v-model.number="size" @change="changeSize">
              <option :value="8">8</option>
              <option :value="12">12</option>
              <option :value="20">20</option>
            </select>
          </label>
          <button type="button" class="secondary-btn mini-btn" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
          <button type="button" class="secondary-btn mini-btn" :disabled="page * size >= total" @click="changePage(page + 1)">下一页</button>
        </div>
      </div>
    </section>

    <section class="content-grid two-column">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Form Preview</p>
            <h3>表单推荐映射预览</h3>
          </div>
        </div>

        <div class="stack-list">
          <div v-for="item in activeForms" :key="item.id" class="definition-card">
            <div class="definition-title">
              <strong>{{ item.label }}</strong>
              <span class="status-pill">{{ item.itemKey }}</span>
            </div>
            <p>{{ item.description || '未填写描述' }}</p>
            <small>待办页 {{ item.defaultTodoPage || '-' }} · 完成页 {{ item.defaultDonePage || '-' }}</small>
          </div>
          <div v-if="!activeForms.length" class="empty-state">当前没有启用中的表单目录。</div>
        </div>
      </article>

      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Page Preview</p>
            <h3>页面目录生效预览</h3>
          </div>
        </div>

        <div class="stack-list">
          <div v-for="item in activePages" :key="item.id" class="definition-card">
            <div class="definition-title">
              <strong>{{ item.label }}</strong>
              <span :class="['status-pill', item.pageMode === 'done' ? 'muted-pill' : 'success-pill']">
                {{ item.pageMode === 'done' ? '完成页' : '待办页' }}
              </span>
            </div>
            <p>{{ item.businessKind || 'generic' }}</p>
            <small>{{ item.itemKey }} · {{ item.description || '未填写描述' }}</small>
          </div>
          <div v-if="!activePages.length" class="empty-state">当前没有启用中的页面目录。</div>
        </div>
      </article>
    </section>

    <section v-if="selectedItem" class="panel-card">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Catalog Detail</p>
          <h3>{{ selectedItem.label }}</h3>
        </div>
        <div class="inline-actions">
          <button type="button" class="secondary-btn mini-btn" @click="editItem(selectedItem)">编辑目录项</button>
          <button
            v-if="selectedItem.itemType === 'PAGE' && connectivityOf(selectedItem).ok"
            type="button"
            class="ghost-btn mini-btn"
            @click="openRuntimePreview(selectedItem)"
          >
            打开路由预览
          </button>
        </div>
      </div>

      <div class="detail-grid compact-detail-grid">
        <div class="detail-item">
          <span class="detail-key">目录 Key</span>
          <strong class="detail-value">{{ selectedItem.itemKey }}</strong>
        </div>
        <div class="detail-item">
          <span class="detail-key">目录类型</span>
          <strong class="detail-value">{{ renderType(selectedItem) }}</strong>
        </div>
        <div class="detail-item">
          <span class="detail-key">业务类型</span>
          <strong class="detail-value">{{ selectedItem.businessKind || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span class="detail-key">路由连通性</span>
          <strong class="detail-value">{{ connectivityOf(selectedItem).ok ? '已连通' : '待修复' }}</strong>
        </div>
      </div>

      <div class="detail-item">
        <span class="detail-key">校验结果</span>
        <div v-if="connectivityOf(selectedItem).issues.length" class="kv-list">
          <div v-for="issue in connectivityOf(selectedItem).issues" :key="issue" class="kv-row">
            <span class="kv-key">问题</span>
            <strong class="kv-value">{{ issue }}</strong>
          </div>
        </div>
        <strong v-else class="detail-value">目录项已经能连到实际前端运行时页面。</strong>
      </div>

      <div v-if="selectedItem.itemType === 'FORM'" class="detail-grid compact-detail-grid">
        <div class="detail-item">
          <span class="detail-key">默认待办页</span>
          <strong class="detail-value">{{ selectedItem.defaultTodoPage || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span class="detail-key">默认完成页</span>
          <strong class="detail-value">{{ selectedItem.defaultDonePage || '-' }}</strong>
        </div>
      </div>

      <div class="detail-item">
        <span class="detail-key">流程引用反查</span>
        <div v-if="referenceLoading" class="muted-text">正在扫描流程定义里的引用节点...</div>
        <div v-else-if="references.length" class="stack-list compact-stack">
          <div v-for="reference in references" :key="`${reference.processDefinitionId}-${reference.elementId}-${reference.matchField}`" class="definition-card">
            <div class="definition-title">
              <strong>{{ reference.processName }}</strong>
              <span class="status-pill">v{{ reference.version }}</span>
            </div>
            <p>
              {{ reference.elementName || reference.elementId }} · {{ reference.elementType }} · 命中 {{ reference.matchField }}
            </p>
            <small>{{ reference.processKey }} · {{ reference.status }}</small>
            <div class="inline-actions">
              <button type="button" class="secondary-btn mini-btn" @click="openProcessReference(reference)">打开流程设计页</button>
            </div>
          </div>
        </div>
        <strong v-else class="detail-value">当前还没有流程节点引用这个目录项。</strong>
      </div>
    </section>

    <div v-if="dialogVisible" class="modal-mask" @click.self="closeDialog">
      <div class="modal-panel form-modal">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Catalog Editor</p>
            <h3>{{ form.id ? '编辑目录项' : '新增目录项' }}</h3>
          </div>
          <button type="button" class="ghost-btn" @click="closeDialog">关闭</button>
        </div>

        <form class="form-stack" @submit.prevent="submitItem">
          <div class="form-grid">
            <label>
              <span>目录类型</span>
              <select v-model="form.itemType" :disabled="isSystemItem">
                <option value="FORM">表单目录</option>
                <option value="PAGE">页面目录</option>
              </select>
            </label>
            <label>
              <span>目录 Key</span>
              <input v-model.trim="form.itemKey" type="text" :disabled="isSystemItem" placeholder="leave-apply-form" />
            </label>
            <label>
              <span>显示名称</span>
              <input v-model.trim="form.label" type="text" placeholder="请假申请单" />
            </label>
            <label>
              <span>业务类型</span>
              <input v-model.trim="form.businessKind" type="text" placeholder="leave / expense / generic" />
            </label>
            <label v-if="form.itemType === 'PAGE'">
              <span>页面模式</span>
              <select v-model="form.pageMode">
                <option value="todo">待办页</option>
                <option value="done">完成页</option>
              </select>
            </label>
            <label>
              <span>排序</span>
              <input v-model.number="form.sort" type="number" min="0" />
            </label>
            <label>
              <span>状态</span>
              <select v-model.number="form.status">
                <option :value="1">启用</option>
                <option :value="0">禁用</option>
              </select>
            </label>
          </div>

          <div v-if="form.itemType === 'FORM'" class="form-grid">
            <label>
              <span>默认待办页</span>
              <input v-model.trim="form.defaultTodoPage" type="text" placeholder="leave-approval" />
            </label>
            <label>
              <span>默认完成页</span>
              <input v-model.trim="form.defaultDonePage" type="text" placeholder="leave-complete" />
            </label>
          </div>

          <label>
            <span>描述</span>
            <textarea v-model.trim="form.description" rows="4" placeholder="描述这个目录项的作用"></textarea>
          </label>

          <div class="action-row">
            <button type="submit" class="primary-btn" :disabled="loading">
              {{ loading ? '保存中...' : form.id ? '保存更新' : '创建目录项' }}
            </button>
            <button type="button" class="secondary-btn" @click="resetForm">重置</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { http } from '../api/http'
import { ensureTaskPageCatalogLoaded } from '../runtime/taskPageCatalog'
import { validateCatalogItemConnectivity } from '../runtime/taskPageConnectivity'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const dialogVisible = ref(false)
const items = ref([])
const allItems = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(8)
const keyword = ref('')
const itemTypeFilter = ref('')
const pageModeFilter = ref('')
const statusFilter = ref('')
const statusMessage = ref('')
const errorMessage = ref('')
const focusMessage = ref('')
const selectedItem = ref(null)
const references = ref([])
const referenceLoading = ref(false)

const form = reactive({
  id: null,
  itemKey: '',
  label: '',
  itemType: 'FORM',
  pageMode: 'todo',
  businessKind: '',
  description: '',
  defaultTodoPage: '',
  defaultDonePage: '',
  systemFlag: 0,
  sort: 0,
  status: 1
})

const formCount = computed(() => allItems.value.filter((item) => item.itemType === 'FORM').length)
const pageCount = computed(() => allItems.value.filter((item) => item.itemType === 'PAGE').length)
const systemCount = computed(() => allItems.value.filter((item) => item.systemFlag === 1).length)
const isSystemItem = computed(() => form.systemFlag === 1)
const activeForms = computed(() =>
  allItems.value.filter((item) => item.status === 1 && item.itemType === 'FORM').slice(0, 6)
)
const activePages = computed(() =>
  allItems.value.filter((item) => item.status === 1 && item.itemType === 'PAGE').slice(0, 6)
)

function resetMessages() {
  statusMessage.value = ''
  errorMessage.value = ''
  focusMessage.value = ''
}

function renderType(item) {
  if (item.itemType === 'FORM') {
    return '表单'
  }
  return item.pageMode === 'done' ? '完成页' : '待办页'
}

function resetForm() {
  form.id = null
  form.itemKey = ''
  form.label = ''
  form.itemType = 'FORM'
  form.pageMode = 'todo'
  form.businessKind = ''
  form.description = ''
  form.defaultTodoPage = ''
  form.defaultDonePage = ''
  form.systemFlag = 0
  form.sort = 0
  form.status = 1
}

function closeDialog() {
  dialogVisible.value = false
  resetForm()
}

function openCreate() {
  resetMessages()
  resetForm()
  dialogVisible.value = true
}

function editItem(item) {
  resetMessages()
  selectedItem.value = item
  form.id = item.id
  form.itemKey = item.itemKey
  form.label = item.label
  form.itemType = item.itemType
  form.pageMode = item.pageMode || 'todo'
  form.businessKind = item.businessKind || ''
  form.description = item.description || ''
  form.defaultTodoPage = item.defaultTodoPage || ''
  form.defaultDonePage = item.defaultDonePage || ''
  form.systemFlag = item.systemFlag || 0
  form.sort = item.sort || 0
  form.status = item.status ?? 1
  dialogVisible.value = true
}

function connectivityOf(item) {
  return validateCatalogItemConnectivity(item, allItems.value)
}

function selectItem(item, syncQuery = true) {
  selectedItem.value = item
  fetchReferences(item)
  if (!syncQuery) {
    return
  }
  router.replace({
    query: {
      ...route.query,
      itemKey: item.itemKey,
      itemType: item.itemType
    }
  })
}

async function fetchReferences(item) {
  if (!item?.id) {
    references.value = []
    return
  }
  referenceLoading.value = true
  try {
    const { data } = await http.get(`/runtime-catalog/items/${item.id}/references`)
    references.value = Array.isArray(data) ? data : []
  } catch (error) {
    references.value = []
  } finally {
    referenceLoading.value = false
  }
}

function openRuntimePreview(item) {
  if (!connectivityOf(item).ok) {
    return
  }
  router.push({
    name: 'task-runtime',
    params: {
      pageKey: item.itemKey
    },
    query: {
      mode: item.pageMode || 'todo',
      preview: '1'
    }
  })
}

function openProcessReference(reference) {
  if (!reference?.processDefinitionId) {
    return
  }
  router.push({
    name: 'designer',
    query: {
      processId: String(reference.processDefinitionId),
      focusElementId: reference.elementId || '',
      source: 'catalog'
    }
  })
}

async function fetchItems() {
  const { data } = await http.get('/runtime-catalog/items/query', {
    params: {
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      itemType: itemTypeFilter.value || undefined,
      pageMode: pageModeFilter.value || undefined,
      status: statusFilter.value === '' ? undefined : Number(statusFilter.value)
    }
  })
  items.value = data.items || []
  total.value = data.total || 0
}

async function fetchAllItems() {
  const { data } = await http.get('/runtime-catalog/items')
  allItems.value = Array.isArray(data) ? data : []
  syncSelectionFromQuery()
}

function searchItems() {
  page.value = 1
  fetchItems()
}

function resetFilters() {
  keyword.value = ''
  itemTypeFilter.value = ''
  pageModeFilter.value = ''
  statusFilter.value = ''
  page.value = 1
  fetchItems()
}

function changePage(nextPage) {
  page.value = nextPage
  fetchItems()
}

function changeSize() {
  page.value = 1
  fetchItems()
}

function syncSelectionFromQuery() {
  const itemKey = String(route.query.itemKey || '')
  const itemType = String(route.query.itemType || '')
  if (!itemKey) {
    if (!selectedItem.value && allItems.value.length) {
      selectItem(allItems.value[0], false)
    }
    return
  }
  const matched = allItems.value.find(
    (item) => item.itemKey === itemKey && (!itemType || item.itemType === itemType)
  )
  if (matched) {
    selectedItem.value = matched
    fetchReferences(matched)
    focusMessage.value = route.query.source === 'designer'
      ? `已从流程设计器定位到目录项：${matched.label}`
      : `已定位到目录项：${matched.label}`
  }
}

async function submitItem() {
  resetMessages()
  loading.value = true
  try {
    const payload = { ...form }
    if (form.itemType !== 'FORM') {
      payload.defaultTodoPage = null
      payload.defaultDonePage = null
    }
    if (form.id) {
      await http.put(`/runtime-catalog/items/${form.id}`, payload)
      statusMessage.value = '目录项已更新'
    } else {
      await http.post('/runtime-catalog/items', payload)
      statusMessage.value = '目录项已创建'
    }
    closeDialog()
    await Promise.all([fetchItems(), fetchAllItems(), ensureTaskPageCatalogLoaded(true)])
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

async function removeItem(item) {
  resetMessages()
  if (!window.confirm(`确认删除目录项「${item.label}」吗？`)) {
    return
  }
  loading.value = true
  try {
    await http.delete(`/runtime-catalog/items/${item.id}`)
    statusMessage.value = '目录项已删除'
    if ((page.value - 1) * size.value >= Math.max(0, total.value - 1) && page.value > 1) {
      page.value -= 1
    }
    await Promise.all([fetchItems(), fetchAllItems(), ensureTaskPageCatalogLoaded(true)])
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await Promise.all([fetchItems(), fetchAllItems()])
})

watch(
  () => route.fullPath,
  () => {
    syncSelectionFromQuery()
  }
)
</script>
