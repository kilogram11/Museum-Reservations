import request from '@/utils/request'

// 1. 获取场馆列表（对应后端 /admin/museum/list，POST请求，需传分页/关键词参数）
export const getVenueListApi = {
  getList: (params) => request({ 
    url: '/admin/museum/list',  // 匹配后端接口路径
    method: 'post',             // 后端是 @PostMapping，必须用POST
    data: params                // 后端接收@RequestBody，用data传参（params是分页/关键词对象：{page, limit, keyword}）
  })
}

// 2. 新增/录入场馆（对应后端 /admin/museum/add，POST请求）
export const addVenueApi = {
  add: (data) => request({ 
    url: '/admin/museum/add',   // 匹配后端添加接口
    method: 'post',
    data: data                  // 传MuseumAddDTO对应的字段（如场馆名称、排期等）
  })
}

// 3. 编辑场馆（对应后端 /admin/museum/edit，POST请求）
export const editVenueApi = {
  edit: (data) => request({ 
    url: '/admin/museum/edit',  // 匹配后端编辑接口
    method: 'post',
    data: data                  // 传MuseumEditDTO对应的字段（如id、修改的场馆信息）
  })
}

// 4. 删除场馆（对应后端 /admin/museum/del，POST请求）
export const delVenueApi = {
  del: (data) => request({ 
    url: '/admin/museum/del',   // 匹配后端删除接口
    method: 'post',
    data: data                  // 传{id: 场馆ID}
  })
}

// 5. 修改场馆状态（对应后端 /admin/museum/status，POST请求）
export const changeVenueStatusApi = {
  changeStatus: (data) => request({ 
    url: '/admin/museum/status',// 匹配后端状态修改接口
    method: 'post',
    data: data                  // 传{id: 场馆ID, status: 状态值（如0/1）}
  })
}

// 6. 获取场馆详情（对应后端 /admin/museum/detail，GET请求，传id参数）
export const getVenueDetailApi = {
  getDetail: (id) => request({ 
    url: '/admin/museum/detail',// 匹配后端详情接口
    method: 'get',              // 后端是 @GetMapping
    params: { id: id }          // 后端@RequestParam接收，用params传参
  })
}

// 7. 获取所有场馆（下拉列表，对应后端 /admin/museum/all，GET请求）
export const getAllVenueApi = {
  getAll: () => request({ 
    url: '/admin/museum/all',   // 匹配后端下拉列表接口
    method: 'get'
  })
}