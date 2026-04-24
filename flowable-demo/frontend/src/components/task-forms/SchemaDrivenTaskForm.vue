<template>
  <div class="schema-form-grid">
    <label
      v-for="field in normalizedSchema"
      :key="field.field"
      :class="['schema-field', { 'span-2': field.span === 2 }]"
    >
      <span>
        {{ field.label || field.field }}
        <small v-if="field.required" class="required-mark">*</small>
      </span>

      <div v-if="readonly" class="schema-readonly">
        {{ formatFieldValue(field, formData[field.field]) }}
      </div>

      <textarea
        v-else-if="field.component === 'textarea'"
        v-model="formData[field.field]"
        :rows="field.rows || 3"
        :readonly="field.readonly"
        :placeholder="field.placeholder || ''"
      ></textarea>

      <select
        v-else-if="field.component === 'select'"
        v-model="formData[field.field]"
        :disabled="field.readonly"
      >
        <option
          v-for="option in resolveFieldOptions(field)"
          :key="String(option.value)"
          :value="normalizeOptionValue(option.value)"
        >
          {{ option.label ?? option.value }}
        </option>
      </select>

      <input
        v-else-if="field.component === 'switch'"
        v-model="formData[field.field]"
        type="checkbox"
        :disabled="field.readonly"
      />

      <input
        v-else
        v-model="formData[field.field]"
        :type="resolveInputType(field.component)"
        :readonly="field.readonly"
        :placeholder="field.placeholder || ''"
      />

      <small v-if="field.help" class="schema-help">{{ field.help }}</small>
      <small v-if="errors[field.field]" class="schema-error">{{ errors[field.field] }}</small>
    </label>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { http } from '../../api/http'

const props = defineProps({
  task: {
    type: Object,
    default: null
  },
  schema: {
    type: Array,
    default: () => []
  },
  formData: {
    type: Object,
    required: true
  },
  errors: {
    type: Object,
    default: () => ({})
  },
  readonly: {
    type: Boolean,
    default: false
  }
})

const remoteOptions = ref({})

const normalizedSchema = computed(() =>
  Array.isArray(props.schema) && props.schema.length
    ? normalizeSchemaList(props.schema)
    : Array.isArray(props.task?.fieldSchema)
      ? normalizeSchemaList(props.task.fieldSchema)
      : []
)

watch(
  normalizedSchema,
  async (fields) => {
    const next = {}
    for (const field of fields) {
      if (!field?.optionsApi) {
        continue
      }
      try {
        const { data } = await http.get(field.optionsApi)
        next[field.field] = normalizeRemoteOptions(field, data)
      } catch {
        next[field.field] = []
      }
    }
    remoteOptions.value = next
  },
  { immediate: true }
)

function normalizeSchemaList(schema) {
  return schema
    .filter((item) => item && item.field)
    .map((item) => ({
      span: Number(item.span) === 1 ? 1 : 2,
      rows: Number(item.rows) > 0 ? Number(item.rows) : 3,
      ...item
    }))
}

function resolveInputType(component) {
  if (component === 'number') {
    return 'number'
  }
  if (component === 'date') {
    return 'date'
  }
  if (component === 'datetime') {
    return 'datetime-local'
  }
  return 'text'
}

function normalizeOptionValue(value) {
  return typeof value === 'boolean' ? value : value ?? ''
}

function resolveFieldOptions(field) {
  if (Array.isArray(field.options) && field.options.length) {
    return field.options
  }
  return remoteOptions.value[field.field] || []
}

function normalizeRemoteOptions(field, payload) {
  const source = Array.isArray(payload)
    ? payload
    : Array.isArray(payload?.items)
      ? payload.items
      : []
  return source.map((item) => {
    if (item && typeof item === 'object' && !Array.isArray(item)) {
      const valueKey = field.optionValueKey || 'id'
      const labelKey = field.optionLabelKey || 'name'
      return {
        value: item[valueKey] ?? item.username ?? item.code ?? item.id,
        label: item[labelKey] ?? item.nickname ?? item.name ?? item.label ?? item.username ?? item.code ?? item.id
      }
    }
    return { value: item, label: item }
  })
}

function formatFieldValue(field, rawValue) {
  if (rawValue === null || rawValue === undefined || rawValue === '') {
    return '-'
  }
  if (typeof rawValue === 'boolean') {
    return rawValue ? '是' : '否'
  }
  if (field.component === 'select') {
    const option = resolveFieldOptions(field).find((item) => item.value === rawValue)
    return option?.label ?? String(rawValue)
  }
  if (typeof rawValue === 'object') {
    return JSON.stringify(rawValue)
  }
  return String(rawValue)
}
</script>

<style scoped>
.schema-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.schema-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.schema-field.span-2 {
  grid-column: 1 / -1;
}

.required-mark {
  color: #bb5f00;
  margin-left: 4px;
}

.schema-help {
  color: #7a664c;
  font-size: 12px;
}

.schema-error {
  color: #b42318;
  font-size: 12px;
}

.schema-readonly {
  min-height: 42px;
  padding: 10px 12px;
  border: 1px solid rgba(121, 101, 74, 0.18);
  border-radius: 12px;
  background: rgba(255, 252, 246, 0.92);
  color: #4f3f2a;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 900px) {
  .schema-form-grid {
    grid-template-columns: 1fr;
  }

  .schema-field.span-2 {
    grid-column: auto;
  }
}
</style>
