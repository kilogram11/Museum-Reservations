import request from '@/utils/request'

// 公告列表：POST /admin/news/list，传递query参数
export const getNoticeListApi = {
  getList: (params) => request({ 
    url: '/admin/news/list', 
    method: 'post', 
    params // 传递keyword/page/limit
  })
}

// 新增公告：POST /admin/news/add，传递JSON体
export const addNoticeApi = {
  add: (data) => request({ 
    url: '/admin/news/add', 
    method: 'post', 
    data // {newsTitle, newsDesc, newsStatus}
  })
}

// 编辑公告：POST /admin/news/edit，传递JSON体
export const editNoticeApi = {
  edit: (data) => request({ 
    url: '/admin/news/edit', 
    method: 'post', 
    data // {id, newsTitle, newsDesc, newsStatus}
  })
}

// 删除公告：POST /admin/news/del，传递id参数
export const deleteNoticeApi = {
  delete: (id) => request({ 
    url: '/admin/news/del', 
    method: 'post', 
    params: { id } // 后端del接口接收id参数
  })
}

// 查看公告详情：替换原publishNoticeApi，对应API /news/view
export const getNoticeDetailApi = {
  getDetail: (id) => request({ 
    url: '/admin/news/view', 
    method: 'post', 
    params: { id } // API要求传递id参数（query/form-data）
  })
}