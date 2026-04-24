<template>
  <div class="page-grid">
    <section class="panel-card">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Form Catalog</p>
          <h3>表单目录与 Schema</h3>
        </div>
        <div class="management-meta">
          <span class="meta-pill">总数 {{ total }}</span>
          <span class="meta-pill">启用 {{ enabledCount }}</span>
          <span class="meta-pill">禁用 {{ disabledCount }}</span>
        </div>
      </div>

      <div class="management-toolbar">
        <div class="management-filters">
          <label class="filter-grow">
            <span>搜索目录</span>
            <input v-model.trim="keyword" type="text" placeholder="搜索表单 Key、名称或说明" @keyup.enter="searchCatalogs" />
          </label>
          <label class="compact-field">
            <span>状态</span>
            <select v-model="statusFilter">
              <option value="">全部状态</option>
              <option value="1">启用</option>
              <option value="0">禁用</option>
            </select>
          </label>
        </div>

        <div class="management-actions">
          <button type="button" class="ghost-btn" @click="searchCatalogs">搜索</button>
          <button type="button" class="secondary-btn" @click="resetFilters">重置</button>
          <button type="button" class="primary-btn" @click="openCreate">新增表单</button>
        </div>
      </div>

      <p v-if="statusMessage" class="feedback success">{{ statusMessage }}</p>
      <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>

      <table class="simple-table">
        <thead>
          <tr>
            <th>表单 Key</th>
            <th>名称</th>
            <th>说明</th>
            <th>状态</th>
            <th>更新时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="catalog in catalogs" :key="catalog.id">
            <td>{{ catalog.formKey }}</td>
            <td>{{ catalog.title }}</td>
            <td>{{ catalog.description || '-' }}</td>
            <td>
              <span :class="['status-pill', catalog.status === 1 ? 'success-pill' : 'muted-pill']">
                {{ catalog.status === 1 ? '启用' : '禁用' }}
              </span>
            </td>
            <td>{{ formatDate(catalog.updatedAt || catalog.createdAt) }}</td>
            <td>
              <div class="inline-actions">
                <button type="button" class="secondary-btn mini-btn" @click="editCatalog(catalog)">编辑</button>
                <button type="button" class="danger-btn mini-btn" @click="removeCatalog(catalog)">删除</button>
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
              <option :value="6">6</option>
              <option :value="10">10</option>
              <option :value="20">20</option>
            </select>
          </label>
          <button type="button" class="secondary-btn mini-btn" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
          <button type="button" class="secondary-btn mini-btn" :disabled="page * size >= total" @click="changePage(page + 1)">下一页</button>
        </div>
      </div>
    </section>

    <div v-if="dialogVisible" class="modal-mask" @click.self="closeDialog">
      <div class="modal-panel schema-modal schema-editor-modal">
        <div class="panel-head schema-modal-head">
          <div>
            <p class="eyebrow">Form Schema Editor</p>
            <h3>{{ form.id ? '编辑表单目录' : '新增表单目录' }}</h3>
          </div>
          <button type="button" class="ghost-btn schema-close-btn" @click="closeDialog">关闭</button>
        </div>

        <p v-if="dialogStatusMessage" class="feedback success">{{ dialogStatusMessage }}</p>
        <p v-if="dialogErrorMessage" class="feedback error">{{ dialogErrorMessage }}</p>

        <form class="form-stack" @submit.prevent="submitCatalog">
          <section class="content-grid two-column schema-workbench">
            <article class="panel-card schema-editor-pane">
              <div class="form-grid schema-basic-grid">
                <label>
                  <span>表单 Key</span>
                  <input v-model.trim="form.formKey" type="text" placeholder="leave-form" :disabled="Boolean(form.id)" />
                </label>
                <label>
                  <span>表单名称</span>
                  <input v-model.trim="form.title" type="text" placeholder="请假申请单" />
                </label>
                <label class="full-width-field">
                  <span>说明</span>
                  <textarea v-model.trim="form.description" rows="3" placeholder="说明当前表单的适用场景"></textarea>
                </label>
                <label>
                  <span>状态</span>
                  <select v-model.number="form.status">
                    <option :value="1">启用</option>
                    <option :value="0">禁用</option>
                  </select>
                </label>
              </div>

              <div class="action-row schema-template-actions">
                <button type="button" class="secondary-btn" @click="applySchemaTemplate('start')">发起表单模板</button>
                <button type="button" class="secondary-btn" @click="applySchemaTemplate('approval')">审批表单模板</button>
                <button type="button" class="secondary-btn" @click="applySchemaTemplate('summary')">摘要模板</button>
              </div>

              <div class="panel-head nested-head">
                <div>
                  <p class="eyebrow">Visual Editor</p>
                  <h3>字段级可视化编辑器</h3>
                </div>
                <div class="inline-actions">
                  <button type="button" class="secondary-btn mini-btn" @click="loadJsonToVisualEditor">从 JSON 载入</button>
                  <button type="button" class="secondary-btn mini-btn" @click="syncVisualSchemaToJson(false)">回写 JSON</button>
                </div>
              </div>

              <p class="muted-text schema-editor-tip">
                日常维护优先在这里编辑字段。右侧 JSON 仍保留给高级配置，手工改 JSON 后可再点“从 JSON 载入”同步回可视化编辑器。
              </p>

              <section v-for="section in schemaSections" :key="section.key" class="schema-section">
                <div class="schema-section-head">
                  <div>
                    <strong>{{ section.title }}</strong>
                    <p>{{ section.description }} · 共 {{ fieldEditor[section.key].length }} 项</p>
                  </div>
                  <div class="inline-actions">
                    <button type="button" class="secondary-btn mini-btn" @click="addEditorField(section.key)">新增字段</button>
                    <button type="button" class="ghost-btn mini-btn" @click="toggleSection(section.key)">
                      {{ collapsedSections[section.key] ? '展开' : '收起' }}
                    </button>
                  </div>
                </div>

                <div v-show="!collapsedSections[section.key]" class="field-editor-list">
                  <article v-for="(field, index) in fieldEditor[section.key]" :key="field.id" class="field-editor-card">
                    <div class="field-editor-card-head">
                      <strong>{{ field.label || field.key || `${section.shortTitle}${index + 1}` }}</strong>
                      <div class="inline-actions">
                        <button type="button" class="ghost-btn mini-btn" :disabled="index === 0" @click="moveEditorField(section.key, index, -1)">上移</button>
                        <button
                          type="button"
                          class="ghost-btn mini-btn"
                          :disabled="index === fieldEditor[section.key].length - 1"
                          @click="moveEditorField(section.key, index, 1)"
                        >
                          下移
                        </button>
                        <button type="button" class="danger-btn mini-btn" @click="removeEditorField(section.key, field.id)">删除</button>
                      </div>
                    </div>

                    <div class="form-grid schema-field-grid">
                      <label>
                        <span>字段 Key</span>
                        <input v-model.trim="field.key" type="text" placeholder="reason" @input="handleVisualSchemaChange" />
                      </label>
                      <label>
                        <span>字段标题</span>
                        <input v-model.trim="field.label" type="text" placeholder="请假原因" @input="handleVisualSchemaChange" />
                      </label>

                      <label v-if="section.key !== 'summaryFields'">
                        <span>组件类型</span>
                        <select v-model="field.type" @change="handleVisualSchemaChange">
                          <option v-for="type in FIELD_TYPES" :key="type.value" :value="type.value">
                            {{ type.label }}
                          </option>
                        </select>
                      </label>

                      <label v-else>
                        <span>展示格式</span>
                        <select v-model="field.formatter" @change="handleVisualSchemaChange">
                          <option value="">默认</option>
                          <option value="datetime">日期时间</option>
                          <option value="date">日期</option>
                          <option value="number">数字</option>
                          <option value="boolean">布尔</option>
                        </select>
                      </label>

                      <label>
                        <span>宽度占比</span>
                        <select v-model.number="field.span" @change="handleVisualSchemaChange">
                          <option :value="1">1 列</option>
                          <option :value="2">2 列</option>
                        </select>
                      </label>

                      <label v-if="section.key !== 'summaryFields'">
                        <span>占位文案</span>
                        <input v-model.trim="field.placeholder" type="text" placeholder="请输入..." @input="handleVisualSchemaChange" />
                      </label>

                      <label>
                        <span>帮助文案</span>
                        <input v-model.trim="field.help" type="text" placeholder="补充字段填写说明" @input="handleVisualSchemaChange" />
                      </label>

                      <label v-if="section.key !== 'summaryFields' && ['select', 'radio'].includes(field.type)">
                        <span>选项接口</span>
                        <input v-model.trim="field.optionsApi" type="text" placeholder="/users/lookup" @input="handleVisualSchemaChange" />
                      </label>

                      <label v-if="section.key !== 'summaryFields' && ['select', 'radio'].includes(field.type)">
                        <span>选项缓存 Key</span>
                        <input v-model.trim="field.optionsKey" type="text" placeholder="approvers" @input="handleVisualSchemaChange" />
                      </label>

                      <label v-if="section.key !== 'summaryFields'">
                        <span>只读条件</span>
                        <input v-model.trim="field.readonlyWhen" type="text" placeholder="always / 表达式" @input="handleVisualSchemaChange" />
                      </label>

                      <label v-if="section.key === 'decisionFields'">
                        <span>审批结果</span>
                        <select v-model="field.outcome" @change="handleVisualSchemaChange">
                          <option value="">全部结果</option>
                          <option value="approved">仅同意</option>
                          <option value="rejected">仅退回</option>
                        </select>
                      </label>

                      <label v-if="section.key !== 'summaryFields' && ['textarea'].includes(field.type)">
                        <span>文本行数</span>
                        <input v-model.number="field.rows" type="number" min="2" max="12" @input="handleVisualSchemaChange" />
                      </label>
                    </div>

                    <div v-if="section.key !== 'summaryFields'" class="validator-panel">
                      <div class="schema-section-head compact-head">
                        <div>
                          <strong>校验规则</strong>
                          <p>按字段维度维护必填、长度、数值和跨字段校验。</p>
                        </div>
                        <button type="button" class="secondary-btn mini-btn" @click="addValidator(field)">新增校验</button>
                      </div>

                      <div v-if="field.validators.length" class="validator-list">
                        <article v-for="validator in field.validators" :key="validator.id" class="validator-card">
                          <div class="validator-grid">
                            <label>
                              <span>类型</span>
                              <select v-model="validator.type" @change="handleVisualSchemaChange">
                                <option v-for="option in VALIDATOR_TYPES" :key="option.value" :value="option.value">
                                  {{ option.label }}
                                </option>
                              </select>
                            </label>
                            <label>
                              <span>提示语</span>
                              <input v-model.trim="validator.message" type="text" placeholder="请输入校验失败提示" @input="handleVisualSchemaChange" />
                            </label>
                            <label v-if="validatorNeedsValue(validator.type)">
                              <span>阈值</span>
                              <input v-model="validator.value" type="text" placeholder="4 / 30" @input="handleVisualSchemaChange" />
                            </label>
                            <label v-if="validator.type === 'afterField'">
                              <span>关联字段</span>
                              <input v-model.trim="validator.field" type="text" placeholder="startDate" @input="handleVisualSchemaChange" />
                            </label>
                            <label v-if="validator.type === 'pattern'">
                              <span>正则表达式</span>
                              <input v-model.trim="validator.pattern" type="text" placeholder="^[A-Z]{2}\\d+$" @input="handleVisualSchemaChange" />
                            </label>
                          </div>

                          <div class="inline-actions">
                            <button type="button" class="danger-btn mini-btn" @click="removeValidator(field, validator.id)">删除规则</button>
                          </div>
                        </article>
                      </div>
                      <div v-else class="empty-state mini-empty-state">当前字段还没有配置校验规则。</div>
                    </div>
                  </article>

                  <div v-if="!fieldEditor[section.key].length" class="empty-state mini-empty-state">
                    当前分组还没有字段，点右上角新增一个。
                  </div>
                </div>

                <div v-if="collapsedSections[section.key]" class="schema-section-collapsed">
                  <span>当前分组已收起</span>
                  <strong>{{ fieldEditor[section.key].length }} 项字段</strong>
                </div>
              </section>
            </article>

            <article class="panel-card schema-preview-pane">
              <div class="panel-head schema-preview-head">
                <div>
                  <p class="eyebrow">Schema Preview</p>
                  <h3>结构预览与 JSON</h3>
                </div>
                <span class="meta-pill">字段 {{ schemaFieldCount }}</span>
              </div>

              <section class="detail-grid compact-detail-grid schema-summary-grid">
                <article class="detail-item schema-summary-card">
                  <span class="detail-key">表单 Key</span>
                  <strong class="detail-value">{{ previewSchema.key || form.formKey || '-' }}</strong>
                </article>
                <article class="detail-item schema-summary-card">
                  <span class="detail-key">标题</span>
                  <strong class="detail-value">{{ previewSchema.title || form.title || '-' }}</strong>
                </article>
                <article class="detail-item schema-summary-card">
                  <span class="detail-key">发起字段</span>
                  <strong class="detail-value">{{ (previewSchema.fields || []).length }}</strong>
                </article>
                <article class="detail-item schema-summary-card">
                  <span class="detail-key">审批字段</span>
                  <strong class="detail-value">{{ (previewSchema.decisionFields || []).length }}</strong>
                </article>
              </section>

              <div v-if="schemaParseError" class="empty-state">
                当前 JSON 还不能被预览，请先修正格式。
              </div>
              <template v-else>
                <div class="stack-list schema-preview-list">
                  <div class="list-item rich-list-item schema-preview-card" v-for="field in previewFieldCards" :key="field.id">
                    <div>
                      <strong>{{ field.label }}</strong>
                      <p>{{ field.meta }}</p>
                    </div>
                    <span class="status-pill">{{ field.group }}</span>
                  </div>
                  <div v-if="!previewFieldCards.length" class="empty-state">当前 schema 还没有配置字段。</div>
                </div>
              </template>

              <section class="json-editor-field">
                <div class="json-editor-head">
                  <div>
                    <span class="json-editor-title">Schema JSON</span>
                    <p class="json-editor-caption">保留高级配置编辑入口，保存前建议先格式化一次。</p>
                  </div>
                  <div class="json-editor-actions">
                    <span class="meta-pill">{{ schemaJsonLineCount }} 行</span>
                    <button type="button" class="secondary-btn mini-btn" @click="beautifySchema">格式化 JSON</button>
                  </div>
                </div>
                <textarea
                  v-model="form.schemaJson"
                  rows="22"
                  class="schema-editor"
                  placeholder='{"fields":[{"key":"reason","label":"原因","type":"textarea"}]}'
                ></textarea>
              </section>

              <p :class="['schema-state', schemaParseError ? 'schema-state-error' : 'schema-state-ok']">
                {{ schemaParseError || `JSON 校验通过，已识别 ${schemaFieldCount} 个字段` }}
              </p>

              <div class="action-row schema-footer-actions">
                <button type="submit" class="primary-btn" :disabled="loading || Boolean(schemaParseError)">
                  {{ loading ? '保存中...' : form.id ? '保存更新' : '创建目录' }}
                </button>
                <button type="button" class="secondary-btn" @click="resetForm">重置</button>
              </div>
            </article>
          </section>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { http } from '../api/http'
