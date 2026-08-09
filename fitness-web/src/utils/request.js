import axios from 'axios'
import { getToken, removeToken } from './auth'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    // 业务层返回401也做处理
    if (res.code === 401) {
      removeToken()
      localStorage.removeItem('userRole')
      localStorage.removeItem('userInfo')
      window.location.hash = '#/login'
      ElMessage.error('登录已过期，请重新登录')
      return Promise.reject(new Error('未授权'))
    }
    return res
  },
  error => {
    // HTTP 401 / 403 统一处理
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        removeToken()
        localStorage.removeItem('userRole')
        localStorage.removeItem('userInfo')
        window.location.hash = '#/login'
        ElMessage.error('登录已过期，请重新登录')
        return Promise.reject(error)
      } else if (status === 403) {
        ElMessage.error('权限不足，无法访问该资源')
      } else {
        ElMessage.error(error.response.data?.message || error.message || '请求失败')
      }
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default request
