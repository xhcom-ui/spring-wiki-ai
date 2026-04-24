<template>
  <section class="panel-card">
    <div class="panel-head">
      <div>
        <p class="eyebrow">{{ eyebrow }}</p>
        <h3>{{ title }}</h3>
      </div>
      <div v-if="$slots.meta" class="management-meta">
        <slot name="meta" />
      </div>
    </div>

    <div v-if="$slots.filters || $slots.actions" class="management-toolbar">
      <div v-if="$slots.filters" class="management-filters">
        <slot name="filters" />
      </div>
      <div v-if="$slots.actions" class="management-actions">
        <slot name="actions" />
      </div>
    </div>

    <p v-if="statusMessage" class="feedback success">{{ statusMessage }}</p>
    <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>

    <slot />

    <AppPagination
      v-if="pagination"
      :page="page"
      :size="size"
      :total="total"
      :disabled="loading"
      :size-options="sizeOptions"
      @update:page="emit('update:page', $event)"
      @update:size="emit('update:size', $event)"
    />
  </section>
</template>

<script setup>
import AppPagination from './AppPagination.vue'

defineProps({
  eyebrow: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    default: ''
  },
  statusMessage: {
    type: String,
    default: ''
  },
  errorMessage: {
    type: String,
    default: ''
  },
  page: {
    type: Number,
    default: 1
  },
  size: {
    type: Number,
    default: 10
  },
  total: {
    type: Number,
    default: 0
  },
  loading: {
    type: Boolean,
    default: false
  },
  pagination: {
    type: Boolean,
    default: true
  },
  sizeOptions: {
    type: Array,
    default: () => [8, 12, 20]
  }
})

const emit = defineEmits(['update:page', 'update:size'])
</script>
