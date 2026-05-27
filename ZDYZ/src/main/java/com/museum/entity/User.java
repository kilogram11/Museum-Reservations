package com.museum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 账号用户表 实体类
 * 对应数据库中的 user 表
 */
@Data // Lombok注解：自动生成Getter/Setter/toString等方法，让代码极其简洁
@TableName("user") // MyBatis-Plus注解：指定对应的数据库表名
public class User {

    @TableId("_id")
    private String id;

    /**
     * 用户业务 ID (用于业务逻辑关联)
     */
    @TableField("USER_ID")
    private String userId;

    /**
     * 小程序 OpenID (微信用户的唯一标识)
     */
    @TableField("USER_MINI_OPENID")
    private String userMiniOpenid;

    /**
     * 用户昵称/姓名
     */
    @TableField("USER_NAME")
    private String userName;

    /**
     * 手机号
     */
    @TableField("USER_MOBILE")
    private String userMobile;

    /**
     * 头像ID (关联头像表)
     */
    @TableField("USER_PIC")
    private Integer userPic;

    /**
     * 头像图片完整地址 (非持久化字段)
     */
    @TableField(exist = false)
    private String userPicUrl;

    /**
     * 注册时间戳 (使用Long对应数据库的BIGINT)
     */
    @TableField("USER_ADD_TIME")
    private Long userAddTime;

    /**
     * 修改时间戳
     */
    @TableField("USER_EDIT_TIME")
    private Long userEditTime;

    @TableField("_pid")
    private String pid;

}