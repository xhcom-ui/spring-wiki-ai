<template>
  <div class="form-stack">
    <label>
      <span>请假原因</span>
      <textarea :value="task.reason || ''" rows="4" readonly></textarea>
    </label>
    <label>
      <span>经理审批建议</span>
      <textarea
        v-model.trim="formData.managerAdvice"
        rows="3"
        placeholder="填写经理审批建议，会随流程变量一起提交"
      ></textarea>
    </label>
    <label>
      <span>是否建议加急</span>
      <select v-model="formData.priority">
        <option value="normal">普通</option>
        <option value="high">高优先级</option>
        <option value="urgent">加急</option>
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
