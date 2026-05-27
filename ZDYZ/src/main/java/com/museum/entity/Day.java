package com.museum.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 每日排期表 实体类
 * 对应数据库中的 day 表
 *
 * 作用：记录每一天的排期情况（开馆/闭馆、当日预约上限）
 */
@Data
@TableName("day")
public class Day {

    /**
     * 记录系统唯一标识符
     */
    @TableId("_id")
    private String id;

    /**
     * 每日排期业务 ID
     */
    @TableField("DAY_ID")
    private String dayId;

    /**
     * 日期（格式：yyyy-MM-dd，如 "2025-12-30"）
     */
    @TableField("DAY")
    private String day;

    /**
     * 关联的场馆ID（外键关联museum表）
     */
    @TableField("MUSEUM_ID")
    private String museumId;

    /**
     * 关联的活动ID（外键关联activity表）
     */
    @TableField("ACTIVITY_ID")
    private String activityId;

    /**
     * 当日状态
     * 1: 可预约/开馆
     * 0: 不可预约/闭馆
     */
    @TableField("STATUS")
    private Integer status;

    /**
     * 当天总预约上限（当日所有时间段加起来的总人数限制）
     */
    @TableField("DAY_LIMIT_CNT")
    private Integer dayLimitCnt;

    /**
     * 创建时间戳
     */
    @TableField("ADD_TIME")
    private Long addTime;

    /**
     * 修改时间戳
     */
    @TableField("EDIT_TIME")
    private Long editTime;

    /**
     * 项目标识
     */
    @TableField("_pid")
    private String pid;
}