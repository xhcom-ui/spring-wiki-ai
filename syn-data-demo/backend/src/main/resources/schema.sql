-- 数据同步平台数据库设计

-- 创建数据库
CREATE DATABASE IF NOT EXISTS syn_data DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE syn_data;

-- 1. 数据源配置表
CREATE TABLE IF NOT EXISTS data_source_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '数据源ID',
    name VARCHAR(255) NOT NULL COMMENT '数据源名称',
    type VARCHAR(50) NOT NULL COMMENT '数据源类型: mysql, postgresql',
    host VARCHAR(255) NOT NULL COMMENT '主机地址',
    port INT NOT NULL COMMENT '端口',
    database_name VARCHAR(255) NOT NULL COMMENT '数据库名称',
    username VARCHAR(255) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    max_connections INT DEFAULT 10 COMMENT '最大连接数',
    min_connections INT DEFAULT 5 COMMENT '最小连接数',
    connection_timeout INT DEFAULT 30 COMMENT '连接超时时间（秒）',
    test_sql VARCHAR(255) DEFAULT 'SELECT 1' COMMENT '连接测试语句',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    project VARCHAR(255) DEFAULT 'default' COMMENT '所属项目',
    description VARCHAR(500) COMMENT '描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源配置表';

-- 2. 同步任务配置表
CREATE TABLE IF NOT EXISTS sync_task_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    name VARCHAR(255) NOT NULL COMMENT '任务名称',
    description VARCHAR(500) COMMENT '任务描述',
    project VARCHAR(255) DEFAULT 'default' COMMENT '所属项目',
    owner VARCHAR(255) COMMENT '负责人',
    priority VARCHAR(20) DEFAULT 'medium' COMMENT '优先级: low, medium, high',
    tags VARCHAR(500) COMMENT '标签（逗号分隔）',
    source_id BIGINT NOT NULL COMMENT '数据源ID',
    es_cluster VARCHAR(255) DEFAULT 'default' COMMENT 'ES集群',
    target_index VARCHAR(255) NOT NULL COMMENT '目标ES索引',
    `sql` TEXT NOT NULL COMMENT 'SQL语句',
    field_mapping TEXT COMMENT '字段映射（JSON格式）',
    sync_mode VARCHAR(50) DEFAULT 'full' COMMENT '同步模式: full, incremental, full+incremental',
    execution_strategy VARCHAR(50) DEFAULT 'immediate' COMMENT '执行策略: immediate, scheduled, triggered',
    incremental_field VARCHAR(255) COMMENT '增量字段',
    incremental_strategy VARCHAR(50) DEFAULT 'timestamp' COMMENT '增量策略: timestamp, autoIncrement, timeWindow, businessLogic',
    enable_checkpoint TINYINT DEFAULT 1 COMMENT '是否启用断点续传',
    schedule_type VARCHAR(50) DEFAULT 'manual' COMMENT '调度类型: manual, cron',
    cron_expression VARCHAR(255) COMMENT 'CRON表达式',
    batch_size INT DEFAULT 1000 COMMENT '批次大小',
    concurrent_threads INT DEFAULT 1 COMMENT '并发线程数',
    connection_timeout INT DEFAULT 30 COMMENT '连接超时（秒）',
    query_timeout INT DEFAULT 60 COMMENT '查询超时（秒）',
    write_timeout INT DEFAULT 60 COMMENT '写入超时（秒）',
    retry_count INT DEFAULT 3 COMMENT '失败重试次数',
    retry_interval INT DEFAULT 5 COMMENT '重试间隔（秒）',
    skip_error_records TINYINT DEFAULT 0 COMMENT '是否跳过错误记录',
    error_handling_strategy VARCHAR(50) DEFAULT 'continue' COMMENT '错误处理策略: continue, pause, terminate',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    last_sync_time DATETIME COMMENT '上次同步时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (source_id) REFERENCES data_source_config(id) ON DELETE CASCADE,
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='同步任务配置表';

-- 3. 同步任务日志表
CREATE TABLE IF NOT EXISTS sync_task_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    task_name VARCHAR(255) NOT NULL COMMENT '任务名称',
    status VARCHAR(50) NOT NULL COMMENT '执行状态: success, failed, running',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    duration BIGINT COMMENT '执行耗时（毫秒）',
    total_count BIGINT COMMENT '处理数据量',
    success_count BIGINT COMMENT '成功数量',
    failed_count BIGINT COMMENT '失败数量',
    error_message TEXT COMMENT '错误信息',
    execution_params TEXT COMMENT '执行参数（JSON格式）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (task_id) REFERENCES sync_task_config(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='同步任务日志表';

