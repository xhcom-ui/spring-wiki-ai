package org.wiki.service;

import cn.hutool.core.collection.RingIndexUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RotatingLogger {
    private final PrintWriter[] writers;
    private final AtomicInteger fileIndex;
    private final int fileCount;

    public RotatingLogger(String basePath, int fileCount) throws IOException {
        this.fileCount = fileCount;
        this.writers = new PrintWriter[fileCount];
        this.fileIndex = new AtomicInteger(0);

        for (int i = 0; i < fileCount; i++) {
            String fileName = basePath + "_" + i + ".log";
            writers[i] = new PrintWriter(new FileWriter(fileName, true));
        }
    }

    /**
     * 写入日志（轮流写入不同文件）
     */
    public synchronized void log(String message) {
        int idx = RingIndexUtil.ringNextInt(fileCount, fileIndex);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        writers[idx].println("[" + timestamp + "] " + message);
        writers[idx].flush();
    }

    /**
     * 批量写入
     */
    public void logBatch(List<String> messages) {
        for (String msg : messages) {
            log(msg);
        }
    }

    /**
     * 关闭所有文件
     */
    public void close() {
        for (PrintWriter writer : writers) {
            if (writer != null) {
                writer.close();
            }
        }
    }
}