package com.museum.booking;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.museum.common.enums.IdentityStatus;
import com.museum.common.enums.JoinStatus;
import com.museum.common.exception.BusinessException;
import com.museum.common.exception.ErrorCode;
import com.museum.entity.*;
import com.museum.mapper.*;
import com.museum.service.BookingStockService;
import com.museum.service.impl.JoinServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static com.museum.booking.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JoinServiceImpl 单元测试 —— Redis 预扣 + MySQL 事务链路
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JoinServiceImpl —— 核心预约提交链路")
class JoinServiceImplTest {

    @InjectMocks
    private JoinServiceImpl joinService;

    @Mock
    private DayMapper dayMapper;
    @Mock
    private TimeMapper timeMapper;
    @Mock
    private IdentityMapper identityMapper;
    @Mock
    private JoinMapper joinMapper;
    @Mock
    private MuseumMapper museumMapper;
    @Mock
    private ActivityMapper activityMapper;
    @Mock
    private com.museum.service.MessageService messageService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private BookingStockService bookingStockService;
    @Mock
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("提交空参观人列表 → BOOKING_NO_VISITORS")
    void submitBooking_emptyVisitors_throwsNoVisitors() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                joinService.submitBooking("user123", "TM_202506140900", Collections.emptyList()));

        assertEquals(ErrorCode.BOOKING_NO_VISITORS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("预约 4 人 → BOOKING_TOO_MANY")
    void submitBooking_exceedMaxBookingCount_throwsTooMany() {
        List<String> identityIds = Arrays.asList("ID_001", "ID_002", "ID_003", "ID_004");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                joinService.submitBooking("user123", "TM_202506140900", identityIds));

        assertEquals(ErrorCode.BOOKING_TOO_MANY.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("请求内重复游客 → IDENTITY_DUPLICATE_IN_REQUEST")
    void submitBooking_duplicateInRequest_throws() {
        List<String> identityIds = Arrays.asList("ID_001", "ID_001");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                joinService.submitBooking("user123", "TM_202506140900", identityIds));

        assertEquals(ErrorCode.IDENTITY_DUPLICATE_IN_REQUEST.getCode(), ex.getCode());
        verify(bookingStockService, never()).tryReserve(anyString(), anyString(), anyList(), anyLong());
    }

    @Test
    @DisplayName("黑名单游客 → IDENTITY_BLACKLISTED")
    @SuppressWarnings("unchecked")
    void submitBooking_blacklistedVisitor_throwsBlacklisted() {
        List<String> identityIds = Collections.singletonList("ID_BLACK");
        Identity blacklisted = buildIdentity("ID_BLACK", "张三", IdentityStatus.BLACKLISTED);

        Time mockTime = buildTime("TM_202506140900", "T01", 100, 10);
        when(timeMapper.selectOne(argThat((QueryWrapper<Time> w) -> true))).thenReturn(mockTime);
        when(dayMapper.selectOne(argThat((QueryWrapper<Day> w) -> true)))
                .thenReturn(buildDay("T01", "2025-06-14"));
        when(identityMapper.selectOne(argThat((QueryWrapper<Identity> w) -> true)))
                .thenReturn(blacklisted);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                joinService.submitBooking("user123", "TM_202506140900", identityIds));

        assertEquals(ErrorCode.IDENTITY_BLACKLISTED.getCode(), ex.getCode());
        verify(bookingStockService, never()).tryReserve(anyString(), anyString(), anyList(), anyLong());
    }

    @Test
    @DisplayName("同日库内已有预约 → IDENTITY_DUPLICATE_BOOKING")
    @SuppressWarnings("unchecked")
    void submitBooking_duplicateBooking_throwsDuplicate() {
        List<String> identityIds = Collections.singletonList("ID_001");
        Identity normalVisitor = buildIdentity("ID_001", "李四", IdentityStatus.NORMAL);

        when(timeMapper.selectOne(argThat((QueryWrapper<Time> w) -> true)))
                .thenReturn(buildTime("TM_202506140900", "T01", 100, 10));
        when(dayMapper.selectOne(argThat((QueryWrapper<Day> w) -> true)))
                .thenReturn(buildDay("T01", "2025-06-14"));
        when(identityMapper.selectOne(argThat((QueryWrapper<Identity> w) -> true)))
                .thenReturn(normalVisitor);
        when(joinMapper.selectCount(argThat((QueryWrapper<Join> w) -> true))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                joinService.submitBooking("user123", "TM_202506140900", identityIds));

        assertEquals(ErrorCode.IDENTITY_DUPLICATE_BOOKING.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("无效时段 → BOOKING_SLOT_INVALID")
    @SuppressWarnings("unchecked")
    void submitBooking_invalidTimeSlot_throwsSlotInvalid() {
        when(timeMapper.selectOne(argThat((QueryWrapper<Time> w) -> true))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                joinService.submitBooking("user123", "INVALID_TIME",
                        Collections.singletonList("ID_001")));

        assertEquals(ErrorCode.BOOKING_SLOT_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("标准预约成功：warm + reserve + 事务落单 + incrSuccCnt")
    @SuppressWarnings("unchecked")
    void submitBooking_validSingleVisitor_passesAllChecks() {
        List<String> identityIds = Collections.singletonList("ID_OK");
        Identity normalVisitor = buildIdentity("ID_OK", "王五", IdentityStatus.NORMAL);
        Time mockTime = buildTime("TM_202506140900", "T01", 100, 10);

        when(timeMapper.selectOne(argThat((QueryWrapper<Time> w) -> true))).thenReturn(mockTime);
        when(dayMapper.selectOne(argThat((QueryWrapper<Day> w) -> true)))
                .thenReturn(buildDay("T01", "2025-06-14"));
        when(identityMapper.selectOne(argThat((QueryWrapper<Identity> w) -> true)))
                .thenReturn(normalVisitor);
        when(joinMapper.selectCount(argThat((QueryWrapper<Join> w) -> true))).thenReturn(0L);
        when(joinMapper.insert(any(Join.class))).thenReturn(1);
        doNothing().when(bookingStockService).warmUpIfAbsent(anyString(), anyInt());
        doNothing().when(bookingStockService).tryReserve(anyString(), anyString(), anyList(), anyLong());
        doNothing().when(bookingStockService).incrMysqlSuccCnt(anyString(), anyInt());
        doNothing().when(messageService).createMessage(anyString(), anyString(), anyString(), anyString());
        stubTxRunsCallback();

        assertDoesNotThrow(() ->
                joinService.submitBooking("user123", "TM_202506140900", identityIds));

        verify(bookingStockService).warmUpIfAbsent(eq("TM_202506140900"), eq(90));
        verify(bookingStockService).tryReserve(eq("TM_202506140900"), eq("2025-06-14"),
                eq(identityIds), anyLong());
        verify(bookingStockService).incrMysqlSuccCnt("TM_202506140900", 1);
        verify(bookingStockService, never()).compensate(anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("MySQL 事务失败后调用 compensate")
    @SuppressWarnings("unchecked")
    void submitBooking_mysqlFail_triggersCompensate() {
        List<String> identityIds = Collections.singletonList("ID_OK");
        Identity normalVisitor = buildIdentity("ID_OK", "王五", IdentityStatus.NORMAL);

        when(timeMapper.selectOne(argThat((QueryWrapper<Time> w) -> true)))
                .thenReturn(buildTime("TM_202506140900", "T01", 100, 10));
        when(dayMapper.selectOne(argThat((QueryWrapper<Day> w) -> true)))
                .thenReturn(buildDay("T01", "2025-06-14"));
        when(identityMapper.selectOne(argThat((QueryWrapper<Identity> w) -> true)))
                .thenReturn(normalVisitor);
        when(joinMapper.selectCount(argThat((QueryWrapper<Join> w) -> true))).thenReturn(0L);
        doNothing().when(bookingStockService).warmUpIfAbsent(anyString(), anyInt());
        doNothing().when(bookingStockService).tryReserve(anyString(), anyString(), anyList(), anyLong());
        doNothing().when(bookingStockService).compensate(anyString(), anyString(), anyList());

        doAnswer(invocation -> {
            throw new RuntimeException("db down");
        }).when(transactionTemplate).executeWithoutResult(any());

        assertThrows(RuntimeException.class, () ->
                joinService.submitBooking("user123", "TM_202506140900", identityIds));

        verify(bookingStockService).compensate(eq("TM_202506140900"), eq("2025-06-14"), eq(identityIds));
    }

    @Test
    @DisplayName("取消预约：MySQL 成功后 Redis compensate")
    @SuppressWarnings("unchecked")
    void cancelBooking_callsCompensateAfterMysql() {
        Join join = new Join();
        join.setJoinId("join_1");
        join.setUserId("user123");
        join.setIdentityId("ID_OK");
        join.setJoinMeetDay("2025-06-14");
        join.setTimeMark("TM_202506140900");
        join.setJoinStatus(JoinStatus.SUCCESS.getCode());
        join.setJoinIsCheckin(0);

        when(joinMapper.selectOne(argThat((QueryWrapper<Join> w) -> true))).thenReturn(join);
        when(joinMapper.updateById(any(Join.class))).thenReturn(1);
        doNothing().when(bookingStockService).incrMysqlSuccCnt("TM_202506140900", -1);
        doNothing().when(bookingStockService).compensate(anyString(), anyString(), anyList());
        stubTxRunsCallback();

        assertDoesNotThrow(() -> joinService.cancelBooking("user123", "join_1"));

        verify(bookingStockService).incrMysqlSuccCnt("TM_202506140900", -1);
        verify(bookingStockService).compensate(eq("TM_202506140900"), eq("2025-06-14"),
                eq(Collections.singletonList("ID_OK")));
    }

    @SuppressWarnings("unchecked")
    private void stubTxRunsCallback() {
        doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }
}
