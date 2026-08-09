import request from '@/utils/request'

export function getCoachList(params) {
  return request({ url: '/coach/list', method: 'get', params })
}

export function getCoachDetail(id) {
  return request({ url: `/coach/${id}`, method: 'get' })
}

export function createCoach(data) {
  return request({ url: '/coach/create', method: 'post', data })
}

export function updateCoach(id, data) {
  return request({ url: `/coach/${id}`, method: 'put', data })
}

export function deleteCoach(id) {
  return request({ url: `/coach/${id}`, method: 'delete' })
}
