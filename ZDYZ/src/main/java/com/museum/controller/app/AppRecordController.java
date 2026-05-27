package com.museum.controller.app;

import cn.hutool.core.util.StrUtil;
import com.museum.common.result.Result;
import com.museum.entity.Join;
import com.museum.service.JoinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * App 预约记录控制器
 */
@RestController
@RequestMapping("/app/record")
public class AppRecordController extends BaseAppController {

    @Autowired
    private JoinService joinService;

    @GetMapping("/list")
    public Result list() {
        String userId = getUserId();
        List<Join> list = joinService.getMyBookings(userId);
        return Result.success("获取成功", list);
    }

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

    @GetMapping("/detail")
    public Result detail(@RequestParam String joinId) {
        String userId = getUserId();
        Join target = findOwnerBooking(userId, joinId);
        if (target == null) {
            return Result.error(500, "记录不存在");
        }
        return Result.success("获取成功", target);
    }

    private Join findOwnerBooking(String userId, String joinId) {
        List<Join> list = joinService.getMyBookings(userId);
        for (Join booking : list) {
            if (booking.getJoinId().equals(joinId)) {
                return booking;
            }
        }
        return null;
    }
}
