package com.museum.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 博物馆/场馆表 实体类
 * 对应数据库中的 museum 表
 */
@Data
@TableName("museum")
public class Museum {

    /**
     * 记录系统唯一标识符
     */
    @TableId("_id")
    private String id;

    /**
     * 场馆业务 ID
     */
    @TableField("MUSEUM_ID")
    private String museumId;

    /**
     * 场馆标题/名称
     */
    @TableField("MUSEUM_TITLE")
    private String museumTitle;

    /**
     * 创建该场馆的管理员ID（外键关联admin表）
     */
    @TableField("ADMIN_ID")
    private String adminId;

    /**
     * 场馆详情对象（存JSON）
     * 包含：简介(desc)、封面(cover)、详细内容(content)、地址、电话等
     */
    @TableField("MUSEUM_OBJ")
    private String museumObj;

    /**
     * 封面图片列表（存JSON Array）
     */
    @TableField("MUSEUM_PIC")
    private String museumPic;

    /**
     * 最大预约人数/次数限制
     */
    @TableField("MUSEUM_MAX_JOIN_CNT")
    private Integer museumMaxJoinCnt;

    /**
     * 可提前多少天预约（例如：7表示可提前7天预约）
     */
    @TableField("MUSEUM_BOOK_SET")
    private Integer museumBookSet;

    /**
     * 状态（1: 启用，0: 禁用）
     */
    @TableField("MUSEUM_STATUS")
    private Integer museumStatus;

    /**
     * 创建时间戳
     */
    @TableField("MUSEUM_ADD_TIME")
    private Long museumAddTime;

    /**
     * 修改时间戳
     */
    @TableField("MUSEUM_EDIT_TIME")
    private Long museumEditTime;

    /**
     * 项目标识
     */
    @TableField("_pid")
    private String pid;


    /**
     * 场馆纬度（用于地图定位）
     */
    @TableField("LATITUDE")
    private Double latitude;

    /**
     * 场馆经度（用于地图定位）
     */
    @TableField("LONGITUDE")
    private Double longitude;

    /**
     * 场馆详细地址
     */
    @JsonProperty("museumAddress")
    @TableField("ADDRESS")
    private String address;

}