package com.museum.controller.admin;

import com.museum.common.result.Result;
import com.museum.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 统计数据控制器
 */
@RestController
@RequestMapping("/stats")
public class StatsController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * 首页统计数据 (4张卡片)
     */
    @GetMapping("/home")
    public Result home() {
        Map<String, Integer> statistics = dashboardService.getStatistics();
        return Result.success("获取成功", statistics);
    }

    /**
     * 图表1: 未来7天预约趋势
     */
    @GetMapping("/trend")
    public Result trend() {
        List<Map<String, Object>> trend = dashboardService.getTrend();
        return Result.success("获取趋势成功", trend);
    }

    /**
     * 图表2: 今日核销状态占比
     */
    @GetMapping("/checkin")
    public Result checkin() {
        Map<String, Integer> status = dashboardService.getCheckinStatus();
        return Result.success("获取核销状态成功", status);
    }

    /**
     * 图表3: 热门公告 Top 5
     */
    @GetMapping("/popular-news")
    public Result popularNews() {
        List<Map<String, Object>> popular = dashboardService.getPopularNews();
        return Result.success("获取热门公告成功", popular);
    }

    /**
     * 图表4: 爽约人数对比 (近4周)
     */
    @GetMapping("/noshow-comparison")
    public Result noShowComparison() {
        List<Map<String, Object>> comparison = dashboardService.getNoShowComparison();
        return Result.success("获取爽约对比成功", comparison);
    }
}
