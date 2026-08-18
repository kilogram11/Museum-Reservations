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
import com.museum.common.constant.BookingConstant;
import com.museum.common.enums.CheckinStatus;
import com.museum.common.enums.IdentityStatus;
import com.museum.common.enums.JoinStatus;
import com.museum.common.exception.BusinessException;
import com.museum.common.exception.ErrorCode;
import com.museum.common.utils.QRCodeUtil;
import com.museum.entity.*;
import com.museum.mapper.*;
import com.museum.service.BookingStockService;
import com.museum.service.JoinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class JoinServiceImpl extends ServiceImpl<JoinMapper, Join> implements JoinService {

    private static final Logger logger = LoggerFactory.getLogger(JoinServiceImpl.class);

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
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private BookingStockService bookingStockService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    // ==================== 查询：可预约日期 ====================

    @Override
    public List<Map<String, Object>> getBookingDays() {
        Museum activeMuseum = findActiveMuseum();
        if (activeMuseum == null) {
            return Collections.emptyList();
        }

        List<Day> days = queryUpcomingDays(activeMuseum.getMuseumId());
        return buildDayResultList(days);
    }

    private Museum findActiveMuseum() {
        return museumMapper.selectOne(
                new QueryWrapper<Museum>()
                        .eq("MUSEUM_STATUS", 1)
                        .orderByAsc("MUSEUM_ADD_TIME")
                        .last("LIMIT 1"));
    }

    private List<Day> queryUpcomingDays(String museumId) {
        return dayMapper.selectList(
                new QueryWrapper<Day>()
                        .ge("DAY", DateUtil.today())
                        .eq("MUSEUM_ID", museumId)
                        .orderByAsc("DAY")
                        .last("LIMIT " + BookingConstant.BOOKING_DAYS_TO_SHOW));
    }

    private List<Map<String, Object>> buildDayResultList(List<Day> days) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Day day : days) {
            Map<String, Object> map = new HashMap<>();
            map.put("day", day.getDay());
            map.put("week", DateUtil.dayOfWeekEnum(DateUtil.parse(day.getDay())).toChinese("周"));
            map.put("status", day.getStatus());
            result.add(map);
        }
        return result;
    }

    // ==================== 查询：可预约时段 ====================

    @Override
    public List<Map<String, Object>> getBookingTimes(String dayStr) {
        List<Time> times = timeMapper.selectList(
                new QueryWrapper<Time>()
                        .like("TIME_MARK", dayStr)
                        .eq("STATUS", 1)
                        .orderByAsc("TIME_START"));

        times.sort((t1, t2) -> compareTimeStart(t1.getTimeStart(), t2.getTimeStart()));

        return buildTimeResultList(times);
    }

    private int compareTimeStart(String start1, String start2) {
        try {
            return normalizeTimeStart(start1).compareTo(normalizeTimeStart(start2));
        } catch (Exception e) {
            logger.warn("时间排序失败: {} vs {}", start1, start2, e);
            return 0;
        }
    }

    private String normalizeTimeStart(String timeStart) {
        return timeStart != null && timeStart.length() == 4 ? "0" + timeStart : timeStart;
    }

    private List<Map<String, Object>> buildTimeResultList(List<Time> times) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Time time : times) {
            int total = time.getLimitCnt() == null ? 0 : time.getLimitCnt();
            int usedDb = time.getSuccCnt() == null ? 0 : time.getSuccCnt();
            int dbRemain = Math.max(0, total - usedDb);

            Integer redisRemain = bookingStockService.getRedisRemain(time.getTimeMark());
            if (redisRemain == null) {
                bookingStockService.warmUpIfAbsent(time.getTimeMark(), dbRemain);
                redisRemain = bookingStockService.getRedisRemain(time.getTimeMark());
                if (redisRemain == null) {
                    redisRemain = dbRemain;
                }
            }

            int surplus = Math.max(0, redisRemain);
            Map<String, Object> map = new HashMap<>();
            map.put("timeMark", time.getTimeMark());
            map.put("startTime", time.getTimeStart());
            map.put("endTime", time.getTimeEnd());
            map.put("total", total);
            map.put("used", Math.max(0, total - surplus));
            map.put("surplus", surplus);
            result.add(map);
        }
        return result;
    }

    // ==================== 核心：提交预约 ====================

    @Override
    public void submitBooking(String userId, String timeMark, List<String> identityIds) {
        validateBookingRequest(identityIds);
        rejectDuplicateIdentityIdsInRequest(identityIds);

        Time time = validateTimeSlot(timeMark);
        Day day = validateDaySchedule(time);
        String meetDay = day.getDay();

        List<Join> joinsToSave = buildJoinRecords(userId, timeMark, identityIds, meetDay);

        int remain = Math.max(0, time.getLimitCnt() - time.getSuccCnt());
        bookingStockService.warmUpIfAbsent(timeMark, remain);
        long bookedTtlSeconds = calcBookedTtlSeconds(meetDay);
        bookingStockService.tryReserve(timeMark, meetDay, identityIds, bookedTtlSeconds);

        try {
            transactionTemplate.executeWithoutResult(status -> {
                saveBookingRecords(joinsToSave, time, meetDay, userId);
                bookingStockService.incrMysqlSuccCnt(timeMark, identityIds.size());
            });
        } catch (RuntimeException e) {
            bookingStockService.compensate(timeMark, meetDay, identityIds);
            throw e;
        }
    }

    private void validateBookingRequest(List<String> identityIds) {
        if (CollUtil.isEmpty(identityIds)) {
            throw new BusinessException(ErrorCode.BOOKING_NO_VISITORS);
        }
        if (identityIds.size() > BookingConstant.MAX_BOOKING_COUNT) {
            throw new BusinessException(ErrorCode.BOOKING_TOO_MANY);
        }
    }

    private void rejectDuplicateIdentityIdsInRequest(List<String> identityIds) {
        Set<String> seen = new HashSet<>();
        for (String identityId : identityIds) {
            if (!seen.add(identityId)) {
                throw new BusinessException(ErrorCode.IDENTITY_DUPLICATE_IN_REQUEST);
            }
        }
    }

    private long calcBookedTtlSeconds(String meetDay) {
        Date endOfDay = DateUtil.endOfDay(DateUtil.parseDate(meetDay));
        long seconds = (endOfDay.getTime() - System.currentTimeMillis()) / 1000
                + BookingConstant.BOOKED_TTL_BUFFER_SECONDS;
        return Math.max(BookingConstant.BOOKED_TTL_BUFFER_SECONDS, seconds);
    }

    private List<Join> buildJoinRecords(String userId, String timeMark,
                                         List<String> identityIds, String meetDay) {
        List<Join> joinsToSave = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (String identityId : identityIds) {
            Identity identity = validateVisitorIdentity(identityId);
            checkDuplicateBooking(identityId, meetDay);
            joinsToSave.add(buildJoinRecord(userId, identity, timeMark, meetDay, now));
        }
        return joinsToSave;
    }

    private Time validateTimeSlot(String timeMark) {
        Time time = timeMapper.selectOne(
                new QueryWrapper<Time>().eq("TIME_MARK", timeMark));
        if (time == null || time.getStatus().equals(0)) {
            throw new BusinessException(ErrorCode.BOOKING_SLOT_INVALID);
        }
        return time;
    }

    private Day validateDaySchedule(Time time) {
        Day day = dayMapper.selectOne(
                new QueryWrapper<Day>().eq("DAY_ID", time.getDayId()));
        if (day == null) {
            throw new BusinessException(ErrorCode.BOOKING_SCHEDULE_ERROR);
        }
        return day;
    }

    private Identity validateVisitorIdentity(String identityId) {
        Identity identity = identityMapper.selectOne(
                new QueryWrapper<Identity>().eq("IDENTITY_ID", identityId));
        if (identity == null) {
            throw new BusinessException(ErrorCode.IDENTITY_NOT_FOUND.getCode(),
                    "游客信息不存在: " + identityId);
        }
        if (IdentityStatus.BLACKLISTED.getCode().equals(identity.getIdentityStatus())) {
            throw new BusinessException(ErrorCode.IDENTITY_BLACKLISTED.getCode(),
                    "游客 " + identity.getIdentityName() + " 在黑名单中，无法预约");
        }
        return identity;
    }

    private void checkDuplicateBooking(String identityId, String meetDay) {
        Long count = joinMapper.selectCount(
                new QueryWrapper<Join>()
                        .eq("IDENTITY_ID", identityId)
                        .eq("JOIN_MEET_DAY", meetDay)
                        .eq("JOIN_STATUS", JoinStatus.SUCCESS.getCode()));
        if (count > 0) {
            Identity identity = identityMapper.selectOne(
                    new QueryWrapper<Identity>().eq("IDENTITY_ID", identityId));
            String name = identity != null ? identity.getIdentityName() : identityId;
            throw new BusinessException(ErrorCode.IDENTITY_DUPLICATE_BOOKING.getCode(),
                    name + " 今日已预约，请勿重复提交");
        }
    }

    private Join buildJoinRecord(String userId, Identity identity,
                                  String timeMark, String meetDay, long now) {
        Join join = new Join();
        join.setId(IdUtil.fastSimpleUUID());
        join.setJoinId(BookingConstant.JOIN_ID_PREFIX + IdUtil.fastSimpleUUID());
        join.setUserId(userId);
        join.setIdentityId(identity.getIdentityId());
        join.setJoinMeetDay(meetDay);
        join.setTimeMark(timeMark);
        join.setJoinStatus(JoinStatus.SUCCESS.getCode());
        join.setJoinIsCheckin(CheckinStatus.UNCHECKED.getCode());
        join.setJoinAddTime(now);
        join.setJoinEditTime(now);
        join.setPid(BookingConstant.DEFAULT_PID);

        join.setJoinForms(buildFormSnapshot(identity));
        join.setJoinQr(generateQrCode(join.getJoinId()));

        return join;
    }

    private String buildFormSnapshot(Identity identity) {
        Map<String, Object> formMap = new HashMap<>();
        formMap.put("name", identity.getIdentityName());
        formMap.put("card", identity.getIdentityCard());
        formMap.put("mobile", identity.getIdentityMobile());
        return JSONUtil.toJsonStr(formMap);
    }

    private String generateQrCode(String joinId) {
        return QRCodeUtil.generateBase64(joinId,
                BookingConstant.QR_CODE_WIDTH, BookingConstant.QR_CODE_HEIGHT);
    }

    private void saveBookingRecords(List<Join> joinsToSave, Time time,
                                     String meetDay, String userId) {
        for (Join join : joinsToSave) {
            joinMapper.insert(join);
            sendBookingSuccessMessage(join, time, meetDay, userId);
        }
    }

    private void sendBookingSuccessMessage(Join join, Time time, String meetDay, String userId) {
        try {
            String visitorName = extractVisitorName(join.getJoinForms());
            String titleName = resolveVenueTitle(time);

            String finalTitle = visitorName + "，您已成功预约 " + titleName;
            String timeRange = meetDay + " " + time.getTimeStart() + "-" + time.getTimeEnd();

            messageService.createMessage(userId, "BOOKING_SUCCESS", finalTitle, timeRange);
        } catch (Exception e) {
            logger.error("发送预约成功消息失败", e);
        }
    }

    // ==================== 查询：我的预约 ====================

    @Override
    public List<Join> getMyBookings(String userId) {
        List<Join> list = joinMapper.selectUserJoinList(userId);
        if (CollUtil.isEmpty(list)) {
            return list;
        }

        Map<String, Museum> museumCache = batchLoadMuseums(list);
        enrichBookingsWithMuseumInfo(list, museumCache);
        return list;
    }

    private Map<String, Museum> batchLoadMuseums(List<Join> bookings) {
        Set<String> timeMarks = bookings.stream()
                .map(Join::getTimeMark)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());

        List<Time> timeList = timeMapper.selectList(
                new QueryWrapper<Time>().in("TIME_MARK", timeMarks));

        Set<String> museumIds = timeList.stream()
                .map(Time::getMuseumId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());

        if (museumIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Museum> museums = museumMapper.selectList(
                new QueryWrapper<Museum>().in("MUSEUM_ID", museumIds));

        Map<String, Museum> cache = new HashMap<>();
        for (Museum museum : museums) {
            cache.put(museum.getMuseumId(), museum);
        }
        return cache;
    }

    private void enrichBookingsWithMuseumInfo(List<Join> bookings,
                                               Map<String, Museum> museumCache) {
        Map<String, Time> timeCache = new HashMap<>();

        for (Join booking : bookings) {
            if (StrUtil.isBlank(booking.getTimeMark())) {
                continue;
            }

            Time time = timeCache.computeIfAbsent(booking.getTimeMark(), mark ->
                    timeMapper.selectOne(
                            new QueryWrapper<Time>().eq("TIME_MARK", mark)));

            if (time == null || StrUtil.isBlank(time.getMuseumId())) {
                continue;
            }

            Museum museum = museumCache.get(time.getMuseumId());
            if (museum == null) {
                continue;
            }

            booking.setMuseumTitle(museum.getMuseumTitle());
            booking.setLatitude(museum.getLatitude());
            booking.setLongitude(museum.getLongitude());
            booking.setMuseumAddress(resolveAddress(museum));
        }
    }

    private String resolveAddress(Museum museum) {
        try {
            String objStr = museum.getMuseumObj();
            if (StrUtil.isNotBlank(objStr)) {
                JSONObject obj = JSONUtil.parseObj(objStr);
                if (StrUtil.isNotBlank(obj.getStr("address"))) {
                    return obj.getStr("address");
                }
            }
        } catch (Exception ignored) {
            logger.debug("解析场馆地址失败, museumId={}", museum.getMuseumId());
        }
        return museum.getAddress();
    }

    // ==================== 取消预约 ====================

    @Override
    public void cancelBooking(String userId, String joinId) {
        Join join = findBookingOwnedByUser(userId, joinId);
        validateCancellable(join);

        String timeMark = join.getTimeMark();
        String meetDay = join.getJoinMeetDay();
        String identityId = join.getIdentityId();

        transactionTemplate.executeWithoutResult(status -> {
            join.setJoinStatus(JoinStatus.CANCELLED.getCode());
            join.setJoinEditTime(System.currentTimeMillis());
            joinMapper.updateById(join);
            bookingStockService.incrMysqlSuccCnt(timeMark, -1);
            sendCancelMessage(userId, join);
        });

        bookingStockService.compensate(timeMark, meetDay, Collections.singletonList(identityId));
    }

    private Join findBookingOwnedByUser(String userId, String joinId) {
        Join join = joinMapper.selectOne(
                new QueryWrapper<Join>()
                        .eq("USER_ID", userId)
                        .and(w -> w.eq("JOIN_ID", joinId).or().eq("_id", joinId)));
        if (join == null) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_FOUND);
        }
        return join;
    }

    private void validateCancellable(Join join) {
        if (!JoinStatus.SUCCESS.getCode().equals(join.getJoinStatus())) {
            throw new BusinessException(ErrorCode.BOOKING_STATUS_INVALID);
        }
        if (!CheckinStatus.UNCHECKED.getCode().equals(join.getJoinIsCheckin())) {
            throw new BusinessException(ErrorCode.BOOKING_ALREADY_CHECKED_IN);
        }
    }

    private void sendCancelMessage(String userId, Join join) {
        try {
            Time time = timeMapper.selectOne(
                    new QueryWrapper<Time>().eq("TIME_MARK", join.getTimeMark()));
            String titleName = time != null ? resolveVenueTitle(time) : "预约";
            messageService.createMessage(userId, "BOOKING_CANCEL", titleName);
        } catch (Exception e) {
            logger.error("发送预约取消消息失败", e);
        }
    }

    // ==================== 核销 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkin(String id) {
        String lockKey = BookingConstant.CHECKIN_LOCK_KEY_PREFIX + id;
        boolean locked = tryAcquireCheckinLock(lockKey);
        if (!locked) {
            throw new BusinessException(ErrorCode.CHECKIN_BUSY);
        }

        try {
            Join join = findBookingById(id);
            validateCheckinAllowed(join);
            markBookingCheckedIn(join);
        } finally {
            releaseCheckinLock(lockKey);
        }
    }

    private boolean tryAcquireCheckinLock(String lockKey) {
        try {
            Boolean success = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", BookingConstant.CHECKIN_LOCK_TTL_SECONDS, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            logger.error("核销时 Redis 不可用: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "Redis 不可用，无法完成核销");
        }
    }

    private void releaseCheckinLock(String lockKey) {
        try {
            stringRedisTemplate.delete(lockKey);
        } catch (Exception e) {
            logger.error("释放核销锁失败, lockKey={}: {}", lockKey, e.getMessage());
        }
    }

    private Join findBookingById(String id) {
        Join join = joinMapper.selectOne(
                new QueryWrapper<Join>()
                        .and(w -> w.eq("JOIN_ID", id).or().eq("_id", id)));
        if (join == null) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_FOUND);
        }
        return join;
    }

    private void validateCheckinAllowed(Join join) {
        if (!JoinStatus.SUCCESS.getCode().equals(join.getJoinStatus())) {
            throw new BusinessException(500, "非预约成功状态，无法核销");
        }
        if (CheckinStatus.CHECKED_IN.getCode().equals(join.getJoinIsCheckin())) {
            throw new BusinessException(ErrorCode.BOOKING_ALREADY_DONE);
        }
        if (CheckinStatus.EXPIRED.getCode().equals(join.getJoinIsCheckin())) {
            throw new BusinessException(ErrorCode.BOOKING_EXPIRED);
        }
    }

    private void markBookingCheckedIn(Join join) {
        join.setJoinIsCheckin(CheckinStatus.CHECKED_IN.getCode());
        join.setJoinEditTime(System.currentTimeMillis());
        joinMapper.updateById(join);
    }

    // ==================== 管理端 ====================

    @Override
    public Page<Join> adminList(String keyword, Integer page, Integer limit) {
        List<Join> allList = joinMapper.searchByKeyword(keyword);
        if (allList == null) {
            allList = new ArrayList<>();
        }

        int total = allList.size();
        List<Join> pagedList = allList.stream()
                .skip((long) (page - 1) * limit)
                .limit(limit)
                .collect(Collectors.toList());

        Page<Join> pageResult = new Page<>(page, limit, total);
        pageResult.setRecords(pagedList);
        return pageResult;
    }

    @Override
    public List<Join> getAllForExport() {
        List<Join> allList = joinMapper.searchByKeyword("");
        return allList != null ? allList : new ArrayList<>();
    }

    // ==================== 公用工具方法 ====================

    private String resolveVenueTitle(Time time) {
        if (time == null) {
            return "博物馆";
        }

        if (StrUtil.isNotBlank(time.getActivityId())) {
            Activity activity = activityMapper.selectOne(
                    new QueryWrapper<Activity>().eq("ACTIVITY_ID", time.getActivityId()));
            if (activity != null) {
                return activity.getActivityTitle();
            }
        }

        if (StrUtil.isNotBlank(time.getMuseumId())) {
            Museum museum = museumMapper.selectOne(
                    new QueryWrapper<Museum>().eq("MUSEUM_ID", time.getMuseumId()));
            if (museum != null) {
                return museum.getMuseumTitle();
            }
        }

        return "博物馆";
    }

    private String extractVisitorName(String joinForms) {
        if (StrUtil.isNotBlank(joinForms)) {
            JSONObject form = JSONUtil.parseObj(joinForms);
            return form.getStr("name", "游客");
        }
        return "游客";
    }
}
