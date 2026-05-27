package com.museum.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.common.constant.BookingConstant;
import com.museum.common.exception.BusinessException;
import com.museum.common.exception.ErrorCode;
import com.museum.common.utils.IdGenerator;
import com.museum.common.utils.JwtUtil;
import com.museum.entity.Head;
import com.museum.entity.User;
import com.museum.mapper.HeadMapper;
import com.museum.mapper.UserMapper;
import com.museum.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
            throw new BusinessException(ErrorCode.USER_MOBILE_EMPTY);
        }

        if (!BookingConstant.MOCK_VERIFY_CODE.equals(code)) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_ERROR);
        }

        User user = this.lambdaQuery().eq(User::getUserMobile, mobile).one();
        if (user == null) {
            user = registerNewUser(mobile);
        }

        return jwtUtil.generateAppToken(user.getUserId());
    }

    @Override
    public User getUserInfo() {
        User user = requireLoginUser();

        if (user.getUserPic() != null) {
            fillUserPicUrl(user);
        }

        return user;
    }

    @Override
    public void updateUserInfo(User user) {
        User exist = requireLoginUser();

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

    // ==================== 私有方法 ====================

    private String getCurrentUserId() {
        String token = request.getHeader("Token");
        if (StrUtil.isBlank(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String userId = jwtUtil.getSubjectFromToken(token);
        if (StrUtil.isBlank(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return userId;
    }

    private User requireLoginUser() {
        String userId = getCurrentUserId();
        User user = this.lambdaQuery().eq(User::getUserId, userId).one();
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private User registerNewUser(String mobile) {
        User user = new User();
        user.setId(IdGenerator.generateId());
        user.setUserId(IdGenerator.generateUserId());
        user.setUserMobile(mobile);
        user.setUserName(generateDefaultUserName(mobile));
        user.setUserPic(1);
        user.setUserAddTime(System.currentTimeMillis());
        user.setUserEditTime(System.currentTimeMillis());
        user.setPid(BookingConstant.DEFAULT_PID);

        this.save(user);
        return user;
    }

    private String generateDefaultUserName(String mobile) {
        String suffix = mobile.length() >= BookingConstant.MOBILE_SUFFIX_LENGTH
                ? mobile.substring(mobile.length() - BookingConstant.MOBILE_SUFFIX_LENGTH)
                : mobile;
        return BookingConstant.DEFAULT_USER_NAME_PREFIX + suffix;
    }

    private void fillUserPicUrl(User user) {
        Head head = headMapper.selectById(user.getUserPic().toString());
        if (head != null) {
            user.setUserPicUrl(head.getHeadPicUrl());
        }
    }
}