import { ensureRemoteFormRegistryLoaded } from '../utils/businessForms'

const FIELD_TYPES = [
  { value: 'text', label: '单行文本' },
  { value: 'textarea', label: '多行文本' },
  { value: 'number', label: '数字' },
  { value: 'select', label: '下拉选择' },
  { value: 'radio', label: '单选' },
  { value: 'datetime-local', label: '日期时间' },
  { value: 'date', label: '日期' }
]

const VALIDATOR_TYPES = [
  { value: 'required', label: '必填' },
  { value: 'minLength', label: '最小长度' },
  { value: 'maxLength', label: '最大长度' },
  { value: 'min', label: '最小值' },
  { value: 'max', label: '最大值' },
  { value: 'positiveInteger', label: '正整数' },
  { value: 'afterField', label: '晚于字段' },
  { value: 'pattern', label: '正则匹配' }
]

const schemaSections = [
  {
    key: 'fields',
    title: '发起字段',
    shortTitle: '发起字段',
    description: '用于流程发起页填写业务数据。'
  },
  {
    key: 'summaryFields',
    title: '摘要字段',
    shortTitle: '摘要字段',
    description: '用于待办、监控和快照中的只读展示。'
  },
  {
    key: 'decisionFields',
    title: '审批字段',
    shortTitle: '审批字段',
    description: '用于待办审批页录入审批意见、退回原因等。'
  }
]

