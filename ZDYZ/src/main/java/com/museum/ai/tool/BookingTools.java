package com.museum.ai.tool;

import cn.hutool.core.util.StrUtil;
import com.museum.ai.context.UserContext;
import com.museum.ai.converter.BookingToolConverter;
import com.museum.ai.dto.*;
import com.museum.ai.support.AiChatBlockCollector;
import com.museum.ai.trace.AiDebugTraceContext;
import com.museum.common.exception.BusinessException;
import com.museum.entity.Join;
import com.museum.service.JoinService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Booking agent tool layer.
 * It reuses JoinService and only adds a side-channel block collector for UI rendering.
 */
@Component
public class BookingTools {

    private final JoinService joinService;
    private final BookingToolConverter converter;

    public BookingTools(JoinService joinService, BookingToolConverter converter) {
        this.joinService = joinService;
        this.converter = converter;
    }

    @Tool(description = "查询近几日可预约日期及开闭馆状态（OPEN/CLOSED）")
    public ToolResult<QueryDaysData> queryDays() {
        return traced("queryDays", Map.of(), () -> {
            try {
                List<Map<String, Object>> bizDays = joinService.getBookingDays();
                return ToolResult.ok(converter.toDaySlots(bizDays));
            } catch (BusinessException e) {
                return castFail(converter.fromBusinessException(e));
            } catch (Exception e) {
                return ToolResult.fail(ToolError.INTERNAL_ERROR, safeMessage(e));
            }
        });
    }

    @Tool(description = "按日期查询各时段余票，day 格式 yyyy-MM-dd；返回 remain 与 AVAILABLE/FULL")
    public ToolResult<QueryTimesData> queryTimes(
            @ToolParam(description = "参观日期，yyyy-MM-dd") String day) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("day", day);
        return traced("queryTimes", input, () -> {
            ToolResult<QueryTimesData> invalid = converter.validateQueryTimesDay(day);
            if (invalid != null) {
                return invalid;
            }
            try {
                String dayTrimmed = day.trim();
                List<Map<String, Object>> bizTimes = joinService.getBookingTimes(dayTrimmed);
                QueryTimesData data = converter.toTimeSlots(dayTrimmed, bizTimes);
                AiChatBlockCollector.recordQueryTimes(data);
                return ToolResult.ok(data);
            } catch (BusinessException e) {
                return castFail(converter.fromBusinessException(e));
            } catch (Exception e) {
                return ToolResult.fail(ToolError.INTERNAL_ERROR, safeMessage(e));
            }
        });
    }

    @Tool(description = "为当前登录用户提交预约；需 timeMark 与 identityIds（1~3）；成功不返回预约号，需再 listRecords")
    public ToolResult<SubmitBookingData> submitBooking(
            @ToolParam(description = "时段 timeMark，来自 queryTimes") String timeMark,
            @ToolParam(description = "游客 identityId 列表，1~3 个且不可重复") List<String> identityIds) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("hasTimeMark", StrUtil.isNotBlank(timeMark));
        input.put("identityCount", identityIds == null ? 0 : identityIds.size());
        return traced("submitBooking", input, () -> {
            String userId = requireUserId();
            if (userId == null) {
                return unauthorized();
            }
            try {
                joinService.submitBooking(userId, timeMark, identityIds);
                int visitorCount = identityIds == null ? 0 : identityIds.size();
                SubmitBookingData data = converter.toSubmitSuccess(timeMark, visitorCount);
                AiChatBlockCollector.recordSubmitBooking(data, "预约成功");
                return ToolResult.ok("预约成功", data);
            } catch (BusinessException e) {
                return castFail(converter.fromBusinessException(e));
            } catch (Exception e) {
                return ToolResult.fail(ToolError.INTERNAL_ERROR, safeMessage(e));
            }
        });
    }

    @Tool(description = "列出当前登录用户的预约记录；可选按 day、status(BOOKED/CANCELLED) 过滤")
    public ToolResult<ListRecordsData> listRecords(
            @ToolParam(description = "可选，按参观日 yyyy-MM-dd 过滤") String day,
            @ToolParam(description = "可选，BOOKED 或 CANCELLED") BookingStatus status) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("filterDay", day);
        input.put("hasStatus", status != null);
        return traced("listRecords", input, () -> {
            String userId = requireUserId();
            if (userId == null) {
                return unauthorized();
            }
            try {
                List<Join> joins = joinService.getMyBookings(userId);
                ListRecordsData data = converter.toBookingRecords(joins, day, status);
                AiChatBlockCollector.recordListRecords(data);
                return ToolResult.ok(data);
            } catch (BusinessException e) {
                return castFail(converter.fromBusinessException(e));
            } catch (Exception e) {
                return ToolResult.fail(ToolError.INTERNAL_ERROR, safeMessage(e));
            }
        });
    }

    @Tool(description = "取消当前登录用户的一条预约；joinId 必须来自 listRecords")
    public ToolResult<CancelBookingData> cancelBooking(
            @ToolParam(description = "业务预约号 joinId") String joinId) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("hasJoinId", StrUtil.isNotBlank(joinId));
        return traced("cancelBooking", input, () -> {
            String userId = requireUserId();
            if (userId == null) {
                return unauthorized();
            }
            if (StrUtil.isBlank(joinId)) {
                return ToolResult.fail(ToolError.BAD_REQUEST, "预约号不能为空");
            }
            try {
                joinService.cancelBooking(userId, joinId.trim());
                AiChatBlockCollector.recordCancelBooking(joinId.trim(), "取消成功");
                return ToolResult.ok("取消成功", converter.toCancelSuccess(joinId.trim()));
            } catch (BusinessException e) {
                return castFail(converter.fromBusinessException(e));
            } catch (Exception e) {
                return ToolResult.fail(ToolError.INTERNAL_ERROR, safeMessage(e));
            }
        });
    }

    private static <T> ToolResult<T> traced(String name, Map<String, Object> inputSummary, Supplier<ToolResult<T>> action) {
        long startedAt = System.currentTimeMillis();
        ToolResult<T> result = action.get();
        AiDebugTraceContext.recordTool(name, startedAt, inputSummary, result);
        return result;
    }

    private static String requireUserId() {
        String userId = UserContext.get();
        return StrUtil.isBlank(userId) ? null : userId;
    }

    private static <T> ToolResult<T> unauthorized() {
        return ToolResult.fail(ToolError.UNAUTHORIZED, "未登录或 Token 无效");
    }

    @SuppressWarnings("unchecked")
    private static <T> ToolResult<T> castFail(ToolResult<?> fail) {
        return (ToolResult<T>) fail;
    }

    private static String safeMessage(Exception e) {
        return StrUtil.blankToDefault(e.getMessage(), ToolError.INTERNAL_ERROR.name());
    }
}
