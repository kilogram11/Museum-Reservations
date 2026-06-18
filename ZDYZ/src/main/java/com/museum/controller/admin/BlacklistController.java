package com.museum.controller.admin;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.common.result.Result;
import com.museum.common.utils.PageParamUtil;
import com.museum.entity.Identity;
import com.museum.service.BlacklistService;
import com.museum.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 黑名单管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/blacklist")
public class BlacklistController {

    @Autowired
    private BlacklistService blacklistService;

    @Autowired
    private ExportService exportService;

    /**
     * 黑名单列表
     */
    @PostMapping("/list")
    public Result list(@RequestBody Map<String, Object> requestParams) {
        String keyword = (String) requestParams.get("keyword");
        int page = PageParamUtil.defaultPage(requestParams.get("page"));
        int limit = PageParamUtil.defaultLimit(requestParams.get("limit"));
        Integer status = (Integer) requestParams.get("status");
        Page<Identity> list = blacklistService.list(keyword, page, limit, status);
        return Result.success("获取成功", list);
    }

    /**
     * 加入黑名单
     */
    @PostMapping("/add")
    public Result add(@RequestBody Map<String, Object> requestParams) {
        String identityId = (String) requestParams.get("identityId");
        String reason = (String) requestParams.get("reason");
        Object endTimeObj = requestParams.get("endTime");

        if (StrUtil.isBlank(identityId) || endTimeObj == null) {
            return Result.error(500, "参数错误: ID和结束时间必填");
        }

        Long endTime = 0L;
        if (endTimeObj instanceof Long) {
            endTime = (Long) endTimeObj;
        } else if (endTimeObj instanceof String) {
            endTime = Long.parseLong((String) endTimeObj);
        }

        blacklistService.add(identityId, reason, endTime);
        return Result.success("操作成功");
    }

    /**
     * 更新黑名单结束时间
     */
    @PostMapping("/updateTime")
    public Result updateTime(@RequestBody Map<String, Object> requestParams) {
        String identityId = (String) requestParams.get("identityId");
        Object endTimeObj = requestParams.get("endTime");

        if (StrUtil.isBlank(identityId) || endTimeObj == null) {
            return Result.error(500, "参数错误");
        }

        Long endTime = 0L;
        if (endTimeObj instanceof Long) {
            endTime = (Long) endTimeObj;
        } else if (endTimeObj instanceof String) {
            endTime = Long.parseLong((String) endTimeObj);
        }

        blacklistService.updateEndTime(identityId, endTime);
        return Result.success("操作成功");
    }

    /**
     * 移除黑名单
     */
    @PostMapping("/remove")
    public Result remove(@RequestBody Map<String, String> requestParams) {
        String identityId = requestParams.get("identityId");
        if (StrUtil.isBlank(identityId)) {
            return Result.error(500, "参数错误");
        }
        blacklistService.remove(identityId);
        return Result.success("操作成功");
    }

    /**
     * 导出黑名单 (Excel)
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        try {
            List<Identity> list = blacklistService.getAllBlacklistForExport();
            exportService.exportBlacklist(response, list);
        } catch (Exception e) {
            log.error("导出黑名单记录失败", e);
        }
    }
}
