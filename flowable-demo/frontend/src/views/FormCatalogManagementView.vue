<template>
  <div class="page-grid">
    <section class="panel-card">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Form Catalog</p>
          <h3>业务表单目录</h3>
        </div>
        <div class="management-meta">
          <span class="meta-pill">总数 {{ total }}</span>
          <span class="meta-pill">启用 {{ enabledCount }}</span>
          <span class="meta-pill">目录组件 {{ componentOptions.length }}</span>
        </div>
      </div>

      <div class="management-toolbar">
        <div class="management-filters">
          <label class="filter-grow">
            <span>搜索目录</span>
            <input v-model.trim="keyword" type="text" placeholder="按 formKey、表单名、页面标签或组件搜索" @keyup.enter="searchCatalogs" />
          </label>
          <label class="compact-field">
            <span>状态</span>
            <select v-model="statusFilter">
              <option value="">全部状态</option>
              <option value="1">启用</option>
              <option value="0">禁用</option>
            </select>
          </label>
          <label class="compact-field">
            <span>范围</span>
            <select v-model="scopeFilter">
              <option value="">全部范围</option>
              <option value="START">发起</option>
              <option value="TASK">任务</option>
            </select>
          </label>
        </div>

        <div class="management-actions">
          <button type="button" class="ghost-btn" @click="searchCatalogs">搜索</button>
          <button type="button" class="secondary-btn" @click="resetFilters">重置</button>
          <button type="button" class="primary-btn" @click="openCreate">新增目录</button>
        </div>
      </div>

      <p v-if="statusMessage" class="feedback success">{{ statusMessage }}</p>
      <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>

      <table class="simple-table">
        <thead>
          <tr>
            <th>表单</th>
            <th>页面</th>
            <th>组件</th>
            <th>范围</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in catalogs" :key="item.id">
            <td>
              <strong>{{ item.formName }}</strong>
              <p class="table-subtext">{{ item.formKey }}</p>
            </td>
            <td>{{ item.pageLabel || '-' }}</td>
            <td>
              <span class="status-pill muted-pill">{{ item.componentKey || '-' }}</span>
            </td>
            <td>{{ item.scope || '-' }}</td>
            <td>
              <span :class="['status-pill', item.status === 1 ? 'success-pill' : 'muted-pill']">
                {{ item.status === 1 ? '启用' : '禁用' }}
              </span>
            </td>
            <td>
              <div class="inline-actions">
                <button type="button" class="ghost-btn mini-btn" @click="showImpact(item)">引用</button>
                <button type="button" class="secondary-btn mini-btn" @click="editCatalog(item)">编辑</button>
                <button type="button" class="danger-btn mini-btn" @click="removeCatalog(item)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="!catalogs.length">
            <td colspan="6" class="empty-row">
              <div class="table-empty-state">
                <strong>没有匹配的表单目录</strong>
                <p>可以新增目录，或者调整筛选条件后重试。</p>
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

    <div v-if="dialogVisible" class="modal-mask" @click.self="closeDialog">
      <div class="modal-panel form-modal">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Catalog Editor</p>
            <h3>{{ form.id ? '编辑表单目录' : '新增表单目录' }}</h3>
          </div>
          <button type="button" class="ghost-btn" @click="closeDialog">关闭</button>
        </div>

        <form class="form-stack" @submit.prevent="submitCatalog">
          <div class="form-grid">
            <label>
              <span>表单 Key</span>
              <input v-model.trim="form.formKey" type="text" placeholder="manager-approval" />
            </label>
            <label>
              <span>表单名称</span>
              <input v-model.trim="form.formName" type="text" placeholder="部门经理审批单" />
            </label>
            <label>
              <span>页面标签</span>
              <input v-model.trim="form.pageLabel" type="text" placeholder="待办审批页" />
            </label>
            <label>
              <span>组件 Key</span>
              <select v-model="form.componentKey">
                <option v-for="item in componentOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
            </label>
            <label>
              <span>适用范围</span>
              <select v-model="form.scope">
                <option value="START">发起</option>
                <option value="TASK">任务</option>
              </select>
            </label>
            <label>
              <span>状态</span>
              <select v-model.number="form.status">
                <option :value="1">启用</option>
                <option :value="0">禁用</option>
              </select>
            </label>
          </div>

          <label>
            <span>描述</span>
            <textarea v-model.trim="form.description" rows="4" placeholder="描述该 formKey 对应的业务组件和使用场景"></textarea>
          </label>

          <div class="schema-editor-head">
            <div>
              <span>组件字段 Schema</span>
              <p class="muted-text">目录会把字段布局下发到发起页、待办页和监控快照，支持 `placeholder/help/optionsApi/validator`。</p>
            </div>
            <div class="inline-actions">
              <button type="button" class="ghost-btn mini-btn" @click="applySchemaTemplate">套用组件模板</button>
              <button type="button" class="ghost-btn mini-btn" @click="formatSchemaJson">格式化 JSON</button>
            </div>
          </div>

          <label>
            <textarea
              v-model.trim="form.fieldSchemaJson"
              rows="14"
              placeholder='[{"field":"managerAdvice","label":"经理审批建议","component":"textarea","required":true}]'
            ></textarea>
          </label>

          <label>
            <span>排序</span>
            <input v-model.number="form.sort" type="number" min="0" />
          </label>

          <div class="action-row">
            <button type="submit" class="primary-btn" :disabled="loading">
              {{ loading ? '保存中...' : form.id ? '保存更新' : '创建目录' }}
            </button>
            <button type="button" class="secondary-btn" @click="resetForm">重置</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="impactVisible" class="modal-mask" @click.self="closeImpact">
      <div class="modal-panel form-modal">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Catalog Impact</p>
            <h3>{{ impactData?.formName || '表单目录引用' }}</h3>
          </div>
          <button type="button" class="ghost-btn" @click="closeImpact">关闭</button>
        </div>

        <div class="management-meta">
          <span class="meta-pill">formKey {{ impactData?.formKey || '-' }}</span>
          <span class="meta-pill">引用 {{ impactData?.usageCount || 0 }}</span>
        </div>

        <div class="stack-list">
          <div v-for="item in impactData?.references || []" :key="item.id" class="list-item rich-list-item">
            <div>
              <strong>{{ item.processName }}</strong>
              <p>{{ item.processKey }} · v{{ item.version }} · {{ item.designerType }}</p>
            </div>
            <div class="inline-actions">
              <span :class="['status-pill', item.status === 'ACTIVE' ? 'success-pill' : 'muted-pill']">{{ item.status }}</span>
              <button type="button" class="secondary-btn mini-btn" @click="openReferencedProcess(item.id)">打开流程</button>
            </div>
          </div>
          <div v-if="!(impactData?.references || []).length" class="empty-state">当前目录还没有被流程版本引用。</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { http } from '../api/http'

