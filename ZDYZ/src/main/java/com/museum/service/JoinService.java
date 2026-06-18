package com.museum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.museum.entity.Join;

import java.util.List;
import java.util.Map;

/**
 * 预约业务接口
 */
public interface JoinService extends IService<Join> {

    /**
     * 获取可预约的日期列表
     */
    List<Map<String, Object>> getBookingDays();

    /**
     * 获取指定日期的时段列表
     * 
     * @param dayStr 2025-01-01
     */
    List<Map<String, Object>> getBookingTimes(String dayStr);

    /**
     * 提交预约
     * 
     * @param userId      当前用户ID
     * @param timeMark    时段标识
     * @param identityIds 游客ID列表
     */
    void submitBooking(String userId, String timeMark, List<String> identityIds);

    /**
     * 获取我的预约记录
     * 
     * @param userId 当前用户ID
     */
    List<Join> getMyBookings(String userId);

    /**
     * 取消预约
     * 
     * @param userId 当前用户ID
     * @param joinId 预约ID
     */
    void cancelBooking(String userId, String joinId);

    /**
     * 管理端搜索预约记录 (分页)
     *
     * @param keyword 姓名或身份证号
     * @param page    页码
     * @param limit   每页条数
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<Join> adminList(String keyword, Integer page,
            Integer limit);

    /**
     * 核销 (管理员操作)
     *
     * @param id 预约ID (业务ID或主键)
     */
    void checkin(String id);

    /**
     * 获取所有预约数据用于导出
     */
    List<Join> getAllForExport();
}
