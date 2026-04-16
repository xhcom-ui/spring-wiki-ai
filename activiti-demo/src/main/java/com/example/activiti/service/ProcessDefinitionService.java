package com.example.activiti.service;

import com.example.activiti.entity.ProcessDefinitionEntity;
import com.example.activiti.repository.ProcessDefinitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ProcessDefinitionService {

    @Autowired
    private ProcessDefinitionRepository processDefinitionRepository;

    @Autowired
    private RepositoryService repositoryService;

    // Save BPMN process definition to database
    public ProcessDefinitionEntity saveProcessDefinition(String processKey, String processName, String bpmnXml, Long userId) {
        // Check if process with this key already exists
        ProcessDefinitionEntity latestVersion = processDefinitionRepository.findLatestByProcessKey(processKey);
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
        List<ProcessDefinitionEntity> processDefinitions = processDefinitionRepository.findByProcessKey(processKey);
        return processDefinitions.stream()
                .filter(pd -> pd.getVersion().equals(version))
                .findFirst()
                .orElse(null);
    }

    // Get latest version of process definition by key
    public ProcessDefinitionEntity getLatestProcessDefinitionByKey(String processKey) {
        return processDefinitionRepository.findLatestByProcessKey(processKey);
    }

    // Get all process definitions
    public List<ProcessDefinitionEntity> getAllProcessDefinitions() {
        return processDefinitionRepository.findAll();
    }

    // Get active process definitions
    public List<ProcessDefinitionEntity> getActiveProcessDefinitions() {
        return processDefinitionRepository.findByStatus("ACTIVE");
    }

    // Update process definition
    public ProcessDefinitionEntity updateProcessDefinition(Long id, String processName, String bpmnXml, Long userId) {
        ProcessDefinitionEntity processDefinition = processDefinitionRepository.findById(id).orElse(null);
        if (processDefinition == null) {
            throw new RuntimeException("Process definition not found");
        }

        // Deploy the updated process to Activiti
        Deployment deployment = repositoryService.createDeployment()
                .addString(processName + ".bpmn", bpmnXml)
                .name(processName)
                .deploy();

        // Update the process definition
        processDefinition.setProcessName(processName);
        processDefinition.setBpmnXml(bpmnXml);
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
            throw new RuntimeException("Process definition not found");
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
            throw new RuntimeException("Process definition not found");
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
            throw new RuntimeException("Process definition not found");
        }

        // Deactivate all other versions of the same process
        List<ProcessDefinitionEntity> allVersions = processDefinitionRepository.findByProcessKey(processDefinition.getProcessKey());
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
}
