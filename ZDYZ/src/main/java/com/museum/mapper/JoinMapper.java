package com.museum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.museum.entity.Join;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 预约记录Mapper接口
 * 核心业务：预约统计、核销管理、爽约检测
 */
@Mapper
public interface JoinMapper extends BaseMapper<Join> {

    Integer countTodayTotal();

    Integer countTodayCheckin();

    Integer countTodayUnchecked();

    Integer countRecentNoShow(@Param("days") int days);

    List<Join> findExpiredJoin();

    // IPage<JoinVO> searchByKeyword(IPage<JoinVO> page, @Param("keyword") String
    // keyword);
    // 恢复为 List<Join> searchByKeyword(@Param("keyword") String keyword);
    // 从 Step 113 的 view_file 看，它有一个 searchByKeyword。
    List<Join> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 根据日期范围统计爽约人数 (JOIN_STATUS=1 AND JOIN_IS_CHECKIN=3)
     * 
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate   结束日期 (yyyy-MM-dd)
     * @return 统计数量
     */
    Integer countNoShowByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 自动更新逾期状态
     * 
     * @param editTime 更新时间
     */
    void updateOverdueStatus(Long editTime);

    /**
     * 查询用户的所有预约记录（关联Time表获取时间段）
     */
    List<Join> selectUserJoinList(@Param("userId") String userId);
}
