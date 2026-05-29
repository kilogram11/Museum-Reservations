package com.museum.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.common.constant.AdminBusinessConstant;
import com.museum.common.constant.BookingConstant;
import com.museum.common.dto.MuseumAddDTO;
import com.museum.common.exception.BusinessException;
import com.museum.entity.Day;
import com.museum.entity.Museum;
import com.museum.entity.Time;
import com.museum.mapper.DayMapper;
import com.museum.mapper.MuseumMapper;
import com.museum.mapper.TimeMapper;
import com.museum.service.MuseumService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MuseumServiceImpl extends ServiceImpl<MuseumMapper, Museum> implements MuseumService {

    @Autowired
    private MuseumMapper museumMapper;

    @Autowired
    private DayMapper dayMapper;

    @Autowired
    private TimeMapper timeMapper;

    @Override
    public Page<Museum> dataList(String keyword, Integer page, Integer limit) {
        Page<Museum> pageParam = new Page<>(page, limit);
        QueryWrapper<Museum> wrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like("MUSEUM_TITLE", keyword);
        }
        wrapper.orderByDesc("MUSEUM_ADD_TIME");
        return museumMapper.selectPage(pageParam, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMuseum(MuseumAddDTO dto) {
        // 1. 校验
        if (StrUtil.isBlank(dto.getMuseumTitle()))
            throw new BusinessException(500, "场馆名称不能为空");
        if (StrUtil.isBlank(dto.getStartDate()) || StrUtil.isBlank(dto.getEndDate())) {
            throw new BusinessException(500, "排期日期范围不能为空");
        }
        if (CollUtil.isEmpty(dto.getTimes())) {
            throw new BusinessException(500, "请至少配置一个时间段");
        }

        // 日期逻辑校验
        Date start = DateUtil.parse(dto.getStartDate());
        Date end = DateUtil.parse(dto.getEndDate());
        if (start.after(end)) {
            throw new BusinessException(500, "开始日期不能晚于结束日期");
        }

        long now = System.currentTimeMillis();
        String museumId = AdminBusinessConstant.MUSEUM_ID_PREFIX + IdUtil.fastSimpleUUID();

        // 2. 构建并插入 Museum
        Museum museum = new Museum();
        museum.setId(IdUtil.fastSimpleUUID());
        museum.setMuseumId(museumId);
        museum.setMuseumTitle(dto.getMuseumTitle());
        museum.setAdminId(dto.getAdminId());
        museum.setMuseumMaxJoinCnt(dto.getMuseumMaxJoinCnt());
        museum.setMuseumBookSet(dto.getMuseumBookSet());
        // === 地图定位信息 ===
        museum.setLatitude(dto.getLatitude());
        museum.setLongitude(dto.getLongitude());
        // 打印前端传来的地址
        log.info("前端传入地址: {}", dto.getAddress());
        museum.setAddress(dto.getMuseumAddress());
        // 打印设置后的地址
        log.info("设置到 Museum 对象的地址: {}", museum.getAddress());

        // 默认为禁用(0)，除非传入1
        Integer status = dto.getMuseumStatus() != null ? dto.getMuseumStatus() : 0;

        // --- 逻辑增强: 如果要启用，先禁用其他所有场馆 ---
        if (status == 1) {
            disableOtherActiveMuseums(null);
        }
        // ---------------------------------------------

        museum.setMuseumStatus(status);
        museum.setMuseumAddTime(now);
        museum.setMuseumEditTime(now);
        museum.setPid(BookingConstant.DEFAULT_PID);

        // 2.1 组装 museumObj
        // 关键变更：将排期配置（startDate, endDate, times）也存入 obj，以便后续启用时读取
        Map<String, Object> museumExtraInfo = new HashMap<>();
        museumExtraInfo.put("desc", dto.getMuseumDesc());
        museumExtraInfo.put("cover", dto.getMuseumCover());
        museumExtraInfo.put("content", dto.getMuseumContent());
        museumExtraInfo.put("address", dto.getMuseumAddress());
        museumExtraInfo.put("phone", dto.getMuseumPhone());
        museumExtraInfo.put("traffic", dto.getMuseumTraffic());

        // --- Store Schedule Params ---
        museumExtraInfo.put("startDate", dto.getStartDate());
        museumExtraInfo.put("endDate", dto.getEndDate());
        museumExtraInfo.put("times", dto.getTimes());
        // -----------------------------

        museum.setMuseumObj(JSONUtil.toJsonStr(museumExtraInfo));

        // 2.2 组装 museumPic
        if (CollUtil.isNotEmpty(dto.getMuseumImgs())) {
            museum.setMuseumPic(JSONUtil.toJsonStr(dto.getMuseumImgs()));
        } else if (StrUtil.isNotBlank(dto.getMuseumCover())) {
            // 如果只有封面，将其包装为数组存入
            museum.setMuseumPic(JSONUtil.toJsonStr(CollUtil.newArrayList(dto.getMuseumCover())));
        } else {
            museum.setMuseumPic(BookingConstant.EMPTY_JSON_ARRAY);
        }

        // --- 【日志打印位置】 ---
        log.info("即将插入数据库的 Museum 对象: {}", JSONUtil.toJsonStr(museum));

        museumMapper.insert(museum);
        log.info("创建场馆成功: {}, 状态: {}", museumId, status);

        // 3. 只有当状态为 1 (启用) 时，才生成排期
        if (status == 1) {
            initSchedule(museumId, dto.getStartDate(), dto.getEndDate(), dto.getTimes());
        }
    }

    /**
     * 初始化排期 (内部调用)
     */
    private void initSchedule(String museumId, String startDateStr, String endDateStr,
            List<MuseumAddDTO.TimeTemplate> templates) {
        Date start = DateUtil.parse(startDateStr);
        Date end = DateUtil.parse(endDateStr);

        if (start.after(end)) {
            log.error("排期生成失败: 开始日期晚于结束日期 {} > {}", startDateStr, endDateStr);
            throw new BusinessException(500, "排期日期无效");
        }

        long dayLimitTotal = templates.stream().mapToInt(MuseumAddDTO.TimeTemplate::getLimit).sum();

        Date current = start;
        while (!current.after(end)) {
            String dayStr = DateUtil.format(current, "yyyy-MM-dd");
            String dayId = AdminBusinessConstant.DAY_ID_PREFIX + IdUtil.fastSimpleUUID();

            Day day = new Day();
            day.setId(IdUtil.fastSimpleUUID());
            day.setDayId(dayId);
            day.setDay(dayStr);
            day.setMuseumId(museumId);
            day.setStatus(1); // 默认跟随场馆状态 (已启用)
            day.setDayLimitCnt((int) dayLimitTotal);
            day.setAddTime(System.currentTimeMillis());
            day.setEditTime(System.currentTimeMillis());
            day.setPid(BookingConstant.DEFAULT_PID);
            dayMapper.insert(day);

            for (MuseumAddDTO.TimeTemplate tmpl : templates) {
                Time time = new Time();
                time.setId(IdUtil.fastSimpleUUID());
                time.setTimeId(AdminBusinessConstant.TIME_ID_PREFIX + IdUtil.fastSimpleUUID());
                time.setDayId(dayId);
                time.setMuseumId(museumId);
                time.setTimeStart(tmpl.getStart());
                time.setTimeEnd(tmpl.getEnd());
                time.setTimeMark(String.format("%s_%s_%s", museumId, dayStr, tmpl.getStart()));
                time.setLimitCnt(tmpl.getLimit());
                time.setSuccCnt(0);
                time.setStatus(1);
                time.setIsLimit(1);
                time.setAddTime(System.currentTimeMillis());
                time.setEditTime(System.currentTimeMillis());
                time.setPid(BookingConstant.DEFAULT_PID);
                timeMapper.insert(time);
            }
            current = DateUtil.offsetDay(current, 1);
        }
        log.info("排期生成完成: {} -> {}, 范围: [{}, {}]", museumId, templates.size(), startDateStr, endDateStr);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editMuseum(com.museum.common.dto.MuseumEditDTO dto) {
        if (StrUtil.isBlank(dto.getId()))
            throw new BusinessException(500, "ID不能为空");

        Museum museum = museumMapper.selectById(dto.getId());
        if (museum == null)
            throw new BusinessException(500, "场馆不存在");

        // 1. 更新基本字段
        museum.setMuseumTitle(dto.getMuseumTitle());
        museum.setMuseumMaxJoinCnt(dto.getMuseumMaxJoinCnt());
        museum.setMuseumBookSet(dto.getMuseumBookSet());
        // === 更新地图定位信息 ===
        museum.setLatitude(dto.getLatitude());
        museum.setLongitude(dto.getLongitude());
        museum.setAddress(dto.getMuseumAddress());


        // 状态更新 (如果传入了)
        if (dto.getMuseumStatus() != null) {
            // --- 逻辑增强: 如果要启用，先禁用其他所有场馆 ---
            if (dto.getMuseumStatus() == 1) {
                disableOtherActiveMuseums(museum.getMuseumId());
            }
            // ---------------------------------------------
            museum.setMuseumStatus(dto.getMuseumStatus());
        }

        // --- 逻辑增强: 检查排期参数是否变更 ---
        boolean scheduleChanged = false;
        JSONObject oldObj = JSONUtil.parseObj(museum.getMuseumObj());

        // 1. 检查日期
        if (StrUtil.isNotBlank(dto.getStartDate()) && !dto.getStartDate().equals(oldObj.getStr("startDate"))) {
            scheduleChanged = true;
        }
        if (StrUtil.isNotBlank(dto.getEndDate()) && !dto.getEndDate().equals(oldObj.getStr("endDate"))) {
            scheduleChanged = true;
        }
        // 2. 检查时间段 (简单比较JSON字符串)
        if (CollUtil.isNotEmpty(dto.getTimes())) {
            String newTimesJson = JSONUtil.toJsonStr(dto.getTimes());
            String oldTimesJson = JSONUtil.toJsonStr(oldObj.getBeanList("times", MuseumAddDTO.TimeTemplate.class));
            // 注意: 用于比较，顺序不同视为不同
            if (!newTimesJson.equals(oldTimesJson)) {
                scheduleChanged = true;
            }
        }

        if (scheduleChanged) {
            log.info("检测到排期变更，正在重置排期数据... MuseumId={}", museum.getMuseumId());
            // (1) 删除旧排期
            dayMapper.delete(new QueryWrapper<Day>().eq("MUSEUM_ID", museum.getMuseumId()));
            timeMapper.delete(new QueryWrapper<Time>().eq("MUSEUM_ID", museum.getMuseumId()));

            // (2) 重新生成 (仅当状态为启用1 或 新状态为1时生成，这里假设如果改了时间，且状态有效则生成)
            // 如果DTO没传status，用旧的；传了用新的
            Integer effectiveStatus = dto.getMuseumStatus() != null ? dto.getMuseumStatus() : museum.getMuseumStatus();

            if (effectiveStatus == 1) {
                // 需要确保 DTO 中有完整的参数，如果没有传，则应该用旧的?
                // 但 EditDTO 通常前端会回显所有数据，所以这里假设 DTO 数据是新的且完整的(如果是部分更新，逻辑会复杂)
                // 根据 DTO 定义和 Controller，这是全量提交 (id必填，其他可选但这通常是表单提交)
                // 为防止 NPE，优先取 DTO，取不到取 oldObj
                String start = StrUtil.isNotBlank(dto.getStartDate()) ? dto.getStartDate() : oldObj.getStr("startDate");
                String end = StrUtil.isNotBlank(dto.getEndDate()) ? dto.getEndDate() : oldObj.getStr("endDate");
                List<MuseumAddDTO.TimeTemplate> times = CollUtil.isNotEmpty(dto.getTimes()) ? dto.getTimes()
                        : oldObj.getBeanList("times", MuseumAddDTO.TimeTemplate.class);

                initSchedule(museum.getMuseumId(), start, end, times);
            }
        } else {
            // 仅状态变更时的同步 (如果排期没变)
            if (dto.getMuseumStatus() != null) {
                if (dto.getMuseumStatus() == 1) {
                    updateScheduleStatus(museum.getMuseumId(), 1);
                } else {
                    updateScheduleStatus(museum.getMuseumId(), 0);
                }
            }
        }
        // -------------------------------------

        museum.setMuseumEditTime(System.currentTimeMillis());

        // 2. 更新 museumObj (复杂字段)
        // 注意：这里需要保留原有的部分排期配置(如果只需要改内容)，或者完全覆盖。
        // 根据需求 "支持更改"，通常意味着覆盖。
        // 但为了安全，排期生成逻辑(Day/Time)不在此处触发，仅更新配置和展示信息。
        Map<String, Object> museumExtraInfo = new HashMap<>();
        museumExtraInfo.put("desc", dto.getMuseumDesc());
        museumExtraInfo.put("cover", dto.getMuseumCover());
        museumExtraInfo.put("content", dto.getMuseumContent());
        museumExtraInfo.put("address", dto.getMuseumAddress());
        museumExtraInfo.put("phone", dto.getMuseumPhone());
        museumExtraInfo.put("traffic", dto.getMuseumTraffic());

        // 重新存入排期配置，以便后续"状态切换"时能读取到最新配置
        museumExtraInfo.put("startDate", dto.getStartDate());
        museumExtraInfo.put("endDate", dto.getEndDate());
        museumExtraInfo.put("times", dto.getTimes());

        museum.setMuseumObj(JSONUtil.toJsonStr(museumExtraInfo));

        // 3. 更新图片列表
        if (CollUtil.isNotEmpty(dto.getMuseumImgs())) {
            museum.setMuseumPic(JSONUtil.toJsonStr(dto.getMuseumImgs()));
        } else if (StrUtil.isNotBlank(dto.getMuseumCover())) {
            museum.setMuseumPic(JSONUtil.toJsonStr(CollUtil.newArrayList(dto.getMuseumCover())));
        } else {
            museum.setMuseumPic(BookingConstant.EMPTY_JSON_ARRAY);
        }

        museumMapper.updateById(museum);
        log.info("编辑场馆: {}", museum.getMuseumId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delMuseum(String id) {
        Museum museum = museumMapper.selectById(id);
        if (museum == null)
            return;

        QueryWrapper<Day> dayWrapper = new QueryWrapper<>();
        dayWrapper.eq("MUSEUM_ID", museum.getMuseumId());
        dayMapper.delete(dayWrapper);

        QueryWrapper<Time> timeWrapper = new QueryWrapper<>();
        timeWrapper.eq("MUSEUM_ID", museum.getMuseumId());
        timeMapper.delete(timeWrapper);

        museumMapper.deleteById(id);
        log.info("删除场馆及排期: {}", museum.getMuseumId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void status(String id, Integer status) {
        // 尝试按 ID 查询
        Museum museum = museumMapper.selectById(id);
        // 如果按 ID 查不到，尝试按 MUSEUM_ID (业务ID) 查询
        if (museum == null) {
            QueryWrapper<Museum> wrapper = new QueryWrapper<>();
            wrapper.eq("MUSEUM_ID", id);
            museum = museumMapper.selectOne(wrapper);
        }

        if (museum == null) {
            log.warn("修改状态失败: 未找到场馆 ID={}", id);
            return;
        }

        // 1. 更新场馆状态
        // --- 逻辑增强: 如果要启用，先禁用其他所有场馆 ---
        if (status == 1) {
            disableOtherActiveMuseums(museum.getMuseumId());
        }
        // ---------------------------------------------

        museum.setMuseumStatus(status);
        museum.setMuseumEditTime(System.currentTimeMillis());
        museumMapper.updateById(museum);

        // 2. 状态级联逻辑
        if (status == 1) {
            // 启用逻辑：检查是否有排期，无则生成，有则启用
            long count = dayMapper.selectCount(new QueryWrapper<Day>().eq("MUSEUM_ID", museum.getMuseumId()));
            if (count == 0) {
                // 读取存储的排期配置
                if (StrUtil.isNotBlank(museum.getMuseumObj())) {
                    JSONObject obj = JSONUtil.parseObj(museum.getMuseumObj());
                    String start = obj.getStr("startDate");
                    String end = obj.getStr("endDate");
                    List<MuseumAddDTO.TimeTemplate> times = obj.getBeanList("times", MuseumAddDTO.TimeTemplate.class);

                    if (StrUtil.isAllNotBlank(start, end) && CollUtil.isNotEmpty(times)) {
                        initSchedule(museum.getMuseumId(), start, end, times);
                    } else {
                        log.warn("场馆启用失败: 缺少排期配置数据, ID: {}", id);
                    }
                }
            } else {
                // 已有排期，全部启用 (逻辑恢复)
                updateScheduleStatus(museum.getMuseumId(), 1);
            }
        } else {
            // 禁用逻辑：全部禁用 (逻辑删除)
            updateScheduleStatus(museum.getMuseumId(), 0);
        }
    }

    /**
     * 禁用其他已启用的场馆 (互斥逻辑)
     * 
     * @param excludeMuseumId 当前需要排除的场馆ID (即正在启用的那个), 新增时传 null
     */
    private void disableOtherActiveMuseums(String excludeMuseumId) {
        QueryWrapper<Museum> wrapper = new QueryWrapper<>();
        wrapper.eq("MUSEUM_STATUS", 1);
        if (StrUtil.isNotBlank(excludeMuseumId)) {
            wrapper.ne("MUSEUM_ID", excludeMuseumId);
        }
        List<Museum> activeList = museumMapper.selectList(wrapper);

        for (Museum m : activeList) {
            // 1. 修改主表状态
            m.setMuseumStatus(0);
            m.setMuseumEditTime(System.currentTimeMillis());
            museumMapper.updateById(m);

            // 2. 联动禁用排期
            updateScheduleStatus(m.getMuseumId(), 0);

            log.info("互斥策略执行: 自动禁用场馆 {}", m.getMuseumId());
        }
    }

    private void updateScheduleStatus(String museumId, Integer status) {
        // 更新 Day 表状态
        Day day = new Day();
        day.setStatus(status);
        dayMapper.update(day, new UpdateWrapper<Day>().eq("MUSEUM_ID", museumId));

        // 更新 Time 表状态
        Time time = new Time();
        time.setStatus(status);
        timeMapper.update(time, new UpdateWrapper<Time>().eq("MUSEUM_ID", museumId));

        log.info("批量更新排期状态: museumId={}, status={}", museumId, status);
    }

    @Override
    public List<Map<String, Object>> getAllList() {
        QueryWrapper<Museum> wrapper = new QueryWrapper<>();
        wrapper.eq("MUSEUM_STATUS", 1);
        wrapper.select("MUSEUM_ID", "MUSEUM_TITLE");
        List<Museum> list = museumMapper.selectList(wrapper);
        return list.stream().map(m -> {
            Map<String, Object> optionItem = new HashMap<>();
            optionItem.put("id", m.getMuseumId());
            optionItem.put("title", m.getMuseumTitle());
            return optionItem;
        }).collect(Collectors.toList());
    }

    @Override
    public Page<Museum> appList(Integer page, Integer limit) {
        Page<Museum> pageParam = new Page<>(page, limit);
        QueryWrapper<Museum> wrapper = new QueryWrapper<>();
        wrapper.eq("MUSEUM_STATUS", 1);
        wrapper.orderByAsc("MUSEUM_ADD_TIME");
        return museumMapper.selectPage(pageParam, wrapper);
    }
}
