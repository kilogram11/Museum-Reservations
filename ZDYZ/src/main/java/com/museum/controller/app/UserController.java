package com.museum.controller.app;

import com.museum.common.result.Result;
import com.museum.entity.User;
import com.museum.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * App用户控制器
 */
@RestController
@RequestMapping("/app/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 手机号登录
     */
    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> params) {
        String mobile = params.get("mobile");
        String code = params.get("code");
        String token = userService.loginByMobile(mobile, code);
        Map<String, String> data = new HashMap<>();
        data.put("token", token);
        return Result.success("登录成功", data);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/info")
    public Result info() {
        User user = userService.getUserInfo();
        return Result.success("获取成功", user);
    }

    /**
     * 修改用户信息
     */
    @PostMapping("/update")
    public Result update(@RequestBody User user) {
        userService.updateUserInfo(user);
        return Result.success("修改成功");
    }

    /**
     * 获取所有可用头像
     */
    @GetMapping("/heads")
    public Result getHeads() {
        return Result.success("获取成功", userService.getHeadList());
    }
}
