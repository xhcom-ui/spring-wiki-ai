package com.syn.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syn.data.entity.SyncTaskLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 同步任务执行记录Mapper接口
 */
@Mapper
public interface SyncTaskLogMapper extends BaseMapper<SyncTaskLog> {

}
