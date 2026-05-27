package com.museum.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    // In a real production app, this should be in application.properties or
    // environment variable
    private static final String API_KEY = "sk-ee6b3958a97e4838a070d916f907f8a0";

    public String chat(String userMessage) {
        try {
            // Construct request body
            JSONObject message = new JSONObject();
            message.set("role", "user");
            message.set("content", userMessage);

            JSONArray messages = new JSONArray();
            messages.add(message);

            JSONObject body = new JSONObject();
            body.set("model", "qwen-flash");
            body.set("messages", messages);

            // Execute Request
            String result = HttpRequest.post(API_URL)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(20000) // 20 seconds timeout
                    .execute()
                    .body();

            // Parse Response
            if (result == null || result.isEmpty()) {
                return "AI 服务暂无响应";
            }

            JSONObject jsonResult = JSONUtil.parseObj(result);

            // Check for error
            if (jsonResult.containsKey("error")) {
                return "Error: " + jsonResult.get("error");
            }

            // Extract content: choices[0].message.content
            JSONArray choices = jsonResult.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject firstChoice = choices.getJSONObject(0);
                JSONObject msg = firstChoice.getJSONObject("message");
                return msg.getStr("content");
            }

            return "无法解析 AI 响应";

        } catch (Exception e) {
            e.printStackTrace();
            return "发生错误: " + e.getMessage();
        }
    }
}
