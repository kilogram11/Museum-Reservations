package com.museum.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.museum.ai.context.UserContext;
import com.museum.annotation.RequireLogin;
import com.museum.common.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
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
 * AppAuthInterceptor 单元测试（不启动 Spring / 不连 Redis、DB）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppAuthInterceptor —— RequireLogin / UserContext")
class AppAuthInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    private AppAuthInterceptor interceptor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        interceptor = new AppAuthInterceptor(jwtUtil, objectMapper);
        UserContext.clear();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @RequireLogin
    static class ProtectedController {
        @RequireLogin
        public void submit() {
        }

        public void days() {
        }
    }

    static class OpenController {
        public void days() {
        }
    }

    @Test
    @DisplayName("无 RequireLogin：无 Token 放行，且开头 clear 脏上下文")
    void openEndpoint_noToken_clearsStaleContext() throws Exception {
        UserContext.set("stale-user");
        HandlerMethod handler = new HandlerMethod(new OpenController(),
                OpenController.class.getMethod("days"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Token")).thenReturn(null);

        assertTrue(interceptor.preHandle(request, response, handler));
        assertNull(UserContext.get());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("无 RequireLogin + 有效 Token → 注入 UserContext")
    void openEndpoint_validToken_setsUserContext() throws Exception {
        HandlerMethod handler = new HandlerMethod(new OpenController(),
                OpenController.class.getMethod("days"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Token")).thenReturn("good-token");
        when(jwtUtil.validateToken("good-token")).thenReturn(true);
        when(jwtUtil.getSubjectFromToken("good-token")).thenReturn("user-ai");

        assertTrue(interceptor.preHandle(request, response, handler));
        assertEquals("user-ai", UserContext.get());
        verify(request).setAttribute(AppAuthInterceptor.ATTR_USER_ID, "user-ai");
    }

    @Test
    @DisplayName("RequireLogin 且无 Token 时返回 401")
    void rejectsMissingToken() throws Exception {
        HandlerMethod handler = new HandlerMethod(new ProtectedController(),
                ProtectedController.class.getMethod("submit"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Token")).thenReturn(null);

        assertFalse(interceptor.preHandle(request, response, handler));
        assertTrue(response.getContentAsString().contains("401"));
        assertNull(UserContext.get());
    }

    @Test
    @DisplayName("RequireLogin 且 Token 无效时返回 401")
    void rejectsInvalidToken() throws Exception {
        HandlerMethod handler = new HandlerMethod(new ProtectedController(),
                ProtectedController.class.getMethod("submit"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Token")).thenReturn("bad-token");
        when(jwtUtil.validateToken("bad-token")).thenReturn(false);

        assertFalse(interceptor.preHandle(request, response, handler));
        assertTrue(response.getContentAsString().contains("401"));
        assertNull(UserContext.get());
    }

    @Test
    @DisplayName("RequireLogin 且 Token 有效时放行并写入 userId")
    void allowsValidToken() throws Exception {
        HandlerMethod handler = new HandlerMethod(new ProtectedController(),
                ProtectedController.class.getMethod("submit"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Token")).thenReturn("good-token");
        when(jwtUtil.validateToken("good-token")).thenReturn(true);
        when(jwtUtil.getSubjectFromToken("good-token")).thenReturn("user-1");

        assertTrue(interceptor.preHandle(request, response, handler));
        verify(request).setAttribute(AppAuthInterceptor.ATTR_USER_ID, "user-1");
        assertEquals("user-1", UserContext.get());
    }

    @Test
    @DisplayName("afterCompletion 清理 UserContext")
    void afterCompletion_clearsUserContext() throws Exception {
        UserContext.set("user-1");
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        interceptor.afterCompletion(request, response, new Object(), null);

        assertNull(UserContext.get());
    }

    @Test
    @DisplayName("OPTIONS 预检直接放行")
    void allowsOptions() throws Exception {
        UserContext.set("stale");
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getMethod()).thenReturn("OPTIONS");

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertNull(UserContext.get());
        verifyNoInteractions(jwtUtil);
    }
}
