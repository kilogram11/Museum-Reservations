package com.museum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.museum.common.dto.AdminLoginDTO;
import com.museum.entity.Admin;
import java.util.Map;

/**
 * 管理员业务接口
 */
public interface AdminService extends IService<Admin> {

    /**
     * 管理员登录
     *
     * @param dto 登录参数
     * @return Admin (包含token)
     */
    Admin login(AdminLoginDTO dto);

    /**
     * 管理员注册
     *
     * @param dto 注册参数
     * @return 注册成功的管理员
     */
    Admin register(AdminLoginDTO dto);

    /**
     * 获取管理员信息
     *
     * @return 管理员信息
     */
    Admin getAdminInfo();

    /**
     * 退出登录
     */
    void logout();

    // ========== 新增个人信息业务方法 ==========
    /**
     * 根据adminId获取个人信息（昵称、简介、头像）
     */
    Admin getAdminProfile(String adminId);

    /**
     * 更新用户名和简介
     */
    boolean updateAdminProfile(Admin admin);

    /**
     * 更新头像
     */
    boolean updateAdminAvatar(String adminId, String avatarUrl);
}
