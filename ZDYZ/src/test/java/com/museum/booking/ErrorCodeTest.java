package com.museum.booking;

import com.museum.common.exception.BusinessException;
import com.museum.common.exception.ErrorCode;
import com.museum.common.result.Result;
import org.junit.jupiter.api.*;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ErrorCode 单元测试 —— 统一错误码语义映射
 * <p>
 * 验证枚举结构完整性、按域分区规范、Result 工厂方法与 BusinessException 的语义映射。
 * 本类为纯逻辑测试，无需 Mock 依赖。
 * </p>
 */
@DisplayName("ErrorCode —— 统一错误码语义映射")
class ErrorCodeTest {

    // ================================================================
    //  测试方法
    // ================================================================

    /**
     * 【结构完整性】—— code 全局唯一性
     */
    @Test
    @DisplayName("E-1 结构完整性: 枚举值 code 全局唯一")
    void errorCode_values_areUnique() {
        ErrorCode[] allCodes = ErrorCode.values();
        Set<Integer> seen = new HashSet<>();

        for (ErrorCode ec : allCodes) {
            assertFalse(seen.contains(ec.getCode()),
                    "发现重复错误码: " + ec.getCode() + " → " + ec.name());
            seen.add(ec.getCode());
        }
        assertTrue(allCodes.length >= 20, "错误码枚举应包含至少 20 个语义化错误");
    }

    /**
     * 【结构完整性】—— 按域分区规范
     */
    @Test
    @DisplayName("E-2 结构完整性: 错误码分区规范")
    void errorCode_domainPartition_correct() {
        assertTrue(ErrorCode.USER_NOT_FOUND.getCode() >= 1000 && ErrorCode.USER_NOT_FOUND.getCode() < 2000);
        assertTrue(ErrorCode.IDENTITY_BLACKLISTED.getCode() >= 2000 && ErrorCode.IDENTITY_BLACKLISTED.getCode() < 3000);
        assertTrue(ErrorCode.BOOKING_TOO_MANY.getCode() >= 3000 && ErrorCode.BOOKING_TOO_MANY.getCode() < 4000);
        assertTrue(ErrorCode.CHECKIN_BUSY.getCode() >= 4000 && ErrorCode.CHECKIN_BUSY.getCode() < 5000);
    }

    /**
     * 【语义映射】—— Result.error(ErrorCode)
     */
    @Test
    @DisplayName("E-3 语义映射: Result.error(ErrorCode) 正确映射 code 和 message")
    void result_errorWithErrorCode_mapsCorrectly() {
        Result result = Result.error(ErrorCode.BOOKING_TOO_MANY);

        assertEquals(ErrorCode.BOOKING_TOO_MANY.getCode(), result.getCode());
        assertEquals(ErrorCode.BOOKING_TOO_MANY.getMessage(), result.getMsg());
        assertNull(result.getData());
        assertFalse(result.isSuccess());
    }

    /**
     * 【语义映射】—— Result.success()
     */
    @Test
    @DisplayName("E-4 语义映射: Result.success() isSuccess() 返回 true")
    void result_success_isSuccess() {
        Result result = Result.success("操作完成", "data_value");

        assertTrue(result.isSuccess());
        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        assertEquals("操作完成", result.getMsg());
        assertEquals("data_value", result.getData());
    }

    /**
     * 【异常传递】—— BusinessException.of(ErrorCode)
     */
    @Test
    @DisplayName("E-5 异常传递: BusinessException.of(ErrorCode) 携带正确的 code 和 message")
    void businessException_ofErrorCode_carriesCodeAndMessage() {
        BusinessException ex = BusinessException.of(ErrorCode.CHECKIN_BUSY);

        assertEquals(ErrorCode.CHECKIN_BUSY.getCode(), ex.getCode());
        assertEquals(ErrorCode.CHECKIN_BUSY.getMessage(), ex.getMessage());
    }

    /**
     * 【边界值 + 语义验证】—— 核销并发场景
     */
    @Test
    @DisplayName("E-6 核销并发: CHECKIN_BUSY 错误码语义正确")
    void checkinBusy_errorCode_isCorrectForConcurrencyConflict() {
        assertEquals(4001, ErrorCode.CHECKIN_BUSY.getCode().intValue());
        assertTrue(ErrorCode.CHECKIN_BUSY.getMessage().contains("核销"));
        assertTrue(ErrorCode.CHECKIN_BUSY.getMessage().contains("重复"));
    }

    /**
     * 【结构完整性】—— 错误码数量达到重构要求
     */
    @Test
    @DisplayName("E-7 重构验证: ErrorCode 枚举数 ≥ 20")
    void errorCode_count_atLeastTwenty() {
        assertTrue(ErrorCode.values().length >= 20,
                "重构后 ErrorCode 应至少包含 20 个语义化错误码，实际: " + ErrorCode.values().length);
    }
}
