package com.museum.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 活动表 实体类
 * 对应数据库中的 activity 表
 */
@Data
@TableName("activity")
public class Activity {

    /**
     * 记录系统唯一标识符
     */
    @TableId("_id")
    private String id;

    /**
     * 活动业务 ID
     */
    @TableField("ACTIVITY_ID")
    private String activityId;

    /**
     * 活动标题
     */
    @TableField("ACTIVITY_TITLE")
    private String activityTitle;

    /**
     * 创建该活动的管理员ID（外键关联admin表）
     */
    @TableField("ADMIN_ID")
    private String adminId;

    /**
     * 活动详情对象（存JSON）
     * 包含：简介、内容、时间、地点等
     */
    @TableField("ACTIVITY_OBJ")
    private String activityObj;

    /**
     * 封面图片列表（存JSON Array）
     */
    @TableField("ACTIVITY_PIC")
    private String activityPic;

    /**
     * 状态（1: 启用/已发布，0: 禁用/未发布）
     */
    @TableField("ACTIVITY_STATUS")
    private Integer activityStatus;

    /**
     * 创建时间戳
     */
    @TableField("ACTIVITY_ADD_TIME")
    private Long activityAddTime;

    /**
     * 修改时间戳
     */
    @TableField("ACTIVITY_EDIT_TIME")
    private Long activityEditTime;

    /**
     * 项目标识
     */
    @TableField("_pid")
    private String pid;

}