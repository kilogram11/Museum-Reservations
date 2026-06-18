package com.museum.service.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.museum.entity.Identity;
import com.museum.entity.Join;
import com.museum.service.ExportService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExportServiceImpl implements ExportService {

    @Override
    public void exportJoinRecords(HttpServletResponse response, List<Join> joins) {
        List<Map<String, Object>> rows = buildJoinRows(joins);
        writeExcel(response, rows, buildJoinHeaderAlias(), "预约记录_");
    }

    @Override
    public void exportBlacklist(HttpServletResponse response, List<Identity> identities) {
        List<Map<String, Object>> rows = buildBlacklistRows(identities);
        writeExcel(response, rows, buildBlacklistHeaderAlias(), "黑名单记录_");
    }

    private List<Map<String, Object>> buildJoinRows(List<Join> joins) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Join join : joins) {
            Map<String, Object> row = new LinkedHashMap<>();
            Map<String, String> visitorInfo = parseVisitorInfo(join.getJoinForms());

            row.put("name", visitorInfo.get("name"));
            row.put("card", visitorInfo.get("card"));
            row.put("date", join.getJoinMeetDay());
            row.put("time", join.getJoinMeetTimeStart() + "-" + join.getJoinMeetTimeEnd());
            row.put("status", resolveJoinStatus(join));
            rows.add(row);
        }
        return rows;
    }

    private Map<String, String> parseVisitorInfo(String joinForms) {
        Map<String, String> visitorInfo = new LinkedHashMap<>();
        visitorInfo.put("name", "");
        visitorInfo.put("card", "");

        if (StrUtil.isNotBlank(joinForms)) {
            try {
                cn.hutool.json.JSONObject obj = JSONUtil.parseObj(joinForms);
                visitorInfo.put("name", obj.getStr("name"));
                visitorInfo.put("card", obj.getStr("card"));
            } catch (Exception ignored) {
                // 保持原导出行为：表单解析失败时导出空姓名和空证件号。
            }
        }
        return visitorInfo;
    }

    private String resolveJoinStatus(Join join) {
        String statusText = "未知";
        if (join.getJoinStatus() == null) {
            return statusText;
        }
        if (join.getJoinStatus() == 2) {
            return "已取消";
        }
        if (join.getJoinStatus() == 1 && join.getJoinIsCheckin() != null) {
            Integer checkin = join.getJoinIsCheckin();
            if (checkin == 1) {
                statusText = "已核销";
            } else if (checkin == 3) {
                statusText = "已爽约";
            } else {
                statusText = "待核销";
            }
        }
        return statusText;
    }

    private List<Map<String, Object>> buildBlacklistRows(List<Identity> identities) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Identity identity : identities) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", identity.getIdentityName());
            row.put("card", identity.getIdentityCard());
            row.put("mobile", identity.getIdentityMobile());
            row.put("reason", parseBlacklistReason(identity.getIdentityObj()));
            rows.add(row);
        }
        return rows;
    }

    private String parseBlacklistReason(String identityObj) {
        if (StrUtil.isBlank(identityObj)) {
            return "";
        }
        try {
            cn.hutool.json.JSONObject obj = JSONUtil.parseObj(identityObj);
            return obj.getStr("blackReason");
        } catch (Exception ignored) {
            return "";
        }
    }

    private Map<String, String> buildJoinHeaderAlias() {
        Map<String, String> headerAlias = new LinkedHashMap<>();
        headerAlias.put("name", "姓名");
        headerAlias.put("card", "身份证号");
        headerAlias.put("date", "预约日期");
        headerAlias.put("time", "时间段");
        headerAlias.put("status", "状态");
        return headerAlias;
    }

    private Map<String, String> buildBlacklistHeaderAlias() {
        Map<String, String> headerAlias = new LinkedHashMap<>();
        headerAlias.put("name", "姓名");
        headerAlias.put("card", "身份证号");
        headerAlias.put("mobile", "手机号");
        headerAlias.put("reason", "拉黑原因");
        return headerAlias;
    }

    private void writeExcel(HttpServletResponse response, List<Map<String, Object>> rows,
                            Map<String, String> headerAlias, String fileNamePrefix) {
        ExcelWriter writer = ExcelUtil.getWriter(true);
        try {
            headerAlias.forEach(writer::addHeaderAlias);
            writer.write(rows, true);
            writer.autoSizeColumnAll();

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            String fileName = URLEncoder.encode(fileNamePrefix + System.currentTimeMillis(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

            ServletOutputStream out = response.getOutputStream();
            writer.flush(out, true);
            IoUtil.close(out);
        } catch (Exception e) {
            throw new IllegalStateException("导出 Excel 失败", e);
        } finally {
            writer.close();
        }
    }
}
