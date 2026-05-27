package com.museum.controller.app;

import cn.hutool.core.util.StrUtil;
import com.museum.common.result.Result;
import com.museum.common.utils.JwtUtil;
import com.museum.entity.Join;
import com.museum.service.JoinService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * App 预约记录控制器
 */
@RestController
@RequestMapping("/app/record")
public class AppRecordController {

    @Autowired
    private JoinService joinService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private HttpServletRequest request;

    private String getUserId() {
        String token = request.getHeader("Token");
        return jwtUtil.getSubjectFromToken(token);
    }

    /**
     * 获取我的预约记录
     */
    @GetMapping("/list")
    public Result list() {
        String userId = getUserId();
        List<Join> list = joinService.getMyBookings(userId);
        return Result.success("获取成功", list);
    }

    /**
     * 取消预约
     */
    @PostMapping("/cancel")
    public Result cancel(@RequestBody Map<String, String> params) {
        String userId = getUserId();
        String joinId = params.get("joinId");
        if (StrUtil.isBlank(joinId)) {
            return Result.error(500, "参数错误");
        }
        joinService.cancelBooking(userId, joinId);
        return Result.success("取消成功");
    }

    /**
     * 获取核销二维码详情
     * (一般列表接口已经返回了 joinQr，这里可以是单独获取详情)
     */
    @GetMapping("/detail")
    public Result detail(@RequestParam String joinId) {
        String userId = getUserId();
        // 复用 getMyBookings 逻辑或者单独查询
        // 这里简单直接查库
        // 实际项目建议封装 verifyOwnership
        List<Join> list = joinService.getMyBookings(userId);
        Join target = list.stream().filter(j -> j.getJoinId().equals(joinId)).findFirst().orElse(null);
        if (target == null)
            return Result.error(500, "记录不存在");
        return Result.success("获取成功", target);
    }
}
