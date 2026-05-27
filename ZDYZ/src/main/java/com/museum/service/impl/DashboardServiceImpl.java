package com.museum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.museum.entity.Day;
import com.museum.entity.Join;
import com.museum.entity.News;
import com.museum.mapper.DayMapper;
import com.museum.mapper.JoinMapper;
import com.museum.mapper.NewsMapper;
import com.museum.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private JoinMapper joinMapper;

    @Autowired
    private DayMapper dayMapper;

    @Autowired
    private NewsMapper newsMapper;

    @Override
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> map = new HashMap<>();
        // 1. 今日预约总人数
        Integer todayTotalBooking = joinMapper.countTodayTotal();
        map.put("todayTotalBooking", todayTotalBooking != null ? todayTotalBooking : 0);
        // 2. 今日核销人数
        Integer todayChecked = joinMapper.countTodayCheckin();
        map.put("todayChecked", todayChecked != null ? todayChecked : 0);
        // 3. 今日未核销人数
        Integer todayUnchecked = joinMapper.countTodayUnchecked();
        map.put("todayUnchecked", todayUnchecked != null ? todayUnchecked : 0);
        // 4. 近七日爽约人数
        Integer weekNoShow = joinMapper.countRecentNoShow(7);
        map.put("weekNoShow", weekNoShow != null ? weekNoShow : 0);
        return map;
    }

    @Override
    public List<Map<String, Object>> getTrend() {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Map<String, Object> dayMap = new HashMap<>();
            dayMap.put("date", dateStr);

            QueryWrapper<Day> dayQuery = new QueryWrapper<>();
            dayQuery.eq("DAY", dateStr);
            List<Day> days = dayMapper.selectList(dayQuery);
            int totalCapacity = days.stream().mapToInt(Day::getDayLimitCnt).sum();

            QueryWrapper<Join> joinQuery = new QueryWrapper<>();
            joinQuery.eq("JOIN_MEET_DAY", dateStr);
            joinQuery.in("JOIN_STATUS", 1, 3, 4); // 有效预约
            Long bookedCount = joinMapper.selectCount(joinQuery);

            dayMap.put("total", totalCapacity);
            dayMap.put("booked", bookedCount);
            result.add(dayMap);
        }
        return result;
    }

    @Override
    public Map<String, Integer> getCheckinStatus() {
        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Map<String, Integer> statusMap = new HashMap<>();

        // 已核销: JOIN_STATUS=1 AND JOIN_IS_CHECKIN=1 (或3? 视业务定义，通常1是正常核销)
        // 修正逻辑：查询 JOIN_IS_CHECKIN = 1 (核销)
        // 注意：joinMapper.countTodayCheckin 使用的是 JOIN_STATUS=1 AND JOIN_IS_CHECKIN=1
        // 这里保持一致
        QueryWrapper<Join> qChecked = new QueryWrapper<>();
        qChecked.eq("JOIN_MEET_DAY", todayStr)
                .eq("JOIN_STATUS", 1)
                .eq("JOIN_IS_CHECKIN", 1);
        statusMap.put("checked", Math.toIntExact(joinMapper.selectCount(qChecked)));

        // 未核销: JOIN_STATUS=1 AND JOIN_IS_CHECKIN=0
        QueryWrapper<Join> qUnchecked = new QueryWrapper<>();
        qUnchecked.eq("JOIN_MEET_DAY", todayStr)
                .eq("JOIN_STATUS", 1)
                .eq("JOIN_IS_CHECKIN", 0);
        statusMap.put("unchecked", Math.toIntExact(joinMapper.selectCount(qUnchecked)));

        // 已取消: JOIN_STATUS=2 (取消的记录通常不做 checkin 状态判断)
        QueryWrapper<Join> qCancelled = new QueryWrapper<>();
        qCancelled.eq("JOIN_MEET_DAY", todayStr)
                .eq("JOIN_STATUS", 2);
        statusMap.put("cancelled", Math.toIntExact(joinMapper.selectCount(qCancelled)));

        return statusMap;
    }

    @Override
    public List<Map<String, Object>> getPopularNews() {
        QueryWrapper<News> query = new QueryWrapper<>();
        query.orderByDesc("NEWS_VIEW_CNT").last("LIMIT 5");
        query.select("NEWS_TITLE", "NEWS_VIEW_CNT");
        List<News> newsList = newsMapper.selectList(query);

        List<Map<String, Object>> result = new ArrayList<>();
        for (News news : newsList) {
            Map<String, Object> map = new HashMap<>();
            map.put("title", news.getNewsTitle());
            map.put("viewCnt", news.getNewsViewCnt() != null ? news.getNewsViewCnt() : 0);
            result.add(map);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getNoShowComparison() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("MM.dd");
        List<Map<String, Object>> result = new ArrayList<>();

        // Generate data for the last 4 weeks
        for (int i = 0; i < 4; i++) {
            // week 0: recent (today-6 to today)
            // week 1: (today-13 to today-7)
            LocalDate endDate = today.minusDays(i * 7);
            LocalDate startDate = endDate.minusDays(6);

            String endStr = endDate.format(fmt);
            String startStr = startDate.format(fmt);

            Integer count = joinMapper.countNoShowByDateRange(startStr, endStr);

            Map<String, Object> map = new HashMap<>();
            String weekLabel = (i == 0) ? "本周" : (i == 1) ? "上周" : (i == 2) ? "两周前" : "三周前";
            String dateRange = startDate.format(labelFmt) + "-" + endDate.format(labelFmt);

            map.put("label", weekLabel + "\n" + dateRange);
            map.put("value", count != null ? count : 0);

            // Add to the beginning of the list to show time ascending (older -> newer) on
            // the chart?
            // Or descending? Usually chart left-to-right is time ascending.
            // Let's add such that the most recent is at the END of the list (right side of
            // chart),
            // or we add to the list and then reverse it.
            // Loop goes 0 (Recent) -> 3 (Oldest).
            // So if we add to list, it will be [Recent, Prior, Prior2, Prior3].
            // We want the chart to show [Prior3, Prior2, Prior, Recent] (Time ->).
            result.add(0, map);
        }

        return result;
    }
}
