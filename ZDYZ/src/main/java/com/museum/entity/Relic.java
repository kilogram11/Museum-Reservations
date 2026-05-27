package com.museum.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 文物信息表 实体类
 */
@Data
@TableName("relic")
public class Relic {

    /**
     * 对应模型识别的 Class ID (0,1,2,3,4)
     */
    @TableId("_id")
    private String id;

    @TableField("RELIC_NAME")
    private String relicName;

    /**
     * 长图文介绍
     */
    @TableField("RELIC_DESC")
    private String relicDesc;

    @TableField("RELIC_IMAGE")
    private String relicImage;
}
