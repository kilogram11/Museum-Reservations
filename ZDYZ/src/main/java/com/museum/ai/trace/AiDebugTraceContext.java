package com.museum.ai.trace;

import com.museum.ai.dto.CancelBookingData;
import com.museum.ai.dto.ListRecordsData;
import com.museum.ai.dto.QueryDaysData;
import com.museum.ai.dto.QueryTimesData;
import com.museum.ai.dto.SubmitBookingData;
import com.museum.ai.dto.ToolResult;
import com.museum.ai.rag.model.ChatIntent;
import com.museum.ai.rag.model.RagHit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Request-scoped debug collector. No-op unless {@link #begin(boolean)} is called with true.
 */
public final class AiDebugTraceContext {

    private static final ThreadLocal<State> STATE = new ThreadLocal<>();

    private AiDebugTraceContext() {
    }

    public static void begin(boolean enabled) {
        STATE.set(new State(enabled));
    }

    public static boolean isEnabled() {
        State state = STATE.get();
        return state != null && state.enabled;
    }

    public static void recordTool(String name, long startedAt, Map<String, Object> inputSummary, ToolResult<?> result) {
        State state = STATE.get();
        if (state == null || !state.enabled) {
            return;
        }
        long durationMs = Math.max(0, System.currentTimeMillis() - startedAt);
        Map<String, Object> output = new LinkedHashMap<>();
        String error = null;
        String status = "FAIL";
        if (result != null && result.isOk()) {
            status = "OK";
            if (result.getData() != null) {
                output.putAll(summarizeOutput(result.getData()));
            }
        } else if (result != null && result.getError() != null) {
            error = result.getError().name();
        }
        state.tools.add(new ToolTraceEntry(
                name,
                status,
                startedAt,
                durationMs,
                copyMap(inputSummary),
                output,
                error));
    }

    public static void recordRagDisabled() {
        State state = STATE.get();
        if (state == null || !state.enabled) {
            return;
        }
        state.rag = new RagTraceEntry(false, false, 0, List.of());
    }

    public static void recordRagHits(List<RagHit> hits) {
        State state = STATE.get();
        if (state == null || !state.enabled) {
            return;
        }
        List<RagHit> safeHits = hits == null ? List.of() : hits;
        Set<String> sources = new LinkedHashSet<>();
        for (RagHit hit : safeHits) {
            if (hit != null && hit.chunk() != null && hit.chunk().sourceType() != null) {
                sources.add(hit.chunk().sourceType());
            }
        }
        state.rag = new RagTraceEntry(true, true, safeHits.size(), new ArrayList<>(sources));
    }

    public static AiDebugTrace snapshot(ChatIntent intent, long startedAt) {
        State state = STATE.get();
        if (state == null || !state.enabled) {
            return null;
        }
        RagTraceEntry rag = state.rag != null
                ? state.rag
                : new RagTraceEntry(false, false, 0, List.of());
        return new AiDebugTrace(
                new AiDebugTrace.RouteTrace(intent, AiDebugTrace.ROUTER_VERSION),
                rag,
                new ArrayList<>(state.tools),
                new AiDebugTrace.TimingTrace(Math.max(0, System.currentTimeMillis() - startedAt)));
    }

    public static void clear() {
        STATE.remove();
    }

    private static Map<String, Object> copyMap(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }

    private static Map<String, Object> summarizeOutput(Object data) {
        Map<String, Object> output = new LinkedHashMap<>();
        if (data instanceof QueryDaysData days) {
            output.put("dayCount", days.getDays() == null ? 0 : days.getDays().size());
        } else if (data instanceof QueryTimesData times) {
            output.put("slotCount", times.getTimes() == null ? 0 : times.getTimes().size());
        } else if (data instanceof SubmitBookingData submit) {
            output.put("booked", submit.isBooked());
            output.put("visitorCount", submit.getVisitorCount());
        } else if (data instanceof ListRecordsData records) {
            output.put("recordCount", records.getRecords() == null ? 0 : records.getRecords().size());
        } else if (data instanceof CancelBookingData cancel) {
            output.put("cancelled", cancel.isCancelled());
        }
        return output;
    }

    private static final class State {
        private final boolean enabled;
        private final List<ToolTraceEntry> tools = new ArrayList<>();
        private RagTraceEntry rag;

        private State(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
