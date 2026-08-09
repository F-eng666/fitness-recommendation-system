import request from '@/utils/request'

export function getMyOrders(params) {
  return request({ url: '/order/my', method: 'get', params })
}

export function getVenueOrders(params) {
  return request({ url: '/order/venue', method: 'get', params })
}

export function createOrder(data) {
  return request({ url: '/order/create', method: 'post', data })
}

export function updateOrderStatus(id, status) {
  return request({ url: `/order/${id}/status`, method: 'put', params: { status } })
}

export function cancelOrder(id) {
  return request({ url: `/order/${id}/cancel`, method: 'put' })
}

export function approveRefund(id) {
  return request({ url: `/order/${id}/approve-refund`, method: 'put' })
}

export function rejectRefund(id) {
  return request({ url: `/order/${id}/reject-refund`, method: 'put' })
}
