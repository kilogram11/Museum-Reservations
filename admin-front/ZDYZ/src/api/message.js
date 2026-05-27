import request from '@/utils/request'

// 列表：GET /admin/message/template/list
export const getTemplateListApi = {
  getList: (params) => request({ 
    url: '/admin/message/template/list', 
    method: 'get', 
    params
  })
}

// 详情：GET /admin/message/template/{id}
export const getTemplateDetailApi = {
  getDetail: (id) => request({ 
    url: `/admin/message/template/${id}`, 
    method: 'get'
  })
}

// 更新：POST /admin/message/template/update
export const updateTemplateApi = {
  update: (data) => request({ 
    url: '/admin/message/template/update', 
    method: 'post', 
    data
  })
}
