package com.museum.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.museum.common.dto.AdminLoginDTO;
import com.museum.common.exception.BusinessException;
import com.museum.entity.Admin;
import com.museum.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class AdminServiceImplIntegrationTest extends AdminBackendIntegrationTestBase {

    @Autowired
    private AdminService adminService;

    @Test
    void registerAndLogin_withValidCredentials_shouldCreateAdminAndReturnToken() {
        AdminLoginDTO registerDTO = new AdminLoginDTO();
        registerDTO.setUsername(TEST_PREFIX + "login_user");
        registerDTO.setPassword("Pass123456");

        Admin registered = adminService.register(registerDTO);

        assertNotNull(registered.getId());
        assertEquals(TEST_PREFIX + "login_user", registered.getAdminName());
        assertNull(registered.getAdminPassword());

        AdminLoginDTO loginDTO = new AdminLoginDTO();
        loginDTO.setUsername("  " + TEST_PREFIX + "login_user  ");
        loginDTO.setPassword("  Pass123456  ");

        Admin loggedIn = adminService.login(loginDTO);

        assertNotNull(loggedIn.getAdminToken());
        assertNull(loggedIn.getAdminPassword());
        assertNotNull(adminMapper.selectOne(
                new QueryWrapper<Admin>().eq("ADMIN_NAME", TEST_PREFIX + "login_user")).getAdminToken());
    }

    @Test
    void register_withDuplicateUsername_shouldThrowBusinessException() {
        AdminLoginDTO dto = new AdminLoginDTO();
        dto.setUsername(TEST_PREFIX + "duplicate_user");
        dto.setPassword("Pass123456");

        adminService.register(dto);

        assertThrows(BusinessException.class, () -> adminService.register(dto));
    }

    @Test
    void login_withBlankUsername_shouldThrowBusinessException() {
        AdminLoginDTO dto = new AdminLoginDTO();
        dto.setUsername(" ");
        dto.setPassword("Pass123456");

        assertThrows(BusinessException.class, () -> adminService.login(dto));
    }

    @Test
    void login_withWrongPassword_shouldThrowBusinessException() {
        AdminLoginDTO registerDTO = new AdminLoginDTO();
        registerDTO.setUsername(TEST_PREFIX + "wrong_password_user");
        registerDTO.setPassword("Pass123456");
        adminService.register(registerDTO);

        AdminLoginDTO loginDTO = new AdminLoginDTO();
        loginDTO.setUsername(TEST_PREFIX + "wrong_password_user");
        loginDTO.setPassword("WrongPass");

        assertThrows(BusinessException.class, () -> adminService.login(loginDTO));
    }
}
