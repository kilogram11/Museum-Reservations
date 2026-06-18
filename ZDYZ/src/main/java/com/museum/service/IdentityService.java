package com.museum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.museum.entity.Identity;

import java.util.List;

/**
 * 身份用户业务接口 (常用游客管理)
 */
public interface IdentityService extends IService<Identity> {

    /**
     * 获取我的常用游客列表
     *
     * @param userId 当前用户ID
     * @return 游客列表
     */
    List<Identity> listMyIdentity(String userId);

    /**
     * 添加/更新常用游客
     *
     * @param userId   当前用户ID
     * @param identity 游客信息
     */
    void saveIdentity(String userId, Identity identity);

    /**
     * 删除常用游客 (解除关联)
     *
     * @param userId     当前用户ID
     * @param identityId 游客ID
     */
    void removeIdentity(String userId, String identityId);
}
