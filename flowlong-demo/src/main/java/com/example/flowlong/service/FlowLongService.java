package com.example.flowlong.service;

import com.aizuda.bpm.engine.ProcessService;
import com.aizuda.bpm.engine.QueryService;
import com.aizuda.bpm.engine.RuntimeService;
import com.aizuda.bpm.engine.TaskService;
import com.aizuda.bpm.engine.core.FlowCreator;
import com.aizuda.bpm.engine.entity.FlwInstance;
import com.aizuda.bpm.engine.entity.FlwProcess;
import com.aizuda.bpm.engine.entity.FlwTask;
import com.aizuda.bpm.engine.entity.FlwTaskActor;
import com.example.flowlong.entity.ProcessInstanceAuditLog;
import com.example.flowlong.entity.FlowLongDeploymentRecord;
import com.example.flowlong.entity.Leave;
import com.example.flowlong.entity.ProcessDefinitionEntity;
import com.example.flowlong.entity.User;
import com.example.flowlong.repository.FlowLongDeploymentRecordRepository;
import com.example.flowlong.repository.LeaveRepository;
import com.example.flowlong.repository.ProcessInstanceAuditLogRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class FlowLongService {
    private static final String TENANT_ID = "workspace";
    private static final String LEAVE_PROCESS_KEY = "leave-process";

    private final ProcessService processService;
    private final RuntimeService runtimeService;
    private final QueryService queryService;
    private final TaskService taskService;
    private final FlowLongDeploymentRecordRepository deploymentRecordRepository;
    private final LeaveRepository leaveRepository;
    private final ProcessInstanceAuditLogRepository processInstanceAuditLogRepository;
    private final ProcessDefinitionService processDefinitionService;
    private final ProcessBusinessConfigService processBusinessConfigService;
    private final UserService userService;
    private final BpmnFlowLongConverter bpmnFlowLongConverter;
    private final ObjectMapper objectMapper;

    public FlowLongService(
            ProcessService processService,
            RuntimeService runtimeService,
            QueryService queryService,
            TaskService taskService,
            FlowLongDeploymentRecordRepository deploymentRecordRepository,
            LeaveRepository leaveRepository,
            ProcessInstanceAuditLogRepository processInstanceAuditLogRepository,
            ProcessDefinitionService processDefinitionService,
            ProcessBusinessConfigService processBusinessConfigService,
            UserService userService,
            BpmnFlowLongConverter bpmnFlowLongConverter,
            ObjectMapper objectMapper
    ) {
        this.processService = processService;
        this.runtimeService = runtimeService;
        this.queryService = queryService;
        this.taskService = taskService;
        this.deploymentRecordRepository = deploymentRecordRepository;
        this.leaveRepository = leaveRepository;
        this.processInstanceAuditLogRepository = processInstanceAuditLogRepository;
        this.processDefinitionService = processDefinitionService;
        this.processBusinessConfigService = processBusinessConfigService;
        this.userService = userService;
        this.bpmnFlowLongConverter = bpmnFlowLongConverter;
        this.objectMapper = objectMapper;
    }

    public Long deployProcess(String processName, String modelContent) {
        String normalizedContent = modelContent != null && modelContent.contains("<")
                ? bpmnFlowLongConverter.convertBpmnXmlToFlowLongJson(modelContent, processName)
                : modelContent;
        FlowCreator creator = currentCreator();
        Long processId = processService.deploy(
                null,
                normalizedContent,
                creator,
                true,
                null
        );
        log.info("FlowLong process deployed, processId: {}, processName: {}", processId, processName);
        return processId;
    }

    public Map<String, Object> deployDefinition(Long definitionId) {
        ProcessDefinitionEntity definition = processDefinitionService.getProcessDefinitionById(definitionId);
        if (definition == null) {
            throw new RuntimeException("流程定义不存在");
        }
        Long processId = deployProcess(definition.getProcessName(), definition.getBpmnXml());
        FlwProcess flowLongProcess = processService.getProcessById(processId);
        recordDeployment(flowLongProcess, definition, "PROCESS_DEFINITION");
        return Map.of(
                "definitionId", definition.getId(),
                "processId", processId,
                "processKey", definition.getProcessKey(),
                "processName", definition.getProcessName(),
                "designerType", definition.getDesignerType(),
                "version", definition.getVersion()
        );
    }

    public Map<String, Object> deployActiveDefinition(String processKey) {
        ProcessDefinitionEntity definition = processDefinitionService.getActiveProcessDefinitionByKey(processKey);
        if (definition == null) {
            throw new RuntimeException("未找到可部署的激活流程定义: " + processKey);
        }
        return deployDefinition(definition.getId());
    }

    public Map<String, Object> getProcessStatus(String processKey) {
        ProcessDefinitionEntity activeDefinition = processDefinitionService.getActiveProcessDefinitionByKey(processKey);
        FlowLongDeploymentRecord lastDeployment = deploymentRecordRepository.findFirstByProcessKeyOrderByDeployedAtDescIdDesc(processKey);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("processKey", processKey);
        response.put("activeDefinitionId", activeDefinition != null ? activeDefinition.getId() : null);
        response.put("activeDefinitionVersion", activeDefinition != null ? activeDefinition.getVersion() : null);
        response.put("activeDefinitionName", activeDefinition != null ? activeDefinition.getProcessName() : null);

        try {
            FlwProcess process = resolveDeployedProcess(lastDeployment);
            if (process == null) {
                process = processService.getProcessByKey(TENANT_ID, processKey);
            }
            response.put("deployed", true);
            response.put("processId", process.getId());
            response.put("processName", process.getProcessName());
            response.put("version", process.getProcessVersion());
            response.put("state", process.getProcessState());
        } catch (Exception ex) {
            response.put("deployed", false);
        }

        if (lastDeployment != null) {
            response.put("lastDeploymentId", lastDeployment.getId());
            response.put("lastDeploymentAt", lastDeployment.getDeployedAt());
            response.put("lastDeploymentBy", lastDeployment.getDeployedByName());
            response.put("deployedDefinitionId", lastDeployment.getSourceDefinitionId());
            response.put("deployedDefinitionVersion", lastDeployment.getSourceDefinitionVersion());
            response.put("deployedFlowLongProcessId", lastDeployment.getFlowLongProcessId());
            response.put("deployedFlowLongVersion", lastDeployment.getFlowLongProcessVersion());
        }

        Integer activeVersion = activeDefinition != null ? activeDefinition.getVersion() : null;
        Integer deployedVersion = lastDeployment != null ? lastDeployment.getSourceDefinitionVersion() : null;
        response.put("definitionSynchronized",
                activeVersion != null && deployedVersion != null && activeVersion.equals(deployedVersion));
        return response;
    }

    public List<FlowLongDeploymentRecord> getDeploymentHistory(String processKey) {
        return deploymentRecordRepository.findByProcessKeyOrderByDeployedAtDescIdDesc(processKey);
    }

    public Map<String, Object> getDeploymentRecordDetail(Long deploymentId) {
        FlowLongDeploymentRecord record = requireDeploymentRecord(deploymentId);
        ProcessDefinitionEntity sourceDefinition = record.getSourceDefinitionId() == null
                ? null
                : processDefinitionService.getProcessDefinitionById(record.getSourceDefinitionId());
        ProcessDefinitionEntity activeDefinition = processDefinitionService.getActiveProcessDefinitionByKey(record.getProcessKey());
        FlowLongDeploymentRecord latestDeployment = deploymentRecordRepository.findFirstByProcessKeyOrderByDeployedAtDescIdDesc(record.getProcessKey());

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", record.getId());
        detail.put("processKey", record.getProcessKey());
        detail.put("processName", record.getProcessName());
        detail.put("flowLongProcessId", record.getFlowLongProcessId());
        detail.put("flowLongProcessVersion", record.getFlowLongProcessVersion());
        detail.put("sourceDefinitionId", record.getSourceDefinitionId());
        detail.put("sourceDefinitionVersion", record.getSourceDefinitionVersion());
        detail.put("sourceType", record.getSourceType());
        detail.put("deployedBy", record.getDeployedBy());
        detail.put("deployedByName", record.getDeployedByName());
        detail.put("deployedAt", record.getDeployedAt());
        detail.put("isCurrentDeployment", latestDeployment != null && record.getId().equals(latestDeployment.getId()));
        detail.put("canRollback", sourceDefinition != null);
        detail.put("sourceDefinition", sourceDefinition == null ? null : toDefinitionDetail(sourceDefinition));
        detail.put("activeDefinition", activeDefinition == null ? null : toDefinitionSummary(activeDefinition));
        return detail;
    }

    public Map<String, Object> rollbackDeployment(Long deploymentId) {
        FlowLongDeploymentRecord record = requireDeploymentRecord(deploymentId);
        if (record.getSourceDefinitionId() == null) {
            throw new RuntimeException("该部署记录没有来源流程定义，无法回滚");
        }

        ProcessDefinitionEntity sourceDefinition = processDefinitionService.activateProcessVersion(record.getSourceDefinitionId(), currentUserId());
        Map<String, Object> deployment = new LinkedHashMap<>(deployDefinition(sourceDefinition.getId()));
        deployment.put("rolledBackFromDeploymentId", record.getId());
        deployment.put("rolledBackToDefinitionId", sourceDefinition.getId());
        deployment.put("rolledBackToVersion", sourceDefinition.getVersion());
        deployment.put("rolledBackProcessKey", sourceDefinition.getProcessKey());
        return deployment;
    }

    private void recordDeployment(FlwProcess process, ProcessDefinitionEntity definition, String sourceType) {
        User currentUser = userService.getCurrentUser();
        FlowLongDeploymentRecord record = new FlowLongDeploymentRecord();
        record.setProcessKey(process.getProcessKey());
        record.setProcessName(process.getProcessName());
        record.setFlowLongProcessId(process.getId());
        record.setFlowLongProcessVersion(process.getProcessVersion());
        record.setSourceDefinitionId(definition != null ? definition.getId() : null);
        record.setSourceDefinitionVersion(definition != null ? definition.getVersion() : null);
        record.setSourceType(sourceType);
        record.setDeployedBy(currentUser != null ? currentUser.getId() : null);
        record.setDeployedByName(currentUser != null
                ? (currentUser.getNickname() != null && !currentUser.getNickname().isBlank()
                ? currentUser.getNickname()
                : currentUser.getUsername())
                : "system");
        record.setDeployedAt(LocalDateTime.now());
        deploymentRecordRepository.save(record);
    }

    private FlowLongDeploymentRecord requireDeploymentRecord(Long deploymentId) {
        return deploymentRecordRepository.findById(deploymentId)
                .orElseThrow(() -> new RuntimeException("部署记录不存在"));
    }

    private Long currentUserId() {
        User currentUser = userService.getCurrentUser();
        return currentUser == null ? null : currentUser.getId();
    }

    private Map<String, Object> toDefinitionSummary(ProcessDefinitionEntity definition) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", definition.getId());
        summary.put("processKey", definition.getProcessKey());
        summary.put("processName", definition.getProcessName());
        summary.put("designerType", definition.getDesignerType());
        summary.put("version", definition.getVersion());
        summary.put("status", definition.getStatus());
        summary.put("updatedAt", definition.getUpdatedAt());
        return summary;
    }

    private Map<String, Object> toDefinitionDetail(ProcessDefinitionEntity definition) {
        Map<String, Object> detail = new LinkedHashMap<>(toDefinitionSummary(definition));
        detail.put("bpmnXml", definition.getBpmnXml());
        detail.put("designSchemaJson", definition.getDesignSchemaJson());
        detail.put("createdAt", definition.getCreatedAt());
        return detail;
    }

    public Map<String, Object> deployProcessAndRecord(String processName, String modelContent) {
        Long processId = deployProcess(processName, modelContent);
        FlwProcess process = processService.getProcessById(processId);
        ProcessDefinitionEntity matchingDefinition = processDefinitionService.getActiveProcessDefinitionByKey(process.getProcessKey());
        recordDeployment(process, matchingDefinition, "CANVAS_XML");
        return Map.of(
                "processId", processId,
                "processKey", process.getProcessKey(),
                "processName", process.getProcessName(),
                "version", process.getProcessVersion()
        );
    }

    public Leave startLeaveProcess(Leave leave) {
        validateLeave(leave);

        FlwProcess process = resolveLeaveProcess().checkState();
        FlowCreator creator = currentCreator();
        String businessKey = leave.getBusinessKey();
        if (businessKey == null || businessKey.isBlank()) {
            businessKey = "LEAVE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
        final String finalBusinessKey = businessKey;

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("applicant", leave.getApplicant());
        variables.put("deptManager", leave.getDeptManager());
        variables.put("generalManager", leave.getGeneralManager());
        variables.put("days", leave.getDays());
        variables.put("reason", leave.getReason());
        variables.put("businessKey", finalBusinessKey);

        FlwInstance instance = runtimeService.createInstance(
                process,
                creator,
                variables,
                process.model().getNodeConfig(),
                false,
                () -> FlwInstance.of(finalBusinessKey)
        );

        LocalDateTime now = LocalDateTime.now();
        leave.setBusinessKey(finalBusinessKey);
        leave.setProcessInstanceId(String.valueOf(instance.getId()));
        leave.setStatus(resolveLeaveStatus(instance.getCurrentNodeName(), instance.getCurrentNodeKey()));
        leave.setPassedNodeLabelsJson(appendPassedNodeLabel(
                leave.getPassedNodeLabelsJson(),
                resolveNodeDisplayLabel(resolveStartNodeConfig(), "提交申请")
        ));
        leave.setCreatedAt(leave.getCreatedAt() == null ? now : leave.getCreatedAt());
        leave.setUpdatedAt(now);
        Leave savedLeave = leaveRepository.save(leave);
        recordStartAudit(savedLeave, variables);
        return savedLeave;
    }

    private FlwProcess resolveLeaveProcess() {
        ProcessDefinitionEntity activeDefinition = processDefinitionService.getActiveProcessDefinitionByKey(LEAVE_PROCESS_KEY);
        if (activeDefinition == null) {
            throw new RuntimeException("未找到激活的请假流程定义，请先在流程设计器发布 leave-process");
        }
        FlowLongDeploymentRecord lastDeployment = deploymentRecordRepository.findFirstByProcessKeyOrderByDeployedAtDescIdDesc(LEAVE_PROCESS_KEY);
        FlwProcess deployedProcess = resolveDeployedProcess(lastDeployment);
        if (deployedProcess != null
                && TENANT_ID.equals(deployedProcess.getTenantId())
                && activeDefinition.getVersion().equals(lastDeployment.getSourceDefinitionVersion())) {
            return deployedProcess;
        }
        Map<String, Object> deployment = deployDefinition(activeDefinition.getId());
        Object processId = deployment.get("processId");
        if (processId instanceof Number number) {
            return processService.getProcessById(number.longValue());
        }
        throw new RuntimeException("请假流程部署成功，但未能读取运行态流程");
    }

    private FlwProcess resolveDeployedProcess(FlowLongDeploymentRecord deploymentRecord) {
        if (deploymentRecord == null || deploymentRecord.getFlowLongProcessId() == null) {
            return null;
        }
        try {
            return processService.getProcessById(deploymentRecord.getFlowLongProcessId());
        } catch (Exception ex) {
            return null;
        }
    }

    public List<Map<String, Object>> getTasksByAssignee(String assignee) {
        if (assignee == null || assignee.isBlank()) {
            return Collections.emptyList();
        }

        Map<String, Object> businessConfig = resolveLeaveBusinessConfig();
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (Leave leave : leaveRepository.findAll()) {
            Long instanceId = parseInstanceId(leave.getProcessInstanceId());
            if (instanceId == null) {
                continue;
            }

            Optional<List<FlwTask>> activeTasks = queryService.getActiveTasksByInstanceId(instanceId);
            if (activeTasks.isEmpty()) {
                continue;
            }

            for (FlwTask task : activeTasks.get()) {
                FlwTaskActor matchedActor = findMatchedActor(task, assignee);
                if (matchedActor == null) {
                    continue;
                }

                Map<String, Object> taskData = new LinkedHashMap<>();
                taskData.put("id", task.getId());
                taskData.put("name", task.getTaskName());
                taskData.put("taskKey", task.getTaskKey());
                taskData.put("processInstanceId", String.valueOf(task.getInstanceId()));
                taskData.put("assignee", matchedActor.getActorName() == null || matchedActor.getActorName().isBlank()
                        ? matchedActor.getActorId()
                        : matchedActor.getActorName());
                taskData.put("assigneeId", matchedActor.getActorId());
                taskData.put("createTime", task.getCreateTime());
                taskData.put("businessKey", leave.getBusinessKey());
                taskData.put("applicant", leave.getApplicant());
                taskData.put("leaveStatus", leave.getStatus());
                taskData.put("days", leave.getDays());
                taskData.put("reason", leave.getReason());
                taskData.put("startDate", leave.getStartDate());
                taskData.put("endDate", leave.getEndDate());
                taskData.put("deptManager", leave.getDeptManager());
                taskData.put("generalManager", leave.getGeneralManager());
                taskData.put("processKey", LEAVE_PROCESS_KEY);
                applyNodeBusinessConfig(taskData, businessConfig, task.getTaskKey());
                tasks.add(taskData);
            }
        }
        return tasks;
    }

    public void completeTask(String taskId, Map<String, Object> variables, String processInstanceId, String status) {
        Long parsedTaskId = parseTaskId(taskId);
        FlwTask task = queryService.getTask(parsedTaskId);
        if (task == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }

        FlowCreator creator = currentCreator();
        Map<String, Object> payload = new LinkedHashMap<>();
        if (variables != null) {
            payload.putAll(variables);
        }

        boolean rejected = status != null && (
                "rejected".equalsIgnoreCase(status)
                        || "reject".equalsIgnoreCase(status)
                        || "terminated".equalsIgnoreCase(status)
        );
        payload.put("approved", !rejected);

        if (rejected) {
            runtimeService.terminate(task.getInstanceId(), task, creator);
        } else {
            taskService.complete(parsedTaskId, creator, payload);
        }

        String instanceKey = processInstanceId;
        if (instanceKey == null || instanceKey.isBlank()) {
            instanceKey = String.valueOf(task.getInstanceId());
        }

        Leave leave = leaveRepository.findByProcessInstanceId(instanceKey);
        Map<String, Object> nodeConfig = processBusinessConfigService.findNodeConfig(LEAVE_PROCESS_KEY, task.getTaskKey());
        Map<String, Object> mergedSnapshot = buildVariableSnapshot(leave, payload);
        recordTaskAudit(task, leave, nodeConfig, rejected ? "REJECTED" : "APPROVED", mergedSnapshot, payload);
        if (leave != null) {
            leave.setStatus(rejected ? "REJECTED" : resolveLeaveStatus(task.getInstanceId()));
            leave.setPassedNodeLabelsJson(appendPassedNodeLabel(
                    leave.getPassedNodeLabelsJson(),
                    resolveNodeDisplayLabel(nodeConfig, task.getTaskName())
            ));
            leave.setUpdatedAt(LocalDateTime.now());
            leaveRepository.save(leave);
        }
    }

    public List<Leave> getAllLeaves() {
        return leaveRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(Leave::getCreatedAt, java.util.Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .toList();
    }

    public Leave getLeaveById(Long id) {
        return leaveRepository.findById(id).orElse(null);
    }

    public List<Leave> getLeavesByApplicant(String applicant) {
        return leaveRepository.findByApplicantOrderByCreatedAtDesc(applicant);
    }

    public FlwTask getTaskById(String taskId) {
        return queryService.getTask(parseTaskId(taskId));
    }

    private void validateLeave(Leave leave) {
        if (leave == null) {
            throw new RuntimeException("请假参数不能为空");
        }
        if (isBlank(leave.getApplicant()) || isBlank(leave.getDeptManager()) || isBlank(leave.getGeneralManager())) {
            throw new RuntimeException("申请人、部门审批人、总经理审批人不能为空");
        }
        if (leave.getStartDate() == null || leave.getEndDate() == null) {
            throw new RuntimeException("请填写完整的请假时间");
        }
        if (leave.getDays() == null || leave.getDays() <= 0) {
            throw new RuntimeException("请假天数必须大于 0");
        }
        if (isBlank(leave.getReason())) {
            throw new RuntimeException("请填写请假原因");
        }
    }

    private FlowCreator currentCreator() {
        User currentUser = userService.getCurrentUser();
        String actorId = currentUser != null && !isBlank(currentUser.getUsername())
                ? currentUser.getUsername()
                : "system";
        String actorName = currentUser != null && !isBlank(currentUser.getNickname())
                ? currentUser.getNickname()
                : actorId;
        return FlowCreator.of(TENANT_ID, actorId, actorName);
    }

    private FlwTaskActor findMatchedActor(FlwTask task, String assignee) {
        return queryService.getActiveTaskActorsByTaskId(task.getId())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(actor -> assignee.equals(actor.getActorId()) || assignee.equals(actor.getActorName()))
                .findFirst()
                .orElse(null);
    }

    private void recordStartAudit(Leave leave, Map<String, Object> variables) {
        Map<String, Object> startNode = resolveStartNodeConfig();
        User currentUser = userService.getCurrentUser();
        ProcessInstanceAuditLog logEntry = new ProcessInstanceAuditLog();
        logEntry.setProcessKey(LEAVE_PROCESS_KEY);
        logEntry.setProcessInstanceId(leave.getProcessInstanceId());
        logEntry.setBusinessKey(leave.getBusinessKey());
        logEntry.setTaskKey(readNodeConfigValue(startNode, "nodeId"));
        logEntry.setTaskName(readNodeConfigValue(startNode, "nodeName"));
        logEntry.setNodeLabel(resolveNodeDisplayLabel(startNode, "提交申请"));
        logEntry.setPageLabel(readNodeConfigValue(startNode, "pageLabel"));
        logEntry.setFormKey(readNodeConfigValue(startNode, "formKey"));
        logEntry.setFormLabel(readNodeConfigValue(startNode, "formLabel"));
        logEntry.setActionType("START");
        logEntry.setSystemRemark("流程发起");
        logEntry.setVariableSnapshotJson(writeJson(buildVariableSnapshot(leave, variables)));
        logEntry.setFormSnapshotJson(writeJson(buildLeaveFormSnapshot(leave)));
        logEntry.setOperatorId(currentUser == null ? null : currentUser.getId());
        logEntry.setOperatorName(resolveOperatorName(currentUser));
        logEntry.setCreatedAt(LocalDateTime.now());
        processInstanceAuditLogRepository.save(logEntry);
    }

    private void recordTaskAudit(
            FlwTask task,
            Leave leave,
            Map<String, Object> nodeConfig,
            String actionType,
            Map<String, Object> variableSnapshot,
            Map<String, Object> payload
    ) {
        User currentUser = userService.getCurrentUser();
        ProcessInstanceAuditLog logEntry = new ProcessInstanceAuditLog();
        logEntry.setProcessKey(LEAVE_PROCESS_KEY);
        logEntry.setProcessInstanceId(String.valueOf(task.getInstanceId()));
        logEntry.setBusinessKey(leave == null ? null : leave.getBusinessKey());
        logEntry.setTaskId(task.getId());
        logEntry.setTaskKey(task.getTaskKey());
        logEntry.setTaskName(task.getTaskName());
        logEntry.setNodeLabel(resolveNodeDisplayLabel(nodeConfig, task.getTaskName()));
        logEntry.setPageLabel(readNodeConfigValue(nodeConfig, "pageLabel"));
        logEntry.setFormKey(readNodeConfigValue(nodeConfig, "formKey"));
        logEntry.setFormLabel(readNodeConfigValue(nodeConfig, "formLabel"));
        logEntry.setActionType(actionType);
        logEntry.setComment(resolvePrimaryComment(actionType, payload));
        logEntry.setApprovalComment(readPayloadText(payload, "approvalComment"));
        logEntry.setRejectReason(readPayloadText(payload, "rejectReason"));
        logEntry.setSystemRemark(readPayloadText(payload, "systemRemark"));
        logEntry.setVariableSnapshotJson(writeJson(variableSnapshot));
        logEntry.setFormSnapshotJson(writeJson(buildLeaveFormSnapshot(leave)));
        logEntry.setOperatorId(currentUser == null ? null : currentUser.getId());
        logEntry.setOperatorName(resolveOperatorName(currentUser));
        logEntry.setCreatedAt(LocalDateTime.now());
        processInstanceAuditLogRepository.save(logEntry);
    }

    private Map<String, Object> resolveStartNodeConfig() {
        try {
            Map<String, Object> config = processBusinessConfigService.getActiveProcessBusinessConfig(LEAVE_PROCESS_KEY);
            Object startNode = config.get("startNode");
            if (startNode instanceof Map<?, ?> map) {
                return castMap(map);
            }
        } catch (Exception ex) {
            log.warn("Failed to resolve start node config for {}", LEAVE_PROCESS_KEY, ex);
        }
        return Map.of();
    }

    private Map<String, Object> buildVariableSnapshot(Leave leave, Map<String, Object> variables) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        if (leave != null) {
            snapshot.put("applicant", leave.getApplicant());
            snapshot.put("deptManager", leave.getDeptManager());
            snapshot.put("generalManager", leave.getGeneralManager());
            snapshot.put("days", leave.getDays());
            snapshot.put("reason", leave.getReason());
            snapshot.put("businessKey", leave.getBusinessKey());
            snapshot.put("leaveStatus", leave.getStatus());
        }
        if (variables != null) {
            snapshot.putAll(variables);
        }
        return snapshot;
    }

    private Map<String, Object> buildLeaveFormSnapshot(Leave leave) {
        if (leave == null) {
            return Map.of();
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("applicant", leave.getApplicant());
        snapshot.put("deptManager", leave.getDeptManager());
        snapshot.put("generalManager", leave.getGeneralManager());
        snapshot.put("days", leave.getDays());
        snapshot.put("reason", leave.getReason());
        snapshot.put("startDate", leave.getStartDate());
        snapshot.put("endDate", leave.getEndDate());
        snapshot.put("businessKey", leave.getBusinessKey());
        return snapshot;
    }

    private String appendPassedNodeLabel(String rawLabelsJson, String nextLabel) {
        if (nextLabel == null || nextLabel.isBlank()) {
            return rawLabelsJson;
        }
        List<String> labels = readStringList(rawLabelsJson);
        if (labels.isEmpty() || !nextLabel.equals(labels.get(labels.size() - 1))) {
            labels.add(nextLabel);
        }
        return writeJson(labels);
    }

    private List<String> readStringList(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private String resolveNodeDisplayLabel(Map<String, Object> nodeConfig, String fallback) {
        String nodeLabel = readNodeConfigValue(nodeConfig, "nodeName");
        if (nodeLabel == null || nodeLabel.isBlank()) {
            nodeLabel = fallback;
        }
        String pageLabel = readNodeConfigValue(nodeConfig, "pageLabel");
        String formLabel = readNodeConfigValue(nodeConfig, "formLabel");
        List<String> parts = new ArrayList<>();
        if (nodeLabel != null && !nodeLabel.isBlank()) {
            parts.add(nodeLabel);
        }
        if (pageLabel != null && !pageLabel.isBlank()) {
            parts.add(pageLabel);
        }
        if (formLabel != null && !formLabel.isBlank()) {
            parts.add(formLabel);
        }
        return String.join(" / ", parts);
    }

    private String readNodeConfigValue(Map<String, Object> nodeConfig, String key) {
        if (nodeConfig == null || nodeConfig.isEmpty()) {
            return "";
        }
        return stringValue(nodeConfig.get(key));
    }

    private String resolveOperatorName(User currentUser) {
        if (currentUser == null) {
            return "system";
        }
        if (currentUser.getNickname() != null && !currentUser.getNickname().isBlank()) {
            return currentUser.getNickname();
        }
        return currentUser.getUsername();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new RuntimeException("序列化审计快照失败", ex);
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String readPayloadText(Map<String, Object> payload, String key) {
        if (payload == null) {
            return "";
        }
        Object value = payload.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String resolvePrimaryComment(String actionType, Map<String, Object> payload) {
        String approvalComment = readPayloadText(payload, "approvalComment");
        String rejectReason = readPayloadText(payload, "rejectReason");
        String systemRemark = readPayloadText(payload, "systemRemark");
        if ("REJECTED".equalsIgnoreCase(actionType) && !rejectReason.isBlank()) {
            return rejectReason;
        }
        if ("APPROVED".equalsIgnoreCase(actionType) && !approvalComment.isBlank()) {
            return approvalComment;
        }
        if (!approvalComment.isBlank()) {
            return approvalComment;
        }
        if (!rejectReason.isBlank()) {
            return rejectReason;
        }
        if (!systemRemark.isBlank()) {
            return systemRemark;
        }
        return readPayloadText(payload, "comment");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> source) {
        return (Map<String, Object>) source;
    }

    private Map<String, Object> resolveLeaveBusinessConfig() {
        try {
            return processBusinessConfigService.getActiveProcessBusinessConfig(LEAVE_PROCESS_KEY);
        } catch (Exception ex) {
            log.warn("Failed to resolve business config for {}", LEAVE_PROCESS_KEY, ex);
            return Map.of();
        }
    }

    private void applyNodeBusinessConfig(Map<String, Object> taskData, Map<String, Object> businessConfig, String taskKey) {
        if (businessConfig.isEmpty() || taskKey == null || taskKey.isBlank()) {
            return;
        }
        Object tasksRaw = businessConfig.get("tasks");
        if (!(tasksRaw instanceof List<?> tasks)) {
            return;
        }
        for (Object item : tasks) {
            if (!(item instanceof Map<?, ?> node)) {
                continue;
            }
            if (!taskKey.equals(node.get("nodeId"))) {
                continue;
            }
            taskData.put("formKey", node.get("formKey"));
            taskData.put("pageLabel", node.get("pageLabel"));
            taskData.put("formLabel", node.get("formLabel"));
            taskData.put("nodeLabel", node.get("nodeName"));
            return;
        }
    }

    private String resolveLeaveStatus(Long instanceId) {
        List<FlwTask> activeTasks = queryService.getActiveTasksByInstanceId(instanceId).orElseGet(Collections::emptyList);
        if (activeTasks.isEmpty()) {
            return "APPROVED";
        }
        FlwTask nextTask = activeTasks.get(0);
        return resolveLeaveStatus(nextTask.getTaskName(), nextTask.getTaskKey());
    }

    private String resolveLeaveStatus(String nodeName, String nodeKey) {
        String normalizedKey = nodeKey == null ? "" : nodeKey.toLowerCase();
        if (normalizedKey.contains("dept")) {
            return "PENDING_DEPT_APPROVAL";
        }
        if (normalizedKey.contains("general")) {
            return "PENDING_GENERAL_APPROVAL";
        }
        if (nodeName != null && !nodeName.isBlank()) {
            return "RUNNING_" + nodeName.replaceAll("\\s+", "_").toUpperCase();
        }
        return "RUNNING";
    }

    private Long parseInstanceId(String processInstanceId) {
        if (processInstanceId == null || processInstanceId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(processInstanceId);
        } catch (NumberFormatException ex) {
            log.warn("Ignore invalid processInstanceId: {}", processInstanceId);
            return null;
        }
    }

    private Long parseTaskId(String taskId) {
        try {
            return Long.valueOf(taskId);
        } catch (NumberFormatException ex) {
            throw new RuntimeException("非法任务ID: " + taskId, ex);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
