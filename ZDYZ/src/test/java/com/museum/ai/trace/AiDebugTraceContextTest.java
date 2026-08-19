package com.museum.ai.trace;

import com.museum.ai.dto.QueryTimesData;
import com.museum.ai.dto.SubmitBookingData;
import com.museum.ai.dto.TimeSlot;
import com.museum.ai.dto.ToolError;
import com.museum.ai.dto.ToolResult;
import com.museum.ai.rag.model.ChatIntent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AiDebugTraceContext - debug switch and summaries")
class AiDebugTraceContextTest {

    @AfterEach
    void tearDown() {
        AiDebugTraceContext.clear();
    }

    @Test
    @DisplayName("debug 关闭时 snapshot 为 null，record 为 no-op")
    void snapshot_whenDisabled_isNull() {
        AiDebugTraceContext.begin(false);
        AiDebugTraceContext.recordTool(
                "submitBooking",
                System.currentTimeMillis(),
                Map.of("identityCount", 2, "identityIds", List.of("secret")),
                ToolResult.ok(new SubmitBookingData(true, "tm", 2, List.of())));

        assertNull(AiDebugTraceContext.snapshot(ChatIntent.BOOKING, System.currentTimeMillis()));
        assertFalse(AiDebugTraceContext.isEnabled());
    }

    @Test
    @DisplayName("debug 开启时工具摘要不含 identityIds")
    void recordTool_doesNotLeakIdentityIds() {
        AiDebugTraceContext.begin(true);
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("hasTimeMark", true);
        input.put("identityCount", 2);
        AiDebugTraceContext.recordTool(
                "submitBooking",
                System.currentTimeMillis() - 5,
                input,
                ToolResult.ok(new SubmitBookingData(true, "tm", 2, List.of())));

        AiDebugTrace debug = AiDebugTraceContext.snapshot(ChatIntent.BOOKING, System.currentTimeMillis() - 10);
        assertNotNull(debug);
        assertEquals(1, debug.getToolTrace().size());
        ToolTraceEntry entry = debug.getToolTrace().get(0);
        assertEquals("submitBooking", entry.getName());
        assertEquals("OK", entry.getStatus());
        assertEquals(2, entry.getInputSummary().get("identityCount"));
        assertFalse(entry.getInputSummary().containsKey("identityIds"));
        assertEquals(true, entry.getOutputSummary().get("booked"));
        assertEquals(2, entry.getOutputSummary().get("visitorCount"));
        assertNull(entry.getError());
    }

    @Test
    @DisplayName("失败 Tool 记录 error 码")
    void recordTool_failure_recordsErrorCode() {
        AiDebugTraceContext.begin(true);
        AiDebugTraceContext.recordTool(
                "queryTimes",
                System.currentTimeMillis(),
                Map.of("day", "bad"),
                ToolResult.fail(ToolError.BAD_REQUEST, "非法日期"));

        ToolTraceEntry entry = AiDebugTraceContext.snapshot(ChatIntent.BOOKING, System.currentTimeMillis())
                .getToolTrace().get(0);
        assertEquals("FAIL", entry.getStatus());
        assertEquals("BAD_REQUEST", entry.getError());
        assertTrue(entry.getOutputSummary().isEmpty());
    }

    @Test
    @DisplayName("queryTimes 成功摘要含 slotCount")
    void recordTool_queryTimes_slotCount() {
        AiDebugTraceContext.begin(true);
        QueryTimesData data = new QueryTimesData("2026-08-20", List.of(
                new TimeSlot(), new TimeSlot()));
        AiDebugTraceContext.recordTool("queryTimes", System.currentTimeMillis(), Map.of("day", "2026-08-20"),
                ToolResult.ok(data));

        assertEquals(2, AiDebugTraceContext.snapshot(ChatIntent.BOOKING, System.currentTimeMillis())
                .getToolTrace().get(0).getOutputSummary().get("slotCount"));
    }
}
