package com.museum.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 头像表 实体类
 * 对应数据库中的 head 表
 *
 * 作用：存储用户头像的URL地址
 */
@Data
@TableName("head")
public class Head {

    /**
     * 记录系统唯一标识符
     */
    @TableId("_id")
    private String id;

    /**
     * 头像业务 ID
     */
    @TableField("HEAD_PIC_ID")
    private String headPicId;

    /**
     * 头像云服务器地址（完整URL）
     */
    @TableField("HEAD_PIC_URL")
    private String headPicUrl;

    /**
     * 项目标识
     */
    @TableField("_pid")
    private String pid;
}