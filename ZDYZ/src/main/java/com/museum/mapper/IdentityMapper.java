package com.museum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.museum.entity.Identity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IdentityMapper extends BaseMapper<Identity> {
    /**
     * 更新违约次数(USER_BAN_NUM)，统计最近7天的逾期记录
     */
    void updateBanStatistics();

    /**
     * 执行拉黑（当 USER_BAN_NUM > 5 时）
     */
    void doBan(@Param("startTime") Long startTime, @Param("endTime") Long endTime);

    /**
     * 自动解封（当 BLACK_END_TIME < now 时）
     */
    void autoUnban(@Param("now") Long now);
}
