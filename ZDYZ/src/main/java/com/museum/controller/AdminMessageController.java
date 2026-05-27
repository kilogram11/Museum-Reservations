package com.museum.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.common.result.Result;
import com.museum.entity.MessageTemplate;
import com.museum.service.MessageTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 消息模版管理 (管理端)
 */
@RestController
@RequestMapping("/admin/message/template")
public class AdminMessageController {

    @Autowired
    private MessageTemplateService templateService;

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer limit) {
        Page<MessageTemplate> pageParam = new Page<>(page, limit);
        return Result.success("获取成功", templateService.page(pageParam));
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public Result get(@PathVariable Long id) {
        return Result.success("获取成功", templateService.getById(id));
    }

    /**
     * 更新
     */
    @PostMapping("/update")
    public Result update(@RequestBody MessageTemplate template) {
        templateService.updateTemplate(template);
        return Result.success("更新成功");
    }
}
