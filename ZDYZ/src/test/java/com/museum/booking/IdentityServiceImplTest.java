package com.museum.booking;

import com.museum.common.exception.BusinessException;
import com.museum.entity.Identity;
import com.museum.mapper.IdentityMapper;
import com.museum.service.impl.IdentityServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * IdentityServiceImpl 单元测试 —— 游客身份管理
 * <p>
 * 覆盖新增、查询、删除三大操作路径，验证重构后扁平流水线的正确性。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IdentityServiceImpl —— 游客身份管理")
class IdentityServiceImplTest {

    @Mock
    private IdentityMapper identityMapper;

    @InjectMocks
    private IdentityServiceImpl identityService;

    @BeforeEach
    void injectParentBaseMapper() {
        ReflectionTestUtils.setField(identityService, "baseMapper", identityMapper);
    }

    // ================================================================
    //  测试方法
    // ================================================================

    /**
     * 【黑盒测试 · 等价类划分】—— 无效等价类：姓名为空
     */
    @Test
    @DisplayName("B-1 黑盒/等价类-无效: 姓名为空 → 抛出 BusinessException")
    void saveIdentity_blankName_throwsException() {
        Identity identity = new Identity();
        identity.setIdentityName("");
        identity.setIdentityCard("110101199001011234");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                identityService.saveIdentity("user001", identity));

        assertTrue(ex.getMessage().contains("姓名"));
    }

    /**
     * 【黑盒测试 · 等价类划分】—— 无效等价类：身份证格式非法
     */
    @Test
    @DisplayName("B-2 黑盒/等价类-无效: 身份证格式非法 → 抛出 BusinessException")
    void saveIdentity_invalidIdCard_throwsException() {
        Identity identity = new Identity();
        identity.setIdentityName("测试用户");
        identity.setIdentityCard("123456789012345");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                identityService.saveIdentity("user001", identity));

        assertTrue(ex.getMessage().contains("身份证"));
    }

    /**
     * 【黑盒测试 · 等价类划分】—— 有效等价类：新增不存在的游客
     * 通过 spy + mock getEntityClass() 绕过 MyBatis-Plus lambdaQuery 对 MapperProxy 的依赖
     */
    @Test
    @DisplayName("B-3 黑盒/等价类-有效: 新增不存在游客 → 成功创建")
    void saveIdentity_newVisitor_createsSuccessfully() {
        Identity identity = new Identity();
        identity.setIdentityName("赵六");
        identity.setIdentityCard("110101199001011237");
        identity.setIdentityMobile("13800138000");

        IdentityServiceImpl spy = Mockito.spy(identityService);
        doReturn(Identity.class).when(spy).getEntityClass();

        when(identityMapper.insert(any(Identity.class))).thenReturn(1);

        assertDoesNotThrow(() -> spy.saveIdentity("user001", identity));

        verify(identityMapper).insert(any(Identity.class));
    }

    /**
     * 【白盒测试 · 基本路径覆盖】—— 查询游客列表时 userId 为空
     */
    @Test
    @DisplayName("B-4 白盒/基本路径: userId 为空 → 返回空列表")
    void listMyIdentity_blankUserId_returnsEmptyList() {
        List<Identity> result = identityService.listMyIdentity(null);
        assertTrue(result.isEmpty());

        result = identityService.listMyIdentity("");
        assertTrue(result.isEmpty());
    }

    /**
     * 【白盒测试 · 条件分支覆盖】—— 删除不存在的游客（early-return 路径）
     * 通过 spy + mock getEntityClass() 绕过 lambdaQuery 对 MapperProxy 的依赖
     */
    @Test
    @DisplayName("B-5 白盒/条件分支: 删除不存在游客 → 静默返回（无异常）")
    void removeIdentity_notFound_silentlyReturns() {
        IdentityServiceImpl spy = Mockito.spy(identityService);
        doReturn(Identity.class).when(spy).getEntityClass();

        assertDoesNotThrow(() -> spy.removeIdentity("user001", "ID_NON_EXIST"));
    }
}
