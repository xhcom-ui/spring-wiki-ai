package org.wiki.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Method;
import org.wiki.entity.SSLConfig;

import javax.net.ssl.*;


// API网关服务
public class ApiGatewayService {

    private SSLContextManager sslManager;

    public ApiGatewayService() {
        this.sslManager = new SSLContextManager();
    }

    // 初始化SSL配置
    public void initSSLConfigs() {
        System.out.println("========== 初始化SSL配置 ==========\n");

        // 配置1：开发环境 - 信任所有证书
        SSLConfig devConfig = new SSLConfig("开发环境");
        devConfig.setProtocol("TLS");
        devConfig.setTrustAll(true);
        sslManager.registerConfig("dev-service", devConfig);

        // 配置2：测试环境 - 使用自签名证书
        SSLConfig testConfig = new SSLConfig("测试环境");
        testConfig.setProtocol("TLSv1.2");
        testConfig.setTruststorePath("test-truststore.jks");
        testConfig.setTruststorePassword("testpass");
        sslManager.registerConfig("test-service", testConfig);

        // 配置3：生产环境 - 双向认证
        SSLConfig prodConfig = new SSLConfig("生产环境");
        prodConfig.setProtocol("TLSv1.2");
        prodConfig.setKeystorePath("prod-client.p12");
        prodConfig.setKeystorePassword("prodpass");
        prodConfig.setTruststorePath("prod-truststore.jks");
        prodConfig.setTruststorePassword("trustpass");
        sslManager.registerConfig("prod-service", prodConfig);

        System.out.println("\nSSL配置初始化完成\n");
    }

    // 调用HTTPS服务
    public String callService(String serviceName, String url, String method, String body) {
        System.out.println("========== 调用服务 ==========");
        System.out.println("服务名称：" + serviceName);
        System.out.println("请求URL：" + url);
        System.out.println("请求方法：" + method);

        try {
            // 获取SSLContext
            SSLContext sslContext = sslManager.getSSLContext(serviceName);
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();

            // 发起HTTP请求
            HttpRequest request = HttpRequest.of(url).method(Method.valueOf(method.toUpperCase()));

            if (StrUtil.isNotBlank(body)) {
                request.body(body);
            }

            HttpResponse response = request
                    .setSSLSocketFactory(socketFactory)
                    .timeout(30000)
                    .execute();

            System.out.println("\n响应结果：");
            System.out.println("状态码：" + response.getStatus());
            System.out.println("响应体：" + response.body());

            return response.body();

        } catch (Exception e) {
            System.err.println("\n请求失败：" + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // 批量调用测试
    public void batchCallTest() {
        System.out.println("\n========== 批量调用测试 ==========\n");

        // 调用开发环境服务
        System.out.println("===== 测试1：开发环境 =====");
        callService("dev-service", "https://dev-api.example.com/health", "GET", null);

        System.out.println("\n");

        // 调用测试环境服务
        System.out.println("===== 测试2：测试环境 =====");
        callService("test-service", "https://test-api.example.com/user/1", "GET", null);

        System.out.println("\n");

        // 调用生产环境服务
        System.out.println("===== 测试3：生产环境 =====");
        String requestBody = "{\"name\":\"test\",\"type\":\"query\"}";
        callService("prod-service", "https://api.example.com/data", "POST", requestBody);
    }
}

