package com.museum.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.common.result.Result;
import com.museum.common.utils.PageParamUtil;
import com.museum.entity.Join;
import com.museum.service.JoinService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
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
 * 预约核销管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/admin/join")
public class JoinManageController {

    @Autowired
    private JoinService joinService;

    /**
     * 预约列表搜索
     */
    @PostMapping("/list")
    public Result list(@RequestBody Map<String, Object> requestParams) {
        String keyword = (String) requestParams.getOrDefault("keyword", "");

        // 兼容前端传参：如果 keyword 为空，尝试取 name 或 idCard
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
            List<Map<String, Object>> rows = new ArrayList<>();

            for (Join join : list) {
                Map<String, Object> row = new LinkedHashMap<>();
                
                // 解析表单信息
                String name = "";
                String card = "";
                if (StrUtil.isNotBlank(join.getJoinForms())) {
                    try {
                        cn.hutool.json.JSONObject obj = JSONUtil.parseObj(join.getJoinForms());
                        name = obj.getStr("name");
                        card = obj.getStr("card");
                    } catch (Exception e) {
                        // ignore parse error
                    }
                }

                row.put("name", name);
                row.put("card", card);
                row.put("date", join.getJoinMeetDay());
                row.put("time", join.getJoinMeetTimeStart() + "-" + join.getJoinMeetTimeEnd());
                
                // 状态翻译
                String statusStr = "未知";
                if (join.getJoinStatus() != null) {
                    if (join.getJoinStatus() == 2) {
                        statusStr = "已取消";
                    } else if (join.getJoinStatus() == 1) {
                        Integer checkin = join.getJoinIsCheckin();
                        if (checkin != null) {
                            if (checkin == 1) statusStr = "已核销";
                            else if (checkin == 3) statusStr = "已爽约";
                            else statusStr = "待核销";
                        }
                    }
                }
                row.put("status", statusStr);
                
                rows.add(row);
            }

            // 通过工具类创建writer，默认创建xls格式
            ExcelWriter writer = ExcelUtil.getWriter(true); // true = xlsx

            // 自定义标题别名
            writer.addHeaderAlias("name", "姓名");
            writer.addHeaderAlias("card", "身份证号");
            writer.addHeaderAlias("date", "预约日期");
            writer.addHeaderAlias("time", "时间段");
            writer.addHeaderAlias("status", "状态");

            // 一次性写出内容，使用默认样式，强制输出标题
            writer.write(rows, true);
            // 自动列宽
            writer.autoSizeColumnAll();

            // response为HttpServletResponse对象
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            String fileName = URLEncoder.encode("预约记录_" + System.currentTimeMillis(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

            ServletOutputStream out = response.getOutputStream();

            writer.flush(out, true);
            // 关闭writer，释放内存
            writer.close();
            // 此处记得关闭输出Servlet流
            IoUtil.close(out);

        } catch (Exception e) {
            log.error("导出预约记录失败", e);
        }
    }
}
