package com.museum.booking;

import com.museum.common.utils.JwtUtil;
import com.museum.controller.app.BaseAppController;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * BaseAppController 单元测试 —— Token 解析与权限继承
 * <p>
 * 验证基类 Token 提取逻辑的正确性及 DRY 原则下子类继承机制。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BaseAppController —— Token 解析与权限继承")
class BaseAppControllerTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private JwtUtil jwtUtil;

    // ================================================================
    //  测试方法
    // ================================================================

    /**
     * 【白盒测试 · 基本路径覆盖】—— getUserId() 正常路径
     */
    @Test
    @DisplayName("D-1 白盒/基本路径: Token 正常解析 → 返回 userId")
    void getUserId_validToken_returnsUserId() throws Exception {
        String expectedUserId = "user_abc_123";
        String validToken = "Bearer eyJhbG...";

        BaseAppController controller = new BaseAppController() {
        };
        ReflectionTestUtils.setField(controller, "request", request);
        ReflectionTestUtils.setField(controller, "jwtUtil", jwtUtil);

        when(request.getHeader("Token")).thenReturn(validToken);
        when(jwtUtil.getSubjectFromToken(validToken)).thenReturn(expectedUserId);

        String actualUserId = (String) ReflectionTestUtils.invokeMethod(controller, "getUserId");

        assertEquals(expectedUserId, actualUserId);
        verify(jwtUtil).getSubjectFromToken(validToken);
    }

    /**
     * 【白盒测试 · 基本路径覆盖】—— getUserId() Token 为 null
     */
    @Test
    @DisplayName("D-2 白盒/基本路径: Token 为 null → 返回 null")
    void getUserId_nullToken_returnsNull() throws Exception {
        BaseAppController controller = new BaseAppController() {
        };
        ReflectionTestUtils.setField(controller, "request", request);
        ReflectionTestUtils.setField(controller, "jwtUtil", jwtUtil);

        when(request.getHeader("Token")).thenReturn(null);
        when(jwtUtil.getSubjectFromToken(null)).thenReturn(null);

        String userId = (String) ReflectionTestUtils.invokeMethod(controller, "getUserId");

        assertNull(userId);
    }

    /**
     * 【结构测试】—— 验证子类继承基类后可复用 getUserId()
     */
    @Test
    @DisplayName("D-3 结构测试: 子类继承基类后可直接调用 getUserId()")
    void subclassInheritsGetUserId_correctly() throws Exception {
        class MockAppController extends BaseAppController {
        }

        MockAppController subController = new MockAppController();
        ReflectionTestUtils.setField(subController, "request", request);
        ReflectionTestUtils.setField(subController, "jwtUtil", jwtUtil);

        when(request.getHeader("Token")).thenReturn("tok_xyz");
        when(jwtUtil.getSubjectFromToken("tok_xyz")).thenReturn("user_xyz");

        String userId = (String) ReflectionTestUtils.invokeMethod(subController, "getUserId");
        assertEquals("user_xyz", userId);
    }
}
