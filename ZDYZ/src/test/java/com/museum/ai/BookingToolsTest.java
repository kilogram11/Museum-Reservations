package com.museum.ai;

import com.museum.ai.context.UserContext;
import com.museum.ai.converter.BookingToolConverter;
import com.museum.ai.dto.*;
import com.museum.ai.rag.model.ChatIntent;
import com.museum.ai.tool.BookingTools;
import com.museum.ai.trace.AiDebugTrace;
import com.museum.ai.trace.AiDebugTraceContext;
import com.museum.ai.trace.ToolTraceEntry;
import com.museum.common.exception.BusinessException;
import com.museum.common.exception.ErrorCode;
import com.museum.entity.Join;
import com.museum.service.JoinService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingTools —— Tool 编排与鉴权")
class BookingToolsTest {

    @Mock
    private JoinService joinService;

    @Spy
    private BookingToolConverter converter = new BookingToolConverter();

    @InjectMocks
    private BookingTools bookingTools;

    @AfterEach
    void tearDown() {
        UserContext.clear();
        AiDebugTraceContext.clear();
    }

    @Test
    @DisplayName("queryDays 成功：status → OPEN/CLOSED")
    void queryDays_success_mapsStatus() {
        Map<String, Object> open = new HashMap<>();
        open.put("day", "2026-08-16");
        open.put("week", "周日");
        open.put("status", 1);
        Map<String, Object> closed = new HashMap<>();
        closed.put("day", "2026-08-17");
        closed.put("week", "周一");
        closed.put("status", 0);
        when(joinService.getBookingDays()).thenReturn(List.of(open, closed));

        ToolResult<QueryDaysData> result = bookingTools.queryDays();

        assertTrue(result.isOk());
        assertNull(result.getError());
        assertEquals(2, result.getData().getDays().size());
        assertEquals(DayOpenStatus.OPEN, result.getData().getDays().get(0).getStatus());
        assertEquals(DayOpenStatus.CLOSED, result.getData().getDays().get(1).getStatus());
    }

    @Test
    @DisplayName("queryTimes 成功：remain / period / AVAILABLE|FULL")
    void queryTimes_success_mapsSlots() {
        Map<String, Object> morning = new HashMap<>();
        morning.put("timeMark", "m_2026-08-16_09:00");
        morning.put("startTime", "09:00");
        morning.put("endTime", "11:00");
        morning.put("total", 50);
        morning.put("surplus", 12);
        morning.put("used", 38);
        Map<String, Object> afternoon = new HashMap<>();
        afternoon.put("timeMark", "m_2026-08-16_14:00");
        afternoon.put("startTime", "14:00");
        afternoon.put("endTime", "16:00");
        afternoon.put("total", 50);
        afternoon.put("surplus", 0);
        afternoon.put("used", 50);
        when(joinService.getBookingTimes("2026-08-16")).thenReturn(List.of(morning, afternoon));

        ToolResult<QueryTimesData> result = bookingTools.queryTimes("2026-08-16");

        assertTrue(result.isOk());
        assertEquals("2026-08-16", result.getData().getDay());
        TimeSlot a = result.getData().getTimes().get(0);
        TimeSlot b = result.getData().getTimes().get(1);
        assertEquals(12, a.getRemain());
        assertEquals(Period.MORNING, a.getPeriod());
        assertEquals(SlotAvailStatus.AVAILABLE, a.getStatus());
        assertEquals(Period.AFTERNOON, b.getPeriod());
        assertEquals(SlotAvailStatus.FULL, b.getStatus());
    }

    @Test
    @DisplayName("queryTimes 非法 day → BAD_REQUEST，不调 Service")
    void queryTimes_invalidDay_badRequest() {
        ToolResult<QueryTimesData> result = bookingTools.queryTimes("08-16");

        assertFalse(result.isOk());
        assertEquals(ToolError.BAD_REQUEST, result.getError());
        verifyNoInteractions(joinService);
    }

    @Test
    @DisplayName("submitBooking 成功：joinIds=null，userId 来自 UserContext")
    void submitBooking_success_noJoinIds() {
        UserContext.set("user_001");
        doNothing().when(joinService).submitBooking(eq("user_001"), eq("tm_1"), anyList());

        ToolResult<SubmitBookingData> result =
                bookingTools.submitBooking("tm_1", List.of("id_1", "id_2"));

        assertTrue(result.isOk());
        assertEquals("预约成功", result.getMessage());
        assertNull(result.getData().getJoinIds());
        assertTrue(result.getData().isBooked());
        assertEquals(2, result.getData().getVisitorCount());
        verify(joinService).submitBooking("user_001", "tm_1", List.of("id_1", "id_2"));
    }

    @Test
    @DisplayName("submitBooking FULL / DUPLICATE_BOOKING")
    void submitBooking_mapsBusinessErrors() {
        UserContext.set("user_001");
        doThrow(new BusinessException(ErrorCode.BOOKING_SLOT_FULL))
                .when(joinService).submitBooking(anyString(), anyString(), anyList());

        ToolResult<SubmitBookingData> full =
                bookingTools.submitBooking("tm_1", List.of("id_1"));
        assertFalse(full.isOk());
        assertEquals(ToolError.FULL, full.getError());

        doThrow(new BusinessException(ErrorCode.IDENTITY_DUPLICATE_BOOKING))
                .when(joinService).submitBooking(anyString(), anyString(), anyList());
        ToolResult<SubmitBookingData> dup =
                bookingTools.submitBooking("tm_1", List.of("id_1"));
        assertEquals(ToolError.DUPLICATE_BOOKING, dup.getError());
    }

