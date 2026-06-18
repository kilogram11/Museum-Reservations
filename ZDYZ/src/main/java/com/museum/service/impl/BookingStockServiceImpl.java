package com.museum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.museum.common.exception.BusinessException;
import com.museum.common.exception.ErrorCode;
import com.museum.entity.Time;
import com.museum.mapper.TimeMapper;
import com.museum.service.BookingStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BookingStockServiceImpl implements BookingStockService {

    @Autowired
    private TimeMapper timeMapper;

    @Override
    public void checkSufficient(Time time, int need) {
        if (time.getSuccCnt() + need > time.getLimitCnt()) {
            throw new BusinessException(ErrorCode.BOOKING_SLOT_FULL);
        }
    }

    @Override
    public void deduct(Time time, int need) {
        time.setSuccCnt(time.getSuccCnt() + need);
        timeMapper.updateById(time);
    }

    @Override
    public void rollback(String timeMark) {
        Time time = timeMapper.selectOne(
                new QueryWrapper<Time>().eq("TIME_MARK", timeMark));
        if (time != null && time.getSuccCnt() > 0) {
            time.setSuccCnt(time.getSuccCnt() - 1);
            timeMapper.updateById(time);
        }
    }
}
