package org.wiki.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;

// 演示程序
public class UniversalQueryDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("========== 通用数据查询平台演示 ==========\n");
        
        // 模拟数据库连接
        Connection conn = getConnection();
        UniversalQueryService queryService = new UniversalQueryService();
        
        // 场景1：查询用户列表
        System.out.println("===== 场景1：查询用户列表 =====");
        QueryCriteria userCriteria = new QueryCriteria("user")
            .addEqual("status", 1)
            .addLike("username", "zhang")
            .orderBy("create_time", true)
            .page(1, 10);
        
        QueryResult userResult = queryService.query(conn, userCriteria);
        
        System.out.println("\n查询结果：");
        System.out.println("总记录数：" + userResult.getTotalCount());
        System.out.println("当前页：" + userResult.getPageNum() + "/" + userResult.getTotalPages());
        System.out.println("数据列表：");
        
        for (Map<String, Object> row : userResult.getDataList()) {
            System.out.println("  " + row);
        }
        
        // 场景2：查询订单列表
        System.out.println("\n\n===== 场景2：查询订单列表 =====");
        QueryCriteria orderCriteria = new QueryCriteria("orders")
            .addEqual("status", "PAID")
            .addLike("order_no", "2024")
            .orderBy("create_time", true)
            .page(1, 20);
        
        QueryResult orderResult = queryService.query(conn, orderCriteria);
        
        System.out.println("\n查询结果：");
        System.out.println("总记录数：" + orderResult.getTotalCount());
        System.out.println("当前页：" + orderResult.getPageNum() + "/" + orderResult.getTotalPages());
        
        // 场景3：无条件查询所有数据
        System.out.println("\n\n===== 场景3：查询所有商品 =====");
        QueryCriteria productCriteria = new QueryCriteria("product")
            .orderBy("price", false)
            .page(1, 15);
        
        QueryResult productResult = queryService.query(conn, productCriteria);
        
        System.out.println("\n查询结果：");
        System.out.println("总记录数：" + productResult.getTotalCount());
        System.out.println("每页显示：" + productResult.getPageSize() + " 条");
        
        System.out.println("\n\n========== 平台特性总结 ==========");
        System.out.println("1. 动态SQL构建，支持等值和模糊查询");
        System.out.println("2. 自动处理Blob/Clob等特殊字段");
        System.out.println("3. SQL格式化输出，便于调试");
        System.out.println("4. 智能移除ORDER BY优化COUNT查询");
        System.out.println("5. 统一的分页查询支持");
        System.out.println("6. 类型安全的参数绑定");
        
        conn.close();
    }
    
    // 模拟获取数据库连接
    private static Connection getConnection() throws Exception {
        // 实际使用时替换为真实的数据库连接
        String url = "jdbc:mysql://localhost:3306/test?useSSL=false";
        String user = "root";
        String password = "password";
        return DriverManager.getConnection(url, user, password);
    }
}