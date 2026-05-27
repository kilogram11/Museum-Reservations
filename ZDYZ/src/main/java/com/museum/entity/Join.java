package com.museum.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 预约记录表 实体类
 * 对应数据库中的 join 表
 *
 * ⭐ 这是整个系统最核心的表
 * 记录了所有的预约信息、核销状态、爽约情况等
 */
@Data
@TableName("`join`") // 注意：join是SQL关键字，需要加反引号
public class Join {

    /**
     * 记录系统唯一标识符
     */
    @TableId("_id")
    private String id;

    /**
     * 预约记录业务 ID
     */
    @TableField("JOIN_ID")
    private String joinId;

    /**
     * 关联的身份用户ID（外键关联identity表）
     * 记录是谁预约的
     */
    @TableField("IDENTITY_ID")
    private String identityId;

    /**
     * 关联的账户ID（外键关联user表）
     * 记录是哪个小程序用户提交的预约
     */
    @TableField("USER_ID")
    private String userId;

    /**
     * 预约参观日期（格式：yyyy-MM-dd，如 "2025-12-30"）
     */
    @TableField("JOIN_MEET_DAY")
    private String joinMeetDay;

    /**
     * 时段唯一标识（外键关联time表）
     * 用于快速定位是哪个时间段的预约
     */
    @TableField("TIME_MARK")
    private String timeMark;

    /**
     * 预约生效时间戳（即预约开始时间的时间戳）
     */
    @TableField("JOIN_START_TIME")
    private Long joinStartTime;

    /**
     * 完整结束时间字符串（格式：yyyy-MM-dd HH:mm:ss）
     */
    @TableField("JOIN_COMPLETE_END_TIME")
    private String joinCompleteEndTime;

    /**
     * 预约状态
     * 1: 预约成功
     * 2: 取消预约
     */
    @TableField("JOIN_STATUS")
    private Integer joinStatus;

    /**
     * 用户填写的表单数据（存JSON Array）
     * 记录用户预约时填写的所有信息
     */
    @TableField("JOIN_FORMS")
    private String joinForms;

    /**
     * 核销状态
     * 0: 未核销
     * 1: 已核销
     * 3: 逾期未核销（爽约）
     */
    @TableField("JOIN_IS_CHECKIN")
    private Integer joinIsCheckin;

    /**
     * 核销二维码地址
     * 用户到场时扫码核销
     */
    @TableField("JOIN_QR")
    private String joinQr;

    /**
     * 下单时间戳（预约提交时间）
     */
    @TableField("JOIN_ADD_TIME")
    private Long joinAddTime;

    /**
     * 修改时间戳
     */
    @TableField("JOIN_EDIT_TIME")
    private Long joinEditTime;

    /**
     * 项目标识
     */
    @TableField("_pid")
    private String pid;

    @TableField(exist = false)
    private String joinMeetTimeStart;

    @TableField(exist = false)
    private String joinMeetTimeEnd;

    @TableField(exist = false)
    private String museumTitle;

    @TableField(exist = false)
    private String museumAddress;

    @TableField(exist = false)
    private Double latitude;

    @TableField(exist = false)
    private Double longitude;

}