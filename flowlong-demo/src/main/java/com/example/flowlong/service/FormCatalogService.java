package com.example.flowlong.service;

import com.example.flowlong.common.PageBuilder;
import com.example.flowlong.common.PageResult;
import com.example.flowlong.controller.dto.FormCatalogQueryRequest;
import com.example.flowlong.entity.FormCatalog;
import com.example.flowlong.repository.FormCatalogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FormCatalogService {

    private final FormCatalogRepository formCatalogRepository;
    private final ObjectMapper objectMapper;

    public FormCatalogService(FormCatalogRepository formCatalogRepository, ObjectMapper objectMapper) {
        this.formCatalogRepository = formCatalogRepository;
        this.objectMapper = objectMapper;
    }

    public List<FormCatalog> getAllEnabledCatalogs() {
        ensureDefaultCatalogs();
        return formCatalogRepository.findByStatus(1);
    }

    public List<FormCatalog> getAllCatalogs() {
        ensureDefaultCatalogs();
        return formCatalogRepository.findAll();
    }

    public PageResult<FormCatalog> queryCatalogs(FormCatalogQueryRequest query) {
        ensureDefaultCatalogs();
        return PageBuilder.build(query, formCatalogRepository.findPageByCondition(query), formCatalogRepository.countByCondition(query));
    }

    public FormCatalog getCatalogByFormKey(String formKey) {
        ensureDefaultCatalogs();
        return formCatalogRepository.findByFormKey(formKey);
    }

    public FormCatalog createCatalog(FormCatalog request) {
        validateRequest(request, null);
        LocalDateTime now = LocalDateTime.now();
        request.setId(null);
        request.setCreatedAt(now);
        request.setUpdatedAt(now);
        if (request.getStatus() == null) {
            request.setStatus(1);
        }
        request.setSchemaJson(normalizeSchemaJson(request));
        return formCatalogRepository.save(request);
    }

    public FormCatalog updateCatalog(Long id, FormCatalog request) {
        FormCatalog existing = requireCatalog(id);
        validateRequest(request, id);
        existing.setFormKey(request.getFormKey().trim());
        existing.setTitle(request.getTitle().trim());
        existing.setDescription(normalizeText(request.getDescription()));
        existing.setStatus(request.getStatus() == null ? existing.getStatus() : request.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setSchemaJson(request.getSchemaJson());
        existing.setSchemaJson(normalizeSchemaJson(existing));
        return formCatalogRepository.save(existing);
    }

    public void deleteCatalog(Long id) {
        FormCatalog existing = requireCatalog(id);
        if (List.of("leave-form", "manager-approval", "general-approval", "default").contains(existing.getFormKey())) {
            throw new RuntimeException("默认表单目录不允许删除");
        }
        formCatalogRepository.delete(existing);
    }

    public List<Map<String, Object>> getRuntimeCatalogViews() {
        ensureDefaultCatalogs();
        return getAllEnabledCatalogs().stream().map(this::toRuntimeView).toList();
    }

    public Map<String, Object> toRuntimeView(FormCatalog formCatalog) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", formCatalog.getId());
        view.put("formKey", formCatalog.getFormKey());
        view.put("title", formCatalog.getTitle());
        view.put("description", formCatalog.getDescription());
        view.put("status", formCatalog.getStatus());
        view.put("schemaJson", formCatalog.getSchemaJson());
        view.put("schema", readSchemaMap(formCatalog.getSchemaJson(), formCatalog));
        view.put("updatedAt", formCatalog.getUpdatedAt());
        return view;
    }

    private FormCatalog requireCatalog(Long id) {
        return formCatalogRepository.findById(id).orElseThrow(() -> new RuntimeException("表单目录不存在"));
    }

    private void validateRequest(FormCatalog request, Long currentId) {
        if (request == null) {
            throw new RuntimeException("表单目录参数不能为空");
        }
        if (request.getFormKey() == null || request.getFormKey().isBlank()) {
            throw new RuntimeException("表单 Key 不能为空");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new RuntimeException("表单名称不能为空");
        }
        FormCatalog sameKey = formCatalogRepository.findByFormKey(request.getFormKey().trim());
        if (sameKey != null && (currentId == null || !sameKey.getId().equals(currentId))) {
            throw new RuntimeException("表单 Key 已存在");
        }
        if (request.getSchemaJson() == null || request.getSchemaJson().isBlank()) {
            throw new RuntimeException("表单 Schema 不能为空");
        }
        readSchemaMap(request.getSchemaJson(), request);
    }

    private void ensureDefaultCatalogs() {
        List<FormCatalog> existing = formCatalogRepository.findAll();
        List<FormCatalog> defaults = buildDefaultCatalogs();
        if (existing.isEmpty()) {
            formCatalogRepository.saveAll(defaults);
            return;
        }
        List<FormCatalog> missing = new ArrayList<>();
        for (FormCatalog item : defaults) {
            boolean exists = existing.stream().anyMatch(catalog -> item.getFormKey().equalsIgnoreCase(catalog.getFormKey()));
            if (!exists) {
                missing.add(item);
            }
        }
        if (!missing.isEmpty()) {
            formCatalogRepository.saveAll(missing);
        }
    }

    private List<FormCatalog> buildDefaultCatalogs() {
        LocalDateTime now = LocalDateTime.now();
        List<FormCatalog> items = new ArrayList<>();
        items.add(buildDefaultCatalog("leave-form", "请假申请单", "发起请假流程时填写完整的申请信息和审批链路。", """
                {
                  "fields": [
                    {
                      "key": "applicant",
                      "label": "申请人",
                      "type": "text",
                      "readonlyWhen": "always",
                      "componentProps": { "autocomplete": "off" },
                      "help": "当前登录用户会自动作为流程发起人。",
                      "validator": [{ "type": "required", "message": "申请人不能为空" }]
                    },
                    {
                      "key": "days",
                      "label": "请假天数",
                      "type": "number",
                      "min": 1,
                      "max": 30,
                      "componentProps": { "step": 1, "inputmode": "numeric" },
                      "placeholder": "请输入 1-30 的整数天数",
                      "help": "建议与开始/结束时间保持一致。",
                      "validator": [
                        { "type": "required", "message": "请假天数不能为空" },
                        { "type": "positiveInteger", "message": "请假天数必须为正整数" },
                        { "type": "max", "value": 30, "message": "请假天数不能超过 30 天" }
                      ]
                    },
                    {
                      "key": "startDate",
                      "label": "开始时间",
                      "type": "datetime-local",
                      "componentProps": { "step": 60 },
                      "help": "请选择实际请假开始时间。",
                      "validator": [{ "type": "required", "message": "开始时间不能为空" }]
                    },
                    {
                      "key": "endDate",
                      "label": "结束时间",
                      "type": "datetime-local",
                      "componentProps": { "step": 60 },
                      "help": "结束时间应晚于开始时间。",
                      "validator": [
                        { "type": "required", "message": "结束时间不能为空" },
                        { "type": "afterField", "field": "startDate", "message": "结束时间必须晚于开始时间" }
                      ]
                    },
                    {
                      "key": "deptManager",
                      "label": "部门经理",
                      "type": "select",
                      "optionsKey": "approvers",
                      "optionsApi": "/users/lookup",
                      "help": "请选择第一审批节点办理人。",
                      "validator": [{ "type": "required", "message": "部门经理不能为空" }]
                    },
                    {
                      "key": "generalManager",
                      "label": "总经理",
                      "type": "select",
                      "optionsKey": "approvers",
                      "optionsApi": "/users/lookup",
                      "help": "请选择第二审批节点办理人。",
                      "validator": [{ "type": "required", "message": "总经理不能为空" }]
                    },
                    {
                      "key": "reason",
                      "label": "请假原因",
                      "type": "textarea",
                      "rows": 5,
                      "span": 2,
                      "componentProps": { "maxlength": 300 },
                      "placeholder": "请输入请假原因",
                      "help": "原因越清晰，审批人越容易快速处理。",
                      "validator": [
                        { "type": "required", "message": "请假原因不能为空" },
                        { "type": "minLength", "value": 4, "message": "请假原因至少填写 4 个字符" }
                      ]
                    }
                  ]
                }
                """, now));
        items.add(buildDefaultCatalog("manager-approval", "部门经理审批单", "请确认请假时间、原因与当前团队安排，再给出审批意见。", """
                {
                  "summaryFields": [
                    { "key": "applicant", "label": "申请人" },
                    { "key": "days", "label": "请假天数" },
                    { "key": "startDate", "label": "开始时间", "formatter": "datetime" },
                    { "key": "endDate", "label": "结束时间", "formatter": "datetime" },
                    { "key": "reason", "label": "请假原因", "span": 2 },
                    { "key": "deptManager", "label": "部门经理" },
                    { "key": "generalManager", "label": "总经理" }
                  ],
                  "decisionFields": [
                    {
                      "key": "approvalComment",
                      "label": "审批意见",
                      "type": "textarea",
                      "rows": 5,
                      "span": 2,
                      "componentProps": { "maxlength": 300 },
                      "placeholder": "请输入同意审批的说明或补充意见",
                      "help": "同意时建议说明审批判断依据。",
                      "outcome": "approved",
                      "validator": [{ "type": "required", "message": "审批意见不能为空" }]
                    },
                    {
                      "key": "rejectReason",
                      "label": "退回原因",
                      "type": "textarea",
                      "rows": 5,
                      "span": 2,
                      "componentProps": { "maxlength": 300 },
                      "placeholder": "请输入驳回原因，便于申请人补充或修正",
                      "help": "驳回时建议明确说明退回原因。",
                      "outcome": "rejected",
                      "validator": [{ "type": "required", "message": "退回原因不能为空" }]
                    },
                    {
                      "key": "systemRemark",
                      "label": "系统备注",
                      "type": "textarea",
                      "rows": 3,
                      "span": 2,
                      "componentProps": { "maxlength": 200 },
                      "placeholder": "记录额外上下文、交接说明或系统备注",
                      "help": "这部分会和审批意见一起进入节点审计日志。"
                    }
                  ]
                }
                """, now));
        items.add(buildDefaultCatalog("general-approval", "总经理审批单", "请结合部门经理意见和业务影响，完成最终审批。", """
                {
                  "summaryFields": [
                    { "key": "applicant", "label": "申请人" },
                    { "key": "days", "label": "请假天数" },
                    { "key": "startDate", "label": "开始时间", "formatter": "datetime" },
                    { "key": "endDate", "label": "结束时间", "formatter": "datetime" },
                    { "key": "reason", "label": "请假原因", "span": 2 },
                    { "key": "deptManager", "label": "部门经理" },
                    { "key": "generalManager", "label": "总经理" }
                  ],
                  "decisionFields": [
                    {
                      "key": "approvalComment",
                      "label": "审批意见",
                      "type": "textarea",
                      "rows": 5,
                      "span": 2,
                      "componentProps": { "maxlength": 300 },
                      "placeholder": "请输入最终审批意见",
                      "help": "同意时建议补充最终审批结论。",
                      "outcome": "approved",
                      "validator": [{ "type": "required", "message": "审批意见不能为空" }]
                    },
                    {
                      "key": "rejectReason",
                      "label": "退回原因",
                      "type": "textarea",
                      "rows": 5,
                      "span": 2,
                      "componentProps": { "maxlength": 300 },
                      "placeholder": "请输入驳回原因，便于申请人补充或修正",
                      "help": "驳回时建议明确说明退回原因。",
                      "outcome": "rejected",
                      "validator": [{ "type": "required", "message": "退回原因不能为空" }]
                    },
                    {
                      "key": "systemRemark",
                      "label": "系统备注",
                      "type": "textarea",
                      "rows": 3,
                      "span": 2,
                      "componentProps": { "maxlength": 200 },
                      "placeholder": "记录额外上下文、交接说明或系统备注",
                      "help": "这部分会和审批意见一起进入节点审计日志。"
                    }
                  ]
                }
                """, now));
        items.add(buildDefaultCatalog("default", "业务表单", "当前节点没有绑定专门的业务表单，展示标准审批摘要。", """
                {
                  "summaryFields": [
                    { "key": "applicant", "label": "申请人" },
                    { "key": "businessKey", "label": "业务键" },
                    { "key": "processInstanceId", "label": "流程实例" },
                    { "key": "reason", "label": "业务说明", "span": 2 }
                  ],
                  "decisionFields": [
                    {
                      "key": "approvalComment",
                      "label": "审批意见",
                      "type": "textarea",
                      "rows": 5,
                      "span": 2,
                      "placeholder": "请输入审批说明",
                      "outcome": "approved"
                    },
                    {
                      "key": "rejectReason",
                      "label": "退回原因",
                      "type": "textarea",
                      "rows": 5,
                      "span": 2,
                      "placeholder": "请输入退回原因",
                      "outcome": "rejected"
                    },
                    {
                      "key": "systemRemark",
                      "label": "系统备注",
                      "type": "textarea",
                      "rows": 3,
                      "span": 2,
                      "placeholder": "记录系统备注"
                    }
                  ]
                }
                """, now));
        return items;
    }

    private FormCatalog buildDefaultCatalog(String formKey, String title, String description, String schemaJson, LocalDateTime now) {
        FormCatalog formCatalog = new FormCatalog();
        formCatalog.setFormKey(formKey);
        formCatalog.setTitle(title);
        formCatalog.setDescription(description);
        formCatalog.setSchemaJson(injectMetaIntoSchema(formKey, title, description, schemaJson));
        formCatalog.setStatus(1);
        formCatalog.setCreatedAt(now);
        formCatalog.setUpdatedAt(now);
        return formCatalog;
    }

    private String normalizeSchemaJson(FormCatalog formCatalog) {
        return injectMetaIntoSchema(formCatalog.getFormKey(), formCatalog.getTitle(), formCatalog.getDescription(), formCatalog.getSchemaJson());
    }

    private String injectMetaIntoSchema(String formKey, String title, String description, String rawSchemaJson) {
        Map<String, Object> schema = readSchemaMap(rawSchemaJson, null);
        schema.put("key", formKey == null ? "" : formKey.trim());
        schema.put("title", title == null ? "" : title.trim());
        schema.put("description", normalizeText(description));
        try {
            return objectMapper.writeValueAsString(schema);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("表单 Schema 序列化失败");
        }
    }

    private Map<String, Object> readSchemaMap(String rawSchemaJson, FormCatalog fallbackCatalog) {
        try {
            Map<String, Object> schema = objectMapper.readValue(rawSchemaJson, new TypeReference<Map<String, Object>>() {});
            if (fallbackCatalog != null) {
                schema.putIfAbsent("key", fallbackCatalog.getFormKey());
                schema.putIfAbsent("title", fallbackCatalog.getTitle());
                schema.putIfAbsent("description", normalizeText(fallbackCatalog.getDescription()));
            }
            return schema;
        } catch (Exception e) {
            throw new RuntimeException("表单 Schema JSON 格式不合法");
        }
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
