import request from '@/utils/request'

export const profileApi = {
  // 获取个人信息（用户名、简介、头像）
  getProfile: () => {
    return request({
      url: '/admin/auth/profile',
      method: 'get'
    })
  },
  // 更新用户名和简介
  updateProfile: (data) => {
    return request({
      url: '/admin/auth/profile/update',
      method: 'post',
      data
    })
  },
  // 更新头像
  updateAvatar: (data) => {
    return request({
      url: '/admin/auth/profile/update-avatar',
      method: 'post',
      data
    })
  }
}