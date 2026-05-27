package com.museum.controller.app;

import com.museum.common.result.Result;
import com.museum.entity.Identity;
import com.museum.service.IdentityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * App端常用游客管理
 */
@RestController
@RequestMapping("/app/identity")
public class AppIdentityController extends BaseAppController {

    @Autowired
    private IdentityService identityService;

    @GetMapping("/list")
    public Result list() {
        String userId = getUserId();
        List<Identity> list = identityService.listMyIdentity(userId);
        return Result.success("获取成功", list);
    }

    @PostMapping("/save")
    public Result save(@RequestBody Identity identity) {
        String userId = getUserId();
        identityService.saveIdentity(userId, identity);
        return Result.success("保存成功");
    }

    @PostMapping("/del")
    public Result del(@RequestBody Map<String, String> params) {
        String userId = getUserId();
        String identityId = params.get("identityId");
        identityService.removeIdentity(userId, identityId);
        return Result.success("删除成功");
    }
}
