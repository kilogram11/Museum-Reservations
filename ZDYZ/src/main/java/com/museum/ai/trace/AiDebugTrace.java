package com.museum.ai.trace;

import com.museum.ai.rag.model.ChatIntent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiDebugTrace {
    public static final String ROUTER_VERSION = "v1";

    private RouteTrace route;
    private RagTraceEntry ragTrace;
    private List<ToolTraceEntry> toolTrace = new ArrayList<>();
    private TimingTrace timing;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteTrace {
        private ChatIntent intent;
        private String routerVersion;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimingTrace {
        private long totalMs;
    }
}
