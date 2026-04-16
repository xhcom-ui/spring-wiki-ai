<template>
  <div class="process-designer">
    <div class="designer-container">
      <div ref="canvas" class="canvas"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import BpmnModeler from 'bpmn-js/lib/Modeler';
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css';
import 'bpmn-js/dist/assets/diagram-js.css';

const canvas = ref(null);
let modeler = null;

onMounted(() => {
  // Initialize BPMN modeler
  modeler = new BpmnModeler({
    container: canvas.value
  });

  // Load default BPMN diagram
  const defaultDiagram = `<?xml version="1.0" encoding="UTF-8"?>
  <bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                    xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                    xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                    id="Definitions_1"
                    targetNamespace="http://bpmn.io/schema/bpmn">
    <bpmn:process id="Process_1" isExecutable="true">
      <bpmn:startEvent id="StartEvent_1" />
    </bpmn:process>
    <bpmndi:BPMNDiagram id="BPMNDiagram_1">
      <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
        <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
          <dc:Bounds x="173" y="102" width="36" height="36" />
        </bpmndi:BPMNShape>
      </bpmndi:BPMNPlane>
    </bpmndi:BPMNDiagram>
  </bpmn:definitions>`;

  modeler.importXML(defaultDiagram);
});

onUnmounted(() => {
  if (modeler) {
    modeler.destroy();
  }
});

// Expose methods to parent component
defineExpose({
  // Get BPMN XML
  async getBpmnXml() {
    const { xml } = await modeler.saveXML({ format: true });
    return xml;
  },

  // Load BPMN XML
  async loadBpmnXml(xml) {
    await modeler.importXML(xml);
  },

  // Load process by ID
  async loadProcessById(processId) {
    try {
      const response = await fetch(`/api/process-definition/${processId}`);
      const processDefinition = await response.json();
      if (processDefinition && processDefinition.bpmnXml) {
        await this.loadBpmnXml(processDefinition.bpmnXml);
        return processDefinition;
      }
    } catch (error) {
      console.error('加载流程失败:', error);
    }
    return null;
  }
});
</script>

<style scoped>
.process-designer {
  height: 600px;
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  background: white;
}

.designer-container {
  width: 100%;
  height: 100%;
  position: relative;
  background: #f8f9fa;
}

.canvas {
  width: 100%;
  height: 100%;
}

/* Style the BPMN elements */
:deep(.bpmn-icon-start-event-none) {
  color: #4CAF50;
}

:deep(.bpmn-icon-end-event-none) {
  color: #f44336;
}

:deep(.bpmn-icon-user-task) {
  color: #2196F3;
}

:deep(.bpmn-icon-service-task) {
  color: #ff9800;
}

:deep(.bpmn-icon-exclusive-gateway) {
  color: #9c27b0;
}

:deep(.bpmn-icon-intermediate-event-catch-timer) {
  color: #607D8B;
}
</style>