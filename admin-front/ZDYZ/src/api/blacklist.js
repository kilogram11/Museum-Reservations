import request from '@/utils/request'

export const getBlacklistApi = {
  // 改为 POST 请求，路径对齐后端 /admin/blacklist/list
  list: (data) => request({ url: '/admin/blacklist/list', method: 'post', data }),

  // 导出黑名单
  exportData: () => {
    const token = localStorage.getItem('token')
    return import('axios').then(axios => {
      const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
      return axios.default.get(baseURL + '/admin/blacklist/export', {
        headers: { Authorization: `Bearer ${token}` },
        responseType: 'blob'
      })
    })
  }
}

export const cancelBlackApi = {
  // 对齐后端移除黑名单接口 /admin/blacklist/remove
  remove: (data) => request({ url: '/admin/blacklist/remove', method: 'post', data })
}

export const addBlackApi = {
  // 对齐后端手动拉黑接口 /admin/blacklist/add
  add: (data) => request({ url: '/admin/blacklist/add', method: 'post', data })
}

export const editBlackTimeApi = {
  // 对齐后端更新拉黑时间接口 /admin/blacklist/updateTime
  updateTime: (data) => request({ url: '/admin/blacklist/updateTime', method: 'post', data })
}