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
          <span class="detail-key">申请人</span>
          <strong class="detail-value">{{ task.applicant || variables.applicant || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span class="detail-key">表单 Key</span>
          <strong class="detail-value">{{ task.formKey || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span class="detail-key">待办页</span>
          <strong class="detail-value">{{ task.todoPage || '-' }}</strong>
        </div>
      </section>

      <section class="detail-grid compact-detail-grid">
        <div class="detail-item">
          <span class="detail-key">请假天数</span>
          <strong class="detail-value">{{ variables.days || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span class="detail-key">开始时间</span>
          <strong class="detail-value">{{ variables.startDate || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span class="detail-key">结束时间</span>
          <strong class="detail-value">{{ variables.endDate || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span class="detail-key">当前状态</span>
          <strong class="detail-value">{{ task.leaveStatus || variables.leaveStatus || '-' }}</strong>
        </div>
      </section>

      <label class="form-stack">
        <span>请假原因</span>
        <textarea :value="variables.reason || '-'" rows="4" readonly></textarea>
      </label>

      <template v-if="mode === 'todo'">
        <div v-if="task.canClaim" class="task-stage-card">
          <div>
            <p class="eyebrow">Claim First</p>
            <h3>先签收再处理</h3>
          </div>
          <p class="muted-text">该任务当前处于候选状态。签收后才会进入真正审批动作。</p>
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
              同意
            </button>
            <button
              type="button"
              :class="['segment-btn', { active: localOutcome === 'rejected' }]"
              @click="localOutcome = 'rejected'"
            >
              驳回
            </button>
          </div>

          <label>
            <span>审批意见</span>
            <textarea v-model.trim="localComment" rows="6" placeholder="请输入审批意见"></textarea>
          </label>

          <div class="inline-actions">
            <button type="button" class="primary-btn" :disabled="submitting" @click="submit">
              {{ submitting ? '提交中...' : `确认${localOutcome === 'approved' ? '同意' : '驳回'}` }}
            </button>
          </div>
        </div>
      </template>

      <section v-else class="detail-item">
        <span class="detail-key">办理回执</span>
        <strong class="detail-value">该节点已完成，可以继续回到任务中心或查看流程监控状态。</strong>
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

const variables = computed(() => props.task?.variables || {})
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

function submit() {
  emit('submit', {
    comment: localComment.value,
    outcome: localOutcome.value
  })
}
</script>
