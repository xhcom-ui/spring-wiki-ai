package com.example.health.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/leak")
    public String testConnectionLeak() throws SQLException {
        // 获取连接但不关闭，模拟连接泄漏
        Connection connection = dataSource.getConnection();
        System.out.println("获取连接: " + connection);
        // 这里故意不关闭连接，模拟连接泄漏
        return "测试连接泄漏";
    }

    @GetMapping("/normal")
    public String testNormalConnection() throws SQLException {
        // 获取连接
        Connection connection = dataSource.getConnection();
        System.out.println("获取连接: " + connection);
        try {
            // 使用连接
            Statement statement = connection.createStatement();
            try {
                ResultSet resultSet = statement.executeQuery("SELECT 1");
                try {
                    if (resultSet.next()) {
                        System.out.println("查询结果: " + resultSet.getInt(1));
                    }
                } finally {
                    resultSet.close();
                }
            } finally {
                statement.close();
            }
        } finally {
            // 关闭连接
            connection.close();
            System.out.println("关闭连接: " + connection);
        }
        return "测试正常连接使用";
    }
}
