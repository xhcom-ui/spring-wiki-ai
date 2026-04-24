package com.example.activiti.service;

import com.example.activiti.controller.dto.TaskInboxItem;
import com.example.activiti.entity.Leave;
import com.example.activiti.entity.Role;
import com.example.activiti.entity.User;
import com.example.activiti.exception.BadRequestException;
import com.example.activiti.exception.ForbiddenException;
import com.example.activiti.exception.NotFoundException;
import com.example.activiti.exception.UnauthorizedException;
import com.example.activiti.repository.LeaveRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ActivitiService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    public void deployProcess() {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("processes/leave.bpmn")
                .name("请假流程")
                .deploy();
        log.info("流程部署成功，部署ID: {}", deployment.getId());
    }

    public Leave startLeaveProcess(Leave leave) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        String processKey = leave.getProcessKey() == null || leave.getProcessKey().isBlank()
                ? "leaveProcess"
                : leave.getProcessKey().trim();

        Map<String, Object> variables = new HashMap<>();
        variables.put("applicant", leave.getApplicant());
        variables.put("deptManager", leave.getDeptManager());
        variables.put("generalManager", leave.getGeneralManager());
        variables.put("days", leave.getDays());
        variables.put("reason", leave.getReason());
        variables.put("startDate", leave.getStartDate() == null ? null : leave.getStartDate().toString());
        variables.put("endDate", leave.getEndDate() == null ? null : leave.getEndDate().toString());
        variables.put("starter", leave.getApplicant());
        variables.put("leaveStatus", "SUBMITTED");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processKey, variables);
        leave.setProcessKey(processKey);
        leave.setProcessInstanceId(processInstance.getId());
        leave.setStatus("SUBMITTED");
        leave.setCreatedAt(LocalDateTime.now());
        leave.setUpdatedAt(LocalDateTime.now());
        return leaveRepository.save(leave);
    }

    public List<Task> getTasksByAssignee(String assignee) {
        TaskService taskService = processEngine.getTaskService();
        return taskService.createTaskQuery()
                .taskAssignee(assignee)
                .list();
    }

    public List<TaskInboxItem> getCurrentUserInbox() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getUsername() == null || currentUser.getUsername().isBlank()) {
            return List.of();
        }

        TaskService taskService = processEngine.getTaskService();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        RepositoryService repositoryService = processEngine.getRepositoryService();

        LinkedHashMap<String, Task> taskMap = new LinkedHashMap<>();
        String username = currentUser.getUsername();

        taskService.createTaskQuery()
                .taskAssignee(username)
                .orderByTaskCreateTime()
                .desc()
                .list()
                .forEach(task -> taskMap.put(task.getId(), task));

        taskService.createTaskQuery()
                .taskCandidateUser(username)
                .orderByTaskCreateTime()
                .desc()
                .list()
                .forEach(task -> taskMap.putIfAbsent(task.getId(), task));

        List<Role> roles = currentUser.getRoles() == null ? List.of() : currentUser.getRoles();
        for (Role role : roles) {
            if (role == null || role.getCode() == null || role.getCode().isBlank()) {
                continue;
            }
            taskService.createTaskQuery()
                    .taskCandidateGroup(role.getCode())
                    .orderByTaskCreateTime()
                    .desc()
                    .list()
                    .forEach(task -> taskMap.putIfAbsent(task.getId(), task));
        }

        return taskMap.values().stream()
                .map(task -> buildInboxItem(task, currentUser, runtimeService, repositoryService, taskService))
                .toList();
    }

    public TaskInboxItem getCurrentUserTask(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new BadRequestException("任务ID不能为空");
        }
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getUsername() == null || currentUser.getUsername().isBlank()) {
            throw new UnauthorizedException("当前用户未登录");
        }

        TaskService taskService = processEngine.getTaskService();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new NotFoundException("任务不存在或已处理");
        }
        if (!canAccessTask(task, currentUser, taskService)) {
            throw new ForbiddenException("当前用户无权访问该任务");
        }
        return buildInboxItem(task, currentUser, runtimeService, repositoryService, taskService);
    }

    public void completeTask(String taskId, Map<String, Object> variables) {
        TaskService taskService = processEngine.getTaskService();
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getUsername() == null || currentUser.getUsername().isBlank()) {
            throw new UnauthorizedException("当前用户未登录");
        }

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new NotFoundException("任务不存在或已处理");
        }

        if (task.getAssignee() != null && !task.getAssignee().equals(currentUser.getUsername())) {
            throw new ForbiddenException("当前用户无权处理该任务");
        }

        if (task.getAssignee() == null) {
            assertCandidatePermission(taskId, currentUser);
            taskService.claim(taskId, currentUser.getUsername());
        }

        taskService.complete(taskId, variables == null ? Map.of() : variables);
        log.info("任务完成，任务ID: {}", taskId);
    }

    public void claimTask(String taskId) {
        TaskService taskService = processEngine.getTaskService();
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getUsername() == null || currentUser.getUsername().isBlank()) {
            throw new UnauthorizedException("当前用户未登录");
        }

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new NotFoundException("任务不存在或已处理");
        }
        if (task.getAssignee() != null) {
            if (currentUser.getUsername().equals(task.getAssignee())) {
                return;
            }
            throw new ForbiddenException("任务已被其他处理人签收");
        }

        assertCandidatePermission(taskId, currentUser);
        taskService.claim(taskId, currentUser.getUsername());
        log.info("任务签收成功，任务ID: {}", taskId);
    }

    public void updateLeaveStatus(String processInstanceId, String status) {
        Leave leave = leaveRepository.findByProcessInstanceId(processInstanceId);
        if (leave != null) {
            leave.setStatus(status);
            leave.setUpdatedAt(LocalDateTime.now());
            leaveRepository.save(leave);
        }
    }

    public String getProcessInstanceStatus(String processInstanceId) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (processInstance != null) {
            return "RUNNING";
        }
        HistoryService historyService = processEngine.getHistoryService();
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (historicProcessInstance != null) {
            return "COMPLETED";
        }
        return "UNKNOWN";
    }

    public List<Leave> getAllLeaves() {
        return leaveRepository.findAll();
    }

    public List<Leave> getLeavesByApplicant(String applicant) {
        if (applicant == null || applicant.isBlank()) {
            return List.of();
        }
        return leaveRepository.findByApplicantOrderByCreatedAtDesc(applicant);
    }

    public Leave getLeaveById(Long id) {
        return leaveRepository.findById(id).orElse(null);
    }

    private void assertCandidatePermission(String taskId, User currentUser) {
        TaskService taskService = processEngine.getTaskService();
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        Set<String> roleCodes = getRoleCodes(currentUser);
        boolean matched = identityLinks.stream()
                .filter(item -> "candidate".equalsIgnoreCase(item.getType()))
                .anyMatch(item -> currentUser.getUsername().equals(item.getUserId()) || roleCodes.contains(item.getGroupId()));
        if (!matched) {
            throw new ForbiddenException("当前用户不在该任务候选范围内");
        }
    }

    private boolean canAccessTask(Task task, User currentUser, TaskService taskService) {
        if (task.getAssignee() != null) {
            return currentUser.getUsername().equals(task.getAssignee());
        }
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
        Set<String> roleCodes = getRoleCodes(currentUser);
        return identityLinks.stream()
                .filter(item -> "candidate".equalsIgnoreCase(item.getType()))
                .anyMatch(item -> currentUser.getUsername().equals(item.getUserId()) || roleCodes.contains(item.getGroupId()));
    }

    private Set<String> getRoleCodes(User currentUser) {
        return currentUser.getRoles() == null
                ? Set.of()
                : currentUser.getRoles().stream()
                .map(Role::getCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private TaskInboxItem buildInboxItem(
            Task task,
            User currentUser,
            RuntimeService runtimeService,
            RepositoryService repositoryService,
            TaskService taskService
    ) {
        TaskInboxItem item = new TaskInboxItem();
        item.setId(task.getId());
        item.setName(task.getName());
        item.setProcessInstanceId(task.getProcessInstanceId());
        item.setProcessDefinitionId(task.getProcessDefinitionId());
        item.setTaskDefinitionKey(task.getTaskDefinitionKey());
        item.setAssignee(task.getAssignee());
        item.setOwner(task.getOwner());
        item.setClaimed(task.getAssignee() != null);
        item.setCreateTime(formatDate(task.getCreateTime()));

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(task.getProcessDefinitionId())
                .singleResult();
        if (processDefinition != null) {
            item.setProcessDefinitionKey(processDefinition.getKey());
            item.setProcessDefinitionName(processDefinition.getName());
        }

        item.setFormKey(task.getFormKey());

        Leave leave = leaveRepository.findByProcessInstanceId(task.getProcessInstanceId());
        if (leave != null) {
            item.setApplicant(leave.getApplicant());
            item.setLeaveStatus(leave.getStatus());
            item.setBusinessKey(firstNonBlank(leave.getProcessKey(), task.getProcessInstanceId()));
        }

        try {
            Map<String, Object> variables = runtimeService.getVariables(task.getExecutionId());
            LinkedHashMap<String, Object> runtimeVariables = variables == null ? new LinkedHashMap<>() : new LinkedHashMap<>(variables);
            item.setVariables(runtimeVariables);
            item.setApplicant(firstNonBlank(item.getApplicant(), stringify(runtimeVariables.get("applicant"))));
            item.setLeaveStatus(firstNonBlank(item.getLeaveStatus(), stringify(runtimeVariables.get("leaveStatus"))));
        } catch (Exception error) {
            item.setVariables(Map.of());
        }

        NodeRuntimeMeta nodeMeta = resolveNodeRuntimeMeta(repositoryService, task.getProcessDefinitionId(), task.getTaskDefinitionKey());
        item.setTodoPage(nodeMeta.todoPage);
        item.setDonePage(nodeMeta.donePage);
        item.setFormKey(firstNonBlank(item.getFormKey(), nodeMeta.formKey));
        item.setBusinessKey(firstNonBlank(item.getBusinessKey(), task.getProcessInstanceId()));

        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
        List<String> candidateUsers = identityLinks.stream()
                .filter(link -> "candidate".equalsIgnoreCase(link.getType()) && link.getUserId() != null)
                .map(IdentityLink::getUserId)
                .distinct()
                .toList();
        List<String> candidateGroups = identityLinks.stream()
                .filter(link -> "candidate".equalsIgnoreCase(link.getType()) && link.getGroupId() != null)
                .map(IdentityLink::getGroupId)
                .distinct()
                .toList();
        item.setCandidateUsers(candidateUsers);
        item.setCandidateGroups(candidateGroups);

        Set<String> roleCodes = getRoleCodes(currentUser);
        boolean canClaim = task.getAssignee() == null
                && (candidateUsers.contains(currentUser.getUsername()) || candidateGroups.stream().anyMatch(roleCodes::contains));
        item.setCanClaim(canClaim);
        item.setAssignmentMode(resolveAssignmentMode(task, currentUser, candidateUsers, candidateGroups, roleCodes));
        return item;
    }

    private String resolveAssignmentMode(Task task, User currentUser, List<String> candidateUsers, List<String> candidateGroups, Set<String> roleCodes) {
        if (task.getAssignee() != null) {
            return currentUser.getUsername().equals(task.getAssignee()) ? "ASSIGNED_TO_ME" : "ASSIGNED";
        }
        if (candidateUsers.contains(currentUser.getUsername())) {
            return "CANDIDATE_USER";
        }
        if (candidateGroups.stream().anyMatch(roleCodes::contains)) {
            return "CANDIDATE_GROUP";
        }
        return "UNASSIGNED";
    }

    private NodeRuntimeMeta resolveNodeRuntimeMeta(RepositoryService repositoryService, String processDefinitionId, String taskDefinitionKey) {
        NodeRuntimeMeta meta = new NodeRuntimeMeta();
        if (processDefinitionId == null || taskDefinitionKey == null) {
            return meta;
        }
        try {
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            if (bpmnModel == null || bpmnModel.getMainProcess() == null) {
                return meta;
            }
            FlowElement flowElement = bpmnModel.getMainProcess().getFlowElement(taskDefinitionKey, true);
            if (flowElement instanceof UserTask userTask) {
                meta.formKey = userTask.getFormKey();
                mergeDocumentationMeta(meta, userTask.getDocumentation());
            }
        } catch (Exception error) {
            log.debug("resolveNodeRuntimeMeta skipped: {}", error.getMessage());
        }
        return meta;
    }

    private void mergeDocumentationMeta(NodeRuntimeMeta meta, String documentation) {
        if (documentation == null || documentation.isBlank()) {
            return;
        }
        Arrays.stream(documentation.split("\\R\\R"))
                .map(String::trim)
                .filter(item -> item.startsWith("{") && item.endsWith("}"))
                .findFirst()
                .ifPresent(json -> {
                    try {
                        Map<String, Object> values = objectMapper.readValue(json, MAP_TYPE);
                        meta.todoPage = stringify(values.get("todoPage"));
                        meta.donePage = stringify(values.get("donePage"));
                        meta.formKey = firstNonBlank(meta.formKey, stringify(values.get("formKey")));
                    } catch (IOException error) {
                        log.debug("ignore malformed documentation meta");
                    }
                });
    }

    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    private String formatDate(Date value) {
        return value == null ? null : value.toInstant().toString();
    }

    private static class NodeRuntimeMeta {
        private String formKey;
        private String todoPage;
        private String donePage;
    }
}
