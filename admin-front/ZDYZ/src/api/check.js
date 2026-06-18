import request from '@/utils/request'
import { PAGINATION } from '@/config/constants'

export const checkApi = {
  list: (params = {}) => {
    const defaultParams = {
      page: PAGINATION.DEFAULT_PAGE,
      limit: PAGINATION.DEFAULT_LIMIT,
      ...params
    }
    return request({ 
      url: '/admin/join/list', 
      method: 'post', 
      data: defaultParams 
    })
  },
  
  check: (id) => request({ 
    url: '/admin/join/checkin', 
    method: 'post', 
    data: { id } 
  })
}