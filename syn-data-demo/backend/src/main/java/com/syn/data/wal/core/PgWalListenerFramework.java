package com.syn.data.wal.core;

import com.syn.data.wal.annotation.PgWatcher;
import com.syn.data.wal.config.PgHost;
import com.syn.data.wal.config.PgHostProfile;
import com.syn.data.wal.listener.IPgDataListener;
import com.syn.data.wal.thread.PgWalThreadStarter;
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
 * PostgreSQL WAL监听框架核心服务
 * 负责管理所有PostgreSQL WAL监听任务
 */
@Slf4j
@Component
public class PgWalListenerFramework implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Resource
    private PgHostProfile hostProfile;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 初始化时启动所有WAL监听
     */
    @PostConstruct
    public void init() {
        log.info("初始化PostgreSQL WAL监听框架...");
        loadAndStartListeners();
    }

    /**
     * 加载并启动所有PostgreSQL WAL监听
     */
    private void loadAndStartListeners() {
        try {
            // 获取所有实现了IPgDataListener接口的Bean
            Map<String, IPgDataListener> beans = applicationContext.getBeansOfType(IPgDataListener.class);

            // 按主机名分组
            Map<String, List<PgDataListenerData>> listeners = beans.values().stream()
                    .map(PgDataListenerData::new)
                    .collect(Collectors.groupingBy(PgDataListenerData::getHostName));

            // 为每个主机启动WAL监听线程
            listeners.forEach((hostName, dataList) -> {
                try {
                    PgHost host = hostProfile.getByNameAndThrow(hostName);
                    new PgWalThreadStarter().runThread(host, dataList, redisTemplate);
                    log.info("启动主机 {} 的WAL监听", hostName);
                } catch (Exception e) {
                    log.error("启动主机 {} 的WAL监听失败", hostName, e);
                }
            });
        } catch (Exception e) {
            log.error("加载PostgreSQL WAL监听失败", e);
        }
    }

    /**
     * 销毁时停止所有WAL监听
     */
    @PreDestroy
    public void destroy() {
        log.info("停止PostgreSQL WAL监听框架...");
        // 这里可以添加停止所有WAL监听的逻辑
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
