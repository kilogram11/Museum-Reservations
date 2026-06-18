package com.museum.controller.app;

import com.museum.common.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * App 端控制器基类
 * 提取公共 Token 解析逻辑，消除子类重复代码 (DRY)
 */
public abstract class BaseAppController {

    @Autowired
    protected HttpServletRequest request;

    @Autowired
    protected JwtUtil jwtUtil;

    protected String getUserId() {
        String token = request.getHeader("Token");
        return jwtUtil.getSubjectFromToken(token);
    }
}
