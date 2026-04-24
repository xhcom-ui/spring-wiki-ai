package com.example.activiti.service;

import com.example.activiti.entity.ProcessDefinitionEntity;
import com.example.activiti.repository.ProcessDefinitionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CopySendService implements JavaDelegate {

    @Autowired
    private ProcessDefinitionRepository processDefinitionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void execute(DelegateExecution execution) {
        String processDefinitionId = execution.getProcessDefinitionId();
        String currentActivityId = execution.getCurrentActivityId();
        String processKey = resolveProcessKey(processDefinitionId);
        List<String> recipients = resolveRecipients(processKey, currentActivityId);
        execution.setVariable("lastCopySendNode", currentActivityId);
        execution.setVariable("lastCopySendRecipients", recipients);
        log.info("Copy send node executed. processKey={}, nodeKey={}, recipients={}", processKey, currentActivityId, recipients);
    }

    private String resolveProcessKey(String processDefinitionId) {
        if (processDefinitionId == null || processDefinitionId.isBlank()) {
            return null;
        }
        String[] parts = processDefinitionId.split(":");
        return parts.length > 0 ? parts[0] : processDefinitionId;
    }

    private List<String> resolveRecipients(String processKey, String nodeKey) {
        if (processKey == null || nodeKey == null) {
            return List.of();
        }
        ProcessDefinitionEntity definition = processDefinitionRepository.findFirstByProcessKeyOrderByVersionDesc(processKey);
        if (definition == null || definition.getDesignerJson() == null || definition.getDesignerJson().isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(definition.getDesignerJson());
            List<String> recipients = new ArrayList<>();
            collectRecipients(root.path("nodeConfig"), nodeKey, recipients);
            return recipients;
        } catch (Exception error) {
            log.warn("Failed to parse designer json for copy send node. processKey={}, nodeKey={}", processKey, nodeKey, error);
            return List.of();
        }
    }

    private boolean collectRecipients(JsonNode node, String nodeKey, List<String> recipients) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return false;
        }
        if (nodeKey.equals(node.path("nodeKey").asText()) && node.path("type").asInt(-1) == 2) {
            JsonNode assignees = node.path("nodeAssigneeList");
            if (assignees.isArray()) {
                assignees.forEach((item) -> {
                    String name = item.path("name").asText();
                    String id = item.path("id").asText();
                    if (!name.isBlank()) {
                        recipients.add(name);
                    } else if (!id.isBlank()) {
                        recipients.add(id);
                    }
                });
            }
            return true;
        }
        if (collectRecipients(node.path("childNode"), nodeKey, recipients)) {
            return true;
        }
        JsonNode conditionNodes = node.path("conditionNodes");
        if (conditionNodes.isArray()) {
            for (JsonNode conditionNode : conditionNodes) {
                if (collectRecipients(conditionNode, nodeKey, recipients)) {
                    return true;
                }
            }
        }
        return false;
    }
}
