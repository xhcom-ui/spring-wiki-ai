package org.wiki.service;

import cn.hutool.core.swing.RobotUtil;

import java.awt.event.KeyEvent;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//场景2：自动化登录
public class AutoLogin {
/**
     * 自动登录
     */
    public void login(int usernameX, int usernameY, 
                     String username, String password) {
        // 设置较长延迟，确保操作稳定
        RobotUtil.setDelay(300);
        
        // 点击用户名输入框
        RobotUtil.mouseMove(usernameX, usernameY);
        RobotUtil.delay();
        RobotUtil.click();
        RobotUtil.delay();
        
        // 清空输入框
        RobotUtil.keyPressWithCtrl(KeyEvent.VK_A);
        RobotUtil.delay();
        
        // 输入用户名
        RobotUtil.keyPressString(username);
        RobotUtil.delay();
        
        // Tab切换到密码框
        RobotUtil.keyClick(KeyEvent.VK_TAB);
        RobotUtil.delay();
        
        // 输入密码
        RobotUtil.keyPressString(password);
        RobotUtil.delay();
        
        // 回车登录
        RobotUtil.keyClick(KeyEvent.VK_ENTER);
        
        System.out.println("登录操作已完成");
    }
    
    /**
     * 登录前截图记录
     */
    public void loginWithScreenshot(int usernameX, int usernameY,
                                   String username, String password,
                                   String screenshotDir) {
        // 登录前截图
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        RobotUtil.captureScreen(new File(screenshotDir, "before_" + timestamp + ".png"));
        
        // 执行登录
        login(usernameX, usernameY, username, password);
        
        // 等待页面加载
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 登录后截图
        RobotUtil.captureScreen(new File(screenshotDir, "after_" + timestamp + ".png"));
    }


    public static void main(String[] args) {
        AutoLogin autoLogin = new AutoLogin();
        // 假设用户名输入框在(500, 300)位置
        autoLogin.login(500, 300, "admin", "password123");
    }
}