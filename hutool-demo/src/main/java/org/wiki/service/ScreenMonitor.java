package org.wiki.service;

import cn.hutool.core.swing.RobotUtil;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


//场景1：定时截屏监控
public class ScreenMonitor {

private final String saveDir;
    private final long interval;
    private volatile boolean running = true;
    
    public ScreenMonitor(String saveDir, long intervalSeconds) {
        this.saveDir = saveDir;
        this.interval = intervalSeconds * 1000;
        
        // 确保目录存在
        new File(saveDir).mkdirs();
    }
    
    /**
     * 开始监控
     */
    public void start() {
        System.out.println("屏幕监控已启动，保存目录: " + saveDir);
        
        while (running) {
            try {
                // 生成文件名
                String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String fileName = "screen_" + timestamp + ".png";
                File file = new File(saveDir, fileName);
                
                // 截屏保存
                RobotUtil.captureScreen(file);
                System.out.println("截图已保存: " + file.getName());
                
                // 等待下次截图
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("屏幕监控已停止");
    }
    
    /**
     * 停止监控
     */
    public void stop() {
        running = false;
    }


    public static void main(String[] args) throws InterruptedException {
        // 每10秒截图一次
        ScreenMonitor monitor = new ScreenMonitor("D:/screenshots", 10);

         // 新线程启动监控
        Thread thread = new Thread(monitor::start);
        thread.start();

         // 运行1分钟后停止
        Thread.sleep(60000);
        monitor.stop();
    }
}