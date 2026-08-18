package com.museum.security;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 管理端认证拦截器：对标注 {@link RequireLogin} 的接口校验 Authorization Bearer 或 Token 头中的 JWT。
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_ADMIN_ID = "adminId";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public AdminAuthInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (!requiresLogin(handlerMethod)) {
            return true;
        }

        String token = resolveToken(request);
        if (token == null || token.isBlank() || !jwtUtil.validateToken(token)) {
            writeUnauthorized(response);
            return false;
        }

        String adminId = jwtUtil.getSubjectFromToken(token);
        if (adminId == null || adminId.isBlank()) {
            writeUnauthorized(response);
            return false;
        }

        request.setAttribute(ATTR_ADMIN_ID, adminId);
        return true;
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return request.getHeader("Token");
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
