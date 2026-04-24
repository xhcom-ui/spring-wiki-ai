<template>
  <div
    v-if="isSimpleNode"
    ref="wrapperRef"
    class="sf-chain-node"
    :class="{ 'sf-chain-node-end': isEndNode }"
  >
    <article
      :class="['sf-node-card', `sf-node-${node.nodeType}`, { selected: isSelected }]"
      @click.stop="$emit('select-node', node)"
    >
      <header class="sf-node-head">
        <span class="sf-node-title">{{ node.nodeName }}</span>
        <button
          v-if="canRemove"
          type="button"
          class="sf-node-close"
          @click.stop="$emit('remove-node', node)"
        >
          ×
        </button>
      </header>
      <p class="sf-node-type">{{ nodeTypeLabel }}</p>
      <p class="sf-node-summary">{{ node.value || defaultSummary }}</p>
      <div v-if="tagLabels.length" class="sf-node-tags">
        <button
          v-for="tag in tagLabels"
          :key="tag"
          type="button"
          class="sf-node-tag"
          @click.stop="$emit('tag-click', node)"
        >
          {{ tag }}
        </button>
      </div>
    </article>

    <div v-if="!readonly && !isEndNode" class="sf-connector">
      <button type="button" class="sf-add-trigger" @click.stop="toggleInsertMenu">
        +
      </button>
      <div v-if="showInsertMenu" class="sf-insert-menu">
        <button type="button" @click.stop="insertNode('between')">审批节点</button>
        <button type="button" @click.stop="insertNode('serial')">条件分支</button>
        <button type="button" @click.stop="insertNode('parallel')">并行分支</button>
      </div>
    </div>
    <div v-else-if="!isEndNode" class="sf-connector sf-connector-readonly"></div>

    <SeaFlowNode
      v-if="node.childNode"
      :node="node.childNode"
      :readonly="readonly"
      :selected-node-id="selectedNodeId"
      @select-node="$emit('select-node', $event)"
      @remove-node="$emit('remove-node', $event)"
      @insert-node="$emit('insert-node', $event)"
      @append-branch="$emit('append-branch', $event)"
      @tag-click="$emit('tag-click', $event)"
    />
  </div>

  <div v-else ref="wrapperRef" class="sf-gateway-shell">
    <article
      :class="['sf-node-card', 'sf-gateway-card', `sf-node-${node.nodeType}`, { selected: isSelected }]"
      @click.stop="$emit('select-node', node)"
    >
      <header class="sf-node-head">
        <span class="sf-node-title">{{ node.nodeName }}</span>
        <button
          v-if="canRemove"
          type="button"
          class="sf-node-close"
          @click.stop="$emit('remove-node', node)"
        >
          ×
        </button>
      </header>
      <p class="sf-node-type">{{ nodeTypeLabel }}</p>
      <p class="sf-node-summary">{{ defaultSummary }}</p>
    </article>

    <div class="sf-branch-toolbar" v-if="!readonly">
      <button
        type="button"
        class="sf-branch-add-btn"
        @click.stop="$emit('append-branch', { node, branchType: node.nodeType })"
      >
        {{ node.nodeType === 'serial' ? '添加条件' : '添加并行' }}
      </button>
    </div>

    <div class="sf-branch-grid" :class="{ parallel: node.nodeType === 'parallel' }">
      <div
        v-for="branch in node.conditionNodes || []"
        :key="branch.nodeId"
        class="sf-branch-column"
      >
        <SeaFlowNode
          :node="branch"
          :readonly="readonly"
          :selected-node-id="selectedNodeId"
          @select-node="$emit('select-node', $event)"
          @remove-node="$emit('remove-node', $event)"
          @insert-node="$emit('insert-node', $event)"
          @append-branch="$emit('append-branch', $event)"
          @tag-click="$emit('tag-click', $event)"
        />
      </div>
    </div>

    <div v-if="!readonly" class="sf-connector">
      <button type="button" class="sf-add-trigger" @click.stop="toggleInsertMenu">
        +
      </button>
      <div v-if="showInsertMenu" class="sf-insert-menu">
        <button type="button" @click.stop="insertNode('between')">审批节点</button>
        <button type="button" @click.stop="insertNode('serial')">条件分支</button>
        <button type="button" @click.stop="insertNode('parallel')">并行分支</button>
      </div>
    </div>
    <div v-else class="sf-connector sf-connector-readonly"></div>

    <SeaFlowNode
      v-if="node.childNode"
      :node="node.childNode"
      :readonly="readonly"
      :selected-node-id="selectedNodeId"
      @select-node="$emit('select-node', $event)"
      @remove-node="$emit('remove-node', $event)"
      @insert-node="$emit('insert-node', $event)"
      @append-branch="$emit('append-branch', $event)"
      @tag-click="$emit('tag-click', $event)"
    />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { getNodeTagLabels } from '../../utils/seaflowDesigner'

