<template>
  <section class="panel-card embedded-panel task-form-shell">
    <div class="panel-head compact-head">
      <div>
        <p class="eyebrow">Runtime Form</p>
        <h3>{{ title }}</h3>
      </div>
      <span class="meta-pill">{{ task.formKey || 'generic-task' }}</span>
    </div>

    <div class="detail-grid compact-detail-grid">
      <div class="detail-item">
        <span class="detail-key">待办页</span>
        <strong class="detail-value">{{ task.todoPage || '-' }}</strong>
      </div>
      <div class="detail-item">
        <span class="detail-key">办理完成页</span>
        <strong class="detail-value">{{ task.donePage || '-' }}</strong>
      </div>
      <div class="detail-item">
        <span class="detail-key">任务定义 Key</span>
        <strong class="detail-value">{{ task.taskDefinitionKey || '-' }}</strong>
      </div>
      <div class="detail-item">
        <span class="detail-key">当前处理人</span>
        <strong class="detail-value">{{ task.assignee || '待签收' }}</strong>
      </div>
    </div>

    <div class="detail-item">
      <span class="detail-key">运行时变量</span>
      <div v-if="variableEntries.length" class="kv-list">
        <div v-for="[key, value] in variableEntries" :key="key" class="kv-row">
          <span class="kv-key">{{ key }}</span>
          <strong class="kv-value">{{ formatValue(value) }}</strong>
        </div>
      </div>
      <strong v-else class="detail-value">当前节点没有可展示的运行时变量</strong>
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

const title = computed(() => {
  if (props.task?.todoPage) {
    return `${props.task.todoPage} 页面任务`
  }
  return '通用任务摘要'
})

const variableEntries = computed(() => Object.entries(props.task?.variables || {}))

function formatValue(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}
</script>
