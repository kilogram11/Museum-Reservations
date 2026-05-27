package com.museum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.museum.entity.Log;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志Mapper接口
 * 使用MyBatis-Plus提供的基础CRUD方法
 */
@Mapper
public interface LogMapper extends BaseMapper<Log> {
    // 日志记录使用MyBatis-Plus默认方法即可
    // 如：insert, selectList等
}
