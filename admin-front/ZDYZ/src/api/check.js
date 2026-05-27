import request from '@/utils/request'

export const getCheckListApi = {
  // 获取核销列表（分页+条件查询）
  getList: (params) => {
    return request({
      url: '/admin/join/list',
      method: 'post',
      data: params
    })
  }
}

export const checkBookingApi = {
  // 核销操作（更新数据库中订单状态）
  check: (id) => {
    return request({
      url: '/admin/join/checkin',
      method: 'post',
      data: { id }
    })
  },
  
  // 导出全部记录
  // 绕过 request.js 拦截器，使用 raw axios 处理 blob
  exportData: () => {
    const token = localStorage.getItem('token')
    return import('axios').then(axios => {
      const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
      return axios.default.get(baseURL + '/admin/join/export', {
        headers: { Authorization: `Bearer ${token}` },
        responseType: 'blob'
      })
    })
  }
}