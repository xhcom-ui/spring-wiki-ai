package com.syn.data;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 数据同步平台应用主类
 */
@SpringBootApplication
@MapperScan("com.syn.data.mapper")
@EnableScheduling
public class SynDataApplication {
    public static void main(String[] args) {
        SpringApplication.run(SynDataApplication.class, args);
    }
}
