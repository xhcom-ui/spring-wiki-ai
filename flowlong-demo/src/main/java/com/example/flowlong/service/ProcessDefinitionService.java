package com.example.flowlong.service;

import com.example.flowlong.entity.ProcessDefinitionEntity;
import com.example.flowlong.repository.ProcessDefinitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class ProcessDefinitionService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final String DESIGNER_TYPE_BPMN = "BPMN";
    private static final String DESIGNER_TYPE_CUSTOM = "CUSTOM";
    private static final String DEFAULT_LEAVE_PROCESS_KEY = "leave-process";

    private final Object defaultDefinitionsLock = new Object();

    private final ProcessDefinitionRepository processDefinitionRepository;

    public ProcessDefinitionService(ProcessDefinitionRepository processDefinitionRepository) {
        this.processDefinitionRepository = processDefinitionRepository;
    }

    @Transactional
    public ProcessDefinitionEntity saveProcessDefinition(
            String processKey,
            String processName,
            String bpmnXml,
            String designerType,
            String designSchemaJson,
            Long userId
    ) {
        String normalizedDesignerType = normalizeDesignerType(designerType);
        validateProcessDefinition(processKey, processName, bpmnXml, normalizedDesignerType, designSchemaJson);

        ProcessDefinitionEntity latestVersion = processDefinitionRepository.findFirstByProcessKeyOrderByVersionDesc(processKey);
        int newVersion = latestVersion != null ? latestVersion.getVersion() + 1 : 1;
        LocalDateTime now = LocalDateTime.now();

        if (latestVersion != null) {
            latestVersion.setStatus(STATUS_INACTIVE);
            latestVersion.setUpdatedAt(now);
            latestVersion.setUpdatedBy(userId);
            processDefinitionRepository.save(latestVersion);
        }

        ProcessDefinitionEntity processDefinition = new ProcessDefinitionEntity();
        processDefinition.setProcessKey(processKey);
        processDefinition.setProcessName(processName);
        processDefinition.setBpmnXml(bpmnXml);
        processDefinition.setDesignerType(normalizedDesignerType);
        processDefinition.setDesignSchemaJson(normalizeSchemaJson(normalizedDesignerType, designSchemaJson));
        processDefinition.setVersion(newVersion);
        processDefinition.setStatus(STATUS_ACTIVE);
        processDefinition.setCreatedAt(now);
        processDefinition.setUpdatedAt(now);
        processDefinition.setCreatedBy(userId);
        processDefinition.setUpdatedBy(userId);

        ProcessDefinitionEntity saved = processDefinitionRepository.save(processDefinition);
        log.info("Saved FlowLong definition for workspace: {} v{} ({})", processKey, newVersion, normalizedDesignerType);
        return saved;
    }

    public ProcessDefinitionEntity getProcessDefinitionById(Long id) {
        ensureDefaultDefinitions();
        return processDefinitionRepository.findById(id).orElse(null);
    }

    public ProcessDefinitionEntity getProcessDefinitionByKeyAndVersion(String processKey, Integer version) {
        ensureDefaultDefinitions();
        return processDefinitionRepository.findByProcessKeyOrderByVersionDesc(processKey).stream()
                .filter(definition -> definition.getVersion().equals(version))
                .findFirst()
                .orElse(null);
    }

    public ProcessDefinitionEntity getLatestProcessDefinitionByKey(String processKey) {
        ensureDefaultDefinitions();
        return processDefinitionRepository.findFirstByProcessKeyOrderByVersionDesc(processKey);
    }

    public List<ProcessDefinitionEntity> getVersionsByProcessKey(String processKey) {
        ensureDefaultDefinitions();
        return processDefinitionRepository.findByProcessKeyOrderByVersionDesc(processKey);
    }

    public ProcessDefinitionEntity getActiveProcessDefinitionByKey(String processKey) {
        ensureDefaultDefinitions();
        return processDefinitionRepository.findFirstByProcessKeyAndStatusOrderByVersionDesc(processKey, STATUS_ACTIVE);
    }

    public List<ProcessDefinitionEntity> getAllProcessDefinitions() {
        ensureDefaultDefinitions();
        return processDefinitionRepository.findAllByOrderByUpdatedAtDescIdDesc();
    }

    public List<ProcessDefinitionEntity> getActiveProcessDefinitions() {
        ensureDefaultDefinitions();
        return processDefinitionRepository.findByStatusOrderByUpdatedAtDesc(STATUS_ACTIVE);
    }

    @Transactional
    public ProcessDefinitionEntity updateProcessDefinition(
            Long id,
            String processName,
            String bpmnXml,
            String designerType,
            String designSchemaJson,
            Long userId
    ) {
        ProcessDefinitionEntity processDefinition = requireDefinition(id);
        String normalizedDesignerType = normalizeDesignerType(designerType);
        validateProcessDefinition(processDefinition.getProcessKey(), processName, bpmnXml, normalizedDesignerType, designSchemaJson);

        processDefinition.setProcessName(processName);
        processDefinition.setBpmnXml(bpmnXml);
        processDefinition.setDesignerType(normalizedDesignerType);
        processDefinition.setDesignSchemaJson(normalizeSchemaJson(normalizedDesignerType, designSchemaJson));
        processDefinition.setUpdatedAt(LocalDateTime.now());
        processDefinition.setUpdatedBy(userId);

        ProcessDefinitionEntity updated = processDefinitionRepository.save(processDefinition);
        log.info("Updated FlowLong definition for workspace: {} ({}, {})", processDefinition.getProcessKey(), id, normalizedDesignerType);
        return updated;
    }

    @Transactional
    public void deactivateProcessDefinition(Long id, Long userId) {
        ProcessDefinitionEntity processDefinition = requireDefinition(id);
        processDefinition.setStatus(STATUS_INACTIVE);
        processDefinition.setUpdatedAt(LocalDateTime.now());
        processDefinition.setUpdatedBy(userId);
        processDefinitionRepository.save(processDefinition);
    }

    @Transactional
    public void deleteProcessDefinition(Long id) {
        requireDefinition(id);
        processDefinitionRepository.deleteById(id);
    }

    @Transactional
    public ProcessDefinitionEntity activateProcessVersion(Long id, Long userId) {
        ProcessDefinitionEntity processDefinition = requireDefinition(id);
        LocalDateTime now = LocalDateTime.now();

        List<ProcessDefinitionEntity> versions = processDefinitionRepository.findByProcessKeyOrderByVersionDesc(processDefinition.getProcessKey());
        for (ProcessDefinitionEntity version : versions) {
            if (!version.getId().equals(id) && STATUS_ACTIVE.equals(version.getStatus())) {
                version.setStatus(STATUS_INACTIVE);
                version.setUpdatedAt(now);
                version.setUpdatedBy(userId);
                processDefinitionRepository.save(version);
            }
        }

        processDefinition.setStatus(STATUS_ACTIVE);
        processDefinition.setUpdatedAt(now);
        processDefinition.setUpdatedBy(userId);
        return processDefinitionRepository.save(processDefinition);
    }

    public Map<String, Object> compareProcessDefinitions(Long leftId, Long rightId) {
        ProcessDefinitionEntity left = requireDefinition(leftId);
        ProcessDefinitionEntity right = requireDefinition(rightId);

        List<String> leftLines = normalizeXmlLines(left.getBpmnXml());
        List<String> rightLines = normalizeXmlLines(right.getBpmnXml());
        int max = Math.max(leftLines.size(), rightLines.size());
        int changedLines = 0;
        List<Map<String, Object>> preview = new ArrayList<>();

        for (int i = 0; i < max; i++) {
            String leftLine = i < leftLines.size() ? leftLines.get(i) : "";
            String rightLine = i < rightLines.size() ? rightLines.get(i) : "";
            if (!Objects.equals(leftLine, rightLine)) {
                changedLines++;
                if (preview.size() < 12) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("line", i + 1);
                    row.put("left", leftLine);
                    row.put("right", rightLine);
                    preview.add(row);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("left", toSummary(left));
        result.put("right", toSummary(right));
        result.put("sameProcessKey", Objects.equals(left.getProcessKey(), right.getProcessKey()));
        result.put("sameDesignerType", Objects.equals(normalizeDesignerType(left.getDesignerType()), normalizeDesignerType(right.getDesignerType())));
        result.put("versionGap", Math.abs(left.getVersion() - right.getVersion()));
        result.put("leftLineCount", leftLines.size());
        result.put("rightLineCount", rightLines.size());
        result.put("changedLineCount", changedLines);
        result.put("diffPreview", preview);
        result.put("leftXml", left.getBpmnXml());
        result.put("rightXml", right.getBpmnXml());
        result.put("schemaComparison", compareSchemaSnapshots(left.getDesignSchemaJson(), right.getDesignSchemaJson()));
        return result;
    }

    private ProcessDefinitionEntity requireDefinition(Long id) {
        ensureDefaultDefinitions();
        return processDefinitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("流程定义不存在"));
    }

    private void ensureDefaultDefinitions() {
        synchronized (defaultDefinitionsLock) {
            ProcessDefinitionEntity existing = processDefinitionRepository.findFirstByProcessKeyOrderByVersionDesc(DEFAULT_LEAVE_PROCESS_KEY);
            if (existing != null) {
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            ProcessDefinitionEntity definition = new ProcessDefinitionEntity();
            definition.setProcessKey(DEFAULT_LEAVE_PROCESS_KEY);
            definition.setProcessName("请假审批流程");
            definition.setBpmnXml(defaultLeaveProcessBpmn());
            definition.setDesignerType(DESIGNER_TYPE_BPMN);
            definition.setDesignSchemaJson(null);
            definition.setVersion(1);
            definition.setStatus(STATUS_ACTIVE);
            definition.setCreatedAt(now);
            definition.setUpdatedAt(now);
            definition.setCreatedBy(null);
            definition.setUpdatedBy(null);
            processDefinitionRepository.save(definition);
        }
    }

    private String defaultLeaveProcessBpmn() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  xmlns:flowable="http://flowable.org/bpmn"
                  id="Definitions_leave_process"
                  targetNamespace="http://flowable.org/processdef">
                  <bpmn:process id="leave-process" name="请假审批流程" isExecutable="true">
                    <bpmn:startEvent id="StartEvent_1" name="提交申请" flowable:formKey="leave-form">
                      <bpmn:documentation>请假发起页 / 请假申请单</bpmn:documentation>
                    </bpmn:startEvent>
                    <bpmn:userTask id="Task_DeptApprove" name="部门经理审批" flowable:assignee="${deptManager}" flowable:formKey="manager-approval">
                      <bpmn:documentation>待办审批页 / 部门经理审批单</bpmn:documentation>
                    </bpmn:userTask>
                    <bpmn:userTask id="Task_GeneralApprove" name="总经理审批" flowable:assignee="${generalManager}" flowable:formKey="general-approval">
                      <bpmn:documentation>待办审批页 / 总经理审批单</bpmn:documentation>
                    </bpmn:userTask>
                    <bpmn:endEvent id="EndEvent_Approved" name="审批结束" />
                    <bpmn:sequenceFlow id="Flow_Start_To_Dept" sourceRef="StartEvent_1" targetRef="Task_DeptApprove" />
                    <bpmn:sequenceFlow id="Flow_Dept_To_General" sourceRef="Task_DeptApprove" targetRef="Task_GeneralApprove" />
                    <bpmn:sequenceFlow id="Flow_General_To_End" sourceRef="Task_GeneralApprove" targetRef="EndEvent_Approved" />
                  </bpmn:process>
                  <bpmndi:BPMNDiagram id="BPMNDiagram_leave_process">
                    <bpmndi:BPMNPlane id="BPMNPlane_leave_process" bpmnElement="leave-process">
                      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
                        <dc:Bounds x="120" y="182" width="36" height="36" />
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Task_DeptApprove_di" bpmnElement="Task_DeptApprove">
                        <dc:Bounds x="220" y="160" width="120" height="80" />
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Task_GeneralApprove_di" bpmnElement="Task_GeneralApprove">
                        <dc:Bounds x="420" y="160" width="120" height="80" />
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="EndEvent_Approved_di" bpmnElement="EndEvent_Approved">
                        <dc:Bounds x="620" y="182" width="36" height="36" />
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNEdge id="Flow_Start_To_Dept_di" bpmnElement="Flow_Start_To_Dept">
                        <di:waypoint x="156" y="200" />
                        <di:waypoint x="220" y="200" />
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_Dept_To_General_di" bpmnElement="Flow_Dept_To_General">
                        <di:waypoint x="340" y="200" />
                        <di:waypoint x="420" y="200" />
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_General_To_End_di" bpmnElement="Flow_General_To_End">
                        <di:waypoint x="540" y="200" />
                        <di:waypoint x="620" y="200" />
                      </bpmndi:BPMNEdge>
                    </bpmndi:BPMNPlane>
                  </bpmndi:BPMNDiagram>
                </bpmn:definitions>
                """;
    }

    private void validateProcessDefinition(
            String processKey,
            String processName,
            String bpmnXml,
            String designerType,
            String designSchemaJson
    ) {
        if (processKey == null || processKey.isBlank()) {
            throw new RuntimeException("流程 Key 不能为空");
        }
        if (processName == null || processName.isBlank()) {
            throw new RuntimeException("流程名称不能为空");
        }
        if (bpmnXml == null || bpmnXml.isBlank()) {
            throw new RuntimeException("BPMN XML 不能为空");
        }
        if (DESIGNER_TYPE_CUSTOM.equals(designerType) && (designSchemaJson == null || designSchemaJson.isBlank())) {
            throw new RuntimeException("自定义设计器 Schema 不能为空");
        }
    }

    private Map<String, Object> toSummary(ProcessDefinitionEntity entity) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", entity.getId());
        summary.put("processKey", entity.getProcessKey());
        summary.put("processName", entity.getProcessName());
        summary.put("designerType", normalizeDesignerType(entity.getDesignerType()));
        summary.put("version", entity.getVersion());
        summary.put("status", entity.getStatus());
        summary.put("updatedAt", entity.getUpdatedAt());
        return summary;
    }

    private String normalizeDesignerType(String designerType) {
        return DESIGNER_TYPE_CUSTOM.equalsIgnoreCase(designerType) ? DESIGNER_TYPE_CUSTOM : DESIGNER_TYPE_BPMN;
    }

    private String normalizeSchemaJson(String designerType, String designSchemaJson) {
        if (!DESIGNER_TYPE_CUSTOM.equals(designerType)) {
            return null;
        }
        return designSchemaJson == null || designSchemaJson.isBlank() ? null : designSchemaJson;
    }

    private List<String> normalizeXmlLines(String xml) {
        if (xml == null || xml.isBlank()) {
            return List.of();
        }
        return xml.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    private Map<String, Object> compareSchemaSnapshots(String leftSchema, String rightSchema) {
        List<String> leftLines = normalizeXmlLines(leftSchema);
        List<String> rightLines = normalizeXmlLines(rightSchema);
        int max = Math.max(leftLines.size(), rightLines.size());
        int changedLines = 0;
        List<Map<String, Object>> preview = new ArrayList<>();

        for (int i = 0; i < max; i++) {
            String leftLine = i < leftLines.size() ? leftLines.get(i) : "";
            String rightLine = i < rightLines.size() ? rightLines.get(i) : "";
            if (!Objects.equals(leftLine, rightLine)) {
                changedLines++;
                if (preview.size() < 10) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("line", i + 1);
                    row.put("left", leftLine);
                    row.put("right", rightLine);
                    preview.add(row);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("leftLineCount", leftLines.size());
        result.put("rightLineCount", rightLines.size());
        result.put("changedLineCount", changedLines);
        result.put("diffPreview", preview);
        return result;
    }
}
