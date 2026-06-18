import request from '@/utils/request'
import { PAGINATION } from '@/config/constants'

export const activityApi = {
  list: (params = {}) => {
    const defaultParams = {
      page: PAGINATION.DEFAULT_PAGE,
      limit: PAGINATION.DEFAULT_LIMIT,
      ...params
    }
    return request({ 
      url: '/admin/activity/list', 
      method: 'post', 
      data: defaultParams 
    })
  },
  
  add: (data) => request({ 
    url: '/admin/activity/add', 
    method: 'post', 
    data 
  }),
  
  edit: (data) => request({ 
    url: '/admin/activity/edit', 
    method: 'post', 
    data 
  }),
  
  delete: (id) => request({ 
    url: '/admin/activity/del', 
    method: 'post', 
    data: { id } 
  }),
  
  detail: (id) => request({ 
    url: '/admin/activity/detail', 
    method: 'get', 
    params: { id } 
  }),
  
  status: (data) => request({ 
    url: '/admin/activity/status', 
    method: 'post', 
    data 
  })
}