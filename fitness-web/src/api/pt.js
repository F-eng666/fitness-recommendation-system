import request from '@/utils/request'

export function getAvailableSlots(params) {
  return request({ url: '/pt/slots', method: 'get', params })
}

export function bookTraining(data) {
  return request({ url: '/pt/book', method: 'post', data })
}

export function getMyBookings() {
  return request({ url: '/pt/my', method: 'get' })
}
