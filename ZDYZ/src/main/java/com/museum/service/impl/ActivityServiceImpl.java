package com.museum.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.common.constant.AdminBusinessConstant;
import com.museum.common.constant.BookingConstant;
import com.museum.common.dto.ActivityAddDTO;
import com.museum.common.exception.BusinessException;
import com.museum.entity.Activity;
import com.museum.entity.Day;
import com.museum.mapper.ActivityMapper;
import com.museum.mapper.DayMapper;
import com.museum.service.ActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityServiceImpl.class);

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private DayMapper dayMapper;

    @Autowired
    private com.museum.service.MessageService messageService;

    @Override
    public Page<Activity> dataList(String keyword, Integer page, Integer limit) {
        Page<Activity> pageParam = new Page<>(page, limit);
        QueryWrapper<Activity> wrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like("ACTIVITY_TITLE", keyword);
        }
        wrapper.orderByDesc("ACTIVITY_ADD_TIME");
        return activityMapper.selectPage(pageParam, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addActivity(ActivityAddDTO dto) {
        // 1. 校验
        if (StrUtil.isBlank(dto.getActivityTitle()))
            throw new BusinessException(500, "活动标题不能为空");
        if (StrUtil.isBlank(dto.getStartDate()) || StrUtil.isBlank(dto.getEndDate())) {
            throw new BusinessException(500, "活动时间范围不能为空");
        }

        // 日期逻辑校验
        Date start = DateUtil.parse(dto.getStartDate());
        Date end = DateUtil.parse(dto.getEndDate());
        if (start.after(end)) {
            throw new BusinessException(500, "开始日期不能晚于结束日期");
        }

        long now = System.currentTimeMillis();
        String activityId = AdminBusinessConstant.ACTIVITY_ID_PREFIX + IdUtil.fastSimpleUUID();

        // 2. 提取图片
        List<String> imgList = new ArrayList<>();
        if (CollUtil.isNotEmpty(dto.getContent())) {
            for (ActivityAddDTO.ContentItem item : dto.getContent()) {
                // 前端 type 为 'image'，后端原逻辑只匹配 'img'
                if (("img".equals(item.getType()) || "image".equals(item.getType()))
                        && StrUtil.isNotBlank(item.getVal())) {
                    imgList.add(item.getVal());
                }
            }
        }

        // 3. 插入 Activity
        Activity activity = new Activity();
        activity.setId(IdUtil.fastSimpleUUID());
        activity.setActivityId(activityId);
        activity.setActivityTitle(dto.getActivityTitle());
        activity.setAdminId(dto.getAdminId());
        activity.setActivityPic(JSONUtil.toJsonStr(imgList));

        // 组装 activityObj (存储排期参数)
        Map<String, Object> activityExtraInfo = new HashMap<>();
        activityExtraInfo.put("startDate", dto.getStartDate());
        activityExtraInfo.put("endDate", dto.getEndDate());
        activityExtraInfo.put("content", dto.getContent());
        activity.setActivityObj(JSONUtil.toJsonStr(activityExtraInfo));

        // 状态处理：默认为0
        Integer status = dto.getStatus() != null ? dto.getStatus() : 0;
        activity.setActivityStatus(status);
        activity.setActivityAddTime(now);
        activity.setActivityEditTime(now);
        activity.setPid(BookingConstant.DEFAULT_PID);
        activityMapper.insert(activity);

        // 4. 判断状态决定是否生成排期
        if (status == 1) {
            initActivitySchedule(activityId, dto.getStartDate(), dto.getEndDate());
            // 发送广播消息
            try {
                messageService.createMessage(AdminBusinessConstant.MESSAGE_RECEIVER_ALL,
                        AdminBusinessConstant.MESSAGE_TEMPLATE_ACTIVITY_NEW, dto.getActivityTitle());
            } catch (Exception e) {
                logger.error("发送新活动通知失败", e);
            }
        }
    }

    private void initActivitySchedule(String activityId, String startDate, String endDate) {
        Date start = DateUtil.parse(startDate);
        Date end = DateUtil.parse(endDate);

        if (start.after(end)) {
            logger.error("排期生成失败: 开始日期晚于结束日期 {} > {}", startDate, endDate);
            throw new BusinessException(500, "排期日期无效");
        }

        Date current = start;
        int count = 0;
        while (!current.after(end)) {
            String dayStr = DateUtil.format(current, "yyyy-MM-dd");
            String dayId = AdminBusinessConstant.DAY_ID_PREFIX + IdUtil.fastSimpleUUID();

            Day day = new Day();
            day.setId(IdUtil.fastSimpleUUID());
            day.setDayId(dayId);
            day.setDay(dayStr);
            day.setMuseumId(null);
            day.setActivityId(activityId);
            day.setStatus(1); // 默认启用
            day.setDayLimitCnt(1000);
            day.setAddTime(System.currentTimeMillis());
            day.setEditTime(System.currentTimeMillis());
            day.setPid(BookingConstant.DEFAULT_PID);
            dayMapper.insert(day);

            current = DateUtil.offsetDay(current, 1);
            count++;
        }
        logger.info("活动排期自动生成: {}, 天数: {}", activityId, count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editActivity(com.museum.common.dto.ActivityEditDTO dto) {
        if (StrUtil.isBlank(dto.getId()))
            throw new BusinessException(500, "ID不能为空");

        Activity activity = activityMapper.selectById(dto.getId());
        if (activity == null)
            throw new BusinessException(500, "活动不存在");

        // 1. 更新基本字段
        activity.setActivityTitle(dto.getActivityTitle());
        if (dto.getStatus() != null) {
            activity.setActivityStatus(dto.getStatus());
        }

        // --- 逻辑增强: 检查排期变更 ---
        boolean scheduleChanged = false;
        JSONObject oldObj = JSONUtil.parseObj(activity.getActivityObj());

        if (StrUtil.isNotBlank(dto.getStartDate()) && !dto.getStartDate().equals(oldObj.getStr("startDate"))) {
            scheduleChanged = true;
        }
        if (StrUtil.isNotBlank(dto.getEndDate()) && !dto.getEndDate().equals(oldObj.getStr("endDate"))) {
            scheduleChanged = true;
        }

        if (scheduleChanged) {
            logger.info("检测到活动时间变更，正在重置排期... ActivityId={}", activity.getActivityId());
            // (1) 删除旧排期
            dayMapper.delete(new QueryWrapper<Day>().eq("ACTIVITY_ID", activity.getActivityId()));

            // (2) 重新生成 (状态判断)
            Integer effectiveStatus = dto.getStatus() != null ? dto.getStatus() : activity.getActivityStatus();
            if (effectiveStatus == 1) {
                String start = StrUtil.isNotBlank(dto.getStartDate()) ? dto.getStartDate() : oldObj.getStr("startDate");
                String end = StrUtil.isNotBlank(dto.getEndDate()) ? dto.getEndDate() : oldObj.getStr("endDate");
                initActivitySchedule(activity.getActivityId(), start, end);
            }
        } else {
            // 仅状态变更
            if (dto.getStatus() != null) {
                if (dto.getStatus() == 1) {
                    updateScheduleStatus(activity.getActivityId(), 1);
                } else {
                    updateScheduleStatus(activity.getActivityId(), 0);
                }
            }
        }
        // -----------------------------
        activity.setActivityEditTime(System.currentTimeMillis());

        // 2. 提取图片与更新 activityObj
        List<String> imgList = new ArrayList<>();
        if (CollUtil.isNotEmpty(dto.getContent())) {
            for (ActivityAddDTO.ContentItem item : dto.getContent()) {
                if (("img".equals(item.getType()) || "image".equals(item.getType()))
                        && StrUtil.isNotBlank(item.getVal())) {
                    imgList.add(item.getVal());
                }
            }
        }
        activity.setActivityPic(JSONUtil.toJsonStr(imgList));

        Map<String, Object> activityExtraInfo = new HashMap<>();
        activityExtraInfo.put("startDate", dto.getStartDate());
        activityExtraInfo.put("endDate", dto.getEndDate());
        activityExtraInfo.put("content", dto.getContent());
        activity.setActivityObj(JSONUtil.toJsonStr(activityExtraInfo));

        activityMapper.updateById(activity);
        logger.info("编辑活动: {}", activity.getActivityId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delActivity(String id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null)
            return;

        // 1. 删除关联的 Day 排期
        QueryWrapper<Day> wrapper = new QueryWrapper<>();
        wrapper.eq("ACTIVITY_ID", activity.getActivityId());
        dayMapper.delete(wrapper);

        // 2. 删除 Activity
        activityMapper.deleteById(id);
        logger.info("删除活动及排期: {}", activity.getActivityId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void status(String id, Integer status) {
        // 尝试按 ID 查询
        Activity activity = activityMapper.selectById(id);
        // 如果按 ID 查不到，尝试按 ACTIVITY_ID (业务ID) 查询
        if (activity == null) {
            QueryWrapper<Activity> wrapper = new QueryWrapper<>();
            wrapper.eq("ACTIVITY_ID", id);
            activity = activityMapper.selectOne(wrapper);
        }

        if (activity == null) {
            logger.warn("修改状态失败: 未找到活动 ID={}", id);
            return;
        }

        // 1. 更新主表状态
        activity.setActivityStatus(status);
        activity.setActivityEditTime(System.currentTimeMillis());
        activityMapper.updateById(activity);

        // 2. 处理排期联动
        if (status == 1) {
            long count = dayMapper.selectCount(new QueryWrapper<Day>().eq("ACTIVITY_ID", activity.getActivityId()));
            if (count == 0) {
                // 无排期则补全
                if (StrUtil.isNotBlank(activity.getActivityObj())) {
                    JSONObject obj = JSONUtil.parseObj(activity.getActivityObj());
                    String start = obj.getStr("startDate");
                    String end = obj.getStr("endDate");
                    if (StrUtil.isAllNotBlank(start, end)) {
                        initActivitySchedule(activity.getActivityId(), start, end);
                    }
                }
            } else {
                // 有排期则启用
                updateScheduleStatus(activity.getActivityId(), 1);
            }
            
            // 发送广播消息 (仅当从未发送过? 这里简化为每次上架都发，或者假设管理员知道)
            try {
                messageService.createMessage(AdminBusinessConstant.MESSAGE_RECEIVER_ALL,
                        AdminBusinessConstant.MESSAGE_TEMPLATE_ACTIVITY_NEW, activity.getActivityTitle());
            } catch (Exception e) {
                logger.error("发送新活动通知失败", e);
            }
        } else {
            // 禁用逻辑：逻辑禁用
            updateScheduleStatus(activity.getActivityId(), 0);
        }
    }

    /**
     * 批量更新排期状态
     */
    private void updateScheduleStatus(String activityId, Integer status) {
        Day day = new Day();
        day.setStatus(status);
        dayMapper.update(day, new QueryWrapper<Day>().eq("ACTIVITY_ID", activityId));
        logger.info("活动排期批量更新: actId={}, status={}", activityId, status);
    }

    @Override
    public Page<Activity> appList(Integer page, Integer limit) {
        Page<Activity> pageParam = new Page<>(page, limit);
        QueryWrapper<Activity> wrapper = new QueryWrapper<>();
        wrapper.eq("ACTIVITY_STATUS", 1);
        wrapper.orderByDesc("ACTIVITY_ADD_TIME");
        return activityMapper.selectPage(pageParam, wrapper);
    }
}
