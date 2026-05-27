package com.museum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.museum.entity.Head;
import com.museum.entity.User;
import java.util.List;

/**
 * 用户业务接口
 */
public interface UserService extends IService<User> {

    /**
     * 手机号登录 (自动注册)
     * 
     * @param mobile 手机号
     * @param code   验证码
     * @return token
     */
    String loginByMobile(String mobile, String code);

    /**
     * 获取用户信息
     */
    User getUserInfo();

    /**
     * 更新用户信息
     */
    void updateUserInfo(User user);

    /**
     * 获取所有预置头像
     */
    List<Head> getHeadList();
}
