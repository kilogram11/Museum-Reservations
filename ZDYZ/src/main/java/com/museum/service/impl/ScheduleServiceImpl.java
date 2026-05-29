package com.museum.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.museum.common.constant.AdminBusinessConstant;
import com.museum.common.constant.BookingConstant;
import com.museum.common.dto.MuseumAddDTO;
import com.museum.common.exception.BusinessException;
import com.museum.entity.Day;
import com.museum.entity.Time;
import com.museum.mapper.DayMapper;
import com.museum.mapper.TimeMapper;
import com.museum.service.ScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private DayMapper dayMapper;

    @Autowired
    private TimeMapper timeMapper;

    @Override
    public void initMuseumSchedule(String museumId, String startDate, String endDate,
                                   List<MuseumAddDTO.TimeTemplate> templates) {
        Date start = DateUtil.parse(startDate);
        Date end = DateUtil.parse(endDate);

        if (start.after(end)) {
            log.error("排期生成失败: 开始日期晚于结束日期 {} > {}", startDate, endDate);
            throw new BusinessException(500, "排期日期无效");
        }

        long dayLimitTotal = templates.stream().mapToInt(MuseumAddDTO.TimeTemplate::getLimit).sum();
        Date current = start;
        while (!current.after(end)) {
            String dayStr = DateUtil.format(current, "yyyy-MM-dd");
            String dayId = AdminBusinessConstant.DAY_ID_PREFIX + IdUtil.fastSimpleUUID();

            Day day = buildMuseumDay(museumId, dayId, dayStr, (int) dayLimitTotal);
            dayMapper.insert(day);

            for (MuseumAddDTO.TimeTemplate template : templates) {
                timeMapper.insert(buildMuseumTime(museumId, dayId, dayStr, template));
            }
            current = DateUtil.offsetDay(current, 1);
        }
        log.info("场馆排期生成完成: {} -> {}, 范围: [{}, {}]", museumId, templates.size(), startDate, endDate);
    }

    @Override
    public void updateMuseumScheduleStatus(String museumId, Integer status) {
        Day day = new Day();
        day.setStatus(status);
        dayMapper.update(day, new UpdateWrapper<Day>().eq("MUSEUM_ID", museumId));

        Time time = new Time();
        time.setStatus(status);
        timeMapper.update(time, new UpdateWrapper<Time>().eq("MUSEUM_ID", museumId));

        log.info("批量更新场馆排期状态: museumId={}, status={}", museumId, status);
    }

    @Override
    public void initActivitySchedule(String activityId, String startDate, String endDate) {
        Date start = DateUtil.parse(startDate);
        Date end = DateUtil.parse(endDate);

        if (start.after(end)) {
            log.error("排期生成失败: 开始日期晚于结束日期 {} > {}", startDate, endDate);
            throw new BusinessException(500, "排期日期无效");
        }

        Date current = start;
        int count = 0;
        while (!current.after(end)) {
            String dayStr = DateUtil.format(current, "yyyy-MM-dd");
            dayMapper.insert(buildActivityDay(activityId, dayStr));
            current = DateUtil.offsetDay(current, 1);
            count++;
        }
        log.info("活动排期自动生成: {}, 天数: {}", activityId, count);
    }

    @Override
    public void updateActivityScheduleStatus(String activityId, Integer status) {
        Day day = new Day();
        day.setStatus(status);
        dayMapper.update(day, new QueryWrapper<Day>().eq("ACTIVITY_ID", activityId));
        log.info("活动排期批量更新: actId={}, status={}", activityId, status);
    }

    private Day buildMuseumDay(String museumId, String dayId, String dayStr, int dayLimitTotal) {
        Day day = new Day();
        day.setId(IdUtil.fastSimpleUUID());
        day.setDayId(dayId);
        day.setDay(dayStr);
        day.setMuseumId(museumId);
        day.setStatus(1);
        day.setDayLimitCnt(dayLimitTotal);
        day.setAddTime(System.currentTimeMillis());
        day.setEditTime(System.currentTimeMillis());
        day.setPid(BookingConstant.DEFAULT_PID);
        return day;
    }

    private Time buildMuseumTime(String museumId, String dayId, String dayStr,
                                 MuseumAddDTO.TimeTemplate template) {
        Time time = new Time();
        time.setId(IdUtil.fastSimpleUUID());
        time.setTimeId(AdminBusinessConstant.TIME_ID_PREFIX + IdUtil.fastSimpleUUID());
        time.setDayId(dayId);
        time.setMuseumId(museumId);
        time.setTimeStart(template.getStart());
        time.setTimeEnd(template.getEnd());
        time.setTimeMark(String.format("%s_%s_%s", museumId, dayStr, template.getStart()));
        time.setLimitCnt(template.getLimit());
        time.setSuccCnt(0);
        time.setStatus(1);
        time.setIsLimit(1);
        time.setAddTime(System.currentTimeMillis());
        time.setEditTime(System.currentTimeMillis());
        time.setPid(BookingConstant.DEFAULT_PID);
        return time;
    }

    private Day buildActivityDay(String activityId, String dayStr) {
        Day day = new Day();
        day.setId(IdUtil.fastSimpleUUID());
        day.setDayId(AdminBusinessConstant.DAY_ID_PREFIX + IdUtil.fastSimpleUUID());
        day.setDay(dayStr);
        day.setMuseumId(null);
        day.setActivityId(activityId);
        day.setStatus(1);
        day.setDayLimitCnt(1000);
        day.setAddTime(System.currentTimeMillis());
        day.setEditTime(System.currentTimeMillis());
        day.setPid(BookingConstant.DEFAULT_PID);
        return day;
    }
}
