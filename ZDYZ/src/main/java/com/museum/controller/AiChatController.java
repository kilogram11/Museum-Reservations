package com.museum.controller;

import com.museum.ai.dto.AiChatResponse;
import com.museum.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    @PostMapping("/chat")
    public AiChatResponse chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        return aiChatService.chat(message);
    }
}
