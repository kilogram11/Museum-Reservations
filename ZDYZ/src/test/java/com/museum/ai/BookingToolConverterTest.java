package com.museum.ai;

import com.museum.ai.context.UserContext;
import com.museum.ai.converter.BookingToolConverter;
import com.museum.ai.dto.*;
import com.museum.common.exception.BusinessException;
import com.museum.common.exception.ErrorCode;
import com.museum.entity.Join;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BookingToolConverter —— AI 语义映射")
class BookingToolConverterTest {

    private final BookingToolConverter converter = new BookingToolConverter();

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("日期 status 1/0 → OPEN/CLOSED")
    void toDaySlots_mapsOpenClosed() {
        Map<String, Object> open = new HashMap<>();
        open.put("day", "2026-08-16");
        open.put("week", "周日");
        open.put("status", 1);
        Map<String, Object> closed = new HashMap<>();
        closed.put("day", "2026-08-17");
        closed.put("week", "周一");
        closed.put("status", 0);

        QueryDaysData data = converter.toDaySlots(List.of(open, closed));

        assertEquals(2, data.getDays().size());
        assertEquals(DayOpenStatus.OPEN, data.getDays().get(0).getStatus());
        assertEquals(DayOpenStatus.CLOSED, data.getDays().get(1).getStatus());
    }

    @Test
    @DisplayName("余票 surplus→remain，0 为 FULL")
    void toTimeSlots_mapsRemainAndFull() {
        Map<String, Object> available = new HashMap<>();
        available.put("timeMark", "m_2026-08-16_09:00");
        available.put("startTime", "09:00");
        available.put("endTime", "11:00");
        available.put("total", 50);
        available.put("surplus", 12);
        available.put("used", 38);

        Map<String, Object> full = new HashMap<>();
        full.put("timeMark", "m_2026-08-16_14:00");
        full.put("startTime", "14:00");
        full.put("endTime", "16:00");
        full.put("total", 50);
        full.put("surplus", 0);
        full.put("used", 50);

        QueryTimesData data = converter.toTimeSlots("2026-08-16", List.of(available, full));

        assertEquals(Period.MORNING, data.getTimes().get(0).getPeriod());
        assertEquals(12, data.getTimes().get(0).getRemain());
        assertEquals(SlotAvailStatus.AVAILABLE, data.getTimes().get(0).getStatus());
        assertEquals(Period.AFTERNOON, data.getTimes().get(1).getPeriod());
        assertEquals(SlotAvailStatus.FULL, data.getTimes().get(1).getStatus());
    }

    @Test
    @DisplayName("day 非法 → BAD_REQUEST")
    void validateQueryTimesDay_rejectsBadDay() {
        assertEquals(ToolError.BAD_REQUEST, converter.validateQueryTimesDay("").getError());
        assertEquals(ToolError.BAD_REQUEST, converter.validateQueryTimesDay("08-16").getError());
        assertNull(converter.validateQueryTimesDay("2026-08-16"));
    }

    @Test
    @DisplayName("period 推断：上午 MORNING，中午起 AFTERNOON")
    void inferPeriod() {
        assertEquals(Period.MORNING, converter.inferPeriod("9:00"));
        assertEquals(Period.MORNING, converter.inferPeriod("11:59"));
        assertEquals(Period.AFTERNOON, converter.inferPeriod("12:00"));
        assertEquals(Period.AFTERNOON, converter.inferPeriod("14:00"));
    }

    @Test
    @DisplayName("Join → BookingRecord，脱敏证件，过滤 status")
    void toBookingRecords_mapsAndFilters() {
        Join booked = new Join();
        booked.setJoinId("join_1");
        booked.setJoinMeetDay("2026-08-16");
        booked.setTimeMark("m_2026-08-16_09:00");
        booked.setJoinMeetTimeStart("09:00");
        booked.setJoinMeetTimeEnd("11:00");
        booked.setJoinStatus(1);
        booked.setJoinIsCheckin(0);
        booked.setJoinForms("{\"name\":\"张三\",\"card\":\"11010119900101123X\",\"mobile\":\"13800000000\"}");
        booked.setMuseumTitle("馆");
        booked.setMuseumAddress("地址");

        Join cancelled = new Join();
        cancelled.setJoinId("join_2");
        cancelled.setJoinMeetDay("2026-08-16");
        cancelled.setJoinStatus(2);
        cancelled.setJoinIsCheckin(0);

        ListRecordsData all = converter.toBookingRecords(List.of(booked, cancelled), null, null);
        assertEquals(2, all.getRecords().size());

        ListRecordsData onlyBooked = converter.toBookingRecords(List.of(booked, cancelled), null, BookingStatus.BOOKED);
        assertEquals(1, onlyBooked.getRecords().size());
        BookingRecord r = onlyBooked.getRecords().get(0);
        assertEquals("join_1", r.getJoinId());
        assertEquals(BookingStatus.BOOKED, r.getStatus());
        assertEquals(CheckinStatusAi.UNCHECKED, r.getCheckin());
        assertEquals(Period.MORNING, r.getPeriod());
        assertEquals("张三", r.getVisitorName());
        assertEquals("1101**********123X", r.getVisitorCardMasked());
        assertFalse(r.getVisitorCardMasked().contains("19900101"));
    }

    @Test
    @DisplayName("证件脱敏 maskCard")
    void maskCard() {
        assertEquals("1101**********123X", converter.maskCard("11010119900101123X"));
        assertNull(converter.maskCard(null));
        assertEquals("****", converter.maskCard("1234"));
    }

    @Test
    @DisplayName("ErrorCode → ToolError")
    void fromErrorCode_mapping() {
        assertEquals(ToolError.FULL, converter.fromErrorCode(3002));
        assertEquals(ToolError.DUPLICATE_BOOKING, converter.fromErrorCode(2003));
        assertEquals(ToolError.UNAUTHORIZED, converter.fromErrorCode(401));
        assertEquals(ToolError.FULL, converter.fromErrorCode(ErrorCode.BOOKING_SLOT_FULL));
        assertEquals(ToolError.INTERNAL_ERROR, converter.fromErrorCode(9999));
    }

    @Test
    @DisplayName("BusinessException → ToolResult.fail")
    void fromBusinessException() {
        ToolResult<?> result = converter.fromBusinessException(
                new BusinessException(ErrorCode.BOOKING_SLOT_FULL));
        assertFalse(result.isOk());
        assertEquals(ToolError.FULL, result.getError());
        assertNotNull(result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("submit / cancel 成功载荷")
    void successPayloads() {
        SubmitBookingData submit = converter.toSubmitSuccess("tm", 2);
        assertTrue(submit.isBooked());
        assertEquals(2, submit.getVisitorCount());
        assertNull(submit.getJoinIds());

        CancelBookingData cancel = converter.toCancelSuccess("join_x");
        assertTrue(cancel.isCancelled());
        assertEquals("join_x", cancel.getJoinId());
    }

    @Test
    @DisplayName("UserContext ThreadLocal")
    void userContext() {
        assertNull(UserContext.get());
        UserContext.set("user_001");
        assertEquals("user_001", UserContext.get());
        UserContext.clear();
        assertNull(UserContext.get());
    }
}
