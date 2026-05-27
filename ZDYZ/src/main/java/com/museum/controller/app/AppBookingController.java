package com.museum.controller.app;

import com.museum.common.dto.BookingSubmitDTO;
import com.museum.common.result.Result;
import com.museum.service.JoinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * App 预约控制器
 */
@RestController
@RequestMapping("/app/booking")
public class AppBookingController extends BaseAppController {

    @Autowired
    private JoinService joinService;

    @GetMapping("/days")
    public Result getDays() {
        List<Map<String, Object>> days = joinService.getBookingDays();
        return Result.success("获取成功", days);
    }

    @GetMapping("/times")
    public Result getTimes(@RequestParam String day) {
        List<Map<String, Object>> times = joinService.getBookingTimes(day);
        return Result.success("获取成功", times);
    }

    @PostMapping("/submit")
    public Result submit(@RequestBody BookingSubmitDTO dto) {
        String userId = getUserId();
        joinService.submitBooking(userId, dto.getTimeMark(), dto.getIdentityIds());
        return Result.success("预约成功");
    }
}
