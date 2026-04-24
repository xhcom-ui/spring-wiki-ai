package com.syn.data.binlog.core;

import com.syn.data.binlog.annotation.MysqlWatcher;
import com.syn.data.binlog.config.MySqlHost;
import com.syn.data.binlog.listener.IMysqlDataListener;
import com.syn.data.binlog.thread.BinlogThreadStarter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MySQL Binlog监听框架核心服务
 * 负责管理所有MySQL Binlog监听任务
 */
@Slf4j
@Component
public class MySQLBinlogListenerFramework implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Resource
    private MySqlHostProfile hostProfile;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 初始化时启动所有Binlog监听
     */
    @PostConstruct
    public void init() {
        log.info("初始化MySQL Binlog监听框架...");
        loadAndStartListeners();
    }

    /**
     * 加载并启动所有MySQL Binlog监听
     */
    private void loadAndStartListeners() {
        try {
            // 获取所有实现了IMysqlDataListener接口的Bean
            Map<String, IMysqlDataListener> beans = applicationContext.getBeansOfType(IMysqlDataListener.class);

            // 按主机名分组
            Map<String, List<MysqlDataListenerData>> listeners = beans.values().stream()
                    .map(MysqlDataListenerData::new)
                    .collect(Collectors.groupingBy(MysqlDataListenerData::getHostName));

            // 为每个主机启动Binlog监听线程
            listeners.forEach((hostName, dataList) -> {
                try {
                    MySqlHost host = hostProfile.getByNameAndThrow(hostName);
                    new BinlogThreadStarter().runThread(host, dataList, redisTemplate);
                    log.info("启动主机 {} 的Binlog监听", hostName);
                } catch (Exception e) {
                    log.error("启动主机 {} 的Binlog监听失败", hostName, e);
                }
            });
        } catch (Exception e) {
            log.error("加载MySQL Binlog监听失败", e);
        }
    }

    /**
     * 销毁时停止所有Binlog监听
     */
    @PreDestroy
    public void destroy() {
        log.info("停止MySQL Binlog监听框架...");
        // 这里可以添加停止所有Binlog监听的逻辑
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
