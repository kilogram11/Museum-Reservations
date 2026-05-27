package com.museum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.museum.entity.Time;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 时间段Mapper接口
 * 用于管理预约时间段
 * 注意：库存操作不在此处实现
 */
@Mapper
public interface TimeMapper extends BaseMapper<Time> {

    /**
     * 根据时间标识查询时间段
     * 
     * @param timeMark 时间段唯一标识
     * @return 时间段信息
     */
    Time selectByTimeMark(@Param("timeMark") String timeMark);

    /**
     * 重写 updateById 以去除 @Param("et")，兼容手写 XML
     */
    int updateById(Time entity);
}
