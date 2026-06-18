package com.museum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.museum.entity.Day;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 日期排期Mapper接口
 * 用于管理每日预约排期
 */
@Mapper
public interface DayMapper extends BaseMapper<Day> {

    /**
     * 查询指定场馆的指定日期排期
     * 
     * @param museumId 场馆ID
     * @param date     日期（格式：yyyy-MM-dd）
     * @return 排期信息
     */
    Day selectByMuseumAndDate(@Param("museumId") String museumId, @Param("date") String date);

    int updateStatusByMuseumId(@Param("museumId") String museumId, @Param("status") Integer status);

    /**
     * 重写 updateById 以去除 @Param("et")，兼容手写 XML
     */
    int updateById(Day entity);
}
