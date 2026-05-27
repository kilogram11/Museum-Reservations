package com.museum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.museum.entity.News;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公告/资讯Mapper接口
 * 使用MyBatis-Plus提供的基础CRUD方法
 */
@Mapper
public interface NewsMapper extends BaseMapper<News> {
    // 公告管理使用MyBatis-Plus默认方法即可
    // 如：insert, updateById, selectById, selectList等
}