const loading = ref(false)
const dialogVisible = ref(false)
const catalogs = ref([])
const statusMessage = ref('')
const errorMessage = ref('')
const dialogStatusMessage = ref('')
const dialogErrorMessage = ref('')
const keyword = ref('')
const statusFilter = ref('')
const page = ref(1)
const size = ref(6)
const total = ref(0)

const form = reactive({
  id: null,
  formKey: '',
  title: '',
  description: '',
  status: 1,
  schemaJson: '{\n  "fields": []\n}'
})

const fieldEditor = reactive({
  fields: [],
  summaryFields: [],
  decisionFields: []
})
const collapsedSections = reactive({
  fields: false,
  summaryFields: true,
  decisionFields: true
})

let editorFieldSeed = 0
let validatorSeed = 0

const enabledCount = computed(() => catalogs.value.filter((item) => item.status === 1).length)
const disabledCount = computed(() => catalogs.value.filter((item) => item.status !== 1).length)
const parsedSchema = computed(() => {
  try {
    return JSON.parse(form.schemaJson || '{}')
  } catch (error) {
    return null
  }
})
const schemaParseError = computed(() => {
  if (!form.schemaJson.trim()) {
    return 'Schema JSON 不能为空'
  }
  try {
    JSON.parse(form.schemaJson)
    return ''
  } catch (error) {
    return error.message || 'Schema JSON 不是合法的 JSON'
  }
})
const previewSchema = computed(() => parsedSchema.value || {})
const schemaFieldCount = computed(() =>
  (previewSchema.value.fields || []).length
  + (previewSchema.value.summaryFields || []).length
  + (previewSchema.value.decisionFields || []).length
)
const schemaJsonLineCount = computed(() => {
  const source = form.schemaJson || ''
  return source ? source.split('\n').length : 0
})
const previewFieldCards = computed(() => {
  if (!previewSchema.value || schemaParseError.value) {
    return []
  }
  const groups = [
    { key: 'fields', label: '发起字段' },
    { key: 'summaryFields', label: '摘要字段' },
    { key: 'decisionFields', label: '审批字段' }
  ]
  return groups.flatMap((group) =>
    (previewSchema.value[group.key] || []).map((field, index) => ({
      id: `${group.key}-${field.key || index}`,
      label: field.label || field.key || `字段 ${index + 1}`,
      group: group.label,
      meta: [
        field.key ? `key: ${field.key}` : '',
        field.type ? `type: ${field.type}` : group.key === 'summaryFields' ? 'type: readonly' : 'type: text',
        field.outcome ? `outcome: ${field.outcome}` : '',
        field.optionsApi ? `optionsApi: ${field.optionsApi}` : '',
        field.placeholder ? `placeholder: ${field.placeholder}` : '',
        field.help ? `help: ${field.help}` : ''
      ]
        .filter(Boolean)
        .join(' · ')
    }))
  )
})

