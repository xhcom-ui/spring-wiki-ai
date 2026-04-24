<template>
  <component :is="currentComponent" :task="task" :form-data="formData" :errors="errors" />
</template>

<script setup>
import { computed } from 'vue'
import { resolveTaskFormRegistration } from './formRegistry'
import SchemaDrivenTaskForm from './SchemaDrivenTaskForm.vue'

const props = defineProps({
  task: {
    type: Object,
    required: true
  },
  formData: {
    type: Object,
    required: true
  },
  errors: {
    type: Object,
    default: () => ({})
  }
})

const currentComponent = computed(() => {
  if (Array.isArray(props.task?.fieldSchema) && props.task.fieldSchema.length) {
    return SchemaDrivenTaskForm
  }
  return resolveTaskFormRegistration(props.task).component
})
</script>
