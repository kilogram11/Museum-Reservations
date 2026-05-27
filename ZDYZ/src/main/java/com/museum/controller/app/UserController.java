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

    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> params) {
        String mobile = params.get("mobile");
        String code = params.get("code");
        String token = userService.loginByMobile(mobile, code);
        Map<String, String> data = new HashMap<>();
        data.put("token", token);
        return Result.success("登录成功", data);
    }

    @GetMapping("/info")
    public Result info() {
        User user = userService.getUserInfo();
        return Result.success("获取成功", user);
    }

    @PostMapping("/update")
    public Result update(@RequestBody User user) {
        userService.updateUserInfo(user);
        return Result.success("修改成功");
    }

    @GetMapping("/heads")
    public Result getHeads() {
        return Result.success("获取成功", userService.getHeadList());
    }
}
