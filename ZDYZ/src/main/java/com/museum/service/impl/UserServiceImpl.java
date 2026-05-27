package com.museum.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.common.exception.BusinessException;
import com.museum.common.utils.IdGenerator;
import com.museum.common.utils.JwtUtil;
import com.museum.entity.Head;
import com.museum.entity.User;
import com.museum.mapper.HeadMapper;
import com.museum.mapper.UserMapper;
import com.museum.service.UserService;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户业务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HeadMapper headMapper;

    @Override
    public String loginByMobile(String mobile, String code) {
        if (StrUtil.isBlank(mobile) || StrUtil.isBlank(code)) {
            throw BusinessException.of("手机号和验证码不能为空");
        }
        // 模拟验证码校验
        if (!"1234".equals(code)) {
            // 实际项目应从 Redis 校验
            // throw BusinessException.of("验证码错误");
        }

        User user = this.lambdaQuery().eq(User::getUserMobile, mobile).one();
        if (user == null) {
            // 注册新用户
            user = new User();
            user.setId(IdGenerator.generateId()); // 使用 id
            user.setUserId(IdGenerator.generateUserId());
            user.setUserMobile(mobile);
            // 默认用户名: 用户+手机尾号后4位
            String suffix = mobile.length() >= 4 ? mobile.substring(mobile.length() - 4) : mobile;
            user.setUserName("用户" + suffix);
            user.setUserPic(1); // 默认选择第一个头像
            user.setUserAddTime(System.currentTimeMillis());
            user.setUserEditTime(System.currentTimeMillis());
            user.setPid("1"); // 使用 pid

            this.save(user);
        }

        // 生成 Token
        return jwtUtil.generateAppToken(user.getUserId());
    }

    @Override
    public User getUserInfo() {
        String token = request.getHeader("Token");
        if (StrUtil.isBlank(token))
            throw BusinessException.of(401, "未登录");

        String userId = jwtUtil.getSubjectFromToken(token);
        if (StrUtil.isBlank(userId))
            throw BusinessException.of(401, "无效Token");

        User user = this.lambdaQuery().eq(User::getUserId, userId).one();
        if (user == null)
            throw BusinessException.of(401, "用户不存在");

        // 填充头像 URL
        if (user.getUserPic() != null) {
            Head head = headMapper.selectById(user.getUserPic().toString());
            if (head != null) {
                user.setUserPicUrl(head.getHeadPicUrl());
            }
        }

        // 密码/敏感信息脱敏（User表目前没存密码，暂不需要）
        return user;
    }

    @Override
    public void updateUserInfo(User user) {
        String token = request.getHeader("Token");
        String userId = jwtUtil.getSubjectFromToken(token);
        if (StrUtil.isBlank(userId))
            throw BusinessException.of(401, "未登录");

        // 确保只能修改自己的信息
        User exist = this.lambdaQuery().eq(User::getUserId, userId).one();
        if (exist == null)
            throw BusinessException.of("用户不存在");

        if (StrUtil.isNotBlank(user.getUserName())) {
            exist.setUserName(user.getUserName());
        }
        if (user.getUserPic() != null) {
            exist.setUserPic(user.getUserPic());
        }
        exist.setUserEditTime(System.currentTimeMillis());

        baseMapper.updateUserInfo(exist);
    }

    @Override
    public List<Head> getHeadList() {
        return headMapper.selectList(null);
    }
}
