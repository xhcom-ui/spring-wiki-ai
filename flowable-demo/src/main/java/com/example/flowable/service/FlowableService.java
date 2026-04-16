package com.example.flowable.service;

import com.example.flowable.entity.Leave;
import com.example.flowable.repository.LeaveRepository;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
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
        String businessKey = "LEAVE-" + System.currentTimeMillis();
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("leave-process", businessKey, Map.of(
                "applicant", leave.getApplicant(),
                "deptManager", leave.getDeptManager(),
                "generalManager", leave.getGeneralManager(),
                "days", leave.getDays(),
                "reason", leave.getReason()
        ));
        leave.setProcessInstanceId(instance.getId());
        leave.setBusinessKey(businessKey);
        leave.setStatus("SUBMITTED");
        leave.setCreatedAt(LocalDateTime.now());
        leave.setUpdatedAt(LocalDateTime.now());
        return leaveRepository.save(leave);
    }

    // 查询用户的待办任务
    public List<Task> getTasksByAssignee(String assignee) {
        return taskService.createTaskQuery()
                .taskAssignee(assignee)
                .list();
    }

    // 完成任务
    public void completeTask(String taskId, Map<String, Object> variables, String processInstanceId, String status) {
        taskService.complete(taskId, variables);
        log.info("任务完成，任务ID: {}", taskId);

        // 更新请假状态
        if (processInstanceId != null && status != null) {
            updateLeaveStatus(processInstanceId, status);
        }
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
        return leaveRepository.findAll();
    }

    // 根据ID获取请假申请
    public Leave getLeaveById(Long id) {
        return leaveRepository.findById(id).orElse(null);
    }

    // 获取流程实例
    public ProcessInstance getProcessInstance(String processInstanceId) {
        return runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
    }

    // 部署流程从文件
    public void deployProcessFromFile(String processName, String resourceName) {
        Deployment deployment = repositoryService.createDeployment()
                .name(processName)
                .addClasspathResource("processes/" + resourceName)
                .deploy();
        log.info("流程部署成功，部署ID: {}, 流程名称: {}", deployment.getId(), processName);
    }
}
