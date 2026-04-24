<template>
  <div v-if="visible" class="modal-mask" @click.self="emit('close')">
    <div class="modal-panel form-modal">
      <div class="panel-head">
        <div>
          <p class="eyebrow">{{ eyebrow }}</p>
          <h3>{{ title }}</h3>
        </div>
        <div class="inline-actions">
          <slot name="header-actions" />
          <button type="button" class="ghost-btn" @click="emit('close')">关闭</button>
        </div>
      </div>

      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  eyebrow: {
    type: String,
    default: 'Editor'
  },
  title: {
    type: String,
    default: '编辑'
  }
})

const emit = defineEmits(['close'])
</script>