    @Test
    @DisplayName("无 UserContext：写/list → UNAUTHORIZED，Service 零调用")
    void writeTools_withoutUser_unauthorized() {
        ToolResult<SubmitBookingData> submit =
                bookingTools.submitBooking("tm_1", List.of("id_1"));
        ToolResult<ListRecordsData> list = bookingTools.listRecords(null, null);
        ToolResult<CancelBookingData> cancel = bookingTools.cancelBooking("join_1");

        assertEquals(ToolError.UNAUTHORIZED, submit.getError());
        assertEquals(ToolError.UNAUTHORIZED, list.getError());
        assertEquals(ToolError.UNAUTHORIZED, cancel.getError());
        verifyNoInteractions(joinService);
    }

    @Test
    @DisplayName("cancelBooking 空白 joinId → BAD_REQUEST")
    void cancelBooking_blankJoinId_badRequest() {
        UserContext.set("user_001");

        ToolResult<CancelBookingData> result = bookingTools.cancelBooking("  ");

        assertEquals(ToolError.BAD_REQUEST, result.getError());
        verifyNoInteractions(joinService);
    }

    @Test
    @DisplayName("cancelBooking NOT_FOUND")
    void cancelBooking_notFound() {
        UserContext.set("user_001");
        doThrow(new BusinessException(ErrorCode.BOOKING_NOT_FOUND))
                .when(joinService).cancelBooking("user_001", "join_x");

        ToolResult<CancelBookingData> result = bookingTools.cancelBooking("join_x");

        assertFalse(result.isOk());
        assertEquals(ToolError.NOT_FOUND, result.getError());
    }

    @Test
    @DisplayName("cancelBooking 成功")
    void cancelBooking_success() {
        UserContext.set("user_001");
        doNothing().when(joinService).cancelBooking("user_001", "join_1");

        ToolResult<CancelBookingData> result = bookingTools.cancelBooking("join_1");

        assertTrue(result.isOk());
        assertEquals("取消成功", result.getMessage());
        assertTrue(result.getData().isCancelled());
        assertEquals("join_1", result.getData().getJoinId());
    }

    @Test
    @DisplayName("listRecords 成功：仅当前用户记录经 Converter 映射")
    void listRecords_success() {
        UserContext.set("user_001");
        Join join = new Join();
        join.setJoinId("join_1");
        join.setJoinMeetDay("2026-08-16");
        join.setTimeMark("tm_1");
        join.setJoinMeetTimeStart("09:00");
        join.setJoinMeetTimeEnd("11:00");
        join.setJoinStatus(1);
        join.setJoinIsCheckin(0);
        join.setJoinForms("{\"name\":\"张三\",\"card\":\"110101199001011234\"}");
        when(joinService.getMyBookings("user_001")).thenReturn(List.of(join));

        ToolResult<ListRecordsData> result =
                bookingTools.listRecords("2026-08-16", BookingStatus.BOOKED);

        assertTrue(result.isOk());
        assertEquals(1, result.getData().getRecords().size());
        assertEquals("join_1", result.getData().getRecords().get(0).getJoinId());
        assertEquals(BookingStatus.BOOKED, result.getData().getRecords().get(0).getStatus());
        assertEquals(Period.MORNING, result.getData().getRecords().get(0).getPeriod());
    }

    @Test
    @DisplayName("非 BusinessException → INTERNAL_ERROR")
    void submitBooking_runtimeException_internalError() {
        UserContext.set("user_001");
        doThrow(new RuntimeException("boom"))
                .when(joinService).submitBooking(anyString(), anyString(), anyList());

        ToolResult<SubmitBookingData> result =
                bookingTools.submitBooking("tm_1", List.of("id_1"));

        assertFalse(result.isOk());
        assertEquals(ToolError.INTERNAL_ERROR, result.getError());
        assertEquals("boom", result.getMessage());
    }

    @Test
    @DisplayName("debug 开启时 queryTimes 记 OK toolTrace")
    void queryTimes_whenDebugEnabled_recordsOkTrace() {
        AiDebugTraceContext.begin(true);
        Map<String, Object> morning = new HashMap<>();
        morning.put("timeMark", "m_2026-08-16_09:00");
        morning.put("startTime", "09:00");
        morning.put("endTime", "11:00");
        morning.put("total", 50);
        morning.put("surplus", 12);
        morning.put("used", 38);
        when(joinService.getBookingTimes("2026-08-16")).thenReturn(List.of(morning));

        bookingTools.queryTimes("2026-08-16");

        AiDebugTrace debug = AiDebugTraceContext.snapshot(ChatIntent.BOOKING, System.currentTimeMillis());
        assertEquals(1, debug.getToolTrace().size());
        ToolTraceEntry entry = debug.getToolTrace().get(0);
        assertEquals("queryTimes", entry.getName());
        assertEquals("OK", entry.getStatus());
        assertEquals("2026-08-16", entry.getInputSummary().get("day"));
        assertEquals(1, entry.getOutputSummary().get("slotCount"));
        assertNull(entry.getError());
    }

    @Test
    @DisplayName("debug 开启且未登录时 submitBooking 记 UNAUTHORIZED")
    void submitBooking_whenDebugEnabledAndAnonymous_recordsUnauthorized() {
        AiDebugTraceContext.begin(true);

        bookingTools.submitBooking("tm_1", List.of("id_1", "id_2"));

        ToolTraceEntry entry = AiDebugTraceContext.snapshot(ChatIntent.BOOKING, System.currentTimeMillis())
                .getToolTrace().get(0);
        assertEquals("submitBooking", entry.getName());
        assertEquals("FAIL", entry.getStatus());
        assertEquals("UNAUTHORIZED", entry.getError());
        assertEquals(2, entry.getInputSummary().get("identityCount"));
        assertFalse(entry.getInputSummary().containsKey("identityIds"));
        assertTrue(entry.getOutputSummary().isEmpty());
    }
}
