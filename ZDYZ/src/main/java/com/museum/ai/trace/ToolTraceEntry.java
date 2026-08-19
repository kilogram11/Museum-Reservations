package com.museum.ai.trace;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolTraceEntry {
    private String name;
    private String status;
    private long startedAt;
    private long durationMs;
    private Map<String, Object> inputSummary = new LinkedHashMap<>();
    private Map<String, Object> outputSummary = new LinkedHashMap<>();
    private String error;
}
