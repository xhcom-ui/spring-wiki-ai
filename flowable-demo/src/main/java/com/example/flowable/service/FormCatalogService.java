package com.example.flowable.service;

import com.example.flowable.entity.FormCatalog;
import com.example.flowable.entity.ProcessDefinitionEntity;
import com.example.flowable.exception.BadRequestException;
import com.example.flowable.exception.ConflictException;
import com.example.flowable.exception.NotFoundException;
import com.example.flowable.repository.FormCatalogRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.flowable.repository.ProcessDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class FormCatalogService {

    @Autowired
    private FormCatalogRepository formCatalogRepository;

    @Autowired
    private ProcessDefinitionRepository processDefinitionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private volatile Map<String, FormCatalog> catalogCache = Map.of();

    public List<FormCatalog> getActiveCatalogs() {
        ensureDefaultCatalogs();
        return loadCatalogCache().values().stream()
                .filter(item -> item.getStatus() != null && item.getStatus() == 1)
                .sorted(Comparator.comparing(FormCatalog::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(FormCatalog::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    public List<FormCatalog> getAllCatalogs() {
        ensureDefaultCatalogs();
        return loadCatalogCache().values().stream()
                .sorted(Comparator.comparing(FormCatalog::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(FormCatalog::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    public FormCatalog getById(Long id) {
        ensureDefaultCatalogs();
        return formCatalogRepository.findById(id).orElse(null);
    }

    public FormCatalog getByFormKey(String formKey) {
        ensureDefaultCatalogs();
        if (formKey == null || formKey.isBlank()) {
            return null;
        }
        return loadCatalogCache().get(formKey.trim().toLowerCase());
    }

    public FormCatalog create(FormCatalog request) {
        FormCatalog existing = getByFormKey(request.getFormKey());
        if (existing != null) {
            throw new ConflictException("表单 Key 已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        request.setId(null);
        request.setFormKey(normalize(request.getFormKey()));
        request.setFormName(trimOrDefault(request.getFormName(), request.getFormKey()));
        request.setPageLabel(trimOrDefault(request.getPageLabel(), ""));
        request.setComponentKey(trimOrDefault(request.getComponentKey(), "GENERIC_TASK"));
        request.setFieldSchemaJson(normalizeFieldSchemaJson(request.getFieldSchemaJson()));
        request.setScope(trimOrDefault(request.getScope(), "TASK"));
        request.setDescription(trimOrDefault(request.getDescription(), ""));
        request.setSort(request.getSort() == null ? 0 : request.getSort());
        request.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        request.setCreatedAt(now);
        request.setUpdatedAt(now);
        FormCatalog saved = formCatalogRepository.save(request);
        refreshCache();
        return saved;
    }

    public FormCatalog update(Long id, FormCatalog request) {
        FormCatalog catalog = formCatalogRepository.findById(id).orElseThrow(() -> new NotFoundException("表单目录不存在"));
        FormCatalog sameKey = getByFormKey(request.getFormKey());
        if (sameKey != null && !sameKey.getId().equals(id)) {
            throw new ConflictException("表单 Key 已存在");
        }
        catalog.setFormKey(normalize(request.getFormKey()));
        catalog.setFormName(trimOrDefault(request.getFormName(), catalog.getFormKey()));
        catalog.setPageLabel(trimOrDefault(request.getPageLabel(), ""));
        catalog.setComponentKey(trimOrDefault(request.getComponentKey(), "GENERIC_TASK"));
        catalog.setFieldSchemaJson(normalizeFieldSchemaJson(request.getFieldSchemaJson()));
        catalog.setScope(trimOrDefault(request.getScope(), "TASK"));
        catalog.setDescription(trimOrDefault(request.getDescription(), ""));
        catalog.setSort(request.getSort() == null ? 0 : request.getSort());
        catalog.setStatus(request.getStatus() == null ? catalog.getStatus() : request.getStatus());
        catalog.setUpdatedAt(LocalDateTime.now());
        FormCatalog saved = formCatalogRepository.save(catalog);
        refreshCache();
        return saved;
    }

    public void delete(Long id) {
        FormCatalog catalog = formCatalogRepository.findById(id).orElseThrow(() -> new NotFoundException("表单目录不存在"));
        if (List.of("leave-form", "manager-approval", "general-approval", "leave-approve-form").contains(catalog.getFormKey())) {
            throw new ConflictException("默认表单目录不允许删除");
        }
        Map<String, Object> impact = getUsageImpact(id);
        int usageCount = ((Number) impact.getOrDefault("usageCount", 0)).intValue();
        if (usageCount > 0) {
            throw new ConflictException("表单目录仍被 " + usageCount + " 个流程版本引用，无法删除");
        }
        formCatalogRepository.delete(catalog);
        refreshCache();
    }

    public Map<String, Object> getUsageImpact(Long id) {
        FormCatalog catalog = formCatalogRepository.findById(id).orElseThrow(() -> new NotFoundException("表单目录不存在"));
        String formKey = normalize(catalog.getFormKey());
        List<Map<String, Object>> references = processDefinitionRepository.findAllByOrderByUpdatedAtDescIdDesc().stream()
                .filter(item -> referencesFormKey(item, formKey))
                .map(this::toProcessReference)
                .toList();
        return Map.of(
                "formCatalogId", catalog.getId(),
                "formKey", catalog.getFormKey(),
                "formName", catalog.getFormName(),
                "usageCount", references.size(),
                "references", references
        );
    }

    public Map<String, Object> buildFormMeta(String formKey, String pageLabel, String formLabel) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("formKey", trimOrDefault(formKey, ""));
        result.put("formLabel", trimOrDefault(formLabel, ""));
        result.put("pageLabel", trimOrDefault(pageLabel, ""));
        result.put("componentKey", "");
        result.put("fieldSchemaJson", "[]");
        result.put("fieldSchema", List.of());
        result.put("scope", "");
        result.put("catalogDescription", "");

        FormCatalog catalog = getByFormKey(formKey);
        if (catalog == null) {
            return result;
        }

        result.put("formCatalogId", catalog.getId());
        result.put("formKey", trimOrDefault(catalog.getFormKey(), formKey));
        result.put("formLabel", trimOrDefault(formLabel, catalog.getFormName()));
        result.put("pageLabel", trimOrDefault(pageLabel, catalog.getPageLabel()));
        result.put("componentKey", trimOrDefault(catalog.getComponentKey(), ""));
        result.put("fieldSchemaJson", trimOrDefault(catalog.getFieldSchemaJson(), "[]"));
        result.put("fieldSchema", parseFieldSchema(catalog.getFieldSchemaJson()));
        result.put("scope", trimOrDefault(catalog.getScope(), ""));
        result.put("catalogDescription", trimOrDefault(catalog.getDescription(), ""));
        return result;
    }

    private void ensureDefaultCatalogs() {
        List<FormCatalog> existing = formCatalogRepository.findAll();
        List<FormCatalog> defaults = buildDefaultCatalogs();
        if (existing.isEmpty()) {
            defaults.forEach(formCatalogRepository::save);
            refreshCache();
            return;
        }
        List<FormCatalog> missing = new ArrayList<>();
        for (FormCatalog item : defaults) {
            boolean present = existing.stream().anyMatch(current -> item.getFormKey().equalsIgnoreCase(current.getFormKey()));
            if (!present) {
                item.setId(null);
                missing.add(item);
            }
        }
        if (!missing.isEmpty()) {
            missing.forEach(formCatalogRepository::save);
            refreshCache();
        }
    }

    private Map<String, FormCatalog> loadCatalogCache() {
        Map<String, FormCatalog> snapshot = catalogCache;
        if (!snapshot.isEmpty()) {
            return snapshot;
        }
        synchronized (this) {
            if (!catalogCache.isEmpty()) {
                return catalogCache;
            }
            refreshCache();
            return catalogCache;
        }
    }

    private void refreshCache() {
        Map<String, FormCatalog> next = new LinkedHashMap<>();
        for (FormCatalog item : formCatalogRepository.findAll()) {
            if (item.getFormKey() != null && !item.getFormKey().isBlank()) {
                next.put(item.getFormKey().trim().toLowerCase(), item);
            }
        }
        catalogCache = next;
    }

    private boolean referencesFormKey(ProcessDefinitionEntity item, String formKey) {
        if (item == null || formKey == null || formKey.isBlank()) {
            return false;
        }
        String xml = item.getBpmnXml() == null ? "" : item.getBpmnXml();
        String schemaJson = item.getDesignSchemaJson() == null ? "" : item.getDesignSchemaJson();
        return xml.contains("flowable:formKey=\"" + formKey + "\"")
                || xml.contains("activiti:formKey=\"" + formKey + "\"")
                || schemaJson.contains("\"formKey\":\"" + formKey + "\"")
                || schemaJson.contains("\"formKey\": \"" + formKey + "\"");
    }

    private Map<String, Object> toProcessReference(ProcessDefinitionEntity item) {
        Map<String, Object> reference = new LinkedHashMap<>();
        reference.put("id", item.getId());
        reference.put("processKey", item.getProcessKey());
        reference.put("processName", item.getProcessName());
        reference.put("version", item.getVersion());
        reference.put("designerType", item.getDesignerType());
        reference.put("status", item.getStatus());
        reference.put("updatedAt", item.getUpdatedAt());
        return reference;
    }

    private List<FormCatalog> buildDefaultCatalogs() {
        LocalDateTime now = LocalDateTime.now();
        List<FormCatalog> catalogs = new ArrayList<>();
        catalogs.add(buildCatalog("leave-form", "请假申请单", "请假发起页", "LEAVE_START", "START", "请假流程发起表单", 1, now));
        catalogs.add(buildCatalog("manager-approval", "部门经理审批单", "待办审批页", "MANAGER_APPROVAL", "TASK", "部门经理审批业务表单", 2, now));
        catalogs.add(buildCatalog("general-approval", "总经理审批单", "待办审批页", "GENERAL_APPROVAL", "TASK", "总经理审批业务表单", 3, now));
        catalogs.add(buildCatalog("leave-approve-form", "审批单", "待办审批页", "MANAGER_APPROVAL", "TASK", "历史兼容审批表单 Key", 4, now));
        return catalogs;
    }

    private FormCatalog buildCatalog(String formKey, String formName, String pageLabel, String componentKey,
                                     String scope, String description, int sort, LocalDateTime now) {
        FormCatalog catalog = new FormCatalog();
        catalog.setFormKey(formKey);
        catalog.setFormName(formName);
        catalog.setPageLabel(pageLabel);
        catalog.setComponentKey(componentKey);
        catalog.setFieldSchemaJson(buildDefaultFieldSchemaJson(componentKey, scope));
        catalog.setScope(scope);
        catalog.setDescription(description);
        catalog.setSort(sort);
        catalog.setStatus(1);
        catalog.setCreatedAt(now);
        catalog.setUpdatedAt(now);
        return catalog;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("表单 Key 不能为空");
        }
        return value.trim().toLowerCase();
    }

    private String trimOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String normalizeFieldSchemaJson(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "[]";
        }
        try {
            JsonNode node = objectMapper.readTree(rawValue);
            if (!node.isArray()) {
                throw new BadRequestException("组件字段 schema 必须是 JSON 数组");
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("组件字段 schema 不是合法 JSON: " + exception.getMessage());
        }
    }

    private List<Map<String, Object>> parseFieldSchema(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawValue, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String buildDefaultFieldSchemaJson(String componentKey, String scope) {
        List<Map<String, Object>> fields = switch (trimOrDefault(componentKey, "GENERIC_TASK").toUpperCase()) {
            case "LEAVE_START" -> List.of(
                    schemaField("applicant", "申请人", "text", true, true, null, "发起人账号自动带入"),
                    schemaField("days", "请假天数", "number", true, false, 1, "支持 1-30 天"),
                    schemaField("startDate", "开始时间", "datetime", true, false, null, "请填写请假开始时间"),
                    schemaField("endDate", "结束时间", "datetime", true, false, null, "请填写请假结束时间"),
                    schemaField("reason", "请假原因", "textarea", true, false, null, "会同步展示在审批和监控快照中")
            );
            case "MANAGER_APPROVAL" -> List.of(
                    schemaField("reason", "请假原因", "textarea", false, true, null, "来自发起页原始申请内容"),
                    schemaField("managerAdvice", "经理审批建议", "textarea", true, false, null, "审批建议会沉淀到流程变量"),
                    schemaSelectField("priority", "优先级", true, "normal", List.of(
                            option("normal", "普通"),
                            option("high", "高优先级"),
                            option("urgent", "加急")
                    ), "高优先级会在监控中心和待办列表里更明显")
            );
            case "GENERAL_APPROVAL" -> List.of(
                    schemaField("reason", "请假原因", "textarea", false, true, null, "来自发起页原始申请内容"),
                    schemaField("finalDecisionNote", "最终审批说明", "textarea", false, false, null, "最终结论会同步到监控快照"),
                    schemaSelectField("allowResubmit", "允许补充材料", false, false, List.of(
                            option(false, "否"),
                            option(true, "是")
                    ), "审批驳回时可决定是否允许补充资料")
            );
            default -> Objects.equals(trimOrDefault(scope, "TASK").toUpperCase(), "START")
                    ? List.of(schemaField("reason", "业务说明", "textarea", false, false, null, "目录驱动的发起说明"))
                    : List.of(
                    schemaField("reason", "业务摘要", "textarea", false, true, null, "运行中业务摘要"),
                    schemaField("commentHint", "处理说明", "text", false, true, null, "目录说明字段")
            );
        };
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(fields);
        } catch (Exception exception) {
            return "[]";
        }
    }

    private Map<String, Object> schemaField(String field, String label, String component, boolean required, boolean readonly, Object defaultValue, String help) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("field", field);
        item.put("label", label);
        item.put("component", component);
        item.put("required", required);
        item.put("readonly", readonly);
        item.put("help", trimOrDefault(help, ""));
        if (defaultValue != null) {
            item.put("defaultValue", defaultValue);
        }
        if ("textarea".equals(component)) {
            item.put("rows", 3);
        }
        item.put("span", component.startsWith("date") ? 1 : 2);
        item.put("placeholder", readonly ? "" : "请输入" + label);
        if ("number".equals(component)) {
            item.put("validator", List.of(
                    Map.of("type", "min", "value", 1, "message", label + "不能小于 1"),
                    Map.of("type", "max", "value", 30, "message", label + "不能大于 30")
            ));
        } else if ("datetime".equals(component)) {
            item.put("validator", List.of(Map.of("type", "required", "message", "请填写" + label)));
        } else if (required) {
            item.put("validator", List.of(Map.of("type", "required", "message", "请填写" + label)));
        }
        return item;
    }

    private Map<String, Object> schemaSelectField(String field, String label, boolean required, Object defaultValue, List<Map<String, Object>> options, String help) {
        Map<String, Object> item = schemaField(field, label, "select", required, false, defaultValue, help);
        item.put("options", options);
        item.put("span", 1);
        return item;
    }

    private Map<String, Object> option(Object value, String label) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("value", value);
        item.put("label", label);
        return item;
    }
}
