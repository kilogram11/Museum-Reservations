import request from '@/utils/request'
import { PAGINATION } from '@/config/constants'

export const blacklistApi = {
  list: (params = {}) => {
    const defaultParams = {
      page: PAGINATION.DEFAULT_PAGE,
      limit: PAGINATION.DEFAULT_LIMIT,
      ...params
    }
    return request({ 
      url: '/admin/blacklist/list', 
      method: 'post', 
      data: defaultParams 
    })
  },
  
  add: (data) => request({ 
    url: '/admin/blacklist/add', 
    method: 'post', 
    data 
  }),
  
  remove: (data) => request({ 
    url: '/admin/blacklist/remove', 
    method: 'post', 
    data 
  }),
  
  updateTime: (data) => request({ 
    url: '/admin/blacklist/updateTime', 
    method: 'post', 
    data 
  })
}