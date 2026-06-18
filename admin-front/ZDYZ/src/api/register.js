import request from '@/utils/request'
export const registerApi = {
    register: (data) => {
    return request({
      url: '/admin/auth/register',
      method: 'post',
      data
    })
  }
}