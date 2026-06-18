import axios from 'axios'
import { ElMessage } from 'element-plus'

export const exportData = async (url, fileName = 'export', params = {}) => {
  try {
    const token = localStorage.getItem('token')
    const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
    
    ElMessage.info('正在下载，请稍候...')
    
    const response = await axios.get(`${baseURL}${url}`, {
      headers: { Authorization: `Bearer ${token}` },
      responseType: 'blob',
      params
    })
    
    const blobUrl = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = blobUrl
    
    const disposition = response.headers['content-disposition']
    let exportFileName = `${fileName}.xlsx`
    if (disposition) {
      const match = disposition.match(/filename=(.+)/)
      if (match && match[1]) {
        exportFileName = decodeURIComponent(match[1])
      }
    }
    
    link.setAttribute('download', exportFileName)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    return true
  } catch (error) {
    console.error('Export failed:', error)
    ElMessage.error('导出失败')
    return false
  }
}

export const exportPostData = async (url, data = {}, fileName = 'export') => {
  try {
    const token = localStorage.getItem('token')
    const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
    
    ElMessage.info('正在下载，请稍候...')
    
    const response = await axios.post(`${baseURL}${url}`, data, {
      headers: { Authorization: `Bearer ${token}` },
      responseType: 'blob'
    })
    
    const blobUrl = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = blobUrl
    
    const disposition = response.headers['content-disposition']
    let exportFileName = `${fileName}.xlsx`
    if (disposition) {
      const match = disposition.match(/filename=(.+)/)
      if (match && match[1]) {
        exportFileName = decodeURIComponent(match[1])
      }
    }
    
    link.setAttribute('download', exportFileName)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    return true
  } catch (error) {
    console.error('Export failed:', error)
    ElMessage.error('导出失败')
    return false
  }
}