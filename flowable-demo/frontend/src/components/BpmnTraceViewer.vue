<template>
  <div class="designer-shell viewer-shell">
    <div class="designer-toolbar">
      <div class="designer-status">
        <span :class="['status-dot', statusClass]"></span>
        <span>{{ statusText }}</span>
      </div>
      <div class="designer-actions">
        <button type="button" class="ghost-btn" @click="zoomOut">缩小</button>
        <button type="button" class="ghost-btn" @click="resetZoom">适配画布</button>
        <button type="button" class="ghost-btn" @click="zoomIn">放大</button>
      </div>
    </div>
    <div ref="canvasRef" class="designer-canvas"></div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import BpmnViewer from 'bpmn-js/lib/NavigatedViewer'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'

const emit = defineEmits(['node-selected'])

const props = defineProps({
  xml: {
    type: String,
    default: ''
  },
  highlightedNodeIds: {
    type: Array,
    default: () => []
  },
  completedActivityIds: {
    type: Array,
    default: () => []
  },
  activeActivityIds: {
    type: Array,
    default: () => []
  },
  gatewayIds: {
    type: Array,
    default: () => []
  },
  currentActivityId: {
    type: String,
    default: ''
  },
  sequenceFlowIds: {
    type: Array,
    default: () => []
  }
})

const canvasRef = ref(null)
const ready = ref(false)
const loadError = ref('')

let viewer
let previousMarkers = []

function markerEntries(ids, marker) {
  return (ids || [])
    .filter((value) => typeof value === 'string' && value.trim())
    .map((id) => ({ id, marker }))
}

function clearMarkers() {
  if (!viewer) {
    previousMarkers = []
    return
  }
  const canvas = viewer.get('canvas')
  for (const item of previousMarkers) {
    try {
      canvas.removeMarker(item.id, item.marker)
    } catch (error) {
      // ignore missing markers
    }
  }
  previousMarkers = []
}

function applyMarkers() {
  if (!viewer || !ready.value) {
    return
  }
  clearMarkers()
  const canvas = viewer.get('canvas')
  const nextMarkers = [
    ...markerEntries(props.highlightedNodeIds, 'trace-node'),
    ...markerEntries(props.completedActivityIds, 'trace-completed'),
    ...markerEntries(props.gatewayIds, 'trace-gateway'),
    ...markerEntries(props.activeActivityIds, 'trace-active'),
    ...markerEntries(props.currentActivityId ? [props.currentActivityId] : [], 'trace-current'),
    ...markerEntries(props.sequenceFlowIds, 'trace-flow')
  ]
  for (const item of nextMarkers) {
    try {
      canvas.addMarker(item.id, item.marker)
      previousMarkers.push(item)
    } catch (error) {
      // ignore markers for missing elements
    }
  }
}

async function importXml(xml) {
  if (!viewer || !xml) {
    ready.value = false
    return
  }
  loadError.value = ''
  try {
    clearMarkers()
    await viewer.importXML(xml)
    viewer.get('canvas').zoom('fit-viewport', 'auto')
    ready.value = true
    await nextTick()
    applyMarkers()
  } catch (error) {
    ready.value = false
    loadError.value = error.message || 'BPMN 轨迹图加载失败'
  }
}

function zoom(step) {
  if (!viewer) {
    return
  }
  const canvas = viewer.get('canvas')
  const current = canvas.zoom()
  canvas.zoom(Math.max(0.2, Math.min(4, current + step)))
}

function zoomIn() {
  zoom(0.1)
}

function zoomOut() {
  zoom(-0.1)
}

function resetZoom() {
  if (viewer) {
    viewer.get('canvas').zoom('fit-viewport', 'auto')
  }
}

const statusText = computed(() => {
  if (loadError.value) {
    return loadError.value
  }
  if (!props.xml) {
    return '暂无 BPMN 轨迹数据'
  }
  if (!ready.value) {
    return 'BPMN 轨迹图加载中'
  }
  if (props.activeActivityIds.length) {
    return props.currentActivityId
      ? `当前停留节点 ${props.currentActivityId}，已高亮 ${props.activeActivityIds.length} 个活动节点`
      : `已高亮 ${props.activeActivityIds.length} 个活动节点`
  }
  return '已加载 BPMN 轨迹图'
})

const statusClass = computed(() => {
  if (loadError.value) {
    return 'status-dirty'
  }
  if (!props.xml || !ready.value) {
    return 'status-waiting'
  }
  return props.activeActivityIds.length ? 'status-clean' : 'status-waiting'
})

watch(
  () => props.xml,
  async (xml) => {
    await importXml(xml)
  }
)

watch(
  () => [props.highlightedNodeIds, props.completedActivityIds, props.activeActivityIds, props.gatewayIds, props.currentActivityId, props.sequenceFlowIds],
  () => {
    applyMarkers()
  },
  { deep: true }
)

onMounted(async () => {
  viewer = new BpmnViewer({
    container: canvasRef.value
  })
  viewer.on('element.click', (event) => {
    const element = event?.element
    const businessObject = element?.businessObject
    const elementId = element?.id || businessObject?.id
    const elementType = businessObject?.$type || element?.type || ''
    if (!elementId || elementType.toLowerCase().includes('sequenceflow')) {
      return
    }
    emit('node-selected', {
      id: elementId,
      type: elementType,
      name: businessObject?.name || element?.businessObject?.name || ''
    })
  })
  if (props.xml) {
    await importXml(props.xml)
  }
})

onUnmounted(() => {
  clearMarkers()
  viewer?.destroy()
})
</script>

<style scoped>
.viewer-shell {
  min-height: 560px;
}

:deep(.djs-element) {
  cursor: pointer;
}

:deep(.trace-node .djs-visual > :nth-child(1)) {
  stroke: #b77727 !important;
  stroke-width: 4px !important;
}

:deep(.trace-completed .djs-visual > :nth-child(1)) {
  fill: rgba(224, 183, 116, 0.35) !important;
  stroke: #b77727 !important;
  stroke-width: 4px !important;
}

:deep(.trace-active .djs-visual > :nth-child(1)) {
  fill: rgba(86, 153, 103, 0.24) !important;
  stroke: #2f7d32 !important;
  stroke-width: 5px !important;
}

:deep(.trace-gateway .djs-visual > :nth-child(1)) {
  fill: rgba(233, 207, 160, 0.45) !important;
  stroke: #9b6b2f !important;
  stroke-width: 4px !important;
  stroke-dasharray: 6 4;
}

:deep(.trace-current .djs-visual > :nth-child(1)) {
  fill: rgba(107, 177, 121, 0.28) !important;
  stroke: #126b33 !important;
  stroke-width: 6px !important;
  filter: drop-shadow(0 0 10px rgba(18, 107, 51, 0.35));
}

:deep(.trace-flow .djs-visual path) {
  stroke: #2f7d32 !important;
  stroke-width: 4px !important;
  stroke-linecap: round;
  marker-end: none;
}
</style>
