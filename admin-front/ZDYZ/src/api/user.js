import request from '@/utils/request'

export const loginApi = {
  // 登录接口（对接后端数据库验证账号密码）
  login: (data) => {
    return request({
      url: '/admin/auth/login',
      method: 'post',
      data
    })
  }
}