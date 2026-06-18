package com.museum.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 身份用户表 实体类
 * 对应数据库中的 identity 表
 *
 * 说明：
 * - 一个小程序用户(User)可以添加多个预约人(Identity)
 * - 预约时选择某个Identity进行预约
 * - 用于黑名单管理和实名制预约
 */
@Data
@TableName("identity")
public class Identity {

    /**
     * 记录系统唯一标识符
     */
    @TableId("_id")
    private String id;

    /**
     * 身份用户业务 ID
     */
    @TableField("IDENTITY_ID")
    private String identityId;

    /**
     * 关联的账户ID数组（存JSON Array）
     * 表示哪些User账号拥有这个身份信息
     */
    @TableField("USER_ID")
    private String userId;

    /**
     * 真实姓名
     */
    @TableField("IDENTITY_NAME")
    private String identityName;

    /**
     * 身份证号（唯一索引，用于去重和黑名单检测）
     */
    @TableField("IDENTITY_CARD")
    private String identityCard;

    /**
     * 手机号
     */
    @TableField("IDENTITY_MOBILE")
    private String identityMobile;

    /**
     * 扩展信息（存JSON）
     */
    @TableField("IDENTITY_OBJ")
    private String identityObj;

    /**
     * 状态（1: 正常，0: 黑名单）
     */
    @TableField("IDENTITY_STATUS")
    private Integer identityStatus;

    /**
     * 拉黑开始时间戳
     */
    @TableField("BLACK_START_TIME")
    private Long blackStartTime;

    /**
     * 拉黑结束时间戳
     */
    @TableField("BLACK_END_TIME")
    private Long blackEndTime;

    /**
     * 违约次数（爽约次数累计）
     */
    @TableField("USER_BAN_NUM")
    private Integer userBanNum;

    /**
     * 拉黑类型（1: 自动拉黑，0: 手动拉黑）
     */
    @TableField("USER_CHECK_TYPE")
    private Integer userCheckType;

    /**
     * 项目标识
     */
    @TableField("_pid")
    private String pid;
}