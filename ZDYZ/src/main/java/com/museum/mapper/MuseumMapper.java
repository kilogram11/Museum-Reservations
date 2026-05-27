package com.museum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.museum.entity.Museum;
import org.apache.ibatis.annotations.Mapper;

/**
 * 博物馆/场馆Mapper接口
 * 使用MyBatis-Plus提供的基础CRUD方法
 */
@Mapper
public interface MuseumMapper extends BaseMapper<Museum> {
    // 场馆配置管理使用MyBatis-Plus默认方法即可
    // 如：insert, updateById, selectById, selectList等

    /**
     * 重写 updateById 以去除 @Param("et")，兼容手写 XML
     */
    int updateById(Museum entity);
}
