<template>
  <div class="pagination-bar">
    <div class="pagination-meta">
      <span>第 {{ page }} 页</span>
      <span>共 {{ total }} 条</span>
    </div>
    <div class="inline-actions">
      <label class="page-size-field">
        <span>每页</span>
        <select :value="size" @change="emit('update:size', Number($event.target.value))">
          <option v-for="option in sizeOptions" :key="option" :value="option">
            {{ option }}
          </option>
        </select>
      </label>
      <button type="button" class="secondary-btn mini-btn" :disabled="page <= 1 || disabled" @click="emit('update:page', page - 1)">
        上一页
      </button>
      <button
        type="button"
        class="secondary-btn mini-btn"
        :disabled="page * size >= total || disabled"
        @click="emit('update:page', page + 1)"
      >
        下一页
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
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
  sizeOptions: {
    type: Array,
    default: () => [8, 12, 20]
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:page', 'update:size'])
</script>
