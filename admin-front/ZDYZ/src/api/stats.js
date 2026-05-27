import request from '@/utils/request'

export const getStatsApi = {
  // 获取首页统计数据 (4张卡片)
  getHomeStats() {
    return request({
      url: '/stats/home',
      method: 'get'
    })
  },

  // 获取预约趋势 (Line Chart)
  getTrend() {
    return request({
      url: '/stats/trend',
      method: 'get'
    })
  },

  // 获取核销状态 (Pie Chart)
  getCheckinStatus() {
    return request({
      url: '/stats/checkin',
      method: 'get'
    })
  },

  // 获取热门公告 (Bar Chart)
  getPopularNews() {
    return request({
      url: '/stats/popular-news',
      method: 'get'
    })
  },

  // 获取爽约对比 (Bar Chart)
  getNoShowComparison() {
    return request({
      url: '/stats/noshow-comparison',
      method: 'get'
    })
  }
}