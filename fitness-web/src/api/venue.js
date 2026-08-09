import request from '@/utils/request'

export function getVenueList(params) {
  return request({ url: '/venue/list', method: 'get', params })
}

export function getMyVenues() {
  return request({ url: '/venue/my', method: 'get' })
}

export function getVenueDetail(id) {
  return request({ url: `/venue/${id}`, method: 'get' })
}

export function createVenue(data) {
  return request({ url: '/venue/create', method: 'post', data })
}

export function updateVenue(id, data) {
  return request({ url: `/venue/${id}`, method: 'put', data })
}

export function updateVenueStatus(id, status) {
  return request({ url: `/venue/${id}/status`, method: 'put', params: { status } })
}

export function deleteVenue(id) {
  return request({ url: `/venue/${id}`, method: 'delete' })
}
