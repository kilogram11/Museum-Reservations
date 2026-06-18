package com.museum.booking;

import com.museum.common.exception.BusinessException;
import com.museum.common.exception.ErrorCode;
import com.museum.common.utils.JwtUtil;
import com.museum.entity.User;
import com.museum.mapper.HeadMapper;
import com.museum.mapper.UserMapper;
import com.museum.service.impl.UserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * UserServiceImpl 单元测试 —— 用户认证与 Token 提纯
 * <p>
 * 覆盖登录/注册/信息查询全链路，验证卫语句拦截与异常传递。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl —— 用户认证与 Token 提纯")
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HeadMapper headMapper;

    /**
     * 手动注入 MyBatis-Plus 父类 baseMapper，
     * Mockito @InjectMocks 对父类 protected 泛型字段注入存在兼容性差异。
     */
    @BeforeEach
    void injectParentBaseMapper() {
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    // ================================================================
    //  测试方法
    // ================================================================

    /**
     * 【黑盒测试 · 等价类划分】—— 无效等价类：手机号为空/空白
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("C-1 黑盒/等价类-无效: 空手机号/空白手机号 → USER_MOBILE_EMPTY")
    void loginByMobile_blankMobile_throwsMobileEmpty(String mobile) {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                userService.loginByMobile(mobile, "1234"));

        assertEquals(ErrorCode.USER_MOBILE_EMPTY.getCode(), ex.getCode());
    }

    /**
     * 【黑盒测试 · 等价类划分】—— 无效等价类：验证码错误
     */
    @Test
    @DisplayName("C-2 黑盒/等价类-无效: 验证码错误 → VERIFY_CODE_ERROR")
    void loginByMobile_wrongVerifyCode_throwsVerifyCodeError() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                userService.loginByMobile("13800138000", "0000"));

        assertEquals(ErrorCode.VERIFY_CODE_ERROR.getCode(), ex.getCode());
    }

    /**
     * 【黑盒测试 · 边界值】—— 手机号仅含空格
     */
    @Test
    @DisplayName("C-3 黑盒/边界值: 手机号仅含空格 → USER_MOBILE_EMPTY")
    void loginByMobile_onlySpaces_throwsMobileEmpty() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                userService.loginByMobile("   ", "1234"));

        assertEquals(ErrorCode.USER_MOBILE_EMPTY.getCode(), ex.getCode());
    }

    /**
     * 【白盒测试 · 条件分支覆盖】—— 获取用户信息时 Token 为空
     */
    @Test
    @DisplayName("C-4 白盒/条件分支: Token 为空 → UNAUTHORIZED")
    void getUserInfo_blankToken_throwsUnauthorized() {
        when(request.getHeader("Token")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                userService.getUserInfo());

        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), ex.getCode());
    }

    /**
     * 【白盒测试 · 条件分支覆盖】—— Token 有效但用户不存在
     * 通过 spy + mock getEntityClass() 绕过 lambdaQuery 对 MapperProxy 的依赖
     */
    @Test
    @DisplayName("C-5 白盒/条件分支: Token 有效但用户不存在 → USER_NOT_FOUND")
    void getUserInfo_validTokenButUserNotFound_throwsUserNotFound() {
        when(request.getHeader("Token")).thenReturn("valid_token");
        when(jwtUtil.getSubjectFromToken("valid_token")).thenReturn("user123");

        UserServiceImpl spy = Mockito.spy(userService);
        doReturn(User.class).when(spy).getEntityClass();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                spy.getUserInfo());

        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
    }

    /**
     * 【黑盒测试 · 等价类划分】—— 有效等价类：新用户首次注册登录
     * 通过 spy + mock getEntityClass() 绕过 lambdaQuery 对 MapperProxy 的依赖
     */
    @Test
    @DisplayName("C-6 黑盒/等价类-有效: 新用户首次登录 → 自动注册并返回 Token")
    void loginByMobile_newUser_registersAndReturnsToken() {
        when(jwtUtil.generateAppToken(anyString())).thenReturn("generated_token_abc");

        UserServiceImpl spy = Mockito.spy(userService);
        doReturn(User.class).when(spy).getEntityClass();

        String token = spy.loginByMobile("13900139000", "1234");

        assertNotNull(token);
        assertEquals("generated_token_abc", token);
        verify(userMapper).insert(any(User.class));
    }
}
