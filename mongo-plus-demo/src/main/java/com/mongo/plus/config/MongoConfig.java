package com.mongo.plus.config;

import com.gitee.hengboy.mongo.MongoPlusBootProperties;
import com.gitee.hengboy.mongo.factory.MongoDataSourceFactory;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MongoDB配置类
 */
@Configuration
@EnableConfigurationProperties(MongoPlusBootProperties.class)
public class MongoConfig {

    /**
     * 创建MongoClient实例
     */
    @Bean
    public MongoClient mongoClient(MongoPlusBootProperties properties) {
        return MongoDataSourceFactory.build(properties);
    }

    /**
     * 创建MongoDatabase实例
     */
    @Bean
    public MongoDatabase mongoDatabase(MongoClient mongoClient, MongoPlusBootProperties properties) {
        return mongoClient.getDatabase(properties.getDatabase());
    }

}
