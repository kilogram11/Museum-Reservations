package com.museum.ai.converter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.museum.ai.dto.*;
import com.museum.common.exception.BusinessException;
import com.museum.common.exception.ErrorCode;
import com.museum.entity.Join;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 业务结构 ↔ AI Tool DTO 转换（不碰 Redis / SQL）
 */
@Component
public class BookingToolConverter {

    public QueryDaysData toDaySlots(List<Map<String, Object>> bizDays) {
        QueryDaysData data = new QueryDaysData();
        if (bizDays == null || bizDays.isEmpty()) {
            return data;
        }
        List<DaySlot> slots = new ArrayList<>();
        for (Map<String, Object> row : bizDays) {
            DaySlot slot = new DaySlot();
            slot.setDay(asString(row.get("day")));
            slot.setWeek(asString(row.get("week")));
            slot.setStatus(toDayOpenStatus(row.get("status")));
            slots.add(slot);
        }
        data.setDays(slots);
        return data;
    }

    /**
     * @return null if valid; otherwise fail ToolResult
     */
    public ToolResult<QueryTimesData> validateQueryTimesDay(String day) {
        if (StrUtil.isBlank(day)) {
            return ToolResult.fail(ToolError.BAD_REQUEST, "日期不能为空");
        }
        try {
            LocalDate.parse(day.trim());
        } catch (DateTimeParseException e) {
            return ToolResult.fail(ToolError.BAD_REQUEST, "日期格式须为 yyyy-MM-dd");
        }
        return null;
    }

    public QueryTimesData toTimeSlots(String day, List<Map<String, Object>> bizTimes) {
        QueryTimesData data = new QueryTimesData();
        data.setDay(day);
        if (bizTimes == null || bizTimes.isEmpty()) {
            return data;
        }
        List<TimeSlot> slots = new ArrayList<>();
        for (Map<String, Object> row : bizTimes) {
            TimeSlot slot = new TimeSlot();
            slot.setTimeMark(asString(row.get("timeMark")));
            String start = asString(row.get("startTime"));
            String end = asString(row.get("endTime"));
            slot.setStartTime(start);
            slot.setEndTime(end);
            slot.setPeriod(inferPeriod(start));
            int total = asInt(row.get("total"), 0);
            int remain = asInt(row.get("surplus"), 0);
            int used = asInt(row.get("used"), Math.max(0, total - remain));
            slot.setTotal(total);
            slot.setRemain(Math.max(0, remain));
            slot.setUsed(used);
            slot.setStatus(remain > 0 ? SlotAvailStatus.AVAILABLE : SlotAvailStatus.FULL);
            slots.add(slot);
        }
        data.setTimes(slots);
        return data;
    }

    public SubmitBookingData toSubmitSuccess(String timeMark, int visitorCount) {
        return new SubmitBookingData(true, timeMark, visitorCount, null);
    }

    public ListRecordsData toBookingRecords(List<Join> joins, String dayFilter, BookingStatus statusFilter) {
        ListRecordsData data = new ListRecordsData();
        if (joins == null || joins.isEmpty()) {
            return data;
        }
        List<BookingRecord> records = new ArrayList<>();
        for (Join join : joins) {
            BookingStatus status = toBookingStatus(join.getJoinStatus());
            if (statusFilter != null && status != statusFilter) {
                continue;
            }
            if (StrUtil.isNotBlank(dayFilter) && !dayFilter.equals(join.getJoinMeetDay())) {
                continue;
            }
            records.add(toBookingRecord(join, status));
        }
        data.setRecords(records);
        return data;
    }

    public CancelBookingData toCancelSuccess(String joinId) {
        return new CancelBookingData(true, joinId);
    }

    public ToolResult<?> fromBusinessException(BusinessException ex) {
        ToolError error = fromErrorCode(ex.getCode());
        String message = StrUtil.blankToDefault(ex.getMessage(), error.name());
        return ToolResult.fail(error, message);
    }

    public ToolError fromErrorCode(ErrorCode errorCode) {
        if (errorCode == null) {
            return ToolError.INTERNAL_ERROR;
        }
        return fromErrorCode(errorCode.getCode());
    }

