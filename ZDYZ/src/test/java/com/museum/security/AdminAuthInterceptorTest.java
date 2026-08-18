package com.museum.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.museum.annotation.RequireLogin;
import com.museum.common.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AdminAuthInterceptor 单元测试（不启动 Spring / 不连 Redis、DB）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAuthInterceptor —— RequireLogin JWT 校验")
class AdminAuthInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    private AdminAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new AdminAuthInterceptor(jwtUtil, new ObjectMapper());
    }

    @RequireLogin
    static class ProtectedAdminController {
        public void list() {
        }
    }

    @Test
    @DisplayName("无 Bearer/Token 时返回 401")
    void rejectsMissingToken() throws Exception {
        HandlerMethod handler = new HandlerMethod(new ProtectedAdminController(),
                ProtectedAdminController.class.getMethod("list"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("Token")).thenReturn(null);

        assertFalse(interceptor.preHandle(request, response, handler));
        assertTrue(response.getContentAsString().contains("401"));
    }

    @Test
    @DisplayName("Authorization Bearer 有效时放行")
    void allowsBearerToken() throws Exception {
        HandlerMethod handler = new HandlerMethod(new ProtectedAdminController(),
                ProtectedAdminController.class.getMethod("list"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer admin-token");
        when(jwtUtil.validateToken("admin-token")).thenReturn(true);
        when(jwtUtil.getSubjectFromToken("admin-token")).thenReturn("admin-1");

        assertTrue(interceptor.preHandle(request, response, handler));
        verify(request).setAttribute(AdminAuthInterceptor.ATTR_ADMIN_ID, "admin-1");
    }
}