const router = useRouter()
const loading = ref(false)
const dialogVisible = ref(false)
const impactVisible = ref(false)
const catalogs = ref([])
const impactData = ref(null)
const keyword = ref('')
const statusFilter = ref('')
const scopeFilter = ref('')
const page = ref(1)
const size = ref(8)
const total = ref(0)
const statusMessage = ref('')
const errorMessage = ref('')

const componentOptions = [
  { value: 'LEAVE_START', label: '请假发起表单' },
  { value: 'MANAGER_APPROVAL', label: '经理审批表单' },
  { value: 'GENERAL_APPROVAL', label: '最终审批表单' },
  { value: 'GENERIC_TASK', label: '通用任务表单' }
]

const schemaTemplates = {
  LEAVE_START: [
    { field: 'applicant', label: '申请人', component: 'text', required: true, readonly: true, span: 1, help: '发起人账号自动带入' },
    {
      field: 'days',
      label: '请假天数',
      component: 'number',
      required: true,
      defaultValue: 1,
      span: 1,
      placeholder: '请输入请假天数',
      help: '支持 1-30 天',
      validator: [
        { type: 'min', value: 1, message: '请假天数不能小于 1' },
        { type: 'max', value: 30, message: '请假天数不能大于 30' }
      ]
    },
    { field: 'startDate', label: '开始时间', component: 'datetime', required: true, span: 1, help: '请填写请假开始时间' },
    { field: 'endDate', label: '结束时间', component: 'datetime', required: true, span: 1, help: '请填写请假结束时间' },
    { field: 'reason', label: '请假原因', component: 'textarea', required: true, rows: 4, span: 2, help: '会同步展示在审批和监控快照中' }
  ],
  MANAGER_APPROVAL: [
    { field: 'reason', label: '请假原因', component: 'textarea', readonly: true, rows: 4, span: 2, help: '来自发起页原始申请内容' },
    { field: 'managerAdvice', label: '经理审批建议', component: 'textarea', required: true, rows: 3, span: 2, help: '审批建议会沉淀到流程变量' },
    {
      field: 'priority',
      label: '优先级',
      component: 'select',
      defaultValue: 'normal',
      span: 1,
      help: '高优先级会在监控中心和待办列表里更明显',
      options: [
        { value: 'normal', label: '普通' },
        { value: 'high', label: '高优先级' },
        { value: 'urgent', label: '加急' }
      ]
    }
  ],
  GENERAL_APPROVAL: [
    { field: 'reason', label: '请假原因', component: 'textarea', readonly: true, rows: 4, span: 2, help: '来自发起页原始申请内容' },
    { field: 'finalDecisionNote', label: '最终审批说明', component: 'textarea', rows: 3, span: 2, help: '最终结论会同步到监控快照' },
    {
      field: 'allowResubmit',
      label: '允许补充材料',
      component: 'select',
      defaultValue: false,
      span: 1,
      help: '审批驳回时可决定是否允许补充资料',
      options: [
        { value: false, label: '否' },
        { value: true, label: '是' }
      ]
    }
  ],
  GENERIC_TASK: [
    { field: 'reason', label: '业务摘要', component: 'textarea', readonly: true, rows: 4, span: 2, help: '运行中业务摘要' },
    { field: 'remark', label: '处理备注', component: 'textarea', rows: 3, span: 2, help: '目录说明字段' }
  ]
}