const SCHEMA_TEMPLATES = {
  start: {
    fields: [
      {
        key: 'applicant',
        label: '申请人',
        type: 'text',
        readonlyWhen: 'always',
        help: '当前登录用户会自动作为流程发起人。',
        validator: [{ type: 'required', message: '申请人不能为空' }]
      },
      {
        key: 'reason',
        label: '申请原因',
        type: 'textarea',
        span: 2,
        placeholder: '请输入申请原因',
        help: '原因越清晰，审批越容易处理。'
      }
    ]
  },
  approval: {
    summaryFields: [
      { key: 'applicant', label: '申请人' },
      { key: 'reason', label: '申请原因', span: 2 }
    ],
    decisionFields: [
      {
        key: 'approvalComment',
        label: '审批意见',
        type: 'textarea',
        rows: 5,
        span: 2,
        placeholder: '请输入同意审批的说明或补充意见',
        outcome: 'approved',
        validator: [{ type: 'required', message: '审批意见不能为空' }]
      },
      {
        key: 'rejectReason',
        label: '退回原因',
        type: 'textarea',
        rows: 5,
        span: 2,
        placeholder: '请输入驳回原因，便于申请人补充或修正',
        outcome: 'rejected',
        validator: [{ type: 'required', message: '退回原因不能为空' }]
      }
    ]
  },
  summary: {
    summaryFields: [
      { key: 'applicant', label: '申请人' },
      { key: 'businessKey', label: '业务键' },
      { key: 'processInstanceId', label: '流程实例' },
      { key: 'reason', label: '业务说明', span: 2 }
    ]
  }
}