    public ToolError fromErrorCode(Integer code) {
        if (code == null) {
            return ToolError.INTERNAL_ERROR;
        }
        return switch (code) {
            case 401 -> ToolError.UNAUTHORIZED;
            case 400 -> ToolError.BAD_REQUEST;
            case 2001 -> ToolError.IDENTITY_NOT_FOUND;
            case 2002 -> ToolError.BLACKLISTED;
            case 2003 -> ToolError.DUPLICATE_BOOKING;
            case 2004 -> ToolError.DUPLICATE_IN_REQUEST;
            case 3001 -> ToolError.SLOT_INVALID;
            case 3002 -> ToolError.FULL;
            case 3003 -> ToolError.TOO_MANY;
            case 3004 -> ToolError.NO_VISITORS;
            case 3005 -> ToolError.NOT_FOUND;
            case 3006 -> ToolError.STATUS_INVALID;
            case 3007 -> ToolError.ALREADY_CHECKED_IN;
            case 3011 -> ToolError.SCHEDULE_ERROR;
            case 3012 -> ToolError.STOCK_UNAVAILABLE;
            default -> ToolError.INTERNAL_ERROR;
        };
    }

    public Period inferPeriod(String startTime) {
        if (StrUtil.isBlank(startTime)) {
            return null;
        }
        String normalized = startTime.trim();
        int colon = normalized.indexOf(':');
        String hourPart = colon > 0 ? normalized.substring(0, colon) : normalized;
        try {
            int hour = Integer.parseInt(hourPart);
            return hour < 12 ? Period.MORNING : Period.AFTERNOON;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String maskCard(String card) {
        if (StrUtil.isBlank(card)) {
            return null;
        }
        String raw = card.trim();
        if (raw.length() <= 8) {
            return "****";
        }
        int maskLen = raw.length() - 8;
        return raw.substring(0, 4) + "*".repeat(maskLen) + raw.substring(raw.length() - 4);
    }

    private BookingRecord toBookingRecord(Join join, BookingStatus status) {
        BookingRecord record = new BookingRecord();
        record.setJoinId(join.getJoinId());
        record.setDay(join.getJoinMeetDay());
        record.setTimeMark(join.getTimeMark());
        record.setStartTime(join.getJoinMeetTimeStart());
        record.setEndTime(join.getJoinMeetTimeEnd());
        record.setPeriod(inferPeriod(join.getJoinMeetTimeStart()));
        record.setStatus(status);
        record.setCheckin(toCheckinStatusAi(join.getJoinIsCheckin()));
        fillVisitorFromForms(join.getJoinForms(), record);
        record.setMuseumTitle(join.getMuseumTitle());
        record.setMuseumAddress(join.getMuseumAddress());
        return record;
    }

    private void fillVisitorFromForms(String joinForms, BookingRecord record) {
        if (StrUtil.isBlank(joinForms)) {
            return;
        }
        try {
            JSONObject obj = JSONUtil.parseObj(joinForms);
            record.setVisitorName(obj.getStr("name"));
            record.setVisitorCardMasked(maskCard(obj.getStr("card")));
        } catch (Exception ignored) {
            // keep nulls
        }
    }

    private DayOpenStatus toDayOpenStatus(Object status) {
        Integer code = asInteger(status);
        if (code != null && code == 1) {
            return DayOpenStatus.OPEN;
        }
        return DayOpenStatus.CLOSED;
    }

    private BookingStatus toBookingStatus(Integer joinStatus) {
        if (joinStatus != null && joinStatus == 2) {
            return BookingStatus.CANCELLED;
        }
        return BookingStatus.BOOKED;
    }

    private CheckinStatusAi toCheckinStatusAi(Integer checkin) {
        if (checkin == null) {
            return CheckinStatusAi.UNCHECKED;
        }
        return switch (checkin) {
            case 1 -> CheckinStatusAi.CHECKED_IN;
            case 3 -> CheckinStatusAi.EXPIRED;
            default -> CheckinStatusAi.UNCHECKED;
        };
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int asInt(Object value, int defaultValue) {
        Integer i = asInteger(value);
        return i != null ? i : defaultValue;
    }
}
