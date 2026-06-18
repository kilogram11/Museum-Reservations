package com.museum.common.dto;

import lombok.Data;

/**
 * 修改公告 DTO
 */
@Data
public class NewsEditDTO {

    /**
     * 公告主键 ID (必填)
     */
    private String id;

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
     */
    private Integer newsStatus;
}
