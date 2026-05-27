package com.museum.common.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 编辑活动 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ActivityEditDTO extends ActivityAddDTO {
    /**
     * 数据库主键 ID (必填)
     */
    private String id;
}
