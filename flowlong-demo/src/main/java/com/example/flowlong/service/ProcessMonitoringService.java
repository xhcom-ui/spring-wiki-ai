package com.example.flowlong.service;

import com.aizuda.bpm.engine.ProcessService;
import com.aizuda.bpm.engine.QueryService;
import com.aizuda.bpm.engine.entity.FlwHisInstance;
import com.aizuda.bpm.engine.entity.FlwHisTask;
import com.aizuda.bpm.engine.entity.FlwHisTaskActor;
import com.aizuda.bpm.engine.entity.FlwInstance;
import com.aizuda.bpm.engine.entity.FlwProcess;
import com.aizuda.bpm.engine.entity.FlwTask;
import com.aizuda.bpm.engine.entity.FlwTaskActor;
import com.example.flowlong.common.PageResult;
import com.example.flowlong.entity.Leave;
import com.example.flowlong.entity.ProcessInstanceAuditLog;
import com.example.flowlong.entity.ProcessDefinitionEntity;
import com.example.flowlong.repository.FlowLongDeploymentRecordRepository;
import com.example.flowlong.repository.LeaveRepository;
import com.example.flowlong.repository.ProcessInstanceAuditLogRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class ProcessMonitoringService {

    @Autowired
    private QueryService queryService;

    @Autowired
    private ProcessService processService;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private ProcessDefinitionService processDefinitionService;

    @Autowired
    private FlowLongDeploymentRecordRepository deploymentRecordRepository;

    @Autowired
    private ProcessBusinessConfigService processBusinessConfigService;

    @Autowired
    private ProcessInstanceAuditLogRepository processInstanceAuditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public PageResult<Map<String, Object>> queryRunningProcessInstances(int page, int size, String keyword) {
        List<Map<String, Object>> records = getRunningProcessInstances().stream()
                .filter(item -> matchesKeyword(keyword, item.values()))
                .toList();
        return paginate(records, page, size);
    }

    public PageResult<Map<String, Object>> queryCompletedProcessInstances(int page, int size, String keyword) {
        List<Map<String, Object>> records = getCompletedProcessInstances().stream()
                .filter(item -> matchesKeyword(keyword, item.values()))
                .toList();
        return paginate(records, page, size);
    }

    public PageResult<Map<String, Object>> queryTasks(int page, int size, String keyword, String assigneeFilter) {
        List<Map<String, Object>> records = getAllTasks().stream()
                .filter(item -> matchesKeyword(keyword, item.values()))
                .filter(item -> matchesAssigneeFilter(item, assigneeFilter))
                .toList();
        return paginate(records, page, size);
    }

    public List<Map<String, Object>> getRunningProcessInstances() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Leave leave : sortedLeaves()) {
            Long instanceId = parseInstanceId(leave.getProcessInstanceId());
            if (instanceId == null) {
                continue;
            }
            FlwInstance instance = queryService.getInstance(instanceId);
            if (instance == null) {
                continue;
            }
            result.add(toRuntimeInstanceView(instance, leave));
        }
        return result;
    }

    public List<Map<String, Object>> getCompletedProcessInstances() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Leave leave : sortedLeaves()) {
            Long instanceId = parseInstanceId(leave.getProcessInstanceId());
            if (instanceId == null) {
                continue;
            }
            FlwHisInstance instance = queryService.getHistInstance(instanceId);
            if (instance == null) {
                continue;
            }
            result.add(toHistoricInstanceView(instance, leave));
        }
        return result;
    }

    public List<Map<String, Object>> getAllTasks() {
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (Leave leave : sortedLeaves()) {
            tasks.addAll(getActiveTasksForLeave(leave));
        }
        return tasks;
    }

    public List<Map<String, Object>> getTasksByAssignee(String assignee) {
        if (assignee == null || assignee.isBlank()) {
            return List.of();
        }
        return getAllTasks().stream()
                .filter(task -> assignee.equals(task.get("assigneeId")) || assignee.equals(task.get("assignee")))
                .toList();
    }

    public List<Map<String, Object>> getProcessInstanceHistory(String processInstanceId) {
        Long instanceId = parseInstanceId(processInstanceId);
        if (instanceId == null) {
            return List.of();
        }
        Map<String, ProcessInstanceAuditLog> auditIndex = indexAuditLogsByTaskId(loadAuditLogs(processInstanceId));
        return queryService.getHisTasksByInstanceId(instanceId)
                .orElseGet(List::of)
                .stream()
                .sorted(Comparator.comparing(FlwHisTask::getCreateTime, Comparator.nullsLast(java.util.Date::compareTo)))
                .map(task -> {
                    Map<String, Object> taskMap = new LinkedHashMap<>();
                    taskMap.put("id", task.getId());
                    taskMap.put("name", task.getTaskName());
                    taskMap.put("taskKey", task.getTaskKey());
                    taskMap.put("createTime", task.getCreateTime());
                    taskMap.put("endTime", task.getFinishTime());
                    taskMap.put("duration", task.getDuration());
                    taskMap.put("taskState", task.getTaskState());
                    taskMap.put("assignee", resolveHistoricAssignee(task));
                    mergeAuditInfo(taskMap, auditIndex.get(String.valueOf(task.getId())));
                    return taskMap;
                })
                .toList();
    }

    public Map<String, Object> getProcessStatistics() {
        List<Map<String, Object>> allTasks = getAllTasks();
        long unassignedTaskCount = allTasks.stream()
                .filter(task -> task.get("assigneeId") == null && task.get("assignee") == null)
                .count();
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("runningProcessCount", getRunningProcessInstances().size());
        statistics.put("completedProcessCount", getCompletedProcessInstances().size());
        statistics.put("totalTaskCount", allTasks.size());
        statistics.put("assignedTaskCount", Math.max(0, allTasks.size() - unassignedTaskCount));
        statistics.put("unassignedTaskCount", unassignedTaskCount);
        statistics.put("leaveRecordCount", leaveRepository.count());
        return statistics;
    }

    public Map<String, Object> getProcessInstanceDetail(String processInstanceId) {
        Long instanceId = parseInstanceId(processInstanceId);
        if (instanceId == null) {
            throw new RuntimeException("流程实例不存在");
        }
        FlwInstance runtimeInstance = queryService.getInstance(instanceId);
        FlwHisInstance historicInstance = queryService.getHistInstance(instanceId);
        if (runtimeInstance == null && historicInstance == null) {
            throw new RuntimeException("流程实例不存在");
        }

        Leave leave = leaveRepository.findByProcessInstanceId(processInstanceId);
        FlwProcess process = resolveProcess(runtimeInstance != null ? runtimeInstance.getProcessId() : historicInstance.getProcessId());
        var deploymentRecord = resolveDeploymentRecord(process);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("processInstanceId", processInstanceId);
        detail.put("active", runtimeInstance != null);
        detail.put("status", runtimeInstance != null ? "RUNNING" : "COMPLETED");
        detail.put("processDefinitionId", process != null ? process.getId() : null);
        detail.put("processDefinitionKey", process != null ? process.getProcessKey() : null);
        detail.put("processDefinitionName", process != null ? process.getProcessName() : null);
        detail.put("processVersion", process != null ? process.getProcessVersion() : null);
        detail.put("deploymentId", deploymentRecord != null ? deploymentRecord.getId() : null);
        detail.put("deployedDefinitionVersion", deploymentRecord != null ? deploymentRecord.getSourceDefinitionVersion() : null);
        detail.put("businessKey", runtimeInstance != null ? runtimeInstance.getBusinessKey() : historicInstance.getBusinessKey());
        detail.put("startTime", runtimeInstance != null ? runtimeInstance.getCreateTime() : historicInstance.getCreateTime());
        detail.put("endTime", historicInstance != null ? historicInstance.getEndTime() : null);
        detail.put("currentNodeName", runtimeInstance != null ? runtimeInstance.getCurrentNodeName() : historicInstance.getCurrentNodeName());
        detail.put("currentNodeKey", runtimeInstance != null ? runtimeInstance.getCurrentNodeKey() : historicInstance.getCurrentNodeKey());
        detail.put("duration", historicInstance != null ? historicInstance.getDuration() : null);
        detail.put("leave", leave);

        Map<String, Object> variables = new LinkedHashMap<>();
        if (historicInstance != null) {
            variables.putAll(historicInstance.variableToMap());
        }
        if (runtimeInstance != null) {
            variables.putAll(runtimeInstance.variableToMap());
        }
        if (leave != null) {
            variables.putIfAbsent("applicant", leave.getApplicant());
            variables.putIfAbsent("deptManager", leave.getDeptManager());
            variables.putIfAbsent("generalManager", leave.getGeneralManager());
            variables.putIfAbsent("days", leave.getDays());
            variables.putIfAbsent("reason", leave.getReason());
            variables.putIfAbsent("businessKey", leave.getBusinessKey());
        }
        detail.put("variables", variables);

        List<Map<String, Object>> currentTasks = leave == null ? List.of() : getActiveTasksForLeave(leave);
        detail.put("currentTasks", currentTasks);

        List<Map<String, Object>> history = getProcessInstanceHistory(processInstanceId);
        detail.put("history", history);
        List<Map<String, Object>> auditLogs = toAuditLogViews(loadAuditLogs(processInstanceId));
        detail.put("auditLogs", auditLogs);
        detail.put("passedNodeLabels", readStringList(leave == null ? null : leave.getPassedNodeLabelsJson()));
        detail.put("variableSnapshots", auditLogs.stream()
                .map(item -> {
                    Map<String, Object> snapshot = new LinkedHashMap<>();
                    snapshot.put("actionType", item.get("actionType"));
                    snapshot.put("nodeLabel", item.get("nodeLabel"));
                    snapshot.put("operatorName", item.get("operatorName"));
                    snapshot.put("createdAt", item.get("createdAt"));
                    snapshot.put("variables", item.get("variableSnapshot"));
                    return snapshot;
                })
                .toList());
        String bpmnXml = resolveBpmnXml(process);
        detail.put("bpmnXml", bpmnXml);
        List<String> completedActivityIds = extractTaskKeys(history);
        List<String> activeActivityIds = extractTaskKeys(currentTasks);
        if (runtimeInstance != null && runtimeInstance.getCurrentNodeKey() != null && !runtimeInstance.getCurrentNodeKey().isBlank()) {
            if (!activeActivityIds.contains(runtimeInstance.getCurrentNodeKey())) {
                activeActivityIds.add(runtimeInstance.getCurrentNodeKey());
            }
        }
        Map<String, Object> trace = buildBpmnTraceData(
                bpmnXml,
                completedActivityIds,
                activeActivityIds,
                runtimeInstance != null ? runtimeInstance.getCurrentNodeKey() : null
        );
        detail.put("completedActivityIds", completedActivityIds);
        detail.put("activeActivityIds", activeActivityIds);
        detail.put("highlightedNodeIds", trace.get("highlightedNodeIds"));
        detail.put("highlightedGatewayIds", trace.get("highlightedGatewayIds"));
        detail.put("highlightedSequenceFlowIds", trace.get("highlightedSequenceFlowIds"));
        detail.put("currentActivityId", trace.get("currentActivityId"));
        detail.put("trajectoryNodes", buildTrajectoryNodes(history, currentTasks, processInstanceId));
        detail.put("trajectoryEdges", buildTrajectoryEdges(history, currentTasks, processInstanceId));
        return detail;
    }

    private List<Leave> sortedLeaves() {
        return leaveRepository.findAll().stream()
                .sorted(Comparator.comparing(Leave::getCreatedAt, Comparator.nullsLast(java.time.LocalDateTime::compareTo)).reversed())
                .toList();
    }

    private PageResult<Map<String, Object>> paginate(List<Map<String, Object>> records, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int fromIndex = Math.min((safePage - 1) * safeSize, records.size());
        int toIndex = Math.min(fromIndex + safeSize, records.size());
        return new PageResult<>(records.subList(fromIndex, toIndex), records.size(), safePage, safeSize);
    }

    private boolean matchesKeyword(String keyword, Collection<Object> values) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String needle = keyword.trim().toLowerCase();
        return values.stream()
                .filter(item -> item != null && !String.valueOf(item).isBlank())
                .map(item -> String.valueOf(item).toLowerCase())
                .anyMatch(item -> item.contains(needle));
    }

    private boolean matchesAssigneeFilter(Map<String, Object> item, String assigneeFilter) {
        if (assigneeFilter == null || assigneeFilter.isBlank() || "all".equalsIgnoreCase(assigneeFilter)) {
            return true;
        }
        boolean assigned = item.get("assignee") != null && !String.valueOf(item.get("assignee")).isBlank();
        if ("assigned".equalsIgnoreCase(assigneeFilter)) {
            return assigned;
        }
        if ("unassigned".equalsIgnoreCase(assigneeFilter)) {
            return !assigned;
        }
        return true;
    }

    private Map<String, Object> toRuntimeInstanceView(FlwInstance instance, Leave leave) {
        FlwProcess process = resolveProcess(instance.getProcessId());
        Map<String, Object> processMap = new LinkedHashMap<>();
        processMap.put("id", instance.getId());
        processMap.put("processDefinitionId", process != null ? process.getId() : instance.getProcessId());
        processMap.put("processDefinitionKey", process != null ? process.getProcessKey() : null);
        processMap.put("processDefinitionName", process != null ? process.getProcessName() : null);
        processMap.put("processVersion", process != null ? process.getProcessVersion() : null);
        processMap.put("startTime", instance.getCreateTime());
        processMap.put("businessKey", instance.getBusinessKey());
        processMap.put("currentNodeName", instance.getCurrentNodeName());
        processMap.put("currentNodeKey", instance.getCurrentNodeKey());
        processMap.put("passedNodeLabels", readStringList(leave != null ? leave.getPassedNodeLabelsJson() : null));
        processMap.put("applicant", leave != null ? leave.getApplicant() : null);
        processMap.put("leaveStatus", leave != null ? leave.getStatus() : null);
        return processMap;
    }

    private Map<String, Object> toHistoricInstanceView(FlwHisInstance instance, Leave leave) {
        FlwProcess process = resolveProcess(instance.getProcessId());
        Map<String, Object> processMap = new LinkedHashMap<>();
        processMap.put("id", instance.getId());
        processMap.put("processDefinitionId", process != null ? process.getId() : instance.getProcessId());
        processMap.put("processDefinitionKey", process != null ? process.getProcessKey() : null);
        processMap.put("processDefinitionName", process != null ? process.getProcessName() : null);
        processMap.put("processVersion", process != null ? process.getProcessVersion() : null);
        processMap.put("startTime", instance.getCreateTime());
        processMap.put("endTime", instance.getEndTime());
        processMap.put("duration", instance.getDuration());
        processMap.put("businessKey", instance.getBusinessKey());
        processMap.put("passedNodeLabels", readStringList(leave != null ? leave.getPassedNodeLabelsJson() : null));
        processMap.put("applicant", leave != null ? leave.getApplicant() : null);
        processMap.put("leaveStatus", leave != null ? leave.getStatus() : null);
        return processMap;
    }

    private List<Map<String, Object>> getActiveTasksForLeave(Leave leave) {
        Long instanceId = parseInstanceId(leave.getProcessInstanceId());
        if (instanceId == null) {
            return List.of();
        }
        return queryService.getActiveTasksByInstanceId(instanceId)
                .orElseGet(List::of)
                .stream()
                .sorted(Comparator.comparing(FlwTask::getCreateTime, Comparator.nullsLast(java.util.Date::compareTo)))
                .flatMap(task -> {
                    List<FlwTaskActor> actors = queryService.getActiveTaskActorsByTaskId(task.getId()).orElseGet(List::of);
                    if (actors.isEmpty()) {
                        return java.util.stream.Stream.of(toTaskView(task, null, leave));
                    }
                    return actors.stream().map(actor -> toTaskView(task, actor, leave));
                })
                .toList();
    }

    private Map<String, Object> toTaskView(FlwTask task, FlwTaskActor actor, Leave leave) {
        Map<String, Object> taskMap = new LinkedHashMap<>();
        taskMap.put("id", task.getId());
        taskMap.put("name", task.getTaskName());
        taskMap.put("taskKey", task.getTaskKey());
        taskMap.put("processInstanceId", String.valueOf(task.getInstanceId()));
        taskMap.put("assignee", actor == null ? null : (actor.getActorName() == null || actor.getActorName().isBlank() ? actor.getActorId() : actor.getActorName()));
        taskMap.put("assigneeId", actor == null ? null : actor.getActorId());
        taskMap.put("createTime", task.getCreateTime());
        taskMap.put("businessKey", leave.getBusinessKey());
        taskMap.put("applicant", leave.getApplicant());
        taskMap.put("leaveStatus", leave.getStatus());
        taskMap.put("passedNodeLabels", readStringList(leave.getPassedNodeLabelsJson()));
        applyNodeBusinessConfig(taskMap, task.getTaskKey(), task.getTaskName());
        return taskMap;
    }

    private String resolveHistoricAssignee(FlwHisTask task) {
        List<FlwHisTaskActor> actors = queryService.getHisTaskActorsByTaskId(task.getId());
        if (actors.isEmpty()) {
            return null;
        }
        FlwHisTaskActor actor = actors.get(0);
        return actor.getActorName() == null || actor.getActorName().isBlank() ? actor.getActorId() : actor.getActorName();
    }

    private List<ProcessInstanceAuditLog> loadAuditLogs(String processInstanceId) {
        return processInstanceAuditLogRepository.findByProcessInstanceIdOrderByCreatedAtAscIdAsc(processInstanceId);
    }

    private Map<String, ProcessInstanceAuditLog> indexAuditLogsByTaskId(List<ProcessInstanceAuditLog> auditLogs) {
        Map<String, ProcessInstanceAuditLog> result = new LinkedHashMap<>();
        for (ProcessInstanceAuditLog item : auditLogs) {
            if (item.getTaskId() == null) {
                continue;
            }
            result.put(String.valueOf(item.getTaskId()), item);
        }
        return result;
    }

    private void mergeAuditInfo(Map<String, Object> taskMap, ProcessInstanceAuditLog auditLog) {
        if (auditLog == null) {
            applyNodeBusinessConfig(taskMap, stringValue(taskMap.get("taskKey")), stringValue(taskMap.get("name")));
            return;
        }
        taskMap.put("actionType", auditLog.getActionType());
        taskMap.put("comment", auditLog.getComment());
        taskMap.put("nodeLabel", auditLog.getNodeLabel());
        taskMap.put("pageLabel", auditLog.getPageLabel());
        taskMap.put("formKey", auditLog.getFormKey());
        taskMap.put("formLabel", auditLog.getFormLabel());
        taskMap.put("operatorName", auditLog.getOperatorName());
        taskMap.put("variableSnapshot", readJsonMap(auditLog.getVariableSnapshotJson()));
        taskMap.put("formSnapshot", readJsonMap(auditLog.getFormSnapshotJson()));
    }

    private List<Map<String, Object>> toAuditLogViews(List<ProcessInstanceAuditLog> auditLogs) {
        return auditLogs.stream()
                .map(item -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", item.getId());
                    row.put("taskId", item.getTaskId());
                    row.put("taskKey", item.getTaskKey());
                    row.put("taskName", item.getTaskName());
                    row.put("nodeLabel", item.getNodeLabel());
                    row.put("pageLabel", item.getPageLabel());
                    row.put("formKey", item.getFormKey());
                    row.put("formLabel", item.getFormLabel());
                    row.put("actionType", item.getActionType());
                    row.put("comment", item.getComment());
                    row.put("approvalComment", item.getApprovalComment());
                    row.put("rejectReason", item.getRejectReason());
                    row.put("systemRemark", item.getSystemRemark());
                    row.put("operatorName", item.getOperatorName());
                    row.put("createdAt", item.getCreatedAt());
                    row.put("variableSnapshot", readJsonMap(item.getVariableSnapshotJson()));
                    row.put("formSnapshot", readJsonMap(item.getFormSnapshotJson()));
                    return row;
                })
                .toList();
    }

    private void applyNodeBusinessConfig(Map<String, Object> taskMap, String taskKey, String fallbackName) {
        try {
            Map<String, Object> nodeConfig = processBusinessConfigService.findNodeConfig("leave-process", taskKey);
            if (nodeConfig == null || nodeConfig.isEmpty()) {
                taskMap.putIfAbsent("nodeLabel", fallbackName);
                return;
            }
            taskMap.put("nodeLabel", resolveNodeDisplayLabel(nodeConfig, fallbackName));
            taskMap.put("pageLabel", nodeConfig.get("pageLabel"));
            taskMap.put("formKey", nodeConfig.get("formKey"));
            taskMap.put("formLabel", nodeConfig.get("formLabel"));
        } catch (Exception ex) {
            taskMap.putIfAbsent("nodeLabel", fallbackName);
        }
    }

    private String resolveNodeDisplayLabel(Map<String, Object> nodeConfig, String fallbackName) {
        List<String> parts = new ArrayList<>();
        String nodeName = stringValue(nodeConfig.get("nodeName"));
        String pageLabel = stringValue(nodeConfig.get("pageLabel"));
        String formLabel = stringValue(nodeConfig.get("formLabel"));
        if (!nodeName.isBlank()) {
            parts.add(nodeName);
        } else if (fallbackName != null && !fallbackName.isBlank()) {
            parts.add(fallbackName);
        }
        if (!pageLabel.isBlank()) {
            parts.add(pageLabel);
        }
        if (!formLabel.isBlank()) {
            parts.add(formLabel);
        }
        return String.join(" / ", parts);
    }

    private List<String> readStringList(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private Map<String, Object> readJsonMap(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            return Map.of("raw", rawJson);
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private List<Map<String, Object>> buildTrajectoryNodes(List<Map<String, Object>> history, List<Map<String, Object>> currentTasks, String processInstanceId) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        Map<String, Object> start = new LinkedHashMap<>();
        start.put("id", processInstanceId + "_start");
        start.put("activityId", "start");
        start.put("activityName", "流程开始");
        start.put("activityType", "startEvent");
        start.put("status", "COMPLETED");
        nodes.add(start);

        int index = 0;
        for (Map<String, Object> item : history) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", processInstanceId + "_history_" + index++);
            node.put("activityId", item.get("taskKey"));
            node.put("activityName", item.get("name"));
            node.put("nodeLabel", item.get("nodeLabel"));
            node.put("activityType", "userTask");
            node.put("assignee", item.get("assignee"));
            node.put("startTime", item.get("createTime"));
            node.put("endTime", item.get("endTime"));
            node.put("duration", item.get("duration"));
            node.put("status", "COMPLETED");
            nodes.add(node);
        }
        for (Map<String, Object> item : currentTasks) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", processInstanceId + "_active_" + index++);
            node.put("activityId", item.get("taskKey"));
            node.put("activityName", item.get("name"));
            node.put("nodeLabel", item.get("nodeLabel"));
            node.put("activityType", "userTask");
            node.put("assignee", item.get("assignee"));
            node.put("startTime", item.get("createTime"));
            node.put("endTime", null);
            node.put("duration", null);
            node.put("status", "ACTIVE");
            nodes.add(node);
        }

        if (currentTasks.isEmpty()) {
            Map<String, Object> end = new LinkedHashMap<>();
            end.put("id", processInstanceId + "_end");
            end.put("activityId", "end");
            end.put("activityName", "流程结束");
            end.put("activityType", "endEvent");
            end.put("status", "COMPLETED");
            nodes.add(end);
        }
        return nodes;
    }

    private List<Map<String, Object>> buildTrajectoryEdges(List<Map<String, Object>> history, List<Map<String, Object>> currentTasks, String processInstanceId) {
        List<Map<String, Object>> nodes = buildTrajectoryNodes(history, currentTasks, processInstanceId);
        List<Map<String, Object>> edges = new ArrayList<>();
        for (int i = 1; i < nodes.size(); i++) {
            Map<String, Object> edge = new LinkedHashMap<>();
            edge.put("from", nodes.get(i - 1).get("id"));
            edge.put("to", nodes.get(i).get("id"));
            edges.add(edge);
        }
        return edges;
    }

    private FlwProcess resolveProcess(Long processId) {
        if (processId == null) {
            return null;
        }
        try {
            return processService.getProcessById(processId);
        } catch (Exception ex) {
            log.debug("Failed to load FlowLong process {}", processId, ex);
            return null;
        }
    }

    private Long parseInstanceId(String processInstanceId) {
        if (processInstanceId == null || processInstanceId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(processInstanceId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveBpmnXml(FlwProcess process) {
        var deployment = resolveDeploymentRecord(process);
        if (deployment != null && deployment.getSourceDefinitionId() != null) {
            ProcessDefinitionEntity definition = processDefinitionService.getProcessDefinitionById(deployment.getSourceDefinitionId());
            if (definition != null) {
                return definition.getBpmnXml();
            }
        }
        if (process == null || process.getProcessKey() == null) {
            return null;
        }
        ProcessDefinitionEntity definition = processDefinitionService.getActiveProcessDefinitionByKey(process.getProcessKey());
        return definition == null ? null : definition.getBpmnXml();
    }

    private com.example.flowlong.entity.FlowLongDeploymentRecord resolveDeploymentRecord(FlwProcess process) {
        if (process == null || process.getProcessKey() == null) {
            return null;
        }
        if (process.getProcessVersion() != null) {
            return deploymentRecordRepository.findFirstByProcessKeyAndFlowLongProcessVersionOrderByDeployedAtDescIdDesc(
                    process.getProcessKey(),
                    process.getProcessVersion()
            );
        }
        return null;
    }

    private List<String> extractTaskKeys(List<Map<String, Object>> tasks) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Map<String, Object> task : tasks) {
            Object rawKey = task.get("taskKey");
            if (rawKey instanceof String key && !key.isBlank()) {
                result.add(key);
            }
        }
        return new ArrayList<>(result);
    }

    private Map<String, Object> buildBpmnTraceData(String bpmnXml, List<String> completedActivityIds, List<String> activeActivityIds, String currentNodeKey) {
        Map<String, Object> trace = new LinkedHashMap<>();
        LinkedHashSet<String> highlightedNodeIds = new LinkedHashSet<>();
        LinkedHashSet<String> highlightedSequenceFlowIds = new LinkedHashSet<>();
        LinkedHashSet<String> highlightedGatewayIds = new LinkedHashSet<>();
        highlightedNodeIds.addAll(completedActivityIds);
        highlightedNodeIds.addAll(activeActivityIds);
        if (currentNodeKey != null && !currentNodeKey.isBlank()) {
            highlightedNodeIds.add(currentNodeKey);
        }

        if (bpmnXml == null || bpmnXml.isBlank()) {
            trace.put("highlightedNodeIds", List.copyOf(highlightedNodeIds));
            trace.put("highlightedGatewayIds", List.copyOf(highlightedGatewayIds));
            trace.put("highlightedSequenceFlowIds", List.copyOf(highlightedSequenceFlowIds));
            trace.put("currentActivityId", currentNodeKey);
            return trace;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(bpmnXml)));
            BpmnGraph bpmnGraph = buildBpmnGraph(document);

            List<String> route = new ArrayList<>(completedActivityIds);
            if (currentNodeKey != null && !currentNodeKey.isBlank() && !route.contains(currentNodeKey)) {
                route.add(currentNodeKey);
            }
            for (String activeActivityId : activeActivityIds) {
                if (!route.contains(activeActivityId)) {
                    route.add(activeActivityId);
                }
            }

            if (!route.isEmpty()) {
                connectFirstReachable(bpmnGraph.startEventIds, route.get(0), bpmnGraph, highlightedNodeIds, highlightedSequenceFlowIds);
            }
            for (int i = 1; i < route.size(); i++) {
                connectNodes(route.get(i - 1), route.get(i), bpmnGraph, highlightedNodeIds, highlightedSequenceFlowIds);
            }
            if (!activeActivityIds.isEmpty()) {
                for (String activeActivityId : activeActivityIds) {
                    connectFirstReachable(List.of(activeActivityId), bpmnGraph.endEventIds, bpmnGraph, highlightedNodeIds, highlightedSequenceFlowIds);
                }
            } else if (!route.isEmpty()) {
                connectFirstReachable(List.of(route.get(route.size() - 1)), bpmnGraph.endEventIds, bpmnGraph, highlightedNodeIds, highlightedSequenceFlowIds);
            }
            for (String nodeId : highlightedNodeIds) {
                String type = bpmnGraph.nodeTypes.get(nodeId);
                if (type != null && type.toLowerCase().contains("gateway")) {
                    highlightedGatewayIds.add(nodeId);
                }
            }
        } catch (Exception ex) {
            log.debug("Failed to build BPMN trace data", ex);
        }

        trace.put("highlightedNodeIds", List.copyOf(highlightedNodeIds));
        trace.put("highlightedGatewayIds", List.copyOf(highlightedGatewayIds));
        trace.put("highlightedSequenceFlowIds", List.copyOf(highlightedSequenceFlowIds));
        trace.put("currentActivityId", currentNodeKey);
        return trace;
    }

    private void connectFirstReachable(Collection<String> startIds, String targetId, BpmnGraph graph,
                                       Set<String> highlightedNodeIds, Set<String> highlightedSequenceFlowIds) {
        if (targetId == null || targetId.isBlank()) {
            return;
        }
        for (String startId : startIds) {
            if (connectNodes(startId, targetId, graph, highlightedNodeIds, highlightedSequenceFlowIds)) {
                return;
            }
        }
    }

    private void connectFirstReachable(List<String> sourceIds, List<String> targetIds, BpmnGraph graph,
                                       Set<String> highlightedNodeIds, Set<String> highlightedSequenceFlowIds) {
        for (String sourceId : sourceIds) {
            for (String targetId : targetIds) {
                if (connectNodes(sourceId, targetId, graph, highlightedNodeIds, highlightedSequenceFlowIds)) {
                    return;
                }
            }
        }
    }

    private boolean connectNodes(String sourceId, String targetId, BpmnGraph graph,
                                 Set<String> highlightedNodeIds, Set<String> highlightedSequenceFlowIds) {
        if (sourceId == null || sourceId.isBlank() || targetId == null || targetId.isBlank()) {
            return false;
        }
        if (sourceId.equals(targetId)) {
            highlightedNodeIds.add(sourceId);
            return true;
        }

        Deque<String> queue = new ArrayDeque<>();
        Map<String, SequenceEdge> previousEdge = new HashMap<>();
        Set<String> visited = new HashSet<>();
        queue.add(sourceId);
        visited.add(sourceId);

        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            for (SequenceEdge edge : graph.adjacency.getOrDefault(current, List.of())) {
                if (visited.contains(edge.targetRef)) {
                    continue;
                }
                visited.add(edge.targetRef);
                previousEdge.put(edge.targetRef, edge);
                if (edge.targetRef.equals(targetId)) {
                    applyPath(sourceId, targetId, previousEdge, highlightedNodeIds, highlightedSequenceFlowIds);
                    return true;
                }
                queue.addLast(edge.targetRef);
            }
        }
        return false;
    }

    private void applyPath(String sourceId, String targetId, Map<String, SequenceEdge> previousEdge,
                           Set<String> highlightedNodeIds, Set<String> highlightedSequenceFlowIds) {
        List<SequenceEdge> path = new ArrayList<>();
        String cursor = targetId;
        while (!sourceId.equals(cursor)) {
            SequenceEdge edge = previousEdge.get(cursor);
            if (edge == null) {
                break;
            }
            path.add(0, edge);
            cursor = edge.sourceRef;
        }
        highlightedNodeIds.add(sourceId);
        for (SequenceEdge edge : path) {
            highlightedNodeIds.add(edge.sourceRef);
            highlightedNodeIds.add(edge.targetRef);
            highlightedSequenceFlowIds.add(edge.id);
        }
    }

    private BpmnGraph buildBpmnGraph(Document document) {
        BpmnGraph graph = new BpmnGraph();
        NodeList nodes = document.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            if (!(nodes.item(i) instanceof Element element)) {
                continue;
            }
            String tagName = element.getTagName();
            String id = element.getAttribute("id");
            if (isTag(tagName, "sequenceFlow")) {
                String sourceRef = element.getAttribute("sourceRef");
                String targetRef = element.getAttribute("targetRef");
                if (!id.isBlank() && !sourceRef.isBlank() && !targetRef.isBlank()) {
                    graph.adjacency.computeIfAbsent(sourceRef, key -> new ArrayList<>())
                            .add(new SequenceEdge(id, sourceRef, targetRef));
                }
            } else if (!id.isBlank()) {
                graph.nodeTypes.put(id, stripPrefix(tagName));
                if (isTag(tagName, "startEvent")) {
                    graph.startEventIds.add(id);
                } else if (isTag(tagName, "endEvent")) {
                    graph.endEventIds.add(id);
                }
            }
        }
        return graph;
    }

    private boolean isTag(String tagName, String localName) {
        return tagName != null && (tagName.equals(localName) || tagName.endsWith(":" + localName));
    }

    private String stripPrefix(String tagName) {
        int index = tagName == null ? -1 : tagName.indexOf(':');
        return index >= 0 ? tagName.substring(index + 1) : tagName;
    }

    private static final class SequenceEdge {
        private final String id;
        private final String sourceRef;
        private final String targetRef;

        private SequenceEdge(String id, String sourceRef, String targetRef) {
            this.id = id;
            this.sourceRef = sourceRef;
            this.targetRef = targetRef;
        }
    }

    private static final class BpmnGraph {
        private final Map<String, List<SequenceEdge>> adjacency = new HashMap<>();
        private final Map<String, String> nodeTypes = new HashMap<>();
        private final List<String> startEventIds = new ArrayList<>();
        private final List<String> endEventIds = new ArrayList<>();
    }
}
