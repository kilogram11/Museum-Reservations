package com.museum.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 管理员表 实体类
 * 对应数据库中的 admin 表
 */
@Data
@TableName("admin")
public class Admin {

    /**
     * 记录系统唯一标识符 (对应数据库主键 _id)
     */
    @TableId("_id")
    private String id;

    /**
     * 管理员业务主键 ID（用于日志记录和业务关联）
     */
    @TableField("ADMIN_ID")
    private String adminId;

    /**
     * 管理员用户名（登录账号）
     */
    @TableField("ADMIN_NAME")
    private String adminName;

    /**
     * 加密后的密码（hash + salt）
     */
    @TableField("ADMIN_PASSWORD")
    private String adminPassword;

    /**
     * 登录令牌（JWT Token）
     */
    @TableField("ADMIN_TOKEN")
    private String adminToken;

    /**
     * 令牌生成时间戳
     */
    @TableField("ADMIN_TOKEN_TIME")
    private Long adminTokenTime;

    /**
     * 创建时间戳
     */
    @TableField("ADMIN_ADD_TIME")
    private Long adminAddTime;

    /**
     * 项目标识
     */
    @TableField("_pid")
    private String pid;

    // ========== 新增数据库对应的个人信息字段 ==========
    /**
     * 显示用用户名（如“李生”，对应前端userName）
     */
    @TableField("ADMIN_NICKNAME")
    private String adminNickname;
    /**
     * 用户简介（对应前端userIntro）
     */
    @TableField("ADMIN_INTRO")
    private String adminIntro;
    /**
     * 头像图片URL路径（对应前端currentAvatar）
     */
    @TableField("ADMIN_AVATAR")
    private String adminAvatar;
    /**
     * 个人信息更新时间戳
     */
    @TableField("ADMIN_INFO_UPDATE_TIME")
    private Long adminInfoUpdateTime;
}