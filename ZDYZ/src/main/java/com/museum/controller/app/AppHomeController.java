package com.museum.controller.app;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.common.result.Result;
import com.museum.entity.Activity;
import com.museum.entity.Museum;
import com.museum.entity.News;
import com.museum.service.ActivityService;
import com.museum.service.MuseumService;
import com.museum.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 移动端首页/公共信息控制器
 */
@RestController
@RequestMapping("/app/home")
public class AppHomeController {

    @Autowired
    private MuseumService museumService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private NoticeService noticeService;

    /**
     * 首页聚合接口
     */
    @Autowired
    private com.museum.mapper.DayMapper dayMapper;

    @Autowired
    private com.museum.mapper.TimeMapper timeMapper;

    /**
     * 首页聚合接口
     */
    @GetMapping("/index")
    public Result index() {
        Map<String, Object> data = new HashMap<>();

        // 1. 获取轮播图 & 场馆信息
        // 修改：改为升序排列，取第一个创建并启用的场馆 (First Museum)
        QueryWrapper<Museum> museumWrapper = new QueryWrapper<>();
        museumWrapper.eq("MUSEUM_STATUS", 1);
        museumWrapper.orderByAsc("MUSEUM_ADD_TIME"); // 取第一顺位
        Page<Museum> museumPage = museumService.page(new Page<>(1, 1), museumWrapper);

        List<String> banners = new ArrayList<>();
        Map<String, Object> museumInfo = new HashMap<>();
        String museumId = null;

        if (CollUtil.isNotEmpty(museumPage.getRecords())) {
            Museum m = museumPage.getRecords().get(0);
            museumId = m.getMuseumId();

            // 重要：即使 JSON 有错，也要保证基本数据返回
            museumInfo.put("title", m.getMuseumTitle());
            museumInfo.put("openTimeStr", "08:30 - 17:00 (周二至周日)"); // 默认静态文本
            // ✅ 新增：经纬度返回给小程序
            museumInfo.put("longitude", m.getLongitude());
            museumInfo.put("latitude", m.getLatitude());

            // 1.1 优先解析 MUSEUM_PIC 数组作为轮播图
            String picStr = m.getMuseumPic();
            if (StrUtil.isNotBlank(picStr) && JSONUtil.isTypeJSON(picStr)) {
                try {
                    cn.hutool.json.JSONArray pics = JSONUtil.parseArray(picStr);
                    for (Object p : pics) {
                        if (p != null)
                            banners.add(p.toString());
                    }
                } catch (Exception e) {
                }
            }

            // 1.2 解析 museumObj 补充细节 (地址、电话等)
            try {
                String objStr = m.getMuseumObj();
                if (StrUtil.isNotBlank(objStr) && JSONUtil.isTypeJSON(objStr)) {
                    cn.hutool.json.JSONObject obj = JSONUtil.parseObj(objStr);

                    // 如果 picStr 为空，则取封面保底
                    if (CollUtil.isEmpty(banners)) {
                        String cover = obj.getStr("cover");
                        if (StrUtil.isNotBlank(cover))
                            banners.add(cover);
                    }

                    // 优先从 JSON 读，没有则从数据库物理字段读 (Fallback)
                    String addr = obj.getStr("address");
                    if (StrUtil.isBlank(addr))
                        addr = m.getAddress();
                    museumInfo.put("address", addr);

                    museumInfo.put("phone", obj.getStr("phone"));
                    museumInfo.put("desc", obj.getStr("desc"));
                    if (StrUtil.isNotBlank(obj.getStr("openTime"))) {
                        museumInfo.put("openTimeStr", obj.getStr("openTime"));
                    }
                } else {
                    // 如果 JSON 为空，则也需要填充基本地址
                    museumInfo.put("address", m.getAddress());
                }
            } catch (Exception e) {
                museumInfo.put("address", m.getAddress());
            }
        }
        data.put("banners", banners);
        data.put("museumInfo", museumInfo);

        // 2. 今日开放状态 (Today Status)
        Map<String, Object> todayInfo = new HashMap<>();
        String todayStr = cn.hutool.core.date.DateUtil.today();
        todayInfo.put("date", cn.hutool.core.date.DateUtil.format(new java.util.Date(), "MM月dd日"));

        if (museumId != null) {
            // 查询 Day 表看今天是否开馆
            com.museum.entity.Day day = dayMapper
                    .selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.museum.entity.Day>()
                            .eq("MUSEUM_ID", museumId)
                            .eq("DAY", todayStr));

            if (day != null) {
                // 用户要求：不判断 day.getStatus() == 1
                todayInfo.put("status", 1); // 默认视为开放，或者由是否有时间段决定
                todayInfo.put("statusText", "今日开放");

                // 查询 Time 表获取具体起止时间
                List<com.museum.entity.Time> times = timeMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.museum.entity.Time>()
                                .eq("DAY_ID", day.getDayId())
                                .eq("STATUS", 1)
                                .orderByAsc("TIME_START"));

                if (CollUtil.isNotEmpty(times)) {
                    // 逻辑修正：遍历寻找最早开始和最晚结束时间 (使用 LocalTime 比较以防止字符串 "9:00" > "10:00" 的问题)
                    java.time.LocalTime minStart = java.time.LocalTime.MAX;
                    java.time.LocalTime maxEnd = java.time.LocalTime.MIN;
                    String minStartStr = "";
                    String maxEndStr = "";

                    for (com.museum.entity.Time t : times) {
                        try {
                            String sStr = t.getTimeStart();
                            String eStr = t.getTimeEnd();
                            if (StrUtil.isBlank(sStr) || StrUtil.isBlank(eStr))
                                continue;

                            String parseS = sStr.length() == 4 ? "0" + sStr : sStr;
                            String parseE = eStr.length() == 4 ? "0" + eStr : eStr;

                            java.time.LocalTime s = java.time.LocalTime.parse(parseS);
                            java.time.LocalTime e = java.time.LocalTime.parse(parseE);

                            if (s.isBefore(minStart)) {
                                minStart = s;
                                minStartStr = t.getTimeStart();
                            }
                            if (e.isAfter(maxEnd)) {
                                maxEnd = e;
                                maxEndStr = t.getTimeEnd();
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    if (StrUtil.isNotBlank(minStartStr)) {
                        todayInfo.put("hours", minStartStr + " - " + maxEndStr);
                    } else {
                        todayInfo.put("hours", "09:00 - 17:00");
                    }
                } else {
                    todayInfo.put("hours", "09:00 - 17:00"); // 默认
                }
            } else {
                todayInfo.put("status", 0); // 闭馆
                todayInfo.put("statusText", "今日闭馆");
                todayInfo.put("hours", "休息中");
            }
        } else {
            todayInfo.put("status", 0);
            todayInfo.put("hours", "--");
        }
        data.put("today", todayInfo);

        // 3. 最新活动 (保留，取前 3 个)
        Page<Activity> activityPage = activityService.appList(1, 3);
        data.put("activities", activityPage.getRecords());

        // 4. 最新公告 (保留，取前 3 条)
        Page<News> noticePage = noticeService.appList(1, 3);
        data.put("notices", noticePage.getRecords());

        return Result.success("获取成功", data);
    }

    /**
     * 活动列表
     */
    @PostMapping("/activity/list")
    public Result activityList(@RequestBody Map<String, Object> params) {
        Integer page = (Integer) params.get("page");
        Integer limit = (Integer) params.get("limit");
        if (page == null)
            page = 1;
        if (limit == null)
            limit = 10;
        return Result.success("获取成功", activityService.appList(page, limit));
    }

    /**
     * 活动详情
     */
    @GetMapping("/activity/detail")
    public Result activityDetail(@RequestParam String id) {
        Activity activity = activityService.getById(id);
        if (activity == null || activity.getActivityStatus() != 1) {
            return Result.error(500, "活动不存在或未上架");
        }
        return Result.success("获取成功", activity);
    }

    /**
     * 公告列表
     */
    @PostMapping("/notice/list")
    public Result noticeList(@RequestBody Map<String, Object> params) {
        Integer page = (Integer) params.get("page");
        Integer limit = (Integer) params.get("limit");
        if (page == null)
            page = 1;
        if (limit == null)
            limit = 10;
        return Result.success("获取成功", noticeService.appList(page, limit));
    }

    /**
     * 公告详情
     */
    @GetMapping("/notice/detail")
    public Result noticeDetail(@RequestParam String id) {
        News news = noticeService.getById(id);
        if (news == null || news.getNewsStatus() != 1) {
            return Result.error(500, "公告不存在或已下架");
        }
        // 增加阅读量
        noticeService.addRead(id);
        news.setNewsViewCnt(news.getNewsViewCnt() + 1); // 返回最新的
        return Result.success("获取成功", news);
    }
}
