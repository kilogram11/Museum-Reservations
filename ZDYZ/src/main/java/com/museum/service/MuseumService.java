package com.museum.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.museum.common.dto.MuseumAddDTO;
import com.museum.entity.Museum;

import java.util.List;
import java.util.Map;

/**
 * 场馆服务接口
 */
public interface MuseumService extends IService<Museum> {
    /**
     * 分页查询场馆列表
     *
     * @param keyword 关键词
     * @param page    页码
     * @param limit   每页数量
     * @return 分页结果
     */
    Page<Museum> dataList(String keyword, Integer page, Integer limit);

    /**
     * 添加场馆（包含排期配置）
     *
     * @param dto 添加参数
     */
    void addMuseum(MuseumAddDTO dto);

    /**
     * 编辑场馆
     *
     * @param dto 编辑参数
     */
    void editMuseum(com.museum.common.dto.MuseumEditDTO dto);

    /**
     * 删除场馆
     *
     * @param id 场馆ID
     */
    void delMuseum(String id);

    /**
     * 修改场馆状态
     *
     * @param id     场馆ID
     * @param status 状态
     */
    void status(String id, Integer status);

    /**
     * 获取所有场馆（用于下拉列表）
     *
     * @return 场馆简要信息列表
     */
    List<Map<String, Object>> getAllList();

    /**
     * 小程序端获取场馆列表
     */
    Page<Museum> appList(Integer page, Integer limit);
}
