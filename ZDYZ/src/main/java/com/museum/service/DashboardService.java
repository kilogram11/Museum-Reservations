package com.museum.service;

import java.util.Map;

/**
 * Dashboard首页统计业务接口
 */
public interface DashboardService {

    /**
     * 获取首页统计数据
     * 
     * @return 统计数据Map
     *         - todayTotal: 今日总预约人数
     *         - todayCheckin: 今日总核销人数
     *         - todayUnchecked: 今日未核销人数
     *         - weekNoShow: 七日爽约人数
     */
    Map<String, Integer> getStatistics();

    /**
     * 获取未来 7 天预约趋势
     * 
     * @return List of Map (date, total, booked)
     */
    java.util.List<java.util.Map<String, Object>> getTrend();

    /**
     * 获取今日核销状态分布
     * 
     * @return Map (checked, unchecked, cancelled)
     */
    java.util.Map<String, Integer> getCheckinStatus();

    /**
     * 获取热门公告 Top 5
     * 
     * @return List of Maps (title, viewCnt)
     */
    java.util.List<java.util.Map<String, Object>> getPopularNews();

    /**
     * 获取爽约人数对比 (近4周)
     * 
     * @return List of Maps (label, value)
     */
    java.util.List<java.util.Map<String, Object>> getNoShowComparison();
}
