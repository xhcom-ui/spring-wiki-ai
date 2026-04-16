package com.example.activiti.service;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProcessMonitoringService {

    @Autowired
    private ProcessEngine processEngine;

    // Get all running process instances
    public List<Map<String, Object>> getRunningProcessInstances() {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().active().list();

        return processInstances.stream().map(processInstance -> {
            Map<String, Object> processMap = new HashMap<>();
            processMap.put("id", processInstance.getId());
            processMap.put("processDefinitionId", processInstance.getProcessDefinitionId());
            processMap.put("processDefinitionKey", processInstance.getProcessDefinitionKey());
            processMap.put("processDefinitionName", processInstance.getProcessDefinitionName());
            processMap.put("startTime", processInstance.getStartTime());
            processMap.put("businessKey", processInstance.getBusinessKey());
            return processMap;
        }).collect(Collectors.toList());
    }

    // Get all tasks
    public List<Map<String, Object>> getAllTasks() {
        TaskService taskService = processEngine.getTaskService();
        List<Task> tasks = taskService.createTaskQuery().list();

        return tasks.stream().map(task -> {
            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put("id", task.getId());
            taskMap.put("name", task.getName());
            taskMap.put("assignee", task.getAssignee());
            taskMap.put("createTime", task.getCreateTime());
            taskMap.put("dueDate", task.getDueDate());
            taskMap.put("processInstanceId", task.getProcessInstanceId());
            taskMap.put("processDefinitionId", task.getProcessDefinitionId());
            return taskMap;
        }).collect(Collectors.toList());
    }

    // Get tasks by assignee
    public List<Map<String, Object>> getTasksByAssignee(String assignee) {
        TaskService taskService = processEngine.getTaskService();
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(assignee).list();

        return tasks.stream().map(task -> {
            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put("id", task.getId());
            taskMap.put("name", task.getName());
            taskMap.put("assignee", task.getAssignee());
            taskMap.put("createTime", task.getCreateTime());
            taskMap.put("dueDate", task.getDueDate());
            taskMap.put("processInstanceId", task.getProcessInstanceId());
            taskMap.put("processDefinitionId", task.getProcessDefinitionId());
            return taskMap;
        }).collect(Collectors.toList());
    }

    // Get completed process instances
    public List<Map<String, Object>> getCompletedProcessInstances() {
        HistoryService historyService = processEngine.getHistoryService();
        List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery()
                .finished()
                .list();

        return historicProcessInstances.stream().map(historicProcessInstance -> {
            Map<String, Object> processMap = new HashMap<>();
            processMap.put("id", historicProcessInstance.getId());
            processMap.put("processDefinitionId", historicProcessInstance.getProcessDefinitionId());
            processMap.put("processDefinitionKey", historicProcessInstance.getProcessDefinitionKey());
            processMap.put("processDefinitionName", historicProcessInstance.getProcessDefinitionName());
            processMap.put("startTime", historicProcessInstance.getStartTime());
            processMap.put("endTime", historicProcessInstance.getEndTime());
            processMap.put("duration", historicProcessInstance.getDurationInMillis());
            processMap.put("businessKey", historicProcessInstance.getBusinessKey());
            return processMap;
        }).collect(Collectors.toList());
    }

    // Get process instance history
    public List<Map<String, Object>> getProcessInstanceHistory(String processInstanceId) {
        HistoryService historyService = processEngine.getHistoryService();
        List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .list();

        return historicTaskInstances.stream().map(historicTaskInstance -> {
            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put("id", historicTaskInstance.getId());
            taskMap.put("name", historicTaskInstance.getName());
            taskMap.put("assignee", historicTaskInstance.getAssignee());
            taskMap.put("createTime", historicTaskInstance.getCreateTime());
            taskMap.put("endTime", historicTaskInstance.getEndTime());
            taskMap.put("duration", historicTaskInstance.getDurationInMillis());
            return taskMap;
        }).collect(Collectors.toList());
    }

    // Get process statistics
    public Map<String, Object> getProcessStatistics() {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        HistoryService historyService = processEngine.getHistoryService();
        TaskService taskService = processEngine.getTaskService();

        Map<String, Object> statistics = new HashMap<>();

        // Count running process instances
        long runningProcessCount = runtimeService.createProcessInstanceQuery().active().count();
        statistics.put("runningProcessCount", runningProcessCount);

        // Count completed process instances
        long completedProcessCount = historyService.createHistoricProcessInstanceQuery().finished().count();
        statistics.put("completedProcessCount", completedProcessCount);

        // Count total tasks
        long totalTaskCount = taskService.createTaskQuery().count();
        statistics.put("totalTaskCount", totalTaskCount);

        // Count tasks by status
        long assignedTaskCount = taskService.createTaskQuery().taskAssigned().count();
        statistics.put("assignedTaskCount", assignedTaskCount);

        long unassignedTaskCount = taskService.createTaskQuery().taskUnassigned().count();
        statistics.put("unassignedTaskCount", unassignedTaskCount);

        return statistics;
    }
}
