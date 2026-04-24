<template>
  <section class="panel-card embedded-panel task-form-shell">
    <div class="panel-head compact-head">
      <div>
        <p class="eyebrow">Leave Form</p>
        <h3>请假单摘要</h3>
      </div>
      <span class="meta-pill">{{ task.formKey || 'leave-form' }}</span>
    </div>

    <div class="detail-grid compact-detail-grid">
      <div class="detail-item">
        <span class="detail-key">申请人</span>
        <strong class="detail-value">{{ task.applicant || normalizedVariables.applicant || '-' }}</strong>
      </div>
      <div class="detail-item">
        <span class="detail-key">请假天数</span>
        <strong class="detail-value">{{ normalizedVariables.days || '-' }}</strong>
      </div>
      <div class="detail-item">
        <span class="detail-key">开始时间</span>
        <strong class="detail-value">{{ normalizedVariables.startDate || '-' }}</strong>
      </div>
      <div class="detail-item">
        <span class="detail-key">结束时间</span>
        <strong class="detail-value">{{ normalizedVariables.endDate || '-' }}</strong>
      </div>
      <div class="detail-item">
        <span class="detail-key">部门经理</span>
        <strong class="detail-value">{{ normalizedVariables.deptManager || '-' }}</strong>
      </div>
      <div class="detail-item">
        <span class="detail-key">总经理</span>
        <strong class="detail-value">{{ normalizedVariables.generalManager || '-' }}</strong>
      </div>
      <div class="detail-item">
        <span class="detail-key">流程状态</span>
        <strong class="detail-value">{{ task.leaveStatus || normalizedVariables.leaveStatus || '-' }}</strong>
      </div>
      <div class="detail-item">
        <span class="detail-key">待办页</span>
        <strong class="detail-value">{{ task.todoPage || '-' }}</strong>
      </div>
    </div>

    <div class="detail-item">
      <span class="detail-key">请假原因</span>
      <strong class="detail-value">{{ normalizedVariables.reason || '-' }}</strong>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  task: {
    type: Object,
    required: true
  }
})

const normalizedVariables = computed(() => props.task?.variables || {})
</script>
