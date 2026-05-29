package com.museum.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.common.result.Result;
import com.museum.common.utils.PageParamUtil;
import com.museum.entity.Join;
import com.museum.service.ExportService;
import com.museum.service.JoinService;
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
 * 预约核销管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/admin/join")
public class JoinManageController {

    @Autowired
    private JoinService joinService;

    @Autowired
    private ExportService exportService;

    /**
     * 预约列表搜索
     */
    @PostMapping("/list")
    public Result list(@RequestBody Map<String, Object> requestParams) {
        String keyword = (String) requestParams.getOrDefault("keyword", "");

        if (keyword.isEmpty()) {
            String name = (String) requestParams.get("name");
            String idCard = (String) requestParams.get("idCard");
            if (name != null && !name.isEmpty()) {
                keyword = name;
            } else if (idCard != null && !idCard.isEmpty()) {
                keyword = idCard;
            }
        }

        int page = PageParamUtil.defaultPage(requestParams.get("page"));
        int limit = PageParamUtil.defaultLimit(requestParams.get("limit"));

        Page<Join> result = joinService.adminList(keyword, page, limit);
        return Result.success("获取成功", result);
    }

    /**
     * 核销
     */
    @PostMapping("/checkin")
    public Result checkin(@RequestBody Map<String, String> requestParams) {
        String joinId = requestParams.get("id");
        joinService.checkin(joinId);
        return Result.success("核销成功");
    }

    /**
     * 导出所有预约记录 (Excel)
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        try {
            List<Join> list = joinService.getAllForExport();
            exportService.exportJoinRecords(response, list);
        } catch (Exception e) {
            log.error("导出预约记录失败", e);
        }
    }
}
