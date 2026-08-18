package com.museum.booking;

import com.museum.common.constant.BookingConstant;
import com.museum.common.exception.BusinessException;
import com.museum.common.exception.ErrorCode;
import com.museum.mapper.TimeMapper;
import com.museum.service.impl.BookingStockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingStockServiceImpl —— Redis 预扣与原子 succCnt")
class BookingStockServiceImplTest {

    @InjectMocks
    private BookingStockServiceImpl bookingStockService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private TimeMapper timeMapper;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private RedisScript<Long> reserveScript;
    @Mock
    private RedisScript<Long> compensateScript;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingStockService, "reserveScript", reserveScript);
        ReflectionTestUtils.setField(bookingStockService, "compensateScript", compensateScript);
    }

    @Test
    @DisplayName("warmUpIfAbsent 使用 SETNX")
    void warmUpIfAbsent_usesSetIfAbsent() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString())).thenReturn(true);

        bookingStockService.warmUpIfAbsent("TM1", 50);

        verify(valueOperations).setIfAbsent(BookingConstant.stockKey("TM1"), "50");
    }

    @Test
    @DisplayName("tryReserve 余票不足 → BOOKING_SLOT_FULL")
    @SuppressWarnings("unchecked")
    void tryReserve_stockInsufficient() {
        doReturn(-1L).when(stringRedisTemplate).execute(eq(reserveScript), anyList(), any(Object[].class));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                bookingStockService.tryReserve("TM1", "2025-06-14",
                        Collections.singletonList("ID1"), 3600));

        assertEquals(ErrorCode.BOOKING_SLOT_FULL.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("tryReserve 一证一约冲突 → IDENTITY_DUPLICATE_BOOKING")
    @SuppressWarnings("unchecked")
    void tryReserve_alreadyBooked() {
        doReturn(-2L).when(stringRedisTemplate).execute(eq(reserveScript), anyList(), any(Object[].class));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                bookingStockService.tryReserve("TM1", "2025-06-14",
                        Collections.singletonList("ID1"), 3600));

        assertEquals(ErrorCode.IDENTITY_DUPLICATE_BOOKING.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("tryReserve 列表内重复 → IDENTITY_DUPLICATE_IN_REQUEST")
    @SuppressWarnings("unchecked")
    void tryReserve_dupInRequest() {
        doReturn(-3L).when(stringRedisTemplate).execute(eq(reserveScript), anyList(), any(Object[].class));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                bookingStockService.tryReserve("TM1", "2025-06-14",
                        Arrays.asList("ID1", "ID1"), 3600));

        assertEquals(ErrorCode.IDENTITY_DUPLICATE_IN_REQUEST.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("incrMysqlSuccCnt 调用原子 SQL，不走 updateById")
    void incrMysqlSuccCnt_usesAtomicSql() {
        when(timeMapper.incrSuccCnt(eq("TM1"), eq(2), anyLong())).thenReturn(1);

        bookingStockService.incrMysqlSuccCnt("TM1", 2);

        verify(timeMapper).incrSuccCnt(eq("TM1"), eq(2), anyLong());
        verify(timeMapper, never()).updateById(any(com.museum.entity.Time.class));
    }

    @Test
    @DisplayName("compensate 失败只打日志不抛异常")
    @SuppressWarnings("unchecked")
    void compensate_failureSwallowed() {
        doThrow(new RuntimeException("redis down"))
                .when(stringRedisTemplate).execute(eq(compensateScript), anyList(), any(Object[].class));

        assertDoesNotThrow(() ->
                bookingStockService.compensate("TM1", "2025-06-14", Collections.singletonList("ID1")));
    }

    @Test
    @DisplayName("tryReserve 成功时传入 booked keys")
    @SuppressWarnings("unchecked")
    void tryReserve_passesBookedKeys() {
        doReturn(1L).when(stringRedisTemplate).execute(eq(reserveScript), anyList(), any(Object[].class));

        bookingStockService.tryReserve("TM1", "2025-06-14", Arrays.asList("A", "B"), 7200);

        verify(stringRedisTemplate).execute(
                eq(reserveScript),
                eq(Collections.singletonList(BookingConstant.stockKey("TM1"))),
                eq("2"),
                eq("7200"),
                eq(BookingConstant.bookedKey("2025-06-14", "A")),
                eq(BookingConstant.bookedKey("2025-06-14", "B")));
    }
}
