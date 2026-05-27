package com.museum.common.dto;

import lombok.Data;

/**
 * 新增公告 DTO
 */
@Data
public class NewsAddDTO {

    /**
     * 公告标题
     */
    private String newsTitle;

    /**
     * 公告简介/内容
     */
    private String newsDesc;

    /**
     * 状态 (1: 正常, 0: 禁用)
     * 选填，默认为 1
     */
    private Integer newsStatus;

    /**
     * 其他可选内容 (若有其他扩展字段，可在此添加)
     */
    private String newsContent;
}
