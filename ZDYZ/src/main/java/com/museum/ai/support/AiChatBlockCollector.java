package com.museum.ai.support;

import com.museum.ai.dto.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request-scoped chat block collector backed by ThreadLocal.
 * It records deterministic blocks derived from tool results so the miniapp
 * can render cards without parsing natural language replies.
 */
public final class AiChatBlockCollector {

    private static final ThreadLocal<List<ChatBlock>> BLOCKS =
            ThreadLocal.withInitial(ArrayList::new);

    private AiChatBlockCollector() {
    }

    public static void begin() {
        BLOCKS.set(new ArrayList<>());
    }

    public static List<ChatBlock> snapshot() {
        return new ArrayList<>(BLOCKS.get());
    }

    public static void clear() {
        BLOCKS.remove();
    }

    public static void recordQueryTimes(QueryTimesData data) {
        if (data == null) {
            return;
        }
        BLOCKS.get().add(new ChatBlock(
                "time_slots",
                data.getDay() == null ? "可预约时段" : data.getDay() + " 可预约时段",
                new ArrayList<>(data.getTimes()),
                "tool:queryTimes"));
    }

    public static void recordListRecords(ListRecordsData data) {
        if (data == null) {
            return;
        }
        BLOCKS.get().add(new ChatBlock(
                "booking_records",
                "预约记录",
                new ArrayList<>(data.getRecords()),
                "tool:listRecords"));
    }

    public static void recordSubmitBooking(SubmitBookingData data, String message) {
        if (data == null) {
            return;
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("text", message);
        item.put("booked", data.isBooked());
        item.put("timeMark", data.getTimeMark());
        item.put("visitorCount", data.getVisitorCount());
        item.put("joinIds", data.getJoinIds());
        BLOCKS.get().add(new ChatBlock(
                "tips",
                "预约结果",
                List.of(item),
                "tool:submitBooking"));
    }

    public static void recordCancelBooking(String joinId, String message) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("text", message);
        item.put("joinId", joinId);
        BLOCKS.get().add(new ChatBlock(
                "tips",
                "取消结果",
                List.of(item),
                "tool:cancelBooking"));
    }

    public static ChatBlock tipBlock(String title, String text, String source) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("text", text);
        return new ChatBlock("tips", title, List.of(item), source);
    }
}
