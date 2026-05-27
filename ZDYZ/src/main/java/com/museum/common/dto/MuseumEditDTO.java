package com.museum.common.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 编辑场馆 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MuseumEditDTO extends MuseumAddDTO {
    /**
     * 数据库主键 ID (必填)
     */
    private String id;
}
