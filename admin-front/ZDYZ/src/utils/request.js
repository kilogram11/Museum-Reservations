import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 5000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器（添加token）
service.interceptors.request.use(
  (config) => {
    console.log('➡️ axios 请求即将发送', config.url, config.data)
    const token = localStorage.getItem('token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    // 如果是FormData类型，删除默认JSON头（axios会自动设置multipart/form-data）
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }
    return config
  },
  (error) => {
    ElMessage.error('请求异常，请重试')
    return Promise.reject(error)
  }
)

// 响应拦截器（统一处理返回值）
service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.msg || '操作失败')
      return Promise.reject(res)
    }
    return res
  },
  (error) => {
    ElMessage.error(error.message || '服务器错误')
    return Promise.reject(error)
  }
)

export default service