function nextEditorFieldId() {
  editorFieldSeed += 1
  return `field-${editorFieldSeed}`
}

function nextValidatorId() {
  validatorSeed += 1
  return `validator-${validatorSeed}`
}

function createValidator(validator = {}) {
  return {
    id: nextValidatorId(),
    type: validator.type || 'required',
    message: validator.message || '',
    value: validator.value ?? '',
    field: validator.field || '',
    pattern: validator.pattern || ''
  }
}

function createEditorField(sectionKey, source = {}) {
  return {
    id: nextEditorFieldId(),
    key: source.key || '',
    label: source.label || '',
    type: source.type || 'text',
    span: Number(source.span) === 2 ? 2 : 1,
    rows: source.rows ? Number(source.rows) : 4,
    placeholder: source.placeholder || '',
    help: source.help || '',
    optionsApi: source.optionsApi || '',
    optionsKey: source.optionsKey || '',
    readonlyWhen: typeof source.readonlyWhen === 'string' ? source.readonlyWhen : '',
    outcome: source.outcome || '',
    formatter: source.formatter || '',
    validators: Array.isArray(source.validator) ? source.validator.map((item) => createValidator(item)) : []
  }
}

function replaceEditorSection(sectionKey, items = []) {
  fieldEditor[sectionKey].splice(0, fieldEditor[sectionKey].length, ...items)
}

function loadVisualEditorFromSchema(schema = {}) {
  replaceEditorSection('fields', (schema.fields || []).map((item) => createEditorField('fields', item)))
  replaceEditorSection('summaryFields', (schema.summaryFields || []).map((item) => createEditorField('summaryFields', item)))
  replaceEditorSection('decisionFields', (schema.decisionFields || []).map((item) => createEditorField('decisionFields', item)))
}

function cleanString(value) {
  return typeof value === 'string' ? value.trim() : ''
}

function validatorNeedsValue(type) {
  return ['minLength', 'maxLength', 'min', 'max'].includes(type)
}

function serializeValidator(validator) {
  const type = cleanString(validator.type)
  if (!type) {
    return null
  }
  const payload = { type }
  const message = cleanString(validator.message)
  if (message) {
    payload.message = message
  }
  if (validatorNeedsValue(type) && `${validator.value}`.trim()) {
    const rawValue = `${validator.value}`.trim()
    payload.value = Number.isNaN(Number(rawValue)) ? rawValue : Number(rawValue)
  }
  if (type === 'afterField' && cleanString(validator.field)) {
    payload.field = cleanString(validator.field)
  }
  if (type === 'pattern' && cleanString(validator.pattern)) {
    payload.pattern = cleanString(validator.pattern)
  }
  return payload
}

