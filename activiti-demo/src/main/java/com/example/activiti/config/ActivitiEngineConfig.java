package com.example.activiti.config;

import org.activiti.engine.HistoryService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.history.HistoryLevel;
import org.activiti.spring.ProcessEngineFactoryBean;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class ActivitiEngineConfig {

    @Value("${spring.application.name:activiti-demo}")
    private String applicationName;

    @Value("${spring.activiti.database-schema-update:true}")
    private String databaseSchemaUpdate;

    @Value("${spring.activiti.history-level:full}")
    private String historyLevelKey;

    @Value("${spring.activiti.process-definition-location-prefix:classpath*:/processes/}")
    private String processDefinitionLocationPrefix;

    @Bean
    public SpringProcessEngineConfiguration springProcessEngineConfiguration(
            DataSource dataSource,
            PlatformTransactionManager transactionManager,
            ApplicationContext applicationContext,
            ResourcePatternResolver resourcePatternResolver
    ) throws Exception {
        SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setTransactionManager(transactionManager);
        configuration.setApplicationContext(applicationContext);
        configuration.setDatabaseSchemaUpdate(databaseSchemaUpdate);
        configuration.setHistory(historyLevelKey);
        configuration.setHistoryLevel(resolveHistoryLevel(historyLevelKey));
        configuration.setDbHistoryUsed(resolveHistoryLevel(historyLevelKey) != HistoryLevel.NONE);
        configuration.setAsyncExecutorActivate(false);
        configuration.setDeploymentName(applicationName);
        configuration.setDeploymentResources(resolveDeploymentResources(resourcePatternResolver));
        Map<Object, Object> beans = new HashMap<>();
        beans.put("copySendService", applicationContext.getBean("copySendService"));
        configuration.setBeans(beans);
        return configuration;
    }

    @Bean(destroyMethod = "close")
    public ProcessEngine processEngine(
            SpringProcessEngineConfiguration configuration,
            ApplicationContext applicationContext
    ) throws Exception {
        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
        factoryBean.setApplicationContext(applicationContext);
        factoryBean.setProcessEngineConfiguration(configuration);
        return factoryBean.getObject();
    }

    @Bean
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    @Bean
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    @Bean
    public TaskService taskService(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    @Bean
    public HistoryService historyService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }

    @Bean
    public ManagementService managementService(ProcessEngine processEngine) {
        return processEngine.getManagementService();
    }

    private Resource[] resolveDeploymentResources(ResourcePatternResolver resourcePatternResolver) throws Exception {
        String prefix = processDefinitionLocationPrefix == null ? "classpath*:/processes/" : processDefinitionLocationPrefix.trim();
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        String pattern = prefix + "*.bpmn*";
        return resourcePatternResolver.getResources(pattern);
    }

    private HistoryLevel resolveHistoryLevel(String key) {
        if (key == null || key.isBlank()) {
            return HistoryLevel.FULL;
        }
        return HistoryLevel.valueOf(key.trim().toUpperCase());
    }
}
