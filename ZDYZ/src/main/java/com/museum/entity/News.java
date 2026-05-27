package com.museum.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 公告/推文表 实体类
 * 对应数据库中的 news 表
 */
@Data
@TableName("news")
public class News {

    /**
     * 记录系统唯一标识符
     */
    @TableId("_id")
    private String id;

    /**
     * 公告业务 ID
     */
    @TableField("NEWS_ID")
    private String newsId;

    /**
     * 公告标题
     */
    @TableField("NEWS_TITLE")
    private String newsTitle;

    /**
     * 公告简介/内容
     */
    @TableField("NEWS_DESC")
    private String newsDesc;

    /**
     * 公告状态
     * 1: 正常展示
     * 0: 下线
     */
    @TableField("NEWS_STATUS")
    private Integer newsStatus;

    /**
     * 浏览量
     */
    @TableField("NEWS_VIEW_CNT")
    private Integer newsViewCnt;

    /**
     * 创建时间戳
     */
    @TableField("NEWS_ADD_TIME")
    private Long newsAddTime;

    /**
     * 修改时间戳
     */
    @TableField("NEWS_EDIT_TIME")
    private Long newsEditTime;

    /**
     * 项目标识
     */
    @TableField("_pid")
    private String pid;

    /**
     * 创建时的 IP 地址
     */
    @TableField("NEWS_ADD_IP")
    private String newsAddIp;

    /**
     * 修改时的 IP 地址
     */
    @TableField("NEWS_EDIT_IP")
    private String newsEditIp;
}