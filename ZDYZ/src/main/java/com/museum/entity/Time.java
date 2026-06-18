package com.museum.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 时间段表 实体类
 * 对应数据库中的 time 表
 *
 * 作用：记录每天每个时间段的详细信息（库存、已预约数量）
 * 例如：2025-12-30 的 09:00-11:00 时段，限额50人，已预约30人
 */
@Data
@TableName("time")
public class Time {

    /**
     * 记录系统唯一标识符
     */
    @TableId("_id")
    private String id;

    /**
     * 时间段业务 ID
     */
    @TableField("TIME_ID")
    private String timeId;

    /**
     * 关联的日排期ID（外键关联day表）
     */
    @TableField("DAY_ID")
    private String dayId;

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
     * 时段开始时间（格式：HH:mm，如 "09:00"）
     */
    @TableField("TIME_START")
    private String timeStart;

    /**
     * 时段结束时间（格式：HH:mm，如 "11:00"）
     */
    @TableField("TIME_END")
    private String timeEnd;

    /**
     * 时段唯一标识（唯一索引）
     * 格式通常为：museumId_date_timeStart，如 "museum_001_2025-12-30_09:00"
     * 用于快速查询和防重复
     */
    @TableField("TIME_MARK")
    private String timeMark;

    /**
     * 时段最大预约名额（库存总量）
     */
    @TableField("LIMIT_CNT")
    private Integer limitCnt;

    /**
     * 已预约人数（已使用的库存）
     */
    @TableField("SUCC_CNT")
    private Integer succCnt;

    /**
     * 时段状态
     * 1: 启用
     * 0: 禁用
     */
    @TableField("STATUS")
    private Integer status;

    /**
     * 是否开启限流
     * 1: 开启（当succCnt >= limitCnt时不允许预约）
     * 0: 不限流（允许超额预约）
     */
    @TableField("IS_LIMIT")
    private Integer isLimit;

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