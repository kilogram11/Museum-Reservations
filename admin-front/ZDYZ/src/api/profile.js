import request from '@/utils/request'

export const profileApi = {
  get: () => request({ 
    url: '/admin/auth/profile', 
    method: 'get' 
  }),
  
  update: (data) => request({ 
    url: '/admin/auth/profile/update', 
    method: 'post', 
    data 
  }),
  
  avatar: (data) => request({ 
    url: '/admin/auth/profile/update-avatar', 
    method: 'post', 
    data 
  })
}