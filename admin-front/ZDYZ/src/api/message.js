import request from '@/utils/request'
import { PAGINATION } from '@/config/constants'

export const messageApi = {
  list: (params = {}) => {
    const defaultParams = {
      page: PAGINATION.DEFAULT_PAGE,
      limit: PAGINATION.DEFAULT_LIMIT,
      ...params
    }
    return request({ 
      url: '/admin/message/template/list', 
      method: 'get', 
      params: defaultParams 
    })
  },
  
  detail: (id) => request({ 
    url: `/admin/message/template/${id}`, 
    method: 'get' 
  }),
  
  update: (data) => request({ 
    url: '/admin/message/template/update', 
    method: 'post', 
    data 
  })
}