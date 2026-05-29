package com.museum.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理端认证拦截器骨架。
 *
 * <p>当前阶段只补齐标准拦截器结构，不注册到 WebMvcConfig，避免改变现有接口行为。</p>
 */
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return true;
    }
}
