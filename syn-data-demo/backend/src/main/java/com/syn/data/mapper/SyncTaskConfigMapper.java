package com.syn.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syn.data.entity.SyncTaskConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 同步任务配置Mapper接口
 */
@Mapper
public interface SyncTaskConfigMapper extends BaseMapper<SyncTaskConfig> {

}
