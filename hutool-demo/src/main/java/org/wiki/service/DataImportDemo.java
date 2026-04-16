package org.wiki.service;

import org.wiki.entity.ImportConfig;
import org.wiki.entity.ImportResult;

import java.sql.Connection;
import java.sql.DriverManager;

// 演示程序
public class DataImportDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("========== 通用数据导入系统 ==========\n");
        
        // 创建数据库连接
        Connection conn = getConnection();
        
        // 创建导入服务
        DataImportService importService = new DataImportService(conn);
        
        // 场景1：导入用户数据
        System.out.println("===== 场景1：导入用户数据 =====\n");
        
        ImportConfig userConfig = new ImportConfig("user");
        userConfig.addColumn("username")
                  .addColumn("age")
                  .addColumn("email")
                  .addColumn("phone")
                  .addColumn("status");
        userConfig.setBatchSize(500);
        userConfig.setSkipError(true);
        
        ImportResult userResult = importService.importFromCsv("users.csv", userConfig);
        importService.printResult(userResult);
        
        // 场景2：导入商品数据
        System.out.println("\n\n===== 场景2：导入商品数据 =====\n");
        
        ImportConfig productConfig = new ImportConfig("product");
        productConfig.addColumn("name")
                     .addColumn("category")
                     .addColumn("price")
                     .addColumn("stock")
                     .addColumn("status");
        productConfig.setBatchSize(1000);
        
        ImportResult productResult = importService.importFromCsv("products.csv", productConfig);
        importService.printResult(productResult);
        
        // 场景3：导入订单数据
        System.out.println("\n\n===== 场景3：导入订单数据 =====\n");
        
        ImportConfig orderConfig = new ImportConfig("orders");
        orderConfig.addColumn("order_no")
                   .addColumn("user_id")
                   .addColumn("product_id")
                   .addColumn("quantity")
                   .addColumn("total_amount")
                   .addColumn("status");
        orderConfig.setBatchSize(800);
        
        ImportResult orderResult = importService.importFromCsv("orders.csv", orderConfig);
        importService.printResult(orderResult);
        
        // 关闭连接
        conn.close();
        
        System.out.println("\n\n========== 系统特性总结 ==========");
        System.out.println("1. 批量导入支持");
        System.out.println("   - 自动分批处理，避免内存溢出");
        System.out.println("   - 可配置批次大小，优化性能");
        System.out.println("   - 支持大数据量导入");
        
        System.out.println("\n2. 事务控制");
        System.out.println("   - 整个导入过程在一个事务中");
        System.out.println("   - 失败自动回滚，保证数据一致性");
        System.out.println("   - 支持部分成功模式（跳过错误行）");
        
        System.out.println("\n3. 错误处理");
        System.out.println("   - 记录每个错误的详细信息");
        System.out.println("   - 可配置是否跳过错误继续导入");
        System.out.println("   - 提供完整的导入结果报告");
        
        System.out.println("\n4. 主键管理");
        System.out.println("   - 自动获取生成的自增主键");
        System.out.println("   - 返回所有插入记录的ID");
        System.out.println("   - 便于后续关联操作");
        
        System.out.println("\n5. 灵活配置");
        System.out.println("   - 支持动态指定表名和列名");
        System.out.println("   - 自动数据类型转换");
        System.out.println("   - 支持NULL值处理");
    }
    
    private static Connection getConnection() throws Exception {
        String url = "jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC";
        String user = "root";
        String password = "password";
        return DriverManager.getConnection(url, user, password);
    }
}