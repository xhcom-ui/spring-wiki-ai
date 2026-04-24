<template>
  <div class="form-stack">
    <label>
      <span>业务摘要</span>
      <textarea :value="task.reason || task.assignmentExpression || ''" rows="4" readonly></textarea>
    </label>
    <label>
      <span>节点标签</span>
      <input :value="[task.pageLabel, task.formLabel].filter(Boolean).join(' / ')" type="text" readonly />
    </label>
  </div>
</template>

<script setup>
defineProps({
  task: {
    type: Object,
    required: true
  }
})
</script>