function serializeEditorField(sectionKey, field) {
  const payload = {}
  const key = cleanString(field.key)
  const label = cleanString(field.label)
  if (key) {
    payload.key = key
  }
  if (label) {
    payload.label = label
  }
  if (sectionKey !== 'summaryFields') {
    payload.type = cleanString(field.type) || 'text'
  }
  if (field.span === 2) {
    payload.span = 2
  }
  if (sectionKey !== 'summaryFields' && payload.type === 'textarea' && Number(field.rows) > 0) {
    payload.rows = Number(field.rows)
  }
  if (sectionKey !== 'summaryFields' && cleanString(field.placeholder)) {
    payload.placeholder = cleanString(field.placeholder)
  }
  if (cleanString(field.help)) {
    payload.help = cleanString(field.help)
  }
  if (sectionKey !== 'summaryFields' && cleanString(field.optionsApi)) {
    payload.optionsApi = cleanString(field.optionsApi)
  }
  if (sectionKey !== 'summaryFields' && cleanString(field.optionsKey)) {
    payload.optionsKey = cleanString(field.optionsKey)
  }
  if (sectionKey !== 'summaryFields' && cleanString(field.readonlyWhen)) {
    payload.readonlyWhen = cleanString(field.readonlyWhen)
  }
  if (sectionKey === 'summaryFields' && cleanString(field.formatter)) {
    payload.formatter = cleanString(field.formatter)
  }
  if (sectionKey === 'decisionFields' && cleanString(field.outcome)) {
    payload.outcome = cleanString(field.outcome)
  }
  if (sectionKey !== 'summaryFields') {
    const validators = field.validators.map((item) => serializeValidator(item)).filter(Boolean)
    if (validators.length) {
      payload.validator = validators
    }
  }
  return payload
}

function buildSchemaFromVisualEditor() {
  const schema = {}
  const key = cleanString(form.formKey)
  const title = cleanString(form.title)
  const description = cleanString(form.description)
  if (key) {
    schema.key = key
  }
  if (title) {
    schema.title = title
  }
  if (description) {
    schema.description = description
  }
  schemaSections.forEach((section) => {
    const items = fieldEditor[section.key]
      .map((field) => serializeEditorField(section.key, field))
      .filter((item) => Object.keys(item).length)
    if (items.length) {
      schema[section.key] = items
    }
  })
  if (!schema.fields && !schema.summaryFields && !schema.decisionFields) {
    schema.fields = []
  }
  return schema
}

function handleVisualSchemaChange() {
  syncVisualSchemaToJson(true)
}

function syncVisualSchemaToJson(silent = true) {
  form.schemaJson = `${JSON.stringify(buildSchemaFromVisualEditor(), null, 2)}\n`
  if (!silent) {
    dialogStatusMessage.value = '可视化字段配置已回写到 JSON'
    dialogErrorMessage.value = ''
  }
}

function loadJsonToVisualEditor() {
  if (schemaParseError.value) {
    dialogErrorMessage.value = schemaParseError.value
    return
  }
  loadVisualEditorFromSchema(parsedSchema.value || {})
  dialogStatusMessage.value = '已从 JSON 同步到可视化编辑器'
  dialogErrorMessage.value = ''
}

function addEditorField(sectionKey) {
  fieldEditor[sectionKey].push(createEditorField(sectionKey))
  handleVisualSchemaChange()
}

function toggleSection(sectionKey) {
  collapsedSections[sectionKey] = !collapsedSections[sectionKey]
}

function removeEditorField(sectionKey, fieldId) {
  const index = fieldEditor[sectionKey].findIndex((item) => item.id === fieldId)
  if (index === -1) {
    return
  }
  fieldEditor[sectionKey].splice(index, 1)
  handleVisualSchemaChange()
}

function moveEditorField(sectionKey, index, offset) {
  const targetIndex = index + offset
  if (targetIndex < 0 || targetIndex >= fieldEditor[sectionKey].length) {
    return
  }
  const [item] = fieldEditor[sectionKey].splice(index, 1)
  fieldEditor[sectionKey].splice(targetIndex, 0, item)
  handleVisualSchemaChange()
}

function addValidator(field) {
  field.validators.push(createValidator())
  handleVisualSchemaChange()
}

function removeValidator(field, validatorId) {
  const index = field.validators.findIndex((item) => item.id === validatorId)
  if (index === -1) {
    return
  }
  field.validators.splice(index, 1)
  handleVisualSchemaChange()
}

