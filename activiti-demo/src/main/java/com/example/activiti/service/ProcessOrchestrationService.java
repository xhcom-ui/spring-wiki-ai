package com.example.activiti.service;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ProcessOrchestrationService {

    @Autowired
    private ProcessEngine processEngine;

    // Orchestrate multiple processes in sequence
    public Map<String, String> orchestrateProcesses(Map<String, Object> orchestrationRequest) {
        Map<String, String> results = new HashMap<>();
        RuntimeService runtimeService = processEngine.getRuntimeService();

        try {
            // Start first process
            String firstProcessKey = (String) orchestrationRequest.get("firstProcessKey");
            Map<String, Object> firstProcessVariables = (Map<String, Object>) orchestrationRequest.get("firstProcessVariables");
            if (firstProcessKey != null) {
                ProcessInstance firstProcessInstance = runtimeService.startProcessInstanceByKey(firstProcessKey, firstProcessVariables);
                results.put("firstProcessInstanceId", firstProcessInstance.getId());
                log.info("Started first process: {} with instance ID: {}", firstProcessKey, firstProcessInstance.getId());
            }

            // Start second process (dependent on first)
            String secondProcessKey = (String) orchestrationRequest.get("secondProcessKey");
            Map<String, Object> secondProcessVariables = (Map<String, Object>) orchestrationRequest.get("secondProcessVariables");
            if (secondProcessKey != null) {
                ProcessInstance secondProcessInstance = runtimeService.startProcessInstanceByKey(secondProcessKey, secondProcessVariables);
                results.put("secondProcessInstanceId", secondProcessInstance.getId());
                log.info("Started second process: {} with instance ID: {}", secondProcessKey, secondProcessInstance.getId());
            }

            // Start third process (dependent on second)
            String thirdProcessKey = (String) orchestrationRequest.get("thirdProcessKey");
            Map<String, Object> thirdProcessVariables = (Map<String, Object>) orchestrationRequest.get("thirdProcessVariables");
            if (thirdProcessKey != null) {
                ProcessInstance thirdProcessInstance = runtimeService.startProcessInstanceByKey(thirdProcessKey, thirdProcessVariables);
                results.put("thirdProcessInstanceId", thirdProcessInstance.getId());
                log.info("Started third process: {} with instance ID: {}", thirdProcessKey, thirdProcessInstance.getId());
            }

            results.put("status", "SUCCESS");
        } catch (Exception e) {
            log.error("Error orchestrating processes: {}", e.getMessage());
            results.put("status", "ERROR");
            results.put("error", e.getMessage());
        }

        return results;
    }

    // Start a process with callback to another process
    public String startProcessWithCallback(String processKey, Map<String, Object> variables, String callbackProcessKey) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        try {
            // Add callback information to variables
            if (callbackProcessKey != null) {
                variables.put("callbackProcessKey", callbackProcessKey);
            }

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processKey, variables);
            log.info("Started process: {} with instance ID: {}", processKey, processInstance.getId());
            return processInstance.getId();
        } catch (Exception e) {
            log.error("Error starting process with callback: {}", e.getMessage());
            throw new RuntimeException("Failed to start process with callback", e);
        }
    }

    // Signal a process to continue
    public void signalProcess(String processInstanceId, String signalName, Map<String, Object> variables) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        try {
            runtimeService.signalEventReceived(signalName, processInstanceId, variables);
            log.info("Signaled process: {} with signal: {}", processInstanceId, signalName);
        } catch (Exception e) {
            log.error("Error signaling process: {}", e.getMessage());
            throw new RuntimeException("Failed to signal process", e);
        }
    }
}
