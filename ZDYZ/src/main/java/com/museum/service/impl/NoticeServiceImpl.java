package com.museum.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.common.exception.BusinessException;
import com.museum.common.utils.IdGenerator;
import com.museum.entity.News;
import com.museum.mapper.NewsMapper;
import com.museum.service.NoticeService;
import org.springframework.stereotype.Service;

/**
 * 公告业务实现类
 */
@Service
public class NoticeServiceImpl extends ServiceImpl<NewsMapper, News> implements NoticeService {

    @Override
    public Page<News> dataList(String keyword, Integer page, Integer limit) {
        Page<News> p = new Page<>(page, limit);
        LambdaQueryWrapper<News> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(News::getNewsTitle, keyword);
        }
        wrapper.orderByDesc(News::getNewsAddTime);
        return this.page(p, wrapper);
    }

    @Override
    public void addNotice(News news) {
        if (StrUtil.isBlank(news.getNewsTitle())) {
            throw BusinessException.of("公告标题不能为空");
        }
        news.setId(IdGenerator.generateId());
        news.setNewsId(IdGenerator.generateNewsId());
        news.setNewsAddTime(System.currentTimeMillis());

        news.setNewsEditTime(System.currentTimeMillis());
        news.setNewsStatus(1);
        news.setNewsViewCnt(0);

        this.save(news);
    }

    @Override
    public void editNotice(News news) {
        if (StrUtil.isBlank(news.getId())) {
            throw BusinessException.of("ID不能为空");
        }
        news.setNewsEditTime(System.currentTimeMillis());
        this.updateById(news);
    }

    @Override
    public void delNotice(String id) {
        this.removeById(id);
    }

    @Override
    public void status(String id, Integer status) {
        News news = new News();
        news.setId(id);
        news.setNewsStatus(status);
        this.updateById(news);
    }

    @Override
    public Page<News> appList(Integer page, Integer limit) {
        Page<News> p = new Page<>(page, limit);
        LambdaQueryWrapper<News> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(News::getNewsStatus, 1);
        wrapper.orderByDesc(News::getNewsAddTime);
        return this.page(p, wrapper);
    }

    @Override
    public void addRead(String id) {
        News news = this.getById(id);
        if (news != null) {
            news.setNewsViewCnt(news.getNewsViewCnt() + 1);
            this.updateById(news);
        }
    }
}