-- 4. 数据质量问题表
CREATE TABLE IF NOT EXISTS data_quality_issue (
    id VARCHAR(36) PRIMARY KEY COMMENT '问题ID（UUID）',
    task_id BIGINT COMMENT '关联任务ID',
    task_name VARCHAR(255) COMMENT '任务名称',
    type VARCHAR(50) NOT NULL COMMENT '问题类型: count, quality, format, business',
    severity VARCHAR(20) DEFAULT 'medium' COMMENT '严重程度: low, medium, high',
    message VARCHAR(500) NOT NULL COMMENT '问题描述',
    field VARCHAR(255) COMMENT '涉及字段',
    source_value TEXT COMMENT '源端值',
    target_value TEXT COMMENT '目标端值',
    status VARCHAR(50) DEFAULT 'detected' COMMENT '状态: detected, processed, ignored',
    fix_strategy VARCHAR(50) COMMENT '修复策略',
    detected_time DATETIME NOT NULL COMMENT '检测时间',
    processed_time DATETIME COMMENT '处理时间',
    processed_by VARCHAR(255) COMMENT '处理人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (task_id) REFERENCES sync_task_config(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据质量问题表';

-- 5. 告警配置表
CREATE TABLE IF NOT EXISTS alert_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '告警配置ID',
    name VARCHAR(255) NOT NULL COMMENT '告警名称',
    type VARCHAR(50) NOT NULL COMMENT '告警类型: failure, timeout, dataAnomaly, delay, quality',
    level VARCHAR(20) DEFAULT 'medium' COMMENT '告警级别: low, medium, high',
    threshold VARCHAR(255) COMMENT '告警阈值',
    notification_methods VARCHAR(500) COMMENT '通知方式（逗号分隔）: email, wechat, dingtalk, sms, phone',
    recipients VARCHAR(1000) COMMENT '接收人',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    description VARCHAR(500) COMMENT '描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警配置表';

-- 6. 告警日志表
CREATE TABLE IF NOT EXISTS alert_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '告警日志ID',
    alert_id BIGINT COMMENT '告警配置ID',
    alert_name VARCHAR(255) NOT NULL COMMENT '告警名称',
    type VARCHAR(50) NOT NULL COMMENT '告警类型',
    level VARCHAR(20) NOT NULL COMMENT '告警级别',
    message TEXT NOT NULL COMMENT '告警消息',
    task_id BIGINT COMMENT '关联任务ID',
    task_name VARCHAR(255) COMMENT '任务名称',
    status VARCHAR(50) DEFAULT 'pending' COMMENT '状态: pending, sent, failed',
    send_time DATETIME COMMENT '发送时间',
    recipients VARCHAR(1000) COMMENT '接收人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (alert_id) REFERENCES alert_config(id) ON DELETE SET NULL,
    FOREIGN KEY (task_id) REFERENCES sync_task_config(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警日志表';

-- 7. 系统配置表
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    key_name VARCHAR(255) NOT NULL COMMENT '配置键',
    value VARCHAR(1000) COMMENT '配置值',
    description VARCHAR(500) COMMENT '描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_key_name (key_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 8. 用户表（预留）
CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(255) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    name VARCHAR(255) COMMENT '真实姓名',
    email VARCHAR(255) COMMENT '邮箱',
    phone VARCHAR(50) COMMENT '电话',
    role VARCHAR(50) DEFAULT 'user' COMMENT '角色: admin, user',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 初始化系统配置
INSERT INTO system_config (key_name, value, description) VALUES
('system.name', '数据同步平台', '系统名称'),
('system.version', '1.0.0', '系统版本'),
('system.description', '可视化、可配置的数据同步平台', '系统描述'),
('elasticsearch.hosts', 'localhost:9200', 'Elasticsearch主机地址'),
('elasticsearch.username', '', 'Elasticsearch用户名'),
('elasticsearch.password', '', 'Elasticsearch密码'),
('xxl.job.admin.addresses', 'http://localhost:9999/xxl-job-admin', 'XXL-Job管理地址'),
('xxl.job.access.token', '', 'XXL-Job访问令牌'),
('xxl.job.executor.appname', 'syn-data-executor', 'XXL-Job执行器名称'),
('xxl.job.executor.ip', '', 'XXL-Job执行器IP'),
('xxl.job.executor.port', '9999', 'XXL-Job执行器端口')
ON DUPLICATE KEY UPDATE value = VALUES(value), updated_at = CURRENT_TIMESTAMP;

-- 初始化告警配置
INSERT INTO alert_config (name, type, level, threshold, notification_methods, recipients, enabled, description) VALUES
('任务失败告警', 'failure', 'high', '1', 'email,wechat', 'admin@example.com', 1, '同步任务执行失败时告警'),
('任务超时告警', 'timeout', 'medium', '3600', 'email', 'admin@example.com', 1, '同步任务执行超时（超过1小时）时告警'),
('数据量异常告警', 'dataAnomaly', 'medium', '10', 'email', 'admin@example.com', 1, '数据量与预期相差超过10%时告警'),
('同步延迟告警', 'delay', 'low', '300', 'email', 'admin@example.com', 1, '同步延迟超过5分钟时告警'),
('数据质量告警', 'quality', 'high', '1', 'email,wechat', 'admin@example.com', 1, '数据质量校验失败时告警')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 初始化测试数据
-- 1. 测试数据源
INSERT INTO data_source_config (name, type, host, port, database_name, username, password, status, description) VALUES
('测试MySQL', 'mysql', 'localhost', 3306, 'test', 'root', '123456', 1, '测试用MySQL数据源'),
('测试PostgreSQL', 'postgresql', 'localhost', 5432, 'test', 'postgres', '123456', 1, '测试用PostgreSQL数据源')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 2. 测试同步任务
INSERT INTO sync_task_config (name, description, source_id, target_index, `sql`, sync_mode, execution_strategy, status) VALUES
('用户数据同步', '同步用户表数据到ES', 1, 'users', 'SELECT * FROM users', 'full', 'immediate', 1),
('订单数据同步', '同步订单表数据到ES', 1, 'orders', 'SELECT * FROM orders WHERE update_time > ${last_sync_time}', 'incremental', 'scheduled', 1),
('商品数据同步', '同步商品表数据到ES', 2, 'products', 'SELECT * FROM products', 'full', 'immediate', 1)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 3. 测试用户
INSERT INTO user (username, password, name, email, role, status) VALUES
('admin', 'admin123', '管理员', 'admin@example.com', 'admin', 1),
('user', 'user123', '普通用户', 'user@example.com', 'user', 1)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;
