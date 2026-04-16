package com.syn.data.wal.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * PostgreSQL WAL监听注解
 * 用于指定要监听的主机、数据库和表
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface PgWatcher {
    String hostName();

    String database();

    String table();
}
