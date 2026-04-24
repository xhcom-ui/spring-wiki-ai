package com.syn.data.service;

import com.syn.data.entity.DataSourceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MySQL Binlog监听器
 * 不依赖第三方库，使用原生Socket实现
 */
@Slf4j
@Service
public class MySQLBinlogListener {

    private final BlockingQueue<byte[]> binlogEvents = new LinkedBlockingQueue<>(10000);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Thread eventProcessingThread;
    private ScheduledExecutorService executorService;
    private long lastHeartbeatTime;
    private static final int HEARTBEAT_INTERVAL = 30000; // 30秒
    private static final int SOCKET_TIMEOUT = 60000; // 60秒

    /**
     * 启动Binlog监听
     */
    public void start(DataSourceConfig dataSource) throws Exception {
        if (running.get()) {
            log.warn("Binlog listener is already running");
            return;
        }

        running.set(true);
        connected.set(false);
        lastHeartbeatTime = System.currentTimeMillis();

        // 初始化线程池
        executorService = Executors.newScheduledThreadPool(2);

        // 启动心跳线程
        executorService.scheduleAtFixedRate(this::sendHeartbeat, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);

        // 连接到MySQL服务器
        connectToMySQL(dataSource);

        // 启动事件处理线程
        eventProcessingThread = new Thread(this::processEvents, "Binlog-Event-Processor");
        eventProcessingThread.setDaemon(true);
        eventProcessingThread.start();

        log.info("MySQL binlog listener started for {}", dataSource.getName());
    }

    /**
     * 停止Binlog监听
     */
    public void stop() {
        running.set(false);
        connected.set(false);
        
        // 停止线程池
        if (executorService != null) {
            executorService.shutdownNow();
        }
        
        // 中断事件处理线程
        if (eventProcessingThread != null && eventProcessingThread.isAlive()) {
            eventProcessingThread.interrupt();
        }
        
        // 关闭socket
        closeResources();
        
        log.info("MySQL binlog listener stopped");
    }

