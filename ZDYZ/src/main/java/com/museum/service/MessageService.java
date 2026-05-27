package com.museum.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.entity.Message;
import com.museum.entity.MessageTemplate;
import com.museum.entity.User;
import com.museum.mapper.MessageMapper;
import com.museum.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService extends ServiceImpl<MessageMapper, Message> {

    @Autowired
    private MessageTemplateService templateService;
    
    @Autowired
    private UserMapper userMapper;

    /**
     * 发送系统消息
     * @param userBusinessId 接收用户USER_ID (业务ID)，若为 "ALL" 则广播给所有用户
     * @param code 模版编码
     * @param args 模版参数
     */
    public void createMessage(String userBusinessId, String code, Object... args) {
        MessageTemplate template = templateService.getByCode(code);
        if (template == null) {
            System.err.println("Message Template not found: " + code);
            return;
        }

        String title = MessageFormat.format(template.getTitleTemplate(), args);
        String content = MessageFormat.format(template.getContentTemplate(), args);

        if ("ALL".equals(userBusinessId)) {
            // 广播: 查询所有用户的 USER_ID 并发送
            List<User> users = userMapper.selectList(null); // Assuming userMapper exists and selects ALL
            for (User user : users) {
                if (user.getUserId() != null) {
                    saveMessage(user.getUserId(), title, content, 2); // 只能是活动/系统广播
                }
            }
        } else {
            // 单发
            saveMessage(userBusinessId, title, content, getMessageType(code));
        }
    }

    private void saveMessage(String userId, String title, String content, int type) {
        Message message = new Message();
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setType(type);
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());
        this.save(message);
    }

    private int getMessageType(String code) {
        if (code.startsWith("BOOKING")) return 1;
        if (code.startsWith("ACTIVITY")) return 2;
        return 0;
    }
    
    public List<Message> getUserMessages(String userId) {
        return this.list(new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, userId)
                .orderByDesc(Message::getCreateTime));
    }

    public long getUnreadCount(String userId) {
        return this.count(new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, userId)
                .eq(Message::getIsRead, 0));
    }
}
