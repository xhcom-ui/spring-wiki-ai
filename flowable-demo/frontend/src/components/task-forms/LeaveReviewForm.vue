<template>
  <div class="form-stack">
    <label>
      <span>请假原因</span>
      <textarea :value="task.reason || ''" rows="4" readonly></textarea>
    </label>
    <label>
      <span>审批节点标签</span>
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
