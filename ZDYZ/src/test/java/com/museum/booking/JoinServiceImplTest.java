package com.museum.booking;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.museum.common.enums.IdentityStatus;
import com.museum.common.exception.BusinessException;
import com.museum.common.exception.ErrorCode;
import com.museum.entity.*;
import com.museum.mapper.*;
import com.museum.service.BookingStockService;
import com.museum.service.impl.JoinServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.museum.booking.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JoinServiceImpl 单元测试 —— 核心预约提交链路
 * <p>
 * 综合运用黑盒测试（等价类划分、边界值分析）与白盒测试（基本路径覆盖、条件分支覆盖）。
 * </p>
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
    private ValueOperations<String, String> valueOperations;

    // ================================================================
    //  测试方法
    // ================================================================

    /**
     * 【黑盒测试 · 等价类划分】—— 无效等价类：空参观人列表
     */
    @Test
    @DisplayName("A-1 黑盒/等价类-无效: 提交空参观人列表 → BOOKING_NO_VISITORS")
    void submitBooking_emptyVisitors_throwsNoVisitors() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                joinService.submitBooking("user123", "TM_202506140900", Collections.emptyList()));

        assertEquals(ErrorCode.BOOKING_NO_VISITORS.getCode(), ex.getCode());
        assertEquals(ErrorCode.BOOKING_NO_VISITORS.getMessage(), ex.getMessage());
    }

    /**
     * 【黑盒测试 · 边界值分析】—— 有效边界：恰好 3 人
     */
    @Test
    @DisplayName("A-2 黑盒/边界值-有效: 预约恰好 3 人（MAX_BOOKING_COUNT）→ 通过人数校验")
    void submitBooking_exactlyMaxBookingCount_passesValidation() {
        List<String> identityIds = Arrays.asList("ID_001", "ID_002", "ID_003");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                joinService.submitBooking("user123", "TM_202506140900", identityIds));

        assertNotEquals(ErrorCode.BOOKING_TOO_MANY.getCode(), ex.getCode(),
                "恰好 3 人不应该触发 BOOKING_TOO_MANY");
    }

    /**
     * 【黑盒测试 · 边界值分析】—— 无效边界：4 人
     */
    @Test
    @DisplayName("A-3 黑盒/边界值-无效: 预约 4 人（超过 MAX_BOOKING_COUNT）→ BOOKING_TOO_MANY")
    void submitBooking_exceedMaxBookingCount_throwsTooMany() {
        List<String> identityIds = Arrays.asList("ID_001", "ID_002", "ID_003", "ID_004");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                joinService.submitBooking("user123", "TM_202506140900", identityIds));

        assertEquals(ErrorCode.BOOKING_TOO_MANY.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains(ErrorCode.BOOKING_TOO_MANY.getMessage()));
    }

    /**
     * 【白盒测试 · 条件分支覆盖】—— 游客黑名单检查
     */
    @Test
    @DisplayName("A-4 白盒/条件分支: 黑名单游客提交 → IDENTITY_BLACKLISTED")
    @SuppressWarnings("unchecked")
    void submitBooking_blacklistedVisitor_throwsBlacklisted() {
        List<String> identityIds = Collections.singletonList("ID_BLACK");
        Identity blacklisted = buildIdentity("ID_BLACK", "张三", IdentityStatus.BLACKLISTED);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        Time mockTime = buildTime("TM_202506140900", "T01", 100, 10);
        when(timeMapper.selectOne(argThat((QueryWrapper<Time> w) -> true)))
                .thenReturn(mockTime);

        Day mockDay = buildDay("T01", "2025-06-14");
        when(dayMapper.selectOne(argThat((QueryWrapper<Day> w) -> true)))
                .thenReturn(mockDay);

        doNothing().when(bookingStockService).checkSufficient(any(Time.class), anyInt());

        when(identityMapper.selectOne(argThat((QueryWrapper<Identity> w) -> true)))
                .thenReturn(blacklisted);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                joinService.submitBooking("user123", "TM_202506140900", identityIds));

        assertEquals(ErrorCode.IDENTITY_BLACKLISTED.getCode(), ex.getCode());
    }

    /**
     * 【白盒测试 · 条件分支覆盖】—— 重复预约检查
     */
    @Test
    @DisplayName("A-5 白盒/条件分支: 同日重复预约 → IDENTITY_DUPLICATE_BOOKING")
    @SuppressWarnings("unchecked")
    void submitBooking_duplicateBooking_throwsDuplicate() {
        List<String> identityIds = Collections.singletonList("ID_001");
        Identity normalVisitor = buildIdentity("ID_001", "李四", IdentityStatus.NORMAL);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        Time mockTime = buildTime("TM_202506140900", "T01", 100, 10);
        when(timeMapper.selectOne(argThat((QueryWrapper<Time> w) -> true)))
                .thenReturn(mockTime);

        Day mockDay = buildDay("T01", "2025-06-14");
        when(dayMapper.selectOne(argThat((QueryWrapper<Day> w) -> true)))
                .thenReturn(mockDay);

        doNothing().when(bookingStockService).checkSufficient(any(Time.class), anyInt());

        when(identityMapper.selectOne(argThat((QueryWrapper<Identity> w) -> true)))
                .thenReturn(normalVisitor);

        when(joinMapper.selectCount(argThat((QueryWrapper<Join> w) -> true)))
                .thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                joinService.submitBooking("user123", "TM_202506140900", identityIds));

        assertEquals(ErrorCode.IDENTITY_DUPLICATE_BOOKING.getCode(), ex.getCode());
    }

    /**
     * 【白盒测试 · 基本路径覆盖】—— 时段无效分支
     */
    @Test
    @DisplayName("A-6 白盒/基本路径: 无效时段 → BOOKING_SLOT_INVALID")
    @SuppressWarnings("unchecked")
    void submitBooking_invalidTimeSlot_throwsSlotInvalid() {
        List<String> identityIds = Collections.singletonList("ID_001");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(timeMapper.selectOne(argThat((QueryWrapper<Time> w) -> true)))
                .thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                joinService.submitBooking("user123", "INVALID_TIME", identityIds));

        assertEquals(ErrorCode.BOOKING_SLOT_INVALID.getCode(), ex.getCode());
    }

    /**
     * 【黑盒测试 · 等价类划分】—— 有效等价类：标准预约完整成功路径
     */
    @Test
    @DisplayName("A-7 黑盒/等价类-有效: 标准预约流程（1人正常身份无重复）→ 通过业务校验")
    @SuppressWarnings("unchecked")
    void submitBooking_validSingleVisitor_passesAllChecks() {
        List<String> identityIds = Collections.singletonList("ID_OK");
        Identity normalVisitor = buildIdentity("ID_OK", "王五", IdentityStatus.NORMAL);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        Time mockTime = buildTime("TM_202506140900", "T01", 100, 10);
        Day mockDay = buildDay("T01", "2025-06-14");
        when(timeMapper.selectOne(argThat((QueryWrapper<Time> w) -> true)))
                .thenReturn(mockTime);
        when(dayMapper.selectOne(argThat((QueryWrapper<Day> w) -> true)))
                .thenReturn(mockDay);

        doNothing().when(bookingStockService).checkSufficient(any(Time.class), anyInt());

        when(identityMapper.selectOne(argThat((QueryWrapper<Identity> w) -> true)))
                .thenReturn(normalVisitor);
        when(joinMapper.selectCount(argThat((QueryWrapper<Join> w) -> true)))
                .thenReturn(0L);

        when(joinMapper.insert(any(Join.class))).thenReturn(1);
        doNothing().when(bookingStockService).deduct(any(Time.class), anyInt());
        doNothing().when(messageService).createMessage(anyString(), anyString(), anyString(), anyString());

        assertDoesNotThrow(() ->
                joinService.submitBooking("user123", "TM_202506140900", identityIds));
    }

    /**
     * 【白盒测试 · 分支覆盖】—— 分布式锁超时
     */
    @Test
    @DisplayName("A-8 白盒/分支: 分布式锁超时 → BOOKING_LOCK_FAILED")
    void submitBooking_lockTimeout_throwsLockFailed() {
        List<String> identityIds = Collections.singletonList("ID_001");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                joinService.submitBooking("user123", "TM_202506140900", identityIds));

        assertEquals(ErrorCode.BOOKING_LOCK_FAILED.getCode(), ex.getCode());
    }
}
