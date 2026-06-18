import request from '@/utils/request'
import { RESPONSE_CODE, PAGINATION } from '@/config/constants'

export const venueApi = {
  list: (params = {}) => {
    const defaultParams = {
      page: PAGINATION.DEFAULT_PAGE,
      limit: PAGINATION.DEFAULT_LIMIT,
      ...params
    }
    return request({ 
      url: '/admin/museum/list', 
      method: 'post', 
      data: defaultParams 
    })
  },
  
  add: (data) => request({ 
    url: '/admin/museum/add', 
    method: 'post', 
    data 
  }),
  
  edit: (data) => request({ 
    url: '/admin/museum/edit', 
    method: 'post', 
    data 
  }),
  
  delete: (id) => request({ 
    url: '/admin/museum/del', 
    method: 'post', 
    data: { id } 
  }),
  
  detail: (id) => request({ 
    url: '/admin/museum/detail', 
    method: 'get', 
    params: { id } 
  }),
  
  status: (data) => request({ 
    url: '/admin/museum/status', 
    method: 'post', 
    data 
  }),
  
  all: () => request({ 
    url: '/admin/museum/all', 
    method: 'get' 
  })
}

export { RESPONSE_CODE, PAGINATION }