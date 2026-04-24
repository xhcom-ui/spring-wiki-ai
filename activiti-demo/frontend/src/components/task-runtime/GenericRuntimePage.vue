<template>
  <div class="page-grid">
    <section class="panel-card">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Runtime Workbench</p>
          <h3>{{ descriptor.title }}</h3>
        </div>
        <span class="meta-pill">{{ modeLabel }}</span>
      </div>

      <p class="muted-text">{{ descriptor.description }}</p>

      <section class="detail-grid compact-detail-grid">
        <div class="detail-item">
          <span class="detail-key">任务名称</span>
          <strong class="detail-value">{{ task.name || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span class="detail-key">表单 Key</span>
          <strong class="detail-value">{{ task.formKey || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span class="detail-key">待办页</span>
          <strong class="detail-value">{{ task.todoPage || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span class="detail-key">完成页</span>
          <strong class="detail-value">{{ task.donePage || '-' }}</strong>
        </div>
      </section>

      <section class="detail-item">
        <span class="detail-key">运行时变量</span>
        <div v-if="variableEntries.length" class="kv-list">
          <div v-for="[key, value] in variableEntries" :key="key" class="kv-row">
            <span class="kv-key">{{ key }}</span>
            <strong class="kv-value">{{ formatValue(value) }}</strong>
          </div>
        </div>
        <strong v-else class="detail-value">当前节点没有可展示的运行时变量</strong>
      </section>

      <template v-if="mode === 'todo'">
        <div v-if="task.canClaim" class="task-stage-card">
          <div>
            <p class="eyebrow">Claim First</p>
            <h3>当前任务待签收</h3>
          </div>
          <div class="inline-actions">
            <button type="button" class="primary-btn" :disabled="claiming" @click="$emit('claim')">
              {{ claiming ? '签收中...' : '签收当前任务' }}
            </button>
          </div>
        </div>

        <div v-else class="form-stack">
          <div class="segmented">
            <button
              type="button"
              :class="['segment-btn', { active: localOutcome === 'approved' }]"
              @click="localOutcome = 'approved'"
            >
              完成
            </button>
            <button
              type="button"
              :class="['segment-btn', { active: localOutcome === 'rejected' }]"
              @click="localOutcome = 'rejected'"
            >
              退回
            </button>
          </div>
          <label>
            <span>处理说明</span>
            <textarea v-model.trim="localComment" rows="6" placeholder="请输入处理说明"></textarea>
          </label>
          <div class="inline-actions">
            <button type="button" class="primary-btn" :disabled="submitting" @click="submit">
              {{ submitting ? '提交中...' : '提交处理结果' }}
            </button>
          </div>
        </div>
      </template>

      <section v-else class="detail-item">
        <span class="detail-key">办理回执</span>
        <strong class="detail-value">节点已完成，当前展示为通用回执页。</strong>
      </section>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  task: {
    type: Object,
    required: true
  },
  mode: {
    type: String,
    default: 'todo'
  },
  descriptor: {
    type: Object,
    required: true
  },
  submitting: {
    type: Boolean,
    default: false
  },
  claiming: {
    type: Boolean,
    default: false
  },
  initialComment: {
    type: String,
    default: ''
  },
  initialOutcome: {
    type: String,
    default: 'approved'
  }
})

const emit = defineEmits(['submit', 'claim', 'update:comment', 'update:outcome'])

const localComment = ref(props.initialComment)
const localOutcome = ref(props.initialOutcome)
const variableEntries = computed(() => Object.entries(props.task?.variables || {}))
const modeLabel = computed(() => (props.mode === 'done' ? '完成页' : '待办页'))

watch(
  () => props.initialComment,
  (value) => {
    localComment.value = value
  }
)

watch(
  () => props.initialOutcome,
  (value) => {
    localOutcome.value = value
  }
)

watch(localComment, (value) => emit('update:comment', value))
watch(localOutcome, (value) => emit('update:outcome', value))

function formatValue(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

function submit() {
  emit('submit', {
    comment: localComment.value,
    outcome: localOutcome.value
  })
}
</script>
