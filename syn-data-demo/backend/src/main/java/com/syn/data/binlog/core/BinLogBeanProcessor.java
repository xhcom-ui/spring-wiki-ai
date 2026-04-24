package com.vd.canary.obmp.binlog.core;

import com.vd.canary.obmp.binlog.config.MySqlHostProfile;
import com.vd.canary.obmp.binlog.listener.IMysqlDataListener;
import com.vd.canary.obmp.binlog.thread.BinlogThreadStarter;
import com.vd.canary.service.api.DistributedLocker;
import com.vd.canary.service.redis.RedisUtil;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/*@Component
@Order(99)*/
public class BinLogBeanProcessor implements CommandLineRunner {

    private ApplicationContext context;

    @Resource
    private MySqlHostProfile profile;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public BinLogBeanProcessor(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        Map<String, IMysqlDataListener> beans = context.getBeansOfType(IMysqlDataListener.class);

        Map<String, List<MysqlDataListenerData>> listeners = beans.values().stream()
                .map(MysqlDataListenerData::new)
                .collect(Collectors.groupingBy(MysqlDataListenerData::getHostName));

        listeners.forEach((k, v) -> new BinlogThreadStarter().runThread(profile.getByNameAndThrow(k), v, redisTemplate));
    }
}
