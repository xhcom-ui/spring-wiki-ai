package org.wiki.config;

import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//你的判断完全正确。thread.isVirtual()报错，根本原因在于 JDK 版本，而非 Spring Boot 3.2.0。
@Configuration
public class VirtualThreadConfig {

//    @Bean
//    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
//        // 让 Tomcat 使用虚拟线程执行器
//        return handler -> handler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
//    }
}