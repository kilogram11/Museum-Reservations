package com.museum.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;
import java.util.List;

/**
 * 添加场馆 DTO
 * 包含场馆基本信息 + 排期配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MuseumAddDTO implements Serializable {
    // === 场馆基本信息 ===
    /**
     * 场馆标题
     */
    private String museumTitle;

    /**
     * 简介
     */
    private String museumDesc;

    /**
     * 封面图 URL
     */
    private String museumCover;

    /**
     * 轮播图列表
     */
    private List<String> museumImgs;

    /**
     * 详情内容 (HTML 或 JSON)
     */
    private String museumContent;

    /**
     * 地址
     */
    private String museumAddress;

    /**
     * 联系电话
     */
    private String museumPhone;

    /**
     * 交通指引
     */
    private String museumTraffic;

    /**
     * 状态 (1: 启用, 0: 禁用)
     */
    private Integer museumStatus;

    /**
     * 管理员ID (前端传入)
     */
    private String adminId;

    /**
     * 最大预约人数限制 (用于场馆级别控制)
     */
    private Integer museumMaxJoinCnt;

    /**
     * 提前预约天数
     */
    private Integer museumBookSet;

    // === 排期配置 ===
    /**
     * 排期开始日期 (yyyy-MM-dd)
     */
    private String startDate;

    /**
     * 排期结束日期 (yyyy-MM-dd)
     */
    private String endDate;

    /**
     * 每日时间段模板
     */
    private List<TimeTemplate> times;

    /**
     * 时间段模板内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeTemplate implements Serializable {
        /**
         * 开始时间 (HH:mm)
         */
        private String start;

        /**
         * 结束时间 (HH:mm)
         */
        private String end;

        /**
         * 该时段名额限制
         */
        private Integer limit;
    }

    // === 地图定位信息 ===

    /**
     * 场馆纬度（地图定位用）
     */
    private Double latitude;

    /**
     * 场馆经度（地图定位用）
     */
    private Double longitude;

    /**
     * 场馆地图地址（结构化地址）
     */
    private String address;

}
