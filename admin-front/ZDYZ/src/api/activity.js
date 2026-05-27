import request from '@/utils/request'

// 获取活动列表（API要求POST方法，支持分页和关键词搜索）
export const getActivityListApi = {
  getList: (data) => request({ 
    url: '/admin/activity/list',  // 修正为API文档的路径
    method: 'post', 
    data  // 传入包含keyword、page、limit的对象
  })
}

// 添加活动（API要求POST方法，调整参数结构）
export const addActivityApi = {
  add: (data) => request({ 
    url: '/admin/activity/add',  // 修正为API文档的路径
    method: 'post', 
    data  // 需包含activityTitle、adminId、startDate等必填字段
  })
}

// 编辑活动（API要求POST方法，修正HTTP方法和URL）
export const editActivityApi = {
  edit: (data) => request({ 
    url: '/admin/activity/edit',  // 修正为API文档的路径
    method: 'post',  // API文档中编辑接口为POST方法
    data  // 需包含id及其他待修改字段
  })
}

// 删除活动（API要求POST方法，修正HTTP方法和参数传递方式）
export const deleteActivityApi = {
  delete: (id) => request({ 
    url: '/admin/activity/del',  // 修正为API文档的路径
    method: 'post',  // API文档中删除接口为POST方法
    data: { id }  // 以JSON body形式传递id
  })
}

// 获取活动详情（新增接口）
export const getActivityDetailApi = {
  get: (id) => request({
    url: '/admin/activity/detail',  // 修正为API文档的路径
    method: 'get',
    params: { id }  // GET方法参数
  })
}

// 新增：修改活动状态接口（对齐API文档）
export const changeActivityStatusApi = {
  change: (data) => request({
    url: '/admin/activity/status',  // API文档中修改状态的路径
    method: 'post',
    data  // 包含id和status字段
  })
}