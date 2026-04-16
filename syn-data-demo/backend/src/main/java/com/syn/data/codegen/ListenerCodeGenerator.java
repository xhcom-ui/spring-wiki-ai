package com.syn.data.codegen;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * 监听器代码生成器
 * 用于根据用户配置动态生成数据库监听器代码
 */
@Slf4j
public class ListenerCodeGenerator {

    private static final String BASE_PACKAGE = "com.syn.data.generated.listener";
    private static final String BASE_PATH = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator + "com" + File.separator + "syn" + File.separator + "data" + File.separator + "generated" + File.separator + "listener";

    /**
     * 生成PostgreSQL监听器代码
     */
    public static String generatePgListenerCode(Map<String, String> config) {
        String hostName = config.get("hostName");
        String database = config.get("database");
        String table = config.get("table");
        String entityName = config.get("entityName");
        String listenerName = config.get("listenerName");

        if (listenerName == null || listenerName.isEmpty()) {
            listenerName = table.substring(0, 1).toUpperCase() + table.substring(1) + "TableWatcher";
        }

        if (entityName == null || entityName.isEmpty()) {
            entityName = table.substring(0, 1).toUpperCase() + table.substring(1);
        }

        String code = "package " + BASE_PACKAGE + ".postgres;\n\n" +
                "import com.syn.data.wal.annotation.PgWatcher;\n" +
                "import com.syn.data.wal.listener.IPgDataListener;\n\n" +
                "@PgWatcher(hostName = \"" + hostName + "\", database = \"" + database + "\", table = \"" + table + "\")\n" +
                "public class " + listenerName + " implements IPgDataListener<" + entityName + "> {\n" +
                "    @Override\n" +
                "    public void onUpdate(" + entityName + " from, " + entityName + " to) {\n" +
                "        System.out.println(\"" + table + "更新: \" + from + \" -> \" + to);\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public void onInsert(" + entityName + " data) {\n" +
                "        System.out.println(\"" + table + "新增: \" + data);\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public void onDelete(" + entityName + " data) {\n" +
                "        System.out.println(\"" + table + "删除: \" + data);\n" +
                "    }\n" +
                "}\n";

        saveCodeToFile(code, "postgres" + File.separator + listenerName + ".java");
        return code;
    }

    /**
     * 生成MySQL监听器代码
     */
    public static String generateMysqlListenerCode(Map<String, String> config) {
        String hostName = config.get("hostName");
        String database = config.get("database");
        String table = config.get("table");
        String entityName = config.get("entityName");
        String listenerName = config.get("listenerName");

        if (listenerName == null || listenerName.isEmpty()) {
            listenerName = table.substring(0, 1).toUpperCase() + table.substring(1) + "TableWatcher";
        }

        if (entityName == null || entityName.isEmpty()) {
            entityName = table.substring(0, 1).toUpperCase() + table.substring(1);
        }

        String code = "package " + BASE_PACKAGE + ".mysql;\n\n" +
                "import com.syn.data.binlog.annotation.MysqlWatcher;\n" +
                "import com.syn.data.binlog.listener.IMysqlDataListener;\n\n" +
                "@MysqlWatcher(hostName = \"" + hostName + "\", database = \"" + database + "\", table = \"" + table + "\")\n" +
                "public class " + listenerName + " implements IMysqlDataListener<" + entityName + "> {\n" +
                "    @Override\n" +
                "    public void onUpdate(" + entityName + " from, " + entityName + " to) {\n" +
                "        System.out.println(\"" + table + "更新: \" + from + \" -> \" + to);\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public void onInsert(" + entityName + " data) {\n" +
                "        System.out.println(\"" + table + "新增: \" + data);\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public void onDelete(" + entityName + " data) {\n" +
                "        System.out.println(\"" + table + "删除: \" + data);\n" +
                "    }\n" +
                "}\n";

        saveCodeToFile(code, "mysql" + File.separator + listenerName + ".java");
        return code;
    }

    /**
     * 保存代码到文件
     */
    private static void saveCodeToFile(String code, String relativePath) {
        try {
            File directory = new File(BASE_PATH + File.separator + relativePath).getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }

            File file = new File(BASE_PATH + File.separator + relativePath);
            FileWriter writer = new FileWriter(file);
            writer.write(code);
            writer.close();

            log.info("生成代码文件: {}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error("保存代码文件失败", e);
        }
    }
}
