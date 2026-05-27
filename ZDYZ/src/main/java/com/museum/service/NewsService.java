package com.museum.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.museum.common.dto.NewsAddDTO;
import com.museum.entity.News;

/**
 * 公告/推文服务接口
 */
public interface NewsService extends IService<News> {

    /**
     * 新增公告/推文
     * 
     * @param dto 新增参数
     */
    void addNews(NewsAddDTO dto);

    /**
     * 修改公告/推文
     * 
     * @param dto 修改参数
     */
    void editNews(com.museum.common.dto.NewsEditDTO dto);

    /**
     * 删除公告/推文
     * 
     * @param id 数据库主键
     */
    void delNews(String id);

    /**
     * 分页查询公告
     * 
     * @param keyword 关键词
     * @param page    页码
     * @param limit   每页条数
     * @return 分页结果
     */
    Page<News> dataList(String keyword, Integer page, Integer limit);

    /**
     * 查看公告详情（浏览量 +1）
     * 
     * @param id 数据库主键
     * @return 公告详情
     */
    News viewNews(String id);
}
