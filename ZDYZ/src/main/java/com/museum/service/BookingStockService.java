package com.museum.service;

import java.util.List;

/**
 * 预约库存：Redis 预扣 + MySQL 原子 succCnt + 补偿
 */
public interface BookingStockService {

    /**
     * SETNX 预热 booking:stock:{timeMark}，已存在则不覆盖
     */
    void warmUpIfAbsent(String timeMark, int remain);

    /**
     * Lua 原子预扣：判余票、判一证一约、扣库存、写 booked（带 TTL）
     */
    void tryReserve(String timeMark, String day, List<String> identityIds, long bookedTtlSeconds);

    /**
     * 补偿：回补库存并删除 booked；失败只打 error 日志
     */
    void compensate(String timeMark, String day, List<String> identityIds);

    /**
     * 读取 Redis 剩余名额；key 不存在返回 null
     */
    Integer getRedisRemain(String timeMark);

    /**
     * MySQL 原子更新 SUCC_CNT = SUCC_CNT + delta（禁止 updateById 整行回写）
     */
    void incrMysqlSuccCnt(String timeMark, int delta);
}
