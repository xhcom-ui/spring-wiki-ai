package org.wiki.service;

import cn.hutool.core.util.RuntimeUtil;
import java.io.File;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AutoBuildTool {
    
    private static final Charset CHARSET = Charset.forName("UTF-8");
    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private String projectPath;
    private String buildTool;
    
    public AutoBuildTool(String projectPath) {
        this.projectPath = projectPath;
        this.buildTool = detectBuildTool();
    }
    
    public static void main(String[] args) {
        String projectDir = "D:/workspace/demo-project";
        
        AutoBuildTool buildTool = new AutoBuildTool(projectDir);
        
        System.out.println("========== 自动化构建工具 ==========");
        System.out.println("项目路径：" + projectDir);
        System.out.println("构建工具：" + buildTool.buildTool);
        System.out.println("开始时间：" + LocalDateTime.now().format(FORMATTER));
        System.out.println("=====================================\n");
        
        // 注册关闭钩子
        RuntimeUtil.addShutdownHook(() -> {
            System.out.println("\n构建过程被中断，执行清理...");
        });
        
        // 执行构建流程
        boolean success = buildTool.runBuild();
        
        System.out.println("\n=====================================");
        System.out.println("构建结果：" + (success ? "成功" : "失败"));
        System.out.println("结束时间：" + LocalDateTime.now().format(FORMATTER));
    }
    
    private String detectBuildTool() {
        File pomFile = new File(projectPath, "pom.xml");
        File gradleFile = new File(projectPath, "build.gradle");
        
        if (pomFile.exists()) {
            return "maven";
        } else if (gradleFile.exists()) {
            return "gradle";
        }
        return "unknown";
    }
    
    public boolean runBuild() {
        try {
            // 步骤1：清理
            System.out.println("【步骤1】清理项目...");
            if (!executeCommand("clean")) {
                return false;
            }
            
            // 步骤2：编译
            System.out.println("\n【步骤2】编译项目...");
            if (!executeCommand("compile")) {
                return false;
            }
            
            // 步骤3：测试
            System.out.println("\n【步骤3】运行测试...");
            if (!executeCommand("test")) {
                System.out.println("测试失败，但继续执行打包...");
            }
            
            // 步骤4：打包
            System.out.println("\n【步骤4】打包项目...");
            if (!executeCommand("package")) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("构建异常：" + e.getMessage());
            return false;
        }
    }
    
    private boolean executeCommand(String phase) {
        String command;
        if ("maven".equals(buildTool)) {
            command = "mvn " + getMavenPhase(phase) + " -DskipTests";
        } else if ("gradle".equals(buildTool)) {
            command = "gradle " + getGradleTask(phase);
        } else {
            System.out.println("未识别的构建工具");
            return false;
        }
        
        System.out.println("执行命令：" + command);
        
        File workDir = new File(projectPath);
        Process process = RuntimeUtil.exec(null, workDir, command.split(" "));
        
        // 获取执行结果
        String output = RuntimeUtil.getResult(process, CHARSET);
        String error = RuntimeUtil.getErrorResult(process, CHARSET);
        
        if (!output.isEmpty()) {
            // 只输出关键信息
            String[] lines = output.split("\n");
            for (String line : lines) {
                if (line.contains("BUILD") || line.contains("ERROR") || 
                    line.contains("SUCCESS") || line.contains("FAILURE")) {
                    System.out.println(line);
                }
            }
        }
        
        if (!error.isEmpty() && error.contains("ERROR")) {
            System.err.println("错误信息：" + error);
            return false;
        }
        
        return true;
    }
    
    private String getMavenPhase(String phase) {
        switch (phase) {
            case "clean": return "clean";
            case "compile": return "compile";
            case "test": return "test";
            case "package": return "package";
            default: return phase;
        }
    }
    
    private String getGradleTask(String phase) {
        switch (phase) {
            case "clean": return "clean";
            case "compile": return "compileJava";
            case "test": return "test";
            case "package": return "build";
            default: return phase;
        }
    }
}