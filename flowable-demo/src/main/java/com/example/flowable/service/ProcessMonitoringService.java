package com.example.flowable.service;

import com.example.flowable.entity.ProcessDefinitionEntity;
import com.example.flowable.exception.NotFoundException;
import com.example.flowable.repository.LeaveRepository;
import com.example.flowable.repository.ProcessDefinitionRepository;
import com.example.flowable.util.PageResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricVariableUpdate;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProcessMonitoringService {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ProcessDefinitionRepository processDefinitionRepository;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private ProcessDesignRuntimeService processDesignRuntimeService;

    public Map<String, Object> queryRunningProcessInstances(int page, int size, String keyword) {
        List<Map<String, Object>> records = getRunningProcessInstances().stream()
                .filter(item -> matchesKeyword(keyword, item.values()))
                .toList();
        return PageResponseUtils.paginate(records, page, size);
    }

    public Map<String, Object> queryCompletedProcessInstances(int page, int size, String keyword) {
        List<Map<String, Object>> records = getCompletedProcessInstances().stream()
                .filter(item -> matchesKeyword(keyword, item.values()))
                .toList();
        return PageResponseUtils.paginate(records, page, size);
    }

    public Map<String, Object> queryTasks(int page, int size, String keyword, String assigneeFilter) {
        List<Map<String, Object>> records = getAllTasks().stream()
                .filter(item -> matchesKeyword(keyword, item.values()))
                .filter(item -> matchesAssigneeFilter(item, assigneeFilter))
                .toList();
        return PageResponseUtils.paginate(records, page, size);
    }

    public List<Map<String, Object>> getRunningProcessInstances() {
        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().active().list();
        return processInstances.stream().map(processInstance -> {
            Map<String, Object> processMap = new HashMap<>();
            processMap.put("id", processInstance.getId());
            processMap.put("processDefinitionId", processInstance.getProcessDefinitionId());
            processMap.put("processDefinitionKey", processInstance.getProcessDefinitionKey());
            processMap.put("processDefinitionName", processInstance.getProcessDefinitionName());
            processMap.put("startTime", processInstance.getStartTime());
            processMap.put("businessKey", processInstance.getBusinessKey());
            enrichProcessSummary(processMap, processInstance.getId(), processInstance.getProcessDefinitionId());
            return processMap;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAllTasks() {
        return taskService.createTaskQuery().list().stream().map(task -> {
            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put("id", task.getId());
            taskMap.put("name", task.getName());
            taskMap.put("assignee", task.getAssignee());
            taskMap.put("createTime", task.getCreateTime());
            taskMap.put("dueDate", task.getDueDate());
            taskMap.put("processInstanceId", task.getProcessInstanceId());
            taskMap.put("processDefinitionId", task.getProcessDefinitionId());
            enrichTaskSummary(taskMap, task.getTaskDefinitionKey(), task.getProcessInstanceId(), task.getProcessDefinitionId());
            return taskMap;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getTasksByAssignee(String assignee) {
        return taskService.createTaskQuery().taskAssignee(assignee).list().stream().map(task -> {
            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put("id", task.getId());
            taskMap.put("name", task.getName());
            taskMap.put("assignee", task.getAssignee());
            taskMap.put("createTime", task.getCreateTime());
            taskMap.put("dueDate", task.getDueDate());
            taskMap.put("processInstanceId", task.getProcessInstanceId());
            taskMap.put("processDefinitionId", task.getProcessDefinitionId());
            enrichTaskSummary(taskMap, task.getTaskDefinitionKey(), task.getProcessInstanceId(), task.getProcessDefinitionId());
            return taskMap;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getCompletedProcessInstances() {
        return historyService.createHistoricProcessInstanceQuery().finished().list().stream().map(instance -> {
            Map<String, Object> processMap = new HashMap<>();
            processMap.put("id", instance.getId());
            processMap.put("processDefinitionId", instance.getProcessDefinitionId());
            processMap.put("processDefinitionKey", instance.getProcessDefinitionKey());
            processMap.put("processDefinitionName", instance.getProcessDefinitionName());
            processMap.put("startTime", instance.getStartTime());
            processMap.put("endTime", instance.getEndTime());
            processMap.put("duration", instance.getDurationInMillis());
            processMap.put("businessKey", instance.getBusinessKey());
            enrichProcessSummary(processMap, instance.getId(), instance.getProcessDefinitionId());
            return processMap;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getProcessInstanceHistory(String processInstanceId) {
        ProcessDefinitionEntity definition = processDesignRuntimeService.resolveDefinitionByProcessInstanceId(processInstanceId);
        return historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .list()
                .stream()
                .map(task -> {
                    Map<String, Object> taskMap = new HashMap<>();
                    taskMap.put("id", task.getId());
                    taskMap.put("name", task.getName());
                    taskMap.put("taskDefinitionKey", task.getTaskDefinitionKey());
                    taskMap.put("assignee", task.getAssignee());
                    taskMap.put("createTime", task.getCreateTime());
                    taskMap.put("endTime", task.getEndTime());
                    taskMap.put("duration", task.getDurationInMillis());
                    if (definition != null) {
                        taskMap.putAll(processDesignRuntimeService.getNodeConfigByDefinition(definition, task.getTaskDefinitionKey()));
                    }
                    return taskMap;
                }).collect(Collectors.toList());
    }

    public Map<String, Object> getProcessStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        long totalTaskCount = taskService.createTaskQuery().count();
        long unassignedTaskCount = taskService.createTaskQuery().taskUnassigned().count();
        statistics.put("runningProcessCount", runtimeService.createProcessInstanceQuery().active().count());
        statistics.put("completedProcessCount", historyService.createHistoricProcessInstanceQuery().finished().count());
        statistics.put("totalTaskCount", totalTaskCount);
        statistics.put("assignedTaskCount", Math.max(0, totalTaskCount - unassignedTaskCount));
        statistics.put("unassignedTaskCount", unassignedTaskCount);
        return statistics;
    }

    public Map<String, Object> getProcessInstanceDetail(String processInstanceId) {
        ProcessInstance runtimeInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (runtimeInstance == null && historicInstance == null) {
            throw new NotFoundException("流程实例不存在");
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("processInstanceId", processInstanceId);
        detail.put("active", runtimeInstance != null);
        detail.put("status", runtimeInstance != null ? "RUNNING" : "COMPLETED");
        String processDefinitionId = runtimeInstance != null ? runtimeInstance.getProcessDefinitionId() : historicInstance.getProcessDefinitionId();
        ProcessDefinitionEntity definition = processDesignRuntimeService.resolveDefinitionByProcessDefinitionId(processDefinitionId);
        detail.put("processDefinitionId", processDefinitionId);
        detail.put("processDefinitionKey", runtimeInstance != null ? runtimeInstance.getProcessDefinitionKey() : historicInstance.getProcessDefinitionKey());
        detail.put("processDefinitionName", runtimeInstance != null ? runtimeInstance.getProcessDefinitionName() : historicInstance.getProcessDefinitionName());
        String deploymentId = resolveDeploymentId(processDefinitionId);
        detail.put("deploymentId", deploymentId);
        detail.put("businessKey", runtimeInstance != null ? runtimeInstance.getBusinessKey() : historicInstance.getBusinessKey());
        detail.put("startTime", runtimeInstance != null ? runtimeInstance.getStartTime() : historicInstance.getStartTime());
        detail.put("endTime", historicInstance != null ? historicInstance.getEndTime() : null);
        detail.put("duration", historicInstance != null ? historicInstance.getDurationInMillis() : null);
        var leave = leaveRepository.findByProcessInstanceId(processInstanceId);
        if (leave != null) {
            detail.put("leave", leave);
        }

        Map<String, Object> variables = new LinkedHashMap<>();
        if (runtimeInstance != null) {
            variables.putAll(runtimeService.getVariables(processInstanceId));
        }
        List<HistoricVariableInstance> historicVariables = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list();
        for (HistoricVariableInstance item : historicVariables) {
            variables.putIfAbsent(item.getVariableName(), item.getValue());
        }
        detail.put("variables", variables);
        List<Map<String, Object>> comments = taskService.getProcessInstanceComments(processInstanceId)
                .stream()
                .map(this::toCommentMap)
                .toList();
        detail.put("comments", comments);

        List<Map<String, Object>> currentTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list()
                .stream()
                .map(task -> {
                    Map<String, Object> taskMap = new LinkedHashMap<>();
                    taskMap.put("id", task.getId());
                    taskMap.put("name", task.getName());
                    taskMap.put("taskDefinitionKey", task.getTaskDefinitionKey());
                    taskMap.put("assignee", task.getAssignee());
                    taskMap.put("createTime", task.getCreateTime());
                    taskMap.put("dueDate", task.getDueDate());
                    if (definition != null) {
                        taskMap.putAll(processDesignRuntimeService.getNodeConfigByDefinition(definition, task.getTaskDefinitionKey()));
                    }
                    return taskMap;
                }).toList();
        detail.put("currentTasks", currentTasks);

        String bpmnXml = resolveBpmnXml(processDefinitionId, deploymentId);
        detail.put("bpmnXml", bpmnXml);

        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();

        List<Map<String, Object>> trajectoryNodes = new ArrayList<>();
        List<Map<String, Object>> trajectoryEdges = new ArrayList<>();
        for (int i = 0; i < activities.size(); i++) {
            HistoricActivityInstance activity = activities.get(i);
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", activity.getActivityId() + "_" + i);
            node.put("activityId", activity.getActivityId());
            node.put("activityInstanceId", activity.getId());
            node.put("activityName", activity.getActivityName());
            node.put("activityType", activity.getActivityType());
            node.put("assignee", activity.getAssignee());
            node.put("startTime", activity.getStartTime());
            node.put("endTime", activity.getEndTime());
            node.put("duration", activity.getDurationInMillis());
            node.put("status", activity.getEndTime() == null ? "ACTIVE" : "COMPLETED");
            if (definition != null) {
                node.putAll(processDesignRuntimeService.getNodeConfigByDefinition(definition, activity.getActivityId()));
            }
            trajectoryNodes.add(node);

            if (i > 0) {
                Map<String, Object> edge = new LinkedHashMap<>();
                edge.put("from", trajectoryNodes.get(i - 1).get("id"));
                edge.put("to", node.get("id"));
                trajectoryEdges.add(edge);
            }
        }

        detail.put("trajectoryNodes", trajectoryNodes);
        detail.put("trajectoryEdges", trajectoryEdges);
        List<Map<String, Object>> history = getProcessInstanceHistory(processInstanceId);
        detail.put("history", history);
        List<Map<String, Object>> variableUpdates = historyService.createHistoricDetailQuery()
                .processInstanceId(processInstanceId)
                .variableUpdates()
                .orderByTime()
                .asc()
                .list()
                .stream()
                .filter(HistoricVariableUpdate.class::isInstance)
                .map(HistoricVariableUpdate.class::cast)
                .map(this::toVariableUpdateMap)
                .toList();
        detail.put("variableUpdates", variableUpdates);
        detail.put("nodeDetails", buildNodeDetails(definition, trajectoryNodes, currentTasks, history, comments, variableUpdates, variables, leave));

        List<String> completedActivityIds = extractCompletedActivityIds(history);
        List<String> activeActivityIds = extractActiveActivityIds(currentTasks);
        String currentActivityId = resolveCurrentActivityId(runtimeInstance, activeActivityIds);
        Map<String, Object> trace = buildBpmnTraceData(bpmnXml, completedActivityIds, activeActivityIds, currentActivityId);
        detail.put("completedActivityIds", completedActivityIds);
        detail.put("activeActivityIds", activeActivityIds);
        detail.put("highlightedNodeIds", trace.get("highlightedNodeIds"));
        detail.put("highlightedGatewayIds", trace.get("highlightedGatewayIds"));
        detail.put("highlightedSequenceFlowIds", trace.get("highlightedSequenceFlowIds"));
        detail.put("currentActivityId", trace.get("currentActivityId"));
        if (definition != null) {
            detail.put("visitedNodeLabels", processDesignRuntimeService.buildTraceNodeLabels(definition, completedActivityIds));
            detail.put("currentNodeLabels", processDesignRuntimeService.buildTraceNodeLabels(definition, activeActivityIds));
        } else {
            detail.put("visitedNodeLabels", List.of());
            detail.put("currentNodeLabels", List.of());
        }
        return detail;
    }

    private void enrichProcessSummary(Map<String, Object> target, String processInstanceId, String processDefinitionId) {
        var leave = leaveRepository.findByProcessInstanceId(processInstanceId);
        if (leave != null) {
            target.put("applicant", leave.getApplicant());
            target.put("leaveStatus", leave.getStatus());
        }
        ProcessDefinitionEntity definition = processDesignRuntimeService.resolveDefinitionByProcessDefinitionId(processDefinitionId);
        if (definition == null) {
            return;
        }
        target.put("designerType", definition.getDesignerType());
        List<String> currentIds = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list()
                .stream()
                .map(Task::getTaskDefinitionKey)
                .filter(item -> item != null && !item.isBlank())
                .toList();
        var currentLabels = processDesignRuntimeService.buildTraceNodeLabels(definition, currentIds);
        if (!currentLabels.isEmpty()) {
            target.put("currentNodeName", currentLabels.get(0).get("summary"));
            target.put("currentNodeLabels", currentLabels);
        }
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

    private void enrichTaskSummary(Map<String, Object> target, String taskDefinitionKey, String processInstanceId, String processDefinitionId) {
        var leave = leaveRepository.findByProcessInstanceId(processInstanceId);
        if (leave != null) {
            target.put("applicant", leave.getApplicant());
        }
        ProcessDefinitionEntity definition = processDesignRuntimeService.resolveDefinitionByProcessDefinitionId(processDefinitionId);
        if (definition == null) {
            return;
        }
        target.put("designerType", definition.getDesignerType());
        target.putAll(processDesignRuntimeService.getNodeConfigByDefinition(definition, taskDefinitionKey));
    }

    private Map<String, Object> buildNodeDetails(ProcessDefinitionEntity definition,
                                                 List<Map<String, Object>> trajectoryNodes,
                                                 List<Map<String, Object>> currentTasks,
                                                 List<Map<String, Object>> history,
                                                 List<Map<String, Object>> comments,
                                                 List<Map<String, Object>> variableUpdates,
                                                 Map<String, Object> variables,
                                                 Object leave) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (definition == null) {
            return result;
        }

        Map<String, Set<String>> nodeTaskIds = new LinkedHashMap<>();
        Map<String, Set<String>> nodeActivityInstanceIds = new LinkedHashMap<>();

        for (Map<String, Object> node : trajectoryNodes) {
            String nodeId = stringValue(node.get("activityId"));
            if (nodeId.isBlank()) {
                continue;
            }
            Map<String, Object> nodeDetail = ensureNodeDetail(result, definition, nodeId);
            nodeDetail.putIfAbsent("latestExecution", node);
            String activityInstanceId = stringValue(node.get("activityInstanceId"));
            if (!activityInstanceId.isBlank()) {
                nodeActivityInstanceIds.computeIfAbsent(nodeId, key -> new LinkedHashSet<>()).add(activityInstanceId);
            }
        }

        for (Map<String, Object> task : history) {
            String nodeId = stringValue(task.get("taskDefinitionKey"));
            if (nodeId.isBlank()) {
                continue;
            }
            Map<String, Object> nodeDetail = ensureNodeDetail(result, definition, nodeId);
            getDetailList(nodeDetail, "history").add(task);
            String taskId = stringValue(task.get("id"));
            if (!taskId.isBlank()) {
                nodeTaskIds.computeIfAbsent(nodeId, key -> new LinkedHashSet<>()).add(taskId);
            }
        }

        for (Map<String, Object> task : currentTasks) {
            String nodeId = stringValue(task.get("taskDefinitionKey"));
            if (nodeId.isBlank()) {
                continue;
            }
            Map<String, Object> nodeDetail = ensureNodeDetail(result, definition, nodeId);
            getDetailList(nodeDetail, "currentTasks").add(task);
            String taskId = stringValue(task.get("id"));
            if (!taskId.isBlank()) {
                nodeTaskIds.computeIfAbsent(nodeId, key -> new LinkedHashSet<>()).add(taskId);
            }
        }

        for (Map.Entry<String, Object> entry : result.entrySet()) {
            String nodeId = entry.getKey();
            Map<String, Object> nodeDetail = castMap(entry.getValue());
            Set<String> taskIds = nodeTaskIds.getOrDefault(nodeId, Set.of());
            Set<String> activityInstanceIds = nodeActivityInstanceIds.getOrDefault(nodeId, Set.of());

            List<Map<String, Object>> nodeComments = comments.stream()
                    .filter(item -> taskIds.contains(stringValue(item.get("taskId"))))
                    .toList();
            nodeDetail.put("comments", nodeComments);

            List<Map<String, Object>> nodeVariableUpdates = variableUpdates.stream()
                    .filter(item -> taskIds.contains(stringValue(item.get("taskId")))
                            || activityInstanceIds.contains(stringValue(item.get("activityInstanceId"))))
                    .toList();
            nodeDetail.put("variableUpdates", nodeVariableUpdates);
            nodeDetail.put("formSnapshot", buildNodeFormSnapshot(nodeId, nodeDetail, nodeVariableUpdates, variables, leave));
        }
        return result;
    }

    private Map<String, Object> ensureNodeDetail(Map<String, Object> target, ProcessDefinitionEntity definition, String nodeId) {
        return castMap(target.computeIfAbsent(nodeId, key -> {
            Map<String, Object> nodeDetail = new LinkedHashMap<>();
            nodeDetail.put("nodeId", nodeId);
            nodeDetail.putAll(processDesignRuntimeService.getNodeConfigByDefinition(definition, nodeId));
            nodeDetail.put("history", new ArrayList<Map<String, Object>>());
            nodeDetail.put("currentTasks", new ArrayList<Map<String, Object>>());
            nodeDetail.put("comments", new ArrayList<Map<String, Object>>());
            nodeDetail.put("variableUpdates", new ArrayList<Map<String, Object>>());
            return nodeDetail;
        }));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getDetailList(Map<String, Object> detail, String key) {
        return (List<Map<String, Object>>) detail.computeIfAbsent(key, ignored -> new ArrayList<Map<String, Object>>());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castSchema(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }
        return List.of();
    }

    private Map<String, Object> buildNodeFormSnapshot(String nodeId,
                                                      Map<String, Object> nodeDetail,
                                                      List<Map<String, Object>> nodeVariableUpdates,
                                                      Map<String, Object> variables,
                                                      Object leave) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("nodeId", nodeId);
        snapshot.put("nodeName", defaultString(stringValue(nodeDetail.get("nodeName")), nodeId));
        snapshot.put("formKey", stringValue(nodeDetail.get("formKey")));
        snapshot.put("formLabel", stringValue(nodeDetail.get("formLabel")));
        snapshot.put("pageLabel", stringValue(nodeDetail.get("pageLabel")));
        snapshot.put("assignmentExpression", stringValue(nodeDetail.get("assignmentExpression")));
        List<Map<String, Object>> schema = castSchema(nodeDetail.get("fieldSchema"));
        snapshot.put("schema", schema);

        LinkedHashSet<String> candidateKeys = new LinkedHashSet<>(List.of(
                "applicant", "days", "reason", "startDate", "endDate",
                "deptManager", "generalManager", "approved", "taskOutcome",
                "comment", "managerAdvice", "priority", "finalDecisionNote", "allowResubmit"
        ));
        String variableKey = stringValue(nodeDetail.get("variableKey"));
        if (!variableKey.isBlank()) {
            candidateKeys.add(variableKey);
        }
        for (Map<String, Object> item : nodeVariableUpdates) {
            String variableName = stringValue(item.get("variableName"));
            if (!variableName.isBlank()) {
                candidateKeys.add(variableName);
            }
        }
        for (Map<String, Object> item : schema) {
            String fieldName = stringValue(item.get("field"));
            if (!fieldName.isBlank()) {
                candidateKeys.add(fieldName);
            }
        }

        Map<String, Object> values = new LinkedHashMap<>();
        List<Map<String, Object>> fields = new ArrayList<>();
        for (Map<String, Object> item : schema) {
            String key = stringValue(item.get("field"));
            if (key.isBlank()) {
                continue;
            }
            Object value = variables.get(key);
            if (value == null && leave != null) {
                value = readLeaveValue(leave, key);
            }
            values.put(key, value);
            Map<String, Object> field = new LinkedHashMap<>();
            field.put("key", key);
            field.put("label", defaultString(stringValue(item.get("label")), humanizeKey(key)));
            field.put("component", stringValue(item.get("component")));
            field.put("help", stringValue(item.get("help")));
            field.put("placeholder", stringValue(item.get("placeholder")));
            field.put("validator", item.get("validator"));
            field.put("value", value);
            fields.add(field);
        }
        for (String key : candidateKeys) {
            if (values.containsKey(key)) {
                continue;
            }
            Object value = variables.get(key);
            if (value == null && leave != null) {
                value = readLeaveValue(leave, key);
            }
            if (value == null) {
                continue;
            }
            values.put(key, value);
            Map<String, Object> field = new LinkedHashMap<>();
            field.put("key", key);
            field.put("label", humanizeKey(key));
            field.put("value", value);
            fields.add(field);
        }
        snapshot.put("values", values);
        snapshot.put("fields", fields);
        return snapshot;
    }

    private Object readLeaveValue(Object leave, String key) {
        try {
            String getterName = "get" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
            return leave.getClass().getMethod(getterName).invoke(leave);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String humanizeKey(String key) {
        return switch (key) {
            case "applicant" -> "申请人";
            case "days" -> "请假天数";
            case "reason" -> "请假原因";
            case "startDate" -> "开始时间";
            case "endDate" -> "结束时间";
            case "deptManager" -> "部门审批人";
            case "generalManager" -> "总经理审批人";
            case "approved" -> "审批结果";
            case "taskOutcome" -> "任务结果";
            case "comment" -> "审批意见";
            case "managerAdvice" -> "经理建议";
            case "priority" -> "优先级";
            case "finalDecisionNote" -> "最终审批说明";
            case "allowResubmit" -> "允许补充材料";
            default -> key;
        };
    }

    private Map<String, Object> toCommentMap(org.flowable.engine.task.Comment item) {
        Map<String, Object> commentMap = new LinkedHashMap<>();
        commentMap.put("id", item.getId());
        commentMap.put("taskId", item.getTaskId());
        commentMap.put("type", item.getType());
        commentMap.put("userId", item.getUserId());
        commentMap.put("time", item.getTime());
        commentMap.put("message", item.getFullMessage());
        return commentMap;
    }

    private Map<String, Object> toVariableUpdateMap(HistoricVariableUpdate item) {
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("id", item.getId());
        update.put("variableName", item.getVariableName());
        update.put("value", item.getValue());
        update.put("time", item.getTime());
        update.put("taskId", item.getTaskId());
        update.put("activityInstanceId", item.getActivityInstanceId());
        update.put("revision", item.getRevision());
        return update;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String resolveDeploymentId(String processDefinitionId) {
        if (processDefinitionId == null || processDefinitionId.isBlank()) {
            return null;
        }
        var processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        return processDefinition == null ? null : processDefinition.getDeploymentId();
    }

    private String resolveBpmnXml(String processDefinitionId, String deploymentId) {
        if (deploymentId != null && !deploymentId.isBlank()) {
            ProcessDefinitionEntity byDeployment = processDefinitionRepository.findByDeploymentId(deploymentId);
            if (byDeployment != null && byDeployment.getBpmnXml() != null && !byDeployment.getBpmnXml().isBlank()) {
                return byDeployment.getBpmnXml();
            }
        }
        if (processDefinitionId == null || processDefinitionId.isBlank()) {
            return null;
        }
        var processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        if (processDefinition == null || processDefinition.getKey() == null) {
            return null;
        }
        int version = processDefinition.getVersion();
        ProcessDefinitionEntity entity = processDefinitionRepository.findByProcessKeyOrderByVersionDesc(processDefinition.getKey())
                .stream()
                .filter(item -> item.getVersion() != null && item.getVersion() == version)
                .findFirst()
                .orElse(null);
        return entity == null ? null : entity.getBpmnXml();
    }

    private List<String> extractCompletedActivityIds(List<Map<String, Object>> history) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Map<String, Object> item : history) {
            Object activityId = item.get("taskDefinitionKey");
            if (activityId instanceof String value && !value.isBlank()) {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    private List<String> extractActiveActivityIds(List<Map<String, Object>> currentTasks) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Map<String, Object> item : currentTasks) {
            Object activityId = item.get("taskDefinitionKey");
            if (activityId instanceof String value && !value.isBlank()) {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    private String resolveCurrentActivityId(ProcessInstance runtimeInstance, List<String> activeActivityIds) {
        if (runtimeInstance != null && runtimeInstance.getActivityId() != null && !runtimeInstance.getActivityId().isBlank()) {
            return runtimeInstance.getActivityId();
        }
        return activeActivityIds.isEmpty() ? null : activeActivityIds.get(0);
    }

    private Map<String, Object> buildBpmnTraceData(String bpmnXml, List<String> completedActivityIds,
                                                   List<String> activeActivityIds, String currentActivityId) {
        Map<String, Object> trace = new LinkedHashMap<>();
        LinkedHashSet<String> highlightedNodeIds = new LinkedHashSet<>();
        LinkedHashSet<String> highlightedSequenceFlowIds = new LinkedHashSet<>();
        LinkedHashSet<String> highlightedGatewayIds = new LinkedHashSet<>();
        highlightedNodeIds.addAll(completedActivityIds);
        highlightedNodeIds.addAll(activeActivityIds);
        if (currentActivityId != null && !currentActivityId.isBlank()) {
            highlightedNodeIds.add(currentActivityId);
        }

        if (bpmnXml == null || bpmnXml.isBlank()) {
            trace.put("highlightedNodeIds", List.copyOf(highlightedNodeIds));
            trace.put("highlightedGatewayIds", List.copyOf(highlightedGatewayIds));
            trace.put("highlightedSequenceFlowIds", List.copyOf(highlightedSequenceFlowIds));
            trace.put("currentActivityId", currentActivityId);
            return trace;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(bpmnXml)));
            BpmnGraph bpmnGraph = buildBpmnGraph(document);

            List<String> route = new ArrayList<>(completedActivityIds);
            if (currentActivityId != null && !currentActivityId.isBlank() && !route.contains(currentActivityId)) {
                route.add(currentActivityId);
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
        trace.put("currentActivityId", currentActivityId);
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
