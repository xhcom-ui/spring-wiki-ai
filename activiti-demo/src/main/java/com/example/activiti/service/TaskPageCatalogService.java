package com.example.activiti.service;

import com.example.activiti.entity.ProcessDefinitionEntity;
import com.example.activiti.entity.TaskPageCatalogItem;
import com.example.activiti.exception.BadRequestException;
import com.example.activiti.exception.ConflictException;
import com.example.activiti.exception.NotFoundException;
import com.example.activiti.repository.ProcessDefinitionRepository;
import com.example.activiti.repository.TaskPageCatalogRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class TaskPageCatalogService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final Set<String> SUPPORTED_NODE_NAMES = Set.of(
            "userTask", "serviceTask", "subProcess", "startEvent", "endEvent"
    );

    @Autowired
    private TaskPageCatalogRepository taskPageCatalogRepository;

    @Autowired
    private ProcessDefinitionRepository processDefinitionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public Map<String, Object> getCatalog() {
        ensureDefaultCatalog();
        List<TaskPageCatalogItem> activeItems = taskPageCatalogRepository.findByStatus(1);
        List<Map<String, Object>> forms = activeItems.stream()
                .filter(item -> "FORM".equalsIgnoreCase(item.getItemType()))
                .sorted(catalogComparator())
                .map(this::toFormMap)
                .toList();
        List<Map<String, Object>> pages = activeItems.stream()
                .filter(item -> "PAGE".equalsIgnoreCase(item.getItemType()))
                .sorted(catalogComparator())
                .map(this::toPageMap)
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("forms", forms);
        result.put("pages", pages);
        return result;
    }

    public List<TaskPageCatalogItem> getAllItems() {
        ensureDefaultCatalog();
        return taskPageCatalogRepository.findAll().stream()
                .sorted(catalogComparator())
                .toList();
    }

    public TaskPageCatalogItem getItemById(Long id) {
        ensureDefaultCatalog();
        return taskPageCatalogRepository.findById(id).orElse(null);
    }

    public TaskPageCatalogItem createItem(TaskPageCatalogItem request) {
        ensureDefaultCatalog();
        validateItem(request, null);
        TaskPageCatalogItem item = normalizeForSave(request, null);
        return taskPageCatalogRepository.save(item);
    }

    public TaskPageCatalogItem updateItem(Long id, TaskPageCatalogItem request) {
        ensureDefaultCatalog();
        TaskPageCatalogItem existing = taskPageCatalogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("目录项不存在"));
        validateItem(request, existing);
        TaskPageCatalogItem item = normalizeForSave(request, existing);
        item.setId(existing.getId());
        item.setCreatedAt(existing.getCreatedAt());
        return taskPageCatalogRepository.save(item);
    }

    public void deleteItem(Long id) {
        ensureDefaultCatalog();
        TaskPageCatalogItem existing = taskPageCatalogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("目录项不存在"));
        if (existing.getSystemFlag() != null && existing.getSystemFlag() == 1) {
            throw new ConflictException("系统默认目录项不允许删除");
        }
        taskPageCatalogRepository.removeById(id);
    }

    public List<Map<String, Object>> getItemReferences(Long id) {
        ensureDefaultCatalog();
        TaskPageCatalogItem item = taskPageCatalogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("目录项不存在"));
        return processDefinitionRepository.findAllByOrderByUpdatedAtDescIdDesc().stream()
                .flatMap(processDefinition -> scanProcessDefinitionReferences(processDefinition, item).stream())
                .toList();
    }

    private void ensureDefaultCatalog() {
        List<TaskPageCatalogItem> existing = taskPageCatalogRepository.findAll();
        if (existing.isEmpty()) {
            taskPageCatalogRepository.saveAll(buildDefaultCatalog());
            return;
        }
        List<TaskPageCatalogItem> missing = new ArrayList<>();
        for (TaskPageCatalogItem item : buildDefaultCatalog()) {
            boolean matched = existing.stream().anyMatch(current -> Objects.equals(current.getItemKey(), item.getItemKey()));
            if (!matched) {
                item.setId(null);
                missing.add(item);
            }
        }
        if (!missing.isEmpty()) {
            taskPageCatalogRepository.saveAll(missing);
        }
    }

    private List<TaskPageCatalogItem> buildDefaultCatalog() {
        LocalDateTime now = LocalDateTime.now();
        List<TaskPageCatalogItem> items = new ArrayList<>();
        items.add(buildForm("leave-apply-form", "请假申请单", "leave",
                "适用于请假发起、部门经理审批、总经理审批等节点。",
                "leave-approval", "leave-complete", 10, now));
        items.add(buildPage("leave-approval", "请假审批页", "todo", "leave",
                "打开请假业务办理页，支持签收、审批和意见录入。", 20, now));
        items.add(buildPage("leave-complete", "请假完成页", "done", "leave",
                "展示请假节点处理完成后的业务回执。", 30, now));
        items.add(buildPage("generic-task", "通用任务页", "todo", "generic",
                "没有专用业务页时，使用统一的通用办理页。", 40, now));
        items.add(buildPage("generic-task-done", "通用完成页", "done", "generic",
                "没有专用完成页时，使用统一的通用回执页。", 50, now));
        return items;
    }

    private TaskPageCatalogItem buildForm(
            String itemKey,
            String label,
            String businessKind,
            String description,
            String defaultTodoPage,
            String defaultDonePage,
            int sort,
            LocalDateTime now
    ) {
        TaskPageCatalogItem item = baseItem(itemKey, label, "FORM", businessKind, description, sort, now);
        item.setDefaultTodoPage(defaultTodoPage);
        item.setDefaultDonePage(defaultDonePage);
        return item;
    }

    private TaskPageCatalogItem buildPage(
            String itemKey,
            String label,
            String pageMode,
            String businessKind,
            String description,
            int sort,
            LocalDateTime now
    ) {
        TaskPageCatalogItem item = baseItem(itemKey, label, "PAGE", businessKind, description, sort, now);
        item.setPageMode(pageMode);
        return item;
    }

    private TaskPageCatalogItem baseItem(
            String itemKey,
            String label,
            String itemType,
            String businessKind,
            String description,
            int sort,
            LocalDateTime now
    ) {
        TaskPageCatalogItem item = new TaskPageCatalogItem();
        item.setItemKey(itemKey);
        item.setLabel(label);
        item.setItemType(itemType);
        item.setBusinessKind(businessKind);
        item.setDescription(description);
        item.setSystemFlag(1);
        item.setSort(sort);
        item.setStatus(1);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        return item;
    }

    private TaskPageCatalogItem normalizeForSave(TaskPageCatalogItem request, TaskPageCatalogItem existing) {
        LocalDateTime now = LocalDateTime.now();
        TaskPageCatalogItem item = new TaskPageCatalogItem();
        item.setItemKey(normalizeKey(existing != null && existing.getSystemFlag() != null && existing.getSystemFlag() == 1
                ? existing.getItemKey()
                : request.getItemKey()));
        item.setLabel(trimToNull(request.getLabel()));
        item.setItemType(normalizeItemType(existing != null && existing.getSystemFlag() != null && existing.getSystemFlag() == 1
                ? existing.getItemType()
                : request.getItemType()));
        item.setPageMode("PAGE".equalsIgnoreCase(item.getItemType()) ? normalizePageMode(request.getPageMode()) : null);
        item.setBusinessKind(normalizeKey(request.getBusinessKind()));
        item.setDescription(trimToNull(request.getDescription()));
        item.setDefaultTodoPage("FORM".equalsIgnoreCase(item.getItemType()) ? normalizeKey(request.getDefaultTodoPage()) : null);
        item.setDefaultDonePage("FORM".equalsIgnoreCase(item.getItemType()) ? normalizeKey(request.getDefaultDonePage()) : null);
        item.setSystemFlag(existing == null ? (request.getSystemFlag() != null && request.getSystemFlag() == 1 ? 1 : 0) : existing.getSystemFlag());
        item.setSort(request.getSort() == null ? 0 : request.getSort());
        item.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        item.setCreatedAt(existing == null ? now : existing.getCreatedAt());
        item.setUpdatedAt(now);
        return item;
    }

    private void validateItem(TaskPageCatalogItem request, TaskPageCatalogItem existing) {
        if (request == null) {
            throw new BadRequestException("目录项不能为空");
        }
        String itemType = normalizeItemType(existing != null && existing.getSystemFlag() != null && existing.getSystemFlag() == 1
                ? existing.getItemType()
                : request.getItemType());
        String itemKey = normalizeKey(existing != null && existing.getSystemFlag() != null && existing.getSystemFlag() == 1
                ? existing.getItemKey()
                : request.getItemKey());

        if (itemKey == null || itemKey.isBlank()) {
            throw new BadRequestException("目录 Key 不能为空");
        }
        if (trimToNull(request.getLabel()) == null) {
            throw new BadRequestException("显示名称不能为空");
        }
        if (itemType == null) {
            throw new BadRequestException("目录类型不能为空");
        }
        if ("PAGE".equals(itemType) && normalizePageMode(request.getPageMode()) == null) {
            throw new BadRequestException("页面目录必须指定页面模式");
        }
        if ("FORM".equals(itemType) && (normalizeKey(request.getDefaultTodoPage()) == null || normalizeKey(request.getDefaultDonePage()) == null)) {
            throw new BadRequestException("表单目录必须配置默认待办页和完成页");
        }

        TaskPageCatalogItem sameKey = taskPageCatalogRepository.findByItemKey(itemKey);
        if (sameKey != null && (existing == null || !sameKey.getId().equals(existing.getId()))) {
            throw new ConflictException("目录 Key 已存在");
        }
    }

    private String normalizeItemType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return "PAGE".equalsIgnoreCase(value) ? "PAGE" : "FORM";
    }

    private String normalizePageMode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return "done".equalsIgnoreCase(value) ? "done" : "todo";
    }

    private String normalizeKey(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Map<String, Object> toFormMap(TaskPageCatalogItem item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", item.getItemKey());
        result.put("label", item.getLabel());
        result.put("kind", item.getBusinessKind());
        result.put("description", item.getDescription());
        result.put("defaultTodoPage", item.getDefaultTodoPage());
        result.put("defaultDonePage", item.getDefaultDonePage());
        return result;
    }

    private Map<String, Object> toPageMap(TaskPageCatalogItem item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", item.getItemKey());
        result.put("label", item.getLabel());
        result.put("mode", item.getPageMode());
        result.put("kind", item.getBusinessKind());
        result.put("description", item.getDescription());
        return result;
    }

    private List<Map<String, Object>> scanProcessDefinitionReferences(ProcessDefinitionEntity definition, TaskPageCatalogItem item) {
        if (definition.getBpmnXml() == null || definition.getBpmnXml().isBlank() || item.getItemKey() == null) {
            return List.of();
        }
        if (!definition.getBpmnXml().contains(item.getItemKey())) {
            return List.of();
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(definition.getBpmnXml())));
            NodeList nodes = document.getElementsByTagNameNS("*", "*");
            List<Map<String, Object>> result = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (!(node instanceof Element element) || !SUPPORTED_NODE_NAMES.contains(element.getLocalName())) {
                    continue;
                }
                Map<String, String> metadata = readElementMetadata(element);
                List<String> matchedFields = resolveMatchedFields(item, metadata);
                for (String matchedField : matchedFields) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("processDefinitionId", definition.getId());
                    row.put("processKey", definition.getProcessKey());
                    row.put("processName", definition.getProcessName());
                    row.put("version", definition.getVersion());
                    row.put("status", definition.getStatus());
                    row.put("elementId", element.getAttribute("id"));
                    row.put("elementName", element.getAttribute("name"));
                    row.put("elementType", element.getLocalName());
                    row.put("matchField", matchedField);
                    row.put("matchedValue", metadata.get(matchedField));
                    result.add(row);
                }
            }
            return result;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<String> resolveMatchedFields(TaskPageCatalogItem item, Map<String, String> metadata) {
        List<String> fields = new ArrayList<>();
        if ("FORM".equalsIgnoreCase(item.getItemType())) {
            if (Objects.equals(item.getItemKey(), metadata.get("formKey"))) {
                fields.add("formKey");
            }
            return fields;
        }
        if ("todo".equalsIgnoreCase(item.getPageMode()) && Objects.equals(item.getItemKey(), metadata.get("todoPage"))) {
            fields.add("todoPage");
        }
        if ("done".equalsIgnoreCase(item.getPageMode()) && Objects.equals(item.getItemKey(), metadata.get("donePage"))) {
            fields.add("donePage");
        }
        return fields;
    }

    private Map<String, String> readElementMetadata(Element element) {
        Map<String, String> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, "formKey", element.getAttribute("activiti:formKey"));
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element docElement) || !"documentation".equals(docElement.getLocalName())) {
                continue;
            }
            String textFormat = docElement.getAttribute("textFormat");
            if (!"application/vnd.activiti-designer+json".equals(textFormat)) {
                continue;
            }
            String json = docElement.getTextContent();
            if (json == null || json.isBlank()) {
                continue;
            }
            try {
                Map<String, Object> parsed = objectMapper.readValue(json, MAP_TYPE);
                putIfPresent(metadata, "formKey", stringify(parsed.get("formKey")));
                putIfPresent(metadata, "todoPage", stringify(parsed.get("todoPage")));
                putIfPresent(metadata, "donePage", stringify(parsed.get("donePage")));
            } catch (Exception ignored) {
                // ignore malformed metadata blocks
            }
        }
        return metadata;
    }

    private void putIfPresent(Map<String, String> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value.trim());
        }
    }

    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Comparator<TaskPageCatalogItem> catalogComparator() {
        return Comparator.comparing(TaskPageCatalogItem::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(TaskPageCatalogItem::getId, Comparator.nullsLast(Long::compareTo));
    }
}
