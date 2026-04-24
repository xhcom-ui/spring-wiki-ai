package com.example.flowable.service;

import com.example.flowable.entity.Leave;
import com.example.flowable.entity.ProcessDefinitionEntity;
import com.example.flowable.entity.Role;
import com.example.flowable.entity.User;
import com.example.flowable.repository.LeaveRepository;
import com.example.flowable.repository.ProcessDefinitionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProcessDesignRuntimeService {

    private static final String DESIGNER_TYPE_CUSTOM = "CUSTOM";
    private static final Pattern SIMPLE_EXPRESSION = Pattern.compile("^\\$\\{\\s*([a-zA-Z0-9_.$-]+)\\s*}$");

    private final ProcessDefinitionRepository processDefinitionRepository;
    private final LeaveRepository leaveRepository;
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final FormCatalogService formCatalogService;
    private final ObjectMapper objectMapper;

    public ProcessDesignRuntimeService(
            ProcessDefinitionRepository processDefinitionRepository,
            LeaveRepository leaveRepository,
            RepositoryService repositoryService,
            RuntimeService runtimeService,
            TaskService taskService,
            HistoryService historyService,
            FormCatalogService formCatalogService,
            ObjectMapper objectMapper
    ) {
        this.processDefinitionRepository = processDefinitionRepository;
        this.leaveRepository = leaveRepository;
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
        this.formCatalogService = formCatalogService;
        this.objectMapper = objectMapper;
    }

    public ProcessDefinitionEntity resolveStartDefinition(String processKey) {
        List<ProcessDefinitionEntity> versions = processDefinitionRepository.findByProcessKeyOrderByVersionDesc(processKey);
        if (versions.isEmpty()) {
            return null;
        }
        return versions.stream()
                .filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus()))
                .findFirst()
                .orElse(versions.get(0));
    }

    public ProcessDefinitionEntity resolveDefinitionByProcessDefinitionId(String processDefinitionId) {
        if (processDefinitionId == null || processDefinitionId.isBlank()) {
            return null;
        }
        ProcessDefinition engineDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        if (engineDefinition == null) {
            return null;
        }
        ProcessDefinitionEntity definition = processDefinitionRepository.findByDeploymentId(engineDefinition.getDeploymentId());
        if (definition != null) {
            return definition;
        }
        return resolveStartDefinition(engineDefinition.getKey());
    }

    public ProcessDefinitionEntity resolveDefinitionByProcessInstanceId(String processInstanceId) {
        if (processInstanceId == null || processInstanceId.isBlank()) {
            return null;
        }
        ProcessInstance runtimeInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (runtimeInstance != null) {
            return resolveDefinitionByProcessDefinitionId(runtimeInstance.getProcessDefinitionId());
        }
        HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (historicInstance != null) {
            return resolveDefinitionByProcessDefinitionId(historicInstance.getProcessDefinitionId());
        }
        return null;
    }

    public Map<String, Object> buildLaunchConfig(String processKey) {
        ProcessDefinitionEntity definition = resolveStartDefinition(processKey);
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("processKey", processKey);
        config.put("designerType", definition == null ? "BPMN" : defaultString(definition.getDesignerType(), "BPMN"));
        config.put("processName", definition == null ? processKey : definition.getProcessName());
        config.put("version", definition == null ? null : definition.getVersion());
        config.put("launchForm", mergeFormCatalogMeta(new LinkedHashMap<>(Map.of(
                "pageLabel", "",
                "formLabel", "",
                "formKey", ""
        ))));

        List<Map<String, Object>> approvalNodes = new ArrayList<>();
        if (definition != null && DESIGNER_TYPE_CUSTOM.equalsIgnoreCase(definition.getDesignerType()) && notBlank(definition.getDesignSchemaJson())) {
            JsonNode root = readSchema(definition.getDesignSchemaJson());
            if (root != null) {
                JsonNode properties = root.path("properties");
                config.put("launchForm", mergeFormCatalogMeta(new LinkedHashMap<>(Map.of(
                        "pageLabel", text(properties, "pageLabel"),
                        "formLabel", text(properties, "formLabel"),
                        "formKey", text(properties, "formKey")
                ))));
                collectApprovalNodes(root, approvalNodes);
            }
        } else if (definition != null && notBlank(definition.getBpmnXml())) {
            Map<String, Object> bpmnLaunchForm = findFirstStartEventConfig(definition.getBpmnXml());
            if (!bpmnLaunchForm.isEmpty()) {
                config.put("launchForm", mergeFormCatalogMeta(bpmnLaunchForm));
            }
            approvalNodes.addAll(findBpmnApprovalNodes(definition.getBpmnXml()));
        }
        if (approvalNodes.isEmpty() && "leave-process".equals(processKey)) {
            approvalNodes.add(defaultApproverSlot("deptManager", "部门经理审批", 1));
            approvalNodes.add(defaultApproverSlot("generalManager", "总经理审批", 2));
        }
        config.put("approvalNodes", approvalNodes.stream().map(this::mergeFormCatalogMeta).toList());
        config.put("formCatalogs", formCatalogService.getActiveCatalogs());
        return config;
    }

    public Map<String, Object> buildStartVariables(Leave leave, ProcessDefinitionEntity definition) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("applicant", leave.getApplicant());
        variables.put("days", leave.getDays());
        variables.put("reason", leave.getReason());
        variables.put("approved", false);
        if (leave.getStartDate() != null) {
            variables.put("startDate", leave.getStartDate().toString());
        }
        if (leave.getEndDate() != null) {
            variables.put("endDate", leave.getEndDate().toString());
        }
        if (leave.getDeptManager() != null && !leave.getDeptManager().isBlank()) {
            variables.put("deptManager", leave.getDeptManager());
        }
        if (leave.getGeneralManager() != null && !leave.getGeneralManager().isBlank()) {
            variables.put("generalManager", leave.getGeneralManager());
        }
        if (leave.getProcessVariables() != null) {
            leave.getProcessVariables().forEach((key, value) -> {
                if (key != null && !key.isBlank() && value != null) {
                    variables.put(key, value);
                }
            });
        }
        if (definition != null && DESIGNER_TYPE_CUSTOM.equalsIgnoreCase(definition.getDesignerType()) && notBlank(definition.getDesignSchemaJson())) {
            for (Map<String, Object> node : flattenNodeSummaries(definition.getDesignSchemaJson())) {
                if (!Boolean.TRUE.equals(node.get("requiresInput"))) {
                    continue;
                }
                String variableKey = Objects.toString(node.get("variableKey"), "");
                if (variableKey.isBlank()) {
                    continue;
                }
                variables.putIfAbsent(variableKey, "");
            }
        }
        return variables;
    }

    public List<Map<String, Object>> getTasksForUser(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            return List.of();
        }
        Set<String> roleCodes = new LinkedHashSet<>();
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                if (role != null && role.getCode() != null && !role.getCode().isBlank()) {
                    roleCodes.add(role.getCode());
                }
            }
        }

        Map<String, Task> taskMap = new LinkedHashMap<>();
        mergeTasks(taskMap, taskService.createTaskQuery().taskAssignee(user.getUsername()).active().list());
        mergeTasks(taskMap, taskService.createTaskQuery().taskCandidateUser(user.getUsername()).active().list());
        for (String roleCode : roleCodes) {
            mergeTasks(taskMap, taskService.createTaskQuery().taskCandidateGroup(roleCode).active().list());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Task task : taskMap.values()) {
            result.add(enrichTask(task));
        }
        return result;
    }

    public Map<String, Object> buildTaskCompletionVariables(Task task, String outcome, String comment, Map<String, Object> extraVariables) {
        Map<String, Object> variables = new LinkedHashMap<>();
        if (extraVariables != null) {
            variables.putAll(extraVariables);
        }
        boolean approved = !"rejected".equalsIgnoreCase(outcome);
        variables.put("approved", approved);
        variables.put("taskOutcome", approved ? "approved" : "rejected");
        if (task != null && task.getTaskDefinitionKey() != null) {
            variables.put("taskDefinitionKey", task.getTaskDefinitionKey());
            variables.put(task.getTaskDefinitionKey() + "_approved", approved);
        }
        if (comment != null && !comment.isBlank()) {
            variables.put("comment", comment.trim());
        }
        return variables;
    }

    public String resolveLeaveStatusAfterTaskComplete(String processInstanceId, String outcome) {
        if ("rejected".equalsIgnoreCase(outcome)) {
            return "REJECTED";
        }
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        return instance == null ? "APPROVED" : "RUNNING";
    }

    public Map<String, Object> compareDesignSchemas(String leftSchemaJson, String rightSchemaJson) {
        List<Map<String, Object>> leftNodes = flattenNodeSummaries(leftSchemaJson);
        List<Map<String, Object>> rightNodes = flattenNodeSummaries(rightSchemaJson);
        int max = Math.max(leftNodes.size(), rightNodes.size());
        int changed = 0;
        List<Map<String, Object>> preview = new ArrayList<>();
        for (int index = 0; index < max; index++) {
            Map<String, Object> leftNode = index < leftNodes.size() ? leftNodes.get(index) : Map.of();
            Map<String, Object> rightNode = index < rightNodes.size() ? rightNodes.get(index) : Map.of();
            String leftSummary = Objects.toString(leftNode.get("summary"), "");
            String rightSummary = Objects.toString(rightNode.get("summary"), "");
            if (!Objects.equals(leftSummary, rightSummary)) {
                changed++;
                if (preview.size() < 12) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("index", index + 1);
                    row.put("left", leftSummary);
                    row.put("right", rightSummary);
                    preview.add(row);
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("leftNodeCount", leftNodes.size());
        result.put("rightNodeCount", rightNodes.size());
        result.put("changedNodeCount", changed);
        result.put("diffPreview", preview);
        result.put("leftNodes", leftNodes);
        result.put("rightNodes", rightNodes);
        result.put("leftSchemaJson", prettySchema(leftSchemaJson));
        result.put("rightSchemaJson", prettySchema(rightSchemaJson));
        return result;
    }

    public List<Map<String, Object>> flattenNodeSummaries(String schemaJson) {
        JsonNode root = readSchema(schemaJson);
        if (root == null) {
            return List.of();
        }
        List<Map<String, Object>> nodes = new ArrayList<>();
        collectNodeSummaries(root, nodes);
        return nodes;
    }

    public Map<String, Object> getNodeConfigByDefinition(ProcessDefinitionEntity definition, String nodeId) {
        return definition == null ? Map.of() : findNodeConfig(definition, nodeId);
    }

    public List<Map<String, Object>> buildTraceNodeLabels(ProcessDefinitionEntity definition, Collection<String> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> items = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String nodeId : nodeIds) {
            if (nodeId == null || nodeId.isBlank() || !seen.add(nodeId)) {
                continue;
            }
            Map<String, Object> config = findNodeConfig(definition, nodeId);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("nodeId", nodeId);
            item.put("nodeName", defaultString(Objects.toString(config.get("nodeName"), ""), nodeId));
            item.put("formLabel", Objects.toString(config.get("formLabel"), ""));
            item.put("pageLabel", Objects.toString(config.get("pageLabel"), ""));
            item.put("formKey", Objects.toString(config.get("formKey"), ""));
            item.put("componentKey", Objects.toString(config.get("componentKey"), ""));
            item.put("summary", buildTraceLabelSummary(item));
            items.add(item);
        }
        return items;
    }

    private void mergeTasks(Map<String, Task> target, Collection<Task> tasks) {
        if (tasks == null) {
            return;
        }
        for (Task task : tasks) {
            if (task != null && task.getId() != null) {
                target.putIfAbsent(task.getId(), task);
            }
        }
    }

    private Map<String, Object> enrichTask(Task task) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", task.getId());
        item.put("name", task.getName());
        item.put("assignee", task.getAssignee());
        item.put("taskDefinitionKey", task.getTaskDefinitionKey());
        item.put("processInstanceId", task.getProcessInstanceId());
        item.put("processDefinitionId", task.getProcessDefinitionId());
        item.put("createTime", task.getCreateTime());
        item.put("claimable", task.getAssignee() == null || task.getAssignee().isBlank());

        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();
        if (instance != null) {
            item.put("businessKey", instance.getBusinessKey());
            item.put("processDefinitionKey", instance.getProcessDefinitionKey());
        }

        Leave leave = leaveRepository.findByProcessInstanceId(task.getProcessInstanceId());
        if (leave != null) {
            item.put("applicant", leave.getApplicant());
            item.put("leaveStatus", leave.getStatus());
            item.put("reason", leave.getReason());
            item.put("days", leave.getDays());
            item.put("startDate", leave.getStartDate());
            item.put("endDate", leave.getEndDate());
        }

        ProcessDefinitionEntity definition = resolveDefinitionByProcessDefinitionId(task.getProcessDefinitionId());
        if (definition != null) {
            item.put("designerType", defaultString(definition.getDesignerType(), "BPMN"));
            item.put("processName", definition.getProcessName());
            Map<String, Object> nodeConfig = findNodeConfig(definition, task.getTaskDefinitionKey());
            if (!nodeConfig.isEmpty()) {
                item.putAll(nodeConfig);
            }
        }
        item.put("comments", taskService.getProcessInstanceComments(task.getProcessInstanceId()).stream()
                .map(comment -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", comment.getId());
                    row.put("taskId", comment.getTaskId());
                    row.put("userId", comment.getUserId());
                    row.put("time", comment.getTime());
                    row.put("message", comment.getFullMessage());
                    return row;
                })
                .toList());
        return item;
    }

    private void collectApprovalNodes(JsonNode node, List<Map<String, Object>> nodes) {
        if (node == null || node.isMissingNode()) {
            return;
        }
        String nodeType = text(node, "nodeType");
        if ("between".equals(nodeType)) {
            Map<String, Object> slot = buildApproverSlot(node, nodes.size() + 1);
            if (!slot.isEmpty()) {
                nodes.add(slot);
            }
        }
        collectApprovalNodes(node.path("childNode"), nodes);
        JsonNode conditionNodes = node.path("conditionNodes");
        if (conditionNodes.isArray()) {
            for (JsonNode child : conditionNodes) {
                collectApprovalNodes(child, nodes);
            }
        }
    }

    private Map<String, Object> buildApproverSlot(JsonNode node, int order) {
        JsonNode properties = node.path("properties");
        String assignmentMode = defaultString(text(properties, "assignmentMode"), "assignee");
        String assignmentExpression = switch (assignmentMode) {
            case "candidateUsers" -> text(properties, "candidateUsers");
            case "candidateGroups" -> text(properties, "candidateGroups");
            default -> text(properties, "assignee");
        };
        String variableKey = extractExpressionVariable(assignmentExpression);
        boolean requiresInput = variableKey != null && !"candidateGroups".equals(assignmentMode);
        Map<String, Object> slot = new LinkedHashMap<>();
        slot.put("order", order);
        slot.put("nodeId", text(node, "nodeId"));
        slot.put("nodeName", text(node, "nodeName"));
        slot.put("assignmentMode", assignmentMode);
        slot.put("assignmentExpression", assignmentExpression);
        slot.put("variableKey", defaultString(variableKey, ""));
        slot.put("requiresInput", requiresInput);
        slot.put("multiple", "candidateUsers".equals(assignmentMode));
        slot.put("pageLabel", text(properties, "pageLabel"));
        slot.put("formLabel", text(properties, "formLabel"));
        slot.put("formKey", text(properties, "formKey"));
        slot.put("summary", buildNodeSummary(node));
        return mergeFormCatalogMeta(slot);
    }

    private Map<String, Object> defaultApproverSlot(String variableKey, String nodeName, int order) {
        Map<String, Object> slot = new LinkedHashMap<>();
        slot.put("order", order);
        slot.put("nodeId", variableKey);
        slot.put("nodeName", nodeName);
        slot.put("assignmentMode", "assignee");
        slot.put("assignmentExpression", "${" + variableKey + "}");
        slot.put("variableKey", variableKey);
        slot.put("requiresInput", true);
        slot.put("multiple", false);
        slot.put("pageLabel", "");
        slot.put("formLabel", "");
        slot.put("formKey", "");
        slot.put("summary", nodeName + " | assignee=${" + variableKey + "}");
        return mergeFormCatalogMeta(slot);
    }

    private void collectNodeSummaries(JsonNode node, List<Map<String, Object>> items) {
        if (node == null || node.isMissingNode()) {
            return;
        }
        String nodeId = text(node, "nodeId");
        if (!nodeId.isBlank()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("nodeId", nodeId);
            item.put("nodeType", text(node, "nodeType"));
            item.put("nodeName", text(node, "nodeName"));
            item.put("summary", buildNodeSummary(node));
            item.put("requiresInput", buildApproverSlot(node, 0).getOrDefault("requiresInput", false));
            item.put("variableKey", buildApproverSlot(node, 0).getOrDefault("variableKey", ""));
            items.add(item);
        }
        collectNodeSummaries(node.path("childNode"), items);
        JsonNode conditionNodes = node.path("conditionNodes");
        if (conditionNodes.isArray()) {
            for (JsonNode child : conditionNodes) {
                collectNodeSummaries(child, items);
            }
        }
    }

    private Map<String, Object> findNodeConfig(ProcessDefinitionEntity definition, String nodeId) {
        if (definition == null || !notBlank(nodeId)) {
            return Map.of();
        }
        String schemaJson = definition.getDesignSchemaJson();
        JsonNode root = readSchema(schemaJson);
        if (root != null) {
            JsonNode node = findNodeById(root, nodeId);
            if (node != null) {
                JsonNode properties = node.path("properties");
                Map<String, Object> config = new LinkedHashMap<>();
                config.put("nodeId", nodeId);
                config.put("nodeType", text(node, "nodeType"));
                config.put("pageLabel", text(properties, "pageLabel"));
                config.put("formLabel", text(properties, "formLabel"));
                config.put("formKey", text(properties, "formKey"));
                config.put("nodeName", text(node, "nodeName"));
                config.put("assignmentMode", text(properties, "assignmentMode"));
                config.put("assignmentExpression", firstNonBlank(
                        text(properties, "assignee"),
                        text(properties, "candidateUsers"),
                        text(properties, "candidateGroups")
                ));
                config.put("variableKey", buildApproverSlot(node, 0).getOrDefault("variableKey", ""));
                config.put("requiresInput", buildApproverSlot(node, 0).getOrDefault("requiresInput", false));
                return mergeFormCatalogMeta(config);
            }
        }
        return findNodeConfigFromBpmnXml(definition.getBpmnXml(), nodeId);
    }

    private Map<String, Object> findNodeConfigFromBpmnXml(String bpmnXml, String nodeId) {
        if (!notBlank(bpmnXml) || !notBlank(nodeId)) {
            return Map.of();
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(bpmnXml)));
            NodeList nodes = document.getElementsByTagName("*");
            for (int index = 0; index < nodes.getLength(); index++) {
                if (!(nodes.item(index) instanceof Element element)) {
                    continue;
                }
                if (!nodeId.equals(element.getAttribute("id"))) {
                    continue;
                }
                Map<String, Object> config = new LinkedHashMap<>();
                config.put("nodeId", nodeId);
                config.put("nodeType", stripPrefix(element.getTagName()));
                config.put("nodeName", element.getAttribute("name"));
                config.put("formKey", firstNonBlank(
                        element.getAttribute("flowable:formKey"),
                        element.getAttribute("activiti:formKey")
                ));
                config.put("assignmentMode", resolveBpmnAssignmentMode(element));
                config.put("assignmentExpression", firstNonBlank(
                        element.getAttribute("flowable:assignee"),
                        element.getAttribute("flowable:candidateUsers"),
                        element.getAttribute("flowable:candidateGroups"),
                        element.getAttribute("activiti:assignee"),
                        element.getAttribute("activiti:candidateUsers"),
                        element.getAttribute("activiti:candidateGroups")
                ));
                Map<String, String> documentationMeta = parseDocumentationMeta(element);
                config.put("pageLabel", documentationMeta.getOrDefault("pageLabel", ""));
                config.put("formLabel", documentationMeta.getOrDefault("formLabel", ""));
                config.put("variableKey", "");
                config.put("requiresInput", false);
                return mergeFormCatalogMeta(config);
            }
        } catch (Exception ignored) {
            return Map.of();
        }
        return Map.of();
    }

    private Map<String, Object> findFirstStartEventConfig(String bpmnXml) {
        List<Map<String, Object>> nodes = findBpmnNodesByTag(bpmnXml, "startEvent");
        return nodes.isEmpty() ? Map.of() : nodes.get(0);
    }

    private List<Map<String, Object>> findBpmnApprovalNodes(String bpmnXml) {
        List<Map<String, Object>> nodes = findBpmnNodesByTag(bpmnXml, "userTask");
        List<Map<String, Object>> result = new ArrayList<>();
        int order = 1;
        for (Map<String, Object> item : nodes) {
            Map<String, Object> row = new LinkedHashMap<>(item);
            row.put("order", order++);
            row.put("multiple", "candidateUsers".equals(item.get("assignmentMode")));
            row.put("summary", buildTraceLabelSummary(item));
            result.add(mergeFormCatalogMeta(row));
        }
        return result;
    }

    private List<Map<String, Object>> findBpmnNodesByTag(String bpmnXml, String localName) {
        if (!notBlank(bpmnXml)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(bpmnXml)));
            NodeList nodes = document.getElementsByTagName("*");
            for (int index = 0; index < nodes.getLength(); index++) {
                if (!(nodes.item(index) instanceof Element element)) {
                    continue;
                }
                if (!localName.equals(stripPrefix(element.getTagName()))) {
                    continue;
                }
                Map<String, Object> config = new LinkedHashMap<>();
                config.put("nodeId", element.getAttribute("id"));
                config.put("nodeType", stripPrefix(element.getTagName()));
                config.put("nodeName", element.getAttribute("name"));
                config.put("formKey", firstNonBlank(
                        element.getAttribute("flowable:formKey"),
                        element.getAttribute("activiti:formKey")
                ));
                config.put("assignmentMode", resolveBpmnAssignmentMode(element));
                config.put("assignmentExpression", firstNonBlank(
                        element.getAttribute("flowable:assignee"),
                        element.getAttribute("flowable:candidateUsers"),
                        element.getAttribute("flowable:candidateGroups"),
                        element.getAttribute("activiti:assignee"),
                        element.getAttribute("activiti:candidateUsers"),
                        element.getAttribute("activiti:candidateGroups")
                ));
                Map<String, String> documentationMeta = parseDocumentationMeta(element);
                config.put("pageLabel", documentationMeta.getOrDefault("pageLabel", ""));
                config.put("formLabel", documentationMeta.getOrDefault("formLabel", ""));
                config.put("variableKey", "");
                config.put("requiresInput", false);
                result.add(mergeFormCatalogMeta(config));
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return result;
    }

    private Map<String, String> parseDocumentationMeta(Element element) {
        Map<String, String> result = new LinkedHashMap<>();
        NodeList docs = element.getElementsByTagName("*");
        for (int index = 0; index < docs.getLength(); index++) {
            if (!(docs.item(index) instanceof Element docElement)) {
                continue;
            }
            if (!"documentation".equals(stripPrefix(docElement.getTagName()))) {
                continue;
            }
            String content = docElement.getTextContent();
            if (content == null || content.isBlank()) {
                continue;
            }
            String[] parts = content.split("/");
            if (parts.length > 0) {
                result.put("pageLabel", parts[0].trim());
            }
            if (parts.length > 1) {
                result.put("formLabel", parts[1].trim());
            }
            return result;
        }
        return result;
    }

    private String resolveBpmnAssignmentMode(Element element) {
        if (notBlank(firstNonBlank(element.getAttribute("flowable:candidateGroups"), element.getAttribute("activiti:candidateGroups")))) {
            return "candidateGroups";
        }
        if (notBlank(firstNonBlank(element.getAttribute("flowable:candidateUsers"), element.getAttribute("activiti:candidateUsers")))) {
            return "candidateUsers";
        }
        if (notBlank(firstNonBlank(element.getAttribute("flowable:assignee"), element.getAttribute("activiti:assignee")))) {
            return "assignee";
        }
        return "";
    }

    private String stripPrefix(String tagName) {
        int index = tagName == null ? -1 : tagName.indexOf(':');
        return index >= 0 ? tagName.substring(index + 1) : tagName;
    }

    private Map<String, Object> mergeFormCatalogMeta(Map<String, Object> target) {
        Map<String, Object> merged = new LinkedHashMap<>(target);
        merged.putAll(formCatalogService.buildFormMeta(
                Objects.toString(target.get("formKey"), ""),
                Objects.toString(target.get("pageLabel"), ""),
                Objects.toString(target.get("formLabel"), "")
        ));
        return merged;
    }

    private JsonNode findNodeById(JsonNode node, String nodeId) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        if (nodeId.equals(text(node, "nodeId"))) {
            return node;
        }
        JsonNode childNode = findNodeById(node.path("childNode"), nodeId);
        if (childNode != null) {
            return childNode;
        }
        JsonNode conditionNodes = node.path("conditionNodes");
        if (conditionNodes.isArray()) {
            for (JsonNode child : conditionNodes) {
                JsonNode hit = findNodeById(child, nodeId);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }

    private JsonNode readSchema(String schemaJson) {
        if (!notBlank(schemaJson)) {
            return null;
        }
        try {
            return objectMapper.readTree(schemaJson);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String prettySchema(String schemaJson) {
        JsonNode node = readSchema(schemaJson);
        if (node == null) {
            return schemaJson == null ? "" : schemaJson;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception ignored) {
            return schemaJson;
        }
    }

    private String buildNodeSummary(JsonNode node) {
        String nodeType = text(node, "nodeType");
        String nodeName = text(node, "nodeName");
        JsonNode properties = node.path("properties");
        if ("between".equals(nodeType)) {
            return nodeName + " | " + defaultString(text(properties, "assignmentMode"), "assignee")
                    + "=" + firstNonBlank(text(properties, "assignee"), text(properties, "candidateUsers"), text(properties, "candidateGroups"))
                    + " | 表单=" + text(properties, "formLabel")
                    + " | 页面=" + text(properties, "pageLabel");
        }
        if ("serial-node".equals(nodeType)) {
            return nodeName + " | 条件=" + firstNonBlank(text(properties, "conditionSummary"), text(properties, "conditionExpression"), text(node, "value"));
        }
        if ("copy".equals(nodeType)) {
            return nodeName + " | 抄送=" + text(properties, "copyUsers")
                    + (Boolean.parseBoolean(text(properties, "allowInitiatorSelect")) ? " | 发起人可自选" : "");
        }
        if ("start".equals(nodeType)) {
            return nodeName + " | 发起页=" + text(properties, "pageLabel") + " | 表单=" + text(properties, "formLabel");
        }
        return nodeName + " | type=" + nodeType;
    }

    private String buildTraceLabelSummary(Map<String, Object> item) {
        List<String> parts = new ArrayList<>();
        String nodeName = Objects.toString(item.get("nodeName"), "");
        if (!nodeName.isBlank()) {
            parts.add(nodeName);
        }
        String pageLabel = Objects.toString(item.get("pageLabel"), "");
        if (!pageLabel.isBlank()) {
            parts.add("页面 " + pageLabel);
        }
        String formLabel = Objects.toString(item.get("formLabel"), "");
        if (!formLabel.isBlank()) {
            parts.add("表单 " + formLabel);
        }
        return String.join(" / ", parts);
    }

    private String extractExpressionVariable(String expression) {
        if (!notBlank(expression)) {
            return null;
        }
        Matcher matcher = SIMPLE_EXPRESSION.matcher(expression.trim());
        return matcher.matches() ? matcher.group(1) : null;
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return "";
        }
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
