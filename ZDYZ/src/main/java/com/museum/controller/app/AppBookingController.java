package com.museum.controller.app;

import com.museum.common.dto.BookingSubmitDTO;
import com.museum.common.result.Result;
import com.museum.common.utils.JwtUtil;
import com.museum.service.JoinService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * App 预约控制器
 */
@RestController
@RequestMapping("/app/booking")
public class AppBookingController {

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
     * 获取可预约日期
     */
    @GetMapping("/days")
    public Result getDays() {
        List<Map<String, Object>> days = joinService.getBookingDays();
        return Result.success("获取成功", days);
    }

    /**
     * 获取指定日期的时段
     */
    @GetMapping("/times")
    public Result getTimes(@RequestParam String day) {
        List<Map<String, Object>> times = joinService.getBookingTimes(day);
        return Result.success("获取成功", times);
    }

    /**
     * 提交预约
     */
    @PostMapping("/submit")
    public Result submit(@RequestBody BookingSubmitDTO dto) {
        String userId = getUserId();
        joinService.submitBooking(userId, dto.getTimeMark(), dto.getIdentityIds());
        return Result.success("预约成功");
    }
}
