import request from '@/utils/request'

export function getTargetReviews(params) {
  return request({ url: '/review/list', method: 'get', params })
}

export function createReview(data) {
  return request({ url: '/review/create', method: 'post', data })
}

export function replyReview(id, reply) {
  return request({ url: `/review/${id}/reply`, method: 'put', data: { reply } })
}

export function deleteReview(id) {
  return request({ url: `/review/${id}`, method: 'delete' })
}

// Alias for venue admin
export function getVenueReviews(params) {
  return request({ url: '/review/list', method: 'get', params })
}
