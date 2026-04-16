package com.vd.canary.obmp.binlog.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author user
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface MysqlWatcher {
    String hostName();

    String database();

    String table();
}
