package com.museum.controller.admin;

import cn.hutool.core.util.StrUtil;
import com.museum.common.dto.AdminLoginDTO;
import com.museum.common.result.Result;
import com.museum.entity.Admin;
import com.museum.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员登录控制器
 */
@RestController
@RequestMapping("/admin/auth")
public class AdminLoginController {

    @Autowired
    private AdminService adminService;

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result login(@RequestBody AdminLoginDTO loginDTO) {
        Admin admin = adminService.login(loginDTO);
        Map<String, String> map = new HashMap<>();
        map.put("token", admin.getAdminToken());
        map.put("adminId", admin.getAdminId());
        return Result.success("登录成功", map);
    }

    /**
     * 注册 (需密钥)
     */
    @PostMapping("/register")
    public Result register(@RequestBody AdminLoginDTO loginDTO) {
        if (!"e7g6s679Ty67N9fTh98".equals(loginDTO.getSecretKey())) {
            return Result.error("注册密钥错误，无法注册");
        }
        Admin admin = adminService.register(loginDTO);
        return Result.success("注册成功", admin);
    }

    /**
     * 获取管理员信息
     */
    @GetMapping("/info")
    public Result info() {
        Admin admin = adminService.getAdminInfo();
        return Result.success("获取成功", admin);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result logout() {
        adminService.logout();
        return Result.success("退出成功");
    }

    // ========== 新增个人信息接口（与前端对应） ==========
    /**
     * 1. 获取个人信息（前端弹窗初始化时调用）
     */
    @GetMapping("/profile")
    public Result getAdminProfile() {
        // 直接通过 service 获取当前登录人的 profile
        // 内部已经包含了 Token 校验逻辑
        Admin profile = adminService.getAdminInfo();

        // 组装前端所需数据格式，确保即使数据库字段为空也有保底显示
        Map<String, Object> data = new HashMap<>();
        data.put("userName",
                StrUtil.isNotBlank(profile.getAdminNickname()) ? profile.getAdminNickname() : profile.getAdminName());
        data.put("userIntro", StrUtil.isNotBlank(profile.getAdminIntro()) ? profile.getAdminIntro() : "管理员");
        data.put("currentAvatar",
                StrUtil.isNotBlank(profile.getAdminAvatar()) ? profile.getAdminAvatar() : "/src/assets/avatars/1.jpg");

        return Result.success("获取成功", data);
    }

    /**
     * 2. 更新用户名和简介（前端编辑后调用）
     */
    @PostMapping("/profile/update")
    public Result updateAdminProfile(@RequestBody Map<String, String> params) {
        Admin currentAdmin = adminService.getAdminInfo();
        String adminId = currentAdmin.getAdminId();

        Admin admin = new Admin();
        admin.setAdminId(adminId);
        admin.setAdminNickname(params.get("userName"));
        admin.setAdminIntro(params.get("userIntro"));

        boolean success = adminService.updateAdminProfile(admin);
        return success ? Result.success("个人信息修改成功") : Result.error("修改失败");
    }

    /**
     * 3. 更新头像（前端切换头像后调用）
     */
    @PostMapping("/profile/update-avatar")
    public Result updateAdminAvatar(@RequestBody Map<String, String> params) {
        Admin currentAdmin = adminService.getAdminInfo();
        String adminId = currentAdmin.getAdminId();
        String avatarUrl = params.get("avatarUrl");

        boolean success = adminService.updateAdminAvatar(adminId, avatarUrl);
        return success ? Result.success("头像修改成功", avatarUrl) : Result.error("头像修改失败");
    }
}
