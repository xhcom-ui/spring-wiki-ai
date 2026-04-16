package org.wiki.service;

import cn.hutool.core.net.SSLUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;
import org.wiki.entity.SSLConfig;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



// SSL上下文管理器
@Service
public class SSLContextManager {

    private Map<String, SSLContext> contextCache = new ConcurrentHashMap<>();
    private Map<String, SSLConfig> configMap = new ConcurrentHashMap<>();

    // 注册SSL配置
    public void registerConfig(String serviceName, SSLConfig config) {
        configMap.put(serviceName, config);
        System.out.println("注册SSL配置：" + serviceName + " -> " + config.getName());
    }

    // 获取SSLContext
    public SSLContext getSSLContext(String serviceName) throws Exception {
        // 从缓存获取
        if (contextCache.containsKey(serviceName)) {
            return contextCache.get(serviceName);
        }

        // 获取配置
        SSLConfig config = configMap.get(serviceName);
        if (config == null) {
            throw new IllegalArgumentException("未找到服务的SSL配置：" + serviceName);
        }

        // 创建SSLContext
        SSLContext sslContext = createSSLContext(config);

        // 缓存
        contextCache.put(serviceName, sslContext);

        return sslContext;
    }

    // 根据配置创建SSLContext
    private SSLContext createSSLContext(SSLConfig config) throws Exception {
        System.out.println("\n创建SSLContext：" + config.getName());
        System.out.println("协议版本：" + config.getProtocol());

        // 场景1：信任所有证书（开发测试环境）
        if (config.isTrustAll()) {
            System.out.println("模式：信任所有证书");
            return SSLUtil.createSSLContext(config.getProtocol());
        }

        // 场景2：双向认证（生产环境）
        if (StrUtil.isNotBlank(config.getKeystorePath()) &&
                StrUtil.isNotBlank(config.getTruststorePath())) {

            System.out.println("模式：双向认证");

            // 加载客户端证书
            KeyManager keyManager = loadKeyManager(
                    config.getKeystorePath(),
                    config.getKeystorePassword()
            );

            // 加载信任证书
            TrustManager trustManager = loadTrustManager(
                    config.getTruststorePath(),
                    config.getTruststorePassword()
            );

            return SSLUtil.createSSLContext(config.getProtocol(), keyManager, trustManager);
        }

        // 场景3：单向认证（只验证服务器）
        if (StrUtil.isNotBlank(config.getTruststorePath())) {
            System.out.println("模式：单向认证");

            TrustManager trustManager = loadTrustManager(
                    config.getTruststorePath(),
                    config.getTruststorePassword()
            );

            return SSLUtil.createSSLContext(config.getProtocol(), null, trustManager);
        }

        // 默认：使用系统默认配置
        System.out.println("模式：系统默认");
        return SSLContext.getDefault();
    }

    // 加载KeyManager
    private KeyManager loadKeyManager(String keystorePath, String password) throws Exception {
        System.out.println("加载客户端证书：" + keystorePath);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, password.toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password.toCharArray());

        KeyManager[] keyManagers = kmf.getKeyManagers();
        System.out.println("客户端证书加载成功");

        return keyManagers.length > 0 ? keyManagers[0] : null;
    }

    // 加载TrustManager
    private TrustManager loadTrustManager(String truststorePath, String password) throws Exception {
        System.out.println("加载信任证书：" + truststorePath);

        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(truststorePath)) {
            trustStore.load(fis, password.toCharArray());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        TrustManager[] trustManagers = tmf.getTrustManagers();
        System.out.println("信任证书加载成功");

        return trustManagers.length > 0 ? trustManagers[0] : null;
    }

    // 清除缓存
    public void clearCache(String serviceName) {
        contextCache.remove(serviceName);
        System.out.println("清除SSL缓存：" + serviceName);
    }

    // 清除所有缓存
    public void clearAllCache() {
        contextCache.clear();
        System.out.println("清除所有SSL缓存");
    }
}

