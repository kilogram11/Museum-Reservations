package com.museum.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.common.dto.AdminLoginDTO;
import com.museum.common.exception.BusinessException;
import com.museum.common.utils.JwtUtil;
import com.museum.common.utils.MD5Util;
import com.museum.entity.Admin;
import com.museum.mapper.AdminMapper;
import com.museum.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 管理员业务实现类
 */
@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private HttpServletRequest request;

    @Override
    public Admin login(AdminLoginDTO dto) {
        // 1. 去除首尾空格，防止因复制粘贴产生额外空格导致登录失败
        String username = dto.getUsername() != null ? dto.getUsername().trim() : "";
        String password = dto.getPassword() != null ? dto.getPassword().trim() : "";

        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            throw BusinessException.of("用户名或密码不能为空");
        }

        Admin admin = this.lambdaQuery().eq(Admin::getAdminName, username).one();
        if (admin == null) {
            throw BusinessException.of("用户不存在");
        }

        // 2. 获取数据库密码并去空格 (防止 CHAR 类型自动补空格)
        String dbPassword = admin.getAdminPassword();
        if (dbPassword != null) {
            dbPassword = dbPassword.trim();
        }

        String inputPwdMd5 = MD5Util.encrypt(password);
        boolean isMd5Match = inputPwdMd5.equals(dbPassword);
        boolean isPlainMatch = false;

        if (!isMd5Match) {
            // 兼容性检查: 明文匹配 (Trimmed)
            if (password.equals(dbPassword)) {
                isPlainMatch = true;
            } else {
                throw BusinessException.of("密码错误");
            }
        }

        // 3. 安全升级: 如果是明文登录成功，自动更新为 MD5 加密
        if (isPlainMatch) {
            admin.setAdminPassword(inputPwdMd5);
        }

        // 4. 健壮性补全: 如果管理员缺失业务 ID (ADMIN_ID)，自动补全
        if (StrUtil.isBlank(admin.getAdminId())) {
            admin.setAdminId("admin_" + admin.getId());
        }

        String token = jwtUtil.generateAdminToken(admin.getAdminId());
        admin.setAdminToken(token);
        admin.setAdminTokenTime(System.currentTimeMillis());
        this.updateById(admin);

        // 脱敏
        admin.setAdminPassword(null);
        return admin;
    }

    @Override
    public Admin register(AdminLoginDTO dto) {
        String username = dto.getUsername();
        String password = dto.getPassword();

        if (StrUtil.hasBlank(username, password)) {
            throw BusinessException.of("用户名或密码不能为空");
        }

        // 检查用户名唯一性
        Long count = this.lambdaQuery().eq(Admin::getAdminName, username).count();
        if (count > 0) {
            throw BusinessException.of("用户名已存在");
        }

        Admin admin = new Admin();
        // 如果 Entity 使用了 @TableId(type = IdType.ASSIGN_ID) 则不需要手动 set id
        // 但 database.txt 显示是 VARCHAR(64) 且 IDGenerator 也是一种选择，这里沿用 IdGenerator
        // admin.setId(IdGenerator.generateId()); // 如果需要物理主键
        // Admin ID (业务主键)
        // 假设实体类有 ID 生成策略或手动生成
        String id = cn.hutool.core.util.IdUtil.fastSimpleUUID();
        admin.setId(id);
        admin.setAdminId("admin_" + id);
        admin.setAdminId("admin_" + id);
        admin.setAdminName(username);
        admin.setAdminPassword(MD5Util.encrypt(password));
        admin.setAdminAddTime(System.currentTimeMillis());
        // admin.setAdminType(1); // 默认类型?

        this.save(admin);

        // 注册即登录? 或者返回不含Token的信息?
        // 简单起见，这里不需要自动登录，或者依据需求。
        // 为了方便，这里不自动登录，返回脱敏后的信息
        admin.setAdminPassword(null);
        return admin;
    }

    @Override
    public Admin getAdminInfo() {
        // 从 Header 获取 Token
        String token = request.getHeader("Token");
        if (StrUtil.isBlank(token)) {
            // 尝试从 Authorization Bearer 获取
            String auth = request.getHeader("Authorization");
            if (StrUtil.isNotBlank(auth) && auth.startsWith("Bearer ")) {
                token = auth.substring(7);
            }
        }

        if (StrUtil.isBlank(token)) {
            throw BusinessException.of(401, "未登录");
        }

        // 无论是哪种方式，都需要校验 Token
        String adminId = jwtUtil.getSubjectFromToken(token);
        if (StrUtil.isBlank(adminId)) {
            throw BusinessException.of(401, "无效的令牌");
        }

        // 根据 AdminId 查询（注意 database.txt 里是 ADMIN_ID 业务主键，还是 _id 物理主键？）
        // jwtUtil.generateAdminToken(admin.getAdminId()) 传入的是 adminId
        // 所以这里 query eq ADMIN_ID
        Admin admin = this.lambdaQuery().eq(Admin::getAdminId, adminId).one();
        if (admin == null) {
            throw BusinessException.of(401, "管理员不存在");
        }

        // 脱敏密码
        admin.setAdminPassword(null);
        return admin;
    }

    @Override
    public void logout() {
        String token = request.getHeader("Token");
        if (StrUtil.isBlank(token))
            return;

        String adminId = jwtUtil.getSubjectFromToken(token);
        if (StrUtil.isNotBlank(adminId)) {
            // 清除数据库中的 Token (可选，实现单点登录互踢等功能时有用)
            LambdaUpdateWrapper<Admin> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Admin::getAdminId, adminId).set(Admin::getAdminToken, null);
            this.update(updateWrapper);
        }
    }

    // ========== 新增个人信息业务方法实现 ==========
    @Override
    public Admin getAdminProfile(String adminId) {
        if (StrUtil.isBlank(adminId)) {
            throw BusinessException.of("管理员ID不能为空");
        }
        // 使用 Lambda 查询确保返回完整的实体对象，避免字段缺失导致前端错误
        Admin profile = this.lambdaQuery().eq(Admin::getAdminId, adminId).one();
        if (profile == null) {
            throw BusinessException.of("个人信息不存在");
        }
        // 脱敏
        profile.setAdminPassword(null);
        return profile;
    }

    @Override
    public boolean updateAdminProfile(Admin admin) {
        if (StrUtil.isBlank(admin.getAdminId())) {
            throw BusinessException.of("管理员ID不能为空");
        }
        // 使用 LambdaUpdate 确保按业务 ID (ADMIN_ID) 进行精准更新
        return this.lambdaUpdate()
                .eq(Admin::getAdminId, admin.getAdminId())
                .set(Admin::getAdminNickname, admin.getAdminNickname())
                .set(Admin::getAdminIntro, admin.getAdminIntro())
                .set(Admin::getAdminInfoUpdateTime, System.currentTimeMillis())
                .update();
    }

    @Override
    public boolean updateAdminAvatar(String adminId, String avatarUrl) {
        if (StrUtil.hasBlank(adminId, avatarUrl)) {
            throw BusinessException.of("管理员ID和头像URL不能为空");
        }
        // 执行头像更新并同步更新时间戳
        return this.lambdaUpdate()
                .eq(Admin::getAdminId, adminId)
                .set(Admin::getAdminAvatar, avatarUrl)
                .set(Admin::getAdminInfoUpdateTime, System.currentTimeMillis())
                .update();
    }
}
