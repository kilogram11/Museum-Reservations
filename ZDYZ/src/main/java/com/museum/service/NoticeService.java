package com.museum.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.museum.entity.News;

import java.util.List;

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
    void status(String id, Integer status);

    /**
     * 小程序端获取公告列表（分页）。
     */
    Page<News> appList(Integer page, Integer limit);

    /**
     * RAG 装库专用：全量可见公告（NEWS_STATUS=1），不过分页。
     * 禁止用 {@link #appList} 装库。
     */
    List<News> listVisibleForRag();

    /**
     * 增加阅读量
     */
    void addRead(String id);
}
