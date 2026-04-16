package com.gateway.manager;

import com.gateway.plugin.GatewayPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件管理器
 */
@Slf4j
@Component
public class PluginManager implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private final Map<String, GatewayPlugin> plugins = new ConcurrentHashMap<>();
    private final Set<String> loadedPlugins = new HashSet<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        // 初始化时加载所有插件
        loadPlugins();
    }

    /**
     * 加载插件
     */
    public void loadPlugins() {
        // 从Spring容器中获取所有实现了GatewayPlugin接口的Bean
        Map<String, GatewayPlugin> pluginBeans = applicationContext.getBeansOfType(GatewayPlugin.class);
        for (GatewayPlugin plugin : pluginBeans.values()) {
            if (!loadedPlugins.contains(plugin.getName())) {
                plugin.initialize();
                plugins.put(plugin.getName(), plugin);
                loadedPlugins.add(plugin.getName());
                log.info("插件加载成功: {}", plugin.getName());
            }
        }

        // 这里可以添加从插件目录加载外部插件的逻辑
        // loadExternalPlugins();
    }

    /**
     * 加载外部插件
     */
    private void loadExternalPlugins() {
        // 插件目录
        String pluginDir = applicationContext.getEnvironment().getProperty("plugin.directory", "plugins");
        File directory = new File(pluginDir);
        if (!directory.exists() || !directory.isDirectory()) {
            log.warn("插件目录不存在: {}", pluginDir);
            return;
        }

        // 加载插件JAR文件
        File[] jarFiles = directory.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            log.info("插件目录中没有JAR文件: {}", pluginDir);
            return;
        }

        for (File jarFile : jarFiles) {
            try {
                // 创建类加载器
                URLClassLoader classLoader = new URLClassLoader(new java.net.URL[]{jarFile.toURI().toURL()});
                // 这里需要扫描JAR文件中的插件类
                // 简化处理，实际项目中需要更复杂的实现
                log.info("加载外部插件: {}", jarFile.getName());
            } catch (Exception e) {
                log.error("加载外部插件失败: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 启动插件
     */
    public void startPlugin(String pluginName) {
        GatewayPlugin plugin = plugins.get(pluginName);
        if (plugin != null) {
            plugin.start();
            log.info("插件启动成功: {}", pluginName);
        } else {
            log.error("插件不存在: {}", pluginName);
        }
    }

    /**
     * 停止插件
     */
    public void stopPlugin(String pluginName) {
        GatewayPlugin plugin = plugins.get(pluginName);
        if (plugin != null) {
            plugin.stop();
            log.info("插件停止成功: {}", pluginName);
        } else {
            log.error("插件不存在: {}", pluginName);
        }
    }

    /**
     * 卸载插件
     */
    public void unloadPlugin(String pluginName) {
        GatewayPlugin plugin = plugins.remove(pluginName);
        if (plugin != null) {
            plugin.destroy();
            loadedPlugins.remove(pluginName);
            log.info("插件卸载成功: {}", pluginName);
        } else {
            log.error("插件不存在: {}", pluginName);
        }
    }

    /**
     * 获取所有插件
     */
    public Map<String, GatewayPlugin> getPlugins() {
        return plugins;
    }

    /**
     * 获取插件
     */
    public GatewayPlugin getPlugin(String pluginName) {
        return plugins.get(pluginName);
    }

    /**
     * 重新加载插件
     */
    public void reloadPlugins() {
        // 停止并卸载所有插件
        for (String pluginName : new ArrayList<>(plugins.keySet())) {
            unloadPlugin(pluginName);
        }
        // 重新加载插件
        loadPlugins();
        log.info("插件重新加载完成");
    }
}
