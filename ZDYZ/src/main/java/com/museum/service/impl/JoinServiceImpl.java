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
import com.museum.common.exception.BusinessException;
import com.museum.common.utils.QRCodeUtil;
import com.museum.entity.*;
import com.museum.mapper.*;
import com.museum.service.JoinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service

public class JoinServiceImpl extends ServiceImpl<JoinMapper, Join> implements JoinService {

    private static final Logger logger = LoggerFactory.getLogger(JoinServiceImpl.class);
    private static final ConcurrentHashMap<String, Object> LOCAL_LOCKS = new ConcurrentHashMap<>();

    @Autowired
    private DayMapper dayMapper;

    @Autowired
    private TimeMapper timeMapper;

    @Autowired
    private IdentityMapper identityMapper;

    @Autowired
    private JoinMapper joinMapper;

    @Autowired
    private MuseumMapper museumMapper;

    @Autowired
    private com.museum.service.MessageService messageService;

    @Autowired
    private ActivityMapper activityMapper;

    @Override
    public List<Map<String, Object>> getBookingDays() {
        // 1. 获取当前启用的场馆 (First Active Museum)
        QueryWrapper<Museum> museumWrapper = new QueryWrapper<>();
        museumWrapper.eq("MUSEUM_STATUS", 1);
        museumWrapper.orderByAsc("MUSEUM_ADD_TIME");
        museumWrapper.last("LIMIT 1");
        Museum activeMuseum = museumMapper.selectOne(museumWrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        if (activeMuseum == null) {
            return result; // 无启用场馆
        }

        // 2. 获取该场馆未来 7 天的排期
        String today = DateUtil.today();

        QueryWrapper<Day> wrapper = new QueryWrapper<>();
        wrapper.ge("DAY", today);
        // wrapper.eq("STATUS", 1); // 用户要求移除状态判断
        wrapper.eq("MUSEUM_ID", activeMuseum.getMuseumId()); // 必须匹配当前场馆
        wrapper.orderByAsc("DAY");
        wrapper.last("LIMIT 7"); // 展示一周

        List<Day> days = dayMapper.selectList(wrapper);

        for (Day day : days) {
            Map<String, Object> map = new HashMap<>();
            map.put("day", day.getDay());
            // Week enum to Chinese
            map.put("week", DateUtil.dayOfWeekEnum(DateUtil.parse(day.getDay())).toChinese("周"));
            map.put("status", day.getStatus()); // 1开馆 0闭馆
            result.add(map);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getBookingTimes(String dayStr) {
        // 根据日期查询 time 表
        QueryWrapper<Time> wrapper = new QueryWrapper<>();
        wrapper.like("TIME_MARK", dayStr); // TIME_MARK 包含日期
        wrapper.eq("STATUS", 1);
        wrapper.orderByAsc("TIME_START");

        List<Time> times = timeMapper.selectList(wrapper);
        // 按实际时间排序（避免字符串 "11:00" 排在 "9:00" 前面）
        times.sort((t1, t2) -> {
            try {
                // 补零对齐后比较字符串即可 (09:00 vs 11:00)
                String s1 = t1.getTimeStart().length() == 4 ? "0" + t1.getTimeStart() : t1.getTimeStart();
                String s2 = t2.getTimeStart().length() == 4 ? "0" + t2.getTimeStart() : t2.getTimeStart();
                return s1.compareTo(s2);
            } catch (Exception e) {
                return 0;
            }
        });

        List<Map<String, Object>> result = new ArrayList<>();

        for (Time time : times) {
            Map<String, Object> map = new HashMap<>();
            map.put("timeMark", time.getTimeMark());
            map.put("startTime", time.getTimeStart());
            map.put("endTime", time.getTimeEnd());
            map.put("total", time.getLimitCnt());
            map.put("used", time.getSuccCnt());
            map.put("surplus", Math.max(0, time.getLimitCnt() - time.getSuccCnt())); // 剩余
            result.add(map);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitBooking(String userId, String timeMark, List<String> identityIds) {
        if (CollUtil.isEmpty(identityIds)) {
            throw new BusinessException(500, "请选择参观人");
        }
        if (identityIds.size() > 3) {
            throw new BusinessException(500, "单次最多预约3人");
        }

        String lockKey = "lock:booking:" + timeMark;
        boolean locked = false;
        long startTime = System.currentTimeMillis();
        try {
            while (true) {
                try {
                    // A. 尝试使用 Redis 分布式锁
                    Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);
                    if (Boolean.TRUE.equals(success)) {
                        locked = true;
                        break;
                    }
                } catch (Exception e) {
                    // B. Redis 不可用，降级为 JVM 本地锁 (适用于单机/开发环境)
                    logger.warn("Redis 连接失败，正在使用 JVM 本地锁降级运行... 错误: {}", e.getMessage());
                    if (LOCAL_LOCKS.putIfAbsent(lockKey, new Object()) == null) {
                        locked = true;
                        break;
                    }
                }

                if (System.currentTimeMillis() - startTime > 15000) {
                    throw new BusinessException(500, "预约人数较多，请稍后刷新重试");
                }
                Thread.sleep(100);
            }

            // 获取锁成功，开始业务逻辑 (Double Check)

            // 1. 校验时段
            QueryWrapper<Time> timeWrapper = new QueryWrapper<>();
            timeWrapper.eq("TIME_MARK", timeMark);
            Time time = timeMapper.selectOne(timeWrapper);
            if (time == null || time.getStatus() == 0) {
                throw new BusinessException(500, "时段无效或未开放");
            }

            // 2. 校验余量 (此时已加锁，数据是安全的)
            int need = identityIds.size();
            // 注意：这里必须查最新的 DB，因为 selectOne 已经是新的了
            if (time.getSuccCnt() + need > time.getLimitCnt()) {
                throw new BusinessException(500, "该时段余量不足");
            }

            // 修正：从 time 关联的 day 表获取日期
            Day day = dayMapper.selectOne(new QueryWrapper<Day>().eq("DAY_ID", time.getDayId()));
            if (day == null) {
                throw new BusinessException(500, "排期日期数据异常");
            }
            String meetDay = day.getDay();

            List<Join> joinsToSave = new ArrayList<>();
            long now = System.currentTimeMillis();

            for (String identityId : identityIds) {
                // 3. 校验游客信息
                Identity identity = identityMapper
                        .selectOne(new QueryWrapper<Identity>().eq("IDENTITY_ID", identityId));
                if (identity == null) {
                    throw new BusinessException(500, "游客信息不存在: " + identityId);
                }
                if (identity.getIdentityStatus() != null && identity.getIdentityStatus() == 0) {
                    throw new BusinessException(500, "游客 " + identity.getIdentityName() + " 在黑名单中，无法预约");
                }

                // 4. 重复预约校验（同一天同一人）
                Long count = joinMapper.selectCount(new QueryWrapper<Join>()
                        .eq("IDENTITY_ID", identityId)
                        .eq("JOIN_MEET_DAY", meetDay)
                        .in("JOIN_STATUS", 1)); // 1成功

                if (count > 0) {
                    throw new BusinessException(500, identity.getIdentityName() + " 今日已预约，请勿重复提交");
                }

                // 5. 组装 Join 对象
                Join join = new Join();
                join.setId(IdUtil.fastSimpleUUID());
                join.setJoinId("join_" + IdUtil.fastSimpleUUID());
                join.setUserId(userId);
                join.setIdentityId(identityId);
                join.setJoinMeetDay(meetDay);

                join.setTimeMark(timeMark);
                join.setJoinStatus(1); // 成功
                join.setJoinIsCheckin(0); // 未核销
                join.setJoinAddTime(now);
                join.setJoinEditTime(now);
                join.setPid("1");

                // 生成表单快照 (JSON)
                Map<String, Object> formMap = new HashMap<>();
                formMap.put("name", identity.getIdentityName());
                formMap.put("card", identity.getIdentityCard());
                formMap.put("mobile", identity.getIdentityMobile());
                join.setJoinForms(JSONUtil.toJsonStr(formMap));

                // 生成二维码内容 (base64)
                String qrContent = join.getJoinId();
                String qrBase64 = QRCodeUtil.generateBase64(qrContent, 300, 300);
                join.setJoinQr(qrBase64);

                joinsToSave.add(join);
            }

            // 6. 批量保存 JOIN
            for (Join j : joinsToSave) {
                joinMapper.insert(j);

                // --- 发送系统消息 ---
                try {
                    // 解析名称
                    String visitorName = "游客";
                    if (StrUtil.isNotBlank(j.getJoinForms())) {
                        JSONObject form = JSONUtil.parseObj(j.getJoinForms());
                        visitorName = form.getStr("name", "游客");
                    }

                    String titleName = "博物馆";
                    if (StrUtil.isNotBlank(time.getActivityId())) {
                        Activity act = activityMapper
                                .selectOne(new QueryWrapper<Activity>().eq("ACTIVITY_ID", time.getActivityId()));
                        if (act != null)
                            titleName = act.getActivityTitle();
                    } else if (StrUtil.isNotBlank(time.getMuseumId())) {
                        Museum mus = museumMapper
                                .selectOne(new QueryWrapper<Museum>().eq("MUSEUM_ID", time.getMuseumId()));
                        if (mus != null)
                            titleName = mus.getMuseumTitle();
                    }

                    // 格式: 张三，您已成功预约 故宫博物院
                    String finalTitle = visitorName + "，您已成功预约 " + titleName;
                    String timeRange = meetDay + " " + time.getTimeStart() + "-" + time.getTimeEnd();

                    // 这里传入 finalTitle 作为参数1，timeRange 作为参数2
                    // 对应模版: {0}，时间：{1}。请准时参观。
                    // 结果: 张三，您已成功预约 故宫博物院，时间：2026-01-10...
                    messageService.createMessage(userId, "BOOKING_SUCCESS", finalTitle, timeRange);
                } catch (Exception e) {
                    logger.error("发送预约成功消息失败", e);
                }
                // ------------------
            }

            // 7. 扣减库存
            time.setSuccCnt(time.getSuccCnt() + need);
            timeMapper.updateById(time);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "预约中断");
        } finally {
            if (locked) {
                try {
                    stringRedisTemplate.delete(lockKey);
                } catch (Exception e) {
                    // 清理本地锁
                    LOCAL_LOCKS.remove(lockKey);
                }
            }
        }
    }

    @Override
    public List<Join> getMyBookings(String userId) {
        // 使用 LEFT JOIN time 表的查询，确保 start/end 时间字段有值
        List<Join> list = joinMapper.selectUserJoinList(userId);
        if (CollUtil.isNotEmpty(list)) {
            // 缓存场馆信息，避免频繁查库 (通常只有一个场馆)
            Map<String, Museum> museumCache = new HashMap<>();
            for (Join j : list) {
                // 根据 timeMark 重新查一下对应的 Museum (通过 time 表)
                QueryWrapper<Time> tWrapper = new QueryWrapper<>();
                tWrapper.eq("TIME_MARK", j.getTimeMark());
                Time t = timeMapper.selectOne(tWrapper);
                if (t != null && StrUtil.isNotBlank(t.getMuseumId())) {
                    Museum m = museumCache.computeIfAbsent(t.getMuseumId(),
                            id -> museumMapper.selectOne(new QueryWrapper<Museum>().eq("MUSEUM_ID", id)));
                    if (m != null) {
                        j.setMuseumTitle(m.getMuseumTitle());
                        j.setLatitude(m.getLatitude());
                        j.setLongitude(m.getLongitude());
                        // 处理地址，逻辑与首页一致
                        String addr = m.getAddress();
                        try {
                            String objStr = m.getMuseumObj();
                            if (StrUtil.isNotBlank(objStr)) {
                                JSONObject obj = JSONUtil.parseObj(objStr);
                                if (StrUtil.isNotBlank(obj.getStr("address"))) {
                                    addr = obj.getStr("address");
                                }
                            }
                        } catch (Exception ignored) {
                        }
                        j.setMuseumAddress(addr);
                    }
                }
            }
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelBooking(String userId, String joinId) {
        // 1. 查询预约
        // joinId 可能是业务ID (join_xxx) 也可能是主键ID
        Join join = joinMapper.selectOne(new QueryWrapper<Join>()
                .eq("USER_ID", userId)
                .and(w -> w.eq("JOIN_ID", joinId).or().eq("_id", joinId)));

        if (join == null) {
            throw new BusinessException(500, "预约记录不存在");
        }

        if (join.getJoinStatus() != 1) {
            throw new BusinessException(500, "当前状态不可取消");
        }

        if (join.getJoinIsCheckin() != 0) {
            throw new BusinessException(500, "已核销或已失效，无法取消");
        }

        // 3. 更新状态
        join.setJoinStatus(2); // 取消状态
        join.setJoinEditTime(System.currentTimeMillis());
        joinMapper.updateById(join);

        // --- 发送取消消息 ---
        try {
            // 查找对应的 Time -> Museum/Activity
            QueryWrapper<Time> tWrapper = new QueryWrapper<>();
            tWrapper.eq("TIME_MARK", join.getTimeMark());
            Time t = timeMapper.selectOne(tWrapper);

            String titleName = "预约";
            if (t != null) {
                if (StrUtil.isNotBlank(t.getActivityId())) {
                    Activity act = activityMapper
                            .selectOne(new QueryWrapper<Activity>().eq("ACTIVITY_ID", t.getActivityId()));
                    if (act != null)
                        titleName = act.getActivityTitle();
                } else if (StrUtil.isNotBlank(t.getMuseumId())) {
                    Museum mus = museumMapper.selectOne(new QueryWrapper<Museum>().eq("MUSEUM_ID", t.getMuseumId()));
                    if (mus != null)
                        titleName = mus.getMuseumTitle();
                }
            }
            messageService.createMessage(userId, "BOOKING_CANCEL", titleName);
        } catch (Exception e) {
            logger.error("发送预约取消消息失败", e);
        }
        // ------------------

        // 4. 回滚库存
        // 查找对应的 Time
        QueryWrapper<Time> timeWrapper = new QueryWrapper<>();
        timeWrapper.eq("TIME_MARK", join.getTimeMark());
        Time time = timeMapper.selectOne(timeWrapper);
        if (time != null) {
            if (time.getSuccCnt() > 0) {
                time.setSuccCnt(time.getSuccCnt() - 1);
                timeMapper.updateById(time);
            }
        }
    }

    @Override
    public Page<Join> adminList(String keyword, Integer page, Integer limit) {

        // 1. 获取所有符合条件的记录
        List<Join> allList = joinMapper.searchByKeyword(keyword);
        if (allList == null) {
            allList = new ArrayList<>();
        }

        // 2. 手动分页
        int total = allList.size();
        List<Join> pagedList = allList.stream()
                .skip((long) (page - 1) * limit)
                .limit(limit)
                .collect(Collectors.toList());

        // 3. 封装 Page 对象
        Page<Join> pageResult = new Page<>(page, limit, total);
        pageResult.setRecords(pagedList);

        return pageResult;
    }

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkin(String id) {
        String lockKey = "lock:checkin:" + id;
        boolean locked = false;
        try {
            try {
                Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
                locked = Boolean.TRUE.equals(success);
            } catch (Exception e) {
                logger.warn("核销时 Redis 不可用，使用 JVM 锁降级");
                locked = LOCAL_LOCKS.putIfAbsent(lockKey, new Object()) == null;
            }

            if (!locked) {
                throw new BusinessException(500, "正在核销中，请勿重复扫描");
            }
            // 1. 查询预约
            Join join = joinMapper.selectOne(new QueryWrapper<Join>()
                    .and(w -> w.eq("JOIN_ID", id).or().eq("_id", id)));

            if (join == null) {
                throw new BusinessException(500, "预约记录不存在");
            }

            // 2. 校验状态
            if (join.getJoinStatus() != 1) {
                throw new BusinessException(500, "非预约成功状态，无法核销");
            }
            if (join.getJoinIsCheckin() == 1) {
                throw new BusinessException(500, "该记录已核销，请勿重复操作");
            }
            if (join.getJoinIsCheckin() == 3) {
                throw new BusinessException(500, "该记录已失效/爽约，无法核销");
            }

            // 3. 执行核销
            join.setJoinIsCheckin(1); // 已核销
            join.setJoinEditTime(System.currentTimeMillis());
            joinMapper.updateById(join);
        } finally {
            if (locked) {
                try {
                    stringRedisTemplate.delete(lockKey);
                } catch (Exception e) {
                    LOCAL_LOCKS.remove(lockKey);
                }
            }
        }
    }

    @Override
    public List<Join> getAllForExport() {
        List<Join> allList = joinMapper.searchByKeyword("");
        return allList != null ? allList : new ArrayList<>();
    }
}
