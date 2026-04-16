package org.wiki.service;


import cn.hutool.core.exceptions.ExceptionUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;

@Slf4j
public class ExceptionService {

    public static void main(String[] args) {
        try {
            // 调用可能抛出多层异常的方法
            executeRemoteSync();
        } catch (Exception e) {
            // 直接找到最底层的IOException
            Throwable rootCause = ExceptionUtil.getCausedBy(e, IOException.class);
            if (rootCause != null) {
                log.error("数据同步失败，根本原因：{}", rootCause.getMessage());
                // 处理IO异常（比如重试）
            }
            if (ExceptionUtil.isCausedBy(e, IOException.class)) {
                log.error("IO异常：{}", e.getMessage());
            } else if (ExceptionUtil.isCausedBy(e, SQLException.class)) {
                log.error("数据库异常：{}", e.getMessage());
            }

            String stackTrace = ExceptionUtil.stacktraceToString(e);
            log.error("{}失败：{}", "taskName", stackTrace);
        }


        try {
             Files.readString(Paths.get("config.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e); // 手动包装成运行时异常
        }

//        ExceptionUtil.wrapRuntime(() ->
//                Files.readString(Paths.get("config.properties"))
//        );


    }



    /**
     * 模拟远程调用/IO操作（会抛出多层包装异常）
     */
    private static void executeRemoteSync() throws Exception {
        try {
            // 模拟IO异常（如：网络请求、文件读写）
            throw new SocketTimeoutException("读取数据超时：3000ms");
        } catch (IOException ex) {
            // 包装成业务异常抛出（多层异常嵌套）
            throw new RuntimeException("远程数据同步失败", ex);
        }
    }

    /**
     * 重试机制（IO超时常用）
     */
    private void retrySyncData() {
        try {
            Thread.sleep(1000);
            executeRemoteSync();
            log.info("重试数据同步成功");
        } catch (Exception retryEx) {
            log.error("重试同步数据仍然失败", retryEx);
        }
    }
}
