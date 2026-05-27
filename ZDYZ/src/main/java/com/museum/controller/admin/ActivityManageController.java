package com.museum.controller.admin;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.common.dto.ActivityAddDTO;
import com.museum.common.result.Result;
import com.museum.entity.Activity;
import com.museum.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 活动管理控制器
 */
@RestController
@RequestMapping("/admin/activity")
public class ActivityManageController {

    @Autowired
    private ActivityService activityService;

    /**
     * 获取活动列表
     */
    @PostMapping("/list")
    public Result list(@RequestBody Map<String, Object> params) {
        String keyword = (String) params.get("keyword");
        Integer page = (Integer) params.get("page");
        Integer limit = (Integer) params.get("limit");
        if (page == null)
            page = 1;
        if (limit == null)
            limit = 10;
        Page<Activity> list = activityService.dataList(keyword, page, limit);
        return Result.success("获取成功", list);
    }

    /**
     * 添加活动
     */
    @PostMapping("/add")
    public Result add(@RequestBody ActivityAddDTO dto) {
        activityService.addActivity(dto);
        return Result.success("添加成功");
    }

    /**
     * 编辑活动
     */
    @PostMapping("/edit")
    public Result edit(@RequestBody com.museum.common.dto.ActivityEditDTO dto) {
        activityService.editActivity(dto);
        return Result.success("修改成功");
    }

    /**
     * 删除活动
     */
    @PostMapping("/del")
    public Result del(@RequestBody Map<String, String> params) {
        String id = params.get("id");
        if (StrUtil.isBlank(id))
            return Result.error(500, "ID不能为空");
        activityService.delActivity(id);
        return Result.success("删除成功");
    }

    /**
     * 修改状态
     */
    @PostMapping("/status")
    public Result status(@RequestBody Map<String, Object> params) {
        String id = (String) params.get("id");
        Integer status = (Integer) params.get("status");
        if (StrUtil.isBlank(id) || status == null)
            return Result.error(500, "参数错误");
        activityService.status(id, status);
        return Result.success("操作成功");
    }

    /**
     * 获取详情
     */
    @GetMapping("/detail")
    public Result detail(@RequestParam String id) {
        Activity activity = activityService.getById(id);
        return Result.success("获取成功", activity);
    }
}
