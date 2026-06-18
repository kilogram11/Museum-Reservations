import request from '@/utils/request'

export const statsApi = {
  home: () => request({ 
    url: '/stats/home', 
    method: 'get' 
  }),
  
  trend: () => request({ 
    url: '/stats/trend', 
    method: 'get' 
  }),
  
  checkin: () => request({ 
    url: '/stats/checkin', 
    method: 'get' 
  }),
  
  popularNews: () => request({ 
    url: '/stats/popular-news', 
    method: 'get' 
  }),
  
  noShowComparison: () => request({ 
    url: '/stats/noshow-comparison', 
    method: 'get' 
  })
}