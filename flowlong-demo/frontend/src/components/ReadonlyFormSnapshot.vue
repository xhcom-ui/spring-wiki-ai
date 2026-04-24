<template>
  <div class="readonly-form-grid" :style="gridStyle">
    <article
      v-for="entry in entries"
      :key="entry.key"
      class="readonly-form-card"
      :class="{ 'readonly-form-card-wide': (entry.span || 1) > 1 }"
    >
      <span class="detail-key">{{ entry.label }}</span>
      <strong class="detail-value">{{ entry.value }}</strong>
      <small v-if="entry.help" class="readonly-form-help">{{ entry.help }}</small>
    </article>
    <div v-if="!entries.length" class="empty-state readonly-form-empty">{{ emptyText }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { buildReadonlyEntries } from '../utils/businessForms'

const props = defineProps({
  formKey: {
    type: String,
    default: ''
  },
  source: {
    type: Object,
    default: () => ({})
  },
  includeRest: {
    type: Boolean,
    default: false
  },
  emptyText: {
    type: String,
    default: '没有可展示的表单快照'
  },
  columns: {
    type: Number,
    default: 2
  },
  extraLabels: {
    type: Object,
    default: () => ({})
  },
  context: {
    type: Object,
    default: () => ({})
  }
})

const entries = computed(() =>
  buildReadonlyEntries(props.formKey, props.source, {
    includeRest: props.includeRest,
    extraLabels: props.extraLabels,
    context: props.context
  })
)

const gridStyle = computed(() => ({
  '--readonly-columns': String(Math.max(1, props.columns || 2))
}))
</script>

<style scoped>
.readonly-form-grid {
  display: grid;
  grid-template-columns: repeat(var(--readonly-columns), minmax(0, 1fr));
  gap: 12px;
}

.readonly-form-card {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 252, 246, 0.72);
  border: 1px solid rgba(87, 69, 44, 0.08);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.readonly-form-card-wide {
  grid-column: span 2;
}

.readonly-form-help {
  color: #8a7152;
  font-size: 12px;
  line-height: 1.5;
}

.readonly-form-empty {
  grid-column: 1 / -1;
}

@media (max-width: 960px) {
  .readonly-form-grid {
    grid-template-columns: 1fr;
  }

  .readonly-form-card-wide {
    grid-column: auto;
  }
}
</style>
