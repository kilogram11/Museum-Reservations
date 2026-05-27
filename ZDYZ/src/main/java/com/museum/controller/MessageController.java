package com.museum.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.common.result.Result;
import com.museum.entity.Message;
import com.museum.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 消息中心 (小程序端)
 */
@RestController
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 获取我的消息列表
     */
    @GetMapping("/my")
    public Result getMyMessages(@RequestParam("userId") String userId) {
        if (userId == null || userId.isEmpty()) {
            return Result.error("用户ID不能为空");
        }

        return Result.success("获取成功", messageService.getUserMessages(userId));
    }
    
    /**
     * 标记已读
     */
    @PostMapping("/read/{id}")
    public Result read(@PathVariable("id") Long id) {
        Message message = messageService.getById(id);
        if (message != null) {
            message.setIsRead(1);
            messageService.updateById(message);
        }
        return Result.success("操作成功");
    }

    /**
     * 获取未读数量
     */
    @GetMapping("/unread/count")
    public Result getUnreadCount(@RequestParam("userId") String userId) {
        if (userId == null || userId.isEmpty()) {
            return Result.success("获取成功", 0);
        }
        return Result.success("获取成功", messageService.getUnreadCount(userId));
    }
}
