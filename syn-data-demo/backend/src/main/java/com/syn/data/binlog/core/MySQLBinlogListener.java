package com.syn.data.binlog.core;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL Binlog监听器
 * 基于原生Socket实现，不依赖第三方库
 */
@Slf4j
public class MySQLBinlogListener {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private long serverId = 1000;
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private final List<MySQLBinlogEventListener> eventListeners = new ArrayList<>();

    public MySQLBinlogListener(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    public void registerEventListener(MySQLBinlogEventListener listener) {
        eventListeners.add(listener);
    }

    public void connect() throws IOException {
        log.info("连接到MySQL服务器: {}:{}", host, port);
        socket = new Socket(host, port);
        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = new DataOutputStream(socket.getOutputStream());

        // 握手
        handshake();
        // 认证
        authenticate();
        // 发送BINLOG_DUMP命令
        startBinlogDump();
        // 开始监听Binlog事件
        listenForEvents();
    }

    private void handshake() throws IOException {
        // 读取服务器握手包
        byte[] handshakePacket = readPacket();
        log.info("收到服务器握手包，长度: {}", handshakePacket.length);
    }

    private void authenticate() throws IOException {
        // 发送认证包
        byte[] authPacket = buildAuthPacket();
        writePacket(authPacket);
        // 读取认证结果
        byte[] resultPacket = readPacket();
        if (resultPacket[0] != 0) {
            throw new IOException("认证失败: " + new String(resultPacket, 1));
        }
        log.info("认证成功");
    }

    private void startBinlogDump() throws IOException {
        // 发送BINLOG_DUMP命令
        byte[] dumpCommand = buildBinlogDumpCommand();
        writePacket(dumpCommand);
        log.info("开始Binlog监听");
    }

    private void listenForEvents() throws IOException {
        while (true) {
            try {
                byte[] eventPacket = readPacket();
                if (eventPacket.length > 0) {
                    // 解析并处理事件
                    processEvent(eventPacket);
                }
            } catch (Exception e) {
                log.error("处理Binlog事件失败", e);
                // 尝试重连
                reconnect();
            }
        }
    }

    private void processEvent(byte[] eventPacket) {
        // 这里可以添加事件解析逻辑
        // 目前简单地将事件传递给监听器
        for (MySQLBinlogEventListener listener : eventListeners) {
            try {
                listener.onEvent(eventPacket);
            } catch (Exception e) {
                log.error("监听器处理事件失败", e);
            }
        }
    }

    private void reconnect() throws IOException {
        log.info("尝试重连到MySQL服务器: {}:{}", host, port);
        socket.close();
        connect();
    }

    private byte[] buildAuthPacket() {
        // 构建认证包
        // 这里只是一个简单的实现，实际需要根据MySQL协议构建正确的认证包
        return new byte[0];
    }

    private byte[] buildBinlogDumpCommand() {
        // 构建BINLOG_DUMP命令
        // 这里只是一个简单的实现，实际需要根据MySQL协议构建正确的命令
        return new byte[0];
    }

    private byte[] readPacket() throws IOException {
        // 读取MySQL协议包
        int length = inputStream.readInt();
        byte[] packet = new byte[length];
        inputStream.readFully(packet);
        return packet;
    }

    private void writePacket(byte[] packet) throws IOException {
        // 写入MySQL协议包
        outputStream.writeInt(packet.length);
        outputStream.write(packet);
        outputStream.flush();
    }

    public void disconnect() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
