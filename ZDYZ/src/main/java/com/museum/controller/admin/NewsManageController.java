package com.museum.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.common.dto.NewsAddDTO;
import com.museum.common.result.Result;
import com.museum.entity.News;
import com.museum.service.NewsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/admin/news")
public class NewsManageController {

    @Autowired
    private NewsService newsService;

    @PostMapping("/add")
    public Result add(@RequestBody NewsAddDTO dto) {
        newsService.addNews(dto);
        return Result.success("发布成功");
    }

    @PostMapping("/del")
    public Result del(String id) {
        newsService.delNews(id);
        return Result.success("删除成功");
    }

    @PostMapping("/list")
    public Result list(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit) {
        Page<News> pageResult = newsService.dataList(keyword, page, limit);
        return Result.success("获取成功", pageResult);
    }

    // 兼容 POST /admin/news/view?id=xxx 或者 RESTful 如果需要
    // 为了保持统一风格，使用 POST + query param id
    @PostMapping("/view")
    public Result view(String id) {
        News news = newsService.viewNews(id);
        return Result.success("获取成功", news);
    }

    @PostMapping("/edit")
    public Result edit(@RequestBody com.museum.common.dto.NewsEditDTO dto) {
        newsService.editNews(dto);
        return Result.success("修改成功");
    }
}
