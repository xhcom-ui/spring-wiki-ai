package com.example.activiti.service;

import com.example.activiti.exception.NotFoundException;
import com.example.activiti.util.PageResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProcessMonitoringService {

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private RepositoryService repositoryService;

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
        RuntimeService runtimeService = processEngine.getRuntimeService();
        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().active().list();

        return processInstances.stream().map(processInstance -> {
            Map<String, Object> variables = collectVariables(processInstance.getId());
            Map<String, Object> processMap = new LinkedHashMap<>();
            processMap.put("id", processInstance.getId());
            processMap.put("processDefinitionId", processInstance.getProcessDefinitionId());
            processMap.put("processDefinitionKey", processInstance.getProcessDefinitionKey());
            processMap.put("processDefinitionName", processInstance.getProcessDefinitionName());
            processMap.put("startTime", processInstance.getStartTime());
            processMap.put("businessKey", processInstance.getBusinessKey());
            processMap.put("applicant", readString(variables, "applicant"));
            processMap.put("currentNodeName", processInstance.getActivityId());
            return processMap;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAllTasks() {
        TaskService taskService = processEngine.getTaskService();
        HistoryService historyService = processEngine.getHistoryService();
        List<Task> tasks = taskService.createTaskQuery().list();

        return tasks.stream().map(task -> {
            Map<String, Object> variables = collectVariables(task.getProcessInstanceId());
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            Map<String, Object> taskMap = new LinkedHashMap<>();
            taskMap.put("id", task.getId());
            taskMap.put("name", task.getName());
            taskMap.put("assignee", task.getAssignee());
            taskMap.put("createTime", task.getCreateTime());
            taskMap.put("dueDate", task.getDueDate());
            taskMap.put("processInstanceId", task.getProcessInstanceId());
            taskMap.put("processDefinitionId", task.getProcessDefinitionId());
            taskMap.put("processDefinitionName", historicProcessInstance == null ? null : historicProcessInstance.getProcessDefinitionName());
            taskMap.put("businessKey", historicProcessInstance == null ? null : historicProcessInstance.getBusinessKey());
            taskMap.put("applicant", readString(variables, "applicant"));
            taskMap.put("currentNodeName", task.getTaskDefinitionKey());
            taskMap.put("variables", variables);
            return taskMap;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getTasksByAssignee(String assignee) {
        return getAllTasks().stream()
                .filter(task -> assignee != null && assignee.equals(task.get("assignee")))
                .toList();
    }

    public List<Map<String, Object>> getCompletedProcessInstances() {
        HistoryService historyService = processEngine.getHistoryService();
        List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery()
                .finished()
                .list();

        return historicProcessInstances.stream().map(historicProcessInstance -> {
            Map<String, Object> variables = collectVariables(historicProcessInstance.getId());
            Map<String, Object> processMap = new LinkedHashMap<>();
            processMap.put("id", historicProcessInstance.getId());
            processMap.put("processDefinitionId", historicProcessInstance.getProcessDefinitionId());
            processMap.put("processDefinitionKey", historicProcessInstance.getProcessDefinitionKey());
            processMap.put("processDefinitionName", historicProcessInstance.getProcessDefinitionName());
            processMap.put("startTime", historicProcessInstance.getStartTime());
            processMap.put("endTime", historicProcessInstance.getEndTime());
            processMap.put("duration", historicProcessInstance.getDurationInMillis());
            processMap.put("businessKey", historicProcessInstance.getBusinessKey());
            processMap.put("applicant", readString(variables, "applicant"));
            return processMap;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getProcessInstanceHistory(String processInstanceId) {
        HistoryService historyService = processEngine.getHistoryService();
        List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceStartTime()
                .asc()
                .list();

        return historicTaskInstances.stream().map(historicTaskInstance -> {
            Map<String, Object> taskMap = new LinkedHashMap<>();
            taskMap.put("id", historicTaskInstance.getId());
            taskMap.put("name", historicTaskInstance.getName());
            taskMap.put("assignee", historicTaskInstance.getAssignee());
            taskMap.put("createTime", historicTaskInstance.getCreateTime());
            taskMap.put("endTime", historicTaskInstance.getEndTime());
            taskMap.put("duration", historicTaskInstance.getDurationInMillis());
            return taskMap;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getProcessStatistics() {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        HistoryService historyService = processEngine.getHistoryService();
        TaskService taskService = processEngine.getTaskService();

        Map<String, Object> statistics = new HashMap<>();
        long runningProcessCount = runtimeService.createProcessInstanceQuery().active().count();
        statistics.put("runningProcessCount", runningProcessCount);

        long completedProcessCount = historyService.createHistoricProcessInstanceQuery().finished().count();
        statistics.put("completedProcessCount", completedProcessCount);

        long totalTaskCount = taskService.createTaskQuery().count();
        statistics.put("totalTaskCount", totalTaskCount);

        long unassignedTaskCount = taskService.createTaskQuery().taskUnassigned().count();
        long assignedTaskCount = Math.max(0, totalTaskCount - unassignedTaskCount);
        statistics.put("assignedTaskCount", assignedTaskCount);
        statistics.put("unassignedTaskCount", unassignedTaskCount);

        return statistics;
    }

    public Map<String, Object> getProcessInstanceDetail(String processInstanceId) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        HistoryService historyService = processEngine.getHistoryService();
        TaskService taskService = processEngine.getTaskService();

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
        detail.put("processDefinitionId", processDefinitionId);
        detail.put("processDefinitionKey", runtimeInstance != null ? runtimeInstance.getProcessDefinitionKey() : historicInstance.getProcessDefinitionKey());
        detail.put("processDefinitionName", runtimeInstance != null ? runtimeInstance.getProcessDefinitionName() : historicInstance.getProcessDefinitionName());
        detail.put("deploymentId", resolveDeploymentId(processDefinitionId));
        detail.put("businessKey", runtimeInstance != null ? runtimeInstance.getBusinessKey() : historicInstance.getBusinessKey());
        detail.put("startTime", runtimeInstance != null ? runtimeInstance.getStartTime() : historicInstance.getStartTime());
        detail.put("endTime", historicInstance != null ? historicInstance.getEndTime() : null);

        Map<String, Object> variables = collectVariables(processInstanceId);
        detail.put("variables", variables);

        List<Map<String, Object>> currentTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list()
                .stream()
                .map(task -> {
                    Map<String, Object> taskMap = new LinkedHashMap<>();
                    taskMap.put("id", task.getId());
                    taskMap.put("name", task.getName());
                    taskMap.put("assignee", task.getAssignee());
                    taskMap.put("createTime", task.getCreateTime());
                    taskMap.put("dueDate", task.getDueDate());
                    return taskMap;
                }).toList();
        detail.put("currentTasks", currentTasks);

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
            node.put("activityName", activity.getActivityName());
            node.put("activityType", activity.getActivityType());
            node.put("assignee", activity.getAssignee());
            node.put("startTime", activity.getStartTime());
            node.put("endTime", activity.getEndTime());
            node.put("duration", activity.getDurationInMillis());
            node.put("status", activity.getEndTime() == null ? "ACTIVE" : "COMPLETED");
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
        detail.put("history", getProcessInstanceHistory(processInstanceId));
        return detail;
    }

    private Map<String, Object> collectVariables(String processInstanceId) {
        Map<String, Object> variables = new LinkedHashMap<>();
        if (processInstanceId == null || processInstanceId.isBlank()) {
            return variables;
        }
        RuntimeService runtimeService = processEngine.getRuntimeService();
        HistoryService historyService = processEngine.getHistoryService();
        try {
            variables.putAll(runtimeService.getVariables(processInstanceId));
        } catch (Exception ignored) {
            log.debug("加载运行时变量失败: {}", processInstanceId);
        }
        List<HistoricVariableInstance> historicVariables = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list();
        for (HistoricVariableInstance item : historicVariables) {
            variables.putIfAbsent(item.getVariableName(), item.getValue());
        }
        return variables;
    }

    private String readString(Map<String, Object> variables, String key) {
        if (variables == null || key == null) {
            return null;
        }
        Object value = variables.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private boolean matchesKeyword(String keyword, Collection<Object> values) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String needle = keyword.trim().toLowerCase();
        return values.stream()
                .filter(item -> item != null && !(item instanceof Map))
                .map(String::valueOf)
                .anyMatch(value -> value.toLowerCase().contains(needle));
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

    private String resolveDeploymentId(String processDefinitionId) {
        if (processDefinitionId == null || processDefinitionId.isBlank()) {
            return null;
        }
        var processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        return processDefinition == null ? null : processDefinition.getDeploymentId();
    }
}
