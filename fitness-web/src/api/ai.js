import request from '@/utils/request'

export function generatePlan(data) {
  return request({ url: '/ai/plan', method: 'post', data })
}