    /**
     * 连接到MySQL服务器
     */
    private void connectToMySQL(DataSourceConfig dataSource) throws Exception {
        int retries = 3;
        while (retries > 0 && running.get()) {
            try {
                socket = new Socket(dataSource.getHost(), dataSource.getPort());
                socket.setSoTimeout(SOCKET_TIMEOUT);
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                
                // 发送握手请求
                sendHandshake(dataSource.getUsername(), dataSource.getPassword());
                
                // 发送Binlog监听请求
                sendBinlogRequest(dataSource.getBinlogFile(), dataSource.getBinlogPosition());
                
                connected.set(true);
                log.info("Connected to MySQL server: {}:{}", dataSource.getHost(), dataSource.getPort());
                return;
            } catch (Exception e) {
                retries--;
                log.error("Failed to connect to MySQL server ({} retries left): {}", retries, e.getMessage());
                if (retries > 0) {
                    Thread.sleep(5000); // 5秒后重试
                } else {
                    throw e;
                }
            }
        }
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
    private void sendBinlogRequest(String binlogFile, long binlogPosition) throws Exception {
        // 构建COM_BINLOG_DUMP命令
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put((byte) 0x12); // COM_BINLOG_DUMP
        buffer.putInt((int) binlogPosition); // binlog pos
        buffer.putShort((short) 0); // flags
        buffer.putInt(12345); // server id
        buffer.put((binlogFile != null ? binlogFile : "mysql-bin.000001").getBytes(StandardCharsets.UTF_8)); // binlog file
        buffer.put((byte) 0); // null terminator

        byte[] packet = new byte[buffer.position()];
        buffer.flip();
        buffer.get(packet);

        sendPacket(packet);
        log.info("Binlog dump request sent for file: {}, position: {}", binlogFile, binlogPosition);
    }

    /**
     * 处理Binlog事件
     */
    private void processEvents() {
        while (running.get()) {
            try {
                byte[] eventPacket = readPacket();
                if (eventPacket != null) {
                    // 检查队列容量，避免内存溢出
                    if (binlogEvents.size() >= 9500) {
                        log.warn("Binlog event queue is almost full, size: {}", binlogEvents.size());
                        // 可以选择丢弃 oldest events 或者暂停处理
                    }
                    
                    binlogEvents.put(eventPacket);
                    lastHeartbeatTime = System.currentTimeMillis();
                    
                    // 简单处理：打印事件类型
                    if (eventPacket.length > 13) {
                        int eventType = eventPacket[13] & 0xFF;
                        log.debug("Received binlog event type: {}", eventType);
                    }
                }
            } catch (SocketTimeoutException e) {
                // 超时异常，可能是心跳超时
                if (System.currentTimeMillis() - lastHeartbeatTime > HEARTBEAT_INTERVAL * 2) {
                    log.warn("Heartbeat timeout, reconnecting...");
                    // 尝试重连
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Event processing thread interrupted");
                break;
            } catch (Exception e) {
                if (running.get()) {
                    log.error("Error processing binlog events", e);
                    // 尝试重连
                    try {
                        Thread.sleep(5000);
                        // 这里可以添加重连逻辑
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    /**
     * 发送心跳包
     */
    private void sendHeartbeat() {
        if (!running.get() || !connected.get()) {
            return;
        }
        
        try {
            // 发送COM_PING命令
            byte[] pingCommand = {0x01};
            sendPacket(pingCommand);
            log.debug("Heartbeat sent");
        } catch (Exception e) {
            log.error("Error sending heartbeat", e);
            // 尝试重连
            connected.set(false);
        }
    }

    /**
     * 读取MySQL数据包
     */
    private byte[] readPacket() throws Exception {
        if (inputStream == null) {
            throw new IOException("Input stream is null");
        }
        
        // 读取数据包长度
        byte[] lengthBuffer = new byte[3];
        int read = inputStream.read(lengthBuffer);
        if (read != 3) {
            throw new IOException("Failed to read packet length, read: " + read);
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
        int totalRead = 0;
        while (totalRead < length) {
            int bytesRead = inputStream.read(data, totalRead, length - totalRead);
            if (bytesRead == -1) {
                throw new IOException("End of stream reached while reading packet");
            }
            totalRead += bytesRead;
        }

        return data;
    }

    /**
     * 发送MySQL数据包
     */
    private void sendPacket(byte[] data) throws Exception {
        if (outputStream == null) {
            throw new IOException("Output stream is null");
        }
        
        int length = data.length;
        byte[] header = new byte[4];
        header[0] = (byte) (length & 0xFF);
        header[1] = (byte) ((length >> 8) & 0xFF);
        header[2] = (byte) ((length >> 16) & 0xFF);
        header[3] = 0; // sequence number

        synchronized (outputStream) {
            outputStream.write(header);
            outputStream.write(data);
            outputStream.flush();
        }
    }

    /**
     * 构建认证包
     */
    private byte[] buildAuthPacket(String username, String password, byte[] handshakePacket) throws NoSuchAlgorithmException {
        // 提取salt
        byte[] salt = new byte[20];
        System.arraycopy(handshakePacket, 32, salt, 0, 20);
        
        // 计算密码哈希
        byte[] passwordHash = computePasswordHash(password, salt);
        
        // 构建认证包
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put((byte) 0x03); // client capability flags
        buffer.putInt(1 << 15); // max packet size
        buffer.put((byte) 8); // charset
        buffer.put(new byte[23]); // reserved
        buffer.put(username.getBytes(StandardCharsets.UTF_8));
        buffer.put((byte) 0); // null terminator
        buffer.put((byte) passwordHash.length);
        buffer.put(passwordHash);
        
        byte[] packet = new byte[buffer.position()];
        buffer.flip();
        buffer.get(packet);
        
        return packet;
    }

    /**
     * 计算密码哈希
     */
    private byte[] computePasswordHash(String password, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(password.getBytes(StandardCharsets.UTF_8));
        md5.update(salt);
        return md5.digest();
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
     * 关闭资源
     */
    private void closeResources() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            log.error("Error closing input stream", e);
        }
        
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            log.error("Error closing output stream", e);
        }
        
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            log.error("Error closing socket", e);
        }
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
        return running.get();
    }

    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * 获取队列大小
     */
    public int getQueueSize() {
        return binlogEvents.size();
    }

    /**
     * 清空队列
     */
    public void clearQueue() {
        binlogEvents.clear();
        log.info("Binlog event queue cleared");
    }
}
