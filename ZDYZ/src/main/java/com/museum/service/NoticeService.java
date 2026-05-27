package com.museum.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.museum.entity.News;

/**
 * 公告业务接口
 */
public interface NoticeService extends IService<News> {

    /**
     * 获取公告列表
     */
    Page<News> dataList(String keyword, Integer page, Integer limit);

    /**
     * 添加公告
     */
    void addNotice(News news);

    /**
     * 编辑公告
     */
    void editNotice(News news);

    /**
     * 删除公告
     */
    void delNotice(String id);

    /**
     * 修改状态
     */
    /**
     * 修改状态
     */
    void status(String id, Integer status);

    /**
     * 小程序端获取公告列表
     */
    /**
     * 小程序端获取公告列表
     */
    Page<News> appList(Integer page, Integer limit);

    /**
     * 增加阅读量
     */
    void addRead(String id);
}