function formatDate(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

function resetMessages() {
  statusMessage.value = ''
  errorMessage.value = ''
}

function resetDialogMessages() {
  dialogStatusMessage.value = ''
  dialogErrorMessage.value = ''
}

function resetForm() {
  form.id = null
  form.formKey = ''
  form.title = ''
  form.description = ''
  form.status = 1
  form.schemaJson = '{\n  "fields": []\n}'
  loadVisualEditorFromSchema({ fields: [] })
}

function resetFilters() {
  keyword.value = ''
  statusFilter.value = ''
  page.value = 1
  searchCatalogs()
}

function openCreate() {
  resetMessages()
  resetDialogMessages()
  resetForm()
  dialogVisible.value = true
}

function closeDialog() {
  dialogVisible.value = false
  resetDialogMessages()
}

function editCatalog(catalog) {
  resetMessages()
  resetDialogMessages()
  form.id = catalog.id
  form.formKey = catalog.formKey
  form.title = catalog.title
  form.description = catalog.description || ''
  form.status = catalog.status ?? 1
  form.schemaJson = catalog.schemaJson || '{\n  "fields": []\n}'
  loadVisualEditorFromSchema(parsedSchema.value || {})
  dialogVisible.value = true
}

function beautifySchema() {
  try {
    form.schemaJson = `${JSON.stringify(JSON.parse(form.schemaJson || '{}'), null, 2)}\n`
    dialogStatusMessage.value = 'Schema JSON 已格式化'
    dialogErrorMessage.value = ''
    return true
  } catch (error) {
    dialogErrorMessage.value = 'Schema JSON 不是合法的 JSON'
    return false
  }
}

function applySchemaTemplate(type) {
  const template = SCHEMA_TEMPLATES[type]
  if (!template) {
    return
  }
  const nextSchema = {
    key: form.formKey || '',
    title: form.title || '',
    description: form.description || '',
    ...template
  }
  form.schemaJson = `${JSON.stringify(nextSchema, null, 2)}\n`
  loadVisualEditorFromSchema(nextSchema)
  resetDialogMessages()
}

async function searchCatalogs() {
  resetMessages()
  const params = {
    page: page.value,
    size: size.value
  }
  if (keyword.value) {
    params.keyword = keyword.value
  }
  if (statusFilter.value !== '') {
    params.status = Number(statusFilter.value)
  }
  try {
    const { data } = await http.get('/form-catalogs/query', { params })
    catalogs.value = data?.items || []
    total.value = data?.total || 0
  } catch (error) {
    catalogs.value = []
    total.value = 0
    errorMessage.value = error.normalizedMessage || error.message || '表单目录加载失败'
  }
}

async function submitCatalog() {
  resetMessages()
  resetDialogMessages()
  if (!beautifySchema()) {
    return
  }
  loading.value = true
  try {
    const payload = {
      formKey: form.formKey,
      title: form.title,
      description: form.description,
      status: form.status,
      schemaJson: form.schemaJson
    }
    if (form.id) {
      await http.put(`/form-catalogs/${form.id}`, payload)
      statusMessage.value = '表单目录已更新'
    } else {
      await http.post('/form-catalogs', payload)
      statusMessage.value = '表单目录已创建'
    }
    dialogVisible.value = false
    await Promise.all([searchCatalogs(), ensureRemoteFormRegistryLoaded(true)])
  } catch (error) {
    dialogErrorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

async function removeCatalog(catalog) {
  if (!window.confirm(`确认删除表单目录“${catalog.title}”吗？`)) {
    return
  }
  resetMessages()
  try {
    await http.delete(`/form-catalogs/${catalog.id}`)
    statusMessage.value = '表单目录已删除'
    await Promise.all([searchCatalogs(), ensureRemoteFormRegistryLoaded(true)])
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  }
}

function changePage(nextPage) {
  page.value = nextPage
  searchCatalogs()
}

function changeSize() {
  page.value = 1
  searchCatalogs()
}

onMounted(async () => {
  try {
    resetForm()
    await Promise.all([searchCatalogs(), ensureRemoteFormRegistryLoaded(true)])
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '表单目录初始化失败'
  }
})
</script>

<style scoped>
.schema-modal {
  width: min(1080px, 72vw);
  max-height: calc(100vh - 72px);
}

.schema-editor-modal {
  padding: 12px;
  border-radius: 10px;
  background: #f6f8fb;
  box-shadow: 0 10px 28px rgba(34, 53, 79, 0.12);
  overflow: auto;
}

.schema-modal-head {
  margin-bottom: 12px;
  padding: 0 2px 12px;
  border-bottom: 1px solid #e4eaf1;
}

.schema-close-btn {
  min-width: 64px;
}

.schema-workbench {
  align-items: start;
  grid-template-columns: minmax(0, 1.16fr) minmax(320px, 0.84fr);
  gap: 12px;
}

.schema-editor-pane,
.schema-preview-pane {
  border: 1px solid #dfe6ef;
  border-radius: 8px;
  background: #fff;
  box-shadow: none;
}

.schema-editor-pane {
  padding: 14px;
}

.schema-preview-pane {
  padding: 14px;
  background: #fff;
}

.schema-basic-grid {
  gap: 10px 12px;
  margin-bottom: 12px;
}

.nested-head {
  margin-top: 14px;
  margin-bottom: 8px;
  padding-top: 12px;
  border-top: 1px solid #e9eef5;
}

.schema-template-actions {
  margin-bottom: 4px;
  flex-wrap: wrap;
  gap: 6px;
  padding: 10px 12px;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
  background: #f8fafc;
}

.schema-editor-tip {
  margin: 0 0 12px;
  padding: 10px 12px;
  border: 1px solid #e8edf4;
  border-radius: 8px;
  background: #f8fafd;
  color: #6b7c93;
  font-size: 12px;
  line-height: 1.6;
}

.schema-section {
  border: 1px solid #e4ebf3;
  border-radius: 8px;
  padding: 12px;
  background: #fff;
  box-shadow: none;
}

.schema-section + .schema-section {
  margin-top: 10px;
}

.schema-section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #eef2f7;
}

.schema-section-head strong {
  display: block;
  margin-bottom: 2px;
  font-size: 13px;
  color: #243445;
}

.schema-section-head p {
  margin: 0;
  font-size: 12px;
  color: #73839a;
}

.schema-section-collapsed {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 40px;
  padding: 8px 10px;
  border: 1px dashed #d8e1ec;
  border-radius: 8px;
  background: #fafbfd;
  color: #718195;
  font-size: 12px;
}

.schema-section-collapsed strong {
  color: #243445;
  font-size: 12px;
  font-weight: 600;
}

.compact-head {
  margin-bottom: 10px;
}

.field-editor-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-editor-card {
  border: 1px solid #e4ebf3;
  border-radius: 8px;
  padding: 10px;
  background: #fff;
  box-shadow: none;
}

.field-editor-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f4f8;
}

.field-editor-card-head strong {
  color: #243445;
  font-size: 13px;
  font-weight: 600;
}

.schema-field-grid,
.validator-grid {
  gap: 10px 12px;
}

.validator-panel {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #d8e1ec;
}

.validator-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.validator-card {
  border: 1px solid #e5ebf3;
  border-radius: 8px;
  padding: 10px;
  background: #fafbfd;
}

.mini-empty-state {
  min-height: 64px;
  font-size: 12px;
  border: 1px dashed #d8e1ec;
  border-radius: 8px;
  background: #fafbfd;
}

.json-editor-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #e4ebf3;
  border-radius: 8px;
  background: #fff;
}

