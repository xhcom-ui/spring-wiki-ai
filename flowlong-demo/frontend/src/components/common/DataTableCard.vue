<template>
  <article class="panel-card">
    <div class="panel-head">
      <div>
        <p class="eyebrow">{{ eyebrow }}</p>
        <h3>{{ title }}</h3>
      </div>
      <div class="inline-actions">
        <span v-if="countLabel" class="muted-text">{{ countLabel }}</span>
        <slot name="header-extra" />
      </div>
    </div>

    <slot />

    <AppPagination
      v-if="pagination"
      :page="page"
      :size="size"
      :total="total"
      :disabled="disabled"
      :size-options="sizeOptions"
      @update:page="emit('update:page', $event)"
      @update:size="emit('update:size', $event)"
    />
  </article>
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
  countLabel: {
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
  disabled: {
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
