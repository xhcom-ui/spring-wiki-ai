package com.example.flowable.service;

import com.example.flowable.entity.Leave;
import com.example.flowable.entity.ProcessDefinitionEntity;
import com.example.flowable.entity.User;
import com.example.flowable.exception.NotFoundException;
import com.example.flowable.repository.LeaveRepository;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class FlowableService {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private ProcessDesignRuntimeService processDesignRuntimeService;

    @Autowired
    private HistoryService historyService;

    // 部署流程
    public void deployProcess(String processName, String modelContent) {
        Deployment deployment = repositoryService.createDeployment()
                .name(processName)
                .addString(processName + ".bpmn20.xml", modelContent)
                .deploy();
        log.info("流程部署成功，部署ID: {}, 流程名称: {}", deployment.getId(), processName);
    }

    // 启动请假流程
    public Leave startLeaveProcess(Leave leave) {
        ProcessDefinitionEntity definition = processDesignRuntimeService.resolveStartDefinition("leave-process");
        if (definition == null) {
            throw new NotFoundException("未找到可用的请假流程定义");
        }
        String businessKey = "LEAVE-" + System.currentTimeMillis();
        Map<String, Object> variables = processDesignRuntimeService.buildStartVariables(leave, definition);
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("leave-process", businessKey, variables);
        leave.setProcessInstanceId(instance.getId());
        leave.setBusinessKey(businessKey);
        leave.setDeptManager(stringValue(variables.get("deptManager"), leave.getDeptManager()));
        leave.setGeneralManager(stringValue(variables.get("generalManager"), leave.getGeneralManager()));
        leave.setStatus("RUNNING");
        leave.setCreatedAt(LocalDateTime.now());
        leave.setUpdatedAt(LocalDateTime.now());
        return leaveRepository.save(leave);
    }

    // 查询用户的待办任务
    public List<Map<String, Object>> getTasksForUser(User user) {
        return processDesignRuntimeService.getTasksForUser(user);
    }

    // 完成任务
    public String completeTask(String taskId, Map<String, Object> variables, String processInstanceId, String outcome, String username, String comment) {
        if (comment != null && !comment.isBlank()) {
            taskService.addComment(taskId, processInstanceId, String.format("[%s] %s", outcome == null ? "审批" : outcome, comment.trim()));
        }
        taskService.complete(taskId, variables);
        log.info("任务完成，任务ID: {}", taskId);

        // 更新请假状态
        if (processInstanceId != null) {
            String nextStatus = processDesignRuntimeService.resolveLeaveStatusAfterTaskComplete(processInstanceId, outcome);
            updateLeaveStatus(processInstanceId, nextStatus);
            return nextStatus;
        }
        return null;
    }

    // 更新请假状态
    public void updateLeaveStatus(String processInstanceId, String status) {
        Leave leave = leaveRepository.findByProcessInstanceId(processInstanceId);
        if (leave != null) {
            leave.setStatus(status);
            leave.setUpdatedAt(LocalDateTime.now());
            leaveRepository.save(leave);
        }
    }

    // 获取所有请假申请
    public List<Leave> getAllLeaves() {
        return leaveRepository.findAll().stream()
                .map(this::enrichLeaveTrace)
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .toList();
    }

    public List<Leave> getLeavesByApplicant(String applicant) {
        return leaveRepository.findByApplicantOrderByCreatedAtDesc(applicant).stream()
                .map(this::enrichLeaveTrace)
                .toList();
    }

    // 根据ID获取请假申请
    public Leave getLeaveById(Long id) {
        return leaveRepository.findById(id).map(this::enrichLeaveTrace).orElse(null);
    }

    // 获取流程实例
    public ProcessInstance getProcessInstance(String processInstanceId) {
        return runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
    }

    public Task getTaskById(String taskId) {
        return taskService.createTaskQuery().taskId(taskId).singleResult();
    }

    public void claimTaskIfNecessary(String taskId, String username) {
        Task task = getTaskById(taskId);
        if (task != null && (task.getAssignee() == null || task.getAssignee().isBlank())) {
            taskService.claim(taskId, username);
        }
    }

    // 部署流程从文件
    public void deployProcessFromFile(String processName, String resourceName) {
        Deployment deployment = repositoryService.createDeployment()
                .name(processName)
                .addClasspathResource("processes/" + resourceName)
                .deploy();
        log.info("流程部署成功，部署ID: {}, 流程名称: {}", deployment.getId(), processName);
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }

    private Leave enrichLeaveTrace(Leave leave) {
        if (leave == null || leave.getProcessInstanceId() == null || leave.getProcessInstanceId().isBlank()) {
            return leave;
        }
        ProcessDefinitionEntity definition = processDesignRuntimeService.resolveDefinitionByProcessInstanceId(leave.getProcessInstanceId());
        if (definition == null) {
            return leave;
        }
        List<String> visitedIds = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(leave.getProcessInstanceId())
                .orderByHistoricTaskInstanceStartTime()
                .asc()
                .list()
                .stream()
                .map(HistoricTaskInstance::getTaskDefinitionKey)
                .filter(key -> key != null && !key.isBlank())
                .toList();
        List<String> currentIds = taskService.createTaskQuery()
                .processInstanceId(leave.getProcessInstanceId())
                .list()
                .stream()
                .map(Task::getTaskDefinitionKey)
                .filter(key -> key != null && !key.isBlank())
                .toList();
        var visitedLabels = processDesignRuntimeService.buildTraceNodeLabels(definition, visitedIds);
        var currentLabels = processDesignRuntimeService.buildTraceNodeLabels(definition, currentIds);
        leave.setVisitedNodeLabels(visitedLabels.stream().map(item -> String.valueOf(item.get("summary"))).filter(text -> text != null && !text.isBlank()).toList());
        leave.setCurrentNodeLabels(currentLabels.stream().map(item -> String.valueOf(item.get("summary"))).filter(text -> text != null && !text.isBlank()).toList());
        leave.setVisitedFormLabels(visitedLabels.stream().map(item -> String.valueOf(item.get("formLabel"))).filter(text -> text != null && !text.isBlank()).distinct().toList());
        leave.setVisitedPageLabels(visitedLabels.stream().map(item -> String.valueOf(item.get("pageLabel"))).filter(text -> text != null && !text.isBlank()).distinct().toList());
        return leave;
    }
}