const form = reactive({
  id: null,
  formKey: '',
  formName: '',
  pageLabel: '',
  componentKey: 'GENERIC_TASK',
  fieldSchemaJson: JSON.stringify(schemaTemplates.GENERIC_TASK, null, 2),
  scope: 'TASK',
  description: '',
  sort: 0,
  status: 1
})

const enabledCount = computed(() => catalogs.value.filter((item) => item.status === 1).length)

function resetMessages() {
  statusMessage.value = ''
  errorMessage.value = ''
}

function resetForm() {
  form.id = null
  form.formKey = ''
  form.formName = ''
  form.pageLabel = ''
  form.componentKey = 'GENERIC_TASK'
  form.fieldSchemaJson = JSON.stringify(schemaTemplates.GENERIC_TASK, null, 2)
  form.scope = 'TASK'
  form.description = ''
  form.sort = 0
  form.status = 1
}

function openCreate() {
  resetMessages()
  resetForm()
  dialogVisible.value = true
}

function closeDialog() {
  dialogVisible.value = false
  resetForm()
}

function closeImpact() {
  impactVisible.value = false
  impactData.value = null
}

function editCatalog(item) {
  resetMessages()
  form.id = item.id
  form.formKey = item.formKey
  form.formName = item.formName
  form.pageLabel = item.pageLabel || ''
  form.componentKey = item.componentKey || 'GENERIC_TASK'
  form.fieldSchemaJson = item.fieldSchemaJson || JSON.stringify(schemaTemplates[item.componentKey] || schemaTemplates.GENERIC_TASK, null, 2)
  form.scope = item.scope || 'TASK'
  form.description = item.description || ''
  form.sort = item.sort ?? 0
  form.status = item.status ?? 1
  dialogVisible.value = true
}

async function fetchCatalogs() {
  const { data } = await http.get('/form-catalog/query', {
    params: {
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      status: statusFilter.value === '' ? undefined : Number(statusFilter.value),
      scope: scopeFilter.value || undefined
    }
  })
  catalogs.value = data.items || []
  total.value = data.total || 0
}

function searchCatalogs() {
  page.value = 1
  fetchCatalogs()
}

function resetFilters() {
  keyword.value = ''
  statusFilter.value = ''
  scopeFilter.value = ''
  page.value = 1
  fetchCatalogs()
}

function changePage(nextPage) {
  page.value = nextPage
  fetchCatalogs()
}

function changeSize() {
  page.value = 1
  fetchCatalogs()
}

async function submitCatalog() {
  resetMessages()
  loading.value = true
  try {
    if (form.fieldSchemaJson) {
      JSON.parse(form.fieldSchemaJson)
    }
    const payload = { ...form }
    if (form.id) {
      await http.put(`/form-catalog/${form.id}`, payload)
      statusMessage.value = '表单目录已更新'
    } else {
      await http.post('/form-catalog', payload)
      statusMessage.value = '表单目录已创建'
    }
    closeDialog()
    await fetchCatalogs()
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

async function removeCatalog(item) {
  resetMessages()
  let impactSummary = ''
  try {
    const { data } = await http.get(`/form-catalog/${item.id}/impact`)
    const refs = (data?.references || []).slice(0, 3)
    if ((data?.usageCount || 0) > 0) {
      impactSummary = `\n当前被 ${data.usageCount} 个流程版本引用：${refs.map((ref) => `${ref.processName} v${ref.version}`).join('、')}`
    }
  } catch (error) {
    errorMessage.value = error.normalizedMessage
    return
  }
  if (!window.confirm(`确认删除表单目录「${item.formName}」吗？${impactSummary}`)) {
    return
  }
  loading.value = true
  try {
    await http.delete(`/form-catalog/${item.id}`)
    statusMessage.value = '表单目录已删除'
    await fetchCatalogs()
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

async function showImpact(item) {
  resetMessages()
  try {
    const { data } = await http.get(`/form-catalog/${item.id}/impact`)
    impactData.value = data || null
    impactVisible.value = true
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  }
}

function openReferencedProcess(id) {
  closeImpact()
  router.push({ path: '/designer', query: { definitionId: String(id) } })
}

function applySchemaTemplate() {
  form.fieldSchemaJson = JSON.stringify(
    schemaTemplates[form.componentKey] || schemaTemplates.GENERIC_TASK,
    null,
    2
  )
}

function formatSchemaJson() {
  resetMessages()
  try {
    form.fieldSchemaJson = JSON.stringify(JSON.parse(form.fieldSchemaJson || '[]'), null, 2)
  } catch (error) {
    errorMessage.value = '组件字段 Schema 不是合法 JSON'
  }
}

onMounted(fetchCatalogs)
</script>

<style scoped>
.schema-editor-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
</style>
