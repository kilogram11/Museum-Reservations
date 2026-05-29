package com.museum.controller.admin;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.common.result.Result;
import com.museum.common.utils.PageParamUtil;
import com.museum.entity.Identity;
import com.museum.service.BlacklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    /**
     * 黑名单列表
     */
    @PostMapping("/list")
    public Result list(@RequestBody Map<String, Object> requestParams) {
        String keyword = (String) requestParams.get("keyword");
        int page = PageParamUtil.defaultPage(requestParams.get("page"));
        int limit = PageParamUtil.defaultLimit(requestParams.get("limit"));
        Integer status = (Integer) requestParams.get("status"); // 新增 status 参数
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
            // 简单支持时间戳字符串
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
            List<Map<String, Object>> rows = new ArrayList<>();

            for (Identity iden : list) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", iden.getIdentityName());
                row.put("card", iden.getIdentityCard());
                row.put("mobile", iden.getIdentityMobile());
                
                String reason = "";
                if (StrUtil.isNotBlank(iden.getIdentityObj())) {
                    try {
                        cn.hutool.json.JSONObject obj = JSONUtil.parseObj(iden.getIdentityObj());
                        reason = obj.getStr("blackReason");
                    } catch (Exception e) {
                        // ignore
                    }
                }
                row.put("reason", reason);
                
                rows.add(row);
            }

            ExcelWriter writer = ExcelUtil.getWriter(true);
            writer.addHeaderAlias("name", "姓名");
            writer.addHeaderAlias("card", "身份证号");
            writer.addHeaderAlias("mobile", "手机号");
            writer.addHeaderAlias("reason", "拉黑原因");

            writer.write(rows, true);
            writer.autoSizeColumnAll();

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            String fileName = URLEncoder.encode("黑名单记录_" + System.currentTimeMillis(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

            ServletOutputStream out = response.getOutputStream();
            writer.flush(out, true);
            writer.close();
            IoUtil.close(out);

        } catch (Exception e) {
            log.error("导出黑名单记录失败", e);
        }
    }
}
