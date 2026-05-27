package com.museum.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.museum.common.dto.ActivityAddDTO;
import com.museum.entity.Activity;

/**
 * 活动服务接口
 */
public interface ActivityService extends IService<Activity> {
    /**
     * 分页查询列表
     */
    Page<Activity> dataList(String keyword, Integer page, Integer limit);

    /**
     * 添加活动 (包含排期生成)
     */
    void addActivity(ActivityAddDTO dto);

    /**
     * 编辑活动
     */
    void editActivity(com.museum.common.dto.ActivityEditDTO dto);

    /**
     * 删除活动 (级联删除排期)
     */
    void delActivity(String id);

    /**
     * 修改状态
     */
    /**
     * 修改状态
     */
    void status(String id, Integer status);

    /**
     * 小程序端获取活动列表
     */
    Page<Activity> appList(Integer page, Integer limit);
}
