<template>
  <div v-if="isSimpleNode" class="sf-chain-node" :class="{ 'sf-chain-node-end': isEndNode }">
    <article :class="['sf-node-card', `sf-node-${node.nodeType}`, { selected: isSelected }]" @click.stop="$emit('select-node', node)">
      <header class="sf-node-head">
        <span class="sf-node-title">{{ node.nodeName }}</span>
        <button v-if="canRemove" type="button" class="sf-node-close" @click.stop="$emit('remove-node', node)">×</button>
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
      <button type="button" class="sf-add-trigger" @click.stop="toggleInsertMenu">+</button>
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

  <div v-else class="sf-gateway-shell">
    <article :class="['sf-node-card', 'sf-gateway-card', `sf-node-${node.nodeType}`, { selected: isSelected }]" @click.stop="$emit('select-node', node)">
      <header class="sf-node-head">
        <span class="sf-node-title">{{ node.nodeName }}</span>
        <button v-if="canRemove" type="button" class="sf-node-close" @click.stop="$emit('remove-node', node)">×</button>
      </header>
      <p class="sf-node-type">{{ nodeTypeLabel }}</p>
      <p class="sf-node-summary">{{ defaultSummary }}</p>
    </article>

    <div v-if="!readonly" class="sf-branch-toolbar">
      <button type="button" class="sf-branch-add-btn" @click.stop="$emit('append-branch', { node, branchType: node.nodeType })">
        {{ node.nodeType === 'serial' ? '添加条件' : '添加并行' }}
      </button>
    </div>

    <div class="sf-branch-grid" :class="{ parallel: node.nodeType === 'parallel' }">
      <div v-for="branch in node.conditionNodes || []" :key="branch.nodeId" class="sf-branch-column">
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
      <button type="button" class="sf-add-trigger" @click.stop="toggleInsertMenu">+</button>
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
import { computed, ref } from 'vue'
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
      return '设置发起人、业务页和表单入口'
    case 'between':
      return '设置办理人、候选人和业务表单'
    case 'serial':
      return '配置条件表达式和默认分支'
    case 'serial-node':
      return '分支条件'
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

function insertNode(nodeType) {
  showInsertMenu.value = false
  if (props.readonly) {
    return
  }
  emit('insert-node', { targetNode: props.node, nodeType })
}
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
  width: 220px;
  min-height: 76px;
  border-radius: 8px;
  border: 1px solid #dbe5f1;
  background: #ffffff;
  box-shadow: 0 6px 18px rgba(25, 55, 99, 0.08);
  padding: 0;
  text-align: left;
  cursor: pointer;
  overflow: hidden;
  position: relative;
}

.sf-node-card.selected {
  border-color: #3d7eff;
  box-shadow: 0 0 0 4px rgba(61, 126, 255, 0.14);
}

.sf-node-start {
  border-top: 4px solid #576a95;
}

.sf-node-between {
  border-top: 4px solid #ff943e;
}

.sf-node-serial,
.sf-node-serial-node {
  border-top: 4px solid #15bc83;
}

.sf-node-parallel {
  border-top: 4px solid #3296fa;
}

.sf-node-end {
  border-top: 4px solid #93a1b7;
}

.sf-gateway-card {
  width: 240px;
}

.sf-node-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px 0;
}

.sf-node-title {
  font-weight: 700;
  color: #1f2d3d;
}

.sf-node-close {
  border: 0;
  background: transparent;
  color: #7b8da6;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}

.sf-node-type,
.sf-node-summary {
  margin: 8px 0 0;
}

.sf-node-type {
  font-size: 12px;
  color: #7d8ea5;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  padding: 0 14px;
}

.sf-node-summary {
  font-size: 13px;
  color: #4d6078;
  white-space: pre-wrap;
  padding: 0 14px 14px;
}

.sf-node-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 2px;
  padding: 0 14px 14px;
}

.sf-node-tag {
  border: 1px solid #d6e5ff;
  background: #f5f9ff;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  color: #2a63c7;
  cursor: pointer;
}

.sf-connector {
  position: relative;
  width: 1px;
  min-height: 64px;
  background: #cfd8e6;
  margin: 8px 0 12px;
}

.sf-connector-readonly {
  min-height: 36px;
}

.sf-add-trigger {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #d6e5ff;
  background: #fff;
  color: #2f63b2;
  box-shadow: 0 10px 20px rgba(40, 80, 140, 0.1);
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}

.sf-insert-menu {
  position: absolute;
  top: 50%;
  left: 26px;
  transform: translateY(-50%);
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 112px;
  padding: 8px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid #dce5f2;
  box-shadow: 0 18px 38px rgba(38, 62, 98, 0.14);
}

.sf-insert-menu button,
.sf-branch-add-btn {
  border: 1px solid #dce5f2;
  border-radius: 10px;
  background: #fff;
  color: #40556f;
  padding: 8px 10px;
  text-align: left;
  cursor: pointer;
}

.sf-branch-toolbar {
  margin: 10px 0 14px;
}

.sf-branch-grid {
  display: flex;
  gap: 22px;
  align-items: flex-start;
  justify-content: center;
  padding: 10px 20px 0;
  position: relative;
}

.sf-branch-grid::before,
.sf-branch-grid::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  width: calc(100% - 40px);
  height: 1px;
  background: #cfd8e6;
}

.sf-branch-grid::before {
  top: 0;
}

.sf-branch-grid::after {
  bottom: 0;
}

.sf-branch-column {
  position: relative;
  padding-top: 20px;
}

.sf-branch-column::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 0;
  transform: translateX(-50%);
  width: 1px;
  height: 20px;
  background: #cfd8e6;
}
</style>
