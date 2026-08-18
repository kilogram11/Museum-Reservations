package com.museum.ai.dto;

import com.museum.ai.rag.model.ChatIntent;
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
}
