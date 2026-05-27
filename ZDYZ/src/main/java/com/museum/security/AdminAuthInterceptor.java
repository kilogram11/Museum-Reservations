package com.museum.security;
//# 管理端认证拦截器
//│   # 拦截 /admin/** 路径
// │   # 检查Token是否有效
// │   # 从Token解析出adminId并存入Request
// │   # 排除 /admin/login 不拦截
public class AdminAuthInterceptor {
}
