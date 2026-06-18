import request from '@/utils/request'
import { PAGINATION } from '@/config/constants'

export const noticeApi = {
  list: (params = {}) => {
    const defaultParams = {
      page: PAGINATION.DEFAULT_PAGE,
      limit: PAGINATION.DEFAULT_LIMIT,
      ...params
    }
    return request({ 
      url: '/admin/news/list', 
      method: 'post', 
      data: defaultParams 
    })
  },
  
  add: (data) => request({ 
    url: '/admin/news/add', 
    method: 'post', 
    data 
  }),
  
  edit: (data) => request({ 
    url: '/admin/news/edit', 
    method: 'post', 
    data 
  }),
  
  delete: (id) => request({ 
    url: '/admin/news/del', 
    method: 'post', 
    params: { id } 
  }),
  
  detail: (id) => request({ 
    url: '/admin/news/view', 
    method: 'post', 
    params: { id } 
  })
}