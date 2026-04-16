package com.syn.data.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.syn.data.entity.DataSourceConfig;

import java.util.List;

/**
 * 数据源配置服务接口
 */
public interface DataSourceConfigService extends IService<DataSourceConfig> {

    /**
     * 测试数据源连接
     */
    boolean testConnection(Long id);

    /**
     * 获取所有启用的数据源
     */
    List<DataSourceConfig> getActiveDataSources();

}
