package com.museum.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.entity.Identity;

/**
 * 黑名单业务接口
 */
public interface BlacklistService {
    /**
     * 分页查询黑名单列表
     */
    Page<Identity> list(String keyword, Integer page, Integer limit, Integer status);

    /**
     * 加入黑名单
     * 
     * @param identityId 身份证ID
     * @param reason     原因
     * @param endTime    结束时间戳
     */
    void add(String identityId, String reason, Long endTime);

    /**
     * 移除黑名单
     */
    void remove(String identityId);

    /**
     * 更新黑名单结束时间
     */
    void updateEndTime(String identityId, Long endTime);

    /**
     * 获取所有黑名单用于导出
     */
    java.util.List<Identity> getAllBlacklistForExport();
}
