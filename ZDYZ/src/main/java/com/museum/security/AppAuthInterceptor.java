package com.museum.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.museum.ai.context.UserContext;
import com.museum.annotation.RequireLogin;
import com.museum.common.exception.ErrorCode;
import com.museum.common.result.Result;
import com.museum.common.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 小程序端认证拦截器：
 * - 标注 {@link RequireLogin}：强制校验 Token；
 * - 未标注：有效 Token 则注入 UserContext（供 /ai 写 Tool），无效/缺失不 401。
 */
@Component
public class AppAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "appUserId";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public AppAuthInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        UserContext.clear();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        boolean requireLogin = requiresLogin(handlerMethod);
        String token = request.getHeader("Token");
        boolean hasToken = token != null && !token.isBlank();

        if (requireLogin) {
            if (!hasToken || !jwtUtil.validateToken(token)) {
                writeUnauthorized(response);
                return false;
            }
            String userId = jwtUtil.getSubjectFromToken(token);
            if (userId == null || userId.isBlank()) {
                writeUnauthorized(response);
                return false;
            }
            bindUser(request, userId);
            return true;
        }

        // 匿名可访问：带有效 Token 则注入上下文，供 AI Tool 使用
        if (hasToken && jwtUtil.validateToken(token)) {
            String userId = jwtUtil.getSubjectFromToken(token);
            if (userId != null && !userId.isBlank()) {
                bindUser(request, userId);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        UserContext.clear();
    }

    private void bindUser(HttpServletRequest request, String userId) {
        request.setAttribute(ATTR_USER_ID, userId);
        UserContext.set(userId);
    }

    private boolean requiresLogin(HandlerMethod handlerMethod) {
        return handlerMethod.getMethodAnnotation(RequireLogin.class) != null
                || handlerMethod.getBeanType().getAnnotation(RequireLogin.class) != null;
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.error(ErrorCode.UNAUTHORIZED));
    }
}
