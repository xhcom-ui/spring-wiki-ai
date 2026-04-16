package com.syn.data.service;

import com.syn.data.entity.DataSourceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * MySQL Binlog监听器
 * 不依赖第三方库，使用原生Socket实现
 */
@Slf4j
@Service
public class MySQLBinlogListener {

    private BlockingQueue<byte[]> binlogEvents = new LinkedBlockingQueue<>(10000);
    private volatile boolean running = false;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    /**
     * 启动Binlog监听
     */
    public void start(DataSourceConfig dataSource) throws Exception {
        if (running) {
            log.warn("Binlog listener is already running");
            return;
        }

        running = true;

        // 连接到MySQL服务器
        socket = new Socket(dataSource.getHost(), dataSource.getPort());
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();

        // 发送握手请求
        sendHandshake(dataSource.getUsername(), dataSource.getPassword());

        // 发送Binlog监听请求
        sendBinlogRequest();

        // 启动事件处理线程
        new Thread(this::processEvents).start();

        log.info("MySQL binlog listener started for {}", dataSource.getName());
    }

    /**
     * 停止Binlog监听
     */
    public void stop() {
        running = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            log.error("Error closing socket", e);
        }
        log.info("MySQL binlog listener stopped");
    }

    /**
     * 发送握手请求
     */
    private void sendHandshake(String username, String password) throws Exception {
        // 读取服务器握手包
        byte[] handshakePacket = readPacket();
        log.debug("Received handshake packet: {}", bytesToHex(handshakePacket));

        // 发送认证包
        byte[] authPacket = buildAuthPacket(username, password, handshakePacket);
        sendPacket(authPacket);

        // 读取认证结果
        byte[] resultPacket = readPacket();
        if (resultPacket[0] == 0) {
            log.info("Authentication successful");
        } else {
            throw new Exception("Authentication failed: " + new String(resultPacket, 1, resultPacket.length - 1, StandardCharsets.UTF_8));
        }
    }

    /**
     * 发送Binlog监听请求
     */
    private void sendBinlogRequest() throws Exception {
        // 构建COM_BINLOG_DUMP命令
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put((byte) 0x12); // COM_BINLOG_DUMP
        buffer.putInt(4); // binlog pos
        buffer.putShort((short) 0); // flags
        buffer.putInt(12345); // server id
        buffer.put("mysql-bin.000001".getBytes(StandardCharsets.UTF_8)); // binlog file
        buffer.put((byte) 0); // null terminator

        byte[] packet = new byte[buffer.position()];
        buffer.flip();
        buffer.get(packet);

        sendPacket(packet);
        log.info("Binlog dump request sent");
    }

    /**
     * 处理Binlog事件
     */
    private void processEvents() {
        while (running) {
            try {
                byte[] eventPacket = readPacket();
                if (eventPacket != null) {
                    binlogEvents.put(eventPacket);
                    // 简单处理：打印事件类型
                    if (eventPacket.length > 13) {
                        int eventType = eventPacket[13] & 0xFF;
                        log.debug("Received binlog event type: {}", eventType);
                    }
                }
            } catch (Exception e) {
                if (running) {
                    log.error("Error processing binlog events", e);
                }
            }
        }
    }

    /**
     * 读取MySQL数据包
     */
    private byte[] readPacket() throws Exception {
        // 读取数据包长度
        byte[] lengthBuffer = new byte[3];
        int read = inputStream.read(lengthBuffer);
        if (read != 3) {
            throw new IOException("Failed to read packet length");
        }

        int length = ((lengthBuffer[0] & 0xFF) | ((lengthBuffer[1] & 0xFF) << 8) | ((lengthBuffer[2] & 0xFF) << 16));
        if (length == 0) {
            return null;
        }

        // 读取序列号
        byte[] sequenceBuffer = new byte[1];
        read = inputStream.read(sequenceBuffer);
        if (read != 1) {
            throw new IOException("Failed to read packet sequence");
        }

        // 读取数据
        byte[] data = new byte[length];
        read = inputStream.read(data);
        if (read != length) {
            throw new IOException("Failed to read packet data");
        }

        return data;
    }

    /**
     * 发送MySQL数据包
     */
    private void sendPacket(byte[] data) throws Exception {
        int length = data.length;
        byte[] header = new byte[4];
        header[0] = (byte) (length & 0xFF);
        header[1] = (byte) ((length >> 8) & 0xFF);
        header[2] = (byte) ((length >> 16) & 0xFF);
        header[3] = 0; // sequence number

        outputStream.write(header);
        outputStream.write(data);
        outputStream.flush();
    }

    /**
     * 构建认证包
     */
    private byte[] buildAuthPacket(String username, String password, byte[] handshakePacket) {
        // 这里只是一个简单实现，实际认证过程更复杂
        // 需要根据handshakePacket中的salt计算密码哈希
        // 这里简化处理，直接发送明文密码
        StringBuilder sb = new StringBuilder();
        sb.append((char) username.length()).append(username);
        sb.append((char) 0); // null terminator
        sb.append((char) password.length()).append(password);
        sb.append((char) 0); // null terminator

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    /**
     * 获取Binlog事件队列
     */
    public BlockingQueue<byte[]> getBinlogEvents() {
        return binlogEvents;
    }

    /**
     * 检查监听器是否运行
     */
    public boolean isRunning() {
        return running;
    }
}
