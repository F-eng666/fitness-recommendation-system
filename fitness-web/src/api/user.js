import request from '@/utils/request'

export function getUserProfile() {
  return request({ url: '/user/profile', method: 'get' })
}

export function updateUserProfile(data) {
  return request({ url: '/user/profile', method: 'put', data })
}

export function changePassword(data) {
  return request({ url: '/user/password', method: 'put', data })
}

export function getUserList(params) {
  return request({ url: '/user/list', method: 'get', params })
}

export function updateUserStatus(id, status) {
  return request({ url: `/user/${id}/status`, method: 'put', data: { status } })
}

// 订单相关
export function getMyOrders(params) {
  return request({ url: '/order/my', method: 'get', params })
}

export function createOrder(data) {
  return request({ url: '/order/create', method: 'post', data })
}
export const bookCourse = createOrder

export function cancelOrder(id) {
  return request({ url: `/order/${id}/cancel`, method: 'put' })
}

export function requestRefund(id, reason) {
  return request({ url: `/order/${id}/request-refund`, method: 'put', params: { reason } })
}


// 打卡相关
export function checkIn(params) {
  return request({ url: '/checkin', method: 'post', params })
}

export function updateCheckIn(id, params) {
  return request({ url: `/checkin/${id}`, method: 'put', params })
}

export function getMyCheckIns(params) {
  return request({ url: '/checkin/my', method: 'get', params })
}

export function getTodayCheckInCount() {
  return request({ url: '/checkin/today/count', method: 'get' })
}

export function getCheckInStreak() {
  return request({ url: '/checkin/streak', method: 'get' })
}

export function getCheckInRank() {
  return request({ url: '/checkin/rank', method: 'get' })
}

// 收藏相关
export function toggleFavorite(targetType, targetId) {
  return request({ url: '/favorite/toggle', method: 'post', params: { targetType, targetId } })
}
// Aliases
export function addFavorite(targetType, targetId) {
  return request({ url: '/favorite/toggle', method: 'post', params: { targetType, targetId } })
}
export const removeFavorite = addFavorite

export function getMyFavorites(params) {
  return request({ url: '/favorite/my', method: 'get', params })
}

export function checkFavorite(targetType, targetId) {
  return request({ url: '/favorite/check', method: 'get', params: { targetType, targetId } })
}

// 评价相关
export function createReview(data) {
  return request({ url: '/review/create', method: 'post', data })
}

export function getTargetReviews(params) {
  return request({ url: '/review/list', method: 'get', params })
}

// Aliases for components using different names
export const getReviewList = getTargetReviews
export function submitReview(data) {
  return request({ url: '/review/create', method: 'post', data })
}

// 管理端
export function getDashboardStats() {
  return request({ url: '/admin/dashboard', method: 'get' })
}

export function getVenueOrders(params) {
  return request({ url: '/order/venue', method: 'get', params })
}

export function updateOrderStatus(id, status) {
  return request({ url: `/order/${id}/status`, method: 'put', params: { status } })
}

export function replyReview(id, reply) {
  return request({ url: `/review/${id}/reply`, method: 'put', data: { reply } })
}

// 公告
export function getActiveAnnouncements() {
  return request({ url: '/announcement/active', method: 'get' })
}

export function getAnnouncementList(params) {
  return request({ url: '/announcement/list', method: 'get', params })
}

export function createAnnouncement(data) {
  return request({ url: '/announcement/create', method: 'post', data })
}

export function deleteAnnouncement(id) {
  return request({ url: `/announcement/${id}`, method: 'delete' })
}

// 教练
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
