// api/user.js
import { get, post } from '../utils/request';

// 手机号登录/注册（不需要Token）
export const login = (mobile, code) => {
  return post('/app/user/login', { mobile, code });
};

// 获取个人信息（需要Token）
export const getUserInfo = () => {
  return get('/app/user/info');
};

// 修改个人信息（需要Token）
export const updateUserInfo = (data) => {
  return post('/app/user/update', data);
};

// 获取头像列表
export const getHeads = () => {
  return get('/app/user/heads');
};