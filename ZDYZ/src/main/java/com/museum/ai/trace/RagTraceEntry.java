package com.museum.ai.trace;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagTraceEntry {
    private boolean enabled;
    private boolean queried;
    private int hitCount;
    private List<String> sources = new ArrayList<>();
}
