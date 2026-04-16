package org.wiki.service;

import cn.hutool.core.util.RuntimeUtil;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProcessGuardian {
    
    private static final Charset CHARSET = Charset.forName("GBK");
    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private String processName;
    private String startCommand;
    private int maxRestarts;
    private int restartCount;
    private volatile boolean running;
    
    public ProcessGuardian(String processName, String startCommand, int maxRestarts) {
        this.processName = processName;
        this.startCommand = startCommand;
        this.maxRestarts = maxRestarts;
        this.restartCount = 0;
        this.running = true;
    }
    
    public static void main(String[] args) {
        // 示例：守护notepad进程
        ProcessGuardian guardian = new ProcessGuardian(
            "notepad.exe",
            "notepad.exe",
            3
        );
        
        System.out.println("========== 进程守护管理器 ==========");
        System.out.println("守护进程ID：" + RuntimeUtil.getPid());
        System.out.println("目标进程：" + guardian.processName);
        System.out.println("最大重启次数：" + guardian.maxRestarts);
        System.out.println("=====================================\n");
        
        // 注册关闭钩子
        RuntimeUtil.addShutdownHook(() -> {
            System.out.println("\n守护程序关闭...");
            guardian.stop();
        });
        
        // 启动守护
        guardian.startGuard();
    }
    
    public void startGuard() {
        // 首次检查并启动
        if (!isProcessRunning()) {
            System.out.println(getTimestamp() + " 目标进程未运行，首次启动...");
            startProcess();
        }
        
        // 循环监控
        while (running) {
            try {
                Thread.sleep(5000);
                
                if (!isProcessRunning()) {
                    System.out.println(getTimestamp() + " 检测到进程已退出");
                    
                    if (restartCount < maxRestarts) {
                        restartCount++;
                        System.out.println(getTimestamp() + " 尝试重启（第" + restartCount + "次）...");
                        startProcess();
                    } else {
                        System.out.println(getTimestamp() + " 已达最大重启次数，停止守护");
                        running = false;
                    }
                } else {
                    System.out.println(getTimestamp() + " 进程运行正常");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private boolean isProcessRunning() {
        List<String> lines = RuntimeUtil.execForLines(CHARSET, "tasklist");
        
        for (String line : lines) {
            if (line.toLowerCase().contains(processName.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    private void startProcess() {
        try {
            Process process = RuntimeUtil.exec(startCommand);
            Thread.sleep(2000);
            
            if (isProcessRunning()) {
                System.out.println(getTimestamp() + " 进程启动成功");
                // 重启成功后重置计数器（可选）
                // restartCount = 0;
            } else {
                System.out.println(getTimestamp() + " 进程启动失败");
            }
        } catch (Exception e) {
            System.err.println(getTimestamp() + " 启动异常：" + e.getMessage());
        }
    }
    
    public void stop() {
        running = false;
    }
    
    private String getTimestamp() {
        return "[" + LocalDateTime.now().format(FORMATTER) + "]";
    }
}