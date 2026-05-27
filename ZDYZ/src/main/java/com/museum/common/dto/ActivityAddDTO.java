package com.museum.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 添加活动 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityAddDTO implements Serializable {

    /**
     * 活动标题
     */
    private String activityTitle;

    /**
     * 开始日期 (yyyy-MM-dd)
     */
    private String startDate;

    /**
     * 结束日期 (yyyy-MM-dd)
     */
    private String endDate;

    /**
     * 状态 (1: 启用, 0: 禁用)
     */
    private Integer status;

    /**
     * 管理员ID (前端传入)
     */
    private String adminId;

    /**
     * 图文混排内容
     */
    private List<ContentItem> content;

    /**
     * 图文内容项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentItem implements Serializable {
        /**
         * 类型: "text" | "img"
         */
        private String type;

        /**
         * 内容: 文本或图片URL
         */
        private String val;
    }
}
