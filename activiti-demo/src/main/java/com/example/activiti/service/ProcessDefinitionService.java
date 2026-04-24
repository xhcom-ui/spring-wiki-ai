package com.example.activiti.service;

import com.example.activiti.entity.ProcessDefinitionEntity;
import com.example.activiti.exception.BadRequestException;
import com.example.activiti.exception.NotFoundException;
import com.example.activiti.repository.ProcessDefinitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class ProcessDefinitionService {

    @Autowired
    private ProcessDefinitionRepository processDefinitionRepository;

    @Autowired
    private RepositoryService repositoryService;

    // Save BPMN process definition to database
    public ProcessDefinitionEntity saveProcessDefinition(
            String processKey,
            String processName,
            String bpmnXml,
            String designerType,
            String designerJson,
            Long userId
    ) {
        validateProcessDefinition(processKey, processName, bpmnXml);

        // Check if process with this key already exists
        ProcessDefinitionEntity latestVersion = processDefinitionRepository.findFirstByProcessKeyOrderByVersionDesc(processKey);
        int newVersion = latestVersion != null ? latestVersion.getVersion() + 1 : 1;

        // Deploy the process to Activiti
        Deployment deployment = repositoryService.createDeployment()
                .addString(processName + ".bpmn", bpmnXml)
                .name(processName + " (v" + newVersion + ")")
                .deploy();

        // Create new process definition entity
        ProcessDefinitionEntity processDefinition = new ProcessDefinitionEntity();
        processDefinition.setProcessKey(processKey);
        processDefinition.setProcessName(processName);
        processDefinition.setBpmnXml(bpmnXml);
        processDefinition.setDesignerType(normalizeDesignerType(designerType));
        processDefinition.setDesignerJson(normalizeDesignerJson(designerJson));
        processDefinition.setVersion(newVersion);
        processDefinition.setDeploymentId(deployment.getId());
        processDefinition.setStatus("ACTIVE");
        processDefinition.setCreatedAt(LocalDateTime.now());
        processDefinition.setUpdatedAt(LocalDateTime.now());
        processDefinition.setCreatedBy(userId);
        processDefinition.setUpdatedBy(userId);

        // Save to database
        ProcessDefinitionEntity savedProcessDefinition = processDefinitionRepository.save(processDefinition);
        log.info("Process definition saved: {} (version {})", processName, newVersion);

        // Deactivate previous versions
        if (latestVersion != null) {
            latestVersion.setStatus("INACTIVE");
            latestVersion.setUpdatedAt(LocalDateTime.now());
            latestVersion.setUpdatedBy(userId);
            processDefinitionRepository.save(latestVersion);
            log.info("Previous version of process {} deactivated", processKey);
        }

        return savedProcessDefinition;
    }

    // Get process definition by ID
    public ProcessDefinitionEntity getProcessDefinitionById(Long id) {
        return processDefinitionRepository.findById(id).orElse(null);
    }

    // Get process definition by key and version
    public ProcessDefinitionEntity getProcessDefinitionByKeyAndVersion(String processKey, Integer version) {
        List<ProcessDefinitionEntity> processDefinitions = processDefinitionRepository.findByProcessKeyOrderByVersionDesc(processKey);
        return processDefinitions.stream()
                .filter(pd -> pd.getVersion().equals(version))
                .findFirst()
                .orElse(null);
    }

    // Get latest version of process definition by key
    public ProcessDefinitionEntity getLatestProcessDefinitionByKey(String processKey) {
        return processDefinitionRepository.findFirstByProcessKeyOrderByVersionDesc(processKey);
    }

    public List<ProcessDefinitionEntity> getVersionsByProcessKey(String processKey) {
        return processDefinitionRepository.findByProcessKeyOrderByVersionDesc(processKey);
    }

    // Get all process definitions
    public List<ProcessDefinitionEntity> getAllProcessDefinitions() {
        return processDefinitionRepository.findAllByOrderByUpdatedAtDescIdDesc();
    }

    // Get active process definitions
    public List<ProcessDefinitionEntity> getActiveProcessDefinitions() {
        return processDefinitionRepository.findByStatusOrderByUpdatedAtDesc("ACTIVE");
    }

    // Update process definition
    public ProcessDefinitionEntity updateProcessDefinition(
            Long id,
            String processName,
            String bpmnXml,
            String designerType,
            String designerJson,
            Long userId
    ) {
        ProcessDefinitionEntity processDefinition = processDefinitionRepository.findById(id).orElse(null);
        if (processDefinition == null) {
            throw new NotFoundException("Process definition not found");
        }
        validateProcessDefinition(processDefinition.getProcessKey(), processName, bpmnXml);

        // Deploy the updated process to Activiti
        Deployment deployment = repositoryService.createDeployment()
                .addString(processName + ".bpmn", bpmnXml)
                .name(processName)
                .deploy();

        // Update the process definition
        processDefinition.setProcessName(processName);
        processDefinition.setBpmnXml(bpmnXml);
        processDefinition.setDesignerType(normalizeDesignerType(designerType));
        processDefinition.setDesignerJson(normalizeDesignerJson(designerJson));
        processDefinition.setDeploymentId(deployment.getId());
        processDefinition.setUpdatedAt(LocalDateTime.now());
        processDefinition.setUpdatedBy(userId);

        // Save to database
        ProcessDefinitionEntity updatedProcessDefinition = processDefinitionRepository.save(processDefinition);
        log.info("Process definition updated: {}", processName);

        return updatedProcessDefinition;
    }

    // Deactivate process definition
    public void deactivateProcessDefinition(Long id) {
        ProcessDefinitionEntity processDefinition = processDefinitionRepository.findById(id).orElse(null);
        if (processDefinition == null) {
            throw new NotFoundException("Process definition not found");
        }

        processDefinition.setStatus("INACTIVE");
        processDefinition.setUpdatedAt(LocalDateTime.now());
        processDefinitionRepository.save(processDefinition);
        log.info("Process definition deactivated: {}", processDefinition.getProcessName());
    }

    // Delete process definition
    public void deleteProcessDefinition(Long id) {
        ProcessDefinitionEntity processDefinition = processDefinitionRepository.findById(id).orElse(null);
        if (processDefinition == null) {
            throw new NotFoundException("Process definition not found");
        }

        // Delete deployment from Activiti
        if (processDefinition.getDeploymentId() != null) {
            repositoryService.deleteDeployment(processDefinition.getDeploymentId(), true);
        }

        // Delete from database
        processDefinitionRepository.deleteById(id);
        log.info("Process definition deleted: {}", processDefinition.getProcessName());
    }

    // Activate a specific version of a process definition
    public ProcessDefinitionEntity activateProcessVersion(Long id, Long userId) {
        ProcessDefinitionEntity processDefinition = processDefinitionRepository.findById(id).orElse(null);
        if (processDefinition == null) {
            throw new NotFoundException("Process definition not found");
        }

        // Deactivate all other versions of the same process
        List<ProcessDefinitionEntity> allVersions = processDefinitionRepository.findByProcessKeyOrderByVersionDesc(processDefinition.getProcessKey());
        for (ProcessDefinitionEntity version : allVersions) {
            if (!version.getId().equals(id)) {
                version.setStatus("INACTIVE");
                version.setUpdatedAt(LocalDateTime.now());
                version.setUpdatedBy(userId);
                processDefinitionRepository.save(version);
            }
        }

        // Activate the selected version
        processDefinition.setStatus("ACTIVE");
        processDefinition.setUpdatedAt(LocalDateTime.now());
        processDefinition.setUpdatedBy(userId);

        // Re-deploy the process to Activiti
        Deployment deployment = repositoryService.createDeployment()
                .addString(processDefinition.getProcessName() + ".bpmn", processDefinition.getBpmnXml())
                .name(processDefinition.getProcessName() + " (v" + processDefinition.getVersion() + ")")
                .deploy();

        processDefinition.setDeploymentId(deployment.getId());

        // Save to database
        ProcessDefinitionEntity activatedProcessDefinition = processDefinitionRepository.save(processDefinition);
        log.info("Process definition activated: {} (version {})", processDefinition.getProcessName(), processDefinition.getVersion());

        return activatedProcessDefinition;
    }

    public Map<String, Object> compareProcessDefinitions(Long leftId, Long rightId) {
        ProcessDefinitionEntity left = processDefinitionRepository.findById(leftId)
                .orElseThrow(() -> new NotFoundException("左侧流程版本不存在"));
        ProcessDefinitionEntity right = processDefinitionRepository.findById(rightId)
                .orElseThrow(() -> new NotFoundException("右侧流程版本不存在"));

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
        result.put("versionGap", Math.abs(left.getVersion() - right.getVersion()));
        result.put("leftLineCount", leftLines.size());
        result.put("rightLineCount", rightLines.size());
        result.put("changedLineCount", changedLines);
        result.put("diffPreview", preview);
        result.put("leftXml", left.getBpmnXml());
        result.put("rightXml", right.getBpmnXml());
        return result;
    }

    private void validateProcessDefinition(String processKey, String processName, String bpmnXml) {
        if (processKey == null || processKey.isBlank()) {
            throw new BadRequestException("流程 Key 不能为空");
        }
        if (processName == null || processName.isBlank()) {
            throw new BadRequestException("流程名称不能为空");
        }
        if (bpmnXml == null || bpmnXml.isBlank()) {
            throw new BadRequestException("BPMN XML 不能为空");
        }
    }

    private Map<String, Object> toSummary(ProcessDefinitionEntity entity) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", entity.getId());
        summary.put("processKey", entity.getProcessKey());
        summary.put("processName", entity.getProcessName());
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

    private String normalizeDesignerType(String designerType) {
        if (designerType == null || designerType.isBlank()) {
            return "CLASSIC";
        }
        return designerType.trim().toUpperCase();
    }

    private String normalizeDesignerJson(String designerJson) {
        if (designerJson == null || designerJson.isBlank()) {
            return null;
        }
        return designerJson.trim();
    }
}
