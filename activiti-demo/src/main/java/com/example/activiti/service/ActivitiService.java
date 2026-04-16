package com.example.activiti.service;

import com.example.activiti.entity.Leave;
import com.example.activiti.repository.LeaveRepository;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ActivitiService {

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private LeaveRepository leaveRepository;

    // 部署流程
    public void deployProcess() {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("processes/leave.bpmn")
                .name("请假流程")
                .deploy();
        log.info("流程部署成功，部署ID: {}", deployment.getId());
    }

    // 启动请假流程
    public Leave startLeaveProcess(Leave leave) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicant", leave.getApplicant());
        variables.put("deptManager", leave.getDeptManager());
        variables.put("generalManager", leave.getGeneralManager());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("leaveProcess", variables);
        leave.setProcessInstanceId(processInstance.getId());
        leave.setStatus("SUBMITTED");
        leave.setCreatedAt(LocalDateTime.now());
        leave.setUpdatedAt(LocalDateTime.now());
        return leaveRepository.save(leave);
    }

    // 查询用户的待办任务
    public List<Task> getTasksByAssignee(String assignee) {
        TaskService taskService = processEngine.getTaskService();
        return taskService.createTaskQuery()
                .taskAssignee(assignee)
                .list();
    }

    // 完成任务
    public void completeTask(String taskId, Map<String, Object> variables) {
        TaskService taskService = processEngine.getTaskService();
        taskService.complete(taskId, variables);
        log.info("任务完成，任务ID: {}", taskId);
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

    // 查询流程实例状态
    public String getProcessInstanceStatus(String processInstanceId) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (processInstance != null) {
            return "RUNNING";
        } else {
            HistoryService historyService = processEngine.getHistoryService();
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (historicProcessInstance != null) {
                return "COMPLETED";
            } else {
                return "UNKNOWN";
            }
        }
    }

    // 查询所有请假申请
    public List<Leave> getAllLeaves() {
        return leaveRepository.findAll();
    }

    // 根据ID查询请假申请
    public Leave getLeaveById(Long id) {
        return leaveRepository.findById(id).orElse(null);
    }
}
