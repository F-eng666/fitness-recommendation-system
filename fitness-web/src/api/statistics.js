import request from '@/utils/request'

export function getAdminDashboard() {
  return request.get('/statistics/dashboard')
}

export function getUserStats() {
  return request.get('/statistics/user-stats')
}

export function getVenueStats(venueId) {
  return request.get('/statistics/venue-stats', { params: { venueId } })
}

export function getMonthlyStats(params) {
  return request.get('/statistics/monthly', { params })
}
