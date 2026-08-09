import request from '@/utils/request'

export function getTodayPlan() {
  return request.get('/diet/today-plan')
}

export function createDietPlan(data) {
  return request.post('/diet/plan', data)
}

export function getDietRecords(params) {
  return request.get('/diet/records', { params })
}

export function getDietSummary(params) {
  return request.get('/diet/summary', { params })
}
