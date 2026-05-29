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
import com.museum.service.ScheduleService;
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
    private ScheduleService scheduleService;

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
        validateAddActivity(dto);
        long now = System.currentTimeMillis();
        String activityId = AdminBusinessConstant.ACTIVITY_ID_PREFIX + IdUtil.fastSimpleUUID();
        Integer status = dto.getStatus() != null ? dto.getStatus() : 0;

        Activity activity = buildActivityEntity(dto, activityId, now, status);
        activityMapper.insert(activity);

        publishActivityIfEnabled(activityId, dto, status);
    }

    private void validateAddActivity(ActivityAddDTO dto) {
        if (StrUtil.isBlank(dto.getActivityTitle()))
            throw new BusinessException(500, "活动标题不能为空");
        if (StrUtil.isBlank(dto.getStartDate()) || StrUtil.isBlank(dto.getEndDate())) {
            throw new BusinessException(500, "活动时间范围不能为空");
        }
        validateDateRange(dto.getStartDate(), dto.getEndDate());
    }

    private void validateDateRange(String startDate, String endDate) {
        Date start = DateUtil.parse(startDate);
        Date end = DateUtil.parse(endDate);
        if (start.after(end)) {
            throw new BusinessException(500, "开始日期不能晚于结束日期");
        }
    }

    private Activity buildActivityEntity(ActivityAddDTO dto, String activityId, long now, Integer status) {
        Activity activity = new Activity();
        activity.setId(IdUtil.fastSimpleUUID());
        activity.setActivityId(activityId);
        activity.setActivityTitle(dto.getActivityTitle());
        activity.setAdminId(dto.getAdminId());
        activity.setActivityPic(JSONUtil.toJsonStr(extractActivityImages(dto.getContent())));
        activity.setActivityObj(JSONUtil.toJsonStr(buildActivityExtraInfo(dto)));
        activity.setActivityStatus(status);
        activity.setActivityAddTime(now);
        activity.setActivityEditTime(now);
        activity.setPid(BookingConstant.DEFAULT_PID);
        return activity;
    }

    private List<String> extractActivityImages(List<ActivityAddDTO.ContentItem> content) {
        List<String> imageList = new ArrayList<>();
        if (CollUtil.isEmpty(content)) {
            return imageList;
        }
        for (ActivityAddDTO.ContentItem item : content) {
            if (("img".equals(item.getType()) || "image".equals(item.getType()))
                    && StrUtil.isNotBlank(item.getVal())) {
                imageList.add(item.getVal());
            }
        }
        return imageList;
    }

    private Map<String, Object> buildActivityExtraInfo(ActivityAddDTO dto) {
        Map<String, Object> activityExtraInfo = new HashMap<>();
        activityExtraInfo.put("startDate", dto.getStartDate());
        activityExtraInfo.put("endDate", dto.getEndDate());
        activityExtraInfo.put("content", dto.getContent());
        return activityExtraInfo;
    }

    private void publishActivityIfEnabled(String activityId, ActivityAddDTO dto, Integer status) {
        if (status == 1) {
            scheduleService.initActivitySchedule(activityId, dto.getStartDate(), dto.getEndDate());
            sendActivityNewMessage(dto.getActivityTitle());
        }
    }

    private void sendActivityNewMessage(String activityTitle) {
        try {
            messageService.createMessage(AdminBusinessConstant.MESSAGE_RECEIVER_ALL,
                    AdminBusinessConstant.MESSAGE_TEMPLATE_ACTIVITY_NEW, activityTitle);
        } catch (Exception e) {
            logger.error("发送新活动通知失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editActivity(com.museum.common.dto.ActivityEditDTO dto) {
        Activity activity = loadActivityForEdit(dto.getId());
        JSONObject oldObj = JSONUtil.parseObj(activity.getActivityObj());
        boolean scheduleChanged = isActivityScheduleChanged(dto, oldObj);

        updateActivityBasicFields(activity, dto);
        rebuildActivityScheduleIfNeeded(activity, dto, oldObj, scheduleChanged);
        syncActivityScheduleStatusIfNeeded(activity, dto, scheduleChanged);
        updateActivityExtraFields(activity, dto);

        activityMapper.updateById(activity);
        logger.info("编辑活动: {}", activity.getActivityId());
    }

    private Activity loadActivityForEdit(String id) {
        if (StrUtil.isBlank(id))
            throw new BusinessException(500, "ID不能为空");

        Activity activity = activityMapper.selectById(id);
        if (activity == null)
            throw new BusinessException(500, "活动不存在");
        return activity;
    }

    private void updateActivityBasicFields(Activity activity, com.museum.common.dto.ActivityEditDTO dto) {
        activity.setActivityTitle(dto.getActivityTitle());
        if (dto.getStatus() != null) {
            activity.setActivityStatus(dto.getStatus());
        }
    }

    private boolean isActivityScheduleChanged(com.museum.common.dto.ActivityEditDTO dto, JSONObject oldObj) {
        if (StrUtil.isNotBlank(dto.getStartDate()) && !dto.getStartDate().equals(oldObj.getStr("startDate"))) {
            return true;
        }
        return StrUtil.isNotBlank(dto.getEndDate()) && !dto.getEndDate().equals(oldObj.getStr("endDate"));
    }

    private void rebuildActivityScheduleIfNeeded(Activity activity, com.museum.common.dto.ActivityEditDTO dto,
                                                 JSONObject oldObj, boolean scheduleChanged) {
        if (!scheduleChanged) {
            return;
        }
        logger.info("检测到活动时间变更，正在重置排期... ActivityId={}", activity.getActivityId());
        dayMapper.delete(new QueryWrapper<Day>().eq("ACTIVITY_ID", activity.getActivityId()));

        Integer effectiveStatus = dto.getStatus() != null ? dto.getStatus() : activity.getActivityStatus();
        if (effectiveStatus == 1) {
            String start = StrUtil.isNotBlank(dto.getStartDate()) ? dto.getStartDate() : oldObj.getStr("startDate");
            String end = StrUtil.isNotBlank(dto.getEndDate()) ? dto.getEndDate() : oldObj.getStr("endDate");
            scheduleService.initActivitySchedule(activity.getActivityId(), start, end);
        }
    }

    private void syncActivityScheduleStatusIfNeeded(Activity activity, com.museum.common.dto.ActivityEditDTO dto,
                                                   boolean scheduleChanged) {
        if (scheduleChanged || dto.getStatus() == null) {
            return;
        }
        scheduleService.updateActivityScheduleStatus(activity.getActivityId(), dto.getStatus() == 1 ? 1 : 0);
    }

    private void updateActivityExtraFields(Activity activity, com.museum.common.dto.ActivityEditDTO dto) {
        activity.setActivityEditTime(System.currentTimeMillis());
        activity.setActivityPic(JSONUtil.toJsonStr(extractActivityImages(dto.getContent())));
        activity.setActivityObj(JSONUtil.toJsonStr(buildActivityExtraInfo(dto)));
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
                        scheduleService.initActivitySchedule(activity.getActivityId(), start, end);
                    }
                }
            } else {
                // 有排期则启用
                scheduleService.updateActivityScheduleStatus(activity.getActivityId(), 1);
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
            scheduleService.updateActivityScheduleStatus(activity.getActivityId(), 0);
        }
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
