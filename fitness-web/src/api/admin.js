import request from '@/utils/request'

export function getAdminStats() {
  return request({ url: '/admin/dashboard', method: 'get' })
}

export function getStatistics(params) {
  return request({ url: '/admin/statistics', method: 'get', params })
}