const props = defineProps({
  node: {
    type: Object,
    required: true
  },
  readonly: {
    type: Boolean,
    default: false
  },
  selectedNodeId: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['select-node', 'remove-node', 'insert-node', 'append-branch', 'tag-click'])

const showInsertMenu = ref(false)
const wrapperRef = ref(null)

const isEndNode = computed(() => props.node.nodeType === 'end')
const isSimpleNode = computed(() => !['serial', 'parallel'].includes(props.node.nodeType))
const isSelected = computed(() => props.selectedNodeId === props.node.nodeId)
const tagLabels = computed(() => getNodeTagLabels(props.node))

const canRemove = computed(() => !props.readonly && !['start', 'end'].includes(props.node.nodeType))

const nodeTypeLabel = computed(() => {
  switch (props.node.nodeType) {
    case 'start':
      return '开始节点'
    case 'between':
      return '审批节点'
    case 'serial':
      return '条件网关'
    case 'serial-node':
      return '条件分支'
    case 'parallel':
      return '并行网关'
    case 'end':
      return '结束节点'
    default:
      return '流程节点'
  }
})

const defaultSummary = computed(() => {
  switch (props.node.nodeType) {
    case 'start':
      return '设置发起人、表单入口和业务标签'
    case 'between':
      return '设置办理人、候选人和表单'
    case 'serial':
      return '按条件表达式决定分支去向'
    case 'serial-node':
      return '设置条件描述与表达式'
    case 'parallel':
      return '并行执行多个审批节点'
    case 'end':
      return '流程结束'
    default:
      return '节点配置'
  }
})

function toggleInsertMenu() {
  showInsertMenu.value = !showInsertMenu.value
}

function closeInsertMenu() {
  showInsertMenu.value = false
}

function insertNode(nodeType) {
  closeInsertMenu()
  if (props.readonly) {
    return
  }
  emit('insert-node', { targetNode: props.node, nodeType })
}

function handleDocumentPointerDown(event) {
  if (!showInsertMenu.value || !wrapperRef.value) {
    return
  }
  if (!wrapperRef.value.contains(event.target)) {
    closeInsertMenu()
  }
}

onMounted(() => {
  document.addEventListener('pointerdown', handleDocumentPointerDown)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', handleDocumentPointerDown)
})
</script>

<style scoped>
.sf-chain-node,
.sf-gateway-shell {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.sf-chain-node-end {
  padding-bottom: 20px;
}

.sf-node-card {
  width: 210px;
  min-height: 112px;
  border-radius: 24px;
  border: 1px solid rgba(77, 98, 127, 0.14);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(245, 248, 252, 0.94));
  box-shadow: 0 22px 54px rgba(24, 40, 61, 0.08);
  backdrop-filter: blur(16px);
  padding: 18px 18px 16px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.sf-node-card:hover {
  transform: translateY(-2px);
  border-color: rgba(31, 61, 102, 0.22);
  box-shadow: 0 26px 62px rgba(19, 34, 57, 0.12);
}

.sf-node-card.selected {
  border-color: rgba(37, 78, 133, 0.56);
  box-shadow: 0 0 0 4px rgba(48, 95, 164, 0.12), 0 28px 64px rgba(25, 44, 70, 0.14);
}

.sf-node-start {
  background: linear-gradient(180deg, rgba(243, 250, 246, 0.98), rgba(231, 243, 236, 0.92));
}

.sf-node-between {
  background: linear-gradient(180deg, rgba(245, 249, 255, 0.98), rgba(231, 239, 251, 0.92));
}

.sf-node-serial,
.sf-node-serial-node {
  background: linear-gradient(180deg, rgba(255, 250, 242, 0.98), rgba(248, 238, 222, 0.92));
}

.sf-node-parallel {
  background: linear-gradient(180deg, rgba(246, 248, 255, 0.98), rgba(232, 237, 251, 0.92));
}

.sf-node-end {
  background: linear-gradient(180deg, rgba(248, 246, 242, 0.98), rgba(236, 232, 225, 0.94));
}

.sf-gateway-card {
  width: 248px;
  min-height: 96px;
}

.sf-node-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.sf-node-title {
  font-weight: 700;
  color: #1c2d43;
  font-size: 1.06rem;
}

.sf-node-close {
  border: 0;
  background: transparent;
  color: #8b6a43;
  font-size: 20px;
  line-height: 1;
}

.sf-node-type,
.sf-node-summary {
  margin: 10px 0 0;
}

.sf-node-type {
  font-size: 12px;
  color: #8b6b44;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-weight: 700;
}

.sf-node-summary {
  font-size: 13px;
  color: #56677e;
  white-space: pre-wrap;
  line-height: 1.55;
}

.sf-node-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.sf-node-tag {
  border: 1px solid rgba(76, 96, 123, 0.12);
  background: rgba(255, 255, 255, 0.88);
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  color: #4f657e;
}

.sf-connector {
  position: relative;
  width: 1px;
  min-height: 72px;
  background: linear-gradient(180deg, rgba(130, 151, 182, 0.12), rgba(130, 151, 182, 0.48), rgba(130, 151, 182, 0.12));
  margin: 10px 0 14px;
}

.sf-connector-readonly {
  min-height: 36px;
}

.sf-add-trigger {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: 1px solid rgba(40, 83, 143, 0.14);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(243, 247, 252, 0.96));
  color: #2a5f9d;
  box-shadow: 0 18px 34px rgba(40, 76, 126, 0.16);
  font-size: 22px;
  line-height: 1;
}

.sf-insert-menu {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(20px, -50%);
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 128px;
  padding: 10px;
  border-radius: 18px;
  background: rgba(252, 254, 255, 0.97);
  border: 1px solid rgba(74, 96, 127, 0.12);
  box-shadow: 0 26px 54px rgba(17, 34, 57, 0.14);
  backdrop-filter: blur(16px);
}

.sf-insert-menu button,
.sf-branch-add-btn {
  border: 1px solid rgba(76, 96, 123, 0.1);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  color: #314861;
  padding: 10px 12px;
  text-align: left;
  transition: border-color 0.16s ease, transform 0.16s ease, background 0.16s ease;
}

.sf-insert-menu button:hover,
.sf-branch-add-btn:hover {
  transform: translateY(-1px);
  border-color: rgba(37, 78, 133, 0.22);
  background: rgba(245, 249, 255, 0.98);
}

.sf-branch-toolbar {
  margin: 12px 0 18px;
}

.sf-branch-grid {
  display: flex;
  gap: 26px;
  align-items: flex-start;
  justify-content: center;
  padding: 14px 26px 0;
  position: relative;
}

.sf-branch-grid::before,
.sf-branch-grid::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  width: calc(100% - 52px);
  height: 1px;
  background: linear-gradient(90deg, rgba(126, 146, 175, 0.08), rgba(126, 146, 175, 0.42), rgba(126, 146, 175, 0.08));
}

.sf-branch-grid::before {
  top: 0;
}

.sf-branch-grid::after {
  bottom: 0;
}

.sf-branch-column {
  position: relative;
  padding-top: 24px;
}

.sf-branch-column::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 0;
  transform: translateX(-50%);
  width: 1px;
  height: 24px;
  background: linear-gradient(180deg, rgba(126, 146, 175, 0.08), rgba(126, 146, 175, 0.44));
}
</style>
