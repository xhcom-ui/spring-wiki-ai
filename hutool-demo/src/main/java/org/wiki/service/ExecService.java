package org.wiki.service;

import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ExecService {

    private static final Charset CHARSET = Charset.forName("GBK");

    public static void main(String[] args) throws Exception {
        // 方式一：单个字符串传入完整命令
        Process process1 = RuntimeUtil.exec("ping 127.0.0.1 -n 3");

        // 方式二：命令和参数分开传入
        Process process2 = RuntimeUtil.exec("ping", "127.0.0.1", "-n", "3");

        // 读取命令输出
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process1.getInputStream(), "GBK")
        );
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        // 等待命令执行完成
        int exitCode = process1.waitFor();
        System.out.println("命令执行完成，退出码：" + exitCode);

        reader.close();


        // 在指定目录下执行命令
        File workDir = new File("D:/workspace/myproject");

        // 在项目目录下执行git命令
        Process process = RuntimeUtil.exec(null, workDir, "git", "status");

        // 等待执行完成
        int exitCode1 = process.waitFor();
        System.out.println("Git命令执行完成，退出码：" + exitCode1);


        // 使用系统默认编码执行命令
        String result1 = RuntimeUtil.execForStr("ipconfig");
        System.out.println("网络配置信息：");
        System.out.println(result1);

        // 指定GBK编码（Windows中文系统）
        String result2 = RuntimeUtil.execForStr(
                Charset.forName("GBK"),
                "systeminfo"
        );
        System.out.println("系统信息：");
        System.out.println(result2);

        // 执行带参数的命令
        String result3 = RuntimeUtil.execForStr("ping", "www.baidu.com", "-n", "2");
        System.out.println("Ping结果：");
        System.out.println(result3);


        // 启动一个持续运行的命令
        Process process3 = RuntimeUtil.exec("ping", "127.0.0.1", "-t");

        System.out.println("Ping命令已启动，3秒后终止...");

        // 等待3秒
        Thread.sleep(3000);

        // 销毁进程
        RuntimeUtil.destroy(process3);
        System.out.println("进程已被终止");

        // 检查进程是否还存活
        boolean isAlive = process3.isAlive();
        System.out.println("进程是否存活：" + isAlive);


        System.out.println("应用程序启动，PID：" + RuntimeUtil.getPid());

        // 注册关闭钩子
        RuntimeUtil.addShutdownHook(() -> {
            System.out.println("JVM即将关闭，执行清理操作...");
            System.out.println("关闭数据库连接...");
            System.out.println("保存缓存数据...");
            System.out.println("清理临时文件...");
            System.out.println("清理操作完成");
        });

        // 可以注册多个钩子
        RuntimeUtil.addShutdownHook(() -> {
            System.out.println("发送应用关闭通知...");
        });

        System.out.println("应用程序运行中...");
        Thread.sleep(2000);
        System.out.println("应用程序正常退出");

        // 程序结束后会自动执行注册的钩子


        System.out.println("========== JVM内存信息 ==========");

        long maxMemory = RuntimeUtil.getMaxMemory();
        long totalMemory = RuntimeUtil.getTotalMemory();
        long freeMemory = RuntimeUtil.getFreeMemory();
        long usableMemory = RuntimeUtil.getUsableMemory();
        int processors = RuntimeUtil.getProcessorCount();

        System.out.println("最大可用内存（-Xmx）：" + formatBytes(maxMemory));
        System.out.println("已申请内存：" + formatBytes(totalMemory));
        System.out.println("剩余内存：" + formatBytes(freeMemory));
        System.out.println("实际可用内存：" + formatBytes(usableMemory));
        System.out.println("处理器核心数：" + processors);

        // 计算内存使用率
        long usedMemory = totalMemory - freeMemory;
        double usagePercent = (double) usedMemory / maxMemory * 100;
        System.out.println("当前内存使用率：" + String.format("%.2f%%", usagePercent));


        // 获取服务器基础信息
        Map<String, String> serverInfo = getServerInfo();
        System.out.println("========== 服务器信息 ==========");
        serverInfo.forEach((key, value) -> {
            System.out.println(key + "：" + value);
        });

        // 检查磁盘使用情况
        System.out.println("\n========== 磁盘使用情况 ==========");
        checkDiskUsage();

        // 检查网络连通性
        System.out.println("\n========== 网络连通性检查 ==========");
        checkNetworkConnectivity("www.baidu.com");
        checkNetworkConnectivity("192.168.1.1");
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }


    public static Map<String, String> getServerInfo() {
        Map<String, String> info = new HashMap<>();

        // 获取计算机名
        String hostname = RuntimeUtil.execForStr(CHARSET, "hostname").trim();
        info.put("主机名", hostname);

        // 获取当前用户
        String username = RuntimeUtil.execForStr(CHARSET, "echo", "%USERNAME%").trim();
        info.put("当前用户", username);

        // 获取系统时间
        String time = RuntimeUtil.execForStr(CHARSET, "time", "/t").trim();
        String date = RuntimeUtil.execForStr(CHARSET, "date", "/t").trim();
        info.put("系统时间", date + " " + time);

        // 获取JVM信息
        info.put("JVM进程ID", String.valueOf(RuntimeUtil.getPid()));
        info.put("CPU核心数", String.valueOf(RuntimeUtil.getProcessorCount()));
        info.put("JVM最大内存", formatMemory(RuntimeUtil.getMaxMemory()));
        info.put("JVM可用内存", formatMemory(RuntimeUtil.getUsableMemory()));

        return info;
    }

    public static void checkDiskUsage() {
        List<String> lines = RuntimeUtil.execForLines(CHARSET, "wmic", "logicaldisk",
                "get", "caption,freespace,size");

        for (String line : lines) {
            if (StrUtil.isNotBlank(line) && !line.contains("Caption")) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 3) {
                    String drive = parts[0];
                    try {
                        long freeSpace = Long.parseLong(parts[1]);
                        long totalSize = Long.parseLong(parts[2]);
                        double usedPercent = (1 - (double) freeSpace / totalSize) * 100;
                        System.out.printf("%s 总容量：%s，可用：%s，使用率：%.1f%%\n",
                                drive, formatMemory(totalSize), formatMemory(freeSpace), usedPercent);
                    } catch (NumberFormatException e) {
                        // 忽略解析错误
                    }
                }
            }
        }
    }

    public static void checkNetworkConnectivity(String host) {
        System.out.println("检测 " + host + " 连通性...");
        String result = RuntimeUtil.execForStr(CHARSET, "ping", host, "-n", "2");

        if (result.contains("TTL=") || result.contains("ttl=")) {
            System.out.println("  结果：网络连通正常");
        } else {
            System.out.println("  结果：网络不可达");
        }
    }

    private static String formatMemory(long bytes) {
        if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