.json-editor-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.json-editor-title {
  display: block;
  color: #243445;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.25;
}

.json-editor-caption {
  margin: 3px 0 0;
  color: #7b8a9b;
  font-size: 11px;
  line-height: 1.45;
}

.json-editor-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.schema-preview-head {
  margin-bottom: 10px;
}

.schema-summary-grid {
  gap: 8px;
  margin-bottom: 12px;
}

.schema-summary-card {
  min-height: 78px;
  padding: 12px;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
  background: #fafbfd;
  box-shadow: none;
}

.schema-preview-list {
  gap: 8px;
  max-height: 360px;
  padding-right: 2px;
  overflow: auto;
}

.schema-preview-card {
  align-items: flex-start;
  padding: 12px;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
  background: #fff;
  box-shadow: none;
}

.schema-preview-card strong {
  color: #1e293b;
}

.schema-preview-card p {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.55;
  word-break: break-word;
}

.schema-editor {
  min-height: 400px;
  padding: 12px 14px;
  border: 1px solid #dfe6ef;
  border-radius: 8px;
  background: #fbfcfe;
  font-family: "SFMono-Regular", "JetBrains Mono", "Fira Code", monospace;
  font-size: 12px;
  line-height: 1.5;
  color: #334155;
  resize: vertical;
}

.schema-state {
  margin: 10px 0 0;
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 12px;
}

.schema-state-ok {
  color: #2f7d32;
  background: rgba(228, 243, 231, 0.92);
}

.schema-state-error {
  color: #9f2f2f;
  background: rgba(252, 234, 234, 0.96);
}

.schema-footer-actions {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e6edf5;
}

.full-width-field {
  grid-column: 1 / -1;
}

@media (max-width: 1180px) {
  .schema-modal {
    width: min(980px, 88vw);
  }

  .schema-editor-modal {
    padding: 12px;
  }

  .schema-workbench {
    grid-template-columns: minmax(0, 1.08fr) minmax(300px, 0.92fr);
  }
}

@media (max-width: 900px) {
  .schema-modal {
    width: min(92vw, 92vw);
    max-height: calc(100vh - 36px);
  }

  .schema-workbench {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .schema-editor-pane,
  .schema-preview-pane {
    padding: 12px;
  }

  .json-editor-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .schema-summary-card {
    min-height: auto;
  }

  .schema-preview-list {
    max-height: none;
  }
}
</style>
