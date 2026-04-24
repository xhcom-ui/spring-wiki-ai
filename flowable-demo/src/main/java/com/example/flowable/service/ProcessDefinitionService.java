package com.example.flowable.service;

import com.example.flowable.entity.ProcessDefinitionEntity;
import com.example.flowable.exception.BadRequestException;
import com.example.flowable.exception.NotFoundException;
import com.example.flowable.repository.ProcessDefinitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class ProcessDefinitionService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final String DESIGNER_TYPE_BPMN = "BPMN";
    private static final String DESIGNER_TYPE_CUSTOM = "CUSTOM";

    private final ProcessDefinitionRepository processDefinitionRepository;
    private final RepositoryService repositoryService;
    private final ProcessDesignRuntimeService processDesignRuntimeService;

    public ProcessDefinitionService(
            ProcessDefinitionRepository processDefinitionRepository,
            RepositoryService repositoryService,
            ProcessDesignRuntimeService processDesignRuntimeService
    ) {
        this.processDefinitionRepository = processDefinitionRepository;
        this.repositoryService = repositoryService;
        this.processDesignRuntimeService = processDesignRuntimeService;
    }

    @Transactional
    public ProcessDefinitionEntity saveProcessDefinition(
            String processKey,
            String processName,
            String bpmnXml,
            String designerType,
            String designSchemaJson,
            Long userId
    ) {
        String normalizedDesignerType = normalizeDesignerType(designerType);
        validateProcessDefinition(processKey, processName, bpmnXml, normalizedDesignerType, designSchemaJson);

        ProcessDefinitionEntity latestVersion = processDefinitionRepository.findFirstByProcessKeyOrderByVersionDesc(processKey);
        int newVersion = latestVersion != null ? latestVersion.getVersion() + 1 : 1;

        Deployment deployment = deployProcess(processKey, processName, bpmnXml, newVersion);
        LocalDateTime now = LocalDateTime.now();

        ProcessDefinitionEntity processDefinition = new ProcessDefinitionEntity();
        processDefinition.setProcessKey(processKey);
        processDefinition.setProcessName(processName);
        processDefinition.setBpmnXml(bpmnXml);
        processDefinition.setDesignerType(normalizedDesignerType);
        processDefinition.setDesignSchemaJson(designSchemaJson);
        processDefinition.setVersion(newVersion);
        processDefinition.setDeploymentId(deployment.getId());
        processDefinition.setStatus(STATUS_ACTIVE);
        processDefinition.setCreatedAt(now);
        processDefinition.setUpdatedAt(now);
        processDefinition.setCreatedBy(userId);
        processDefinition.setUpdatedBy(userId);

        deactivateOtherVersions(processKey, null, userId, now);
        ProcessDefinitionEntity savedProcessDefinition = processDefinitionRepository.save(processDefinition);
        log.info("Saved Flowable process definition: {} v{}", processKey, newVersion);
        return savedProcessDefinition;
    }

    public ProcessDefinitionEntity getProcessDefinitionById(Long id) {
        return processDefinitionRepository.findById(id).orElse(null);
    }

    public ProcessDefinitionEntity getProcessDefinitionByKeyAndVersion(String processKey, Integer version) {
        return processDefinitionRepository.findByProcessKeyOrderByVersionDesc(processKey).stream()
                .filter(definition -> definition.getVersion().equals(version))
                .findFirst()
                .orElse(null);
    }

    public ProcessDefinitionEntity getLatestProcessDefinitionByKey(String processKey) {
        return processDefinitionRepository.findFirstByProcessKeyOrderByVersionDesc(processKey);
    }

    public List<ProcessDefinitionEntity> getVersionsByProcessKey(String processKey) {
        return processDefinitionRepository.findByProcessKeyOrderByVersionDesc(processKey);
    }

    public List<ProcessDefinitionEntity> getAllProcessDefinitions() {
        return processDefinitionRepository.findAllByOrderByUpdatedAtDescIdDesc();
    }

    public List<ProcessDefinitionEntity> getActiveProcessDefinitions() {
        return processDefinitionRepository.findByStatusOrderByUpdatedAtDesc(STATUS_ACTIVE);
    }

    @Transactional
    public ProcessDefinitionEntity updateProcessDefinition(
            Long id,
            String processName,
            String bpmnXml,
            String designerType,
            String designSchemaJson,
            Long userId
    ) {
        ProcessDefinitionEntity processDefinition = requireDefinition(id);
        String normalizedDesignerType = normalizeDesignerType(designerType);
        validateProcessDefinition(processDefinition.getProcessKey(), processName, bpmnXml, normalizedDesignerType, designSchemaJson);

        deleteDeploymentIfPresent(processDefinition.getDeploymentId());
        Deployment deployment = deployProcess(
                processDefinition.getProcessKey(),
                processName,
                bpmnXml,
                processDefinition.getVersion()
        );

        processDefinition.setProcessName(processName);
        processDefinition.setBpmnXml(bpmnXml);
        processDefinition.setDesignerType(normalizedDesignerType);
        processDefinition.setDesignSchemaJson(designSchemaJson);
        processDefinition.setDeploymentId(deployment.getId());
        processDefinition.setUpdatedAt(LocalDateTime.now());
        processDefinition.setUpdatedBy(userId);

        ProcessDefinitionEntity updated = processDefinitionRepository.save(processDefinition);
        log.info("Updated Flowable process definition: {} ({})", processDefinition.getProcessKey(), id);
        return updated;
    }

    @Transactional
    public void deactivateProcessDefinition(Long id, Long userId) {
        ProcessDefinitionEntity processDefinition = requireDefinition(id);
        processDefinition.setStatus(STATUS_INACTIVE);
        processDefinition.setUpdatedAt(LocalDateTime.now());
        processDefinition.setUpdatedBy(userId);
        processDefinitionRepository.save(processDefinition);
        log.info("Deactivated Flowable process definition: {}", processDefinition.getProcessKey());
    }

    @Transactional
    public void deleteProcessDefinition(Long id) {
        ProcessDefinitionEntity processDefinition = requireDefinition(id);
        deleteDeploymentIfPresent(processDefinition.getDeploymentId());
        processDefinitionRepository.deleteById(id);
        log.info("Deleted Flowable process definition: {}", processDefinition.getProcessKey());
    }

    @Transactional
    public ProcessDefinitionEntity activateProcessVersion(Long id, Long userId) {
        ProcessDefinitionEntity processDefinition = requireDefinition(id);
        LocalDateTime now = LocalDateTime.now();

        deactivateOtherVersions(processDefinition.getProcessKey(), id, userId, now);
        deleteDeploymentIfPresent(processDefinition.getDeploymentId());

        Deployment deployment = deployProcess(
                processDefinition.getProcessKey(),
                processDefinition.getProcessName(),
                processDefinition.getBpmnXml(),
                processDefinition.getVersion()
        );

        processDefinition.setStatus(STATUS_ACTIVE);
        processDefinition.setDeploymentId(deployment.getId());
        processDefinition.setUpdatedAt(now);
        processDefinition.setUpdatedBy(userId);

        ProcessDefinitionEntity activated = processDefinitionRepository.save(processDefinition);
        log.info("Activated Flowable process definition: {} v{}", processDefinition.getProcessKey(), processDefinition.getVersion());
        return activated;
    }

    public Map<String, Object> compareProcessDefinitions(Long leftId, Long rightId) {
        ProcessDefinitionEntity left = requireDefinition(leftId);
        ProcessDefinitionEntity right = requireDefinition(rightId);

        List<String> leftLines = normalizeXmlLines(left.getBpmnXml());
        List<String> rightLines = normalizeXmlLines(right.getBpmnXml());
        int max = Math.max(leftLines.size(), rightLines.size());
        int changedLines = 0;
        List<Map<String, Object>> preview = new ArrayList<>();

        for (int i = 0; i < max; i++) {
            String leftLine = i < leftLines.size() ? leftLines.get(i) : "";
            String rightLine = i < rightLines.size() ? rightLines.get(i) : "";
            if (!Objects.equals(leftLine, rightLine)) {
                changedLines++;
                if (preview.size() < 12) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("line", i + 1);
                    row.put("left", leftLine);
                    row.put("right", rightLine);
                    preview.add(row);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("left", toSummary(left));
        result.put("right", toSummary(right));
        result.put("sameProcessKey", Objects.equals(left.getProcessKey(), right.getProcessKey()));
        result.put("sameDesignerType", Objects.equals(normalizeDesignerType(left.getDesignerType()), normalizeDesignerType(right.getDesignerType())));
        result.put("versionGap", Math.abs(left.getVersion() - right.getVersion()));
        result.put("leftLineCount", leftLines.size());
        result.put("rightLineCount", rightLines.size());
        result.put("changedLineCount", changedLines);
        result.put("diffPreview", preview);
        result.put("leftXml", left.getBpmnXml());
        result.put("rightXml", right.getBpmnXml());
        result.put("schemaComparison", processDesignRuntimeService.compareDesignSchemas(left.getDesignSchemaJson(), right.getDesignSchemaJson()));
        return result;
    }

    private ProcessDefinitionEntity requireDefinition(Long id) {
        return processDefinitionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("流程定义不存在"));
    }

    private Deployment deployProcess(String processKey, String processName, String bpmnXml, int version) {
        return repositoryService.createDeployment()
                .name(processName + " (v" + version + ")")
                .key(processKey)
                .addString(processKey + ".bpmn20.xml", bpmnXml)
                .deploy();
    }

    private void deactivateOtherVersions(String processKey, Long activeId, Long userId, LocalDateTime now) {
        List<ProcessDefinitionEntity> versions = processDefinitionRepository.findByProcessKeyOrderByVersionDesc(processKey);
        for (ProcessDefinitionEntity version : versions) {
            if (activeId != null && activeId.equals(version.getId())) {
                continue;
            }
            if (!STATUS_ACTIVE.equals(version.getStatus())) {
                continue;
            }
            version.setStatus(STATUS_INACTIVE);
            version.setUpdatedAt(now);
            version.setUpdatedBy(userId);
            processDefinitionRepository.save(version);
        }
    }

    private void deleteDeploymentIfPresent(String deploymentId) {
        if (deploymentId == null || deploymentId.isBlank()) {
            return;
        }
        try {
            repositoryService.deleteDeployment(deploymentId, true);
        } catch (Exception ex) {
            log.warn("Failed to delete deployment {} before redeploy", deploymentId, ex);
        }
    }

    private void validateProcessDefinition(
            String processKey,
            String processName,
            String bpmnXml,
            String designerType,
            String designSchemaJson
    ) {
        if (processKey == null || processKey.isBlank()) {
            throw new BadRequestException("流程 Key 不能为空");
        }
        if (processName == null || processName.isBlank()) {
            throw new BadRequestException("流程名称不能为空");
        }
        if (bpmnXml == null || bpmnXml.isBlank()) {
            throw new BadRequestException("BPMN XML 不能为空");
        }
        if (DESIGNER_TYPE_CUSTOM.equals(designerType) && (designSchemaJson == null || designSchemaJson.isBlank())) {
            throw new BadRequestException("自定义流程设计 JSON 不能为空");
        }
    }

    private String normalizeDesignerType(String designerType) {
        if (designerType == null || designerType.isBlank()) {
            return DESIGNER_TYPE_BPMN;
        }
        if (DESIGNER_TYPE_CUSTOM.equalsIgnoreCase(designerType)) {
            return DESIGNER_TYPE_CUSTOM;
        }
        return DESIGNER_TYPE_BPMN;
    }

    private Map<String, Object> toSummary(ProcessDefinitionEntity entity) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", entity.getId());
        summary.put("processKey", entity.getProcessKey());
        summary.put("processName", entity.getProcessName());
        summary.put("designerType", entity.getDesignerType());
        summary.put("hasDesignSchema", entity.getDesignSchemaJson() != null && !entity.getDesignSchemaJson().isBlank());
        summary.put("version", entity.getVersion());
        summary.put("status", entity.getStatus());
        summary.put("deploymentId", entity.getDeploymentId());
        summary.put("updatedAt", entity.getUpdatedAt());
        return summary;
    }

    private List<String> normalizeXmlLines(String xml) {
        if (xml == null || xml.isBlank()) {
            return List.of();
        }
        return xml.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }
}
