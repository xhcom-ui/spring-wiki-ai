<template>
  <div class="form-stack">
    <label>
      <span>请假原因</span>
      <textarea :value="task.reason || ''" rows="4" readonly></textarea>
    </label>
    <label>
      <span>最终审批说明</span>
      <textarea
        v-model.trim="formData.finalDecisionNote"
        rows="3"
        placeholder="填写最终审批说明"
      ></textarea>
    </label>
    <label>
      <span>是否允许补充材料</span>
      <select v-model="formData.allowResubmit">
        <option :value="false">否</option>
        <option :value="true">是</option>
      </select>
    </label>
  </div>
</template>

<script setup>
defineProps({
  task: {
    type: Object,
    required: true
  },
  formData: {
    type: Object,
    required: true
  }
})
</script>
