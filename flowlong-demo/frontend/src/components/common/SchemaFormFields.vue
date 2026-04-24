<template>
  <div :class="layoutClass">
    <template v-for="field in fields" :key="field.key">
      <label v-if="!field.hidden">
        <span>{{ field.label }}</span>
        <textarea
          v-if="field.type === 'textarea'"
          :value="readValue(field)"
          :rows="field.rows || 4"
          :placeholder="field.placeholder || ''"
          :disabled="resolveDisabled(field)"
          @input="writeValue(field.key, $event.target.value)"
        ></textarea>
        <select
          v-else-if="field.type === 'select'"
          :value="readValue(field)"
          :disabled="resolveDisabled(field)"
          @change="writeValue(field.key, castValue(field, $event.target.value))"
        >
          <option
            v-for="option in normalizedOptions(field)"
            :key="String(option.value)"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>
        <select
          v-else-if="field.type === 'multiselect'"
          :value="Array.isArray(readValue(field)) ? readValue(field) : []"
          multiple
          :size="field.size || 8"
          :disabled="resolveDisabled(field)"
          @change="writeValue(field.key, readMultiple($event.target.selectedOptions, field))"
        >
          <option
            v-for="option in normalizedOptions(field)"
            :key="String(option.value)"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>
        <input
          v-else
          :type="field.type || 'text'"
          :value="readValue(field)"
          :placeholder="field.placeholder || ''"
          :min="field.min"
          :disabled="resolveDisabled(field)"
          @input="writeValue(field.key, castValue(field, $event.target.value))"
        />
      </label>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  model: {
    type: Object,
    required: true
  },
  fields: {
    type: Array,
    default: () => []
  },
  layout: {
    type: String,
    default: 'stack'
  }
})

const layoutClass = computed(() => (props.layout === 'grid' ? 'form-grid' : 'form-stack'))

function normalizedOptions(field) {
  return Array.isArray(field.options) ? field.options : []
}

function resolveDisabled(field) {
  return typeof field.disabled === 'function' ? Boolean(field.disabled(props.model)) : Boolean(field.disabled)
}

function readValue(field) {
  const value = props.model[field.key]
  return value == null ? (field.type === 'multiselect' ? [] : '') : value
}

function castValue(field, rawValue) {
  if (field.cast === 'number') {
    return rawValue === '' ? null : Number(rawValue)
  }
  return rawValue
}

function readMultiple(selectedOptions, field) {
  return Array.from(selectedOptions || []).map((option) => castValue(field, option.value))
}

function writeValue(key, value) {
  props.model[key] = value
}
</script>
