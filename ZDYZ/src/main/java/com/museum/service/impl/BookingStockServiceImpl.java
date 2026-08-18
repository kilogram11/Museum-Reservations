package com.museum.service.impl;

import com.museum.common.constant.BookingConstant;
import com.museum.common.exception.BusinessException;
import com.museum.common.exception.ErrorCode;
import com.museum.mapper.TimeMapper;
import com.museum.service.BookingStockService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class BookingStockServiceImpl implements BookingStockService {

    private static final Logger logger = LoggerFactory.getLogger(BookingStockServiceImpl.class);

    private static final long LUA_OK = 1L;
    private static final long LUA_STOCK_INSUFFICIENT = -1L;
    private static final long LUA_ALREADY_BOOKED = -2L;
    private static final long LUA_DUP_IN_REQUEST = -3L;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private TimeMapper timeMapper;

    private RedisScript<Long> reserveScript;
    private RedisScript<Long> compensateScript;

    @PostConstruct
    void initScripts() {
        DefaultRedisScript<Long> reserve = new DefaultRedisScript<>();
        reserve.setResultType(Long.class);
        reserve.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/booking_reserve.lua")));
        reserveScript = reserve;

        DefaultRedisScript<Long> compensate = new DefaultRedisScript<>();
        compensate.setResultType(Long.class);
        compensate.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/booking_compensate.lua")));
        compensateScript = compensate;
    }

    @Override
    public void warmUpIfAbsent(String timeMark, int remain) {
        String key = BookingConstant.stockKey(timeMark);
        Boolean created = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, String.valueOf(Math.max(0, remain)));
        if (Boolean.TRUE.equals(created)) {
            logger.debug("预热库存 key={}, remain={}", key, remain);
        }
    }

    @Override
    public void tryReserve(String timeMark, String day, List<String> identityIds, long bookedTtlSeconds) {
        String stockKey = BookingConstant.stockKey(timeMark);
        List<String> bookedKeys = toBookedKeys(day, identityIds);
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(identityIds.size()));
        args.add(String.valueOf(Math.max(1L, bookedTtlSeconds)));
        args.addAll(bookedKeys);

        Long result;
        try {
            result = stringRedisTemplate.execute(reserveScript, Collections.singletonList(stockKey),
                    args.toArray());
        } catch (Exception e) {
            logger.error("Redis 预扣失败 timeMark={}, identityIds={}, err={}",
                    timeMark, identityIds, e.getMessage());
            throw new BusinessException(ErrorCode.BOOKING_STOCK_REDIS_ERROR);
        }

        if (result == null) {
            throw new BusinessException(ErrorCode.BOOKING_STOCK_REDIS_ERROR);
        }
        if (result == LUA_OK) {
            return;
        }
        if (result == LUA_STOCK_INSUFFICIENT) {
            throw new BusinessException(ErrorCode.BOOKING_SLOT_FULL);
        }
        if (result == LUA_ALREADY_BOOKED || result == LUA_DUP_IN_REQUEST) {
            throw new BusinessException(result == LUA_DUP_IN_REQUEST
                    ? ErrorCode.IDENTITY_DUPLICATE_IN_REQUEST
                    : ErrorCode.IDENTITY_DUPLICATE_BOOKING);
        }
        throw new BusinessException(ErrorCode.BOOKING_STOCK_REDIS_ERROR);
    }

    @Override
    public void compensate(String timeMark, String day, List<String> identityIds) {
        if (identityIds == null || identityIds.isEmpty()) {
            return;
        }
        String stockKey = BookingConstant.stockKey(timeMark);
        List<String> bookedKeys = toBookedKeys(day, identityIds);
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(identityIds.size()));
        args.addAll(bookedKeys);

        try {
            stringRedisTemplate.execute(compensateScript, Collections.singletonList(stockKey),
                    args.toArray());
        } catch (Exception e) {
            logger.error("库存补偿失败，需人工/定时对账 timeMark={}, identityIds={}, need={}, err={}",
                    timeMark, identityIds, identityIds.size(), e.getMessage(), e);
        }
    }

    @Override
    public Integer getRedisRemain(String timeMark) {
        String value = stringRedisTemplate.opsForValue().get(BookingConstant.stockKey(timeMark));
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("库存值非法 timeMark={}, value={}", timeMark, value);
            return null;
        }
    }

    @Override
    public void incrMysqlSuccCnt(String timeMark, int delta) {
        int rows = timeMapper.incrSuccCnt(timeMark, delta, System.currentTimeMillis());
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.BOOKING_SLOT_FULL.getCode(),
                    "更新预约成功人数失败");
        }
    }

    private List<String> toBookedKeys(String day, List<String> identityIds) {
        List<String> keys = new ArrayList<>(identityIds.size());
        for (String identityId : identityIds) {
            keys.add(BookingConstant.bookedKey(day, identityId));
        }
        return keys;
    }
}
