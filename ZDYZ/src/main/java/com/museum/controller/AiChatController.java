package com.museum.controller;

import com.museum.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String reply = aiChatService.chat(message);

        Map<String, String> response = new HashMap<>();
        response.put("reply", reply);
        return response;
    }
}
