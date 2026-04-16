package com.example.flowlong.service;

import com.aizuda.flowlong.engine.core.ProcessService;
import com.aizuda.flowlong.engine.core.TaskService;
import com.aizuda.flowlong.engine.core.instance.InstanceService;
import com.aizuda.flowlong.engine.core.instance.model.ProcessInstance;
import com.aizuda.flowlong.engine.core.instance.param.StartProcessParams;
import com.aizuda.flowlong.engine.core.task.model.Task; 
import com.aizuda.flowlong.engine.core.task.param.CompleteTaskParams;
import com.example.flowlong.entity.Leave;
import com.example.flowlong.repository.LeaveRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class FlowLongService {

    @Autowired
    private ProcessService processService;

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private LeaveRepository leaveRepository;

    // 部署流程
    public void deployProcess(String processName, String modelContent) {
        processService.deploy(processName, modelContent);
        log.info("流程部署成功，流程名称: {}", processName);
    }

    // 启动请假流程
    public Leave startLeaveProcess(Leave leave) {
        StartProcessParams params = new StartProcessParams();
        params.setProcessKey("leave-process");
        params.setBusinessKey("LEAVE-" + System.currentTimeMillis());
        params.setVariable("applicant", leave.getApplicant());
        params.setVariable("deptManager", leave.getDeptManager());
        params.setVariable("generalManager", leave.getGeneralManager());
        params.setVariable("days", leave.getDays());
        params.setVariable("reason", leave.getReason());

        ProcessInstance instance = instanceService.start(params);
        leave.setProcessInstanceId(instance.getId());
        leave.setBusinessKey(params.getBusinessKey());
        leave.setStatus("SUBMITTED");
        leave.setCreatedAt(LocalDateTime.now());
        leave.setUpdatedAt(LocalDateTime.now());
        return leaveRepository.save(leave);
    }

    // 查询用户的待办任务
    public List<Task> getTasksByAssignee(String assignee) {
        return taskService.findTodoTasks(assignee);
    }

    // 完成任务
    public void completeTask(String taskId, Map<String, Object> variables, String processInstanceId, String status) {
        CompleteTaskParams params = new CompleteTaskParams();
        params.setTaskId(taskId);
        params.setVariables(variables);
        taskService.complete(params);
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
}
