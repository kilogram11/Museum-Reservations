package com.museum.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.museum.ai.rag.model.ChatIntent;
import com.museum.ai.trace.AiDebugTrace;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {
    private String reply;
    private ChatIntent intent;
    private List<ChatBlock> blocks = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private AiDebugTrace debug;

    public AiChatResponse(String reply, ChatIntent intent, List<ChatBlock> blocks, List<String> suggestions) {
        this(reply, intent, blocks, suggestions, null);
    }
}
