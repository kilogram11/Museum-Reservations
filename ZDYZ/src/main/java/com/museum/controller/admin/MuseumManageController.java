package com.museum.controller.admin;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.common.dto.MuseumAddDTO;
import com.museum.common.result.Result;
import com.museum.entity.Museum;
import com.museum.service.MuseumService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 场馆管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/museum")
public class MuseumManageController {

    @Autowired
    private MuseumService museumService;

    /**
     * 获取场馆列表
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
        Page<Museum> list = museumService.dataList(keyword, page, limit);
        return Result.success("获取成功", list);
    }

    /**
     * 场馆及排期录入
     */
    @PostMapping("/add")
    public Result add(@RequestBody MuseumAddDTO dto) {

        log.info("【新增场馆】latitude={}, longitude={}, address={}",
                dto.getLatitude(), dto.getLongitude(), dto.getAddress());

        museumService.addMuseum(dto);
        return Result.success("添加成功");
    }

    /**
     * 编辑场馆
     */
    @PostMapping("/edit")
    public Result edit(@RequestBody com.museum.common.dto.MuseumEditDTO dto) {
        museumService.editMuseum(dto);
        return Result.success("修改成功");
    }

    /**
     * 删除场馆
     */
    @PostMapping("/del")
    public Result del(@RequestBody Map<String, String> params) {
        String id = params.get("id");
        if (StrUtil.isBlank(id))
            return Result.error(500, "ID不能为空");
        museumService.delMuseum(id);
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
        museumService.status(id, status);
        return Result.success("操作成功");
    }

    /**
     * 获取详情
     */
    @GetMapping("/detail")
    public Result detail(@RequestParam String id) {
        Museum museum = museumService.getById(id);
        return Result.success("获取成功", museum);
    }

    /**
     * 获取所有场馆 (下拉列表)
     */
    @GetMapping("/all")
    public Result all() {
        return Result.success("获取成功", museumService.getAllList());
    }
}
