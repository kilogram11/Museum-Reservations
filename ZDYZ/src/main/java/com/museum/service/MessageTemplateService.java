package com.museum.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.entity.MessageTemplate;
import com.museum.mapper.MessageTemplateMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MessageTemplateService extends ServiceImpl<MessageTemplateMapper, MessageTemplate> {

    public MessageTemplate getByCode(String code) {
        return this.getOne(new LambdaQueryWrapper<MessageTemplate>().eq(MessageTemplate::getCode, code));
    }

    public void updateTemplate(MessageTemplate template) {
        template.setUpdateTime(LocalDateTime.now());
        this.updateById(template);
    }
}
