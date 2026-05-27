package com.museum.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 日志表 实体类
 * 对应数据库中的 log 表
 *
 * 作用：记录管理员的所有操作，便于追溯
 */
@Data
@TableName("log")
public class Log {

    /**
     * 记录系统唯一标识符
     */
    @TableId("_id")
    private String id;

    /**
     * 日志业务 ID
     */
    @TableField("LOG_ID")
    private String logId;

    /**
     * 日志内容（描述具体操作，如 "核销了预约记录 join_001"）
     */
    @TableField("LOG_CONTENT")
    private String logContent;

    /**
     * 日志类型
     * 0: 登录日志
     * 99: 操作日志
     */
    @TableField("LOG_TYPE")
    private Integer logType;

    /**
     * 操作管理员ID（外键关联admin表）
     */
    @TableField("LOG_ADMIN_ID")
    private String logAdminId;

    /**
     * 操作管理员名称（冗余字段，方便查询）
     */
    @TableField("LOG_ADMIN_NAME")
    private String logAdminName;

    /**
     * 记录时间戳
     */
    @TableField("LOG_ADD_TIME")
    private Long logAddTime;

    /**
     * 修改时间戳
     */
    @TableField("LOG_EDIT_TIME")
    private Long logEditTime;

    /**
     * 项目标识
     */
    @TableField("_pid")
    private String pid;
}